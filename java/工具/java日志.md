# Java - 日志

# 一、日志门面

当我们的系统变的更加复杂的时候，我们的日志就容易发生混乱。随着系统开发的进行，可能会更新不同的日志框架，造成当前系统中存在不同的日志依赖，让我们难以统一的管理和控制。就算我们强制要求所有的模块使用相同的日志框架，系统中也难以避免使用其他类似spring,mybatis等其他的第三方框架，它们依赖于我们规定不同的日志框架，而且他们自身的日志系统就有着不一致性，依然会出来日志体系的混乱。

所以我们需要借鉴JDBC的思想，为日志系统也提供一套门面，那么我们就可以面向这些接口规范来开发，避免了直接依赖具体的日志框架。这样我们的系统在日志中，就存在了日志的门面和日志的实现。

**常见的日志门面 ：**
JCL、`slf4j`

**常见的日志实现：**
JUL、log4j、`logback`、`log4j2`

日志门面和日志实现的关系：

![image-20210721234557769](assert/image-20210721234557769.png)

**日志框架出现的历史顺序：**
log4j -->JUL–>JCL–> slf4j --> logback --> log4j2



# 二、 SLF4J的使用

简单日志门面(Simple Logging Facade For Java) SLF4J主要是为了给Java日志访问提供一套标准、规范的API框架，其主要意义在于提供接口，具体的实现可以交由其他日志框架，例如log4j和logback等。

当然slf4j自己也提供了功能较为简单的实现，但是一般很少用到。对于一般的Java项目而言，日志框架会选择slf4j-api作为门面，配上具体的实现框架（log4j、logback等），中间使用桥接器完成桥接。

官方网站： https://www.slf4j.org/

SLF4J是目前市面上最流行的日志门面。现在的项目中，基本上都是使用SLF4J作为我们的日志系统。

SLF4J日志门面主要提供两大功能：

日志框架的绑定
日志框架的桥接



## 2.1 SLF4J入门

1. 添加依赖

   ```
   dependencies {
       // slf4j日志门面
       // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
       compile group: 'org.slf4j', name: 'slf4j-api', version: '1.7.30'
   
       // slf4j内置的简单日志实现
       // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
       testCompile group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.30'
   
       // https://mvnrepository.com/artifact/junit/junit
       testCompile group: 'junit', name: 'junit', version: '4.13'
   }
   ```

2. 编写代码

   ```java
   package top.onefine;
   
   import org.junit.Test;
   import org.slf4j.Logger;
   import org.slf4j.LoggerFactory;
   
   public class Slf4jTest {
   
       // 声明日志对象
       public static final Logger LOGGER = LoggerFactory.getLogger(Slf4jTest.class);
   
       @Test
       public void testQuick() {
           // 打印日志信息
           LOGGER.error("error");
           LOGGER.warn("warn");
           LOGGER.info("info");  // 默认级别
           LOGGER.debug("debug");
           LOGGER.trace("trace");
   
           // 使用占位符输出日志信息
           String name = "one fine";
           Integer age = 18;
           LOGGER.info("用户：{}， 年龄：{}", name, age);
   
           // 将系统的日常信息输出
           try {
               int i = 1 / 0;
           } catch (Exception e) {
   //            e.printStackTrace();
               LOGGER.error("出现异常", e);
           }
       }
   }
   
   ```

   

输出

