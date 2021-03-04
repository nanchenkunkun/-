# session机制

## Tomcat Session 概述

首先 HTTP 是一个无状态的协议, 这意味着每次发起的HTTP请求, 都是一个全新的请求(与上个请求没有任何联系, 服务端不会保留上个请求的任何信息), 而 Session 的出现就是为了解决这个问题, 将 Client 端的每次请求都关联起来, 要实现 Session 机制 通常通过 Cookie(cookie 里面保存统一标识符号), URI 附加参数, 或者就是SSL (就是SSL 中的各种属性作为一个Client请求的唯一标识), 而在初始化 ApplicationContext 指定默认的Session追踪机制(URL + COOKIE), 若 Connector 配置了 SSLEnabled, 则将通过 SSL 追踪Session的模式也加入追踪机制里面 (将 ApplicationContext.populateSessionTrackingModes()方法)

## Cookie 概述

Cookie 是在Http传输中存在于Header中的一小撮文本信息(KV), 每次浏览器都会将服务端发送给自己的Cookie信息返回发送给服务端(PS: Cookie的内容存储在浏览器端); 有了这种技术服务端就知道这次请求是谁发送过来的(比如我们这里的Session, 就是基于在Http传输中, 在Cookie里面加入一个全局唯一的标识符号JsessionId来区分是哪个用户的请求)

## Tomcat 中 Cookie 的解析

在 Tomcat 8.0.5 中 Cookie 的解析是通过内部的函数 processCookies() 来进行操作的(其实就是将Http header 的内容直接赋值给 Cookie 对象, Cookie在Header中找name是"Cookie"的数据, 拿出来进行解析), 我们这里主要从 jsessionid 的角度来看一下整个过程是如何触发的, 我们直接看函数 CoyoteAdapter.postParseRequest() 中解析 jsessionId 那部分

```java
// 尝试从 URL, Cookie, SSL 回话中获取请求的 ID, 并将 mapRequired 设置为 false
String sessionID = null;
// 1. 是否支持通过 URI 尾缀 JSessionId 的方式来追踪 Session 的变化 (默认是支持的)
if (request.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.URL)) {
    // 2. 从 URI 尾缀的参数中拿取 jsessionId 的数据 (SessionConfig.getSessionUriParamName 是获取对应cookie的名字, 默认 jsessionId, 可以在 web.xml 里面进行定义)
    sessionID = request.getPathParameter( SessionConfig.getSessionUriParamName(request.getContext()));
    if (sessionID != null) { 
        // 3. 若从 URI 里面拿取了 jsessionId, 则直接进行赋值给 request
        request.setRequestedSessionId(sessionID);
        request.setRequestedSessionURL(true);
    }
}

// Look for session ID in cookies and SSL session
// 4. 通过 cookie 里面获取 JSessionId 的值
parseSessionCookiesId(req, request);   
// 5. 在 SSL 模式下获取 JSessionId 的值                             
parseSessionSslId(request);                                         

/**
 * Parse session id in URL.
 */
protected void parseSessionCookiesId(org.apache.coyote.Request req, Request request) {

    // If session tracking via cookies has been disabled for the current
    // context, don't go looking for a session ID in a cookie as a cookie
    // from a parent context with a session ID may be present which would
    // overwrite the valid session ID encoded in the URL
    Context context = request.getMappingData().context;
    // 1. Tomcat 是否支持 通过 cookie 机制 跟踪 session
    if (context != null && !context.getServletContext()
            .getEffectiveSessionTrackingModes().contains(
                    SessionTrackingMode.COOKIE)) {                      
        return;
    }

    // Parse session id from cookies
     // 2. 获取 Cookie的实际引用对象 (PS: 这里还没有触发 Cookie 解析, 也就是 serverCookies 里面是空数据, 数据还只是存储在 http header 里面)
    Cookies serverCookies = req.getCookies(); 
    // 3. 就在这里出发了 Cookie 解析Header里面的数据 (PS: 其实就是 轮训查找 Header 里面那个 name 是 Cookie 的数据, 拿出来进行解析)    
    int count = serverCookies.getCookieCount();                         
    if (count <= 0) {
        return;
    }

    // 4. 获取 sessionId 的名称 JSessionId
    String sessionCookieName = SessionConfig.getSessionCookieName(context); 

    for (int i = 0; i < count; i++) {
        // 5. 轮询所有解析出来的 Cookie
        ServerCookie scookie = serverCookies.getCookie(i);      
        // 6. 比较 Cookie 的名称是否是 jsessionId        
        if (scookie.getName().equals(sessionCookieName)) {              
            logger.info("scookie.getName().equals(sessionCookieName)");
            logger.info("Arrays.asList(Thread.currentThread().getStackTrace()):" + Arrays.asList(Thread.currentThread().getStackTrace()));
            // Override anything requested in the URL
            // 7. 是否 jsessionId 还没有解析 (并且只将第一个解析成功的值 set 进去)
            if (!request.isRequestedSessionIdFromCookie()) {            
                // Accept only the first session id cookie
                // 8. 将MessageBytes转成 char
                convertMB(scookie.getValue());        
                // 9. 设置 jsessionId 的值                
                request.setRequestedSessionId(scookie.getValue().toString());
                request.setRequestedSessionCookie(true);
                request.setRequestedSessionURL(false);
                if (log.isDebugEnabled()) {
                    log.debug(" Requested cookie session id is " +
                        request.getRequestedSessionId());
                }
            } else {
                // 10. 若 Cookie 里面存在好几个 jsessionid, 则进行覆盖 set 值
                if (!request.isRequestedSessionIdValid()) {             
                    // Replace the session id until one is valid
                    convertMB(scookie.getValue());
                    request.setRequestedSessionId
                        (scookie.getValue().toString());
                }
            }
        }
    }

}
```

