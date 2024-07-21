# 典型回答

在使用Maven进行项目构建的应用中，如果在应用运行期发生了NoSuchMethodError、ClassNotFoundException等异常或者错误时，需要考虑Jar包冲突的问题。

如果在应用中，我们同时依赖了两个第三方的jar包A，B，而A，B中又都依赖了C包，但是依赖的C的版本不同，那么就可能发生jar包冲突，导致最终编译打包后，应用的classpath中只有一个C的jar包。

那么，因为maven有一定的Jar包依赖传递原则，所以有可能他最终选择的jar包并不是我们真正想用的 ，那这时候怎么办呢？就需要我们手动的进行依赖仲裁，通过人工干预的方式来保留我们需要的jar包。

# 扩展知识

## 依赖传递
几乎所有的Jar包冲突都和依赖传递原则有关，所以我们先说Maven中的依赖传递原则，主要由以下两个默认的原则：

**最短路径优先原则**<br />假如引入了2个Jar包A和B，都传递依赖了Z这个Jar包：<br />A -> X -> Y -> Z(2.5)<br />B -> X -> Z(2.0)<br />那其实最终生效的是Z(2.0)这个版本。因为他的路径更加短。

**最先声明优先原则**<br />如果路径长短一样，优先选最先声明的那个。<br />A -> Z(3.0)<br />B -> Z(2.5)<br />这里A最先声明，所以传递过来的Z选择用3.0版本的。

显然这种默认的原则并不一定能使我们一定能获得自己真正想用的jar包，那么就需要人工仲裁了。
## 依赖树
在开发中，我们可以通过maven的依赖树来判断是否发生了jar包冲突，以及如何发生的冲突，只需要在maven项目中使用以下命令即可打印依赖树：

```
mvn dependency:tree
```


打印出来的就是整个项目的jar包依赖的一个树形结构，我们找到发生了冲突的jar包就可以查看他的多种不同版本的依赖路径了。

在IDEA中，也可以使用maven helper插件来快速查看依赖树及冲突情况。

## 依赖仲裁

如果经过分析，项目中发生了依赖冲突，那么就需要我们进行仲裁解决，解决的手段有以下几种：

### 排除依赖

在pom文件中，我们可以在一个<dependency></dependency>标签内使用排除jar包的方式进行jar包排除。

如我们想排除X中对于Y的依赖
```
<dependency>
	<groupId>com.hollis.javabagu</groupId>
    <artifactId>X</artifactId>
		<exclusions>
			<exclusion>
				<groupId>com.hollis.javabagu</groupId>
				<artifactId>Y</artifactId>
			</exclusion>
	</exclusions>
</dependency>
```

这样，在项目中，就只会依赖X，而X依赖的Y的对应版本就会被排除了。

### 版本锁定

一般在我们开发的web项目中都会有多个Module，每一个module都会有很多外部依赖，而多种依赖可能都依赖了jar包Y，就涉及到了多个版本，如果用排除法的话，需要一个一个的进行exclusion，这样太麻烦了。

有一个一劳永逸的方法进行版本锁定。

大型项目一般都会有父级pom，你想指定哪个版本只需要在你项目的父POM中定义如下：
```
<dependencyManagement>
    <dependency>
        <groupId>com.hollis.javabagu</groupId>
        <artifactId>Y</artifactId>
        <version>3.0</version>
    </dependency>
</dependencyManagement>
```


这样对于Y的jar包的依赖就只会保留3.0这个版本了。

但是需要注意的是dependencyManagement标签只能做依赖仲裁管理使用，他并不会引入对Jar包的依赖，还需要你使用dependency进行jar包引入。
