### 背景

有这样一个业务场景，作为一个金融产品，很多用户会在借款后发生逾期，每一笔逾期都是一笔单独的借据，在贷后催收环节中，需要基于用户维度做聚合，把多笔借据合并成一个案件进行统一的催收。

那么就需要一个把多笔借据合并成一个案件的操作。最开始采用的方案就是定时任务扫表，每天早上凌晨5-8点之间进行定时任务扫表，然后进行案件的合并。

但是随着业务量的增多，扫表经常会扫不完，于是业务上为了提效，把定时任务改为分布式任务，借助多实例进行批量扫表。

但是这样做就导致数据库扛不住了，数据库的连接池经常在跑任务的时候被打满。于是就需要想办法解决这个问题。

### 技术选型

关于这个问题，有挺多方案的。

首先就是可以选择分库分表，把原来的单独分成多个库，这样整体的连接数就多了，也就可以扛得住并发扫表了。但是这个方案比较重，分表后也会带来一系列问题。

于是考虑了另外一种方案，那就是基于"同步转异步"的思想，在借据生成的时候，就进行合并，而不是定时任务批量合并。

这样就可以把集中地流量分散到每一条借据生成的过程中，而且这个过程允许失败，一旦失败了，通过定时任务补偿即可。

但是这么做就会导致借据生成这部分逻辑很复杂，需要考虑到合并案件的事情，耦合性太深了。于是就基于Spring Event，把借据生成和案件合并进行解耦。

所以，整体方案就是基于Spring Event，实现同步转异步，解决定时任务扫表导致数据库连接池不够的问题。

在方案改造前，每次扫表需要处理的数据量有20万条，改造后，只需要1000左右的数据量需要扫表处理，大大提升系统的可用性。

### 你做了什么

在借据生成的方法中，增加一个事件发送：

```

protected BaseManageResponse genenrateLoan(LoanGenerateEvent loanGenerateEvent) {
    BaseManageResponse manageResponse = new BaseManageResponse();

    try {
        //开启事务
        return transactionTemplate.execute(transactionStatus -> {
            
            //核心逻辑执行
            doGenerateLoan(loanGenerateEvent);

            //发送一个案件入催完成的事件
            try {
                applicationContext.publishEvent(new CaseStartFinishEvent(loanGenerateEven));
            } catch (Exception e) {
                LOG.warn("publishLoanGenerateEventEvent failed", e);
            }

            //结果返回
            return manageResponse.successResponse(caseModel);
        });
    } catch (Exception e) {
        LoanGenerateStream existStream = queryExistStream(request);
        if (existStream != null) {
            return manageResponse.duplicatedResponse(existStream);
        }
        throw e;
    }

}
```

这里在`applicationContext.publishEvent(new CaseStartFinishEvent(caseModel.getCaseItem()));`中发送一个事件，并且用try-catch包上，一旦失败了，不影响主流程。

然后再定义一个监听器，处理这个事件：

```

/**
 * 案件中心内部事件监听器
 *
 * @author Hollis
 */
@Component
public class CollectionCaseEventListener {

    @Autowired
    private CaseManageService caseManageService;

    @Autowired
    private DistributeLockSupport distributeLockSupport;
 
    @EventListener(CaseStartFinishEvent.class)
    @Async("caseStartFinishExecutor")
    public void onApplicationEvent(CaseStartFinishEvent event) {
        LoanGenerateEvent loanGenerateEvent = (LoanGenerateEvent) event.getSource();
        //加分布式锁，避免并发情况下导致创建多条案件
        if (!distributeLockSupport.acquireLock(loanGenerateEvent.getUserId(), loanGenerateEvent.getBizId(), 10000)) {
            return;
        }
        try {
            CaseMergeEvent mergeEvent = new CaseMergeEvent();
            mergeEvent.setCaseItemId(loanGenerateEvent.getId());
            mergeEvent.setUserId(loanGenerateEvent.getUserId());
            mergeEvent.setUserIdType(loanGenerateEvent.getUserIdType());
            mergeEvent.setIdentifier(UUID.randomUUID().toString());
            mergeEvent.setProduct(loanGenerateEvent.getProduct());
            mergeEvent.setBizId(loanGenerateEvent.getBizId());
            mergeEvent.setBizDate(new Date());
            caseManageService.merge(mergeEvent);
            
        } finally {
            distributeLockSupport.releaseLock(loanGenerateEvent.getUserId(), loanGenerateEvent.getBizId());
        }
    }
}
```

这里主要有一个点需要提一下，就是加了分布式锁，避免并发导致重复。即一锁二查三更新。

### 学习资料

[✅定时任务扫表的方案有什么缺点？](https://www.yuque.com/hollis666/fo22bm/bgr91vskph8odcsr?view=doc_embed)

[✅分库分表后会带来哪些问题？](https://www.yuque.com/hollis666/fo22bm/yhseig?view=doc_embed)

[✅在Spring中如何使用Spring Event做事件驱动](https://www.yuque.com/hollis666/fo22bm/lgs78ulq6l3cg1qk?view=doc_embed)

[✅如何解决接口幂等的问题？](https://www.yuque.com/hollis666/fo22bm/gz2qwl?view=doc_embed)