上面的步骤其实就是依次从 URI, Cookie, SSL 里面进行 jsessionId 的解析, 其中从Cookie里面进行解析是最常用的, 而且 就这个Tomcat版本里面, 从cookie里面解析 jsessionid 藏得比较深, 是由 Cookie.getCookieCount() 来进行触发的, 整个解析的过程其实就是将线程 header 里面的数据依次遍历, 找到 name="Cookie"的数据,拿出来解析字符串(这里就不再叙述了); 程序到这里其实若客户端传 jsessionId 的话, 则服务端已经将其解析出来, 并且set到Request对象里面了, 但是 Session 对象还没有触发创建, 最多也就是查找一下 jsessionId 对应的 Session 在 Manager 里面是否存在。

## tomcat session 设计分析

tomcat session 组件图如下所示，其中 `Context` 对应一个 webapp 应用，每个 webapp 有多个 `HttpSessionListener`， 并且每个应用的 session 是独立管理的，而 session 的创建、销毁由 `Manager` 组件完成，它内部维护了 N 个 `Session` 实例对象。在前面的文章中，我们分析了 `Context` 组件，它的默认实现是 `StandardContext`，它与 `Manager` 是一对一的关系，`Manager` 创建、销毁会话时，需要借助 `StandardContext` 获取 `HttpSessionListener` 列表并进行事件通知，而 `StandardContext` 的后台线程会对 `Manager` 进行过期 Session 的清理工作

![1609896968982](assets/1609896968982.png)

`org.apache.catalina.Manager` 接口的主要方法如下所示，它提供了 `Context`、`org.apache.catalina.SessionIdGenerator`的 getter/setter 接口，以及创建、添加、移除、查找、遍历 `Session` 的 API 接口，此外还提供了 `Session` 持久化的接口（load/unload） 用于加载/卸载会话信息，当然持久化要看不同的实现类

```java
public interface Manager {
    public Context getContext();
    public void setContext(Context context);
    public SessionIdGenerator getSessionIdGenerator();
    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator);
    public void add(Session session);
    public void addPropertyChangeListener(PropertyChangeListener listener);
    public void changeSessionId(Session session);
    public void changeSessionId(Session session, String newId);
    public Session createEmptySession();
    public Session createSession(String sessionId);
    public Session findSession(String id) throws IOException;
    public Session[] findSessions();
    public void remove(Session session);
    public void remove(Session session, boolean update);
    public void removePropertyChangeListener(PropertyChangeListener listener);
    public void unload() throws IOException;
    public void backgroundProcess();
    public boolean willAttributeDistribute(String name, Object value);
}
```

tomcat8.5 提供了 4 种实现，默认使用 `StandardManager`，tomcat 还提供了集群会话的解决方案，但是在实际项目中很少运用

