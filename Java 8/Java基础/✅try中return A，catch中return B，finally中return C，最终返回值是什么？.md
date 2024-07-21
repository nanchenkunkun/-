# 典型回答

最终的返回值将会是C！

因为finally块总是在try和catch块之后执行，无论是否有异常发生。如果finally块中有一个return语句，它将覆盖try块和catch块中的任何return语句。

```java
//无异常情况
public static String getValue(){
    try{
        return "A";
    }catch (Exception e){
        return "B";
    }finally {
        return "C";
    }
}

//有异常情况
public static String getValue(){
    try{
        System.out.println(1/0);
        return "A";
    }catch (Exception e){
        return "B";
    }finally {
        return "C";
    }
}
```

所以在这种情况下，无论try和catch块的执行情况如何，finally块中的return C;总是最后执行的语句，并且其返回值将是整个代码块的返回值。

这个问题还有一个兄弟问题，那就是如下代码得到的结果是什么：

```java
public static void getValue() {

    int i = 0;

    try {
        i = 1;
    } catch (Exception e) {
        i = 2;
    } finally {
        i = 3;
    }
    System.out.println(i);
}
```

原理和上面的是一样的，最终输出内容为3。

# 扩展知识

## finally和return的关系

很多时候，我们的一个方法会通过return返回一个值，那么如以下代码：

```java
public static int getValue() {

    int i = 1;

    try {
         i++;
         return i;
    } catch (Exception e) {
        i = 66;
    } finally {
        i = 100;
    }

    return i;
}
```

这个代码得到的结果是2，try-catch-finally的执行顺序是try->finally或者try-catch-finally，然后在执行每一个代码块的过程中，如果遇到return那么就会把当前的结果暂存，然后再执行后面的代码块，然后再把之前暂存的结果返回回去。

所以以上代码，会先把i++即2的结果暂存，然后执行i=100，接着再把2返回。

但是，在执行后续的代码块过程中，如果遇到了新的return，那么之前的暂存结果就会被覆盖。如：

```java
public static int getValue() {

    int i = 1;

    try {
         i++;
         return i;
    } catch (Exception e) {
        i = 66;
    } finally {
        i = 100;
        return i;
    }
}
```

以上代码方法得到的结果是100，是因为在finally中遇到了一个新的return，就会把之前的结果给覆盖掉。

如果代码出现异常也同理：

```java
public static int getValue() {

    int i = 1;

    try {
        i++;
        System.out.println(1 / 0);
        return i;
    } catch (Exception e) {
        i = 66;
        return i;
    } finally {
        i = 100;
        return i;
    }
}
```

在try中出现一个异常之后，会执行catch，在执行finally，最终得到100。如果没有finally：

```java
public static int getValue() {

    int i = 1;

    try {
        i++;
        System.out.println(1 / 0);
        return i;
    } catch (Exception e) {
        i = 66;
        return i;
    } 
}
```

那么得到的结果将是66。

**所以，如果finally块中有return语句，则其返回值将是整个try-catch-finally结构的返回值。如果finally块中没有return语句，则try或catch块中的return语句（取决于哪个执行了）将确定最终的返回值。**

