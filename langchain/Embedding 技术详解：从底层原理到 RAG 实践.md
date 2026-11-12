# Embedding 技术详解：从底层原理到 RAG 实践

> 本文围绕一条主线展开：**文本如何被表示为向量，向量如何完成语义检索，检索结果如何进入 RAG，并最终形成可评估、可维护的生产系统。**
>

## 前言：为什么 RAG 项目常常“检索不准”？

很多 RAG 问题表面发生在答案生成阶段，根源却在检索阶段：

- 文档切分不合理，一个 Chunk 同时包含多个无关主题；
- Query 与 Document 使用了不同模型，向量空间不兼容；
- 模型要求添加 `query:`、`passage:` 等前缀，代码却没有添加 ；
- 向量没有按模型要求归一化，距离指标又配置错误；
- 只设置 `top_k`，没有元数据过滤、混合检索和重排；
- 更换模型后没有重建历史向量；
- 没有评估集，仅凭几个主观案例判断检索效果。

因此，学习 Embedding 不能只停留在“把文字变成一串数字”。更重要的是理解：**向量怎样产生、怎样比较、怎样存储、怎样检索，以及怎样评价检索是否有效。**

读完本文，你应当能够：

1. 解释 Tokenizer、Token Embedding、Pooling 和 Sentence Embedding 的关系；
2. 手写向量查找、 masked mean pooling、L2 归一化和余弦相似度；
3. 使用 Sentence Transformers 构建一个可运行的语义检索器；
4. 使用 LangChain 完成文本切分、向量生成和检索；
5. 使用 Weaviate 保存自建向量，完成过滤、向量检索和混合检索；
6. 构造 RAG Prompt，并理解“检索不到”和“生成不好”的边界；
7. 使用 Recall@K、MRR、NDCG 评估检索质量；
8. 安全地完成 Embedding 模型升级和向量数据迁移。

---

## 一、Embedding 是什么：把语义映射到可计算的向量空间

### 1.1 从字符串比较到语义比较

计算机可以直接判断两个字符串是否相等，但“字符串不同”不代表“语义不同”。例如：

```text
句子 A：如何重置登录密码？
句子 B：忘记密码后应该怎样找回？
句子 C：今天适合出门散步吗？
```

A 和 B 几乎没有完全相同的连续字符串，却表达了相近意图；A 和 C 都与日常生活有关，但检索意图完全不同。

Embedding（嵌入）的作用，是通过模型把文本映射为一个数值向量：

```text
文本 -> Embedding 模型 -> 固定长度的浮点向量
```

例如，一个 4 维向量可以写成：

```python
vector = [0.125, -0.328, 0.762, 0.091]
```

实际模型的向量通常是 384、768、1024 或更高维度。**维度是向量长度，不是文本长度，也不是 Token 数量。**

如果模型训练得当，语义相近的文本通常会在向量空间中更加接近。于是，“理解两句话是否相关”就被转化为“计算两个向量是否接近”。

### 1.2 一个最小但完整的向量检索例子

下面先不调用神经网络，只用 NumPy 演示检索的本质。假设已经有 3 个文档向量和一个查询向量：

```python
import numpy as np

# 每一行代表一个文本向量；示例数值仅用于演示计算过程。
document_vectors = np.array(
    [
        [0.90, 0.10, 0.00],  # 密码找回
        [0.85, 0.15, 0.05],  # 密码重置
        [0.00, 0.10, 0.95],  # 天气与出行
    ],
    dtype=np.float32,
)
query_vector = np.array([0.88, 0.12, 0.02], dtype=np.float32)

# 对向量做 L2 归一化，使点积等价于余弦相似度。
def l2_normalize(vectors: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(vectors, axis=-1, keepdims=True)
    return vectors / np.clip(norms, a_min=1e-12, a_max=None)

normalized_documents = l2_normalize(document_vectors)
normalized_query = l2_normalize(query_vector)

# (3,)：查询向量分别与三个文档向量计算点积。
scores = normalized_documents @ normalized_query
best_index = int(np.argmax(scores))

print("相似度：", scores)
print("最相关文档下标：", best_index)
```

这段代码已经包含了向量检索的核心：

1. 文档表示为矩阵，每行是一个向量；
2. 查询也表示为向量；
3. 计算查询与所有文档的相似度；
4. 按相似度排序并返回 Top-K。

真实的 RAG 系统只是在此基础上增加了模型推理、海量数据索引、过滤、持久化、并发和评估等能力。

### 1.3 Embedding 不等于 LLM，也不等于向量数据库

| 组件 | 输入 | 输出 | 在 RAG 中的职责 |
|---|---|---|---|
| Embedding 模型 | Query 或 Document 文本 | 数值向量 | 建立语义表示 |
| 向量数据库 | 向量、文本、元数据 | 相似对象 | 存储、索引、过滤和检索 |
| LLM | 指令、问题、检索上下文 | 自然语言或结构化结果 | 基于上下文生成答案 |

一句话概括：**Embedding 负责“表示”，向量数据库负责“找到”，LLM 负责“回答”。**

### 1.4 常见应用

- RAG 知识库检索；
- 站内语义搜索；
- 文本分类、聚类与去重；
- 相似问推荐；
- 用户意图识别；
- 推荐系统中的内容召回；
- 跨语言检索；
- 图像、音频等多模态检索。

本文重点讨论文本向量及其在 RAG 中的应用。

---

## 二、底层原理：文本如何一步步变成向量

### 2.1 完整计算链路

```text
原始文本
   ↓
Tokenizer：切分 Token 并映射为 Token ID
   ↓
Embedding Matrix：按 ID 查找 Token 向量
   ↓
Transformer：结合上下文更新每个 Token 的隐藏状态
   ↓
Pooling：把多个 Token 隐藏状态压缩成一个文本向量
   ↓
可选归一化：将向量缩放到单位长度
   ↓
Sentence / Text Embedding
```

这里必须区分两个概念：

- **Token Embedding**：序列中每个 Token 都有自己的向量，形状通常是 `[sequence_length, dimension]`；
- **Text Embedding**：整段文本最终只得到一个向量，形状是 `[dimension]`。

### 2.2 Tokenizer 不只是“按字或按词切分”

Tokenizer 根据模型词表和切分算法，把文本转换成 Token ID。不同模型可能使用 WordPiece、BPE、SentencePiece 等不同方案。

```text
原始文本：Embedding 很有用

一种可能的切分形式：
["Embed", "##ding", "很", "有", "用"]

一种可能的 ID 形式：
[18120, 4832, 7023, 2418, 3315]
```

以上 Token 和 ID 只是帮助理解的形式化示例，并不对应某个具体模型。真实结果必须使用目标模型自带的 Tokenizer：

```python
from transformers import AutoTokenizer

model_name = "intfloat/multilingual-e5-small"
tokenizer = AutoTokenizer.from_pretrained(model_name)

text = "Embedding 可以将文本转换为向量。"
encoded = tokenizer(
    text,
    add_special_tokens=True,
    truncation=True,
    max_length=512,
    return_tensors="pt",
)

print("Token ID：", encoded["input_ids"][0].tolist())
print("Tokens：", tokenizer.convert_ids_to_tokens(encoded["input_ids"][0]))
print("Attention Mask：", encoded["attention_mask"][0].tolist())
```

`attention_mask` 用来区分真实 Token 和 Padding。批量处理不同长度文本时，较短的序列会被补齐，Pooling 时不能让 Padding 参与平均值计算。

### 2.3 Embedding Matrix 本质上是一个可学习的查找表

假设词表大小为 `V`，Token 向量维度为 `D`，则 Embedding Matrix 的形状是 `[V, D]`。输入 Token ID 后，相当于取出矩阵中对应的行。

