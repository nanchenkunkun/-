# 典型回答

在Java 9之前，字符串内部是由字符数组char[] 来表示的。

```java
    /** The value is used for character storage. */
    private final char value[];
```

由于Java内部使用UTF-16，每个char占据两个字节，即使某些字符可以用一个字节（LATIN-1）表示，但是也仍然会占用两个字节。所以，JDK 9就对他做了优化。

这就是Java 9引入了"**Compact String**"的概念：

每当我们创建一个字符串时，如果它的所有字符都可以用单个字节（Latin-1）表示，那么将会在内部使用字节数组来保存一半所需的空间，但是如果有一个字符需要超过8位来表示，Java将继续使用UTF-16与字符数组。

> Latin1（又称ISO 8859-1）是一种字符编码格式，用于表示西欧语言，包括英语、法语、德语、西班牙语、葡萄牙语、意大利语等。它由国际标准化组织（ISO）定义，并涵盖了包括ASCII在内的128个字符。
> **Latin1编码使用单字节编码方案，也就是说每个字符只占用一个字节**，其中第一位固定为0，后面的七位可以表示128个字符。这样，Latin1编码可以很方便地与ASCII兼容。


那么，问题来了 ，所有字符串操作时，它如何区分到底用Latin-1还是UTF-16表示呢？

为了解决这个问题，对String的内部实现进行了另一个更改。引入了一个名为coder的字段，用于保存这些信息。

```java
/**
 * The value is used for character storage.
 *
 * @implNote This field is trusted by the VM, and is a subject to
 * constant folding if String instance is constant. Overwriting this
 * field after construction will cause problems.
 *
 * Additionally, it is marked with {@link Stable} to trust the contents
 * of the array. No other facility in JDK provides this functionality (yet).
 * {@link Stable} is safe here, because value is never null.
 */
@Stable
private final byte[] value;

/**
 * The identifier of the encoding used to encode the bytes in
 * {@code value}. The supported values in this implementation are
 *
 * LATIN1
 * UTF16
 *
 * @implNote This field is trusted by the VM, and is a subject to
 * constant folding if String instance is constant. Overwriting this
 * field after construction will cause problems.
 */
private final byte coder;
```


coder字段的取值可以是以下两种

```java
static final byte LATIN1 = 0;
static final byte UTF16 = 1;
```

在很多字符串的相关操作中都需要做一下判断，如：

```java
public int indexOf(int ch, int fromIndex) {
    return isLatin1() 
      ? StringLatin1.indexOf(value, ch, fromIndex) 
      : StringUTF16.indexOf(value, ch, fromIndex);
}  

private boolean isLatin1() {
    return COMPACT_STRINGS && coder == LATIN1;
}
```
