# 什么是循环依赖

```java
@Component
public class A {
    // A中注入了B
 @Autowired
 private B b;
}

@Component
public class B {
    // B中也注入了A
 @Autowired
 private A a;
}
```

当然，这是最常见的一种循环依赖，比较特殊的还有

```java
// 自己依赖自己
@Component
public class A {
    // A中注入了A
 @Autowired
 private A a;
}
```

虽然体现形式不一样，但是实际上都是同一个问题----->循环依赖

# 什么情况下循环依赖可以被处理

在回答这个问题之前首先要明确一点，Spring解决循环依赖是有前置条件的

1. 出现循环依赖的Bean必须要是单例
2. 依赖注入的方式不能全是构造器注入的方式（很多博客上说，只能解决setter方法的循环依赖，这是错误的）

其中第一点应该很好理解，第二点：不能全是构造器注入是什么意思呢？我们还是用代码说话

```java
@Component
public class A {
// @Autowired
// private B b;
 public A(B b) {

 }
}


@Component
public class B {

// @Autowired
// private A a;

 public B(A a){

 }
}
```

在上面的例子中，A中注入B的方式是通过构造器，B中注入A的方式也是通过构造器，这个时候循环依赖是无法被解决，如果你的项目中有两个这样相互依赖的Bean，在启动时就会报出以下错误：

```
Caused by: org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name 'a': Requested bean is currently in creation: Is there an unresolvable circular reference?
```

| 依赖情况               | 依赖注入方式                                       | 循环依赖是否被解决 |
| :--------------------- | :------------------------------------------------- | :----------------- |
| AB相互依赖（循环依赖） | 均采用setter方法注入                               | 是                 |
| AB相互依赖（循环依赖） | 均采用构造器注入                                   | 否                 |
| AB相互依赖（循环依赖） | A中注入B的方式为setter方法，B中注入A的方式为构造器 | 是                 |
| AB相互依赖（循环依赖） | B中注入A的方式为setter方法，A中注入B的方式为构造器 | 否                 |

从上面的测试结果我们可以看到，不是只有在setter方法注入的情况下循环依赖才能被解决，即使存在构造器注入的场景下，循环依赖依然被可以被正常处理掉。

那么到底是为什么呢？Spring到底是怎么处理的循环依赖呢？不要急，我们接着往下看

# Spring是如何解决的循环依赖？

关于循环依赖的解决方式应该要分两种情况来讨论

1. 简单的循环依赖（没有AOP）

2. 结合了AOP的循环依赖

   

## **简单的循环依赖（没有AOP）**

我们先来分析一个最简单的例子，就是上面提到的那个demo

```java
@Component
public class A {
    // A中注入了B
 @Autowired
 private B b;
}

@Component
public class B {
    // B中也注入了A
 @Autowired
 private A a;
}
```

通过上文我们已经知道了这种情况下的循环依赖是能够被解决的，那么具体的流程是什么呢？我们一步步分析

首先，我们要知道**Spring在创建Bean的时候默认是按照自然排序来进行创建的，所以第一步Spring会去创建A**。

与此同时，我们应该知道，Spring在创建Bean的过程中分为三步

1. 实例化，对应方法：`AbstractAutowireCapableBeanFactory`中的`createBeanInstance`方法,简单理解就是new了一个对象.

2. 属性注入，对应方法：`AbstractAutowireCapableBeanFactory`的`populateBean`方法,为实例化中new出来的对象填充属性

3. 初始化，对应方法：`AbstractAutowireCapableBeanFactory`的`initializeBean`,执行aware接口中的方法，初始化方法，完成`AOP`代理

   

基于上面的知识，我们开始解读整个循环依赖处理的过程，整个流程应该是以A的创建为起点，前文也说了，第一步就是创建A嘛！

![](A:\gitdir\学习资料\java\java框架\spring\img\4.jpg)

创建A的过程实际上就是调用`getBean`方法，这个方法有两层含义

1. 创建一个新的Bean
2. 从缓存中获取到已经被创建的对象

我们现在分析的是第一层含义，因为这个时候缓存中还没有A嘛！



### 调用getSingleton(beanName)

首先调用`getSingleton(a)`方法，这个方法又会调用`getSingleton(beanName, true)`，在上图中我省略了这一步

```java
public Object getSingleton(String beanName) {
    return getSingleton(beanName, true);
}
```

`getSingleton(beanName, true)`这个方法实际上就是到缓存中尝试去获取Bean，整个缓存分为三级

1. `singletonObjects`，一级缓存，存储的是所有创建好了的单例Bean
2. `earlySingletonObjects`，完成实例化，但是还未进行属性注入及初始化的对象
3. `singletonFactories`，提前暴露的一个单例工厂，二级缓存中存储的就是从这个工厂中获取到的对象

