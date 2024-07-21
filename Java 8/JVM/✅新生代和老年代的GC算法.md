三种垃圾回收算法，标记复制、标记清除、标记整理中，比较适合新生代的算法是标记复制算法。

[✅JVM有哪些垃圾回收算法？](https://www.yuque.com/hollis666/fo22bm/sinedm?view=doc_embed)

因为对于新生代来说，一般来说GC的次数是要比老年代高很多的，所以需要一个效率更高的算法，而且最好不要有碎片，因为很多对象都是需要先在新生代分配空间的，如果碎片太多的话，那么就会导致很多对象无法正常分配了。

所以，**新生代选择了标记复制算法进行垃圾回收**，但是标记复制算法存在一个缺点就是会浪费空间，新生代为了解决这个问题，把区域进一步细分成一个Eden区和两个Survivor区，同时工作的只有一个Eden区+一个Survivor区，这样，另外一个Survivor主要用来复制就可以了。只需要动态的调整Eden区和Survivor区的比例就可以降低空间浪费的问题。

**对于老年代来说，通常会采用标记整理算法**，虽然效率低了一点，但是可以减少空间的浪费并且不会有空间碎片等问题。在有些回收器上面，如CMS，为了降低STW的时长，也会采用**标记清除算法**。

[✅Java的堆是如何分代的？为什么分代？](https://www.yuque.com/hollis666/fo22bm/iop1msfpeny48x4c?view=doc_embed)

[✅新生代如果只有一个Eden+一个Survivor可以吗？](https://www.yuque.com/hollis666/fo22bm/eigm8iqgpwmd2eg8?view=doc_embed)