下面是一个完整、可运行的 NumPy 模拟：

```python
import numpy as np

# 词表只有 5 个 Token，每个 Token 用 3 维向量表示。
embedding_matrix = np.array(
    [
        [0.10, 0.20, 0.30],
        [0.50, 0.60, 0.70],
        [0.30, 0.20, 0.10],
        [0.80, 0.40, 0.50],
        [0.05, 0.15, 0.25],
    ],
    dtype=np.float32,
)

# 两段文本被补齐到相同长度；0 被当作 padding_id。
token_ids = np.array(
    [
        [1, 3, 2, 0],
        [4, 2, 0, 0],
    ],
    dtype=np.int64,
)
attention_mask = (token_ids != 0).astype(np.float32)

# NumPy 高级索引：结果为 [batch_size, sequence_length, dimension]。
token_embeddings = embedding_matrix[token_ids]

print("Token ID 形状：", token_ids.shape)
print("Token 向量形状：", token_embeddings.shape)
print("第一段文本的 Token 向量：\n", token_embeddings[0])
```

输出形状为 `(2, 4, 3)`，含义是：2 段文本，每段 4 个位置，每个位置一个 3 维向量。

真实模型中，这个矩阵不是人工逐行填写，而是通过训练数据和损失函数自动学习得到。

### 2.4 单个维度通常没有固定的人类语义

对于向量 `[0.12, -0.35, 0.68, 0.21]`，不能简单认定：

- 第 1 维表示情感；
- 第 2 维表示语法；
- 第 3 维表示主题；
- 第 4 维表示事实性。

这些维度是模型学习到的分布式特征，语义通常分散在多个维度的组合中。某些可解释性研究可以发现维度或方向与特定属性相关，但这不意味着每个维度都有稳定、独立、可人工命名的含义。

### 2.5 Pooling：如何把多个 Token 向量合成一个文本向量

Transformer 输出每个 Token 的上下文隐藏状态后，还需要 Pooling。常见方式包括：

| Pooling 方式 | 计算方式 | 注意事项 |
|---|---|---|
| CLS Pooling | 取特殊 `[CLS]` 位置的向量 | 是否适合检索取决于模型训练方式 |
| Mean Pooling | 对有效 Token 向量求平均 | 必须使用 Attention Mask 排除 Padding |
| Max Pooling | 每个维度取最大值 | 对异常值和模型训练方式较敏感 |
| 加权 Pooling | 按位置或注意力加权 | 权重策略需要与模型设计一致 |

下面实现标准的 masked mean pooling，并继续计算文本向量：

```python
import numpy as np

# 模拟 2 段文本、每段 4 个 Token、每个 Token 3 维的 Transformer 输出。
token_hidden_states = np.array(
    [
        [
            [1.0, 2.0, 3.0],
            [2.0, 4.0, 6.0],
            [3.0, 6.0, 9.0],
            [99.0, 99.0, 99.0],  # Padding，不应参与计算
        ],
        [
            [2.0, 0.0, 4.0],
            [4.0, 2.0, 6.0],
            [88.0, 88.0, 88.0],  # Padding
            [99.0, 99.0, 99.0],  # Padding
        ],
    ],
    dtype=np.float32,
)
attention_mask = np.array(
    [
        [1, 1, 1, 0],
        [1, 1, 0, 0],
    ],
    dtype=np.float32,
)

# 扩展 Mask，使其可以与最后一个向量维度广播相乘。
expanded_mask = np.expand_dims(attention_mask, axis=-1)
masked_hidden_states = token_hidden_states * expanded_mask

# 对序列维度求和，再除以每段文本的有效 Token 数。
sum_embeddings = masked_hidden_states.sum(axis=1)
token_counts = np.clip(expanded_mask.sum(axis=1), a_min=1e-12, a_max=None)
sentence_embeddings = sum_embeddings / token_counts

print("文本向量：\n", sentence_embeddings)
print("文本向量形状：", sentence_embeddings.shape)

# 第一段文本应得到 [2, 4, 6]，Padding 中的 99 不会影响结果。
np.testing.assert_allclose(sentence_embeddings[0], [2.0, 4.0, 6.0])
```

这段代码揭示了一个常见 Bug：如果直接对所有位置求平均，Padding 会改变文本向量，而且同一文本在不同批次、不同 Padding 长度下可能得到不同结果。

### 2.6 对比学习：为什么语义相近的文本会靠得更近

专门的 Embedding 模型通常使用对比学习或相关目标训练。一个训练样本可以包含：

```text
Anchor：忘记密码怎么办？
Positive：如何重置登录密码？
Negative：服务器如何配置负载均衡？
```

训练目标是提高 Anchor 与 Positive 的相似度，同时降低 Anchor 与 Negative 的相似度。以余弦相似度为例，可以抽象为：

```text
拉近：similarity(anchor, positive)
推远：similarity(anchor, negative)
```

实际训练通常使用交叉熵、InfoNCE 等损失，并利用批内负样本或难负样本提升区分能力。这里的关键不是记住某个公式，而是理解：**向量空间的几何关系由训练目标和训练数据塑造，并非天然具有检索能力。**

---

## 三、相似度与距离：向量生成之后如何比较

### 3.1 点积、余弦相似度和欧氏距离

设两个向量为 `a` 和 `b`：

- **点积**：同时受向量方向和模长影响；
- **余弦相似度**：关注方向，通常取值范围为 `[-1, 1]`；
- **欧氏距离**：关注空间中的直线距离，越小越接近。

下面用 NumPy 完整实现三种指标：

```python
import numpy as np


def dot_product(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.dot(a, b))


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    denominator = max(float(np.linalg.norm(a) * np.linalg.norm(b)), 1e-12)
    return float(np.dot(a, b) / denominator)


def euclidean_distance(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.linalg.norm(a - b))


a = np.array([1.0, 2.0, 3.0], dtype=np.float32)
b = np.array([2.0, 4.0, 6.0], dtype=np.float32)  # 与 a 同方向，但长度不同
c = np.array([3.0, 1.0, 0.0], dtype=np.float32)

print("a 与 b 的点积：", dot_product(a, b))
print("a 与 b 的余弦相似度：", cosine_similarity(a, b))
print("a 与 b 的欧氏距离：", euclidean_distance(a, b))
print("a 与 c 的余弦相似度：", cosine_similarity(a, c))
```

`a` 和 `b` 方向完全相同，因此余弦相似度为 1，但点积和欧氏距离并不为“相同”。

### 3.2 为什么归一化后点积等价于余弦相似度

L2 归一化把向量长度变为 1：

```python
import numpy as np


def normalize_rows(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    return matrix / np.clip(norms, a_min=1e-12, a_max=None)


vectors = np.array(
    [
        [3.0, 4.0],
        [1.0, 0.0],
        [0.0, 0.0],
    ],
    dtype=np.float32,
)
normalized = normalize_rows(vectors)

print("归一化结果：\n", normalized)
print("每行 L2 范数：", np.linalg.norm(normalized, axis=1))

# 零向量没有有效方向，代码保留为零向量，避免除零。
np.testing.assert_allclose(normalized[0], [0.6, 0.8])
np.testing.assert_allclose(normalized[2], [0.0, 0.0])
```

对于单位向量，`dot(a, b) = cosine(a, b)`。此外，归一化向量之间的平方欧氏距离满足：

```text
||a - b||² = 2 - 2 × cosine(a, b)
```

因此，在向量已归一化时，余弦、点积和欧氏距离可以产生一致的最近邻排序；未归一化时则不能简单混用结论。

### 3.3 相似度分数不是“答案正确概率”