因为A是第一次被创建，所以不管哪个缓存中必然都是没有的，因此会进入`getSingleton`的另外一个重载方法`getSingleton(beanName, singletonFactory)`。

### 调用getSingleton(beanName, singletonFactory)

这个方法就是用来创建Bean的，其源码如下：

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name must not be null");
    synchronized (this.singletonObjects) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null) {

            // ....
            // 省略异常处理及日志
            // ....

            // 在单例对象创建前先做一个标记
            // 将beanName放入到singletonsCurrentlyInCreation这个集合中
            // 标志着这个单例Bean正在创建
            // 如果同一个单例Bean多次被创建，这里会抛出异常
            beforeSingletonCreation(beanName);
            boolean newSingleton = false;
            boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
            if (recordSuppressedExceptions) {
                this.suppressedExceptions = new LinkedHashSet<>();
            }
            try {
                // 上游传入的lambda在这里会被执行，调用createBean方法创建一个Bean后返回
                singletonObject = singletonFactory.getObject();
                newSingleton = true;
            }
            // ...
            // 省略catch异常处理
            // ...
            finally {
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = null;
                }
                // 创建完成后将对应的beanName从singletonsCurrentlyInCreation移除
                afterSingletonCreation(beanName);
            }
            if (newSingleton) {
                // 添加到一级缓存singletonObjects中
                addSingleton(beanName, singletonObject);
            }
        }
        return singletonObject;
    }
}
```

上面的代码我们主要抓住一点，通过`createBean`方法返回的Bean最终被放到了一级缓存，也就是单例池中。

那么到这里我们可以得出一个结论：**一级缓存中存储的是已经完全创建好了的单例Bean**

### 调用addSingletonFactory方法

如下图所示：

![](\img\5.jpg)

在完成Bean的实例化后，属性注入之前Spring将Bean包装成一个工厂添加进了三级缓存中，对应源码如下：

```java
// 这里传入的参数也是一个lambda表达式，() -> getEarlyBeanReference(beanName, mbd, bean)
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized (this.singletonObjects) {
        if (!this.singletonObjects.containsKey(beanName)) {
            // 添加到三级缓存中
            this.singletonFactories.put(beanName, singletonFactory);
            this.earlySingletonObjects.remove(beanName);
            this.registeredSingletons.add(beanName);
        }
    }
}
```

这里只是添加了一个工厂，通过这个工厂（`ObjectFactory`）的`getObject`方法可以得到一个对象，而这个对象实际上就是通过`getEarlyBeanReference`这个方法创建的。那么，什么时候会去调用这个工厂的`getObject`方法呢？这个时候就要到创建B的流程了。

当A完成了实例化并添加进了三级缓存后，就要开始为A进行属性注入了，在注入时发现A依赖了B，那么这个时候Spring又会去`getBean(b)`，然后反射调用setter方法完成属性注入。

![](A:\gitdir\学习资料\java\java框架\spring\img\6.png)

因为B需要注入A，所以在创建B的时候，又会去调用`getBean(a)`，这个时候就又回到之前的流程了，但是不同的是，之前的`getBean`是为了创建Bean，而此时再调用`getBean`不是为了创建了，而是要从缓存中获取，因为之前A在实例化后已经将其放入了三级缓存`singletonFactories`中，所以此时`getBean(a)`的流程就是这样子了

![](A:\gitdir\学习资料\java\java框架\spring\img\7.png)

从这里我们可以看出，注入到B中的A是通过`getEarlyBeanReference`方法提前暴露出去的一个对象，还不是一个完整的Bean，那么`getEarlyBeanReference`到底干了啥了，我们看下它的源码

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
}
```

它实际上就是调用了后置处理器的`getEarlyBeanReference`，而真正实现了这个方法的后置处理器只有一个，就是通过`@EnableAspectJAutoProxy`注解导入的`AnnotationAwareAspectJAutoProxyCreator`。**也就是说如果在不考虑****AOP的情况下，上面的代码等价于：**

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    return exposedObject;
}
```

**也就是说这个工厂啥都没干，直接将实例化阶段创建的对象返回了！所以说在不考虑****AOP的情况下三级缓存有用嘛？讲道理，真的没什么用**，我直接将这个对象放到二级缓存中不是一点问题都没有吗？如果你说它提高了效率，那你告诉我提高的效率在哪?

那么三级缓存到底有什么作用呢？不要急，我们先把整个流程走完，在下文结合`AOP`分析循环依赖的时候你就能体会到三级缓存的作用！

到这里不知道小伙伴们会不会有疑问，B中提前注入了一个没有经过初始化的A类型对象不会有问题吗？

答：不会

