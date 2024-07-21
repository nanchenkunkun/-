Dump是Java虚拟机的**运行时快照**。将Java虚拟机运行时的状态和信息保存到文件。

**线程Dump**，包含所有线程的运行状态。纯文本格式。

**堆Dump**，包含线程Dump，并包含所有堆对象的状态。二进制格式。

#### Java Dump有什么用？

补足传统Bug分析手段的不足: 可在任何Java环境使用;信息量充足。 针对非功能正确性的Bug,主要为:多线程幵发、内存泄漏。

# 扩展知识
### 如何制作Java Dump
#### 使用Java虚拟机制作Dump

指示虚拟机在发生内存不足错误时,自动生成堆Dump

```
-XX:+HeapDumpOnOutOfMemoryError
```

#### 使用图形化工具制作Dump
使用JDK(1.6)自带的工具:Java VisualVM。
#### 使用命令行制作Dump
jstack:打印线程的栈信息,制作线程Dump。<br />jmap:打印内存映射,制作堆Dump。

