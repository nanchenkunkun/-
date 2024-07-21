# 典型回答

所谓分片策略，其实就是我们提到的分表算法，就是说一个分表字段之后，基于什么样的策略或者算法来决策出数据应该去哪个库/表中进行读取或者写入。

[✅分表算法都有哪些？](https://www.yuque.com/hollis666/fo22bm/anpg4kfcb8p7egag?view=doc_embed)

已经有文章单独写过算法了，但是为啥要单独介绍一下所谓的 SharedingJDBC分片策略呢？

算法 、策略好像是一回事儿，其实真要较真的话，可以认为**分片策略 = 分片算法+分片键**

ShardingJDBC目前提供4种分片算法。包括了：

- **精确分片算法**
   - 对应PreciseShardingAlgorithm，用于处理使用单一键作为分片键的=与IN进行分片的场景。需要配合StandardShardingStrategy使用。
- **范围分片算法**
   - 对应RangeShardingAlgorithm，用于处理使用单一键作为分片键的BETWEEN AND进行分片的场景。需要配合StandardShardingStrategy使用。
- **复合分片算法**
   - 对应ComplexKeysShardingAlgorithm，**用于处理使用多键作为分片键进行分片的场景**，包含多个分片键的逻辑较复杂，需要应用开发者自行处理其中的复杂度。需要配合ComplexShardingStrategy使用。
- **Hint分片算法**
   - 对应HintShardingAlgorithm，用于处理使用Hint行分片的场景。需要配合HintShardingStrategy使用。

ShardingJDBC目前提供5种分片策略。包括了：

- **标准分片策略**
   - 对应StandardShardingStrategy。提供对SQL语句中的=, IN和BETWEEN AND的分片操作支持。StandardShardingStrategy只支持单分片键，提供PreciseShardingAlgorithm和RangeShardingAlgorithm两个分片算法。PreciseShardingAlgorithm是必选的，用于处理=和IN的分片。RangeShardingAlgorithm是可选的，用于处理BETWEEN AND分片，如果不配置RangeShardingAlgorithm，SQL中的BETWEEN AND将按照全库路由处理。
- **复合分片策略**
   - 对应ComplexShardingStrategy。复合分片策略。提供对SQL语句中的=, IN和BETWEEN AND的分片操作支持。ComplexShardingStrategy支持多分片键，由于多分片键之间的关系复杂，因此并未进行过多的封装，而是直接将分片键值组合以及分片操作符透传至分片算法，完全由应用开发者实现，提供最大的灵活度。
- **行表达式分片策略**
   - 对应InlineShardingStrategy。使用Groovy的表达式，提供对SQL语句中的=和IN的分片操作支持，只支持单分片键。对于简单的分片算法，可以通过简单的配置使用，从而避免繁琐的Java代码开发，
- **Hint分片策略**
   - 对应HintShardingStrategy。通过Hint而非SQL解析的方式分片的策略。
- **不分片策略**
   - 对应NoneShardingStrategy。不分片的策略。

**一般来说，在工作中，行表达式分片策略、复合分片策略用的比较多**，还有就是一些特殊情况下 Hint 分片策略的也会用。

行表达式分片策略用的多的原因是因为他比较简单，不需要单独做算法的实现，只需要写 groovy 表达式进行分片即可，如: `t_user_$->{u_id % 8}` 表示t_user表根据u_id模8，而分成8张表，表名称为t_user_0到t_user_7。

复合分片策略用的也很多，主要是因为我们在项目中同时存在多个分表键的情况比较多的，比如我们介绍过的基因法，就是订单号和用户 ID同时都作为分片建的情况。

[✅分表字段如何选择？](https://www.yuque.com/hollis666/fo22bm/mec4ust5rpfob78r?view=doc_embed)

Hint 这种一般是在需要指定具体那张表做查询的时候，会通过这种'提示'的方式明确的告知 ShardingJDBC 你要用哪张物理表！

