# 典型回答

Spring中比较容易失效的就是通过@Transactional 定义的声明式事务，他在以下几个场景中会导致事务失效，首先，就是Spring的@Transactional是基于Spring的AOP机制实现的，而AOP机制又是基于动态代理实现的。那么如果代理失效了，事务也就会失效。

### 代理失效的情况

[✅Spring的AOP在什么场景下会失效？](https://www.yuque.com/hollis666/fo22bm/qogczxzhispgvw96?view=doc_embed)

在上面这篇中介绍过几种代理失效的情况，主要就是分为类的内部调用、final方法、static方法等。

所以就是以下几种情况就会失效：

1、@Transactional 应用在非 public 修饰的方法上

```
public class MyService {
    @Transactional
    private void doInternal() {
        System.out.println("Doing internal work...");
    }
}
```

private方法，只会在当前对象中的其他方法中调用，也就是会进行对象的自调用，这种情况是用this调用的，并不会走到代理对象，而@Transactional是基于动态代理实现的，所以代理会失效。

2、同一个类中方法调用，导致@Transactional失效

```
public class MyService {
    public void doSomething() {
        doInternal(); // 自调用方法
    }

  	 @Transactional
    public void doInternal() {
        System.out.println("Doing internal work...");
    }
}

```

以上，和private是一回事，因为没办法走到代理服务，所以事务会失效。

3、final、static方法

由于AOP是通过创建代理对象来实现的，而无法对final方法进行子类化和覆盖，所以无法拦截这些方法。

还有就是调用static方法，因为这类方法是属于这个类的，并不是对象的，所以无法被AOP。

### @Transactional用的不对
<br />1、@Transactional 注解属性 propagation 设置错误

```
@Service
public class ExampleService {
    
    @Autowired
    private ExampleRepository repository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void methodA() {
        repository.someDatabaseOperation();
    }

    public void methodB() {
        repository.anotherDatabaseOperation();
    }
}


@Service
public class SomeService {
    
    @Autowired
    private ExampleService exampleService;

    @Transactional
    public void doSomething() {
        // 这里执行一些业务逻辑
        exampleService.methodA(); // 这个方法由于 NOT_SUPPORTED 属性，会在非事务下执行
        exampleService.methodB();
        // ...
    }
}

```

以上，如果事务发生回滚，则methodA并不会回滚。因为他的propagation事不支持事务，那么他就不会一起回滚。

[✅Spring的事务传播机制有哪些？](https://www.yuque.com/hollis666/fo22bm/ixgoek25ybmy7ws4?view=doc_embed)

<br />2、@Transactional 注解属性 rollbackFor 设置错误

```
public class MyService {
    @Transactional(rollbackFor = RuntimeException.class)
    private void doInternal() {
        System.out.println("Doing internal work...");
    }
}
```

以上，如果发生非RuntimeException，则事务不会回滚，那么就会导致事务失效。所以需要指定为`(rollbackFor = Exception.class)`


3、用错注解

有的时候，你排查了很久，发现都没问题，但是还是不生效，然后找别人来帮你看，他上来就看了一下你用的@Transactional，发现并不是Spring中的，而是其他什么地方的，比如 javax.transaction.Transactional  ，这样也会导致事务失效。

### 异常被捕获
<br />5、异常被catch捕获导致@Transactional失效

```
public class MyService {
		@Transactional
    public void doSomething() {
      	try{
					doInternal(); 
        }catch(Exception e){
        	logger.error(e);
        }
    }
}
```

因为异常被捕获，所以就没办法基于异常进行rollback了，所以事务会失效。


### 事务中用了多线程

@Transactional 的事务管理使用的是 ThreadLocal 机制来存储事务上下文，而** ThreadLocal 变量是线程隔离的**，即每个线程都有自己的事务上下文副本。因此，在多线程环境下，Spring 的声明式事务会失效，即新线程中的操作不会被包含在原有的事务中。

[✅Spring的事务在多线程下生效吗？为什么？](https://www.yuque.com/hollis666/fo22bm/qi1vgi3yg8l663yy?view=doc_embed)
### 数据库引擎不支持事务

这个好理解，如myisam，不支持的肯定就不行了。
