# 典型回答
面对这个问题的时候，读者朋友们可能会有点懵逼，但是我们回到Bean初始化的本质上来看，Bean初始化有两个时机：

1. Spring容器主动去初始化该Bean
2. 其他Bean依赖该Bean，该Bean会先被初始化

从这两个出发点来思考解决问题方案的话，大概有如下几种方式：
### 直接依赖某Bean
如下代码所示：
```java
@Component
public class A {

    @Autowired
    private B b;
}
```
如上，在加载Bean A的时候，一定会先初始化Bean B
### DependsOn
对于应用之外的二方或者三方库来说，因为我们不能修改外部库的代码，如果想要二方库的Bean在初始化之前就初始化我们内部的某个bean，就不能用第一种直接依赖的方式，可以使用`@DependsOn`注解来完成，如下代码所示：
```java
@Configuration
public class BeanOrderConfiguration {

    @Bean
    @DependsOn("beanB")
    public BeanA beanA(){
        return new BeanA();
    }
}
```
当然，DependsOn注解也可以作用在`@Component`注解上面
### BeanFactoryPostProcessor
前两种方式只能对于特定的Bean生效，如果我们希望某个Bean在其他所有Bean加载之前就初始化，用前面两种方式显然是不合适的，我们这个时候，就需要从Spring容器的生命周期中去找方法。<br />[✅SpringBean的初始化流程](https://www.yuque.com/hollis666/fo22bm/zlvhpz?view=doc_embed)<br />通过上面的参考文章我们可以知道，Spring的Bean在初始化之前，会通过`BeanFactoryPostProcessor#postProcessBeanFactory`对工厂进行处理，我们可以依赖这个特性，在此刻提前初始化我们需要的bean
```java
@Component
public class PrimaryBeanProcessor implements BeanFactoryPostProcessor {


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        PrimaryBean bean = beanFactory.getBean(PrimaryBean.class);
        System.out.println(bean);
    }
}
@Component
public class PrimaryBean {

    public PrimaryBean() {
        System.out.println("init primary bean");
    }

    @Override
    public String toString() {
        return "PrimaryBean{aaa}";
    }
}
```
这个时候我们通过控制台发现，PrimaryBean的初始化等级会优于其他Bean，如下所示：
```bash
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.5.2)

2023-05-21 14:32:09.499  INFO 59380 --- [           main] cn.wxxlamp.spring.boot.Main              : Starting Main using Java 1.8.0_291 on B-13CKQ05P-0131.local with PID 59380 (/Users/chenkai/code/test/target/classes started by chenkai in /Users/chenkai/code/test)
2023-05-21 14:32:09.503  INFO 59380 --- [           main] cn.wxxlamp.spring.boot.Main              : No active profile set, falling back to default profiles: default
init primary bean // 希望初始化的Bean
PrimaryBean{aaa}
aware applicationContext // 系统配置Bean
initializingBean
```
### 踩坑陷阱
Order只能控制同一个Bean类型中集合的顺序，不能控制不同Bean的初始化顺序，举个例子：
```java
@Component
public class Container {

	private final List<Bean> beanList;
    
    public Container(List<Bean> beanList) {
        this.beanList = beanList;
    }
}
@Order(1)
@Component
class BeanA implements Bean {}
@Order(2)
@Component
class BeanB implements Bean {}
```
这样在读取的时候，beanList中，BeanA的顺序是先于BeanB的。
