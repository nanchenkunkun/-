# LangChain 高级用法：Retrieval（检索）详解

## 一、Retrieval 是什么？解决什么问题？

### 1.1 LLM 的两个根本局限

大语言模型能力很强，但有两个绕不过去的硬限制：

| 局限 | 含义 |
| --- | --- |
| **有限的上下文（Finite context）** | 模型无法一次性吞下整个语料库。即使上下文窗口越来越大，全量灌入也意味着高昂的成本和"大海捞针"式的注意力稀释。 |
| **静态的知识（Static knowledge）** | 训练数据冻结在某个时间点。模型不知道昨天的新闻、你公司的内部文档、你数据库里的最新记录。 |

**Retrieval（检索）就是答案**：在查询发生的时刻，从外部知识源中把"相关的部分"捞出来，喂给模型。这正是 **RAG（Retrieval-Augmented Generation，检索增强生成）** 的基础——用与上下文相关的信息增强 LLM 的回答。

### 1.2 一个关键认知转变

> Retrieval 不是 RAG 的附属品，**检索本身是独立的基础能力**。大多数真实应用会再进一步：把检索与生成集成，产出有依据（grounded）、上下文感知的答案——这时检索管线就变成了更大系统的地基。

换句话说：**RAG 的质量上限，几乎完全由检索质量决定。** 生成模型再强，喂给它的上下文是错的，输出也必然是错的（garbage in, garbage out）。

### 1.3 知识库：不一定要重建

**知识库（Knowledge Base）** 是检索时使用的文档或结构化数据的仓库。

一个重要的实践判断：**如果你已经有知识库**（SQL 数据库、文档数据库、CRM、内部文档系统），**不需要重建它**。你有两条路：

1. 把它作为一个**工具（tool）** 接入 Agent —— 对应 Agentic RAG；
2. 直接查询它，把检索到的内容作为上下文提供给 LLM —— 对应 2-Step RAG。

只有当你确实需要一个全新的、可语义搜索的知识库时，才需要动用 LangChain 的 document loaders + vector stores 从零构建。

## 二、构建知识库：索引管线（Indexing Pipeline）

### 2.1 整体流程

```text
 原始数据源（PDF/Web/DB/...）
        |
        v
  Document Loaders        ── 加载为 Document 对象
        |
        v
  Text Splitters          ── 拆分为合适粒度的 chunks
        |
        v
  Embedding Models        ── 将文本向量化
        |
        v
  Vector Store            ── 存储并建立索引
```

每个组件都是模块化的：可以替换 loader、splitter、embedding 模型或向量库，而不需要重写应用逻辑。

### 2.2 Document：一切的原子单位

`Document` 是"文本单元 + 元数据"的抽象：

| 属性 | 说明 |
| --- | --- |
| `page_content` | 字符串，文档内容 |
| `metadata` | 字典，任意元数据（来源、页码、与其他文档的关系等） |
| `id` | （可选）字符串标识符 |

```python
from langchain_core.documents import Document

documents = [
    Document(
        page_content="Dogs are great companions, known for their loyalty and friendliness.",
        metadata={"source": "mammal-pets-doc"},
    ),
    Document(
        page_content="Cats are independent pets that often enjoy their own space.",
        metadata={"source": "mammal-pets-doc"},
    ),
]
```

> **注意**：metadata 不是附属信息，而是后续做元数据过滤、来源引用、权限控制的关键。建库时就要设计好 metadata schema。

### 2.3 Document Loaders：从数据源到 Document

以 PDF 为例（107 页的 Nike 10-K 财报）：

```python
import pypdf
from langchain_core.documents import Document

def load_pdf_pages(file_path: str) -> list[Document]:
    reader = pypdf.PdfReader(file_path)
    return [
        Document(
            page_content=page.extract_text() or "",
            metadata={"source": file_path, "page": i},   # 保留页码，后续可溯源
        )
        for i, page in enumerate(reader.pages)
    ]

docs = load_pdf_pages("../example_data/nke-10k-2023.pdf")
print(len(docs))  # 107
```

