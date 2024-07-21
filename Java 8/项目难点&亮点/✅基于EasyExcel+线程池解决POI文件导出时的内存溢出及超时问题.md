### 背景

在一个后台管理功能中，需要导出Excel，但是当处理大数据量的Excel文件导出时，常用的Apache POI库可能因其内存占用较高而导致内存溢出问题。同时，数据处理过程可能非常耗时，导致用户等待时间过长或请求超时。为解决这些问题，采用了基于 EasyExcel 和线程池的解决方案。

[✅POI导致内存溢出排查](https://www.yuque.com/hollis666/fo22bm/sabwlgzwy2nhnseg?view=doc_embed)

### 技术选型

Excel的导出很多种方案，包括了POI、EasyExcel还有Hutool中也有类似的功能。在市面上，用的最多的还是POI和EasyExcel，而在处理大文件这方面，EasyExcel更加适合一些。

在文件导出过程中，用异步的方式进行，用户不需要在页面一直等待。异步文件生成之后，把文件上传到云存储中，再通知用户去下载即可。

这里云存储选择阿里云的OSS，线程池异步处理采用@Async

用户通知这里就是用Spring Mail进行邮件发送即可。

### 具体实现

入口是一个Controller，主要接收用户的文件导出请求。

```
@RestController
@RequestMapping("/export")
public class DataExportController {

    @Autowired
    private ExcelExportService exportService;

    @GetMapping("/data")
    public ResponseEntity<String> exportData() {
        List<DataModel> data = fetchData();
        String fileUrl = exportService.exportDataAsync(data);

        return ResponseEntity.ok("导出任务开始，文件生成后会通知您下载链接");
    }

    private List<DataModel> fetchData() {
        // 获取需要导出的数据
    }
}

```

这里做了一些简化，比如筛选条件、以及具体的获取数据部分我都省略了，大家可以根据自己的业务情况来实现。

下面是导出服务的具体实现:

```
@Service
public class ExcelExportService {

    @Async("exportExecutor")
    public String exportDataAsync(List<DataModel> data) {
        // 生成 Excel 文件并获取 InputStream
        InputStream fileContent = generateExcelFile(data);
        String fileName = "data_" + System.currentTimeMillis() + ".xlsx";
        
        // 上传到 OSS
        String fileUrl = ossService.uploadFile(fileName, fileContent);
        // 发送邮件
        emailService.sendEmail(data.getUserEmail(), "文件导出通知", "您的文件已导出，下载链接: " + fileUrl);
        return fileUrl;
    }

   private InputStream generateExcelFile(List<DataModel> data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ExcelWriterBuilder writerBuilder = EasyExcel.write(outputStream, DataModel.class);
            writerBuilder.sheet("Data").doWrite(data);
        } catch (Exception e) {
            // 处理异常
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    // DataModel 类定义
    public static class DataModel {
        //省略参数及setter/getter
    }
}

```

这里面用到了@Async来实现一个异步处理，这里主要干了三件事：

- 使用EasyExcel生成文件
- OSS上传生成后的文件
- 给用户发邮件通知下载地址

这里为了用到真正的线程池，制定了一个自定义的exportExecutor，实现如下:

```
@Configuration
@EnableAsync
public class AsyncExecutorConfig {
    @Bean("exportExecutor")
    public Executor exportExecutor() {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("registerSuccessExecutor-%d").build();

        ExecutorService executorService = new ThreadPoolExecutor(10, 20,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

        return executorService;
    }

}

```

OSS上传服务部分代码实现如下，依赖阿里云OSS的API进行文件上传：

```
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

public class OssService {

    private String endpoint = "<OSS_ENDPOINT>";
    private String accessKeyId = "<ACCESS_KEY_ID>";
    private String accessKeySecret = "<ACCESS_KEY_SECRET>";
    private String bucketName = "<BUCKET_NAME>";

    public String uploadFile(String fileName, InputStream fileContent) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.putObject(new PutObjectRequest(bucketName, fileName, fileContent));
            // 设置URL过期时间为1小时
            Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
            return url.toString();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}

```

邮件发送部分实现：

```
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String toAddress, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@example.com");
        message.setTo(toAddress);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}

```

还需要一些额外的Spring Mail的配置，配置到**application.properties**：

```
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=user@example.com
spring.mail.password=yourpassword
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 学习资料

[✅POI的如何做大文件的写入](https://www.yuque.com/hollis666/fo22bm/kalmkdx5fukxt13q?view=doc_embed)

[✅为什么不建议直接使用Spring的@Async](https://www.yuque.com/hollis666/fo22bm/naw927g44ywpxw4e?view=doc_embed)



