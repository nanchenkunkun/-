# 典型回答
# 扩展知识

下面是JDK 1.8中HashMap的remove方法的简要实现过程：

1. 首先，remove方法会计算键的哈希值，并通过哈希值计算出在数组中的索引位置。
2. 如果该位置上的元素为空，说明没有找到对应的键值对，直接返回null。
3. 如果该位置上的元素不为空，检查是否与当前键相等，如果相等，那么将该键值对删除，并返回该键值对的值。
4. 如果该位置上的元素不为空，但也与当前键不相等，那么就需要在链表或红黑树中继续查找。
5. 遍历链表或者红黑树，查找与当前键相等的键值对，找到则将该键值对删除，并返回该键值对的值，否则返回null。

## 源码解读
```
public V remove(Object key) {
  Node<K, V> e;
  return (e = removeNode(hash(key), key, null, false, true)) == null ?
  null : e.value;
}
```

重点还是来看下 removeNode 方法：

```
  /**
     * Implements Map.remove and related methods.
     *
     * @param hash       hash 值
     * @param key        key 值
     * @param value      value 值
     * @param matchValue 是否需要值匹配 false 表示不需要
     * @param movable    不用管
     * @return the node, or null if none
     */
    final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        //当前HashMap 中的散列表的引用
        Node<K, V>[] tab;
        //p：表示当前的Node元素
        Node<K, V> p;
        // n：table 的长度
        // index：桶的下标位置
        int n, index;
        //(tab = table) != null && (n = tab.length) > 0 条件成立，说明table不为空（table 为空就没必要执行了）
        // p = tab[index = (n - 1) & hash]) != null 将定位到的捅位的元素赋值给 p ，并判断定位到的元素不为空
        if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
            //进到 if 里面来了，说明已经定位到元素了
            //node：保存查找到的结果
            //e：表示当前元素的下一个元素
            Node<K, V> node = null, e;
            K k;
            V v;
            // 该条件如果成立，说明当前的元素就是要找的结果（这是最简单的情况，这个是很好理解的）
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
                node = p;
            }
            //到这一步，如果 (e = p.next) != null 说明该捅位找到的元素可能是链表或者是树，需要继续判断
            else if ((e = p.next) != null) {
                //树，不考虑
                if (p instanceof TreeNode) {
                    node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
                }
                //处理链表的情况
                else {
                    do {
                        //如果条件成立，说明已经匹配到了元素，直接将查找到的元素赋值给 node，并跳出循环（总体还是很好理解的）
                        if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        //将正在遍历的当前的临时元素 e 赋值给 p
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            // node != null 说明匹配到了元素
            //matchValue为false ，所以!matchValue  = true，后面的条件直接不用看了
            if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                //树，不考虑
                if (node instanceof TreeNode) {
                    ((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
                }
                // 这种情况是上面的最简单的情况
                else if (node == p) {
                    //直接将当前节点的下一个节点放在当前的桶位置（注意不是下一个桶位置，是该桶位置的下一个节点）
                    tab[index] = node.next;
                } else {
                    //说明定位到的元素不是该桶位置的头元素了，那直接进行一个简单的链表的操作即可
                    p.next = node.next;
                }
                //移除和添加都属于结构的修改，需要同步自增 modCount 的值
                ++modCount;
                //table 中的元素个数减 1
                --size;
                //啥也没做，不用管
                afterNodeRemoval(node);
                //返回被移除的节点元素
                return node;
            }
        }
        //没有匹配到返回null 即可
        return null;
    }
```

我想对你说的话都在注释里面了，亲一定要好好看哦。

另外 remove 还有一个方法是key 和 value 都需要匹配上才移除

```
 public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }
```

这个关键点就是这句话

```
//  (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))
//matchValue = true，所以 !matchValue = false,所以此时必须保证后面的值是true 才执行真正的 remove 操作
if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
  }
```