LangChain 提供了大量专用加载器（如 `PyPDFLoader`、Web、Notion、数据库等），详见官方 document loaders 集成列表。

### 2.4 Text Splitters：拆分是最容易被低估的一步

为什么必须拆分？**单页（甚至单文档）对检索来说粒度太粗**——相关段落会被周围无关文本稀释，导致：

- embedding 向量被"平均化"，相似度信号变弱；
- 检索回来的 chunk 塞进上下文窗口时浪费 token。

官方推荐的通用方案是 `RecursiveCharacterTextSplitter`：递归地按常见分隔符（段落 → 换行 → 句子 → 字符）拆分，直到每个 chunk 达到目标大小。

```python
from langchain_text_splitters import RecursiveCharacterTextSplitter

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,        # 每个 chunk 的目标字符数
    chunk_overlap=200,      # 相邻 chunk 的重叠字符数（防止语义在边界处被切断）
    add_start_index=True,   # 在 metadata 中保留原文偏移量，方便溯源
)
all_splits = text_splitter.split_documents(docs)
print(len(all_splits))  # 107 页 → 516 个 chunks
```

三个参数的含义与取舍：

| 参数 | 作用 | 取舍 |
| --- | --- | --- |
| `chunk_size` | chunk 目标大小 | 太小：语义不完整，召回碎片化；太大：向量信号稀释、上下文浪费 |
| `chunk_overlap` | 相邻重叠 | 防止关键句被切断；代价是存储和索引量增大 |
| `add_start_index` | 记录原文偏移 | 几乎总是该开，调试和溯源时非常有用 |

### 2.5 Embeddings：文本 → 向量

向量搜索的原理：文本被编码为数值向量，查询也被编码为同维度向量，再用相似度度量（如余弦相似度）找最近的邻居。

```python
from langchain_openai import OpenAIEmbeddings

embeddings = OpenAIEmbeddings(model="text-embedding-3-large")

vector_1 = embeddings.embed_query(documents[0].page_content)
print(len(vector_1))  # 1536 维
```

官方文档列出了 15+ 个 embedding 提供商，常用的有：

| 提供商 | 示例 | 特点 |
| --- | --- | --- |
| OpenAI | `OpenAIEmbeddings(model="text-embedding-3-large")` | 通用、效果好、按量计费 |
| Azure OpenAI | `AzureOpenAIEmbeddings(...)` | 企业合规场景 |
| Google Gemini | `GoogleGenerativeAIEmbeddings(model="models/gemini-embedding-001")` | 与 Gemini 生态配套 |
| HuggingFace | `HuggingFaceEmbeddings(model_name="sentence-transformers/all-mpnet-base-v2")` | 本地运行、零 API 成本、可私有化 |
| Ollama | `OllamaEmbeddings(model="llama3")` | 本地部署 |
| Voyage AI | `VoyageAIEmbeddings(model="voyage-3")` | 检索质量口碑好 |
| Fake | `DeterministicFakeEmbedding(size=4096)` | 测试用，不消耗 API |

### 2.6 Vector Store：存储与索引

```python
from langchain_core.vectorstores import InMemoryVectorStore

# 开发/测试：内存向量库
vector_store = InMemoryVectorStore(embeddings)

# 生产：换成持久化向量库，接口完全一致
from langchain_chroma import Chroma
vector_store = Chroma(
    collection_name="example_collection",
    embedding_function=embeddings,
    persist_directory="./chroma_langchain_db",
)

# 索引文档
ids = vector_store.add_documents(documents=all_splits)
```

常见选择对比：

| 向量库 | 定位 |
| --- | --- |
| `InMemoryVectorStore` | 开发测试，零依赖 |
| Chroma | 本地持久化，轻量 |
| Milvus | 大规模、高性能、可分布式 |
| PGVector | 复用已有 PostgreSQL 基础设施 |
| Qdrant | 功能全面、过滤能力强 |
| Pinecone / OpenSearch / MongoDB Atlas | 托管云服务 |

