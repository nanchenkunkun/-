## 生命周期机制Lifecycle

## 什么是Lifecycle?

Lifecycle，其实就是一个状态机，对组件的由生到死状态的管理。

- 当组件在`STARTING_PREP`、`STARTING`或`STARTED`时，调用`start()`方法没有任何效果
- 当组件在`NEW`状态时，调用`start()`方法会导致`init()`方法被立刻执行，随后`start()`方法被执行
- 当组件在`STOPPING_PREP`、`STOPPING`或`STOPPED`时，调用`stop()`方法没有任何效果
- 当一个组件在`NEW`状态时，调用`stop()`方法会将组件状态变更为`STOPPED`，比较典型的场景就是组件启动失败，其子组件还没有启动。当一个组件停止的时候，它将尝试停止它下面的所有子组件，即使子组件还没有启动。

## Lifecycle方法

我们看看Lifecycle有哪些方法，如下所示：

```
public interface Lifecycle {
    // 添加监听器
    public void addLifecycleListener(LifecycleListener listener);
    // 获取所以监听器
    public LifecycleListener[] findLifecycleListeners();
    // 移除某个监听器
    public void removeLifecycleListener(LifecycleListener listener);
    // 初始化方法
    public void init() throws LifecycleException;
    // 启动方法
    public void start() throws LifecycleException;
    // 停止方法，和start对应
    public void stop() throws LifecycleException;
    // 销毁方法，和init对应
    public void destroy() throws LifecycleException;
    // 获取生命周期状态
    public LifecycleState getState();
    // 获取字符串类型的生命周期状态
    public String getStateName();
}
```

## LifecycleBase

`LifecycleBase`是`Lifecycle`的基本实现。我们逐一来看Lifecycle的各个方法。

### 增加、删除和获取监听器

```
private final List<LifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

@Override
public void addLifecycleListener(LifecycleListener listener) {
    lifecycleListeners.add(listener);
}
@Override
public LifecycleListener[] findLifecycleListeners() {
    return lifecycleListeners.toArray(new LifecycleListener[0]);
}
@Override
public void removeLifecycleListener(LifecycleListener listener) {
    lifecycleListeners.remove(listener);
}
```

1. 生命周期监听器保存在一个线程安全的List中，`CopyOnWriteArrayList`。所以add和remove都是直接调用此List的相应方法。
2. findLifecycleListeners返回的是一个数组，为了线程安全，所以这儿会生成一个新数组。

### init()

```
@Override
public final synchronized void init() throws LifecycleException {
    // 非NEW状态，不允许调用init()方法
    if (!state.equals(LifecycleState.NEW)) {
        invalidTransition(Lifecycle.BEFORE_INIT_EVENT);
    }

    try {
        // 初始化逻辑之前，先将状态变更为`INITIALIZING`
        setStateInternal(LifecycleState.INITIALIZING, null, false);
        // 初始化，该方法为一个abstract方法，需要组件自行实现
        initInternal();
        // 初始化完成之后，状态变更为`INITIALIZED`
        setStateInternal(LifecycleState.INITIALIZED, null, false);
    } catch (Throwable t) {
        // 初始化的过程中，可能会有异常抛出，这时需要捕获异常，并将状态变更为`FAILED`
        ExceptionUtils.handleThrowable(t);
        setStateInternal(LifecycleState.FAILED, null, false);
        throw new LifecycleException(
                sm.getString("lifecycleBase.initFail",toString()), t);
    }
}
```

`setStateInternal`方法用于维护状态，同时在状态转换成功之后触发事件。为了状态的可见性，所以state声明为volatile类型的。

