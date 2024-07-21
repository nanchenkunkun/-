# 典型回答
这是个典型的 top K问题，如果从海量数据查找最大的 k 个值，并且是内存有限的话，可以使用小顶堆（如果内存无限，可以直接用大顶堆）。如以下操作步骤：

1. 限制堆的大小：设定堆的最大容量为 𝑘，其中 𝑘 是你想要找出的最大元素的个数
2. 添加元素：每次向堆中添加一个新元素时，首先比较它是否大于堆顶元素（即堆中最小的元素）。
   - 如果新元素比堆顶元素大，将堆顶元素弹出，然后将新元素添加到堆中。
   - 如果新元素不大于堆顶元素，直接丢弃该元素。
3. 维持堆的性质：通过上述操作，堆始终保持最小的 k 个最大元素。堆顶元素是这些元素中最小的，堆中其他元素是比堆顶元素大的值。
4. 结果查询：当需要查询最大元素时，只需查看堆中的元素。如果 𝑘=1，直接返回堆顶元素即可；如果 𝑘>1，则堆中所有元素都是最大的 𝑘 个元素中的一部分。


假设我们有以下这些数字：5,15,1,3,10,20,2。找出最最大的三个元素。

1. 初始化一个最大容量为3的小顶堆。这个堆会存储当前遇到的最大的3个数字。
- 添加第一个元素（5）：堆现在包含 5。
- 添加第二个元素（15）：堆现在包含 5,15。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1715488917177-1348a6f7-85a3-49a5-9a3e-3a4261afeb21.png#averageHue=%23fbfbfb&clientId=u603cc7b2-6630-4&from=paste&height=291&id=u125abfcb&originHeight=424&originWidth=626&originalType=binary&ratio=1&rotation=0&showTitle=false&size=24310&status=done&style=none&taskId=u6659689d-9754-4e17-9cc4-3d42799be69&title=&width=429)

- 添加第三个元素（1）：1 小于堆顶元素（5），所以丢弃1。堆保持不变 5,15。
- 添加第四个元素（3）：3 小于堆顶元素（5），所以丢弃3。堆保持不变 5,15。
- 添加第五个元素（10）：堆现在包含 5,15,10。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1715488983962-dfdab57c-61db-4622-8bd8-317cd8c4d9a1.png#averageHue=%23f8f8f8&clientId=u603cc7b2-6630-4&from=paste&height=289&id=uf173acce&originHeight=464&originWidth=558&originalType=binary&ratio=1&rotation=0&showTitle=false&size=31525&status=done&style=none&taskId=udbf82e9a-6a67-4815-b5b1-319b8efc8e4&title=&width=347)

- 添加第六个元素（20）：20 大于堆顶元素（5），移除5，添加20。堆更新为 10,15,20。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1715489019281-08d397bb-ddd9-42b9-a151-8eb9c275637f.png#averageHue=%23fafafa&clientId=u603cc7b2-6630-4&from=paste&height=281&id=uda88b231&originHeight=470&originWidth=938&originalType=binary&ratio=1&rotation=0&showTitle=false&size=37538&status=done&style=none&taskId=uf370fe78-9756-446c-bdb3-8a87a263e3c&title=&width=560)

- 添加第七个元素（2）：2 小于堆顶元素（10），所以丢弃2。堆保持不变 10,15,20。

处理结束：堆中的元素 10,15,20 就是整个数据集中最大的3个数。

使用小顶堆的一个显著优势是内存效率。当处理非常大的数据集，但只关心顶部 k 个最大元素时，小顶堆只需维护 k 个元素的大小，而大顶堆则需要存储整个数据集来保持堆结构，这在数据量极大时非常耗费内存。

对于动态数据流（即不断有新数据加入），小顶堆允许更高效地维护最大 𝑘 个元素。每次插入操作只需 𝑂(log⁡𝑘)的时间复杂度，而如果使用大顶堆，每次更新可能需要重新处理更多的数据。

小顶堆适用于“Top K”问题，即从大量元素中找出最大的 k 个元素。
