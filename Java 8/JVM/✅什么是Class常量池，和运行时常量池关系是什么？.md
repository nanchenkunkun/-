# 典型回答

Class常量池可以理解为是Class文件中的资源仓库。 Class文件中除了包含类的版本、字段、方法、接口等描述信息外，还有一项信息就是常量池(constant pool table)，用于存放编译器生成的各种字面量(Literal)和符号引用(Symbolic References)。

Class是用来保存常量的一个媒介场所，并且是一个中间场所。**Class文件中的常量池部分的内容，会在运行期加载到常量池中去。

**
# 扩展知识

## 查看Class常量池

由于不同的Class文件中包含的常量的个数是不固定的，所以在Class文件的常量池入口处会设置两个字节的常量池容量计数器，记录了常量池中常量的个数。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1696936241650-d0243715-ff77-4920-b9b1-d1148365b16c.png#averageHue=%23fcfcfb&clientId=uf17925ef-8ea8-4&from=paste&height=290&id=ud5f26828&originHeight=290&originWidth=1394&originalType=binary&ratio=1&rotation=0&showTitle=false&size=188499&status=done&style=none&taskId=udf4f5dc8-6bdf-4c28-8c6c-537493b3ec3&title=&width=1394)

当然，还有一种比较简单的查看Class文件中常量池的方法，那就是通过`javap`命令。对于以上的`HelloWorld.class`，可以通过

```c
javap -v  HelloWorld.class
```

查看常量池内容如下:

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1696936273095-37316678-260d-4684-98a7-09f4ce95d3d0.png#averageHue=%23090605&clientId=uf17925ef-8ea8-4&from=paste&height=834&id=uc703f485&originHeight=834&originWidth=655&originalType=binary&ratio=1&rotation=0&showTitle=false&size=361881&status=done&style=none&taskId=u6dc3a396-b415-42a6-b88c-e7373dc58b9&title=&width=655)


> 从上图中可以看到，反编译后的class文件常量池中共有16个常量。而Class文件中常量计数器的数值是0011，将该16进制数字转换成10进制的结果是17。
> 
> 原因是与Java的语言习惯不同，常量池计数器是从1开始而不是从0开始的，常量池的个数是10进制的17，这就代表了其中有16个常量，索引值范围为1-16。



