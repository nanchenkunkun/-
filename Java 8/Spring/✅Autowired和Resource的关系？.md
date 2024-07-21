# 典型回答
## 相同点
对于下面的代码来说，如果是Spring容器的话，两个注解的功能基本是等价的，他们都可以将bean注入到对应的field中
```java
@Autowired
private Bean beanA;
@Resource
private Bean beanB;
```

## 不同点
### byName和byType匹配顺序不同

1. Autowired在获取bean的时候，先是byType的方式，再是byName的方式。意思就是先在Spring容器中找以Bean为类型的Bean实例，如果找不到或者找到多个bean，则会通过fieldName来找。举个例子：
```java
@Component("beanOne")
class BeanOne implements Bean {}
@Component("beanTwo")
class BeanTwo implements Bean {}
@Service
class Test {
    // 此时会报错，先byType找到两个bean：beanOne和beanTwo
    // 然后通过byName（bean）仍然没办法匹配
	@Autowired
    private Bean bean; 

    // 先byType找到两个bean，然后通过byName确认最后要注入的bean
    @Autowired
    private Bean beanOne;

    // 先byType找到两个bean，然后通过byName确认最后要注入的bean
    @Autowired
    @Qualifier("beanOne")
    private Bean bean;
}
```

2. Resource在获取bean的时候，和Autowired恰好相反，先是byName方式，然后再是byType方式。当然，我们也可以通过注解中的参数显示指定通过哪种方式。同样举个例子：
```java
@Component("beanOne")
class BeanOne implements Bean {}
@Component("beanTwo")
class BeanTwo implements Bean {}
@Service
class Test {
    // 此时会报错，先byName，发现没有找到bean
    // 然后通过byType找到了两个Bean：beanOne和beanTwo，仍然没办法匹配
	@Resource
    private Bean bean; 

    // 先byName直接找到了beanOne，然后注入
    @Resource
    private Bean beanOne;

    // 显示通过byType注入，能注入成功
    @Resource(type = BeanOne.class)
    private Bean bean;
}
```
### 作用域不同

1. Autowired可以作用在构造器，字段，setter方法上
2. Resource 只可以使用在field，setter方法上
### 支持方不同

1. Autowired是Spring提供的自动注入注解，只有Spring容器会支持，如果做容器迁移，是需要修改代码的
2. Resource是JDK官方提供的自动注入注解（JSR-250）。它等于说是一个标准或者约定，所有的IOC容器都会支持这个注解。假如系统容器从Spring迁移到其他IOC容器中，是不需要修改代码的。
