# 典型回答
我们知道，在foreach的同时修改集合，会触发fail-fast机制，要避免fail-fast机制，有如下处理方案：

1. 通过普通的for循环（不建议，可能会漏删）
```java
public void listRemove() { 
    List<Student> students = this.getStudents(); 
    for (int i=0; i<students.size(); i++) { 
        if (students.get(i).getId()%3 == 0) { 
            Student student = students.get(i); 
            students.remove(student); 
            //做一次i--，避免漏删
            i--;
        } 
    } 
} 
```

2. 通过普通的for循环进行倒叙遍历（也能用）

上面的方式需要做i--避免漏删，还有个办法，那就是倒叙遍历也能避免这个问题：

```java
public void listRemove() { 
    List<Student> students = this.getStudents(); 
    for (int i = students.size() - 1; i >= 0; i--) { 
        if (students.get(i).getId()%3 == 0) { 
            Student student = students.get(i); 
            students.remove(student); 
        } 
    } 
} 
```

3. 使用迭代器循环（可以用）
```java
public void iteratorRemove() { 
    List<Student> students = this.getStudents(); 
    Iterator<Student> stuIter = students.iterator(); 
    while (stuIter.hasNext()) { 
        Student student = stuIter.next(); 
        if (student.getId() % 2 == 0) {
            //这里要使用Iterator的remove方法移除当前对象，如果使用List的remove方法，则同样会出现ConcurrentModificationException 
        	stuIter.remove();
        } 
    }
} 
```

4. 将原来的copy一份副本，遍历原来的list，然后删除副本（可以用，fail-safe的，但是比较复杂）
```java
public void copyRemove() {
    // 注意，这种方法的equals需要重写
	List<Student> students = this.getStudents();
    List<Student> studentsCopy = deepclone(students);
    for(Student stu : students) {
        if(needDel(stu)) {
            studentsCopy.remove(stu);
        }
    }
}
```

4. 使用并发安全的集合类（可以用，但是需要转成线程安全的集合）
```java
public void cowRemove() { 
    List<String> students = new CopyOnWriteArrayList<>(this.getStudents());
    for(Student stu : students) {
        if(needDel(stu)) {
            students.remove(stu);
        }
    }
}
```

5. 通过Stream的过滤方法，因为Stream每次处理后都会生成一个新的Stream，不存在并发问题，所以Stream的filter也可以修改list集合。（**建议，简单高效**）

```java
public List<String> streamRemove() { 
    List<String> students = this.getStudents();
    return students.stream()
        .filter(this::notNeedDel)
        .collect(Collectors.toList());
}

```

6. 通过removeIf方法，实现元素的过滤删除。从Java 8开始，List接口提供了removeIf方法用于删除所有满足特定条件的数组元素（**推荐**）

```java
arraylist.removeIf(this::needDel);
```