余弦相似度 `0.82` 不表示“文档有 82% 的概率正确”。分数受到模型、数据分布、Query 类型和文本长度等因素影响：

- 不同模型的分数分布不可直接比较；
- 同一个固定阈值不一定适用于所有知识库；
- Top-1 相似度最高，也不保证内容能回答问题；
- 检索结果相似，不代表生成答案一定忠实。

正确做法是使用业务评估集选择阈值，并结合人工审查、重排和答案引用校验。

---

## 四、Embedding 模型与 LLM 的关系及选型

### 4.1 两者都可能使用 Transformer，但训练目标不同

| 对比维度 | Embedding 模型 | 生成式大语言模型 |
|---|---|---|
| 核心目标 | 学习可比较的语义表示 | 根据上下文预测并生成后续内容 |
| 主要输出 | 一个或多个数值向量 | Token 序列、文本或结构化数据 |
| 典型接口 | `encode`、`embed_query` | `invoke`、`chat.completions.create` |
| 主要成本 | 文档批量向量化与查询向量化 | Prompt 与生成 Token 推理 |
| RAG 位置 | 离线索引和在线召回 | 在线答案生成 |

LLM 内部也存在 Token Embedding 层，但它主要服务于模型内部的下一 Token 预测。**不能默认把 LLM 某一层隐藏状态直接取出，就得到适合检索的文本向量。**检索模型需要专门的 Pooling、训练目标和评估。

### 4.2 本地部署与 API 调用

| 方案 | 优点 | 代价与风险 | 适合场景 |
|---|---|---|---|
| 本地开源模型 | 数据可控、可离线、便于版本固定 | 需要下载模型和负责推理资源、并发、监控 | 隐私要求高、调用量大、长期运行 |
| 第三方 API | 接入快、无需维护推理环境 | 网络依赖、限流、费用、数据出域评估 | 原型验证、资源有限、弹性调用 |

选择模型时至少检查：

1. 中文或目标语言的检索效果；
2. 模型许可证与商用限制；
3. 最大输入 Token 数；
4. 输出维度与存储成本；
5. Query/Document 是否需要不同前缀或指令；
6. 是否推荐归一化以及默认相似度函数；
7. 推理吞吐、首 token 延迟之外的整批编码延迟；
8. 模型版本是否可固定；
9. 在自有评估集上的 Recall@K、MRR、NDCG。

**不要只根据排行榜或向量维度选型。**公开榜单反映特定数据集上的平均表现，不等于你的业务语料上的表现。

### 4.3 Query 与 Document 前缀不能忽略

以 E5 系列为例，常见用法要求：

```text
Query：query: 用户问题
Document：passage: 文档内容
```

前缀不是业务元数据，而是模型输入格式的一部分。漏加、互换或者只在索引阶段添加，都可能影响检索效果。不同模型的要求不同，应以对应模型卡片和官方文档为准，不要把某个模型的规则套用到所有模型。

---

## 五、第一个完整实践：用 Sentence Transformers 构建语义检索器

### 5.1 安装与项目约定

建议使用独立虚拟环境。以下示例使用 `intfloat/multilingual-e5-small`，并显式添加 E5 前缀。首次运行需要下载模型，后续可以使用本地缓存。

```bash
python -m venv .venv
```

Windows 激活环境：

```bash
.venv\Scripts\activate
```

macOS / Linux 激活环境：

```bash
source .venv/bin/activate
```

安装依赖：

```bash
python -m pip install --upgrade pip
python -m pip install numpy sentence-transformers transformers
```

### 5.2 完整代码：向量化、内存索引、Top-K 检索

保存为 `numpy_semantic_search.py` 后运行：

```python
from dataclasses import dataclass

import numpy as np
from sentence_transformers import SentenceTransformer


@dataclass(frozen=True)
class SearchHit:
    chunk_id: str
    content: str
    score: float


class E5EmbeddingModel:
    """统一封装模型名称、输入前缀、归一化和输出校验。"""

    def __init__(self, model_name: str = "intfloat/multilingual-e5-small") -> None:
        self.model_name = model_name
        self.model = SentenceTransformer(model_name, device="cpu")
        self.dimension = int(self.model.get_sentence_embedding_dimension())

    def embed_documents(self, texts: list[str]) -> np.ndarray:
        if not texts:
            return np.empty((0, self.dimension), dtype=np.float32)
        inputs = [f"passage: {text}" for text in texts]
        vectors = self.model.encode(
            inputs,
            batch_size=16,
            convert_to_numpy=True,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return self._validate(vectors, expected_rows=len(texts))

    def embed_query(self, text: str) -> np.ndarray:
        if not text.strip():
            raise ValueError("查询文本不能为空")
        vector = self.model.encode(
            f"query: {text}",
            convert_to_numpy=True,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return self._validate(vector, expected_rows=None)

    def _validate(
        self,
        vectors: np.ndarray,
        expected_rows: int | None,
    ) -> np.ndarray:
        vectors = np.asarray(vectors, dtype=np.float32)
        if expected_rows is None:
            if vectors.shape != (self.dimension,):
                raise ValueError(f"查询向量形状异常：{vectors.shape}")
        elif vectors.shape != (expected_rows, self.dimension):
            raise ValueError(f"文档向量形状异常：{vectors.shape}")
        if not np.isfinite(vectors).all():
            raise ValueError("向量中包含 NaN 或 Infinity")
        return vectors


class NumpyVectorStore:
    """教学用内存向量库：适合理解原理，不适合大规模生产数据。"""

    def __init__(self, dimension: int) -> None:
        self.dimension = dimension
        self._chunk_ids: list[str] = []
        self._contents: list[str] = []
        self._vectors: list[np.ndarray] = []

    def add(self, chunk_ids: list[str], contents: list[str], vectors: np.ndarray) -> None:
        if not (len(chunk_ids) == len(contents) == len(vectors)):
            raise ValueError("ID、文本和向量数量不一致")
        if len(set(chunk_ids)) != len(chunk_ids):
            raise ValueError("本批次存在重复 chunk_id")
        if vectors.ndim != 2 or vectors.shape[1] != self.dimension:
            raise ValueError(f"向量维度必须为 {self.dimension}")

        for chunk_id, content, vector in zip(chunk_ids, contents, vectors, strict=True):
            if not chunk_id or not content.strip():
                raise ValueError("chunk_id 和 content 不能为空")
            self._chunk_ids.append(chunk_id)
            self._contents.append(content)
            self._vectors.append(vector.copy())

    def search(self, query_vector: np.ndarray, top_k: int = 3) -> list[SearchHit]:
        if not self._vectors:
            return []
        if query_vector.shape != (self.dimension,):
            raise ValueError("查询向量维度不匹配")
        if top_k <= 0:
            raise ValueError("top_k 必须大于 0")

        matrix = np.stack(self._vectors)
        scores = matrix @ query_vector
        actual_k = min(top_k, len(self._chunk_ids))

        # 先取前 K 个候选，再只对这 K 个结果排序，避免对全部结果做完整排序。
        candidate_indices = np.argpartition(-scores, actual_k - 1)[:actual_k]
        sorted_indices = candidate_indices[np.argsort(-scores[candidate_indices])]

        return [
            SearchHit(
                chunk_id=self._chunk_ids[int(index)],
                content=self._contents[int(index)],
                score=float(scores[index]),
            )
            for index in sorted_indices
        ]


def main() -> None:
    documents = [
        ("chunk-password", "用户忘记登录密码时，可以在登录页选择忘记密码，通过绑定邮箱重置。"),
        ("chunk-invoice", "企业用户可以在订单详情页下载电子发票，并填写公司抬头和税号。"),
        ("chunk-refund", "订单未发货时可以申请退款；已发货订单需要先联系客服确认退货流程。"),
        ("chunk-weather", "户外活动前应关注天气预报，雷雨天气不建议前往山区。"),
    ]
    question = "密码忘了，怎么找回？"

    embedding_model = E5EmbeddingModel()
    vectors = embedding_model.embed_documents([content for _, content in documents])

    store = NumpyVectorStore(dimension=embedding_model.dimension)
    store.add(
        chunk_ids=[chunk_id for chunk_id, _ in documents],
        contents=[content for _, content in documents],
        vectors=vectors,
    )

    query_vector = embedding_model.embed_query(question)
    hits = store.search(query_vector, top_k=3)

    print(f"模型：{embedding_model.model_name}")
    print(f"向量维度：{embedding_model.dimension}")
    print(f"问题：{question}")
    for rank, hit in enumerate(hits, start=1):
        print(f"Top {rank} | {hit.chunk_id} | score={hit.score:.4f}")
        print(hit.content)


if __name__ == "__main__":
    main()
```

