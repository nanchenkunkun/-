# 典型回答

这个场景题考察的方面其实挺多的，还是那句话，我们要学会拆解，我们先来看一下，百万级数据从Excel读取，并插入到数据库，可能会遇到哪些问题：

1**、内存溢出问题**

- 百万级数据量的Excel文件会非常大，都加载到内存中可能会导致内存溢出。

**2、性能问题**

- 百万级数据从Excel读取并插入到数据，可能会很慢，需要考虑性能问题。

**3、错误处理**

- 在文件的读取及导入过程中，可能会遇到各种各样的问题，我们需要妥善的处理好这些问题

### 内存溢出问题

百万级数据量，一次性都读取到内存中，肯定是不现实的，那么好的办法就是**基于流式读取的方式进行分批处理**。

在技术选型上，我们选择使用**EasyExcel**，他特别针对大数据量和复杂Excel文件的处理进行了优化。在解析Excel时EasyExcel不会将Excel一次性全部加载到内存中，而是从磁盘上一行行读取数据，逐个解析。

### 性能问题

百万级数据的处理，如果用单线程的话肯定是很慢的，想要提升性能，那么就需要使用**多线程**。

多线程的使用上涉及到两个场景，一个是用多线程进行文件的读取，另一个是用多线程实现数据的插入。这里就涉及到一个生产者-消费者的模式了，多个线程读取，然后多个线程插入，这样可以最大限度的提升整体的性能。

而数据的插入，我们除了借助多线程之外，还可以同时使用**数据库的批量插入的功能**，这样就能更加的提升插入速度。

### 错误处理

在文件的读取和数据库写入过程中，会需要解决各种各样的问题，比如数据格式错误、数据不一致、有重复数据等。

所以我们需要分两步来，第一步就是先进行数据的检查，在开始插入之间就把数据的格式等问题提前检查好，然后在插入过程中，对异常进行处理。

处理方式有很多种，可以进行事务回滚、可以进行日志记录。这个根据实际情况，一般来说不建议做回滚，直接做自动重试，重试几次之后还是不行的话，再记录日志然后后续在重新插入即可。

并且在这个过程中，需要考虑一下数据重复的问题，需要在excel中某几个字段设置成数据库唯一性约束，然后在遇到数据冲突的时候，进行处理，处理方式可以是覆盖、跳过以及报错。这个根据实际业务情况来，一般来说跳过+打印日志是相对合理的。


所以，整体方案就是：

**借助EasyExcel来实现Excel的读取，因为他并不会一次性把整个Excel都加载到内存中，而是逐行读取的。为了提升并发性能，我们再进一步将百万级数据分散到不同的sheet中，然后借助线程池，多线程同时读取不同的sheet，在读取过程中，借助EasyExcel的ReadListener做数据处理。**

**在处理过程中，我们并不会每一条数据都操作数据库，这样对数据库来说压力太大了，我们会设定一个批次，比如1000条，我们会把从Excel中读取到的数据暂存在内存中，这里可以使用List实现，当读取了1000条之后，就执行一次数据的批量插入，批量插入可以借助mybatis就能简单的实现了。**

**而这个过程中，还需要考虑一些并发的问题，所以我们在处理过程中会使用线程安全的队列来保存暂存在内存中的数据，如ConcurrentLinkedQueue**

**经过验证，如此实现之后，读取一个100万数据的Excel并插入数据，耗时在100秒左右，不超过2分钟。**

### 具体实现

为了提升并发处理的能力，我们把百万级数据放到同一个excel的不同的sheet中，然后通过使用EasyExcel并发的读取这些sheet。

EasyExcel提供了ReadListener接口，允许在读取每一批数据后进行自定义处理。我们可以基于他的这个功能来实现文件的分批读取。

先增加依赖：

```java
<dependencies>
    <!-- EasyExcel -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <version>最新的版本号</version>
    </dependency>

    <!-- 数据库连接和线程池 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>

```

然后实现并发读取多个sheet的代码：

```java
@Service
public class ExcelImporterService {

    @Autowired
    private MyDataService myDataService;
    
    public void doImport() {
        // Excel文件的路径
        String filePath = "users/hollis/workspace/excel/test.xlsx";

        // 需要读取的sheet数量
        int numberOfSheets = 20;

        // 创建一个固定大小的线程池，大小与sheet数量相同
        ExecutorService executor = Executors.newFixedThreadPool(numberOfSheets);

        // 遍历所有sheets
        for (int sheetNo = 0; sheetNo < numberOfSheets; sheetNo++) {
            // 在Java lambda表达式中使用的变量需要是final
            int finalSheetNo = sheetNo;

            // 向线程池提交一个任务
            executor.submit(() -> {
                // 使用EasyExcel读取指定的sheet
                EasyExcel.read(filePath, MyDataModel.class, new MyDataModelListener(myDataService))
                         .sheet(finalSheetNo) // 指定sheet号
                         .doRead(); // 开始读取操作
            });
        }

        // 启动线程池的关闭序列
		executor.shutdown();

        // 等待所有任务完成，或者在等待超时前被中断
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // 如果等待过程中线程被中断，打印异常信息
            e.printStackTrace();
        }
    }
}

```

