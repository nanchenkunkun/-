# HTTP请求处理过程（二）

## Engine处理请求

我们在前面的文章中讲过**StandardEngine**的构造函数为自己的Pipeline添加了基本阀**StandardEngineValve**，代码如下：

```java
public StandardEngine() {
    super();
    pipeline.setBasic(new StandardEngineValve());
    try {
        setJvmRoute(System.getProperty("jvmRoute"));
    } catch(Exception ex) {
        log.warn(sm.getString("standardEngine.jvmRouteFail"));
    }
}
```

接下来我们看看`StandardEngineValve`的`invoke()`方法。该方法主要是选择合适的Host，然后调用Host中pipeline的第一个`Valve的invoke()`方法。

```java
public final void invoke(Request request, Response response)
    throws IOException, ServletException {

    // Select the Host to be used for this Request
    Host host = request.getHost();
    if (host == null) {
        response.sendError
            (HttpServletResponse.SC_BAD_REQUEST,
             sm.getString("standardEngine.noHost",
                          request.getServerName()));
        return;
    }
    if (request.isAsyncSupported()) {
        request.setAsyncSupported(host.getPipeline().isAsyncSupported());
    }

    // Ask this Host to process this request
    host.getPipeline().getFirst().invoke(request, response);
}
```

该方法很简单，校验该Engline 容器是否含有Host容器，如果不存在，返回400错误，否则继续执行 `host.getPipeline().getFirst().invoke(request, response)`，可以看到 Host 容器先获取自己的管道，再获取第一个阀门，我们再看看该阀门的 invoke 方法。

## Host处理请求

分析Host的时候，我们从Host的构造函数入手，该方法主要是设置基础阀门。

```java
public StandardHost() {
    super();
    pipeline.setBasic(new StandardHostValve());
}
```

StandardPipeline调用getFirst得到第一个阀去处理请求，由于基本阀是最后一个，所以最后会由基本阀去处理请求。

StandardHost的Pipeline里面一定有 ErrorReportValve 与 StandardHostValve两个Valve，ErrorReportValve主要是检测 Http 请求过程中是否出现过什么异常, 有异常的话, 直接拼装 html 页面, 输出到客户端。

我们看看ErrorReportValve的invoke方法：

```java
public void invoke(Request request, Response response)
    throws IOException, ServletException {
    // Perform the request
    // 1. 先将 请求转发给下一个 Valve
    getNext().invoke(request, response);  
    // 2. 这里的 isCommitted 表明, 请求是正常处理结束    
    if (response.isCommitted()) {               
        return;
    }
    // 3. 判断请求过程中是否有异常发生
    Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    if (request.isAsyncStarted() && ((response.getStatus() < 400 &&
            throwable == null) || request.isAsyncDispatching())) {
        return;
    }
    if (throwable != null) {
        // The response is an error
        response.setError();
        // Reset the response (if possible)
        try {
            // 4. 重置 response 里面的数据(此时 Response 里面可能有些数据)
            response.reset();                  
        } catch (IllegalStateException e) {
            // Ignore
        }
         // 5. 这就是我们常看到的 500 错误码
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    response.setSuspended(false);
    try {
        // 6. 这里就是将 异常的堆栈信息组合成 html 页面, 输出到前台        
        report(request, response, throwable);                                   
    } catch (Throwable tt) {
        ExceptionUtils.handleThrowable(tt);
    }
    if (request.isAsyncStarted()) {          
        // 7. 若是异步请求的话, 设置对应的 complete (对应的是 异步 Servlet)                   
        request.getAsyncContext().complete();
    }
}
```

该方法首先执行了下个阀门的 invoke 方法。然后根据返回的Request 属性设置一些错误信息。那么下个阀门是谁呢？其实就是基础阀门了：StandardHostValve，该阀门的 invoke 的方法是如何实现的呢？