> LangChain 的价值在于统一接口：换向量库只改初始化代码，查询逻辑不变。

## 三、检索管线（Retrieval Pipeline）

### 3.1 向量库的查询方式

LangChain 向量库支持多种查询形态：

- 同步与异步
- 按字符串查询 / 按向量查询
- 带或不带相似度分数
- 普通相似度 / **MMR（最大边际相关性）**

```python
# 1. 字符串相似度搜索
results = vector_store.similarity_search(
    "How many distribution centers does Nike have in the US?"
)

# 2. 异步
results = await vector_store.asimilarity_search("When was Nike incorporated?")

# 3. 带相似度分数（注意：不同 provider 的分数语义不同，
#    有的是距离（越小越相似），有的是相似度（越大越相似））
results = vector_store.similarity_search_with_score("What was Nike's revenue in 2023?")
doc, score = results[0]

# 4. 按向量搜索（自己做 query 变换后直接传向量）
embedding = embeddings.embed_query("How were Nike's margins impacted in 2023?")
results = vector_store.similarity_search_by_vector(embedding)
```

### 3.2 Retriever：把检索变成可组合的 Runnable

`VectorStore` **不是** `Runnable` 的子类；而 `Retriever` 是。这意味着 Retriever 支持标准的 `invoke` / `batch` / 异步方法，可以无缝接进任何链或 Agent。

两种创建方式：

```python
# 方式一：@chain 手动包装（灵活，可加自定义逻辑）
from typing import List
from langchain_core.documents import Document
from langchain_core.runnables import chain

@chain
def retriever(query: str) -> List[Document]:
    return vector_store.similarity_search(query, k=1)

retriever.batch(["query 1", "query 2"])

# 方式二：as_retriever（标准用法）
retriever = vector_store.as_retriever(
    search_type="similarity",
    search_kwargs={"k": 1},
)
```

`VectorStoreRetriever` 支持的三种 `search_type`：

| search_type | 说明 |
| --- | --- |
| `"similarity"`（默认） | 纯相似度搜索，取 top-k |
| `"mmr"` | 最大边际相关性：在与查询相似的同时，惩罚结果之间的互相重复，平衡**相关性**与**多样性** |
| `"similarity_score_threshold"` | 按相似度分数阈值过滤，宁可少返回也不要低质量结果 |

> Retriever 也可以包装非向量来源（外部 API、SQL 数据库等）——任何"输入 query、输出 Document 列表"的逻辑都能成为 Retriever。

## 四、从检索到 RAG：三种架构

这是官方文档的核心分类。RAG 不只一种形态，按"谁来决定何时检索"分为三种架构：

| 架构 | 描述 | 控制力 | 灵活性 | 延迟 | 典型用例 |
| --- | --- | --- | --- | --- | --- |
| **2-Step RAG** | 检索永远在生成之前执行，简单可预测 | ✅ 高 | ❌ 低 | ⚡ 快 | FAQ、文档问答机器人 |
| **Agentic RAG** | LLM Agent 在推理过程中自主决定**何时**、**如何**检索 | ❌ 低 | ✅ 高 | ⏳ 可变 | 需要访问多个工具的研究助手 |
| **Hybrid** | 结合两者，加入校验环节 | ⚖️ 中 | ⚖️ 中 | ⏳ 可变 | 需要质量验证的领域问答 |

> **关于延迟的补充**：2-Step RAG 的延迟更可**预测**，因为 LLM 调用次数已知且有上限——前提是 LLM 推理是主导因素。实际延迟还会受检索环节影响（API 响应、网络、数据库查询），取决于具体基础设施。

### 4.1 2-Step RAG：固定管线