- StandardManager：Manager 默认实现，在内存中管理 session，宕机将导致 session 丢失；但是当调用 Lifecycle 的 start/stop 接口时，将采用 jdk 序列化保存 Session 信息，因此当 tomcat 发现某个应用的文件有变更进行 reload 操作时，这种情况下不会丢失 Session 信息
- DeltaManager：增量 Session 管理器，用于Tomcat集群的会话管理器，某个节点变更 Session 信息都会同步到集群中的所有节点，这样可以保证 Session 信息的实时性，但是这样会带来较大的网络开销
- BackupManager：用于 Tomcat 集群的会话管理器，与DeltaManager不同的是，某个节点变更 Session 信息的改变只会同步给集群中的另一个 backup 节点
- PersistentManager：当会话长时间空闲时，将会把 Session 信息写入磁盘，从而限制内存中的活动会话数量；此外，它还支持容错，会定期将内存中的 Session 信息备份到磁盘

我们来看下 `StandardManager` 的类图，它也是个 `Lifecycle` 组件，并且 `ManagerBase` 实现了主要的逻辑。

![1609897399483](assets/1609897399483.png)

## Tomcat 中 Session 的创建

经过上面的Cookie解析, 则若存在jsessionId的话, 则已经set到Request里面了, 那Session又是何时触发创建的呢? 主要还是代码**request.getSession()**, 看代码:

```java
public class SessionExample extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException  {
        HttpSession session = request.getSession();
        // other code......
    }
}
```

我们来看看getSession():

```java
// 获取 request 对应的 session
public HttpSession getSession() {
    // 这里就是 通过 managerBase.sessions 获取 Session
    Session session = doGetSession(true); 
    if (session == null) {
        return null;
    }
    return session.getSession();
}

// create 代表是否创建 StandardSession
protected Session doGetSession(boolean create) {              

    // There cannot be a session if no context has been assigned yet
    // 1. 检验 StandardContext
    if (context == null) {
        return (null);                                           
    }

    // Return the current session if it exists and is valid
     // 2. 校验 Session 的有效性
    if ((session != null) && !session.isValid()) {              
        session = null;
    }
    if (session != null) {
        return (session);
    }

    // Return the requested session if it exists and is valid
    Manager manager = null;
    if (context != null) {
        //拿到StandardContext 中对应的StandardManager，Context与 Manager 是一对一的关系
        manager = context.getManager();
    }
    if (manager == null)
     {
        return (null);      // Sessions are not supported
    }
    if (requestedSessionId != null) {
        try {        
            // 3. 通过 managerBase.sessions 获取 Session
            // 4. 通过客户端的 sessionId 从 managerBase.sessions 来获取 Session 对象
            session = manager.findSession(requestedSessionId);   
        } catch (IOException e) {
            session = null;
        }
         // 5. 判断 session 是否有效
        if ((session != null) && !session.isValid()) {          
            session = null;
        }
        if (session != null) {
            // 6. session access +1
            session.access();                                    
            return (session);
        }
    }

    // Create a new session if requested and the response is not committed
    // 7. 根据标识是否创建 StandardSession ( false 直接返回)
    if (!create) {
        return (null);                                           
    }
    // 当前的 Context 是否支持通过 cookie 的方式来追踪 Session
    if ((context != null) && (response != null) && context.getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE) && response.getResponse().isCommitted()) {
        throw new IllegalStateException
          (sm.getString("coyoteRequest.sessionCreateCommitted"));
    }

    // Attempt to reuse session id if one was submitted in a cookie
    // Do not reuse the session id if it is from a URL, to prevent possible
    // phishing attacks
    // Use the SSL session ID if one is present.
    // 8. 到这里其实是没有找到 session, 直接创建 Session 出来
    if (("/".equals(context.getSessionCookiePath()) && isRequestedSessionIdFromCookie()) || requestedSessionSSL ) {
        session = manager.createSession(getRequestedSessionId()); // 9. 从客户端读取 sessionID, 并且根据这个 sessionId 创建 Session
    } else {
        session = manager.createSession(null);
    }

    // Creating a new session cookie based on that session
    if ((session != null) && (getContext() != null)&& getContext().getServletContext().getEffectiveSessionTrackingModes().contains(SessionTrackingMode.COOKIE)) {
        // 10. 根据 sessionId 来创建一个 Cookie
        Cookie cookie = ApplicationSessionCookieConfig.createSessionCookie(context, session.getIdInternal(), isSecure());
        // 11. 最后在响应体中写入 cookie
        response.addSessionCookieInternal(cookie);              
    }

    if (session == null) {
        return null;
    }
    // 12. session access 计数器 + 1
    session.access();                                          
    return session;
}
```

我们看看 **manager.createSession(null****);**

