
背景：工作2年

面试问题：

1. 为什么找机会
2. 公司使用的技术栈是啥
3. 项目的最高QPS有多高
4. 遇到的开发问题有哪些？
5. 觉得自己从校招到现在有哪些提升
6. 项目-搭建的xx平台
7. 如何处理下游超时，一致性怎么保证

[✅如何基于本地消息表实现分布式事务？](https://www.yuque.com/hollis666/fo22bm/xm675quxo1bc5qm8?view=doc_embed)

8. RocketMq的特性

[✅RocketMQ的架构是怎么样的？](https://yuque.com/hollis666/fo22bm/fkx1hga7xlpbfbuv)

9. 如何用Redis统计亿级网站的uv

[✅除了做缓存，Redis还能用来干什么？](https://www.yuque.com/hollis666/fo22bm/gxqm60?view=doc_embed)

10. 遇到过什么线上问题，如何处理

[✅RocketMQ消费堆积问题排查](https://www.yuque.com/hollis666/fo22bm/za04hyyegpeg4h2i?view=doc_embed)

11. 分布式锁如何设计

[✅如何用SETNX实现分布式锁？](https://www.yuque.com/hollis666/fo22bm/feovxr7gr8ois5yt?view=doc_embed)

12. 分布式框架的服务发现怎么做

[✅Dubbo实现服务调用的过程是什么样的？](https://www.yuque.com/hollis666/fo22bm/io1pkwin43mkwaup?view=doc_embed)

13. TheadLocal的使用场景，如何防止内存泄露

[✅什么是ThreadLocal，如何实现的？](https://www.yuque.com/hollis666/fo22bm/ihoye3?view=doc_embed)

14. JVM的堆栈各存储什么内容

[✅JVM的运行时内存区域是怎样的？](https://www.yuque.com/hollis666/fo22bm/oyxrdhamqrmn291o?view=doc_embed)

15. 写过什么公共组件么？
16. 反问：用增营销和交易营销的区别、技术栈，生活 作息等
17. 写题：
```java
import java.util.Arrays;

public class Main {

    public static int[] findClosestElements(int[] arr, int k, int x) {
        //书写算法逻辑
        int left = 0, right = arr.length - 1;
        int mid = (left + right)/2;
        while(left < right) {
            if(arr[mid] > x) {
                right = mid;
            } else if(arr[mid] < x) {
                left = mid + 1;
            } else {
                break;
            }
            mid = (left + right)/2;
        }
        int lp = mid, rp = mid, count = 1;
        while(count < k) {
            if(rp > arr.length - 1) {
                lp --;
            }
            if(lp < 0) {
                rp ++;
            }
            if(x - arr[lp] > arr[rp] - x) { 
                rp ++;
                
            } else {
                lp --;
            }
            count ++;
        }
        int[] ans = new int[rp - lp + 1];
        for(int i = lp; i <= rp; i ++) { 
            ans[i - lp] = arr[i]; 
        }
        return ans;
    }

    // 获取到离x最近的k个数
    public static void main(String[] args) {
        int[] arr = {0, 0, 1, 2, 3, 3, 4, 7, 7, 8};
        int k = 3;
        int x = 5;
        int[] res = findClosestElements(arr, k, x);
        System.out.println(Arrays.toString(res));
    }
}

```
