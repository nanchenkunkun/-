### 问题发现

在一个内部平台中，系统提供了一个功能，允许管理员导出报表数据为Excel文件。随着平台用户量的增长和数据量的积累，管理员开始报告导出操作时系统频繁崩溃。

监控系统显示，在执行导出操作期间，应用服务器的内存使用量急剧上升，最终导致Java虚拟机抛出 OutOfMemoryError 异常。

### 问题排查

首先，我们需要确认是哪部分代码或对象导致了内存溢出。这可以通过分析堆转储（Heap Dump）来实现。当 OutOfMemoryError 发生时，可以配置JVM自动生成堆Dump文件，或者使用诸如 jmap 工具手动生成堆Dump。

```
-XX:+HeapDumpOnOutOfMemoryError

配置OOM时自动生成Dump文件
```

```
jmap -dump:live,format=b,file=heapdump.hprof <pid>
```

这里面建议在GC前和GC后分别获取Dump，如果只获取一次，一定要知道是GC前的还是GC后的。不然很容易分析错误。

接下来使用堆分析工具MAT加载和分析 heapdump.hprof 文件。通过分析，我们发现存在大量的 org.apache.poi.xssf.usermodel.XSSFWorkbook 实例占用了大部分堆内存。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1700378903070-68af4a3c-9227-4f46-8fc4-383639dfcd6a.png#averageHue=%23dee2e5&clientId=u102f89ce-5f21-4&from=paste&height=1248&id=uba718850&originHeight=1248&originWidth=1666&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1197855&status=done&style=none&taskId=u2cc18e13-eeb5-4180-aaa1-9250bdf51ca&title=&width=1666)<br />_（当时的分析截图未保存，这个截图是我从网上找的，几乎和我们当时现象一模一样）_

定位到使用 Apache POI 的代码部分，发现系统在处理大量数据时，是将所有数据一次性加载到 XSSFWorkbook 对象中的。

我们的业务代码中确实有使用XSSF的部分代码如下：

```
public ByteArrayOutputStream exportData(List<Data> dataList) {
    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet sheet = workbook.createSheet("Data");
  
    int rownum = 0;
    for (Data data : dataList) {
        Row row = sheet.createRow(rownum++);
        // ... 填充数据行 ...
    }
  
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    return out;
}

```

### 问题解决

问题定位到了，解决起来就简单了。根据下文：

[✅POI的如何做大文件的写入](https://www.yuque.com/hollis666/fo22bm/kalmkdx5fukxt13q?view=doc_embed)

[✅为啥SXSSFWorkbook占用内存更小?](https://www.yuque.com/hollis666/fo22bm/ivczis4gyskog9q2?view=doc_embed)

我们知道，可以使用SXSSF来解决这个问题，因为SXSSFWorkbook 这种方式将数据写入临时文件，而不是保留在内存中。

```
public ByteArrayOutputStream exportData(List<Data> dataList) {
		// 使用SXSSFWorkbook，并保留100行数据在内存中，其余写入磁盘
    SXSSFWorkbook workbook = new SXSSFWorkbook(100); 
    Sheet sheet = workbook.createSheet("Data");

    int rownum = 0;
    for (Data data : dataList) {
        Row row = sheet.createRow(rownum++);
        // ... 填充数据行 ...
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    out.close();

    // 清除临时文件
    workbook.dispose();
    return out;
}

```

### 后续改进

通过优化后，解决了内存溢出的问题，但是随着文件越来越大，会存在导出实践过长的问题，可以考虑用异步在后台生成报表的方式。这里就不展开了。

可以参考：

[✅基于EasyExcel+线程池解决POI文件导出时的内存溢出及超时问题](https://www.yuque.com/hollis666/fo22bm/wcm6xqvp0z004ing?view=doc_embed)
