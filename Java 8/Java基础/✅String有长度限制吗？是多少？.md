# 典型回答

有，编译期和运行期不一样。

编译期需要用CONSTANT_Utf8_info 结构用于表示字符串常量的值，而这个结构是有长度限制，他的限制是65535。

运行期，String的length参数是Int类型的，那么也就是说，String定义的时候，最大支持的长度就是int的最大范围值。根据Integer类的定义，`java.lang.Integer#MAX_VALUE`的最大值是2^31 - 1;


# 扩展知识

## 常量池限制

我们知道，javac是将Java文件编译成class文件的一个命令，那么在Class文件生成过程中，就需要遵守一定的格式。

根据《Java虚拟机规范》中第4.4章节常量池的定义，CONSTANT_String_info 用于表示 java.lang.String 类型的常量对象，格式如下：

```
CONSTANT_String_info {
    u1 tag;
    u2 string_index;
}
```

其中，string_index 项的值必须是对常量池的有效索引， 常量池在该索引处的项必须是 CONSTANT_Utf8_info 结构，表示一组 Unicode 码点序列，这组 Unicode 码点序列最终会被初始化为一个 String 对象。

CONSTANT_Utf8_info 结构用于表示字符串常量的值：

```
CONSTANT_Utf8_info {
    u1 tag;
    u2 length;
    u1 bytes[length];
}
```

其中，length则指明了 bytes[]数组的长度，其类型为u2，

通过翻阅《规范》，我们可以获悉。u2表示两个字节的无符号数，那么1个字节有8位，2个字节就有16位。

16位无符号数可表示的最大值位2^16 - 1 = 65535。

也就是说，Class文件中常量池的格式规定了，其字符串常量的长度不能超过65535。

那么，我们尝试使用以下方式定义字符串：

```
 String s = "11111...1111";//其中有65535个字符"1"
```

尝试使用javac编译，同样会得到"错误: 常量字符串过长"，那么原因是什么呢？<br />其实，这个原因在javac的代码中是可以找到的，在Gen类中有如下代码：

```
private void checkStringConstant(DiagnosticPosition var1, Object var2) {
    if (this.nerrs == 0 && var2 != null && var2 instanceof String && ((String)var2).length() >= 65535) {
        this.log.error(var1, "limit.string", new Object[0]);
        ++this.nerrs;
    }
}
```

代码中可以看出，当参数类型为String，并且长度大于等于65535的时候，就会导致编译失败。

这个地方大家可以尝试着debug一下javac的编译过程（ [视频中](https://www.bilibili.com/video/BV1uK4y1t7H1/?spm_id_from=333.999.0.0)有对java的编译过程进行debug的方法），也可以发现这个地方会报错。

如果我们尝试以65534个字符定义字符串，则会发现可以正常编译。

其实，关于这个值，在《Java虚拟机规范》也有过说明：

> if the Java Virtual Machine code for a method is exactly 65535 bytes long and ends with an instruction that is 1 byte long, then that instruction cannot be protected by an exception handler. A compiler writer can work around this bug by limiting the maximum size of the generated Java Virtual Machine code for any method, instance initialization method, or static initializer (the size of any code array) to 65534 bytes


## 运行期限制


上面提到的这种String长度的限制是编译期的限制，也就是使用String s= “”;这种字面值方式定义的时候才会有的限制。

那么。String在运行期有没有限制呢，答案是有的。

String类中有很多重载的构造函数，其中有几个是支持用户传入length来执行长度的：

```
public String(byte bytes[], int offset, int length)
```

可以看到，这里面的参数length是使用int类型定义的，那么也就是说，String定义的时候，最大支持的长度就是int的最大范围值。

根据Integer类的定义，`java.lang.Integer#MAX_VALUE`的最大值是2^31 - 1;这个值约等于4G，在运行期，如果String的长度超过这个范围，就可能会抛出异常。(在jdk 1.9之前）

int 是一个 32 位变量类型，取正数部分来算的话，他们最长可以有

```
2^31-1 =2147483647 个 16-bit Unicodecharacter

2147483647 * 16 = 34359738352 位
34359738352 / 8 = 4294967294 (Byte)
4294967294 / 1024 = 4194303.998046875 (KB)
4194303.998046875 / 1024 = 4095.9999980926513671875 (MB)
4095.9999980926513671875 / 1024 = 3.99999999813735485076904296875 (GB)
```

有近 4G 的容量。

很多人会有疑惑，编译的时候最大长度都要求小于65535了，运行期怎么会出现大于65535的情况呢。这其实很常见，如以下代码：

```
String s = "";
for (int i = 0; i <100000 ; i++) {
    s+="i";
}
```

得到的字符串长度就有10万，另外我之前在实际应用中遇到过这个问题。

之前一次系统对接，需要传输高清图片，约定的传输方式是对方将图片转成BASE64编码，我们接收到之后再转成图片。

在将BASE64编码后的内容赋值给字符串的时候就抛了异常。

后来为了解决这个问题，不再传输图片的BASE64编码内容了，而是先把文件上传到OSS或者FTP中，然后直接传递文件地址。