```java
public abstract class ManagerBase extends LifecycleMBeanBase implements Manager {
    //Manager管理着当前Context的所有session
    protected Map<String, Session> sessions = new ConcurrentHashMap<>();
    @Override
    public Session findSession(String id) throws IOException {
        if (id == null) {
            return null;
        }
        //通过JssionId获取session
        return sessions.get(id);
    }
    
    public Session createSession(String sessionId) {
        // 1. 判断 单节点的 Session 个数是否超过限制
        if ((maxActiveSessions >= 0) && (getActiveSessions() >= maxActiveSessions)) {      
            rejectedSessions++;
            throw new TooManyActiveSessionsException(
                    sm.getString("managerBase.createSession.ise"),
                    maxActiveSessions);
        }

        // Recycle or create a Session instance
        // 创建一个 空的 session
        // 2. 创建 Session
        Session session = createEmptySession();                     

        // Initialize the properties of the new session and return it
        // 初始化空 session 的属性
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        // 3. StandardSession 最大的默认 Session 激活时间
        session.setMaxInactiveInterval(this.maxInactiveInterval); 
        String id = sessionId;
        // 若没有从 client 端读取到 jsessionId
        if (id == null) {      
            // 4. 生成 sessionId (这里通过随机数来生成)    
            id = generateSessionId();                              
        }
        //这里会将session存入Map<String, Session> sessions = new ConcurrentHashMap<>();
        session.setId(id);
        sessionCounter++;

        SessionTiming timing = new SessionTiming(session.getCreationTime(), 0);
        synchronized (sessionCreationTiming) {
            // 5. 每次创建 Session 都会创建一个 SessionTiming, 并且 push 到 链表 sessionCreationTiming 的最后
            sessionCreationTiming.add(timing); 
            // 6. 并且将 链表 最前面的节点删除        
            sessionCreationTiming.poll();                         
        }      
        // 那这个 sessionCreationTiming 是什么作用呢, 其实 sessionCreationTiming 是用来统计 Session的新建及失效的频率 (好像Zookeeper 里面也有这个的统计方式)    
        return (session);
    }
    
    @Override
    public void add(Session session) {
        //将创建的Seesion存入Map<String, Session> sessions = new ConcurrentHashMap<>();
        sessions.put(session.getIdInternal(), session);
        int size = getActiveSessions();
        if( size > maxActive ) {
            synchronized(maxActiveUpdateLock) {
                if( size > maxActive ) {
                    maxActive = size;
                }
            }
        }
    }
}

@Override
public void setId(String id) {
    setId(id, true);
}

@Override
public void setId(String id, boolean notify) {

    if ((this.id != null) && (manager != null))
        manager.remove(this);

    this.id = id;

    if (manager != null)
        manager.add(this);

    if (notify) {
        tellNew();
    }
}
```

其主要的步骤就是:

```
1. 若 request.Session != null, 则直接返回 (说明同一时刻之前有其他线程创建了Session, 并且赋值给了 request)
2. 若 requestedSessionId != null, 则直接通过 manager 来进行查找一下, 并且判断是否有效
3. 调用 manager.createSession 来创建对应的Session，并将Session存入Manager的Map中
4. 根据 SessionId 来创建 Cookie, 并且将 Cookie 放到 Response 里面
5. 直接返回 Session
```

## Session清理

### Background 线程

前面我们分析了 Session 的创建过程，而 Session 会话是有时效性的，下面我们来看下 tomcat 是如何进行失效检查的。在分析之前，我们先回顾下 `Container` 容器的 Background 线程。

tomcat 所有容器组件，都是继承至 `ContainerBase` 的，包括 `StandardEngine`、`StandardHost`、`StandardContext`、`StandardWrapper`，而 `ContainerBase` 在启动的时候，如果 `backgroundProcessorDelay` 参数大于 0 则会开启 `ContainerBackgroundProcessor` 后台线程，调用自己以及子容器的 `backgroundProcess` 进行一些后台逻辑的处理，和 `Lifecycle` 一样，这个动作是具有传递性的，也就

![1609898392467](assets/1609898392467.png)

关键代码如下所示：