```
private volatile LifecycleState state = LifecycleState.NEW;。
private synchronized void setStateInternal(LifecycleState state,
        Object data, boolean check) throws LifecycleException {
    if (log.isDebugEnabled()) {
        log.debug(sm.getString("lifecycleBase.setState", this, state));
    }

    // 是否校验状态
    if (check) {
        // Must have been triggered by one of the abstract methods (assume
        // code in this class is correct)
        // null is never a valid state
        // state不允许为null
        if (state == null) {
            invalidTransition("null");
            // Unreachable code - here to stop eclipse complaining about
            // a possible NPE further down the method
            return;
        }

        // Any method can transition to failed
        // startInternal() permits STARTING_PREP to STARTING
        // stopInternal() permits STOPPING_PREP to STOPPING and FAILED to
        // STOPPING
        if (!(state == LifecycleState.FAILED ||
                (this.state == LifecycleState.STARTING_PREP &&
                        state == LifecycleState.STARTING) ||
                (this.state == LifecycleState.STOPPING_PREP &&
                        state == LifecycleState.STOPPING) ||
                (this.state == LifecycleState.FAILED &&
                        state == LifecycleState.STOPPING))) {
            // No other transition permitted
            invalidTransition(state.name());
        }
    }

    // 设置状态
    this.state = state;
    // 触发事件
    String lifecycleEvent = state.getLifecycleEvent();
    if (lifecycleEvent != null) {
        fireLifecycleEvent(lifecycleEvent, data);
    }
}
```

我们看看`fireLifecycleEvent`方法，

```
public void fireLifecycleEvent(String type, Object data) {
    // 事件监听,观察者模式的另一种方式
    LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
    LifecycleListener interested[] = listeners;// 监听器数组 关注 事件(启动或者关闭事件)
    // 循环通知所有生命周期时间侦听器
    for (int i = 0; i < interested.length; i++)
        // 每个监听器都有自己的逻辑
        interested[i].lifecycleEvent(event);
}
```

首先, 创建一个事件对象, 然通知所有的监听器发生了该事件.并做响应.

### start()