```text
用户问题 ──► 检索（retriever）──► 问题+上下文塞进 prompt ──► LLM 生成答案
```

检索步骤永远在生成之前执行。架构直白、行为可预测，适合"检索相关文档是生成答案的明确前提"的场景——大多数 FAQ、文档机器人都是这个形态。

### 4.2 Agentic RAG：检索即工具

**Agentic RAG** 把 RAG 与 Agent 推理结合：不再先检索再回答，而是由 Agent 逐步推理，**自主决定何时检索、检索什么**。

关键认知：**让一个 Agent 具备 RAG 能力，唯一需要的就是给它一个或多个能获取外部知识的工具**（文档加载器、Web API、数据库查询……）。

最简示例：

```python
import requests
from langchain.tools import tool
from langchain.agents import create_agent

@tool
def fetch_url(url: str) -> str:
    """Fetch text content from a URL"""
    response = requests.get(url, timeout=10.0)
    response.raise_for_status()
    return response.text

agent = create_agent(
    model="claude-sonnet-4-6",
    tools=[fetch_url],   # 检索能力 = 一个工具
    system_prompt="Use fetch_url when you need to fetch information from a web-page; "
                  "quote relevant snippets.",
)
```

官方的完整示例是"基于 llms.txt 的 LangGraph 文档问答 Agent"，值得注意的设计细节：

```python
ALLOWED_DOMAINS = ["https://docs.langchain.com/"]
LLMS_TXT = 'https://docs.langchain.com/llms.txt'

@tool
def fetch_documentation(url: str) -> str:
    """Fetch and convert documentation from a URL"""
    # 安全护栏：只允许白名单域名
    if not any(url.startswith(domain) for domain in ALLOWED_DOMAINS):
        return f"Error: URL not allowed. Must start with one of: {', '.join(ALLOWED_DOMAINS)}"
    response = requests.get(url, timeout=10.0)
    response.raise_for_status()
    return markdownify(response.text)  # HTML → Markdown，减少 token

# llms.txt 内容不需要 LLM 就能拿到，提前获取并直接写进系统提示
llms_txt_content = requests.get(LLMS_TXT).text

system_prompt = f"""You are an expert Python developer...
1. 遇到不确定的问题，MUST 使用 fetch_documentation 查阅文档
2. 引用文档时要总结清楚并附上相关上下文
3. 禁止使用白名单外的 URL
4. 文档获取失败时告知用户，并基于已有知识尽力回答

可用的官方文档来源：
{llms_txt_content}
"""

agent = create_agent(
    model=init_chat_model("claude-sonnet-4-6", max_tokens=32_000),
    tools=[fetch_documentation],
    system_prompt=system_prompt,
    name="Agentic RAG",
)
```

这个示例里有四个值得抄作业的技巧：

1. **URL 白名单**：给能上网的工具加域名校验，防止 Agent 被诱导访问任意网站（提示注入防护）。
2. **HTML → Markdown**：`markdownify` 压缩网页内容，显著减少 token 消耗。
3. **目录前置**：`llms.txt`（文档目录）不需要 LLM 参与，直接静态塞进系统提示，省一次工具调用。
4. **提示词兜底**：明确告诉 Agent 检索失败时该怎么办（告知用户 + 尽力回答），避免死循环重试。

### 4.3 Hybrid RAG：管线 + 校验

一句话：**流程还是那条固定流程，只是在关键位置加了几道"检查站"，不合格就回头重做。**

打个比方。2-Step RAG 像一个只会照章办事的实习生：拿到问题就去档案室翻资料，翻到什么就答什么，翻错了也照答。Hybrid RAG 则是给这个流程配了三个复核的人：

| 检查站在哪 | 大白话就是问 | 不合格怎么办 |
| --- | --- | --- |
| 查资料之前 | 这问题问清楚了吗？"这个能退吗"离开上下文根本没法查 | 先改写成一句能直接拿去检索的话 |
| 查完资料 | 找到的东西够回答这个问题吗？ | 不够就换个说法再翻一次 |
| 写完答案 | 答案里的每句话，资料里真有依据吗？ | 没有就指出哪句是编的，打回重写 |

