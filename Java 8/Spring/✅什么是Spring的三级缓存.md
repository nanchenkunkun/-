# 典型回答

在Spring的BeanFactory体系中，BeanFactory是Spring IoC容器的基础接口，其DefaultSingletonBeanRegistry类实现了BeanFactory接口，并且维护了三级缓存：

```
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {
  //一级缓存，保存完成的Bean对象
  private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
  //三级缓存，保存单例Bean的创建工厂
  private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
  //二级缓存，存储"半成品"的Bean对象
  private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
}
```

**singletonObjects是一级缓存，存储的是完整创建好的单例bean对象。**在创建一个单例bean时，会先从singletonObjects中尝试获取该bean的实例，如果能够获取到，则直接返回该实例，否则继续创建该bean。

**earlySingletonObjects是二级缓存，存储的是尚未完全创建好的单例bean对象。**在创建单例bean时，如果发现该bean存在循环依赖，则会先创建该bean的"半成品"对象，并将"半成品"对象存储到earlySingletonObjects中。当循环依赖的bean创建完成后，Spring会将完整的bean实例对象存储到singletonObjects中，并将earlySingletonObjects中存储的代理对象替换为完整的bean实例对象。这样可以保证单例bean的创建过程不会出现循环依赖问题。

**singletonFactories是三级缓存，存储的是单例bean的创建工厂。**当一个单例bean被创建时，Spring会先将该bean的创建工厂存储到singletonFactories中，然后再执行创建工厂的getObject()方法，生成该bean的实例对象。在该bean被其他bean引用时，Spring会从singletonFactories中获取该bean的创建工厂，创建出该bean的实例对象，并将该bean的实例对象存储到singletonObjects中。


# 扩展知识

## 三级缓存与循环依赖

[✅三级缓存是如何解决循环依赖的问题的？](https://www.yuque.com/hollis666/fo22bm/ffk7dlcrwk35glpl?view=doc_embed)