### 5.3 这段代码为什么这样设计？

- `embed_documents()` 与 `embed_query()` 分开，确保 Query 和 Document 使用各自前缀；
- 统一设置 `normalize_embeddings=True`，因此矩阵点积就是余弦相似度；
- `_validate()` 检查形状和非法数值，避免错误向量悄悄进入索引；
- `NumpyVectorStore` 同时保存 ID、原文和向量，检索后可以返回可读内容；
- 相似度计算使用矩阵乘法，而不是 Python 循环逐个计算；
- `argpartition` 只找出 Top-K 候选，数据量大时比完整排序更合适。

需要注意：这个实现仍然会在每次查询时扫描全部向量，时间复杂度与文档数量线性相关。它适合教学、小数据集和单元测试，不替代真正的 ANN 向量索引。

---

## 六、RAG 全链路：Embedding 在离线索引与在线问答中的位置

### 6.1 离线阶段：构建可检索的知识库

```text
PDF / Word / HTML / Markdown / 数据库
                ↓
      文档解析与文本清洗
                ↓
     按结构和长度切分 Chunk
                ↓
  保存 document_id、page、chunk_id 等元数据
                ↓
   同一 Embedding 配置批量生成文档向量
                ↓
  向量 + 原文 + 元数据写入向量数据库
```

关键产物不是“一个向量文件”，而是可以追溯的一条知识记录：

```json
{
  "chunk_id": "doc_001:000003",
  "document_id": "doc_001",
  "knowledge_base_id": "kb_support",
  "content": "订单未发货时可以申请退款。",
  "chunk_index": 3,
  "source": "refund-policy.pdf",
  "page_number": 2,
  "embedding_model": "intfloat/multilingual-e5-small",
  "embedding_version": "v1"
}
```

### 6.2 在线阶段：检索、组装上下文、生成答案

```text
用户问题
   ↓
可选：问题改写、权限与知识库过滤
   ↓
同一兼容 Embedding 配置生成 Query Vector
   ↓
向量召回 / BM25 / 混合召回
   ↓
可选：Cross-Encoder Rerank
   ↓
去重、裁剪、按上下文预算组装 Prompt
   ↓
LLM 生成答案并附引用
   ↓
日志、评估、反馈与持续优化
```

文档和查询必须落在**同一套兼容向量空间**中。这里的“兼容”不仅是模型名称相同，还包括模型版本、输入前缀、Pooling、归一化和降维配置等。

### 6.3 为什么要先切分文档？

直接把一篇长文档编码为一个向量会带来三个问题：

1. 模型最大输入长度有限，超出部分可能被截断；
2. 多个主题被平均到一个向量中，具体知识点被稀释；
3. 检索只能定位整篇文档，无法把精准片段放入有限上下文。

但 Chunk 也不是越小越好：

- 太小：上下文不完整，代词、条件和结论被拆开；
- 太大：主题混杂、向量表示被稀释、占用更多 LLM 上下文；
- 重叠太少：跨边界信息丢失；
- 重叠太多：重复向量、重复召回、存储和上下文浪费。

### 6.4 使用 LangChain 递归切分中文文本

`RecursiveCharacterTextSplitter` 会按分隔符优先级递归切分。下面示例会输出每个 Chunk 的长度和内容，便于观察实际效果：

```python
from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter


raw_documents = [
    Document(
        page_content=(
            "退款政策\n\n"
            "订单未发货时，用户可以直接申请退款。审核通过后，款项按原支付路径退回。\n\n"
            "订单已经发货时，用户需要先联系客服。退回商品验收无误后，平台再处理退款。"
        ),
        metadata={"document_id": "doc_refund", "source": "refund.md", "page_number": 1},
    )
]

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=80,
    chunk_overlap=20,
    length_function=len,
    separators=["\n\n", "\n", "。", "！", "？", "；", "，", ""],
    add_start_index=True,
)

chunks = text_splitter.split_documents(raw_documents)

for index, chunk in enumerate(chunks):
    chunk.metadata["chunk_index"] = index
    chunk.metadata["chunk_id"] = f"{chunk.metadata['document_id']}:{index:06d}"
    print(f"Chunk {index}，长度 {len(chunk.page_content)}")
    print("元数据：", chunk.metadata)
    print("正文：", chunk.page_content)
```

安装依赖：

```bash
python -m pip install langchain-core langchain-text-splitters
```

这里的 `chunk_size=80` 使用默认 `len` 按字符计算，**不是 80 个 Token**。生产配置需要结合模型 Tokenizer、中文文档特征和评估集调优。

### 6.5 构造带来源、限制幻觉的 RAG Prompt

下面的生成示例使用 OpenAI 兼容接口。检索代码可以独立运行；只有执行生成时才需要配置 API。

```bash
python -m pip install openai
```

完整 Prompt 构造函数与生成函数：

```python
import os
from collections.abc import Sequence

from openai import OpenAI


def build_rag_prompt(question: str, contexts: Sequence[dict]) -> str:
    if not question.strip():
        raise ValueError("问题不能为空")
    if not contexts:
        return (
            "你没有获得任何知识库资料。请明确告诉用户：当前知识库中没有足够信息，"
            "不要编造答案。\n\n"
            f"用户问题：{question}"
        )

    context_blocks = []
    for index, item in enumerate(contexts, start=1):
        source = item.get("source", "unknown")
        page = item.get("page_number", "unknown")
        content = item["content"].strip()
        context_blocks.append(
            f"[资料 {index}]\n来源：{source}\n页码：{page}\n内容：{content}"
        )

    return f"""你是一个严谨的知识库问答助手。
请只根据给定资料回答，不得使用资料之外的事实补全答案。
如果资料不足，请明确说“根据现有资料无法确定”，并说明缺少什么信息。
回答末尾使用 [资料 1] 这样的格式标注引用，不得虚构来源。

用户问题：
{question}

知识库资料：
{chr(10).join(context_blocks)}
"""


def generate_answer(question: str, contexts: Sequence[dict]) -> str:
    if not os.getenv("OPENAI_API_KEY"):
        raise RuntimeError("请先设置 OPENAI_API_KEY")
    if not os.getenv("LLM_MODEL"):
        raise RuntimeError("请先设置 LLM_MODEL")

    client_kwargs = {"api_key": os.environ["OPENAI_API_KEY"]}
    if os.getenv("OPENAI_BASE_URL"):
        client_kwargs["base_url"] = os.environ["OPENAI_BASE_URL"]

    client = OpenAI(**client_kwargs)
    response = client.chat.completions.create(
        model=os.environ["LLM_MODEL"],
        messages=[
            {
                "role": "system",
                "content": "你负责基于检索资料回答问题，并清楚区分事实与未知信息。",
            },
            {
                "role": "user",
                "content": build_rag_prompt(question, contexts),
            },
        ],
        temperature=0,
    )
    return response.choices[0].message.content or ""


if __name__ == "__main__":
    demo_contexts = [
        {
            "content": "订单未发货时，用户可以直接申请退款，款项按原支付路径退回。",
            "source": "refund-policy.pdf",
            "page_number": 2,
        }
    ]
    answer = generate_answer("未发货订单怎么退款？", demo_contexts)
    print(answer)
```