```
[Test worker] ERROR top.onefine.Slf4jTest - error
[Test worker] WARN top.onefine.Slf4jTest - warn
[Test worker] INFO top.onefine.Slf4jTest - info
[Test worker] INFO top.onefine.Slf4jTest - 用户：one fine， 年龄：18
[Test worker] ERROR top.onefine.Slf4jTest - 出现异常
java.lang.ArithmeticException: / by zero
	at top.onefine.Slf4jTest.testQuick(Slf4jTest.java:28)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.runTestClass(JUnitTestClassExecutor.java:110)
	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:58)
	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:38)
	at org.gradle.api.internal.tasks.testing.junit.AbstractJUnitTestClassProcessor.processTestClass(AbstractJUnitTestClassProcessor.java:62)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.processTestClass(SuiteTestClassProcessor.java:51)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:33)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:94)
	at com.sun.proxy.$Proxy2.processTestClass(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.processTestClass(TestWorker.java:118)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:182)
	at org.gradle.internal.remote.internal.hub.MessageHubBackedObjectConnection$DispatchWrapper.dispatch(MessageHubBackedObjectConnection.java:164)
	at org.gradle.internal.remote.internal.hub.MessageHub$Handler.run(MessageHub.java:413)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:64)
	at org.gradle.internal.concurrent.ManagedExecutorImpl$1.run(ManagedExecutorImpl.java:48)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at org.gradle.internal.concurrent.ThreadFactoryImpl$ManagedThreadRunnable.run(ThreadFactoryImpl.java:56)
	at java.lang.Thread.run(Thread.java:748)

```

**为什么要使用SLF4J作为日志门面？**

```
* 1. 使用SLF4J框架，可以在部署时迁移到所需的日志记录框架。

* 2. SLF4J提供了对所有流行的日志框架的绑定，例如log4j，JUL，Simple logging和NOP。因此可以在部署时切换到任何这些流行的框架。

* 3. 无论使用哪种绑定，SLF4J都支持参数化日志记录消息。由于SLF4J将应用程序和日志记录框架分离，因此可以轻松编写独立于日志记录框架的应用程序。而无需担心用于编写应用程序的日志记录框架。

* 4. SLF4J提供了一个简单的Java工具，称为迁移器。使用此工具，可以迁移现有项目，这些项目使用日志框架(如Jakarta Commons Logging(JCL)或log4j或Java.util.logging(JUL))到SLF4J。

```



## 2.2 绑定日志的实现（Binding）

如前所述，SLF4J支持各种日志框架。SLF4J发行版附带了几个称为“SLF4J绑定”的jar文件，每个绑定对应一个受支持的框架。

### 使用slf4j的日志绑定流程:

1.添加slf4j-api的依赖
2.使用slf4j的API在项目中进行统一的日志记录
3.绑定具体的日志实现框架
	-绑定已经实现了slf4j的日志框架,直接添加对应依赖
	-绑定没有实现slf4j的日志框架,先添加日志的适配器,再添加实现类的依赖
4.slf4j有且仅有一个日志实现框架的绑定（如果出现多个默认使用第一个依赖日志实现）

**通过maven引入常见的日志实现框架：**

```
dependencies {
    // slf4j日志门面
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    compile group: 'org.slf4j', name: 'slf4j-api', version: '1.7.30'

//    // slf4j内置的简单日志实现
//    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
//    testCompile group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.30'

//    // logback 日志实现
//    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
//    testCompile group: 'ch.qos.logback', name: 'logback-classic', version: '1.2.3'

//    // nop 日志开关
//    // https://mvnrepository.com/artifact/org.slf4j/slf4j-nop
//    testCompile group: 'org.slf4j', name: 'slf4j-nop', version: '1.7.30'

//    // 绑定log4j日志实现需要的适配器
//    // https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12
//    testCompile group: 'org.slf4j', name: 'slf4j-log4j12', version: '1.7.30'
//    // https://mvnrepository.com/artifact/log4j/log4j
//    compile group: 'log4j', name: 'log4j', version: '1.2.17'

//    // 绑定jul的日志实现框架
//    // https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14
//    testCompile group: 'org.slf4j', name: 'slf4j-jdk14', version: '1.7.30'

    // jcl
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-jcl
    testCompile group: 'org.slf4j', name: 'slf4j-jcl', version: '1.7.30'

    // https://mvnrepository.com/artifact/junit/junit
    testCompile group: 'junit', name: 'junit', version: '4.13'
}

```



