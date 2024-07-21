# 典型回答
Stream 使用一种类似用 SQL 语句从数据库查询数据的直观方式来提供一种对 Java 集合运算和表达的高阶抽象。<br />Stream API可以极大提高Java程序员的生产力，让程序员写出高效率、干净、简洁的代码。<br />这种风格将要处理的元素集合看作一种流，流在管道中传输，并且可以在管道的节点上进行处理，比如筛选，排序，聚合等。<br />Stream有以下特性及优点：

- 无存储。Stream不是一种数据结构，它只是某种数据源的一个视图，数据源可以是一个数组，Java容器或I/O channel等。
- 为函数式编程而生。对Stream的任何修改都不会修改背后的数据源，比如对Stream执行过滤操作并不会删除被过滤的元素，而是会产生一个不包含被过滤元素的新Stream。
- 惰式执行。Stream上的操作并不会立即执行，只有等到用户真正需要结果的时候才会执行。
- 可消费性。Stream只能被“消费”一次，一旦遍历过就会失效，就像容器的迭代器那样，想要再次遍历必须重新生成。

我们举一个例子，来看一下到底Stream可以做什么事情：<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1668857553666-b12e8359-e999-4db4-b745-192d16150c8c.jpeg#averageHue=%23fcfbf9&clientId=uf271e553-bcc4-4&from=paste&id=u5bf84c7f&originHeight=895&originWidth=1600&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u35602e8a-3b6f-4f55-9fba-85e573cd8c6&title=)<br />上面的例子中，获取一些带颜色塑料球作为数据源，首先过滤掉红色的、把它们融化成随机的三角形。再过滤器并删除小的三角形。最后计算出剩余图形的周长。<br />如上图，对于流的处理，主要有三种关键性操作：分别是**流的创建、中间操作（intermediate operation）以及最终操作(terminal operation)。**
## Stream的创建
在Java 8中，可以有多种方法来创建流。<br />1、通过已有的集合来创建流<br />在Java 8中，除了增加了很多Stream相关的类以外，还对集合类自身做了增强，在其中增加了stream方法，可以将一个集合类转换成流。<br />List<String> strings = Arrays.asList("Hollis", "HollisChuang", "hollis", "Hello", "HelloWorld", "Hollis"); Stream<String> stream = strings.stream();<br />以上，通过一个已有的List创建一个流。除此以外，还有一个parallelStream方法，可以为集合创建一个并行流。<br />这种通过集合创建出一个Stream的方式也是比较常用的一种方式。<br />2、通过Stream创建流<br />可以使用Stream类提供的方法，直接返回一个由指定元素组成的流。<br />Stream<String> stream = Stream.of("Hollis", "HollisChuang", "hollis", "Hello", "HelloWorld", "Hollis");<br />如以上代码，直接通过of方法，创建并返回一个Stream。
## Stream中间操作
Stream有很多中间操作，多个中间操作可以连接起来形成一个流水线，每一个中间操作就像流水线上的一个工人，每人工人都可以对流进行加工，加工后得到的结果还是一个流。<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1668857553673-54763554-4ff1-4d51-8f12-6d474ba4c67f.jpeg#averageHue=%23fdfcfb&clientId=uf271e553-bcc4-4&from=paste&id=u4853b4c1&originHeight=799&originWidth=1600&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u508f6bef-6e49-4dfa-b165-8f6c02a4448&title=)<br />以下是常用的中间操作列表:<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1668857553673-956aa8e3-0b42-4965-9351-5e8beb62a03f.jpeg#averageHue=%23eaeae8&clientId=uf271e553-bcc4-4&from=paste&id=ue074161e&originHeight=605&originWidth=1600&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u0ab51192-ad2a-407f-a0b2-0f4c0c1500c&title=)
### filter
filter 方法用于通过设置的条件过滤出元素。以下代码片段使用 filter 方法过滤掉空字符串：
```java
List<String> strings = Arrays.asList("Hollis", "", "HollisChuang", "H", "hollis"); 
strings.stream().filter(string -> !string.isEmpty()).forEach(System.out::println); 
//Hollis, HollisChuang, H, hollis
```
### map
map 方法用于映射每个元素到对应的结果，以下代码片段使用 map 输出了元素对应的平方数：
```java
List<Integer> numbers = Arrays.asList(3, 2, 2, 3, 7, 3, 5); 
numbers.stream().map( i -> i*i).forEach(System.out::println); 
//9,4,4,9,49,9,25
```
### limit/skip
limit 返回 Stream 的前面 n 个元素；skip 则是扔掉前 n 个元素。以下代码片段使用 limit 方法保留4个元素：
```java
List<Integer> numbers = Arrays.asList(3, 2, 2, 3, 7, 3, 5); 
numbers.stream().limit(4).forEach(System.out::println); //3,2,2,3
```
### sorted
sorted 方法用于对流进行排序。以下代码片段使用 sorted 方法进行排序：
```java
List<Integer> numbers = Arrays.asList(3, 2, 2, 3, 7, 3, 5); 
numbers.stream().sorted().forEach(System.out::println); //2,2,3,3,3,5,7
```
### distinct
distinct主要用来去重，以下代码片段使用 distinct 对元素进行去重：
```java
List<Integer> numbers = Arrays.asList(3, 2, 2, 3, 7, 3, 5); 
numbers.stream().distinct().forEach(System.out::println); //3,2,7,5
```
接下来我们通过一个例子和一张图，来演示下，当一个Stream先后通过filter、map、sort、limit以及distinct处理后会发生什么。<br />代码如下：
```java
List<String> strings = Arrays.asList("Hollis", "HollisChuang", "hollis", "Hello", "HelloWorld", "Hollis"); 
Stream s = strings.stream().filter(string -> string.length()<= 6)
    .map(String::length).sorted()
    .limit(3)             
    .distinct();
```
过程及每一步得到的结果如下图：<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1668857553670-9579460d-5027-4ee8-aec5-229a1c8ad968.jpeg#averageHue=%23f4f4eb&clientId=uf271e553-bcc4-4&from=paste&id=uafca1fd4&originHeight=699&originWidth=1019&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=uc4f16b7a-3957-4a36-a690-0778d9d770e&title=)
## Stream最终操作
Stream的中间操作得到的结果还是一个Stream，那么如何把一个Stream转换成我们需要的类型呢？比如计算出流中元素的个数、将流装换成集合等。这就需要最终操作（terminal operation）<br />最终操作会消耗流，产生一个最终结果。也就是说，在最终操作之后，不能再次使用流，也不能在使用任何中间操作，否则将抛出异常：<br />`java.lang.IllegalStateException: stream has already been operated upon or closed`<br />俗话说，“你永远不会两次踏入同一条河”也正是这个意思。<br />常用的最终操作如下图：<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1668857553666-c22a6d72-8f0f-43f5-ada1-e9435df38f75.jpeg#averageHue=%23eef0ed&clientId=uf271e553-bcc4-4&from=paste&id=hpBtO&originHeight=583&originWidth=1600&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u8064f530-5b32-4421-8a16-17eac701e35&title=)
### forEach
Stream 提供了方法 ‘forEach’ 来迭代流中的每个数据。以下代码片段使用 forEach 输出了10个随机数：
```java
Random random = new Random(); 
random.ints().limit(10).forEach(System.out::println);
```
### count
count用来统计流中的元素个数。
```java
List<String> strings = Arrays.asList("Hollis", "HollisChuang", "hollis","Hollis666", "Hello", "HelloWorld", "Hollis"); 
System.out.println(strings.stream().count()); //7
```
### collect
collect就是一个归约操作，可以接受各种做法作为参数，将流中的元素累积成一个汇总结果：
```java
List<String> strings = Arrays.asList("Hollis", "HollisChuang", "hollis","Hollis666", "Hello", "HelloWorld", "Hollis");
strings  = strings.stream().filter(string -> string.startsWith("Hollis")).collect(Collectors.toList()); 
System.out.println(strings); //Hollis, HollisChuang, Hollis666, Hollis
```
接下来，我们还是使用一张图，来演示下，前文的例子中，当一个Stream先后通过filter、map、sort、limit以及distinct处理后会，在分别使用不同的最终操作可以得到怎样的结果：<br />下图，展示了文中介绍的所有操作的位置、输入、输出以及使用一个案例展示了其结果。![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1668857554694-4279c522-5497-46f1-ab48-8e5270f8fa34.jpeg#averageHue=%23f7f2eb&clientId=uf271e553-bcc4-4&from=paste&id=LhMr7&originHeight=791&originWidth=830&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u308d6379-474a-4e82-9979-c33654ae721&title=)