即使 Prompt 写得严格，也不能把“要求模型不要幻觉”当成完整保障。还需要检查检索命中、引用真实性、上下文覆盖率和答案一致性。

---

## 七、LangChain 实践：统一 Embedding 接口与内存检索

### 7.1 为什么使用统一接口？

LangChain 的 Embeddings 抽象通常提供：

- `embed_documents(texts)`：批量处理文档；
- `embed_query(text)`：处理单个查询；
- 对应的异步接口由具体实现提供。

统一接口的价值在于：业务代码不必到处依赖某个 SDK 的原始返回结构，模型封装、测试替身和异常处理都更一致。但统一接口不代表可以随意换模型，**接口兼容不等于向量空间兼容。**

### 7.2 完整示例：切分、向量化、入库和检索

```python
from langchain_core.documents import Document
from langchain_core.vectorstores import InMemoryVectorStore
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter


MODEL_NAME = "intfloat/multilingual-e5-small"

# 文档与查询分别配置 E5 前缀，两侧都显式归一化。
embeddings = HuggingFaceEmbeddings(
    model_name=MODEL_NAME,
    model_kwargs={"device": "cpu"},
    encode_kwargs={
        "prompt": "passage: ",
        "normalize_embeddings": True,
        "batch_size": 16,
    },
    query_encode_kwargs={
        "prompt": "query: ",
        "normalize_embeddings": True,
    },
)

source_documents = [
    Document(
        page_content=(
            "密码找回\n\n用户忘记密码时，可以在登录页选择忘记密码。"
            "系统会向绑定邮箱发送重置链接。为了账号安全，重置链接具有有效期限。"
        ),
        metadata={"document_id": "doc_account", "source": "account.md"},
    ),
    Document(
        page_content=(
            "退款规则\n\n未发货订单可以直接申请退款。"
            "已发货订单需要联系客服确认退货流程，验收后再处理退款。"
        ),
        metadata={"document_id": "doc_refund", "source": "refund.md"},
    ),
]

splitter = RecursiveCharacterTextSplitter(
    chunk_size=60,
    chunk_overlap=15,
    separators=["\n\n", "\n", "。", "！", "？", "，", ""],
    add_start_index=True,
)
chunks = splitter.split_documents(source_documents)

for index, chunk in enumerate(chunks):
    chunk.metadata["chunk_index"] = index
    chunk.metadata["chunk_id"] = f"{chunk.metadata['document_id']}:{index:06d}"

vector_store = InMemoryVectorStore.from_documents(
    documents=chunks,
    embedding=embeddings,
)

question = "忘记密码后如何找回？"
results = vector_store.similarity_search(question, k=3)

print("问题：", question)
for rank, document in enumerate(results, start=1):
    print(f"Top {rank}：{document.page_content}")
    print("元数据：", document.metadata)
```

安装依赖：

```bash
python -m pip install langchain-core langchain-huggingface langchain-text-splitters sentence-transformers
```

`InMemoryVectorStore` 适合演示和测试。进程退出后数据不会持久化，也不提供面向大规模数据的分布式索引能力。

### 7.3 自行计算相似度时，不要误解 Score

不同 VectorStore 的 `similarity_search_with_score()` 可能返回相似度，也可能返回距离；数值越大是否越好，要看具体实现。例如“余弦距离”通常越小越好，而“余弦相似度”通常越大越好。

工程上应当：

1. 查看当前 VectorStore 的接口说明；
2. 写一个小样本测试确认排序方向；
3. 在返回结构中把字段命名为 `distance` 或 `similarity`，不要笼统叫 `score`；
4. 不要把某个实现的阈值直接迁移到另一个实现。

---

## 八、Weaviate 实践：自建向量的持久化、过滤与混合检索

### 8.1 为什么需要向量数据库？

NumPy 暴力检索需要每次扫描所有向量。向量数据库通常提供：

- 向量与原始对象的持久化；
- HNSW 等近似最近邻索引；
- 元数据过滤；
- 批量写入与更新删除；
- 关键词和向量混合检索；
- 分布式、备份与运维能力。

本节采用 **Self-provided Vectors**：应用自己生成向量，Weaviate 不调用外部 Vectorizer。这样可以明确控制模型版本、前缀和归一化。

### 8.2 启动本地 Weaviate

创建 `docker-compose.yml`：

```yaml
services:
  weaviate:
    image: cr.weaviate.io/semitechnologies/weaviate:latest
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "50051:50051"
    environment:
      AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: "true"
      PERSISTENCE_DATA_PATH: "/var/lib/weaviate"
    volumes:
      - weaviate_data:/var/lib/weaviate

volumes:
  weaviate_data:
```

启动：

```bash
docker compose up -d
```

该配置仅用于本地学习：匿名访问已开启，不应直接暴露到公网。生产环境应固定经过验证的镜像版本，并配置认证、TLS、备份和资源限制。

安装 Python 依赖：

```bash
python -m pip install weaviate-client sentence-transformers numpy
```

### 8.3 创建 Collection

下面代码创建 `KnowledgeChunkDemo`，显式声明属性和 Cosine HNSW 索引。重复运行不会重复创建 Collection。

```python
import weaviate
from weaviate.classes.config import Configure, DataType, Property, VectorDistances


COLLECTION_NAME = "KnowledgeChunkDemo"

client = weaviate.connect_to_local(
    host="localhost",
    port=8080,
    grpc_port=50051,
)

try:
    if not client.is_ready():
        raise RuntimeError("Weaviate 尚未就绪")

    if not client.collections.exists(COLLECTION_NAME):
        client.collections.create(
            name=COLLECTION_NAME,
            description="使用应用侧 E5 模型生成向量的教学知识库",
            vector_config=Configure.Vectors.self_provided(
                vector_index_config=Configure.VectorIndex.hnsw(
                    distance_metric=VectorDistances.COSINE,
                )
            ),
            properties=[
                Property(name="content", data_type=DataType.TEXT),
                Property(name="chunk_id", data_type=DataType.TEXT),
                Property(name="document_id", data_type=DataType.TEXT),
                Property(name="knowledge_base_id", data_type=DataType.TEXT),
                Property(name="source", data_type=DataType.TEXT),
                Property(name="chunk_index", data_type=DataType.INT),
                Property(name="page_number", data_type=DataType.INT),
                Property(name="embedding_model", data_type=DataType.TEXT),
                Property(name="embedding_version", data_type=DataType.TEXT),
            ],
        )
        print(f"已创建 Collection：{COLLECTION_NAME}")
    else:
        print(f"Collection 已存在：{COLLECTION_NAME}")
finally:
    client.close()
```

注意：已存在 Collection 的向量配置不会因为这段代码自动更新。修改模型或距离指标前，应先检查现有配置并设计迁移，而不是直接复用同名 Collection。

### 8.4 批量生成向量并写入

以下示例使用确定性 UUID：同一 `chunk_id` 重试写入时，可以避免因为随机 UUID 而产生重复对象。批量写入结束后必须检查失败对象。