```java
@Override
public final void invoke(Request request, Response response)
    throws IOException, ServletException {

    // Select the Context to be used for this Request
    Context context = request.getContext();
    if (context == null) {
        response.sendError
            (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
             sm.getString("standardHost.noContext"));
        return;
    }

    // Bind the context CL to the current thread
    if( context.getLoader() != null ) {
        // Not started - it should check for availability first
        // This should eventually move to Engine, it's generic.
        if (Globals.IS_SECURITY_ENABLED) {
            PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                    context.getLoader().getClassLoader());
            AccessController.doPrivileged(pa);                
        } else {
            Thread.currentThread().setContextClassLoader
                    (context.getLoader().getClassLoader());
        }
    }
    if (request.isAsyncSupported()) {
        request.setAsyncSupported(context.getPipeline().isAsyncSupported());
    }

    // Don't fire listeners during async processing
    // If a request init listener throws an exception, the request is
    // aborted
    boolean asyncAtStart = request.isAsync(); 
    // An async error page may dispatch to another resource. This flag helps
    // ensure an infinite error handling loop is not entered
    boolean errorAtStart = response.isError();
    if (asyncAtStart || context.fireRequestInitEvent(request)) {

        // Ask this Context to process this request
        try {
            context.getPipeline().getFirst().invoke(request, response);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            if (errorAtStart) {
                container.getLogger().error("Exception Processing " +
                        request.getRequestURI(), t);
            } else {
                request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
                throwable(request, response, t);
            }
        }

        // If the request was async at the start and an error occurred then
        // the async error handling will kick-in and that will fire the
        // request destroyed event *after* the error handling has taken
        // place
        if (!(request.isAsync() || (asyncAtStart &&
                request.getAttribute(
                        RequestDispatcher.ERROR_EXCEPTION) != null))) {
            // Protect against NPEs if context was destroyed during a
            // long running request.
            if (context.getState().isAvailable()) {
                if (!errorAtStart) {
                    // Error page processing
                    response.setSuspended(false);

                    Throwable t = (Throwable) request.getAttribute(
                            RequestDispatcher.ERROR_EXCEPTION);

                    if (t != null) {
                        throwable(request, response, t);
                    } else {
                        status(request, response);
                    }
                }

                context.fireRequestDestroyEvent(request);
            }
        }
    }

    // Access a session (if present) to update last accessed time, based on a
    // strict interpretation of the specification
    if (ACCESS_SESSION) {
        request.getSession(false);
    }

    // Restore the context classloader
    if (Globals.IS_SECURITY_ENABLED) {
        PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                StandardHostValve.class.getClassLoader());
        AccessController.doPrivileged(pa);                
    } else {
        Thread.currentThread().setContextClassLoader
                (StandardHostValve.class.getClassLoader());
    }
}
```

首先校验了Request 是否存在 Context，其实在执行 CoyoteAdapter.postParseRequest 方法的时候就设置了，如果Context 不存在，就返回500，接着还是老套路：context.getPipeline().getFirst().invoke，该管道获取的是基础阀门：StandardContextValve，我们还是关注他的 invoke 方法。

## Context处理请求

接着Context会去处理请求，同理，StandardContextValve的invoke方法会被调用：

```java
@Override
public final void invoke(Request request, Response response)
    throws IOException, ServletException {
    // Disallow any direct access to resources under WEB-INF or META-INF
    MessageBytes requestPathMB = request.getRequestPathMB();
    if ((requestPathMB.startsWithIgnoreCase("/META-INF/", 0))
            || (requestPathMB.equalsIgnoreCase("/META-INF"))
            || (requestPathMB.startsWithIgnoreCase("/WEB-INF/", 0))
            || (requestPathMB.equalsIgnoreCase("/WEB-INF"))) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    // Select the Wrapper to be used for this Request
    Wrapper wrapper = request.getWrapper();
    if (wrapper == null || wrapper.isUnavailable()) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    // Acknowledge the request
    try {
        response.sendAcknowledgement();
    } catch (IOException ioe) {
        container.getLogger().error(sm.getString(
                "standardContextValve.acknowledgeException"), ioe);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ioe);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
    }

    if (request.isAsyncSupported()) {
        request.setAsyncSupported(wrapper.getPipeline().isAsyncSupported());
    }
    wrapper.getPipeline().getFirst().invoke(request, response);
}
```

