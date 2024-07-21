# 典型回答

Spring的事务传播机制用于控制在多个事务方法相互调用时事务的行为。

在复杂的业务场景中，多个事务方法之间的调用可能会导致事务的不一致，如出现数据丢失、重复提交等问题，使用事务传播机制可以避免这些问题的发生，保证事务的一致性和完整性。

Spring的事务规定了7种事务的传播级别，默认的传播机制是**REQUIRED**

- **REQUIRED**，如果不存在事务则开启一个事务，如果存在事务则加入之前的事务，总是只有一个事务在执行
- **REQUIRES_NEW**，每次执行新开一个事务，如果当前存在事务，则把当前事务挂起
- **SUPPORTS**，有事务则加入事务，没有事务则普通执行
- **NOT_SUPPORTED**，有事务则暂停该事务，没有则普通执行
- **MANDATORY**，强制有事务，没有事务则报异常
- **NEVER**，有事务则报异常
- **NESTED**，如果之前有事务，则创建嵌套事务，嵌套事务回滚不影响父事务，反之父事务影响嵌套事务

# 扩展知识
## 用法

假设有两个业务方法A和B，方法A在方法B中被调用，需要在事务中保证它们的一致性，如果方法A或方法B中的任何一个方法发生异常，则需要回滚事务。

使用Spring的事务传播机制，可以在方法A和方法B上使用相同的事务管理器，并通过设置相同的传播行为来保证事务的一致性和完整性。具体实现如下：

```
@Service
public class TransactionFooService {
    
    @Autowired
    private FooDao fooDao;
  
    
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void methodA() throws Exception {
        // do something
        fooDao.updateFoo();
    }
}

@Service
public class TransactionBarService {
    
    @Autowired
    private BarDao barDao;

  	@Autowired
  	private TransactionFooService transactionFooService;
    
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void methodB() throws Exception {
        // do something
        barDao.updateBar();
        transactionFooService.methodA();
    }
}



```

在上述示例中，方法A和方法B都使用了REQUIRED的传播行为，表示如果当前存在事务，则在当前事务中执行；如果当前没有事务，则创建一个新的事务。如果在方法A或方法B中出现异常，则整个事务会自动回滚。

## rollbackFor

rollbackFor是Spring事务中的一个属性，用于指定哪些异常会触发事务回滚。

在一个事务方法中，如果发生了rollbackFor属性指定的异常或其子类异常，则事务会回滚。**如果不指定rollbackFor，则默认情况下只有RuntimeException和Error会触发事务回滚。**

## 场景题

问：一个长的事务方法a，在读写分离的情况下，里面既有读库操作，也有写库操作，再调用个读库方法b，方法b该用什么传播机制呢？

这种情况，读方法如果是最后一步，直接not_supported就行了，避免读报错导致数据回滚。如果是中间步骤，最好还是要required，因为异常失败需要回滚一下。

例如：A B C三个操作，C就是最后一步，B就是中间步骤。<br />如果一个读操作在中间（如B操作）失败了，那么就需要让A做回滚，因为C还没执行，所以A必须回滚才能保证一致性。

