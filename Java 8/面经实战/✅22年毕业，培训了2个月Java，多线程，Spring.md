# 面试者背景

:::warning
22年毕业，培训了Java 2个月，双非二本，非科班<br />怎么理解面向对象？什么是多态？Java是值传递还是引用传递？值传递<br />AQS介绍一下？state是什么类型，state的修改是怎么改的？state=1表示什么？<br />AQS有哪些具体实现？reentrantLock和synchronized区别是啥？<br />公平锁和非公平锁区别是啥？公平锁的缺点？reentrantLock是如何实现公平锁和非公平锁的？<br />你觉得用synchronized和reentrant哪个好？synchronized锁的是什么？锁对象和锁类有区别吗？<br />同一个类中有两个synchronized方法，能被两个线程同时执行吗？同一个线程可以吗？重入怎么判断？为啥要做成可重入？死锁了解么？什么情况会发生死锁？如何解决死锁的问题？<br />如何创建一个线程？子线程抛异常，主线程能捕获到吗？start和run区别是啥<br />主子线程的执行顺序是怎么样的？守护线程知道吗？<br />实现功能：统计Spring的某个bean中的某个方法被调用多少次？如果不让改动bean的类呢？<br />用过spring中的哪些注解？@Service、@Compotent区别是啥？<br />同一个接口有多个实现，如何指定该注入哪个实现？<br />SpringEvent用过吗？IOC控制反转是啥意思？把什么反转了，谁反转给谁了？<br />Spring有几种注入方式？平常用哪个？字段注入IDEA有没有给警告？<br />Spring和SpringBoot最大的区别是啥？简化开发，内置web服务器？内置了哪些web服务器？<br />讲讲Spring的循环依赖问题？二级缓存就够了？<br />Mysql主键一定是自增的吗？建表一定要有主键吗？隐藏row_id干嘛用的？聚簇索引<br />如果没有创建主键，一定会创建隐藏主键吗？如果有这时候有唯一键一定就不会了吗？<br />主键用自增ID和UUID哪个好？自增ID有缺点吗？<br />分库分表字段怎么选的，雪花算法？按照ID分表？查询怎么办？<br />日志输出用的什么框架，slf4j只是个门面，日志怎么输出的？<br />Maven出现了jar冲突，该怎么解决？删一个，
:::
# 题目解析

> **怎么理解面向对象？什么是多态？Java是值传递还是引用传递？值传递**