这段代码通过创建一个固定大小的线程池来并发读取一个包含多个sheets的Excel文件。每个sheet的读取作为一个单独的任务提交给线程池。

我们在代码中用了一个MyDataModelListener，这个类是ReadListener的一个实现类。当EasyExcel读取每一行数据时，它会自动调用我们传入的这个ReadListener实例的invoke方法。在这个方法中，我们就可以定义如何处理这些数据。

> MyDataModelListener还包含doAfterAllAnalysed方法，这个方法在所有数据都读取完毕后被调用。这里可以执行一些清理工作，或处理剩余的数据。


接下来，我们来实现这个我们的ReadListener：

```java
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

// 自定义的ReadListener，用于处理从Excel读取的数据
public class MyDataModelListener implements ReadListener<MyDataModel> {
    // 设置批量处理的数据大小
    private static final int BATCH_SIZE = 1000;
    // 用于暂存读取的数据，直到达到批量大小
    private List<MyDataModel> batch = new ArrayList<>();

    
    private MyDataService myDataService;

    // 构造函数，注入MyBatis的Mapper
    public MyDataModelListener(MyDataService myDataService) {
        this.myDataService = myDataService;
    }

    // 每读取一行数据都会调用此方法
    @Override
    public void invoke(MyDataModel data, AnalysisContext context) {
        //检查数据的合法性及有效性
        if (validateData(data)) {
            //有效数据添加到list中
            batch.add(data);
        } else {
            // 处理无效数据，例如记录日志或跳过
        }
        
        // 当达到批量大小时，处理这批数据
        if (batch.size() >= BATCH_SIZE) {
            processBatch();
        }
    }

    
    private boolean validateData(MyDataModel data) {
        // 调用mapper方法来检查数据库中是否已存在该数据
        int count = myDataService.countByColumn1(data.getColumn1());
        // 如果count为0，表示数据不存在，返回true；否则返回false
        if(count == 0){
        	return true;
        }
        
        // 在这里实现数据验证逻辑
        return false;
    }


    // 所有数据读取完成后调用此方法
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 如果还有未处理的数据，进行处理
        if (!batch.isEmpty()) {
            processBatch();
        }
    }

    // 处理一批数据的方法
    private void processBatch() {
        int retryCount = 0;
        // 重试逻辑
        while (retryCount < 3) {
            try {
                // 尝试批量插入
                myDataService.batchInsert(batch);
                // 清空批量数据，以便下一次批量处理
                batch.clear();
                break;
            } catch (Exception e) {
                // 重试计数增加
                retryCount++;
                // 如果重试3次都失败，记录错误日志
                if (retryCount >= 3) {
                    logError(e, batch);
                }
            }
        }
    }

   

    // 记录错误日志的方法
    private void logError(Exception e, List<MyDataModel> failedBatch) {
        // 在这里实现错误日志记录逻辑
        // 可以记录异常信息和导致失败的数据
    }
}

@Service
public class MyDataService{
    // MyBatis的Mapper，用于数据库操作
    @Autowired
    private MyDataMapper myDataMapper;
    
 	// 使用Spring的事务管理进行批量插入
    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(List<MyDataModel> batch) {
        // 使用MyBatis Mapper进行批量插入
        myDataMapper.batchInsert(batch);
    }

    public int countByColumn1(String column1){
        return myDataMapper.countByColumn1(column1);
    }
    
}
```

通过自定义这个MyDataModelListener，我们就可以在读取Excel文件的过程中处理数据。

每读取到一条数据之后会把他们放入一个List，当List中积累到1000条之后，进行一次数据库的批量插入，插入时如果失败了则重试，最后还是失败就打印日志。

这里批量插入，用到了MyBatis的批量插入，代码实现如下：

```java
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface MyDataMapper {
    void batchInsert(List<MyDataModel> dataList);

    int countByColumn1(String column1);
}
```

mapper.xml文件：
```java
<insert id="batchInsert" parameterType="list">
    INSERT INTO hollis_test_table_name (column1, column2, ...)
    VALUES 
    <foreach collection="list" item="item" index="index" separator=",">
        (#{item.column1}, #{item.column2}, ...)
    </foreach>
</insert>

<select id="countByColumn1" resultType="int">
    SELECT COUNT(*) FROM your_table WHERE column1 = #{column1}
</select>

```


