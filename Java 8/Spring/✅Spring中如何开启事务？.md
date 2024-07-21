# 典型回答

事务管理在系统开发中是不可缺少的一部分，Spring提供了很好事务管理机制，主要分为编程式事务和声明式事务两种。

#### 编程式事务

基于底层的API，如PlatformTransactionManager、TransactionDefinition 和 TransactionTemplate 等核心接口，开发者完全可以通过编程的方式来进行事务管理。

**编程式事务方式需要是开发者在代码中手动的管理事务的开启、提交、回滚等操作。**

```latex
public void test() {
      TransactionDefinition def = new DefaultTransactionDefinition();
      TransactionStatus status = transactionManager.getTransaction(def);

       try {
         // 事务操作
         // 事务提交
         transactionManager.commit(status);
      } catch (DataAccessException e) {
         // 事务回滚
         transactionManager.rollback(status);
         throw e;
      }
}
```

当然，我们也可以使用Spring中提供的TransactionTemplate来实现编程式事务。

```
    @Autowired
    protected TransactionTemplate transactionTemplate;

  	public void test(){
      return transactionTemplate.execute(transactionStatus -> {
         //事务操作
      });
    }

		public void test1(){
      transactionTemplate.execute(new TransactionCallbackWithoutResult() {
          @Override
          protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
              
          }
      });
    }
    
```

如以上代码，开发者可以通过API自己控制事务。

#### 声明式事务

**声明式事务管理方法允许开发者配置的帮助下来管理事务，而不需要依赖底层API进行硬编码。开发者可以只使用注解或基于配置的 XML 来管理事务。**

```latex
@Transactional
public void test() {
     // 事务操作  
}
```

如上，使用@Transactional即可给test方法增加事务控制。

# 扩展知识

## 声明式事务的优点

通过上面的例子，其实我们可以很容易的看出来，声明式事务帮助我们节省了很多代码，他会自动帮我们进行事务的开启、提交以及回滚等操作，把程序员从事务管理中解放出来。

**声明式事务管理使用了 AOP 实现的，本质就是在目标方法执行前后进行拦截。**在目标方法执行前加入或创建一个事务，在执行方法执行后，根据实际情况选择提交或是回滚事务。

**使用这种方式，对代码没有侵入性，方法内只需要写业务逻辑就可以了。**

但是，声明式事务真的有这么好么？倒也不见得。

## 声明式事务的粒度问题

首先，**声明式事务有一个局限，那就是他的最小粒度要作用在方法上。**

也就是说，如果想要给一部分代码块增加事务的话，那就需要把这个部分代码块单独独立出来作为一个方法。

但是，正是因为这个粒度问题，本人并不建议过度的使用声明式事务。

首先，因为声明式事务是通过注解的，有些时候还可以通过配置实现，这就会导致一个问题，那就是这个事务有可能被开发者忽略。

事务被忽略了有什么问题呢？

**首先，如果开发者没有注意到一个方法是被事务嵌套的，那么就可能会再方法中加入一些如RPC远程调用、消息发送、缓存更新、文件写入等操作。**

我们知道，这些操作如果被包在事务中，有两个问题：

1、这些操作自身是无法回滚的，这就会导致数据的不一致。可能RPC调用成功了，但是本地事务回滚了，可是RPC调用无法回滚了。

2、在事务中有远程调用，就会拉长整个事务。那么就会导致本事务的数据库连接一直被占用，那么如果类似操作过多，就会导致数据库连接池耗尽。

有些时候，即使没有在事务中进行远程操作，但是有些人还是可能会不经意的进行一些内存操作，如运算。或者如果遇到分库分表的情况，有可能不经意间进行跨库操作。

但是如果是编程式事务的话，业务代码中就会清清楚楚看到什么地方开启事务，什么地方提交，什么时候回滚。这样有人改这段代码的时候，就会强制他考虑要加的代码是否应该方法事务内。

有些人可能会说，已经有了声明式事务，但是写代码的人没注意，这能怪谁。

话虽然是这么说，但是我们还是希望可以通过一些机制或者规范，降低这些问题发生的概率。

比如建议大家使用编程式事务，而不是声明式事务。因为，作者工作这么多年来，发生过不止一次开发者没注意到声明式事务而导致的故障。

因为有些时候，声明式事务确实不够明显。

## 声明式事务用不对容易失效

除了事务的粒度问题，还有一个问题那就是声明式事务虽然看上去帮我们简化了很多代码，但是一旦没用对，也很容易导致事务失效。

如以下几种场景就可能导致声明式事务失效：

1、[@Transactional ](/Transactional ) 应用在非 public 修饰的方法上 <br />2、[@Transactional ](/Transactional ) 注解属性 propagation 设置错误 <br />3、[@Transactional ](/Transactional ) 注解属性 rollbackFor 设置错误 <br />4、同一个类中方法调用，导致@Transactional失效<br />5、异常被catch捕获导致@Transactional失效<br />6、数据库引擎不支持事务

以上几个问题，如果使用编程式事务的话，很多都是可以避免的。

使用声明事务失效的问题我们发生过很多次。不知道大家有没有遇到过，我是实际遇到过的

因为Spring的事务是基于AOP实现的，但是在代码中，有时候我们会有很多切面，不同的切面可能会来处理不同的事情，多个切面之间可能会有相互影响。

在之前的一个项目中，我就发现我们的Service层的事务全都失效了，一个SQL执行失败后并没有回滚，排查下来才发现，是因为一位同事新增了一个切面，这个切面里面做个异常的统一捕获，导致事务的切面没有捕获到异常，导致事务无法回滚。

> 后来解决的方式可以设置切面的执行顺序，比如使用@Order或者实现Ordered接口等，但是我们这里没这么做，而是经过代码分析后把异常捕获的这部分移除了，异常抛给上游再处理。
> 之所以没有通过设置顺序解决，主要是因为这玩意不好控制，完以后面新增了别的注解之类的还得再改动，所以经过分析之后这里异常捕获并非必要，就这么改了。


这样的问题，发生过不止一次，而且不容易被发现。

很多人还是会说，说到底还是自己能力不行，对事务理解不透彻，用错了能怪谁。

但是我还是那句话，**我们确实无法保证所有人的能力都很高**，也无法要求所有开发者都能不出错。我们能做的就是，尽量可以通过机制或者规范，来避免或者降低这些问题发生的概率。

其实，如果大家有认真看过阿里巴巴出的那份Java开发手册的话，其实就能发现，其中的很多规约并不是完完全全容易被人理解，有些也比较生硬，但是其实，这些规范都是从无数个坑里爬出来的开发者们总结出来的。

关于@Transactional的用法，规约中也有提到过，只不过规约中的观点没有我这么鲜明：

![](http://www.hollischuang.com/wp-content/uploads/2020/10/Jietu20201011-171246.jpg#height=163&id=pKRyh&originHeight=163&originWidth=1602&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=&width=1602)