要切换日志框架，只需替换类路径上的slf4j绑定。例如，要从java.util.logging切换到log4j，只需将slf4j-jdk14-1.7.27.jar替换为slf4j-log4j12-1.7.27.jar即可。

SLF4J不依赖于任何特殊的类装载。实际上，每个SLF4J绑定在编译时都是硬连线的， 以使用一个且只有一个特定的日志记录框架。例如，slf4j-log4j12-1.7.27.jar绑定在编译时绑定以使用log4j。在您的代码中，除了slf4j-api-1.7.27.jar之外，您只需将您选择的一个且只有一个绑定放到相应的类路径位置。不要在类路径上放置多个绑定。以下是一般概念的图解说明。


![image-20210722000316614](assert/image-20210722000316614.png)



## 2.3 桥接旧的日志框架（Bridging）

通常，您依赖的某些组件依赖于SLF4J以外的日志记录API。您也可以假设这些组件在不久的将来不会切换到SLF4J。为了解决这种情况，SLF4J附带了几个桥接模块，这些模块将对log4j，JCL和java.util.logging API的调用重定向，就好像它们是对SLF4J API一样。

桥接解决的是项目中日志的遗留问题，当系统中存在之前的日志API，可以通过桥接转换到slf4j的实现

1.先去除之前老的日志框架的依赖
2.添加SLF4J提供的桥接组件
3.为项目添加SLF4J的具体实现

![image-20210722000515402](assert/image-20210722000515402.png)

**迁移的方式：**
如果我们要使用SLF4J的桥接器，替换原有的日志框架，那么我们需要做的第一件事情，就是删除掉原有项目中的日志框架的依赖。然后替换成SLF4J提供的桥接器。

引入

```
dependencies {
//    // slf4j日志门面
//    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
//    compile group: 'org.slf4j', name: 'slf4j-api', version: '1.7.30'

    // https://mvnrepository.com/artifact/log4j/log4j
    compile group: 'log4j', name: 'log4j', version: '1.2.17'

    // https://mvnrepository.com/artifact/junit/junit
    testCompile group: 'junit', name: 'junit', version: '4.13'
}

```

```
package top.onefine.test;

import org.apache.log4j.Logger;
import org.junit.Test;

public class Log4jTest {

    // 定义log4j日志记录器
    public static final Logger LOGGER = Logger.getLogger(Log4jTest.class);

    // 测试桥接器
    @Test
    public void test() {
        LOGGER.info("hello log4j");
    }
}

```

输出：【注`log4j.appender.console.layout.conversionPattern = %d [%t] %-5p [%c] - %m%n`】

```

dependencies {
    // slf4j日志门面
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    compile group: 'org.slf4j', name: 'slf4j-api', version: '1.7.30'

    // logback 日志实现
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    testCompile group: 'ch.qos.logback', name: 'logback-classic', version: '1.2.3'

//    // https://mvnrepository.com/artifact/log4j/log4j
//    compile group: 'log4j', name: 'log4j', version: '1.2.17'
    // log4j的桥接器
    // https://mvnrepository.com/artifact/org.slf4j/log4j-over-slf4j
    compile group: 'org.slf4j', name: 'log4j-over-slf4j', version: '1.7.30'

    // https://mvnrepository.com/artifact/junit/junit
    testCompile group: 'junit', name: 'junit', version: '4.13'
}

```

![在这里插入图片描述](assert/20200519143102495.png)




### 2.4 SLF4J原理解析

1.SLF4J通过LoggerFactory加载日志具体的实现对象。
2.LoggerFactory在初始化的过程中，会通过performInitialization()方法绑定具体的日志实现。
3.在绑定具体实现的时候，通过类加载器，加载org/slf4j/impl/StaticLoggerBinder.class
4.所以，只要是一个日志实现框架，在org.slf4j.impl包中提供一个自己的StaticLoggerBinder类，在其中提供具体日志实现的LoggerFactory就可以被SLF4J所加载

# 三、Logback的使用 

todo