# 典型回答

CMS（Concurrent Mark-Sweep）和G1（Garbage-First）是两种常见的收集器，它们都旨在减少应用程序停顿时间。他们的GC过程采用三色标记法，把整个GC过程分为了初始标记、并发标记、重新标记、以及垃圾清理。

其中初始标记和重新标记都是需要STW的，而并发标记则不需要。

[✅什么是STW？有什么影响？](https://www.yuque.com/hollis666/fo22bm/qg9fvqfnzpbd70hl?view=doc_embed)

在初始标记阶段，针对根（GCRoot）直接引用的对象进行标记，这个过程也通常被叫做根扫描。为了防止在初始标记过程中根对象被修改，这个过程是STW的，虽然G1可以通过采用写屏障技术（[https://www.yuque.com/hollis666/fo22bm/lva8a9gfhagbrw2g#CejOa](https://www.yuque.com/hollis666/fo22bm/lva8a9gfhagbrw2g#CejOa) ）来获知对象是否发生了修改，但是因为大多数的GCRoot他并不是对象，所以无法被获知的，所以，这个阶段是需要进行STW的。

GC Root都有哪些：<br />[✅JVM如何判断对象是否存活？](https://www.yuque.com/hollis666/fo22bm/zcd5ur?view=doc_embed&inner=tnBTG)

并发标记阶段。会标记所有从直接可达对象间接可达的对象，这个过程是不会STW的，也就是说用户的线程和GC的线程是并发执行的。这样可以最大限度的减少应用的停顿时间。

重新标记阶段，目的是修正并发标记阶段因应用程序继续运行而产生的任何变化（因为并发标记没有STW，所以会有变化）。此时，需要重新检查和更新那些在并发标记阶段可能发生变化的对象标记信息。重新标记是清理前的最后一次标记，需要确保这个过程的准确性，所以需要做STW来保证。

总之，三个阶段，为了提升性能肯定是能不STW就不STW，而最后一个阶段——重新标记因为是最终阶段，所以需要STW来确保准确性。而第一个阶段——初始标记，因为无法感知到GCRoot的变化，所以需要做STW来确保这个阶段的准确性。