**一个真会踩的坑。** 用户问："跨境订单能用 7 天无理由退货吗？"
资料库里有两份文件：《退货政策》写着"7 天无理由退货"，《跨境业务规则》写着"跨境订单不适用"。

2-Step 会答错：检索出来一堆"7 天无理由退货"的段落，相关度特别高，模型张口就答"可以"——它压根没意识到还有一份讲例外的文件。

加了检查站之后：

1. **查之前**：把问题拆成三条查询，其中一条专门问"例外情况/适用条件"——这一步就把跨境那份文件勾出来了；
2. **查完**：发现只找到通用规则、没找到例外条款 → 判定资料不够 → 再查一次；
3. **写完**：逐句核对，"跨境订单支持退货"这句话在资料里找不到出处 → 打回重写。

最后输出："国内订单支持 7 天无理由退货，跨境订单不支持。"

就这么点事。它的价值不是"让答案更漂亮"，而是**让错的答案过不去**。

流程长这样：

```text
问题 ─► 改写查询 ─► 检索 ─► 资料够吗？──不够──┐
                              │够            │（最多重试 2 次，
                              ▼              │  还不够就带着残缺资料
                            生成答案 ◄───────┘  作答，并在答案里说明）
                              │
                              ▼
                        答案有依据吗？──没有──► 打回重写（最多 2 次）
                              │有
                              ▼
                            输出
```

图里那两个"最多 2 次"是重点：**循环必须有刹车**。校验只要一直说"不合格"，程序就死循环了——这是自己动手实现时最容易忘的一件事。

代价也很直白：2-Step 调用一次就出答案，Hybrid 最少要 4 次（改写、查资料校验、生成、答案校验），又慢又贵。所以它只适合**答错代价高**的场合——合同、医疗、金融合规；用来做 FAQ 客服纯属浪费钱。

最后一个实用的小经验：判断"资料够不够"时，别让模型打 1-10 分（分数没人说得准），直接让它回答**"还缺什么"**。模型说得出缺什么，你就知道下一轮该查什么——这比一个分数有用得多。

### 4.4 三种架构怎么选

```text
问题是否明确需要外部知识才能回答？
   ├── 是，且流程固定 ──────────► 2-Step RAG（最简单，先做它）
   └── 不一定，或需要多步推理 ──► 答错的代价高吗？（合规/金额/医疗）
                                    ├── 高 ──► Hybrid RAG（管线 + 校验，错误答案不外流）
                                    └── 低 ──► Agentic RAG（Agent 自主决定何时检索）
```

经验法则：**从 2-Step 开始，用评估数据证明它不够，再升级到 Hybrid 或 Agentic。** 复杂度应该由证据驱动，而不是由"听起来更先进"驱动。

另一个务实的折中：**不必全量二选一**。用前置分类器（甚至几条规则）把简单问题分流到 2-Step、复杂或高风险问题进 Hybrid 图，既拿到质量又控住成本——这是生产中 Hybrid RAG 最常见的真实形态。

### 4.5 动手：一个最小可运行的 RAG

前面讲的都是零件，这里把它们装成一台能跑的机器。场景很小：**公司制度问答助手**——你问"年假能攒到明年吗"，它翻资料回答，并说清答案出自哪份文件。

#### 4.5.1 装依赖

```bash
pip install -U langchain langchain-openai langchain-text-splitters
```

#### 4.5.2 完整代码（一个文件，可直接跑）

