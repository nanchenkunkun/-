# commons-logging的使用简介

## 简介：

> Jakarta Commons-logging（JCL）是apache最早提供的日志的门面接口。提供简单的日志实现以及日志解耦功能。

​    JCL能够选择使用Log4j（或其他如slf4j等）还是JDK Logging，但是他不依赖Log4j，JDK Logging的API。如果项目的classpath中包含了log4j的类库，就会使用log4j，否则就使用JDK Logging。使用commons-logging能够灵活的选择使用那些日志方式，而且不需要修改源代码。（类似于JDBC的API接口）

## 原理：

​    JCL有两个基本的抽象类： Log( 基本记录器 ) 和 LogFactory( 负责创建 Log 实例 ) 。当 commons-logging.jar 被加入到 CLASSPATH之后，它会合理地猜测你想用的日志工具，然后进行自我设置，用户根本不需要做任何设置。默认的 LogFactory 是按照下列的步骤去发现并决定那个日志工具将被使用的（按照顺序，寻找过程会在找到第一个工具时中止） :

> ①首先在classpath下寻找commons-logging.properties文件。如果找到，则使用其中定义的Log实现类；如果找不到，则在查找是否已定义系统环境变量org.apache.commons.logging.Log，找到则使用其定义的Log实现类；

> ②查看classpath中是否有Log4j的包，如果发现，则自动使用Log4j作为日志实现类；

> ③否则，使用JDK自身的日志实现类（JDK1.4以后才有日志实现类）；

> ④否则，使用commons-logging自己提供的一个简单的日志实现类SimpleLog；

org.apache.commons.logging.Log 的具体实现有如下：

---org.apache.commons.logging.impl.Jdk14Logger 使用 JDK1.4 。

---org.apache.commons.logging.impl.Log4JLogger 使用 Log4J 。

---org.apache.commons.logging.impl.LogKitLogger  使用 avalon-Logkit 。

---org.apache.commons.logging.impl.SimpleLog   common-logging 自带日志实现类。

---org.apache.commons.logging.impl.NoOpLog     common-logging 自带日志实现类。它实现了 Log 接口。 其输出日志的方法中不进行任何操作。

Maven依赖：

```maven

<dependency>
	<groupId>commons-logging</groupId>
	<artifactId>commons-logging</artifactId>
	<version>1.2</version>
</dependency>
```

## 一、Commons-logging简单日志实现：

①新建commons-logging.properties文件，放置在classpath根路径下：

②代码中使用

```

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
public class CommonsTest {
	private final static Log logger = LogFactory.getLog(CommonsTest.class);
 
	public static void main(String[] args) {
		logger.debug("DEBUG ...");
		logger.info("INFO ...");
		logger.error("ERROR ...");
	}
}
```

## 二、Commons-logging的解耦功能：

commons-logging最核心有用的功能是解耦，它的SimpleLog实现性能比不上其他实现，如log4j等。 

①添加依赖

```

<dependency>
	<groupId>commons-logging</groupId>
	<artifactId>commons-logging</artifactId>
	<version>1.2</version>
</dependency>
<dependency>
	<groupId>log4j</groupId>
	<artifactId>log4j</artifactId>
	<version>1.2.17</version>
</dependency>
```

修改commons-logging.properties文件：显示地指定log4j

```
org.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger
```

并添加log4j.properties的配置文件：

```
log4j.rootLogger=DEBUG,console
 
# 输出到控制台
log4j.appender.console=org.apache.log4j.ConsoleAppender
# 设置输出样式    
log4j.appender.console.layout=org.apache.log4j.PatternLayout
# 日志输出信息格式为   
log4j.appender.console.layout.ConversionPattern=[%-d{yyyy-MM-dd HH:mm:ss}]-[%t-%5p]-[%C-%M(%L)]: %m%n
```

③代码中使用

```
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
public class CommonsTest {
	private final static Log logger = LogFactory.getLog(CommonsTest.class);
 
	public static void main(String[] args) {
		logger.debug("DEBUG ...");
		logger.info("INFO ...");
		logger.error("ERROR ...");
	}
}
```

