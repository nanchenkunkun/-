E – Element (在集合中使用，因为集合中存放的是元素)<br />T – Type（Java 类）<br />K – Key（键）<br />V – Value（值）<br />N – Number（数值类型）<br />？ – 表示不确定的java类型（无限制通配符类型）<br />S、U、V – 这几个有时候也有，这些字母本身没有特定的含义，它们只是代表某种未指定的类型。一般认为和T差不多。<br />Object – 是所有类的根类，任何类的对象都可以设置给该Object引用变量，使用的时候可能需要类型强制转换，但是用使用了泛型T、E等这些标识符后，在实际用之前类型就已经确定了，不需要再进行类型强制转换。


# 扩展知识

## 代码示例

```java
// 示例1：使用T作为泛型类型参数，表示任何类型
public class MyGenericClass<T> {
    private T myField;

    public MyGenericClass(T myField) {
        this.myField = myField;
    }

    public T getMyField() {
        return myField;
    }
}

// 示例2：使用K、V作为泛型类型参数，表示键值对中的键和值的类型
public class MyMap<K, V> {
    private List<Entry<K, V>> entries;

    public MyMap() {
        entries = new ArrayList<>();
    }

    public void put(K key, V value) {
        Entry<K, V> entry = new Entry<>(key, value);
        entries.add(entry);
    }

    public V get(K key) {
        for (Entry<K, V> entry : entries) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private class Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}

// 示例3：使用E作为泛型类型参数，表示集合中的元素类型
public class MyList<E> {
    private List<E> elements;

    public MyList() {
        elements = new ArrayList<>();
    }

    public void add(E element) {
        elements.add(element);
    }

    public E get(int index) {
        return elements.get(index);
    }
}

// 示例4：使用Object作为泛型类型参数，表示可以接受任何类型
public class MyGenericClass {
    private Object myField;

    public MyGenericClass(Object myField) {
        this.myField = myField;
    }

    public Object getMyField() {
        return myField;
    }
}

```