```python
# rag_demo.py
import os

from langchain_core.documents import Document
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.vectorstores import InMemoryVectorStore
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

os.environ.setdefault("OPENAI_API_KEY", "sk-xxx")   # 换成你自己的 key

# ---------- 1. 资料：真实项目里换成 PDF / 网页 / 数据库 ----------
docs = [
    Document(
        page_content="员工入职满一年后，每年享有 10 天带薪年假。当年未休完的年假最多结转 5 天至次年，"
                     "且须在次年 3 月底前使用完毕，逾期作废。",
        metadata={"source": "员工手册.md"},
    ),
    Document(
        page_content="出差住宿标准：一线城市每晚上限 600 元，二线城市 400 元，其他城市 300 元。"
                     "超标部分需部门负责人审批。",
        metadata={"source": "差旅制度.md"},
    ),
    Document(
        page_content="员工每月可申请 4 天远程办公，需提前一天在系统中提交申请，经直属主管批准后生效。"
                     "试用期员工不适用远程办公。",
        metadata={"source": "远程办公规定.md"},
    ),
    Document(
        page_content="报销单据须在费用发生后 30 天内提交，逾期需附情况说明并由部门负责人签字。"
                     "发票抬头必须为公司全称。",
        metadata={"source": "财务报销流程.md"},
    ),
]

# ---------- 2. 切块 ----------
splitter = RecursiveCharacterTextSplitter(chunk_size=200, chunk_overlap=30)
chunks = splitter.split_documents(docs)

# ---------- 3. 向量化 + 入库 ----------
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vector_store = InMemoryVectorStore(embeddings)
vector_store.add_documents(chunks)

# ---------- 4. 检索器 ----------
retriever = vector_store.as_retriever(search_kwargs={"k": 3})

def format_docs(docs):
    """把检回的文档拼成一段带出处的上下文"""
    return "\n\n".join(f"[{d.metadata['source']}] {d.page_content}" for d in docs)

# ---------- 5. 拼 prompt + 生成 ----------
prompt = ChatPromptTemplate.from_template(
    """你是公司制度助手。只依据下面的资料回答问题，资料里没有的内容就说"制度中未提及"，不要自己编。

资料：
{context}

问题：{question}"""
)

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)

chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}
    | prompt
    | llm
    | StrOutputParser()
)

if __name__ == "__main__":
    print(chain.invoke("年假可以攒到明年吗？"))
    print("-" * 50)
    print(chain.invoke("公司提供免费午餐吗？"))
```

运行结果（示意）：

```text
可以，但最多只能结转 5 天到明年，并且要在次年 3 月底前用完，逾期作废。（来源：员工手册.md）
--------------------------------------------------
制度中未提及。
```

第二条是关键：**问题在资料里确实没有答案时，它老实说"没有"，而不是编一个。** 这靠的就是 prompt 里那句"资料里没有就说未提及"——这也是 RAG 里最便宜、最有效的一句约束。

#### 4.5.3 五步分别在干什么

| 步骤 | 代码里对应 | 换成别的会怎样 |
| --- | --- | --- |
| 准备资料 | `docs` 列表 | 真实项目用 loader 从 PDF/网页/数据库读，`Document` 结构不变 |
| 切块 | `RecursiveCharacterTextSplitter` | 换成按标题拆、按段落拆，直接影响检索精度 |
| 向量化入库 | `OpenAIEmbeddings` + `InMemoryVectorStore` | 换 embedding 模型或换向量库（Chroma/PGVector），查询代码一行不用改 |
| 检索 | `as_retriever(k=3)` | 加 `search_type="mmr"` 提多样性，或用混合检索 |
| 生成 | prompt + `ChatOpenAI` | 换任何模型都行，prompt 里那句"没提就说没提"建议保留 |

#### 4.5.4 三个立刻能用的小技巧

**① 先看它翻到了什么，再看答案对不对。** 90% 的 RAG 问题出在检索，不在模型：

```python
for d in retriever.invoke("年假可以攒到明年吗？"):
    print(d.metadata["source"], "|", d.page_content[:50])
```

如果这里就没翻到《员工手册》，后面 prompt 写得再花也没用。