```python
import numpy as np
import weaviate
from sentence_transformers import SentenceTransformer
from weaviate.util import generate_uuid5


COLLECTION_NAME = "KnowledgeChunkDemo"
MODEL_NAME = "intfloat/multilingual-e5-small"
EMBEDDING_VERSION = "e5-small-v1"

chunks = [
    {
        "chunk_id": "doc_account:000000",
        "document_id": "doc_account",
        "knowledge_base_id": "kb_demo",
        "content": "忘记密码时，可以在登录页通过绑定邮箱接收重置链接。",
        "source": "account.md",
        "chunk_index": 0,
        "page_number": 1,
    },
    {
        "chunk_id": "doc_refund:000000",
        "document_id": "doc_refund",
        "knowledge_base_id": "kb_demo",
        "content": "未发货订单可以直接申请退款，款项按原支付路径退回。",
        "source": "refund.md",
        "chunk_index": 0,
        "page_number": 1,
    },
    {
        "chunk_id": "doc_invoice:000000",
        "document_id": "doc_invoice",
        "knowledge_base_id": "kb_demo",
        "content": "电子发票可以在订单详情页下载，企业报销需要填写公司抬头和税号。",
        "source": "invoice.md",
        "chunk_index": 0,
        "page_number": 1,
    },
]

model = SentenceTransformer(MODEL_NAME, device="cpu")
vectors = model.encode(
    [f"passage: {item['content']}" for item in chunks],
    batch_size=16,
    convert_to_numpy=True,
    normalize_embeddings=True,
    show_progress_bar=False,
)
vectors = np.asarray(vectors, dtype=np.float32)

client = weaviate.connect_to_local(host="localhost", port=8080, grpc_port=50051)
try:
    collection = client.collections.use(COLLECTION_NAME)

    with collection.batch.dynamic() as batch:
        for item, vector in zip(chunks, vectors, strict=True):
            properties = {
                **item,
                "embedding_model": MODEL_NAME,
                "embedding_version": EMBEDDING_VERSION,
            }
            batch.add_object(
                properties=properties,
                uuid=generate_uuid5(item["chunk_id"]),
                vector=vector.tolist(),
            )
            if batch.number_errors > 10:
                raise RuntimeError("批量写入错误过多，已停止")

    failed_objects = collection.batch.failed_objects
    if failed_objects:
        raise RuntimeError(f"部分对象写入失败：{failed_objects}")

    print(f"写入完成，共处理 {len(chunks)} 个 Chunk，向量形状为 {vectors.shape}")
finally:
    client.close()
```

真实项目还应记录源文件哈希、内容哈希和模型配置哈希。只有内容或配置变化时才重建对应向量，可以显著减少重复计算。

### 8.5 向量检索与元数据过滤

下面查询只在 `kb_demo` 内检索，并把 Cosine Distance 转为便于阅读的 Cosine Similarity：

```python
import numpy as np
import weaviate
from sentence_transformers import SentenceTransformer
from weaviate.classes.query import Filter, MetadataQuery


COLLECTION_NAME = "KnowledgeChunkDemo"
MODEL_NAME = "intfloat/multilingual-e5-small"
QUESTION = "密码忘记了怎么办？"
TOP_K = 3

model = SentenceTransformer(MODEL_NAME, device="cpu")
query_vector = model.encode(
    f"query: {QUESTION}",
    convert_to_numpy=True,
    normalize_embeddings=True,
    show_progress_bar=False,
)
query_vector = np.asarray(query_vector, dtype=np.float32)

client = weaviate.connect_to_local(host="localhost", port=8080, grpc_port=50051)
try:
    collection = client.collections.use(COLLECTION_NAME)
    response = collection.query.near_vector(
        near_vector=query_vector.tolist(),
        limit=TOP_K,
        filters=Filter.by_property("knowledge_base_id").equal("kb_demo"),
        return_metadata=MetadataQuery(distance=True),
    )

    print(f"问题：{QUESTION}")
    for rank, obj in enumerate(response.objects, start=1):
        distance = obj.metadata.distance
        similarity = None if distance is None else 1.0 - distance
        print(f"Top {rank} | UUID={obj.uuid} | similarity={similarity}")
        print("正文：", obj.properties["content"])
        print("来源：", obj.properties["source"])
finally:
    client.close()
```

**过滤条件往往与 Embedding 本身同样重要。**多租户系统如果遗漏 `tenant_id` 或权限过滤，不仅会导致检索不准，还可能造成越权数据泄露。权限过滤应在检索时执行，不能只在返回前端前做一次可绕过的展示过滤。

### 8.6 普通查询：查看文档分块不需要向量检索

展示某个文档的全部分块时，直接按 `document_id` 查询即可：

```python
import weaviate
from weaviate.classes.query import Filter, Sort


client = weaviate.connect_to_local(host="localhost", port=8080, grpc_port=50051)
try:
    collection = client.collections.use("KnowledgeChunkDemo")
    response = collection.query.fetch_objects(
        filters=Filter.by_property("document_id").equal("doc_refund"),
        sort=Sort.by_property("chunk_index", ascending=True),
        limit=100,
    )

    for obj in response.objects:
        print(obj.properties["chunk_index"], obj.properties["content"])
finally:
    client.close()
```

分页展示大量 Chunk 时，不应简单不断增加 `limit`，应使用游标分页或适合当前客户端版本的深分页方案。

### 8.7 混合检索：关键词与语义互补

纯向量检索可能忽略订单号、错误码、型号等精确词；纯关键词检索又难以处理同义表达。Hybrid Search 可以把两者结合：

```python
import numpy as np
import weaviate
from sentence_transformers import SentenceTransformer
from weaviate.classes.query import MetadataQuery


question = "怎样申请电子发票？"
model = SentenceTransformer("intfloat/multilingual-e5-small", device="cpu")
query_vector = model.encode(
    f"query: {question}",
    convert_to_numpy=True,
    normalize_embeddings=True,
)
query_vector = np.asarray(query_vector, dtype=np.float32)

client = weaviate.connect_to_local(host="localhost", port=8080, grpc_port=50051)
try:
    collection = client.collections.use("KnowledgeChunkDemo")
    response = collection.query.hybrid(
        query=question,
        vector=query_vector.tolist(),
        alpha=0.5,
        limit=3,
        return_metadata=MetadataQuery(score=True),
    )

    for rank, obj in enumerate(response.objects, start=1):
        print(f"Top {rank} | hybrid score={obj.metadata.score}")
        print(obj.properties["content"])
finally:
    client.close()
```

`alpha` 控制关键词与向量部分的相对权重，但最佳值应通过业务评估确定，不能默认 `0.5` 永远最优。Hybrid 返回的是融合分数，不应解释为余弦相似度。

---

## 九、从暴力检索到 HNSW：为什么大规模检索不必扫描全部向量

### 9.1 暴力检索的成本

如果有 `N` 个 `D` 维向量，暴力扫描通常需要计算 `N` 次相似度，单次查询成本近似随 `N × D` 增长。数据量较小时，它简单、准确，也很容易验证；数据量增大后，延迟和计算成本会变高。

### 9.2 ANN 与 HNSW 的直观理解

ANN（Approximate Nearest Neighbor，近似最近邻）用少量准确性损失换取更高检索效率。HNSW 可以理解为一种多层图导航结构：

```text
稀疏上层：快速跨越较远距离，定位大致区域
                ↓
较密下层：逐步接近候选邻居
                ↓
底层：返回最终 Top-K 候选
```

它不是把每个查询都与所有向量比较，而是沿图搜索一部分候选。因此需要关注两类参数：

- **索引构建参数**：影响图质量、构建时间、内存与召回率；
- **查询搜索参数**：影响候选探索范围、延迟与召回率。

“近似”不代表结果一定差，也不代表可以忽略评估。正确的比较方式是：以暴力检索或高质量结果作为基准，测量 ANN 的 Recall@K、P95/P99 延迟和资源消耗。

