# 典型回答

[✅Tomcat中有哪些类加载器?](https://www.yuque.com/hollis666/fo22bm/rgupmyr7wo4s8zi0?view=doc_embed)

关于这个问题，网上有很多种说法，甚至我看过某国内非常知名的付费专栏中，关于这个点也并不是讲解的特别清楚。那么，这里我们先总结一下Tomcat的类加载机制，然后再来证明为啥我这么说：

**Tomcat的类加载机制，在默认情况下，是先把当前要加载的类委托给BootstrapClassLoader尝试加载，为了避免JRE中的核心类被我们应用自己给覆盖（如String等），Bootstrap如果无法加载，那么就由WebAppClassLoader尝试加载，如果无法加载，那么再委托通过双亲委派的方式向上委派给Common、System等类加载进行加载，即顺序为：Bootstrap->WebApp->System->Common**

**上面的是默认情况，tomcat中有一个配置**`**delegate**`**，他的默认值是false，如果设置成true了，那么他就会严格遵守双亲委派，按照Bootstrap->System->Common->WebApp的顺序进行加载。**

talk is cheap，show me the code

以下是tomcat中WebappClassLoaderBase.java中loadClass的代码，我做了一些精简，并加了一些注释：

```java
public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

    //加锁，防止并发
    synchronized (JreCompat.isGraalAvailable() ? this : getClassLoadingLock(name)) {
        if (log.isDebugEnabled()) {
            log.debug("loadClass(" + name + ", " + resolve + ")");
        }
        Class<?> clazz = null;

        // ...

        // 检查本地缓存是否已加载该类，如果是，则直接返回缓存中的 Class 对象。
        clazz = findLoadedClass0(name);
        if (clazz != null) {
            if (log.isDebugEnabled()) {
                log.debug("  Returning class from cache");
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        // 检查另一个类加载缓存，如果是GraalVM环境，直接返回缓存中的 Class 对象。
        clazz = JreCompat.isGraalAvailable() ? null : findLoadedClass(name);
        if (clazz != null) {
            if (log.isDebugEnabled()) {
                log.debug("  Returning class from cache");
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        /*
         * 尝试使用Bootstrap类加载器加载类，以防止Web应用程序覆盖Java SE类。如果加载成功，则返回加载的 Class 对象。
         */
        String resourceName = binaryNameToPath(name, false);

        ClassLoader javaseLoader = getJavaseClassLoader();
        boolean tryLoadingFromJavaseLoader;
        try {

            URL url = javaseLoader.getResource(resourceName);
            tryLoadingFromJavaseLoader = url != null;
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            tryLoadingFromJavaseLoader = true;
        }

        if (tryLoadingFromJavaseLoader) {
            try {
                clazz = javaseLoader.loadClass(name);
                if (clazz != null) {
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        boolean delegateLoad = delegate || filter(name, true);

        // 根据 delegate 属性和其他条件判断是否应该委派加载给父类加载器。
        // 如果需要委派，则直接先进行委派
        if (delegateLoad) {
            if (log.isDebugEnabled()) {
                log.debug("  Delegating to parent classloader1 " + parent);
            }
            try {
                clazz = Class.forName(name, false, parent);
                if (clazz != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("  Loading class from parent");
                    }
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        // 自己尝试加载
        // 能走到这里，肯定是BootStrap没加载到，之后还有两种情况：
        // 1、如果delegate为ture的话，说明上层类加载器也没记载到。
        // 2、如果delegate为false，那么就还没有进行过委派，先在这里尝试自己加载。
        if (log.isDebugEnabled()) {
            log.debug("  Searching local repositories");
        }
        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (log.isDebugEnabled()) {
                    log.debug("  Loading class from local repository");
                }
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        // 如果delegate为false,说明还没有做过委派，那么委派给父类加载器加载类。
        if (!delegateLoad) {
            if (log.isDebugEnabled()) {
                log.debug("  Delegating to parent classloader at end: " + parent);
            }
            try {
                clazz = Class.forName(name, false, parent);
                if (clazz != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("  Loading class from parent");
                    }
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
    }

    throw new ClassNotFoundException(name);
}

```

整个代码的过程就是：

1. 加锁： 方法使用同步块确保线程安全
2. 检查已加载类缓存： 首先，通过调用 findLoadedClass0 方法检查本地缓存是否已加载该类，如果是，则直接返回缓存中的 Class 对象。
3. 检查已加载类缓存（GraalVM 兼容性处理）： 通过调用 findLoadedClass 方法检查另一个类加载缓存，如果是GraalVM环境，直接返回缓存中的 Class 对象。
4. **尝试使用Bootstrap类加载器加载**： 尝试使用Bootstrap类加载器加载类，以防止Web应用程序覆盖Java SE类。如果加载成功，则返回加载的 Class 对象。
5. **决定是否委派加载**： 根据 delegate 属性和其他条件判断是否应该委派加载给父类加载器。
6. **委派给父类加载器**： 如果需要委派加载（delegate为true），尝试使用父类加载器加载类。
7. **自己尝试加载**： 如果未指定需要委派（delegate为false），或者未从父类加载器中找到类，则调用 findClass 方法尝试自己进行类加载。
8. **委派给父类加载器**： 如果未指定需要委派（delegate为false），且自己没加载到类，则尝试使用父类加载器加载类。


![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702559818249-905f973a-4a20-4ee2-b603-688e0c460fd8.png#averageHue=%23fbfbfb&clientId=uf5cdcf1f-8d4f-4&from=paste&height=1055&id=u6799edf9&originHeight=1055&originWidth=1015&originalType=binary&ratio=1&rotation=0&showTitle=false&size=705802&status=done&style=none&taskId=uc936b000-6a17-4123-be0f-f80163657ba&title=&width=1015)<br />（图中红线和绿线是2选一分别执行的，不会同时执行，也不会交叉执行。）

以上，就是Tomcat的类加载机制。你说他打破双亲委派了么？

打破了，当delegate = false的时候，打破了双亲委派。但是也并不是上来就自己直接加载，而是也得先给老大哥——BootStrap尝试加载，避免JRE中的类被覆盖。

没打破，当delegate = true的时候，他是严格的遵守了双亲委派的。

# 扩展知识

## 为什么破坏双亲委派

一个Tomcat，是可以同时运行多个应用的，而不同的应用可能会同时依赖一些相同的类库，但是他们使用的版本可能是不一样的，但是这些类库中的Class的全路径名因为是一样的，如果都采用双亲委派的机制的话，是无法重复加载同一个类的，那么就会导致版本冲突。

而为了有更好的隔离性，所以在Tomcat中，每个应用都由一个独立的WebappClassLoader进行加载，这样就可以完全隔离开。而多个WebAppClassLoader之间是没有委派关系的，他们就是各自加载各自需要加载的Jar包。

由于每个Web应用程序都有自己的类加载器，因此不同Web应用程序中的类可以使用相同的类名，而不会产生命名冲突。

同时，由于每个Web应用程序都有自己的类加载器，因此在卸载一个Web应用程序时，它的所有类都会从内存中清除，这可以避免内存泄漏的问题。

这种层次化的类加载器结构和委派机制确保了类的唯一性和隔离性，避免了类的重复加载和冲突，同时也实现了多个Web应用程序的隔离和独立运行。


### 如何避免重复加载

因为每个应用都是用WebAppClassLoader独自加载的，但是如果有一个公共的jar包，比如Spring，各个应用的版本都一样，那么岂不是要重复加载很多次了？这不是浪费么？

Tomcat给了个方案，那就是SharedClassLoader，我们可以把可以指定一个目录，让SharedClassLoader来加载，他加载的类在各个APP中都是可以共享使用的。