**② 换模型只改一行。** 国内用 DeepSeek / 通义，加个 `base_url` 即可（注意 embedding 得单独选，DeepSeek 没有 embedding 接口）：

```python
llm = ChatOpenAI(model="deepseek-chat", base_url="https://api.deepseek.com/v1")
```

**③ 生产环境把 `InMemoryVectorStore` 换掉。** 内存库重启即失，只适合本地验证；上线换成 Chroma、PGVector 或 Qdrant，代码只需改初始化那两行。

## 五、工程实践与思考

以下内容是基于实际 RAG 项目经验的思考，作为对官方文档的补充。

### 5.1 检索质量 > 生成质量：RAG 的第一性原理

一个常见的误区是：效果不好就换更强的 LLM。实际上大多数 RAG 失败的根因在检索侧：

| 症状 | 真实根因 | 该动的手术 |
| --- | --- | --- |
| 答案"答非所问" | 检索回来的 chunk 根本不相关 | 改进 embedding / 查询改写 / 混合检索 |
| 答案"差一口气" | 相关 chunk 被切断，语义不完整 | 调整 chunk_size / overlap / 按结构拆分 |
| 答案互相矛盾 | 检回了多个互相冲突的过期文档 | metadata 加时间戳/版本，检索时过滤 |
| 简单问题答得很啰嗦 | 检回了过多无关 chunk | 降低 k，或用 score threshold |

**调试顺序应该是：先看检索结果，再看 prompt，最后才看模型。** LangSmith 的 tracing 在这里非常关键——你能直接看到每一步检回了什么。

### 5.2 拆分策略：没有银弹，只有匹配

`RecursiveCharacterTextSplitter` 是好的默认值，但"好"的拆分策略取决于文档结构：

- **有明确结构的文档**（Markdown、代码、HTML）：优先用结构感知的 splitter（如按标题层级拆），保持语义单元完整。
- **表格密集的文档**：字符拆分会把表格切碎，考虑把表格整体作为一个 chunk（或转 Markdown 后单独处理）。
- **chunk_size 的本质**是在"语义完整性"和"向量精确性"之间权衡。经验起点：中文场景 300~500 字符往往比 1000 表现更好——中文信息密度高，大 chunk 稀释效应更明显。
- **overlap 不是免费的**：200 字符重叠意味着约 20% 的冗余存储和索引。对长文档库这是真金白银的成本。

### 5.3 向量检索不是唯一答案：混合检索

官方文档聚焦向量检索，但生产系统里**纯向量检索有明显盲区**：

- **精确匹配场景**（型号、编号、人名、专有名词）：向量检索会"语义漂移"，BM25/关键词检索反而更准。
- **罕见词、新词**：embedding 模型没见过的新术语，向量表示不可靠。

实践结论：**dense（向量）+ sparse（BM25）混合检索 + 重排序（rerank）** 是生产级标配：

```text
查询 ──┬──► 向量检索 top-20 ──┐
       └──► BM25 检索 top-20 ─┴──► 合并去重 ──► Rerank 模型 ──► top-5
```

LangChain 中可以用 `EnsembleRetriever` 组合多个检索器；rerank 环节可以接 Cohere Rerank 或开源的 bge-reranker。**先用大 k 召回（召回率优先），再用 rerank 精选（精确率优先）**——把检索拆成"召回"和"精排"两个阶段，是搜索系统的经典智慧，在 RAG 里同样成立。

### 5.4 metadata 是被严重低估的资产

建库时多花一小时设计 metadata schema，检索时能省十小时调试。建议至少包含：

- `source` / `page` / `start_index`：溯源与引用展示
- `created_at` / `version`：时效性过滤（"只查最新版文档"）
- `department` / `acl`：权限过滤（企业场景刚需）
- `doc_type`：类型过滤（FAQ、合同、教程的检索策略可能不同）

有了这些，"元数据过滤 + 向量检索"的组合拳能解决大量看似需要复杂方案的问题。