```
@Override
public final synchronized void start() throws LifecycleException {
    // `STARTING_PREP`、`STARTING`和`STARTED时，将忽略start()逻辑
    if (LifecycleState.STARTING_PREP.equals(state) || LifecycleState.STARTING.equals(state) ||
            LifecycleState.STARTED.equals(state)) {

        if (log.isDebugEnabled()) {
            Exception e = new LifecycleException();
            log.debug(sm.getString("lifecycleBase.alreadyStarted", toString()), e);
        } else if (log.isInfoEnabled()) {
            log.info(sm.getString("lifecycleBase.alreadyStarted", toString()));
        }

        return;
    }

    // `NEW`状态时，执行init()方法
    if (state.equals(LifecycleState.NEW)) {
        init();
    }

    // `FAILED`状态时，执行stop()方法
    else if (state.equals(LifecycleState.FAILED)) {
        stop();
    }

    // 不是`INITIALIZED`和`STOPPED`时，则说明是非法的操作
    else if (!state.equals(LifecycleState.INITIALIZED) &&
            !state.equals(LifecycleState.STOPPED)) {
        invalidTransition(Lifecycle.BEFORE_START_EVENT);
    }

    try {
        // start前的状态设置
        setStateInternal(LifecycleState.STARTING_PREP, null, false);
        // start逻辑，抽象方法，由组件自行实现
        startInternal();
        // start过程中，可能因为某些原因失败，这时需要stop操作
        if (state.equals(LifecycleState.FAILED)) {
            // This is a 'controlled' failure. The component put itself into the
            // FAILED state so call stop() to complete the clean-up.
            stop();
        } else if (!state.equals(LifecycleState.STARTING)) {
            // Shouldn't be necessary but acts as a check that sub-classes are
            // doing what they are supposed to.
            invalidTransition(Lifecycle.AFTER_START_EVENT);
        } else {
            // 设置状态为STARTED
            setStateInternal(LifecycleState.STARTED, null, false);
        }
    } catch (Throwable t) {
        // This is an 'uncontrolled' failure so put the component into the
        // FAILED state and throw an exception.
        ExceptionUtils.handleThrowable(t);
        setStateInternal(LifecycleState.FAILED, null, false);
        throw new LifecycleException(sm.getString("lifecycleBase.startFail", toString()), t);
    }
}
```

### stop()

```
@Override
public final synchronized void stop() throws LifecycleException {
    // `STOPPING_PREP`、`STOPPING`和STOPPED时，将忽略stop()的执行
    if (LifecycleState.STOPPING_PREP.equals(state) || LifecycleState.STOPPING.equals(state) ||
            LifecycleState.STOPPED.equals(state)) {

        if (log.isDebugEnabled()) {
            Exception e = new LifecycleException();
            log.debug(sm.getString("lifecycleBase.alreadyStopped", toString()), e);
        } else if (log.isInfoEnabled()) {
            log.info(sm.getString("lifecycleBase.alreadyStopped", toString()));
        }

        return;
    }

    // `NEW`状态时，直接将状态变更为`STOPPED`
    if (state.equals(LifecycleState.NEW)) {
        state = LifecycleState.STOPPED;
        return;
    }

    // stop()的执行，必须要是`STARTED`和`FAILED`
    if (!state.equals(LifecycleState.STARTED) && !state.equals(LifecycleState.FAILED)) {
        invalidTransition(Lifecycle.BEFORE_STOP_EVENT);
    }

    try {
        // `FAILED`时，直接触发BEFORE_STOP_EVENT事件
        if (state.equals(LifecycleState.FAILED)) {
            // Don't transition to STOPPING_PREP as that would briefly mark the
            // component as available but do ensure the BEFORE_STOP_EVENT is
            // fired
            fireLifecycleEvent(BEFORE_STOP_EVENT, null);
        } else {
            // 设置状态为STOPPING_PREP
            setStateInternal(LifecycleState.STOPPING_PREP, null, false);
        }

        // stop逻辑，抽象方法，组件自行实现
        stopInternal();

        // Shouldn't be necessary but acts as a check that sub-classes are
        // doing what they are supposed to.
        if (!state.equals(LifecycleState.STOPPING) && !state.equals(LifecycleState.FAILED)) {
            invalidTransition(Lifecycle.AFTER_STOP_EVENT);
        }
        // 设置状态为STOPPED
        setStateInternal(LifecycleState.STOPPED, null, false);
    } catch (Throwable t) {
        ExceptionUtils.handleThrowable(t);
        setStateInternal(LifecycleState.FAILED, null, false);
        throw new LifecycleException(sm.getString("lifecycleBase.stopFail",toString()), t);
    } finally {
        if (this instanceof Lifecycle.SingleUse) {
            // Complete stop process first
            setStateInternal(LifecycleState.STOPPED, null, false);
            destroy();
        }
    }
}
```

### destroy()

```
@Override
public final synchronized void destroy() throws LifecycleException {
    // `FAILED`状态时，直接触发stop()逻辑
    if (LifecycleState.FAILED.equals(state)) {
        try {
            // Triggers clean-up
            stop();
        } catch (LifecycleException e) {
            // Just log. Still want to destroy.
            log.warn(sm.getString(
                    "lifecycleBase.destroyStopFail", toString()), e);
        }
    }

    // `DESTROYING`和`DESTROYED`时，忽略destroy的执行
    if (LifecycleState.DESTROYING.equals(state) ||
            LifecycleState.DESTROYED.equals(state)) {

        if (log.isDebugEnabled()) {
            Exception e = new LifecycleException();
            log.debug(sm.getString("lifecycleBase.alreadyDestroyed", toString()), e);
        } else if (log.isInfoEnabled() && !(this instanceof Lifecycle.SingleUse)) {
            // Rather than have every component that might need to call
            // destroy() check for SingleUse, don't log an info message if
            // multiple calls are made to destroy()
            log.info(sm.getString("lifecycleBase.alreadyDestroyed", toString()));
        }

        return;
    }

    // 非法状态判断
    if (!state.equals(LifecycleState.STOPPED) &&
            !state.equals(LifecycleState.FAILED) &&
            !state.equals(LifecycleState.NEW) &&
            !state.equals(LifecycleState.INITIALIZED)) {
        invalidTransition(Lifecycle.BEFORE_DESTROY_EVENT);
    }

    try {
        // destroy前状态设置
        setStateInternal(LifecycleState.DESTROYING, null, false);
       // 抽象方法，组件自行实现
        destroyInternal();
        // destroy后状态设置
        setStateInternal(LifecycleState.DESTROYED, null, false);
    } catch (Throwable t) {
        ExceptionUtils.handleThrowable(t);
        setStateInternal(LifecycleState.FAILED, null, false);
        throw new LifecycleException(
                sm.getString("lifecycleBase.destroyFail",toString()), t);
    }
}
```

### 模板方法

从上述源码看得出来，`LifecycleBase`是使用了状态机+模板模式来实现的。模板方法有下面这几个：

```
// 初始化方法
protected abstract void initInternal() throws LifecycleException;
// 启动方法
protected abstract void startInternal() throws LifecycleException;
// 停止方法
protected abstract void stopInternal() throws LifecycleException;
// 销毁方法
protected abstract void destroyInternal() throws LifecycleException;
```

