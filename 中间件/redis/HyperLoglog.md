## Hyper LogLog

​		Hyper LogLog 计数器的名称是具有自描述性的。 你可以仅仅使用`loglog(Nmax)+ O(1)`位来估计基数为 Nmax 的集合的基数。

## Redis Hyperloglog 操作

要进行 Redis Hyperloglog 的操作，我们可以使用以下三个命令：

- `PFADD`
- `PFCOUNT`
- `PFMERGE`

我们用一个实际的例子来解释这些命令。比如，有这么个场景，用户登录到系统，我们需要在一小时内统计不同的用户。 因此，我们需要一个 key，例如 USER:LOGIN:2019092818。 换句话说，我们要统计在 2019 年 09 月 28 日下午 18 点至 19 点之间发生用户登录操作的非重复用户数。对于将来的时间，我们也需要使用对应的 key 进行表示，比如 2019111100、2019111101、2019111102 等。

我们假设，用户 A、B、C、D、E 和 F 在下午 18 点至 19 点之间登录了系统。

```
127.0.0.1:6379> pfadd USER:LOGIN:2019092818 A
(integer) 1
127.0.0.1:6379> pfadd USER:LOGIN:2019092818 B C D E F
(integer) 1
127.0.0.1:6379>
```

当进行计数时，你会得到预期的 6。

```
127.0.0.1:6379> pfcount USER:LOGIN:2019092818
(integer) 6
```

如果 A 和 B 在这个时间内多次登录系统，你也将得到相同的结果，因为我们仅保留不同的用户。

```
127.0.0.1:6379> pfadd USER:LOGIN:2019092818 A B
(integer) 0
127.0.0.1:6379> pfcount USER:LOGIN:2019092818
(integer) 6
```

如果用户 A~F 和另外一个其他用户 G 在下午 19 点至下午 20 点之间登录系统：

```
127.0.0.1:6379> pfadd USER:LOGIN:2019092819 A B C D E F G
(integer) 1
127.0.0.1:6379> pfcount USER:LOGIN:2019092819
(integer) 7
```

现在，我们有两个键 USER:LOGIN:2019092818 和 USER:LOGIN:2019092819，如果我们想知道在 18 点到 20 点（2 小时）之间有多少不同的用户登录到系统中，我们可以直接使用`pfcount`命令对两个键进行合并计数：

```
127.0.0.1:6379> pfcount USER:LOGIN:2019092818 USER:LOGIN:2019092819
(integer) 7
```

如果我们需要保留键值而避免一遍又一遍地计数，那么我们可以将键合并为一个键 USER:LOGIN:2019092818-19，然后直接对该键进行`pfcount`操作，如下所示。

```
127.0.0.1:6379> pfmerge USER:LOGIN:2019092818-19 USER:LOGIN:2019092818 USER:LOGIN:2019092819
OK
127.0.0.1:6379> pfcount USER:LOGIN:2019092818-19
(integer) 7
```