### 5.5 三种 RAG 架构的真实成本观

官方表格里的"延迟"只是显性成本，完整的账应该这样算：

| 维度 | 2-Step | Hybrid | Agentic |
| --- | --- | --- | --- |
| LLM 调用次数 | 固定 1-2 次 | 2-5 次（视校验） | 不可预测，可能 10+ 次 |
| 失败模式 | 检索差 → 答案差（直接、易诊断） | 校验误判 → 循环或误杀 | Agent 迷路、死循环、忘记回答 |
| 可调试性 | 高（线性管线） | 中 | 低（需要 trace 工具） |
| token 成本 | 低且稳定 | 中 | 高且波动大 |

由此得出一个务实的架构演进观：

1. **先上 2-Step**，把索引和检索质量打磨好；
2. **用评估集量化短板**：是"查不到"（检索问题）、"查到了但答错"（生成问题）、还是"该查不查"（决策问题）；
3. 只有"该查不查 / 需要多步"类问题占主导时，才值得引入 Agentic——否则你付出的成本和不可预测性换不来收益。

### 5.6 与 Agent 生态的关系：检索正在"工具化"

一个值得注意的趋势：在 LangChain 新架构里，**检索越来越被抽象为 Agent 的一个普通工具**。Agentic RAG 不是"RAG 系统里加了个 Agent"，而是"Agent 系统里检索恰好是工具之一"。这与 Multi-Agent 文档中 Skills 模式的"渐进式披露"一脉相承——知识不再预先灌入上下文，而是按需加载。

对 LLMOps 类项目的启示：设计检索服务时，**对外暴露的接口应该是"工具化"的**（清晰的名称、描述、结构化输入输出），这样它既能被 2-Step 管线直接调用，也能无缝挂到任何 Agent 上，还能被子 Agent 复用——一次建设，三种架构复用。

### 5.7 评估：没有评估集，一切优化都是盲改

RAG 系统必须回答三个可量化的问题：

- **召回率**：相关文档被检回了吗？（检索侧）
- **忠实度（Faithfulness）**：答案是否忠于检索到的上下文，没有幻觉？（生成侧）
- **答案相关性**：答案真正回应了用户问题吗？（整体）

可以用 LangSmith 建立评估集，跑回归测试。每次调整 chunk 策略、换 embedding 模型、改 prompt 之后重跑评估——**RAG 的优化必须形成"评估 → 假设 → 修改 → 验证"的闭环**，否则只是在随机游走。

## 六、总结

1. **Retrieval 解决 LLM 的两大硬限制**：有限上下文 + 静态知识；它是 RAG 的地基，也决定 RAG 的质量上限。
2. **索引管线四步**：Load → Split → Embed → Store，每步都可替换；其中**拆分策略**和 **metadata 设计**最容易被低估。
3. **检索侧的关键概念**：similarity / MMR / score threshold 三种模式；Retriever 是 Runnable、可组合，VectorStore 不是。
4. **三种 RAG 架构**按"谁决定检索时机"划分：2-Step（固定管线，快且可预测）、Agentic（Agent 自主决策，灵活但不可预测）、Hybrid（管线 + 校验，用 4 倍成本换"错误答案不外流"）。
5. **Hybrid RAG 一句话**：固定的流程上加三道检查站——查之前问"问题问清了吗"，查完问"资料够吗"，写完问"每句话有依据吗"；循环要设刹车，成本约为 2-Step 的 4 倍，所以只用在答错代价高的场合。
6. **工程上的核心观点**：检索质量优先于生成质量；混合检索 + rerank 是生产标配；架构复杂度应由评估数据驱动，从 2-Step 起步渐进升级。
7. **先跑起来再优化**：RAG 的最小闭环只有五步——资料 → 切块 → 入库 → 检索 → 生成（4.5 节有一个 60 行就能跑的完整例子）；跑通之后，再逐块替换成更好的切分、更强的 embedding、更稳的检索策略。
