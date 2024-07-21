# 典型回答
使用包装类，不要使用基本类型，比如某个字段表示费率的`Float rate`,在接口中返回时，如果出现接口异常的情况，那么可能会返回默认值，float的话返回的是0.0，而Float返回的是null。

在接口中，为了避免发生歧义，建议使用对象，因为他默认值是null，当看到null的时候，我们明确的知道他是出错了，但是看到0.0的时候，你不知道是因为出错返回的0.0，还是就是不出错真的返回了0.0，虽然可以用其他的字段如错误码或者getSuccess判断，但是还是尽量减少歧义的可能
# 知识扩展
## 在接口定义的时候，如何定义一个字段表示是否成功？
以下四种：
```java
boolean success
Boolean success
boolean isSuccess
Boolean isSuccess
```
建议使用第2种：

```
Boolean success
```

首先，作为接口的返回对象的参数，这个字段不应该有不确定的值，而Boolean类型的默认值是null，而boolean的默认值是false，所以，当拿到一个false的时候，你就不知道是真的false了，还是因为出了问题而默认返回的false。

其他，关于参数名称，要使用success还是isSuccess，这一点在阿里巴巴Java开发手册中有明确规定和解释：

【强制】 POJO 类中的任何布尔类型的变量，都不要加 is，否则部分框架解析会引起序列化错误。<br />反例： 定义为基本数据类型 boolean isSuccess；的属性，它的方法也是 isSuccess()， RPC<br />框架在反向解析的时候， “ 以为” 对应的属性名称是 success，导致属性获取不到，进而抛出<br />异常。
