# 典型回答

在Spring框架中，使用@PostConstruct、自定义的init-method方法，和InitializingBean接口和afterPropertiesSet方法都是用于在Bean初始化阶段执行特定方法的方式。他们的执行顺序是：**构造函数>@PostConstruct > afterPropertiesSet > init-method**

- **@PostConstruct** 是javax.annotation 包中的注解(Spring Boot 3.0之后jakarta.annotation中，用于在构造函数执行完毕并且依赖注入完成后执行特定的初始化方法。标注在方法上，表示这个方法将在Bean初始化阶段被调用。
- **init-method** 是在Spring配置文件（如XML文件）中配置的一种方式。通过在Bean的配置中指定 init-method 属性，可以告诉Spring在Bean初始化完成后调用指定的初始化方法。如果不使用xml文件，也可以使用 @Bean 注解的 initMethod 属性来指定初始化方法。（下面的例子就是用的这种方式）
- **afterPropertiesSet** 是 Spring 的 InitializingBean 接口中的方法。如果一个 Bean 实现了 InitializingBean 接口，Spring 在初始化阶段会调用该接口的 afterPropertiesSet 方法。


Talk is Cheap，Show me the Code ，如下是一个示例：

```java
import jakarta.annotation.PostConstruct;
//因为我用的是Spring Boot 3.0.如果是2.0需要改成javax.annotation.PostConstruct
import org.springframework.beans.factory.InitializingBean;

public class ExampleBean implements InitializingBean {

    private String message;

    public ExampleBean() {
        System.out.println("构造函数执行");
    }
    
    @PostConstruct
    public void postConstructMethod() {
        System.out.println("@PostConstruct执行");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet执行");
    }

    public void customInitMethod() {
        System.out.println("init-method执行");
    }
}

```

```java
package cn.hollis;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ExampleConfig {

    @Bean(initMethod = "customInitMethod")
    public ExampleBean exampleBean(){
        return new ExampleBean();
    }
}

```

启动Spring：
```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.hollis")
public class TestMain {

    public static void main(String[] args) {
        SpringApplication.run(TestMain.class, args);
    }

}

```

输出结果：·

```java
构造函数执行
@PostConstruct执行
afterPropertiesSet执行
init-method执行
```

所以，他们的执行顺序就是**构造函数>@PostConstruct > afterPropertiesSet > init-method**

# 扩展知识

## 顺序背后的原理

这个执行顺序，难道要死记硬背吗？那就太low了，其实这个执行顺序，如果大家看过下面这篇之后，就会理所应当的知道了。

[✅Spring Bean的初始化过程是怎么样的？](https://www.yuque.com/hollis666/fo22bm/zlvhpz?view=doc_embed)

在Bean的初始化过程中，在initializeBean方法中，会调用invokeInitMethods方法：


```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    invokeAwareMethods(beanName, bean);

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                (mbd != null ? mbd.getResourceDescription() : null), beanName, ex.getMessage(), ex);
    }
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```


invokeInitMethods方法有两个if分支，第一个分支就是执行afterPropertiesSet方法的，第二个分支就是执行initMethod的，所以，一定是先执行afterPropertiesSet后执行initMethod。
```java
protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd)
        throws Throwable {

    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.hasAnyExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }
        ((InitializingBean) bean).afterPropertiesSet();
    }

    if (mbd != null && bean.getClass() != NullBean.class) {
        String[] initMethodNames = mbd.getInitMethodNames();
        if (initMethodNames != null) {
            for (String initMethodName : initMethodNames) {
                if (StringUtils.hasLength(initMethodName) &&
                        !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
                        !mbd.hasAnyExternallyManagedInitMethod(initMethodName)) {
                    invokeCustomInitMethod(beanName, bean, mbd, initMethodName);
                }
            }
        }
    }
}
```

那么，@PostConstruct的执行在哪个过程呢？

前面的initializeBean方法中，在调用invokeInitMethods之前，会调用applyBeanPostProcessorsBeforeInitialization，这个就是我们知道的调用BeanPostProcessor的前置处理方法。即调用postProcessBeforeInitialization

这里就需要登场一个InitDestroyAnnotationBeanPostProcessor了，看名字很容易知道，他就是在Bean初始化和销毁过程中的一个处理器。那么我们继续看postProcessBeforeInitialization方法：

```java
@Override
public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
    try {
        metadata.invokeInitMethods(bean, beanName);
    }
    catch (InvocationTargetException ex) {
        throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
    }
    return bean;
}

```

这有一段熟悉的代码了，那就是invokeInitMethods，好像和我们要找的方法有关，看看metadata是咋来的，就看findLifecycleMetadata：

```java
private LifecycleMetadata findLifecycleMetadata(Class<?> beanClass) {
    if (this.lifecycleMetadataCache == null) {
        // Happens after deserialization, during destruction...
        return buildLifecycleMetadata(beanClass);
    }
    // Quick check on the concurrent map first, with minimal locking.
    LifecycleMetadata metadata = this.lifecycleMetadataCache.get(beanClass);
    if (metadata == null) {
        synchronized (this.lifecycleMetadataCache) {
            metadata = this.lifecycleMetadataCache.get(beanClass);
            if (metadata == null) {
                metadata = buildLifecycleMetadata(beanClass);
                this.lifecycleMetadataCache.put(beanClass, metadata);
            }
            return metadata;
        }
    }
    return metadata;
}

```

看到这里面的核心逻辑就是buildLifecycleMetadata，继续看buildLifecycleMetadata方法：

```java
private LifecycleMetadata buildLifecycleMetadata(final Class<?> beanClass) {
    if (!AnnotationUtils.isCandidateClass(beanClass, this.initAnnotationTypes) &&
            !AnnotationUtils.isCandidateClass(beanClass, this.destroyAnnotationTypes)) {
        return this.emptyLifecycleMetadata;
    }

    List<LifecycleMethod> initMethods = new ArrayList<>();
    List<LifecycleMethod> destroyMethods = new ArrayList<>();
    Class<?> currentClass = beanClass;

    do {
        final List<LifecycleMethod> currInitMethods = new ArrayList<>();
        final List<LifecycleMethod> currDestroyMethods = new ArrayList<>();

        ReflectionUtils.doWithLocalMethods(currentClass, method -> {
            for (Class<? extends Annotation> initAnnotationType : this.initAnnotationTypes) {
                if (initAnnotationType != null && method.isAnnotationPresent(initAnnotationType)) {
                    currInitMethods.add(new LifecycleMethod(method, beanClass));
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found init method on class [" + beanClass.getName() + "]: " + method);
                    }
                }
            }
            for (Class<? extends Annotation> destroyAnnotationType : this.destroyAnnotationTypes) {
                if (destroyAnnotationType != null && method.isAnnotationPresent(destroyAnnotationType)) {
                    currDestroyMethods.add(new LifecycleMethod(method, beanClass));
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found destroy method on class [" + beanClass.getName() + "]: " + method);
                    }
                }
            }
        });

        initMethods.addAll(0, currInitMethods);
        destroyMethods.addAll(currDestroyMethods);
        currentClass = currentClass.getSuperclass();
    }
    while (currentClass != null && currentClass != Object.class);

    return (initMethods.isEmpty() && destroyMethods.isEmpty() ? this.emptyLifecycleMetadata :
            new LifecycleMetadata(beanClass, initMethods, destroyMethods));
}

```
 <br />这里其实就是在寻找这个bean中有没有哪些方法上有initAnnotationType注解，如果有的话，那么就把他们添加到initMethods中，返回给前面的方法执行。

这里的initAnnotationTypes是一个集合：

```java
private final Set<Class<? extends Annotation>> initAnnotationTypes = new LinkedHashSet<>(2);


public void addInitAnnotationType(@Nullable Class<? extends Annotation> initAnnotationType) {
    if (initAnnotationType != null) {
        this.initAnnotationTypes.add(initAnnotationType);
    }
}
```

看看他是从哪设置进来的就行了，直接找他在哪里被调用了，会发现在CommonAnnotationBeanPostProcessor中有调用：

```java
public CommonAnnotationBeanPostProcessor() {
    setOrder(Ordered.LOWEST_PRECEDENCE - 3);

    // Jakarta EE 9 set of annotations in jakarta.annotation package
    addInitAnnotationType(loadAnnotationType("jakarta.annotation.PostConstruct"));
    addDestroyAnnotationType(loadAnnotationType("jakarta.annotation.PreDestroy"));

    // Tolerate legacy JSR-250 annotations in javax.annotation package
    addInitAnnotationType(loadAnnotationType("javax.annotation.PostConstruct"));
    addDestroyAnnotationType(loadAnnotationType("javax.annotation.PreDestroy"));

    // java.naming module present on JDK 9+?
    if (jndiPresent) {
        this.jndiFactory = new SimpleJndiBeanFactory();
    }
}

```
 <br />因为我用的是Spring Boot 3.0，SpringBoot 2.0的代码长这样：
```java
public CommonAnnotationBeanPostProcessor() {
    setOrder(Ordered.LOWEST_PRECEDENCE - 3);
    setInitAnnotationType(PostConstruct.class);
    setDestroyAnnotationType(PreDestroy.class);
    ignoreResourceType("javax.xml.ws.WebServiceContext");
}
```

看到了吧，在这里设置了PostConstruct这个注解作为InitAnnotationType。

总结一下，在CommonAnnotationBeanPostProcessor初始化的时候，会把PostConstruct设置到initAnnotationTypes中，然后在InitDestroyAnnotationBeanPostProcessor的postProcessBeforeInitialization方法执行过程中，会检查这个Bean中有哪些initAnnotationType，把加了initAnnotationType注解的方法当做初始化方法进行调用。

所以，@PostConstruct 这个注解就在这个阶段被调用了。