## Wrapper处理请求

Wrapper是一个Servlet的包装，我们先来看看构造方法。主要作用就是设置基础阀门`StandardWrapperValve`。

```java
public StandardWrapper() {
    super();
    swValve=new StandardWrapperValve();
    pipeline.setBasic(swValve);
    broadcaster = new NotificationBroadcasterSupport();
}
```

接下来我们看看`StandardWrapperValve`的`invoke()`方法。

```java
@Override
public final void invoke(Request request, Response response)
    throws IOException, ServletException {

    // Initialize local variables we may need
    boolean unavailable = false;
    Throwable throwable = null;
    // This should be a Request attribute...
    long t1=System.currentTimeMillis();
    requestCount.incrementAndGet();
    StandardWrapper wrapper = (StandardWrapper) getContainer();
    Servlet servlet = null;
    Context context = (Context) wrapper.getParent();

    // Check for the application being marked unavailable
    if (!context.getState().isAvailable()) {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                       sm.getString("standardContext.isUnavailable"));
        unavailable = true;
    }

    // Check for the servlet being marked unavailable
    if (!unavailable && wrapper.isUnavailable()) {
        container.getLogger().info(sm.getString("standardWrapper.isUnavailable",
                wrapper.getName()));
        long available = wrapper.getAvailable();
        if ((available > 0L) && (available < Long.MAX_VALUE)) {
            response.setDateHeader("Retry-After", available);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    sm.getString("standardWrapper.isUnavailable",
                            wrapper.getName()));
        } else if (available == Long.MAX_VALUE) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    sm.getString("standardWrapper.notFound",
                            wrapper.getName()));
        }
        unavailable = true;
    }

    // Allocate a servlet instance to process this request
    try {
        // 关键点1：这儿调用Wrapper的allocate()方法分配一个Servlet实例
        if (!unavailable) {
            servlet = wrapper.allocate();
        }
    } catch (UnavailableException e) {
        container.getLogger().error(
                sm.getString("standardWrapper.allocateException",
                        wrapper.getName()), e);
        long available = wrapper.getAvailable();
        if ((available > 0L) && (available < Long.MAX_VALUE)) {
            response.setDateHeader("Retry-After", available);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                       sm.getString("standardWrapper.isUnavailable",
                                    wrapper.getName()));
        } else if (available == Long.MAX_VALUE) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                       sm.getString("standardWrapper.notFound",
                                    wrapper.getName()));
        }
    } catch (ServletException e) {
        container.getLogger().error(sm.getString("standardWrapper.allocateException",
                         wrapper.getName()), StandardWrapper.getRootCause(e));
        throwable = e;
        exception(request, response, e);
    } catch (Throwable e) {
        ExceptionUtils.handleThrowable(e);
        container.getLogger().error(sm.getString("standardWrapper.allocateException",
                         wrapper.getName()), e);
        throwable = e;
        exception(request, response, e);
        servlet = null;
    }

    MessageBytes requestPathMB = request.getRequestPathMB();
    DispatcherType dispatcherType = DispatcherType.REQUEST;
    if (request.getDispatcherType()==DispatcherType.ASYNC) dispatcherType = DispatcherType.ASYNC;
    request.setAttribute(Globals.DISPATCHER_TYPE_ATTR,dispatcherType);
    request.setAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR,
            requestPathMB);
    // Create the filter chain for this request
    // 关键点2，创建过滤器链，类似于Pipeline的功能
    ApplicationFilterChain filterChain =
            ApplicationFilterFactory.createFilterChain(request, wrapper, servlet);

    // Call the filter chain for this request
    // NOTE: This also calls the servlet's service() method
    try {
        if ((servlet != null) && (filterChain != null)) {
            // Swallow output if needed
            if (context.getSwallowOutput()) {
                try {
                    SystemLogHandler.startCapture();
                    if (request.isAsyncDispatching()) {
                        request.getAsyncContextInternal().doInternalDispatch();
                    } else {
                        // 关键点3，调用过滤器链的doFilter，最终会调用到Servlet的service方法
                        filterChain.doFilter(request.getRequest(),
                                response.getResponse());
                    }
                } finally {
                    String log = SystemLogHandler.stopCapture();
                    if (log != null && log.length() > 0) {
                        context.getLogger().info(log);
                    }
                }
            } else {
                if (request.isAsyncDispatching()) {
                    request.getAsyncContextInternal().doInternalDispatch();
                } else {
                    // 关键点3，调用过滤器链的doFilter，最终会调用到Servlet的service方法
                    filterChain.doFilter
                        (request.getRequest(), response.getResponse());
                }
            }

        }
    } catch (ClientAbortException e) {
        throwable = e;
        exception(request, response, e);
    } catch (IOException e) {
        container.getLogger().error(sm.getString(
                "standardWrapper.serviceException", wrapper.getName(),
                context.getName()), e);
        throwable = e;
        exception(request, response, e);
    } catch (UnavailableException e) {
        container.getLogger().error(sm.getString(
                "standardWrapper.serviceException", wrapper.getName(),
                context.getName()), e);
        //            throwable = e;
        //            exception(request, response, e);
        wrapper.unavailable(e);
        long available = wrapper.getAvailable();
        if ((available > 0L) && (available < Long.MAX_VALUE)) {
            response.setDateHeader("Retry-After", available);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                       sm.getString("standardWrapper.isUnavailable",
                                    wrapper.getName()));
        } else if (available == Long.MAX_VALUE) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        sm.getString("standardWrapper.notFound",
                                    wrapper.getName()));
        }
        // Do not save exception in 'throwable', because we
        // do not want to do exception(request, response, e) processing
    } catch (ServletException e) {
        Throwable rootCause = StandardWrapper.getRootCause(e);
        if (!(rootCause instanceof ClientAbortException)) {
            container.getLogger().error(sm.getString(
                    "standardWrapper.serviceExceptionRoot",
                    wrapper.getName(), context.getName(), e.getMessage()),
                    rootCause);
        }
        throwable = e;
        exception(request, response, e);
    } catch (Throwable e) {
        ExceptionUtils.handleThrowable(e);
        container.getLogger().error(sm.getString(
                "standardWrapper.serviceException", wrapper.getName(),
                context.getName()), e);
        throwable = e;
        exception(request, response, e);
    }

    // Release the filter chain (if any) for this request
    // 关键点4，释放掉过滤器链及其相关资源
    if (filterChain != null) {
        filterChain.release();
    }

    // 关键点5，释放掉Servlet及相关资源
    // Deallocate the allocated servlet instance
    try {
        if (servlet != null) {
            wrapper.deallocate(servlet);
        }
    } catch (Throwable e) {
        ExceptionUtils.handleThrowable(e);
        container.getLogger().error(sm.getString("standardWrapper.deallocateException",
                         wrapper.getName()), e);
        if (throwable == null) {
            throwable = e;
            exception(request, response, e);
        }
    }

    // If this servlet has been marked permanently unavailable,
    // unload it and release this instance
    // 关键点6，如果servlet被标记为永远不可达，则需要卸载掉它，并释放这个servlet实例
    try {
        if ((servlet != null) &&
            (wrapper.getAvailable() == Long.MAX_VALUE)) {
            wrapper.unload();
        }
    } catch (Throwable e) {
        ExceptionUtils.handleThrowable(e);
        container.getLogger().error(sm.getString("standardWrapper.unloadException",
                         wrapper.getName()), e);
        if (throwable == null) {
            throwable = e;
            exception(request, response, e);
        }
    }
    long t2=System.currentTimeMillis();

    long time=t2-t1;
    processingTime += time;
    if( time > maxTime) maxTime=time;
    if( time < minTime) minTime=time;
}
```

