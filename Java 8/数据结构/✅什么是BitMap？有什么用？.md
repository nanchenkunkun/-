# 典型回答

位图（BitMap），基本思想就是用一个bit来标记元素，bit是计算机中最小的单位，也就是我们常说的计算机中的0和1，这种就是用一个位来表示的。

所谓位图，其实就是一个bit数组，即每一个位置都是一个bit，其中的取值可以是0或者1

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1684394416334-19381463-1f61-4f08-bb1c-6f904070c44d.png#averageHue=%23f7f7f7&clientId=u7dfec50b-61cd-4&from=paste&height=165&id=u1aa24ce0&originHeight=160&originWidth=651&originalType=binary&ratio=2&rotation=0&showTitle=false&size=5957&status=done&style=none&taskId=u9c875eaa-e1a2-4bb7-a00e-850b8353d88&title=&width=669.5)<br />像上面的这个位图，可以用来表示1，,4，6：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1684394525006-a77cfaae-23d1-46e2-85b5-e0a8aa161391.png#averageHue=%23f9f9f9&clientId=u7dfec50b-61cd-4&from=paste&height=236&id=u2d86efd9&originHeight=251&originWidth=692&originalType=binary&ratio=2&rotation=0&showTitle=false&size=9523&status=done&style=none&taskId=u37ba96cd-283e-4b7d-bcb7-fd50fc2f1d4&title=&width=651)<br />如果不用位图的话，我们想要记录1，4，,6 这三个整型的话，就需要用三个unsigned int，已知每个unsigned int占4个字节，那么就是3*4 = 12个字节，一个字节有8 bit，那么就是 12*8 = 96 个bit。

所以，**位图最大的好处就是节省空间。**

位图有很多种用途，特别适合用在去重、排序等场景中，著名的布隆过滤器就是基于位图实现的。

[✅什么是布隆过滤器，实现原理是什么？](https://www.yuque.com/hollis666/fo22bm/gp9ymie1n39uavah?view=doc_embed)


但是位图也有着一定的限制，那就是他只能表示0和1，无法存储其他的数字。所以他只适合这种能表示true or false的场景。
# 知识扩展
## 什么是BitSet
[✅Set是如何保证元素不重复的](https://www.yuque.com/hollis666/fo22bm/iyr09c?view=doc_embed&inner=deT38)

