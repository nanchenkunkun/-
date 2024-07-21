# 典型回答

通过AOP即可实现，通过AOP对Bean进行代理，在每次执行方法前或者后进行几次计数统计。这个主要就是考虑好如何避免并发情况下不准，以及如何使用AOP实现代理。

主要的代码如下：

首先我们先自定义一个注解，有了这个注解之后，我们可以在想要统计的方法上加上这个注解：

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
* @author Hollis
**/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodCallCount {
    
}
```

接下来定义一个切面，来对这个注解进行增强处理：

```java
@Aspect
@Component
public class MethodCallCounterAspect {

	private Map<String, AtomicInteger> methodCallCountMap = new ConcurrentHashMap<>();

    @Around("@annotation(com.hollis.chuang.java.bagu.MethodCallCount)")
    public Object facade(ProceedingJoinPoint pjp) throws Exception {

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String methodName = method.getName();

        try{
            return pjp.proceed();
        } catch (Throwable e) {
            //异常处理
        } finally{
            //计数+1
            AtomicInteger counter = methodCallCountMap.computeIfAbsent(methodName, k -> new AtomicInteger(0));
            counter.incrementAndGet();
        }
    }

    public int getMethodCallCount(String methodName) {
        AtomicInteger counter = methodCallCountMap.get(methodName);
        return (counter != null) ? counter.get() : 0;
    }
}
```


有了以上注解和切面后，只需要在我们想要统计的方法上使用该注解就行了：

```java
@Service
public class HollisTestServiceImpl implements HollisTestService {

 
    @MethodCallCount
    @Override
    public HollisTestResponse test(HollisTestRequest hollisTestRequest) {
        
    }
```


以上，当test方法被调用时，就会自动统计调用次数。

但是需要注意的是，这个统计结果只在内存中有效，如果应用发生重启，就会归零了。如果想要持久化保存，就需要考虑持久化存储了，如存在mysql或者redis中。

另外，如果并发特别高，对统计结果要求没那么精确， 可以用LongAdder替代AtomicInteger