通过阅读源码，我们发现了几个关键点。现罗列如下，后面我们会逐一分析这些关键点相关的源码。

1. 关键点1：这儿调用Wrapper的allocate()方法分配一个Servlet实例
2. 关键点2，创建过滤器链，类似于Pipeline的功能
3. 关键点3，调用过滤器链的doFilter，最终会调用到Servlet的service方法
4. 关键点4，释放掉过滤器链及其相关资源
5. 关键点5，释放掉Servlet及相关资源
6. 关键点6，如果servlet被标记为永远不可达，则需要卸载掉它，并释放这个servlet实例

### 关键点1 - Wrapper分配Servlet实例

我们来分析一下Wrapper.allocate()方法

```java
@Override
public Servlet allocate() throws ServletException {

    // If we are currently unloading this servlet, throw an exception
    // 卸载过程中，不能分配Servlet
    if (unloading) {
        throw new ServletException(sm.getString("standardWrapper.unloading", getName()));
    }

    boolean newInstance = false;

    // If not SingleThreadedModel, return the same instance every time
    // 如果Wrapper没有实现SingleThreadedModel，则每次都会返回同一个Servlet
    if (!singleThreadModel) {
        // Load and initialize our instance if necessary
        // 实例为null或者实例还未初始化，使用synchronized来保证并发时的原子性
        if (instance == null || !instanceInitialized) {
            synchronized (this) {
                if (instance == null) {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Allocating non-STM instance");
                        }

                        // Note: We don't know if the Servlet implements
                        // SingleThreadModel until we have loaded it.
                        // 加载Servlet
                        instance = loadServlet();
                        newInstance = true;
                        if (!singleThreadModel) {
                            // For non-STM, increment here to prevent a race
                            // condition with unload. Bug 43683, test case
                            // #3
                            countAllocated.incrementAndGet();
                        }
                    } catch (ServletException e) {
                        throw e;
                    } catch (Throwable e) {
                        ExceptionUtils.handleThrowable(e);
                        throw new ServletException(sm.getString("standardWrapper.allocate"), e);
                    }
                }
                // 初始化Servlet
                if (!instanceInitialized) {
                    initServlet(instance);
                }
            }
        }

        if (singleThreadModel) {
            if (newInstance) {
                // Have to do this outside of the sync above to prevent a
                // possible deadlock
                synchronized (instancePool) {
                    instancePool.push(instance);
                    nInstances++;
                }
            }
        }
        // 非单线程模型，直接返回已经创建的Servlet，也就是说，这种情况下只会创建一个Servlet
        else {
            if (log.isTraceEnabled()) {
                log.trace("  Returning non-STM instance");
            }
            // For new instances, count will have been incremented at the
            // time of creation
            if (!newInstance) {
                countAllocated.incrementAndGet();
            }
            return instance;
        }
    }

    // 如果是单线程模式，则使用servlet对象池技术来加载多个Servlet
    synchronized (instancePool) {
        while (countAllocated.get() >= nInstances) {
            // Allocate a new instance if possible, or else wait
            if (nInstances < maxInstances) {
                try {
                    instancePool.push(loadServlet());
                    nInstances++;
                } catch (ServletException e) {
                    throw e;
                } catch (Throwable e) {
                    ExceptionUtils.handleThrowable(e);
                    throw new ServletException(sm.getString("standardWrapper.allocate"), e);
                }
            } else {
                try {
                    instancePool.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("  Returning allocated STM instance");
        }
        countAllocated.incrementAndGet();
        return instancePool.pop();
    }
}
```

