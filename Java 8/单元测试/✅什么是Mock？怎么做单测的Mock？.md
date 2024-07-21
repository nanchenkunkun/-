# 典型回答

**Mock测试就是在测试过程中，对于某些不容易构造或者不容易获取的对象，用一个虚拟的对象来创建以便测试的测试方法。**

不仅仅是软件开发领域的单元测试，其实在很多工业测试场景中，也会使用一些Mock技术，比如常见的汽车碰撞测试。

汽车在上市之前都需要经过碰撞测试，并且公布测试结果。碰撞测试过程中需要通过真实撞击来评定汽车的耐撞能力以及对内部驾驶人员的保护能力。

但是，为了保证验证的准确性，有为了保证测试人员的安全，一般会采用假人来进行测试。这些假人都是经过特制的，他们的生物学性能一般和真人是一样的，比如体重、关节能力、组织强度等。

而且有时候，为了保证测试的全面性，还会采用各种各样的假人，如成年人、老人、小孩子、男性、女性等都需要充分测试到。

所以，这个假人其实就是一个**Mock对象**。在软件单元测试中，我们也需要用到这些测试对象。目的也类似，就是为了保证测试的全面性及准确性。

之所以要在测试中使用Mock对象，其实有很多原因，其中最重要的原因就是真实对象的构造成本太高。这时候一般就会采用mock对象。

而市面上也有很多工具可以方便的帮助我们进行单元测试的mock，如Easymock、jMock、Mockito、Unitils Mock、PowerMock、JMockit等。

其中比较常用的就是Easymock（[https://easymock.org/](https://easymock.org/) ）、JMockit（[https://jmockit.github.io/index.html](https://jmockit.github.io/index.html) ）和Mockito（[https://site.mockito.org/](https://site.mockito.org/) ）这三种，用起来都比较简单。

关于这些工具的对比，在JMockit官网中有一张图还是挺明显的，如下：

![](https://cdn.nlark.com/yuque/0/2023/jpeg/5378072/1675140679876-3911c1c4-fbb4-4548-ab47-3fc40877fdd9.jpeg#averageHue=%233d3d3c&clientId=u7566d060-cabf-4&id=DSa9K&originHeight=1050&originWidth=1748&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u1b2c5083-b921-464d-be17-a9d508c54c8&title=)

# 扩展知识
### 接口mock

如我们的要测试的一个方法，其中依赖了一个RPC远程服务，因为远程服务的返回值可能是各种各样的，我们为了测试我们的接口的鲁棒性，就会针对各种边界情况进行充分测试。

如果把外部接口mock掉，也就是把外部接口的返回值当做一个mock对象，那么我们就可以很方便的模拟各种情况。如外部接口正常返回、异常返回、请求超时等等，都可以很方便的被测试。

其实，随着软件开发这么多年的发展，mock技术已经不仅仅局限于单元测试阶段了，尤其是随着微服务的兴起，应用拆分的越来越细，应用见依赖也越来越多。

这时候Mock技术就显得尤为重要了。

很多时候，一个项目中，大家可能是同时开发的，而我们的很多下游依赖可能还没开完完，或者有些特殊case没办法构造，那这时候就可以利用mock技术来mock掉下游接口。

我们日常开发中用到的时候很多，比如日常开发环境调用支付宝，我们需要经常构造诸如协议过期、余额不足、请求超时、账户不存在等case的时候，就可以想办法将他们的接口mock掉。

市面上现在也有很多接口mock工具可以使用，如RAP、Yapi、Moco和DOClever等。
#### RAP

RAP是阿里团队出的一款接口管理工具，能给你提供方便的接口文档管理、Mock、导出等功能。他可以通过分析接口结构，动态生成模拟数据，校验真实接口正确性，围绕接口定义，通过一系列自动化工具提升我们的协作效率。

现在该项目已暂停维护，但是官方团队推出了RAP2，RAP2 是在 RAP1 基础上重做的新项目，项目地址：[https://github.com/thx/rap2-delos](https://github.com/thx/rap2-delos)

![](http://www.hollischuang.com/wp-content/uploads/2020/12/16078497879943.jpg#id=izlVy&originHeight=850&originWidth=2168&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)
#### YApi

YApi是去哪儿网移动架构组开发的一个开源项目，旨在为开发、产品、测试人员提供更优雅的接口管理服务。可以帮助开发者轻松创建、发布、维护 API。官网地址：[https://hellosean1025.github.io/yapi/](https://hellosean1025.github.io/yapi/)

![](http://www.hollischuang.com/wp-content/uploads/2020/12/16078499594983.jpg#id=yjRQB&originHeight=1674&originWidth=2152&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)
#### Moco

Moco 是一个搭建模拟服务器的工具，其支持 API 和独立运行两种方式，前者通常是在 junit 等测试框架中使用，后者则是通过运行一个 jar 包开启服务。项目地址：[https://github.com/dreamhead/moco](https://github.com/dreamhead/moco)

#### DOClever

DOClever是一个可视化免费开源的接口管理工具 ,可以分析接口结构，校验接口正确性， 围绕接口定义文档，通过一系列自动化工具提升我们的协作效率。主要提供接口信息管理、接口调试运行、接口Mock、自动化测试、团队协作等功能。官网地址：[http://doclever.cn/controller/index/index.html](http://doclever.cn/controller/index/index.html)

![](http://www.hollischuang.com/wp-content/uploads/2020/12/16078500933599-scaled.jpg#id=Z2dbi&originHeight=1083&originWidth=2560&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)
