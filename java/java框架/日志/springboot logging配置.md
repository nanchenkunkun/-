# Spring Boot Logging 配置



Spring Boot 能够使用Logback, Log4J2 , java util logging 作为日志记录工具。Spring Boot 默认使用Logback作为日志记录工具。日志默认输出到控制台但也能输出到文件中。我们通过spring-boot-starter-logging 加入Logback依赖，其实只要我们加入任意的Spring Boot starter 都会默认引入spring-boot-starter-logging，因此 我们不需要分开加入他们。

如果Logback JAR在类路径一直可用那么Spring Boot 将一直选择Logback 记录日志。所以想用其他的日志工具如Log4J2，我们需要去除Logback JAR并且在类路径中加入Log4J2的依赖。如果使用Logback记录日志我们不用做任何事情，只要在**application.properties**或者**application.yml**中配置日志级别就可以了。console默认输入ERROR, WARN ，INFO级别的日志。可通过修改logging.level属性来改变日志的输出级别。可以通过配置logging.file属性或logging.path属性将日志输出到文件中。当文件到达10M的时候，将新建一个文件记录日志

**logging.level.\* :** 作为package（包）的前缀来设置日志级别。
 **logging.file :**配置日志输出的文件名，也可以配置文件名的绝对路径。
 **logging.path :**配置日志的路径。如果没有配置**logging.file**,Spring Boot 将默认使用spring.log作为文件名。
 **logging.pattern.console :**定义console中logging的样式。
 **logging.pattern.file :**定义文件中日志的样式。
 **logging.pattern.level :**定义渲染不同级别日志的格式。默认是%5p.
 **logging.exception-conversion-word :**.定义当日志发生异常时的转换字
 **PID :**定义当前进程的ID

下面将讨论在**application.properties** , **application.ym** ,**Logback XML**中配置Logback以及在Spirng Boot 应用中使用Log4J2.

##### 1.logging.level

logging.level设置日志级别。我们可以使用TARCE , DEBUG , INFO , WARN , ERROR , FATAL , OFF 。可以使用root级别和package级别来控制日志的输入级别。创建一个具有以下依赖关系的应用程序。

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency> 
```

使用**src\main\resources\application.properties**配置日志级别

```
logging.level.root= WARN
logging.level.org.springframework.security= DEBUG
logging.level.org.springframework.web= ERROR
logging.level.org.hibernate= DEBUG
logging.level.org.apache.commons.dbcp2= DEBUG 
```

使用**src\main\resources\application.yml**

```
logging:
  level:
    root: WARN        
    org:
      springframework:
        security: DEBUG
        web: ERROR    
      hibernate: DEBUG        
      apache:
        commons:
          dbcp2: DEBUG 
```

##### 2.logging.file

Spring Boot 默认把日志输入到console，如果我们要把日志输入到文件中，需要配置logging.file 或者logging.path属性性。logging.file属性用来定义文件名。他不仅仅可以配置文件名，也可以路径+文件名。

```
logging.level.org.springframework.security= DEBUG
logging.level.org.hibernate= DEBUG

logging.file = mylogfile.log 
```

在这种情况下mylogfile.log将在根目录中创建。我们也可以为为mylogfile.log分配一个路径，如concretepage/mylogfile.log。这种情况下我们将在相对根目录下创建concretepage/mylogfile.log。我们也可以为日志文件配置绝对路径。
 **application.yml**中配置

```
logging:
  level:
    org:
      springframework:
        security: DEBUG
    hibernate: DEBUG

  file: mylogfile.log  
```

##### 3.logging.path

配置logging.path或者logging.path属性将日志输出到文件夹中。logging.path属性用来定义日志文件路径
 在**application.properties**中配置logging.path属性

```
logging.level.org.springframework.security= DEBUG
logging.level.org.hibernate= DEBUG

logging.path = concretepage/logs  
```

将会相对根路径下创建concretepage/logs/spring.log ,也可以配置绝对路径

**application.yml**配置

```
logging:
  level:
    org:
      springframework:
        security: DEBUG
    hibernate: DEBUG

  path: concretepage/logs  
```

##### 4.logging.pattern.console

通过设置logging.patter.console属性我们能改变输出到console的日志样式。日志样式包括时间，日志级别，线程名，日志名以及消息。我们可以按我们的喜好改变日志样式。
 **application.properties**

```

logging.level.org.springframework.security= DEBUG
logging.level.org.hibernate= DEBUG

logging.pattern.console= %d{yyyy-MMM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{15} - %msg%n
```

**application.yml**

```
logging:
  level:
    org:
      springframework:
        security: DEBUG
    hibernate: DEBUG

  pattern:
    console: '%d{yyyy-MMM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{15} - %msg%n' 
```

##### 5.logging.pattern.file

改变文件中的日志样式我们需要设置logging.pattern.file属性。首先通过logging.file或logging.path属性，把日志记录到文件中。

```
logging.level.org.springframework.security= DEBUG
logging.level.org.hibernate= DEBUG

logging.path = concretepage/logs
logging.pattern.file= %d{yyyy-MMM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{15} - %msg%n
logging.pattern.console= %d{yyyy-MMM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{15} - %msg%n  
```

通过logging.path属性将在根目录下创建concretepage/logs并默认使用spring.log作为文件名。logging.pattern.console是设置console的日志样式
 **application.yml**

```
logging:
  level:
    org:
      springframework:
        security: DEBUG
    hibernate: DEBUG
    
  path: concretepage/logs
  pattern:
    file: '%d{yyyy-MMM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{15} - %msg%n'
    console: '%d{yyyy-MMM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{15} - %msg%n' 
```

**通过命令行改变日志的输出级别**
 Sping Boot 默认输出ERROR , WARN , INFO 级别的日志。我们可以通过命令行使能DEBUG ,TRACE级别的日志输出，效果是跟配置文件一样的。想象我们有一个名为my-app.jar的可执行的JAR包我们可以在启动应用是使能DEBUG级别日志输出。

```css
java -jar my-app.jar --debug  
```

在application.properties中配置



```bash
debug=true  
```

application.yml



```bash
debug=true  
```

相同的方式使能TRACE级别的日志



```css
java -jar my-app.jar --trace  
```

application.properties



```bash
trace=true
```

application.yml



```bash
trace=true
```

**在应用程序中记录日志**
 创建一个SLF4J的例子，首先获得org.slf4j.Logger的实例。



```java
package com.concretepage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class MyApplication {
    private static final Logger logger = LoggerFactory.getLogger(MyApplication.class);  
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        logger.debug("--Application Started--");
        }       
}  
```

在application.properties配置包com.concretepage日志记录级别



```undefined
logging.level.root= WARN
logging.level.org.springframework.web= ERROR
logging.level.com.concretepage= DEBUG  
```

输出

```css
2017-03-25 19:03:54.189 DEBUG 4828 --- [           main] com.concretepage.MyApplication           : Running with Spring Boot v1.5.2.RELEASE, Spring v4.3.7.RELEASE
2017-03-25 19:03:54.189  INFO 4828 --- [           main] com.concretepage.MyApplication           : No active profile set, falling back to default profiles: default
2017-03-25 19:03:58.846  INFO 4828 --- [           main] com.concretepage.MyApplication           : Started MyApplication in 5.209 seconds (JVM running for 5.66)
2017-03-25 19:03:58.846 DEBUG 4828 --- [           main] com.concretepage.MyApplication   
```



