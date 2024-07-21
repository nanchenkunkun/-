

**YoungGC的触发条件比较简单，那就是当年轻代中的eden区分配满的时候就会触发。**

**FullGC的触发条件**比较复杂也比较多，主要以下几种：

- **老年代空间不足**
   - 创建一个大对象，超过指定阈值会直接保存在老年代当中，如果老年代空间也不足，会触发Full GC。
   - YoungGC之后，发现要移到老年代的对象，老年代存不下的时候，会触发一次FullGC
- **空间分配担保失败**(空间分配担保详见:[https://www.yuque.com/hollis666/fo22bm/eigm8iqgpwmd2eg8#l3Gjz](https://www.yuque.com/hollis666/fo22bm/eigm8iqgpwmd2eg8#l3Gjz))

   - 当准备要触发一次YoungGC时，会进行空间分配担保，在担保过程中，发现**虚拟机会检查老年代最大可用的连续空间小于新生代所有对象的总空间，但是HandlePromotionFailure=false**，那么就会触发一次FullGC（HandlePromotionFailure 这个配置，在JDK 7中并不在支持了，这一步骤在该版本已取消）
   - 当准备要触发一次YoungGC时，会进行空间分配担保，在担保过程中，发现**虚拟机会检查老年代最大可用的连续空间小于新生代所有对象的总空间，但是**HandlePromotionFailure=true，继续检查发现**老年代最大可用连续空间小于历次晋升到老年代的对象的平均大小时**，会触发一次FullGC
- **永久代空间不足**
   - 如果有永久代的话，当在永久代分配空间时没有足够空间的时候，会触发FullGC
- **代码中执行System.gc()**
   - 代码中执行System.gc()的时候，会触发FullGC，但是并不保证一定会立即触发。