总结下来，注意以下几点即可：

1. 卸载过程中，不能分配Servlet
2. 如果不是单线程模式，则每次都会返回同一个Servlet（默认Servlet实现方式）
3. `Servlet`实例为`null`或者`Servlet`实例还`未初始化`，使用synchronized来保证并发时的原子性
4. 如果是单线程模式，则使用servlet对象池技术来加载多个Servlet

接下来我们看看`loadServlet()`方法

```java
public synchronized Servlet loadServlet() throws ServletException {

    // Nothing to do if we already have an instance or an instance pool
    if (!singleThreadModel && (instance != null))
        return instance;

    PrintStream out = System.out;
    if (swallowOutput) {
        SystemLogHandler.startCapture();
    }

    Servlet servlet;
    try {
        long t1=System.currentTimeMillis();
        // Complain if no servlet class has been specified
        if (servletClass == null) {
            unavailable(null);
            throw new ServletException
                (sm.getString("standardWrapper.notClass", getName()));
        }

        // 关键的地方，就是通过实例管理器，创建Servlet实例，而实例管理器是通过特殊的类加载器来加载给定的类
        InstanceManager instanceManager = ((StandardContext)getParent()).getInstanceManager();
        try {
            servlet = (Servlet) instanceManager.newInstance(servletClass);
        } catch (ClassCastException e) {
            unavailable(null);
            // Restore the context ClassLoader
            throw new ServletException
                (sm.getString("standardWrapper.notServlet", servletClass), e);
        } catch (Throwable e) {
            e = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(e);
            unavailable(null);

            // Added extra log statement for Bugzilla 36630:
            // https://bz.apache.org/bugzilla/show_bug.cgi?id=36630
            if(log.isDebugEnabled()) {
                log.debug(sm.getString("standardWrapper.instantiate", servletClass), e);
            }

            // Restore the context ClassLoader
            throw new ServletException
                (sm.getString("standardWrapper.instantiate", servletClass), e);
        }

        if (multipartConfigElement == null) {
            MultipartConfig annotation =
                    servlet.getClass().getAnnotation(MultipartConfig.class);
            if (annotation != null) {
                multipartConfigElement =
                        new MultipartConfigElement(annotation);
            }
        }

        // Special handling for ContainerServlet instances
        // Note: The InstanceManager checks if the application is permitted
        //       to load ContainerServlets
        if (servlet instanceof ContainerServlet) {
            ((ContainerServlet) servlet).setWrapper(this);
        }

        classLoadTime=(int) (System.currentTimeMillis() -t1);

        if (servlet instanceof SingleThreadModel) {
            if (instancePool == null) {
                instancePool = new Stack<>();
            }
            singleThreadModel = true;
        }

        // 调用Servlet的init方法
        initServlet(servlet);

        fireContainerEvent("load", this);

        loadTime=System.currentTimeMillis() -t1;
    } finally {
        if (swallowOutput) {
            String log = SystemLogHandler.stopCapture();
            if (log != null && log.length() > 0) {
                if (getServletContext() != null) {
                    getServletContext().log(log);
                } else {
                    out.println(log);
                }
            }
        }
    }
    return servlet;
}
```

