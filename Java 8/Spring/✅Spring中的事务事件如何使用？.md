# 典型回答

`@TransactionalEventListener` 是 Spring Framework 提供的一个注解，用于处理事务事件。

它可以在事务提交前后（或回滚前后）触发事件监听器，以执行一些特定的操作。这个注解的使用场景是在需要基于事务状态执行后处理逻辑时非常有用。

Spring事件机制见：

[✅在Spring中如何使用Spring Event做事件驱动](https://www.yuque.com/hollis666/fo22bm/lgs78ulq6l3cg1qk?view=doc_embed)

上面链接介绍的Spring Event的使用方式是没有考虑事务的，如果要考虑在事务的过程中发送和处理事件，则可以用`@TransactionalEventListener`

如以下是一个简单的例子：

```yaml
@Service
public class UserService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerUser(User user) {
        // 用户注册逻辑

        // 发布用户注册事件
        UserRegistrationEvent registrationEvent = new UserRegistrationEvent(user);
        eventPublisher.publishEvent(registrationEvent);
    }
}

@Component
public class UserRegistrationEventListener {

    @TransactionalEventListener
    public void handleUserRegistrationEvent(UserRegistrationEvent event) {
        // 事务成功提交后执行的逻辑
        sendWelcomeEmail(event.getUser());
    }

    private void sendWelcomeEmail(User user) {
        // 发送欢迎邮件
    }
}

```

在上面的示例中，UserRegistrationEvent 事件在用户注册成功后发布，然后 UserRegistrationEventListener 中的 handleUserRegistrationEvent 方法在事务成功提交后触发，发送欢迎邮件。

`TransactionalEventListener` 默认只在成功提交的事务中触发事件监听器。如果需要在事务回滚后也触发，可以使用 phase 属性进行配置。

phase 属性用于指定事件监听器的触发时机，它有四种不同的阶段，分别是 BEFORE_COMMIT、AFTER_COMMIT、AFTER_ROLLBACK 和 AFTER_COMPLETION。它们的区别如下：

1. BEFORE_COMMIT：在事务提交前触发。事件监听器将在事务尚未提交时执行，这意味着它可以在事务内部进行回滚操作，如果事件监听器抛出异常，将导致事务回滚。这个阶段通常用于在事务即将提交前执行某些额外的逻辑。
2. AFTER_COMMIT：在事务成功提交后触发。事件监听器将在事务已成功提交后执行，这意味着它不会影响事务的回滚。这个阶段通常用于执行那些不应该导致事务回滚的后处理操作，如发送通知或记录日志。
3. AFTER_ROLLBACK：在事务回滚后触发。事件监听器将在事务回滚后执行，这通常用于清理或记录与事务回滚相关的操作。
4. AFTER_COMPLETION：在事务完成（不管是提交还是回滚）后触发。事件监听器将在事务完成后执行，无论事务是否成功提交或回滚。这个阶段通常用于执行一些与事务状态无关的清理工作。