### 9.3 向量数据库不替代业务数据库

向量数据库擅长相似检索，但常见 RAG 系统仍需要业务数据库维护：

- 用户、权限与知识库关系；
- 文档上传、解析和索引任务状态；
- Embedding 模型版本；
- 反馈、评估和审计记录；
- 文件存储位置与内容哈希。

向量记录中的 `document_id`、`knowledge_base_id` 等字段，是连接两套数据的关键。

---

## 十、检索质量优化：Embedding 只是其中一环

### 10.1 检索链路分层

一个较完整的检索链路可以分为：

```text
Query 理解与改写
        ↓
权限和元数据过滤
        ↓
向量 / 关键词 / 混合召回
        ↓
结果去重与多样化
        ↓
Cross-Encoder Rerank
        ↓
上下文裁剪与排序
```

不要把所有问题都归结为“换一个更大的 Embedding 模型”。

### 10.2 Rerank 与 Embedding 召回的区别

Embedding 是双编码器思路：Query 和 Document 分别独立编码，文档向量可以提前缓存，因此适合从海量数据快速召回。

Cross-Encoder 重排则把 Query 与候选 Document 一起输入模型，交互更充分，通常更适合精细排序，但不能直接对百万级文档逐一计算。

常见组合：

```text
混合召回 Top 50 -> Rerank 精排 -> 取 Top 5 给 LLM
```

Top 50 和 Top 5 都只是示例，最终应通过评估和延迟预算确定。

### 10.3 常见问题与对应策略

| 症状 | 可能原因 | 优先检查 |
|---|---|---|
| 完全相关的文档排不上来 | 前缀错误、模型不兼容、切分过长 | 模型配置、Chunk 内容、召回日志 |
| 精确型号检索差 | 向量弱化字符级精确匹配 | Hybrid、关键词过滤、保留原始型号 |
| 结果主题相关但不能回答 | Chunk 缺少条件或结论 | 调整边界、增加重叠、父子块检索 |
| 重复内容占满 Top-K | 重叠过高、重复入库 | 内容哈希去重、结果多样化 |
| 换了模型后效果骤降 | 新旧向量混用 | 全量重建与版本隔离 |
| 相似度很高但答案编造 | Prompt 或上下文不足 | 引用约束、拒答策略、答案评估 |
| 数据量大后查询变慢 | 暴力扫描、Top-K 过大、过滤低效 | ANN、索引参数、 profiling |

---

## 十一、模型升级与数据迁移：为什么不能只改一行模型名

### 11.1 维度不同会直接不兼容

```text
模型 A：384 维
模型 B：1024 维
```

如果索引按 384 维建立，直接写入或查询 1024 维向量可能报错。

### 11.2 维度相同也不代表向量空间兼容

即使两个模型都输出 768 维，也不能据此认为：

- 第 1 维在两个模型中含义相同；
- 同一句话的向量坐标相近；
- 模型 B 的 Query 可以检索模型 A 的 Document；
- 原相似度阈值仍然有效。

### 11.3 推荐的蓝绿迁移流程

```text
Collection_V1：旧模型、旧向量，继续提供线上查询
                       │
                       ├─ 创建 Collection_V2
                       ├─ 固定新模型版本和完整配置
                       ├─ 全量重建向量并校验数量、维度、失败记录
                       ├─ 在评估集上对比 Recall@K / MRR / NDCG / 延迟
                       ├─ 小流量切换并观察
                       └─ 达标后切换入口，保留可回滚版本
```

不要在原 Collection 上“边查边换”，因为这会产生一段新旧向量混杂的不一致窗口。

### 11.4 建议保存的模型配置

```json
{
  "provider": "sentence-transformers",
  "model_name": "intfloat/multilingual-e5-small",
  "model_revision": "固定为实际验证过的模型修订版本",
  "dimension": 384,
  "query_prefix": "query: ",
  "document_prefix": "passage: ",
  "pooling": "mean",
  "normalize_embeddings": true,
  "distance_metric": "cosine",
  "embedding_version": "e5-small-v1"
}
```

这里的 `model_revision` 应替换为实际验证过的修订标识。仅仅保存 `latest` 或模型短名称，不足以保证未来环境完全可复现。

---

## 十二、检索评估：没有评估集，就无法证明优化有效

### 12.1 建立最小评估集

每条样本至少包含：

```json
{
  "question": "忘记密码后怎样找回？",
  "relevant_chunk_ids": ["doc_account:000000"]
}
```

更完整的数据集还可以包含问题类型、难度、必须引用的来源、可接受答案和权限条件。评估样本应覆盖真实业务表达，而不是只从文档标题机械生成。

### 12.2 Recall@K、MRR、NDCG 分别衡量什么？

| 指标 | 关注点 | 直观解释 |
|---|---|---|
| Recall@K | 相关结果是否被召回 | 前 K 个结果找到了多少相关 Chunk |
| MRR | 第一个相关结果排得多靠前 | 越靠前，倒数排名越大 |
| NDCG@K | 整体排序质量 | 越相关的结果越应排在前面 |

### 12.3 完整可运行的评估代码

下面代码使用二值相关性：一个 Chunk 要么相关，要么不相关。

```python
import math
from collections.abc import Iterable


def recall_at_k(
    retrieved_ids: list[str],
    relevant_ids: set[str],
    k: int,
) -> float:
    if not relevant_ids:
        raise ValueError("相关文档集合不能为空")
    top_k = retrieved_ids[:k]
    hit_count = len(set(top_k) & relevant_ids)
    return hit_count / len(relevant_ids)


def reciprocal_rank_at_k(
    retrieved_ids: list[str],
    relevant_ids: set[str],
    k: int,
) -> float:
    for rank, chunk_id in enumerate(retrieved_ids[:k], start=1):
        if chunk_id in relevant_ids:
            return 1.0 / rank
    return 0.0


def ndcg_at_k(
    retrieved_ids: list[str],
    relevant_ids: set[str],
    k: int,
) -> float:
    dcg = 0.0
    for rank, chunk_id in enumerate(retrieved_ids[:k], start=1):
        relevance = 1.0 if chunk_id in relevant_ids else 0.0
        dcg += relevance / math.log2(rank + 1)

    ideal_hits = min(len(relevant_ids), k)
    idcg = sum(1.0 / math.log2(rank + 1) for rank in range(1, ideal_hits + 1))
    return dcg / idcg if idcg else 0.0


def evaluate(
    evaluation_rows: Iterable[dict],
    k: int = 5,
) -> dict[str, float]:
    rows = list(evaluation_rows)
    if not rows:
        raise ValueError("评估集不能为空")

    recall_values = []
    rr_values = []
    ndcg_values = []

    for row in rows:
        retrieved = row["retrieved_chunk_ids"]
        relevant = set(row["relevant_chunk_ids"])
        recall_values.append(recall_at_k(retrieved, relevant, k))
        rr_values.append(reciprocal_rank_at_k(retrieved, relevant, k))
        ndcg_values.append(ndcg_at_k(retrieved, relevant, k))

    return {
        f"Recall@{k}": sum(recall_values) / len(rows),
        f"MRR@{k}": sum(rr_values) / len(rows),
        f"NDCG@{k}": sum(ndcg_values) / len(rows),
    }


if __name__ == "__main__":
    demo_rows = [
        {
            "retrieved_chunk_ids": ["irrelevant-1", "password-1", "refund-1"],
            "relevant_chunk_ids": ["password-1"],
        },
        {
            "retrieved_chunk_ids": ["refund-1", "invoice-1", "password-1"],
            "relevant_chunk_ids": ["refund-1"],
        },
    ]
    metrics = evaluate(demo_rows, k=3)
    for name, value in metrics.items():
        print(f"{name}: {value:.4f}")
```