关键的地方有两个：

1. 通过实例管理器，创建Servlet实例，而实例管理器是通过特殊的类加载器来加载给定的类
2. 调用Servlet的init方法

### 关键点2 - 创建过滤器链

创建过滤器链是调用的`org.apache.catalina.core.ApplicationFilterFactory`的`createFilterChain()`方法。我们来分析一下这个方法。该方法需要注意的地方已经在代码的comments里面说明了。

```java
public static ApplicationFilterChain createFilterChain(ServletRequest request,
        Wrapper wrapper, Servlet servlet) {

    // If there is no servlet to execute, return null
    if (servlet == null)
        return null;

    // Create and initialize a filter chain object
    // 1. 如果加密打开了，则可能会多次调用这个方法
    // 2. 为了避免重复生成filterChain对象，所以会将filterChain对象放在Request里面进行缓存
    ApplicationFilterChain filterChain = null;
    if (request instanceof Request) {
        Request req = (Request) request;
        if (Globals.IS_SECURITY_ENABLED) {
            // Security: Do not recycle
            filterChain = new ApplicationFilterChain();
        } else {
            filterChain = (ApplicationFilterChain) req.getFilterChain();
            if (filterChain == null) {
                filterChain = new ApplicationFilterChain();
                req.setFilterChain(filterChain);
            }
        }
    } else {
        // Request dispatcher in use
        filterChain = new ApplicationFilterChain();
    }

    filterChain.setServlet(servlet);
    filterChain.setServletSupportsAsync(wrapper.isAsyncSupported());

    // Acquire the filter mappings for this Context
    StandardContext context = (StandardContext) wrapper.getParent();
    // 从这儿看出过滤器链对象里面的元素是根据Context里面的filterMaps来生成的
    FilterMap filterMaps[] = context.findFilterMaps();

    // If there are no filter mappings, we are done
    if ((filterMaps == null) || (filterMaps.length == 0))
        return (filterChain);

    // Acquire the information we will need to match filter mappings
    DispatcherType dispatcher =
            (DispatcherType) request.getAttribute(Globals.DISPATCHER_TYPE_ATTR);

    String requestPath = null;
    Object attribute = request.getAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR);
    if (attribute != null){
        requestPath = attribute.toString();
    }

    String servletName = wrapper.getName();

    // Add the relevant path-mapped filters to this filter chain
    // 类型和路径都匹配的情况下，将context.filterConfig放到过滤器链里面
    for (int i = 0; i < filterMaps.length; i++) {
        if (!matchDispatcher(filterMaps[i] ,dispatcher)) {
            continue;
        }
        if (!matchFiltersURL(filterMaps[i], requestPath))
            continue;
        ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
            context.findFilterConfig(filterMaps[i].getFilterName());
        if (filterConfig == null) {
            // FIXME - log configuration problem
            continue;
        }
        filterChain.addFilter(filterConfig);
    }

    // Add filters that match on servlet name second
    // 类型和servlet名称都匹配的情况下，将context.filterConfig放到过滤器链里面
    for (int i = 0; i < filterMaps.length; i++) {
        if (!matchDispatcher(filterMaps[i] ,dispatcher)) {
            continue;
        }
        if (!matchFiltersServlet(filterMaps[i], servletName))
            continue;
        ApplicationFilterConfig filterConfig = (ApplicationFilterConfig)
            context.findFilterConfig(filterMaps[i].getFilterName());
        if (filterConfig == null) {
            // FIXME - log configuration problem
            continue;
        }
        filterChain.addFilter(filterConfig);
    }

    // Return the completed filter chain
    return filterChain;
}
```