```java
ContainerBase.java

protected synchronized void startInternal() throws LifecycleException {
    // other code......
    // 开启ContainerBackgroundProcessor线程用于处理子容器，默认情况下backgroundProcessorDelay=-1，不会启用该线程
    threadStart();
}

protected class ContainerBackgroundProcessor implements Runnable {
    public void run() {
        // threadDone 是 volatile 变量，由外面的容器控制
        while (!threadDone) {
            try {
                Thread.sleep(backgroundProcessorDelay * 1000L);
            } catch (InterruptedException e) {
                // Ignore
            }
            if (!threadDone) {
                processChildren(ContainerBase.this);
            }
        }
    }

    protected void processChildren(Container container) {
        container.backgroundProcess();
        Container[] children = container.findChildren();
        for (int i = 0; i < children.length; i++) {
            // 如果子容器的 backgroundProcessorDelay 参数小于0，则递归处理子容器
            // 因为如果该值大于0，说明子容器自己开启了线程处理，因此父容器不需要再做处理
            if (children[i].getBackgroundProcessorDelay() <= 0) {
                processChildren(children[i]);
            }
        }
    }
}
```

### Session 检查

`backgroundProcessorDelay` 参数默认值为 `-1`，单位为秒，即默认不启用后台线程，而 tomcat 的 Container 容器需要开启线程处理一些后台任务，比如监听 jsp 变更、tomcat 配置变动、Session 过期等等，因此 `StandardEngine` 在构造方法中便将 `backgroundProcessorDelay` 参数设为 10（当然可以在 `server.xml` 中指定该参数），即每隔 10s 执行一次。那么这个线程怎么控制生命周期呢？我们注意到 `ContainerBase` 有个 `threadDone` 变量，用 `volatile` 修饰，如果调用 Container 容器的 stop 方法该值便会赋值为 false，那么该后台线程也会退出循环，从而结束生命周期。另外，有个地方需要注意下，父容器在处理子容器的后台任务时，需要判断子容器的 `backgroundProcessorDelay` 值，只有当其小于等于 0 才进行处理，因为如果该值大于0，子容器自己会开启线程自行处理，这时候父容器就不需要再做处理了

前面分析了容器的后台线程是如何调度的，下面我们重点来看看 webapp 这一层，以及 `StandardManager` 是如何清理过期会话的。`StandardContext` 重写了 `backgroundProcess` 方法，除了对子容器进行处理之外，还会对一些缓存信息进行清理，关键代码如下所示：

```java
StandardContext.java

@Override
public void backgroundProcess() {
    if (!getState().isAvailable())
        return;
    // 热加载 class，或者 jsp
    Loader loader = getLoader();
    if (loader != null) {
        loader.backgroundProcess();
    }
    // 清理过期Session
    Manager manager = getManager();
    if (manager != null) {
        manager.backgroundProcess();
    }
    // 清理资源文件的缓存
    WebResourceRoot resources = getResources();
    if (resources != null) {
        resources.backgroundProcess();
    }
    // 清理对象或class信息缓存
    InstanceManager instanceManager = getInstanceManager();
    if (instanceManager instanceof DefaultInstanceManager) {
        ((DefaultInstanceManager)instanceManager).backgroundProcess();
    }
    // 调用子容器的 backgroundProcess 任务
    super.backgroundProcess();
}
```

`StandardContext` 重写了 `backgroundProcess` 方法，在调用子容器的后台任务之前，还会调用 `Loader`、`Manager`、`WebResourceRoot`、`InstanceManager` 的后台任务，这里我们只关心 `Manager` 的后台任务。弄清楚了 `StandardManager` 的来龙去脉之后，我们接下来分析下具体的逻辑。

`StandardManager` 继承至 `ManagerBase`，它实现了主要的逻辑，关于 Session 清理的代码如下所示。backgroundProcess 默认是每隔10s调用一次，但是在 `ManagerBase` 做了取模处理，默认情况下是 60s 进行一次 Session 清理。tomcat 对 Session 的清理并没有引入时间轮，因为对 Session 的时效性要求没有那么精确，而且除了通知 `SessionListener`。

```java
ManagerBase.java

public void backgroundProcess() {
    // processExpiresFrequency 默认值为 6，而backgroundProcess默认每隔10s调用一次，也就是说除了任务执行的耗时，每隔 60s 执行一次
    count = (count + 1) % processExpiresFrequency;
    if (count == 0) // 默认每隔 60s 执行一次 Session 清理
        processExpires();
}

/**
 * 单线程处理，不存在线程安全问题
 */
public void processExpires() {
    long timeNow = System.currentTimeMillis();
    Session sessions[] = findSessions();    // 获取所有的 Session
    int expireHere = 0 ;
    for (int i = 0; i < sessions.length; i++) {
        // Session 的过期是在 isValid() 里面处理的
        if (sessions[i]!=null && !sessions[i].isValid()) {
            expireHere++;
        }
    }
    long timeEnd = System.currentTimeMillis();
    // 记录下处理时间
    processingTime += ( timeEnd - timeNow );
}
```