[✅如何理解面向对象和面向过程？](https://www.yuque.com/hollis666/fo22bm/sy3eyr?view=doc_embed)

[✅Java是值传递还是引用传递？](https://www.yuque.com/hollis666/fo22bm/lbdoqe?view=doc_embed)

> **AQS介绍一下？state是什么类型，state的修改是怎么改的？state=1表示什么？**
> **AQS有哪些具体实现？**


[✅如何理解AQS？](https://www.yuque.com/hollis666/fo22bm/qka9yt?view=doc_embed)

> **reentrantLock和synchronized区别是啥？**
> **公平锁和非公平锁区别是啥？公平锁的缺点？reentrantLock是如何实现公平锁和非公平锁的？**


[✅synchronized和reentrantLock区别？](https://www.yuque.com/hollis666/fo22bm/bitupp?view=doc_embed)

[✅公平锁和非公平锁的区别？](https://www.yuque.com/hollis666/fo22bm/bnt978?view=doc_embed)

> **你觉得用synchronized和reentrant哪个好？synchronized锁的是什么？锁对象和锁类有区别吗？**


[✅synchronized锁的是什么？](https://www.yuque.com/hollis666/fo22bm/xpwgigmu7xz4uvzn?view=doc_embed)

> **同一个类中有两个synchronized方法，能被两个线程同时执行吗？同一个线程可以吗？重入怎么判断？为啥要做成可重入？死锁了解么？什么情况会发生死锁？如何解决死锁的问题？**


[✅什么是可重入锁，怎么实现可重入锁？](https://www.yuque.com/hollis666/fo22bm/zvx2w5h9sr7trle7?view=doc_embed)

[✅什么是死锁，如何解决？](https://www.yuque.com/hollis666/fo22bm/mtdxsd?view=doc_embed)

> **如何创建一个线程？子线程抛异常，主线程能捕获到吗？start和run区别是啥**
> **主子线程的执行顺序是怎么样的？守护线程知道吗？**


[✅创建线程有几种方式？](https://www.yuque.com/hollis666/fo22bm/gmgb2a?view=doc_embed)

[✅run/start、wait/sleep、notify/notifyAll区别?](https://www.yuque.com/hollis666/fo22bm/bw9p42?view=doc_embed)

[✅为什么不能在try-catch中捕获子线程的异常?](https://www.yuque.com/hollis666/fo22bm/dtci5npzb1cidzxk?view=doc_embed)

[https://www.yuque.com/hollis666/fo22bm/dlg6vw](https://www.yuque.com/hollis666/fo22bm/dlg6vw)


> **实现功能：统计Spring的某个bean中的某个方法被调用多少次？如果不让改动bean的类呢？**
> **用过spring中的哪些注解？@Service、@Compotent区别是啥？**
> **同一个接口有多个实现，如何指定该注入哪个实现？**


[✅如何统计一个Bean中的方法调用次数](https://www.yuque.com/hollis666/fo22bm/mnnadn?view=doc_embed)

[✅Spring中@Service 、@Component、@Repository等注解区别是什么？](https://www.yuque.com/hollis666/fo22bm/twxw1ws403puq2zl?view=doc_embed)

@Qualifier注解

> **SpringEvent用过吗？IOC控制反转是啥意思？把什么反转了，谁反转给谁了？**
> **Spring有几种注入方式？平常用哪个？字段注入IDEA有没有给警告？**


[✅介绍一下Spring的IOC](https://www.yuque.com/hollis666/fo22bm/wswp59?view=doc_embed)

[✅为什么Spring不建议使用基于字段的依赖注入？](https://www.yuque.com/hollis666/fo22bm/lbst9ffoy74od6kr?view=doc_embed)

> **Spring和SpringBoot最大的区别是啥？简化开发，内置web服务器？内置了哪些web服务器？**


[✅SpringBoot和Spring的区别是什么？](https://www.yuque.com/hollis666/fo22bm/meyfwphs8t6c3znd?view=doc_embed)

> **讲讲Spring的循环依赖问题？二级缓存就够了？**


[✅三级缓存是如何解决循环依赖的问题的？](https://www.yuque.com/hollis666/fo22bm/ffk7dlcrwk35glpl?view=doc_embed)

[✅Spring解决循环依赖一定需要三级缓存吗？](https://www.yuque.com/hollis666/fo22bm/edvhrik3pbw300os?view=doc_embed)

> **Mysql主键一定是自增的吗？建表一定要有主键吗？隐藏row_id干嘛用的？聚簇索引**
> **如果没有创建主键，一定会创建隐藏主键吗？如果有这时候有唯一键一定就不会了吗？**


[✅MySQL的主键一定是自增的吗？](https://www.yuque.com/hollis666/fo22bm/glycgnryk8953c24?view=doc_embed)

> **主键用自增ID和UUID哪个好？自增ID有缺点吗？**


[✅uuid和自增id做主键哪个好，为什么？](https://www.yuque.com/hollis666/fo22bm/uted9tvkngs62pmu?view=doc_embed)

> **分库分表字段怎么选的，雪花算法？按照ID分表？查询怎么办？**


[✅分表字段如何选择？](https://www.yuque.com/hollis666/fo22bm/mec4ust5rpfob78r?view=doc_embed)

> **日志输出用的什么框架，slf4j只是个门面，日志怎么输出的？**
> **Maven出现了jar冲突，该怎么解决？删一个，**


[✅为什么不能直接使用Log4j、Logback中的 API？](https://www.yuque.com/hollis666/fo22bm/tai6nceh3x9qf8m6?view=doc_embed)

[✅Maven如何解决jar包冲突的问题？](https://www.yuque.com/hollis666/fo22bm/vkkiva?view=doc_embed)
