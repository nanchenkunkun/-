# 典型回答

XXL-JOB 作为一个定时任务调度工具，他需要确保同一时间内同一任务只会在一个执行器上执行。这个特性对于避免任务的重复执行非常关键，特别是在分布式环境中，多个执行器实例可能同时运行相同的任务。

这个特性被XXL-JOB描述为“调度一致性”，并且官方文档也给出了这个问题的答案：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1710564674184-dfedd5ba-42ca-47b3-bd17-320fffdf56b1.png#averageHue=%23f2efed&clientId=ud06dea0f-c7f0-4&from=paste&height=88&id=u5f9327b4&originHeight=88&originWidth=1257&originalType=binary&ratio=1&rotation=0&showTitle=false&size=21710&status=done&style=none&taskId=u793aa55d-99fc-4402-98d2-73081f983db&title=&width=1257)

“调度中心”通过DB锁保证集群分布式调度的一致性, 一次任务调度只会触发一次执行；

调度中心在XXL-JOB中负责管理所有任务的调度，它知道哪些任务需要执行，以及任务的调度配置（如CRON表达式）。当到达指定的执行时间点，调度中心会选择一个执行器实例来执行任务。

**调度相关的JobScheduleHelper是XXL-JOB中的一个核心组件，负责协调任务的调度逻辑**，确保任务触发的正确性和唯一性。

通过查看[JobScheduleHelper](https://github.com/xuxueli/xxl-job/blob/master/xxl-job-admin/src/main/java/com/xxl/job/admin/core/thread/JobScheduleHelper.java)的源码，在他的scheduleThread的方法中，我们可以看到以下代码

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1710564986617-bbb1962c-bb28-491c-9f7e-368853e8152b.png#averageHue=%23fefefe&clientId=ud06dea0f-c7f0-4&from=paste&height=255&id=u6eed7107&originHeight=255&originWidth=1300&originalType=binary&ratio=1&rotation=0&showTitle=false&size=26799&status=done&style=none&taskId=u49d55ff8-64e5-4a21-9d05-f6fa5c0ff0b&title=&width=1300)

这里面的`select * from xxl_job_lock where lock_name = 'schedule_lock' for update`是关键，这明显是一个基于数据的悲观锁实现的一个加锁过程。

>  xxl_job_lock是XXL-JOB的一张表，是一张任务调度锁表；在使用XXL-JOB的时候需要提前创建好这张表。并且需要提前插入一条记录：INSERT INTO `xxl_job_lock` ( `lock_name`) VALUES ( 'schedule_lock');
> ![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1710565454303-ac6840b6-2f6a-4840-a16b-7e23c5494210.png#averageHue=%23fefdfd&clientId=ud06dea0f-c7f0-4&from=paste&height=352&id=udd797597&originHeight=352&originWidth=1005&originalType=binary&ratio=1&rotation=0&showTitle=false&size=40359&status=done&style=none&taskId=uc5822bf7-6c04-4768-b653-5b98d7c7ba4&title=&width=1005)
> 来自 tables_xxl_job.sql


通过`select for update`的方式添加一个悲观锁，可以确保在同一时刻，只能有一个事务获取到锁。这样获取到锁的线程就可以执行任务的调度了。

[✅乐观锁与悲观锁如何实现？](https://www.yuque.com/hollis666/fo22bm/ionc18?view=doc_embed)

并且这个锁会随着事务的存在一直存在，这个事务最最终是在方法的finally中实现的：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1710565086585-609ec2ef-0176-43ad-b484-7fb170a08c3a.png#averageHue=%23fefefe&clientId=ud06dea0f-c7f0-4&from=paste&height=794&id=ua0da21f3&originHeight=794&originWidth=680&originalType=binary&ratio=1&rotation=0&showTitle=false&size=43808&status=done&style=none&taskId=udd399db7-319a-4a83-b591-97c585fe152&title=&width=680)