### 清理过期 Session

在上面的代码，我们并没有看到太多的过期处理，只是调用了 `sessions[i].isValid()`，原来清理动作都在这个方法里面处理的，相当的隐晦。在 `StandardSession#isValid()` 方法中，如果 `now - thisAccessedTime >= maxInactiveInterval`则判定当前 Session 过期了，而这个 `thisAccessedTime` 参数在每次访问都会进行更新

```java
public boolean isValid() {
    // other code......
    // 如果指定了最大不活跃时间，才会进行清理，这个时间是 Context.getSessionTimeout()，默认是30分钟
    if (maxInactiveInterval > 0) {
        int timeIdle = (int) (getIdleTimeInternal() / 1000L);
        if (timeIdle >= maxInactiveInterval) {
            expire(true);
        }
    }
    return this.isValid;
}
```

而 `expire` 方法处理的逻辑较繁锁，下面我用伪代码简单地描述下核心的逻辑，由于这个步骤可能会有多线程进行操作，因此使用 `synchronized` 对当前 Session 对象加锁，还做了双重校验，避免重复处理过期 Session。它还会向 Container 容器发出事件通知，还会调用 `HttpSessionListener` 进行事件通知，这个也就是我们 web 应用开发的 `HttpSessionListener` 了。由于 `Manager` 中维护了 `Session` 对象，因此还要将其从 `Manager` 移除。Session 最重要的功能就是存储数据了，可能存在强引用，而导致 Session 无法被 gc 回收，因此还要移除内部的 key/value 数据。由此可见，tomcat 编码的严谨性了，稍有不慎将可能出现并发问题，以及出现内存泄露

```java
public void expire(boolean notify) {
    //1、校验 isValid 值，如果为 false 直接返回，说明已经被销毁了
    synchronized (this) {   // 加锁
        //2、双重校验 isValid 值，避免并发问题
        Context context = manager.getContext();
        if (notify) {   
            Object listeners[] = context.getApplicationLifecycleListeners();
            HttpSessionEvent event = new HttpSessionEvent(getSession());
            for (int i = 0; i < listeners.length; i++) {
            //3、判断是否为 HttpSessionListener，不是则继续循环
            //4、向容器发出Destory事件，并调用 HttpSessionListener.sessionDestroyed() 进行通知
            context.fireContainerEvent("beforeSessionDestroyed", listener);
            listener.sessionDestroyed(event);
            context.fireContainerEvent("afterSessionDestroyed", listener);
        }
        //5、从 manager 中移除该  session
        //6、向 tomcat 的 SessionListener 发出事件通知，非 HttpSessionListener
        //7、清除内部的 key/value，避免因为强引用而导致无法回收 Session 对象
    }
}
```

由前面的分析可知，tomcat 会根据时间戳清理过期 Session，那么 tomcat 又是如何更新这个时间戳呢？ tomcat 在处理完请求之后，会对 `Request` 对象进行回收，并且会对 Session 信息进行清理，而这个时候会更新 `thisAccessedTime`、`lastAccessedTime` 时间戳。此外，我们通过调用 `request.getSession()` 这个 API 时，在返回 Session 时会调用 `Session#access()` 方法，也会更新 `thisAccessedTime` 时间戳。这样一来，每次请求都会更新时间戳，可以保证 Session 的鲜活时间。

```java
org.apache.catalina.connector.Request.java

protected void recycleSessionInfo() {
    if (session != null) {  
        session.endAccess();    // 更新时间戳
    }
    // 回收 Request 对象的内部信息
    session = null;
    requestedSessionCookie = false;
    requestedSessionId = null;
    requestedSessionURL = false;
    requestedSessionSSL = false;
}
```

**org.apache.catalina.session.StandardSession.java**

```java
public void endAccess() {
    isNew = false;
    if (LAST_ACCESS_AT_START) {     // 可以通过系统参数改变该值，默认为false
        this.lastAccessedTime = this.thisAccessedTime;
        this.thisAccessedTime = System.currentTimeMillis();
    } else {
        this.thisAccessedTime = System.currentTimeMillis();
        this.lastAccessedTime = this.thisAccessedTime;
    }
}

public void access() {
    this.thisAccessedTime = System.currentTimeMillis();
}
```

