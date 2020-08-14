# 一、什么是Class文件

Class文件又称字节码文件，一种二进制文件，它是由某种语言经过编译而来，注意这里**并不一定是Java语言**，还有可能是Clojure、Groovy、JRuby、Jython、Scala等，Class文件运行在Java虚拟机上。Java虚拟机不与任何一种语言绑定，它只与Class文件这种特定的二进制文件格式所关联。

虚拟机具有语言无关性，它不关心Class文件的来源是何种语言，它只关心Class文件中的内容。Java语言中的各种变量、关键字和运算符号的语义最终都是由多条字节码命名组合而成的，因此字节码命令所能提供的语义描述能力比Java语言本身更加强大。

# 二、Class文件的结构

虚拟机可以接受任何语言编译而成的Class文件，因此也给虚拟机带来了安全隐患，为了提供语言无关性的功能就必须做好安全防备措施，避免危险有害的类文件载入到虚拟机中，对虚拟机造成损害。所以在类加载的第二大阶段就是验证，这一步工作是虚拟机安全防护的关键所在，其中检查的步骤就是对class文件按照《Java虚拟机规范》规定的内容来对其进行验证。

![1.png](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8jLqJjX0tVxpIozicN9Do0qH0K8n4gBl37iahv2oibuA0uUUMIW0nddVsQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

## 1.总体结构

Class文件是一组**以8位字节为**基础单位的二进制流，各个数据项目严格按照顺序紧凑地排列在Class文件之中，中间没有添加任何分隔符，Class文件中存储的内容几乎全部是程序运行的必要数据，没有空隙存在。当遇到需要占用8位字节以上空间的数据项时，就按照高位在前的方式分割成若干个8位字节进行存储。

Class文件格式采用类似于C语言结构体的伪结构来存储数据，这种伪结构只有两种数据类型：无符号数和表。

- 无符号数属于基本的数据类型，以u1、u2、u4、u8来分别代表1个字节、2个字节、4个字节、8个字节的无符号数，无符号数可以来描述数字、索引引用、数量值或者按照UTF-8编码构成字符串值。
- 表是由多个无符号数或者其他表作为数据项构成的复合数据类型，所有表都习惯性的以“_info”结尾。表用于描述有层次关系的复合结构的数据，整个Class文件本质上就是一张表，它的数据项构成如下图。

![](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8fhVupjB1tia2Pf12hABedelnZd6GarXnq9RiatCqofRwcj1NuXUWhV3Q/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



## 2.魔数（Magic Number）

每一个Class文件的头4个字节成为魔数（Magic Number），**它的唯一作用是确定这个文件是否是一个能被虚拟机接收的Class文件**。很多文件存储标准中都是用魔数来进行身份识别，比如gif、png、jpeg等都有魔数。使用魔数主要是来识别文件的格式，相比于通过文件后缀名识别，这种方式准确性更高，因为文件后缀名可以随便更改，但更改二进制文件内容的却很少。Class类文件的魔数是Oxcafebabe，cafe babe？咖啡宝贝？至于为什么是这个， 这个名字在java语言诞生之初就已经确定了，它象征着著名咖啡品牌Peet's Coffee中深受欢迎的Baristas咖啡，Java的商标logo也源于此。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb83YUl5aqCPWFwumV31ibmYblicwyvkibQExqUPWkvB2o1aBjpnAHJ3Mmdg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)



## 3.文件版本（Version）

在魔数后面的4个字节就是Class文件的版本号，第5和第6个字节是次版本号（Minor Version），第7和第8个字节是主版本号（Major Version）。Java的版本号是从45开始的，JDK1.1之后的每个JDK大版本发布主版本号向上加1（JDK1.0~1.1使用的版本号是45.0~45.3），比如我这里是十六进制的Ox0034，也就是十进制的52，所以说明该class文件可以被JDK1.8及以上的虚拟机执行，否则低版本虚拟机执行会报`java.lang.UnsupportedClassVersionError`错误。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8ebYK96RQAibnoLkiaLW0wqXn3iaVURL9SFdhC2v2pjibwztBCGmU3ketrQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

4.常量池（Constant Pool）

在主版本号紧接着的就是常量池的入口，它是Class文件结构中与其他项目关联最多的数据类型，**也是占用空间最大的数据之一**。常量池的容量由后2个字节指定，比如这里我的是Ox001d，即十进制的29，这就表示常量池中有29项常量，而常量池的索引是从1开始的，这一点需要特殊记忆，因为程序员习惯性的计数法是从0开始的，而这里不一样，所以我这里常量池的索引范围是1~29。设计者将第0项常量空出来是有目的的，这样可以满足后面某些指向常量池的索引值的数据在特定情况下需要表达“不引用任何一个常量池项目”的含义。

通过`javap -v`命令反编译出class文件之后，我们可以看到常量池的内容

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb82StpGOd2XSMHPiaIRTib8l0f3YZNCyXVt7sI2HGubhbEfYPyhECBzYDA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

常量池中主要存放两大类常量：**字面量**和**符号引用**。比如文本字符、声明为final的常量值就属于字面量，而符号引用则包含下面三类常量：

- 类和接口的全限名
- 字段的名称和描述符
- 方法的名称和描述符

在加载类过程的第二大阶段连接的第三个阶段**解析**的时候，**会将常量池中的符号引用替换为直接引用**。相信很多人在开始了解那里的时候也是一头雾水，当我了解到常量池的构成的时候才明白真正意思。**Java代码在编译的时候，是在虚拟机加载Class文件的时候才会动态链接，也就是说Class文件中不会保存各个方法、字段的最终内存布局信息，因此这些字段、方法的符号引用不经过运行期转换的话无法获得真正的内存入口地址，也就无法直接被虚拟机使用。当虚拟机运行时，需要从常量池获得对应的符号引用，然后在类创建时或运行时解析、翻译到具体的内存地址之中**。

常量池中每一项常量都是一张表，见下图。

- 常量池项目类型表：![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8mA4dJvGSQP4icHib8De7ZicEoDLvtUmyL1oy75HE94RjzGza3dUMdicOZg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)
- 常量池常量项的结构总表：![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8xyQ8gQMvL3DrXl4Npicvh5JT8ukesTIibNtabeHPqKj1wib3R4voupc9w/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

比如我这里测试的class文件第一项常量，它的标志位是Ox0a，即十进制10，即表示tag为10的常量项，查表发现是CONSTANT_Methodref_info类型，和上面反编译之后的到的第一个常量是一致的，Methodref表示类中方法的符号引用。查上面《常量池常量项的结构总表》可以看到Methodref中含有3个项目，第一个tag就是上述的Ox0a，那么第二个项目就是Ox0006，第三个项目就是Ox000f，分别指向的CONSTANT_Class_info索引项和CONSTANT_NameAndType_info索引项为6和15，那么反编译的结果该项常量指向的应该是#6和#15，查看上面反编译的图应证我们的推测是对的。后面的常量项就以此类推。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8fxzhGcwia0HQ18iah9ZicfnSsPT9WM3kRn8A1jwaLP4cdP31mdj88Scyg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

这里需要特殊说明一下utf8常量项的内容，这里我以第29项常量项解释，也就是最后一项常量项。查《常量池常量项的结构总表》可以看到utf8项有三个内容：tag、length、bytes。tag表示常量项类型，这里是Ox01，表示是CONSTANT_Utf8_info类型，紧接着的是长度length，这里是Ox0015，即十进制21，那么再紧接着的21个字节都表示该项常量项的具体内容。**特别注意length表示的最大值是65535，所以Java程序中仅能接收小于等于64KB英文字符的变量和变量名，否则将无法编译**。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8NicHx1M0VNW17NJu6qiaCUI4Muic0jtxqcx8IpmK2kjGHAg7Sdfl1vYCg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

## 5.访问标志（Access Flags）

在常量池结束后，紧接着的两个字节代表访问标志（Access Flags），该标志用于识别一些类或者接口层次的访问信息，其中包括：Class是类还是接口、是否定义为public、是否定义为abstract类型、类是否被声明为final等。

访问标志表![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb8l1H0uKtZdMmmnLgADWUhtK5C9icq2DyYaFQFbiacFAT6LxMUZpUZpMqA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

标志位一共有16个，但是并不是所有的都用到，上表只列举了其中8个，没有使用的标志位统统置为0，access_flags只有2个字节表示，但是有这么多标志位怎么计算而来的呢？它是由标志位为true的标志位值取或运算而来，比如这里我演示的class文件是一个类并且是public的，所以对应的ACC_PUBLIC和ACC_SIPER标志应该置为true，其余标志不满足则为false，那么access_flags的计算过程就是：Ox0001 | Ox0020 = Ox0021

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14Kk5tRJNbqUJIvTSySdQLb87vJEgzNBLyRS6V2UO1Qp4cvm6QJeoy1Uicn3pQS4mDZSdSRKvNepPRA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

## 6. 索引（Index）

索引又分类索引、父类索引和接口索引集合，类索引（this_class）和父类索引（super_class）都是一个u2类型的数据，而接口索引集合（interfaces）是一组u2类型的数据的集合，**Class文件依靠这些索引数据来确定这个类的继承关系**。所有类（除了java.lang.Object）都只有一个父类索引（Java的单继承），即父类索引不为0，只有java.lang.Object的父类索引为0。接口索引用来描述该类实现了哪些接口，它们的出现顺序是按照implements语句后接口的先后顺序出现的，如果这个类是一个接口就按照extends后面出现的顺序来。

类索引和父类索引各自指向一个CONSTANT_Class_info的类描述符常量，然后通过CONSTANT_Class_info可以定位到一个CONSTANT_Utf8_info类型的常量中的*全限名*字符串。而接口索引集合则以接口计数器开头，和前面常量池类似，若计数器表示n则后面紧跟着的n个u2数据是表示该类实现的n个接口的类索引，分别指向对应的类描述符常量。

> 全限名："java/lang/Object"表示Object类的全限名，将类全名中的“.”替换成“/”而已，多个全限名之间是“;”分隔。

仍然以我上次的那个Test.class文件为例，这里三个u2类型的值分别为Ox0005、Ox0006、Ox0000，前两个分别表示的是类索引、父类索引所指向的常量描述符。第三个表示接口集合的个数，这里为0即没有实现任何接口。假设为2，则表示接下来的2个u2数据表示实现的两个接口，每个u2数据也指向的是常量描述符。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14KqRBx533M9ZZibJZMEC9aUP6WAgM6UX3YGVlrgm5kEKA5k3zYKnJS55Ju5YMZyIzSrmvqhATJsW8A/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

## 7.字段表集合（Field Info）

字段表（field_info）用于描述接口或者类中声明的变量。**字段包括类级变量以及实例级变量，但不包括在方法内部声明的局部变量**。字段包含的信息比较多，包含以下内容：

- 字段的作用域：public、private、protect修饰符
- 变量类型（类变量or实例变量）：static
- 可变性：final
- 并发可见性：volatile
- 可否序列化：transient
- 数据类型：基本数据类型、对象、数组
- 字段名称

上面的这些信息除了字段数据类型和字段名称其他都是以**布尔值**来描述的，有就是true且对应一个标志位，没有则false，这种表示方法和上一节的Access Flags一样。字段数据类型和字段名称是引用的常量池中的常量来描述，可能是CONSTANT_Class_info也可能是CONSTANT_Utf8_info。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14KqRBx533M9ZZibJZMEC9aUPVtlB3yY2Z8KBunpSUGonSQ78c01nUNDBRVTFIhCSAjFhCD3TTNfomQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

根据Java语言的语法我们可以知道，ACC_PUBLIC、ACC_PRIVATE、ACC_PROTECTED三个标志只能选一个，ACC_FINAL、ACC_VOLATILE不能同时存在，接口必须有ACC_PUBLIC、ACC_STATIC、ACC_FINAL标志。

**描述符**

描述符的作用是用来描述字段的数据类型、方法的参数列表（数量、类型、顺序）和返回值。其中基本数据类型以及void返回值类型都是用一个大写字母来表示的，对象的类型由一个L加对象全限名表示。

![img](https://mmbiz.qpic.cn/mmbiz_jpg/ddKQgauN14KqRBx533M9ZZibJZMEC9aUPjFxax0c5tKI7mWavPxyaEufpA6ebY77CwffHghQFbter6CoVtzFBag/640?wx_fmt=jpeg&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

基本数据类型和普通类型都已经知道怎么表示了，但Java中有一个特殊类型就是数组类型，它是在编译期产生的，它的描述符是在变量描述符前面加一个"["，如果是二维则加两个[，比如"[["。例如一个`String[][]`记录为`[[Ljava/lang/String`，一个`int[]`记录为`[I`。

如果是描述一个方法则在描述符前面加一个括号“()”，如果有参数则在其中按顺序添加描述符即可。例如一个`String toString(char[] c,int a,String[] b)`的描述符为：“`([CI[Ljava.lang.String)Ljava.lang.String`”。

这里同样以Test.class文件来验证，第一个u2数据是容量技术器fields_count，这里是Ox0000，说明没有字段表数据，看文章开头的java代码，确实没有定义任何字段。由于在编译class文件开始没有考虑周全，没有定义字段，这里容量技术器为0也就看不到后面的字段描述内容，这里先假设是Ox0001，即有一个字段。第二个u2数据是访问标识符access_flags，假设这里是Ox0002，说明字段标志为ACC_PRIVATE。第三个u2数据是字段名称name_index，假设值为Ox0005，指向#5的常量池CONSTANT_Utf8_info字符串。第四个u2数据是字段描述符，这里是Ox0007，指向#7的常量池字符串。

## 8. 方法表集合

方法表的描述和字段表集合描述形式一样，只需要按照对应的表格对照就可以了。方法表结构依次包含了access_flags（访问标志）、name_index（方法名索引）、descriptor_index（描述符索引）、attribute（属性表集合）几项。方法内的具体代码存放在属性表集合attribute的名为“Code”的属性里面。

方法表结构表：

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

方法访问标志表：

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

继续以Test.class文件分析，容量计数器methods_count的值为Ox0002，表示由两个方法，疑惑？看文章开头的代码只有一个main方法啊，为什么会有两个？其实字节码中包含了平时省略了的无参构造方法<init>。

紧跟着的是2个方法描述集合，这里以第一个无参构造来解释，首先是访问标志access_flags，值是Ox0001，查表可知是ACC_PUBLIC类型的，然后是方法名索引name_index，值是Ox0007，指向的是常量池CONSTANT_Utf8_info字符串，即#7，我们查看反编译的代码可以看到#7确实是<init>。

然后是描述符索引descriptor_index，值是Ox0008指向的是常量项#8，反编译后看到是`()V`，构造方法无返回值，所以用的void的标识字符V，但是在书写代码时不能显式加void，因为其验证是在编译期。紧接着的是属性表集合的属性计数量attributes_count，这里是Ox0001，说明只有一个属性，即前面说的“Code”属性。

接下来的就是分别表示每一个属性的具体指向，这里只有一个当然就只需看一个u2数据，这里是Ox0009，指向的是常量项#9，反编译结果#9确实是Code。

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14KqRBx533M9ZZibJZMEC9aUPAP7VwOn8MJia3k4tnWl6QxtVibS7HtqV0U2oyFbOXEfQSgcprbczYK3w/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

![img](https://mmbiz.qpic.cn/mmbiz_png/ddKQgauN14KqRBx533M9ZZibJZMEC9aUPJeW8heBdDruX3PSbX5VVtWSlgmG4sjTm3Fjeiaiaegm0u7VTSrzicakGw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

**如果方法在子类中没有被重写，方法表集合中就不会出现来自父类的信息。**

从方法表集合可以看出，Class文件对一个方法的特征识别（《Java虚拟机规范》称之为特征签名）有很多，比如方法描述符、访问控制标志、返回值、属性表等。



## 9.属性表集合

属性表（attribute_info）存在于Class文件、字段表、方法表等，它用于描述某些场合专有的信息。在class文件中对属性表的限定并不是很严格，只要不要与已有属性名重复，任何不能实现的编译器都可以向属性表中写入自己定义的属性信息，虚拟机在运行时会忽略掉它不认识的属性。这一部分内容较多并且不固定。