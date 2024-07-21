# 典型回答

[✅ES支持哪些数据类型，和MySQL之间的映射关系是怎么样的？](https://www.yuque.com/hollis666/fo22bm/neoehwlpx3yeeg7m?view=doc_embed)

通过上文我们知道，ES 不支持 decimal 类型的，只有 double、float 等类型，那么，MySQL 中的 decimal 类型，同步到 ES 之后，如何避免丢失精度呢？

```
price DECIMAL(10, 2)
```

如以上 price 字段，在 es 中如何表示呢？有以下几种方式：
### 使用字符串类型（推荐）
将 decimal 数据作为字符串类型存储在 Elasticsearch 中。这种方式可以保证数字的精度不会丢失，因为字符串会保留数字的原始表示形式。

- 优点：完全保留数字的精度。简单易于实现，数据迁移时不需特别处理。
- 缺点：作为字符串存储的数字不能直接用于数值比较或数学运算，需要在应用层处理转换。

```
{
  "properties": {
    "price": {
      "type": "keyword"
    }
  }
}
```
### 扩大浮点类型的精度（推荐）
虽然 double 类型在理论上可能会有精度损失，但实际上 double 类型提供的精度对于许多业务需求已经足够使用。如果决定使用这种方法，可以在数据迁移或同步时适当扩大数值范围以尽量减小精度损失。

- 优点：可以直接进行数值比较和数学运算。
- 缺点：在非常高精度的需求下可能存在精度损失。

```
{
  "properties": {
    "amount": {
      "type": "double"
    }
  }
}
```

### 使用scaled_float（推荐）

Elasticsearch 的 scaled_float 类型是一种数值数据类型，专门用于存储浮点数。其特点是通过一个缩放因子（scaling factor）将浮点数转换为整数来存储，从而在一定范围内提高存储和计算的效率。

他使用一个缩放因子将浮点数转换为整数存储。例如，如果缩放因子是 100，那么值 123.45 会存储为 12345。**这样可以避免浮点数存储和计算中的精度问题。**

```java
{
  "mappings": {
    "properties": {
      "price": {
        "type": "scaled_float",
        "scaling_factor": 100
      }
    }
  }
}

```

### 使用多个字段
在某些情况下，可以将 decimal 数值拆分为两个字段存储：一个为整数部分，另一个为小数部分。这样做可以在不丢失精度的情况下，将数值分开处理。

- 优点：保持数值精确，同时可进行部分数学运算。
- 缺点：增加了数据处理的复杂性，需要在应用层重建数值。

```
{
  "properties": {
    "total_price_yuan": {
      "type": "integer"
    },
    "total_price_cents": {
      "type": "integer"
    }
  }
}
```
### 使用自定义脚本
在查询时，可以使用 Elasticsearch 的脚本功能（如 Painless 脚本）来处理数值计算，确保在处理过程中控制精度。

- 优点：灵活控制数据处理逻辑。
- 缺点：可能影响查询性能，增加系统复杂性。