### 关键点3 - 调用过滤器链的doFilter

ApplicationFilterChain类的doFilter函数代码如下,它会将处理委托给internalDoFilter函数。

```java
@Override
public void doFilter(ServletRequest request, ServletResponse response)
    throws IOException, ServletException {

    if( Globals.IS_SECURITY_ENABLED ) {
        final ServletRequest req = request;
        final ServletResponse res = response;
        try {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run()
                        throws ServletException, IOException {
                        internalDoFilter(req,res);
                        return null;
                    }
                }
            );
        } catch( PrivilegedActionException pe) {
            Exception e = pe.getException();
            if (e instanceof ServletException)
                throw (ServletException) e;
            else if (e instanceof IOException)
                throw (IOException) e;
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new ServletException(e.getMessage(), e);
        }
    } else {
        internalDoFilter(request,response);
    }
}
```

ApplicationFilterChain类的internalDoFilter函数代码如下：

```java
// 1. `internalDoFilter`方法通过pos和n来调用过滤器链里面的每个过滤器。pos表示当前的过滤器下标，n表示总的过滤器数量
// 2. `internalDoFilter`方法最终会调用servlet.service()方法
private void internalDoFilter(ServletRequest request,
                              ServletResponse response)
    throws IOException, ServletException {

    // Call the next filter if there is one
    // 1. 当pos小于n时, 则执行Filter
    if (pos < n) {
        // 2. 得到 过滤器 Filter，执行一次post++
        ApplicationFilterConfig filterConfig = filters[pos++];
        try {
            Filter filter = filterConfig.getFilter();

            if (request.isAsyncSupported() && "false".equalsIgnoreCase(
                    filterConfig.getFilterDef().getAsyncSupported())) {
                request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR, Boolean.FALSE);
            }
            if( Globals.IS_SECURITY_ENABLED ) {
                final ServletRequest req = request;
                final ServletResponse res = response;
                Principal principal =
                    ((HttpServletRequest) req).getUserPrincipal();

                Object[] args = new Object[]{req, res, this};
                SecurityUtil.doAsPrivilege ("doFilter", filter, classType, args, principal);
            } else {
                // 4. 这里的 filter 的执行 有点递归的感觉, 通过 pos 来控制从 filterChain 里面拿出那个 filter 来进行操作
                // 这里把this（filterChain）传到自定义filter里面，我们自定义的filter，会重写doFilter，在这里会被调用，doFilter里面会执行业务逻辑，如果执行业务逻辑成功，则会调用 filterChain.doFilter(servletRequest, servletResponse); ，filterChain就是这里传过去的this；如果业务逻辑执行失败，则return，filterChain终止，后面的servlet.service(request, response)也不会执行了
                // 所以在 Filter 里面所调用 return, 则会终止 Filter 的调用, 而下面的 Servlet.service 更本就没有调用到
                filter.doFilter(request, response, this);
            }
        } catch (IOException | ServletException | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            e = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(e);
            throw new ServletException(sm.getString("filterChain.filter"), e);
        }
        return;
    }

    // We fell off the end of the chain -- call the servlet instance
    try {
        if (ApplicationDispatcher.WRAP_SAME_OBJECT) {
            lastServicedRequest.set(request);
            lastServicedResponse.set(response);
        }

        if (request.isAsyncSupported() && !servletSupportsAsync) {
            request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR,
                    Boolean.FALSE);
        }
        // Use potentially wrapped request from this point
        if ((request instanceof HttpServletRequest) &&
                (response instanceof HttpServletResponse) &&
                Globals.IS_SECURITY_ENABLED ) {
            final ServletRequest req = request;
            final ServletResponse res = response;
            Principal principal =
                ((HttpServletRequest) req).getUserPrincipal();
            Object[] args = new Object[]{req, res};
            SecurityUtil.doAsPrivilege("service",
                                       servlet,
                                       classTypeUsedInService,
                                       args,
                                       principal);
        } else {
            //当pos等于n时，过滤器都执行完毕，终于执行了熟悉的servlet.service(request, response)方法。
            servlet.service(request, response);
        }
    } catch (IOException | ServletException | RuntimeException e) {
        throw e;
    } catch (Throwable e) {
        e = ExceptionUtils.unwrapInvocationTargetException(e);
        ExceptionUtils.handleThrowable(e);
        throw new ServletException(sm.getString("filterChain.servlet"), e);
    } finally {
        if (ApplicationDispatcher.WRAP_SAME_OBJECT) {
            lastServicedRequest.set(null);
            lastServicedResponse.set(null);
        }
    }
}
```

自定义Filter

```java
@WebFilter(urlPatterns = "/*", filterName = "myfilter")
public class FileterController implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("Filter初始化中");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        System.out.println("登录逻辑");
        if("登录失败"){
            response.getWriter().write("登录失败");
            //后面的拦截器和servlet都不会执行了
            return;
        }
        //登录成功，执行下一个过滤器
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        System.out.println("Filter销毁中");
    }
}
```

- **pos和n是ApplicationFilterChain的成员变量，分别表示过滤器链的当前位置和过滤器总数，所以当pos小于n时，会不断执行ApplicationFilterChain的doFilter方法；**
- **当pos等于n时，过滤器都执行完毕，终于执行了熟悉的servlet.service(request, response)方法。**