实际接入时，把 `retrieved_chunk_ids` 替换为检索器真实返回的 ID 列表即可。评估时要固定数据集、Top-K、过滤条件、模型版本和检索参数，否则不同轮次结果不可比较。

### 12.4 检索评估与答案评估不能互相替代

- 检索正确、生成错误：可能是 Prompt、上下文组织或 LLM 问题；
- 检索错误、答案碰巧正确：可能来自模型参数知识，不能证明 RAG 有效；
- 检索和生成都正确：还需要检查引用是否指向真实资料；
- 无答案问题：应评估系统是否能正确拒答，而不是强迫模型回答。

线上还应记录检索延迟、生成延迟、失败率、空召回率、用户反馈和引用点击率。

---

## 十三、生产工程化清单

### 13.1 统一封装与配置管理

不要在多个模块中分别实例化模型。统一服务至少负责：

- 模型加载与生命周期；
- Query/Document 前缀；
- 批大小、设备与并发；
- 归一化和维度校验；
- 超时、重试与异常分类；
- 指标和日志；
- 模型版本配置。

推荐边界：

```python
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class EmbeddingConfig:
    model_name: str
    dimension: int
    query_prefix: str = ""
    document_prefix: str = ""
    normalize_embeddings: bool = True


class TextEmbeddingService(Protocol):
    @property
    def config(self) -> EmbeddingConfig:
        ...

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        ...

    def embed_query(self, text: str) -> list[float]:
        ...
```

这个 Protocol 让业务层依赖稳定接口，也便于在单元测试中替换为确定性的假模型。Protocol 中的 `...` 是接口方法体的标准写法，不是省略实现；具体实现由基础设施层提供。

### 13.2 批量、缓存与幂等

- 文档入库使用批量编码和批量写入；
- 根据“模型配置 + 规范化文本”计算缓存键；
- 对内容哈希未变化的 Chunk 复用向量；
- 使用确定性 `chunk_id` 和 UUID 支持失败重试；
- 批量任务设置检查点，不要每次从头开始；
- 重试只处理限流、临时网络错误等可恢复异常；
- 对无效输入、维度不匹配等永久错误不要无脑重试。

### 13.3 数据一致性与安全

- 文档删除时，同步删除或标记对应 Chunk 和向量；
- 权限变化后及时更新可检索范围；
- 多租户过滤必须在服务端检索阶段执行；
- 密钥通过环境变量或密钥管理服务提供，不写入源码；
- 外部文档可能包含 Prompt Injection，不能把检索内容当作系统指令；
- 日志避免默认记录完整敏感文本和完整向量；
- 模型文件、依赖和 Docker 镜像都应进行版本与供应链管理。

### 13.4 可观测性

每次问答建议记录以下非敏感或可脱敏信息：

```text
trace_id
knowledge_base_id
embedding_version
query_token_count
retrieval_latency_ms
retrieved_chunk_ids
retrieval_distances 或 similarities
rerank_latency_ms
context_token_count
generation_latency_ms
empty_retrieval
error_type
```

这些字段能帮助你区分：是向量化失败、召回为空、排序不好、上下文超长，还是 LLM 调用失败。

---

## 十四、常见误区与故障排查

### 误区 1：向量维度越高，模型一定越好

维度主要影响表示容量、存储和计算成本，不直接等于检索效果。应在同一评估集上比较质量、延迟和成本。

### 误区 2：所有文本都共用一个通用相似度阈值

不同模型和数据分布的分数范围可能差异很大。阈值应根据验证集和线上人工复核确定，并保留“无足够资料”的拒答分支。

### 误区 3：Embedding 模型就是 LLM 的 Embedding 层

LLM 内部 Token Embedding 是输入表示的一部分；检索模型输出的是经过专门训练与 Pooling 的可比较文本向量，两者用途不同。

### 误区 4：LangChain 接口相同，就可以无成本换模型

接口只统一调用方式，不统一向量空间。换模型仍需要检查前缀、维度、距离、归一化、版本和历史数据迁移。

### 误区 5：Top-K 越大越好

Top-K 过大会引入噪声、增加 Rerank 成本并挤占上下文。应结合召回率、上下文预算和答案质量共同调优。

### 误区 6：相似度最高就一定能够回答问题

文档可能只与问题主题相关，却没有问题所需的具体条件、数值或结论。需要评估“答案支持度”，而不只是主题相似度。

### 排查顺序建议

```text
1. 原文是否被正确解析？
2. Chunk 是否包含完整答案？
3. Query 与 Document 模型配置是否一致？
4. 前缀、Pooling、归一化和距离是否正确？
5. 元数据与权限过滤是否排除了目标文档？
6. Top-K 与混合检索是否合理？
7. Rerank 是否把正确结果排后？
8. Prompt 中是否真的包含命中文本？
9. LLM 是否遵守引用与拒答要求？
```

按顺序排查可以避免一开始就盲目更换模型或调整 Prompt。

---

## 十五、总结：围绕“表示、召回、生成、评估”理解 Embedding

Embedding 的本质是：**把文本映射为向量，使语义关系能够通过向量空间中的数学关系进行计算。**

围绕这条主线，需要掌握以下知识：

1. Tokenizer 将文本映射为 Token ID；
2. Embedding Matrix 按 ID 提供可学习的 Token 向量；
3. Transformer 根据上下文生成隐藏状态；
4. Pooling 将 Token 序列压缩成文本向量，Mean Pooling 必须处理 Padding；
5. 归一化后，点积与余弦相似度等价；
6. 相似度不是答案正确概率，阈值需要评估；
7. Embedding 模型负责表示，向量数据库负责检索，LLM 负责生成；
8. RAG 离线阶段切分并索引文档，在线阶段检索并组装上下文；
9. Query/Document 前缀、Pooling、模型版本和归一化都是向量兼容性的一部分；
10. 向量数据库同时提供持久化、ANN、过滤和混合检索，不只是保存数组；
11. 检索质量还受到切分、权限过滤、Hybrid、Rerank 和上下文组织影响；
12. 更换模型必须重建向量，最好通过新旧 Collection 进行可回滚迁移；
13. 使用 Recall@K、MRR、NDCG 和答案级指标持续评估，而不是凭感觉优化。

最终可以用一张图收束全文：

```text
                    RAG 系统
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
   语义表示          检索召回          答案生成
 Embedding       Vector Store          LLM
        │              │              │
 Tokenizer        向量持久化         Prompt 组装
 Token ID         ANN / HNSW         上下文裁剪
 Transformer      元数据过滤         基于资料回答
 Pooling          Hybrid / Rerank     引用与拒答
        │              │              │
        └──────────────┼──────────────┘
                       ▼
          评估、监控、反馈与版本迁移
```

**真正可靠的 RAG，不是“向量库 + 大模型”的简单拼接，而是从数据解析、语义表示、检索排序到答案评估的一整套工程闭环。**

---

## 参考资料

- [Sentence Transformers：Computing Embeddings](https://sbert.net/examples/sentence_transformer/applications/computing-embeddings)
- [LangChain：HuggingFaceEmbeddings API Reference](https://reference.langchain.com/python/langchain-huggingface/embeddings/huggingface/HuggingFaceEmbeddings)
- [Weaviate：Python Client](https://docs.weaviate.io/weaviate/client-libraries/python)
- [Weaviate：Vectorizer and Vector Index Configuration](https://docs.weaviate.io/weaviate/manage-collections/vector-config)

> 第三方 SDK 的接口会持续演进。实际落地时，应结合当前安装版本的官方文档和本地小样本测试确认参数行为，并在生产环境锁定经验证的依赖版本。
