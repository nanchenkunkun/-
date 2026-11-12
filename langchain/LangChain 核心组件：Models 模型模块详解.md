# LangChain 核心组件：Models 模型模块详解

## 一、Models 是什么？

### 1.1 基本概念

Models 是 LangChain 的核心组件之一，主要负责与大语言模型进行交互。

简单来说：

**Models 就是 LangChain 与大语言模型之间的桥梁。**

我们平时使用大模型，通常需要完成以下工作：

1. 选择一个大模型，例如 GPT、Claude、Gemini 等。
2. 将用户的问题和相关上下文组织成模型能够理解的输入。
3. 调用模型接口，获取模型生成的结果。
4. 对模型返回的结果进行处理，供后续业务使用。

如果直接调用不同厂商的 API，就需要分别适配不同的接口、参数和返回格式。

LangChain 的 Models 模块就是为了统一这些操作。

例如：

- 使用 OpenAI，需要适配 OpenAI 的 API。
- 使用 Anthropic，需要适配 Anthropic 的 API。
- 使用 Google Gemini，需要适配 Google 的 API。
- 使用本地部署的模型，需要适配相应的推理服务。

LangChain 通过统一的模型接口，将这些不同的模型封装起来，让开发者能够使用相似的代码调用不同厂商的模型。

### 1.2 Models 在 LangChain 中的定位

Models 是 Agent 的核心执行组件，也是普通 LLM 应用中负责内容生成的核心组件。

它主要负责：

- 理解输入：接收用户消息、系统指令和上下文。
- 内容生成：根据输入生成文本或其他类型的内容。
- 工具决策：在支持 Tool Calling 的情况下，决定是否调用外部工具。
- 结构化输出：按照指定的数据结构返回结果。
- 多模态处理：部分模型支持图片、音频、视频等输入。

可以将它理解为整个应用的模型能力提供者。

### 1.3 Models 的工作位置

一个典型的 LLM 应用可以抽象成下面的结构：

```text
        用户输入
           |
           v
      Prompt Template
           |
           v
         Models
           |
           v
       Output Parser
           |
           v
        业务结果
```

各组件的职责：

| 组件            | 主要职责              |
| ------------- | ----------------- |
| Prompt        | 组织输入，告诉模型应该做什么    |
| Models        | 调用大模型，完成理解和生成     |
| Output Parser | 将模型输出转换成业务需要的数据格式 |

在 Agent 架构中，Models 还承担决策职责：

```text
          用户任务
              |
              v
          Agent
              |
              v
           Models
              |
        +-----+------+
        |            |
        v            v
      调用工具     直接回答
        |
        v
      Tools
        |
        v
      工具结果
        |
        v
       Models
        |
        v
      最终回答
```

需要注意：模型本身通常不会直接执行开发者定义的 Python 函数。它负责生成工具调用请求，Agent 负责调度工具、执行函数，并将结果交还给模型。

---

## 二、Models 的核心分类

LangChain 中涉及的模型技术主要可以从以下几个方面理解。

### 2.1 Chat Models（聊天模型）

Chat Models 是目前 LangChain 中最重要、最常用的模型接口。

它主要用于与支持对话形式的大语言模型进行交互。

与传统的文本补全模型不同，Chat Models 通常以消息（Messages）作为输入，以 AIMessage 作为输出。

例如：

```python
from langchain.chat_models import init_chat_model

model = init_chat_model("openai:gpt-5.5")

response = model.invoke("你好，请介绍一下自己")

print(response.content)
```

这里的 `invoke()` 就是调用模型的核心方法。

Chat Models 通常支持以下消息类型：

| 消息类型          | 作用                  |
| ------------- | ------------------- |
| SystemMessage | 系统消息，用于设定模型的角色、行为规则 |
| HumanMessage  | 用户消息，代表用户输入         |
| AIMessage     | AI 消息，代表模型生成的内容     |
| ToolMessage   | 工具消息，代表工具执行后返回的结果   |

例如：

```python
from langchain.messages import (
    SystemMessage,
    HumanMessage,
    AIMessage
)

messages = [
    SystemMessage("你是一个专业的 Python 开发工程师"),
    HumanMessage("什么是装饰器？"),
    AIMessage("装饰器是一种用于扩展函数功能的语法结构。"),
    HumanMessage("请举一个简单的例子")
]

response = model.invoke(messages)

print(response.content)
```

这里需要理解一个重要概念：

**Chat Models 并不意味着模型自己拥有长期记忆。**

每次调用模型时，应用需要根据业务需要，将相关历史消息重新组织并传递给模型。模型根据本次收到的上下文生成回答。

这也是为什么 LangChain 还需要 Memory、Checkpointer 等相关机制来管理对话历史。

### 2.2 LLMs（传统文本补全模型）

LLMs 在 LangChain 的传统接口体系中，主要指基于文本补全（Text Completion）的模型。

它与 Chat Models 的主要区别在于输入输出形式。

例如：

传统 LLM：

```text
输入：
请介绍一下 Python。

输出：
Python 是一种高级编程语言……
```

Chat Model：

```text
输入：
[
    SystemMessage("你是一个编程助手"),
    HumanMessage("请介绍一下 Python")
]

输出：
AIMessage("Python 是一种高级编程语言……")
```

两者的区别：

可以把 Chat Models 看作传统 LLM 接口在现代对话式应用中的演进形态。

| 对比维度 | LLM        | Chat Model             |
| ---- | ---------- | ---------------------- |
| 输入   | 通常是字符串     | 消息或字符串                 |
| 输出   | 通常是字符串     | AIMessage              |
| 对话角色 | 通常没有标准消息角色 | 支持 System、Human、AI 等角色 |
| 适用场景 | 传统文本补全     | 多轮对话、Agent、工具调用        |
| 当前应用 | 较多属于传统接口   | 现代应用的主流选择              |

需要注意：

这里的 LLMs 和 Chat Models 是 LangChain API 层面的分类，不是说 Chat Model 不属于大语言模型。

Chat Model 本身同样可能由大语言模型驱动。

对于新项目，通常优先学习 Chat Models。

### 2.3 Embeddings（嵌入模型）

Embeddings 与 Chat Models 有明显区别。

Chat Models 主要负责理解输入并生成内容，而 Embeddings 主要负责将文本转换为向量。

例如：

```text
输入：
"我喜欢学习人工智能"

       |
       v

Embedding Model

       |
       v

[0.12, -0.35, 0.87, 0.21, ...]
```

这个向量可以理解为文本在高维向量空间中的一种数学表示。

它的主要作用是：

- 将文本转换为向量。
- 根据向量计算文本之间的语义相似度。
- 为向量数据库提供数据。
- 支持语义搜索和 RAG 检索。

LangChain 提供了统一的 Embeddings 接口，主要有两个常用方法：

**1. embed_documents()**

将多个文档转换成向量。

```python
from langchain_openai import OpenAIEmbeddings

embeddings = OpenAIEmbeddings(
    model="text-embedding-3-small"
)

documents = [
    "Python 是一种编程语言",
    "Java 是一种面向对象的编程语言"
]

vectors = embeddings.embed_documents(documents)

print(vectors)
```

**2. embed_query()**

将用户查询转换成向量。

```python
query = "什么是 Python？"

query_vector = embeddings.embed_query(query)

print(query_vector)
```

为什么需要区分这两个方法？

因为在 RAG 中：

- 文档需要先转换为向量，存储到向量数据库。
- 用户提出问题时，需要将问题转换成向量。
- 然后通过相似度计算，找到与问题相关的文档。

整个过程：

```text
         文档
           |
           v
    Embedding Model
           |
           v
        文档向量
           |
           v
        向量数据库
           |
           |
用户问题  |
    |      |
    v      |
Embedding Model
    |
    v
  查询向量
    |
    v
向量相似度检索
    |
    v
相关文档
    |
    v
   Chat Model
    |
    v
   最终回答
```

注意：Embeddings 不是用来直接生成自然语言回答的，它主要负责文本向量化和语义表示。

---

## 三、模型初始化与配置

理解模型的分类之后，接下来学习如何创建和配置模型。

### 3.1 使用 init_chat_model 初始化模型

LangChain 提供了 `init_chat_model()` 方法，可以通过统一入口初始化不同供应商的聊天模型。

例如：

```python
from langchain.chat_models import init_chat_model

model = init_chat_model(
    "openai:gpt-5.5"
)

response = model.invoke("你好")

print(response.content)
```

也可以使用模型供应商提供的具体类：

```python
from langchain_openai import ChatOpenAI

model = ChatOpenAI(
    model="gpt-5.5"
)

response = model.invoke("你好")

print(response.content)
```

两种方式有什么区别？

| 方式              | 特点                       |
| --------------- | ------------------------ |
| init_chat_model | 统一初始化入口，方便切换不同供应商        |
| ChatOpenAI 等具体类 | 直接使用供应商集成类，可以配置更多供应商专属参数 |

如果你希望降低对具体模型供应商的依赖，可以优先学习 `init_chat_model()`。

如果需要使用某个供应商的特殊功能，则可以使用对应的具体模型类。

### 3.2 常见模型参数

初始化模型时，通常需要配置一些参数。

以下是比较常见的参数：

| 参数          | 作用               |
| ----------- | ---------------- |
| model       | 指定使用哪个模型         |
| api_key     | 模型供应商的 API 密钥    |
| temperature | 控制生成结果的随机性       |
| max_tokens  | 限制生成结果的 Token 数量 |
| timeout     | 请求超时时间           |
| max_retries | 请求失败后的最大重试次数     |

**temperature**

Temperature 主要控制模型生成结果的随机性。

可以简单理解为：

- 值较低：生成结果相对稳定，更适合分类、提取等任务。
- 值较高：生成结果通常更加多样，适合创意写作等任务。

例如：

```python
model = init_chat_model(
    "openai:gpt-5.5",
    temperature=0.7
)
```

注意：不同模型对 temperature 的支持情况不同，有些推理模型不支持或者限制该参数。

**max_tokens**

控制模型生成内容的最大 Token 数量。

```python
model = init_chat_model(
    "openai:gpt-5.5",
    max_tokens=1000
)
```

注意：不同供应商可能使用 `max_tokens`、`max_completion_tokens` 或其他参数名称，具体以模型集成文档为准。

**timeout**

设置模型调用的超时时间：

```python
model = init_chat_model(
    "openai:gpt-5.5",
    timeout=30
)
```

超过设定时间后，请求会因为超时而失败。

**max_retries**

设置模型请求失败后的最大重试次数：

```python
model = init_chat_model(
    "openai:gpt-5.5",
    max_retries=6
)
```

例如遇到网络异常、请求频率限制（429）或者服务端错误（5xx），模型集成通常会自动重试。

但认证错误（401）等客户端错误一般不会通过重试解决。

### 3.3 模型参数应该如何选择？

不要认为所有任务都需要相同的参数。

例如：

| 业务场景 | 参数建议               |
| ---- | ------------------ |
| 文本分类 | 较低的 temperature    |
| 信息提取 | 较低的 temperature    |
| 代码生成 | 根据模型特性选择，优先保证准确性   |
| 创意写作 | 可以适当提高 temperature |
| 普通问答 | 根据业务要求调整           |

实际开发中，参数需要根据模型本身的能力和业务测试结果确定。

---

## 四、Models 的核心调用方法

模型创建完成后，就需要调用模型。

LangChain 提供了几个非常重要的方法：

- invoke()
- stream()
- batch()

它们是学习 Models 时必须掌握的内容。

### 4.1 invoke()：普通调用

`invoke()` 是最基础的模型调用方法。

它会等待模型生成完整结果，然后返回。

```python
response = model.invoke("什么是 LangChain？")

print(response.content)
```

适用场景：

- 普通问答。
- 文本生成。
- 信息提取。
- 分类任务。

返回结果通常是一个 `AIMessage` 对象。

除了 `content`，还可以获取其他信息：

```python
print(response.content)

# 返回的模型信息
print(response.response_metadata)

# Token 使用情况
print(response.usage_metadata)
```

其中：

- content：模型生成的内容。
- response_metadata：模型响应的元数据，例如结束原因等。
- usage_metadata：Token 使用情况。

### 4.2 stream()：流式调用

普通调用需要等待模型生成完整内容。

但是在实际应用中，如果模型需要生成较长的回答，用户可能需要等待较长时间。

这时可以使用 `stream()`。

```python
for chunk in model.stream("请详细介绍一下 LangChain"):
    print(chunk.text, end="", flush=True)
```

它的特点是：

模型生成一部分内容，就返回一部分内容。

例如：

```text
第一次：LangChain
第二次：是一个
第三次：用于构建
第四次：大模型应用的框架
```

与 invoke() 的区别：

| 对比维度 | invoke()  | stream()       |
| ---- | --------- | -------------- |
| 返回方式 | 完整结果      | 分段返回           |
| 用户体验 | 等待完整生成    | 可以实时看到内容       |
| 适用场景 | 后台处理、普通调用 | 聊天界面、实时输出      |
| 返回类型 | AIMessage | AIMessageChunk |

需要注意，流式输出不代表模型生成得更快，而是将生成过程逐步展示出来，改善用户等待体验。

### 4.3 batch()：批量调用

如果需要处理多个相互独立的问题，逐个调用模型可能会造成不必要的等待。

这时可以使用 `batch()`。

```python
questions = [
    "什么是 Python？",
    "什么是 Java？",
    "什么是 LangChain？"
]

responses = model.batch(questions)

for response in responses:
    print(response.content)
```

它可以并行处理多个请求，减少整体处理时间。

还可以控制最大并发数量：

```python
responses = model.batch(
    questions,
    config={
        "max_concurrency": 3
    }
)
```

注意：

这里的 batch() 是 LangChain 在客户端组织并发模型调用的接口，不等于模型供应商提供的专门批处理 API。

---

## 五、Models 的高级能力

除了基本的文本生成，现代 Chat Models 还提供了很多重要能力。

### 5.1 Tool Calling（工具调用）

Tool Calling 是 Agent 的重要基础能力。

它允许模型根据用户的问题，生成调用外部工具的请求。

例如：

用户问：

```text
北京今天的天气怎么样？
```

模型可能判断自己需要调用天气查询工具。

于是返回类似这样的信息：

```json
{
    "name": "get_weather",
    "args": {
        "location": "北京"
    }
}
```

注意：

这只是模型生成的工具调用请求，并不意味着模型已经真正执行了 Python 函数。

#### 绑定工具

LangChain 使用 `bind_tools()` 将工具绑定到模型。

```python
from langchain.tools import tool

@tool
def get_weather(location: str) -> str:
    """查询指定城市的天气"""
    return f"{location}今天晴天，气温25度"

model_with_tools = model.bind_tools(
    [get_weather]
)

response = model_with_tools.invoke(
    "北京今天的天气怎么样？"
)

print(response.tool_calls)
```

模型返回的 `tool_calls` 中包含工具名称、参数等信息。

#### 工具调用的完整过程

```text
       用户提出问题
             |
             v
         Chat Model
             |
             v
       判断是否需要工具
             |
             v
       生成 Tool Call
             |
             v
       Agent 执行工具
             |
             v
         获取结果
             |
             v
         Chat Model
             |
             v
         最终回答
```

这里有一个非常重要的区别：

**模型负责决定调用什么工具，Agent 负责执行工具和组织整个调用流程。**

如果单独使用模型而没有 Agent，就需要开发者自己处理工具执行、结果回传和下一轮模型调用。

### 5.2 Structured Output（结构化输出）

在实际业务中，我们不一定希望模型返回一大段自然语言。

例如：

我们希望模型识别用户信息，并且返回：

```json
{
    "name": "张三",
    "age": 25
}
```

如果直接让模型输出 JSON，可能出现字段缺失、格式错误或者类型不正确的问题。

LangChain 提供了 `with_structured_output()`，可以根据指定的 Schema 约束模型输出。

例如：

```python
from pydantic import BaseModel, Field

class Person(BaseModel):
    name: str = Field(description="姓名")
    age: int = Field(description="年龄")

structured_model = model.with_structured_output(
    Person
)

result = structured_model.invoke(
    "张三今年25岁"
)

print(result)
```

可能得到：

```python
Person(name="张三", age=25)
```

这样就不需要自己再通过字符串处理、JSON 解析等方式提取数据。

结构化输出的主要应用场景：

- 信息抽取。
- 数据分类。
- 实体识别。
- 自动生成业务数据。
- API 参数生成。

注意：结构化输出的具体实现方式取决于模型供应商，有些使用原生 JSON Schema，有些通过 Tool Calling 等机制实现。它也不能保证模型提取的信息本身一定正确，仍然需要进行业务校验。

### 5.3 Multimodality（多模态）

传统的语言模型主要处理文本。

而现在很多模型已经支持多模态能力，可以接收和生成不同类型的数据。

例如：

- 文本：普通文字输入。
- 图片：识别图片内容。
- 音频：语音识别、语音理解和生成。
- 视频：理解视频内容。
- 其他结构化内容：根据模型能力处理相应的数据类型。

例如，用户可以发送一张图片，并要求模型识别图片中的内容。

但需要注意：

并不是所有 Chat Models 都支持多模态输入。具体需要根据供应商和模型的能力判断。

---

## 六、Models 与 Runnable 的关系

这是一个比较重要的知识点。

在 LangChain 中，Chat Models 不仅仅是一个普通的 Python 对象，它们也实现了 Runnable 接口。

因此，模型可以参与 LangChain 的表达式组合（LCEL）。

例如：

```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

prompt = ChatPromptTemplate.from_template(
    "请回答以下问题：{question}"
)

chain = prompt | model | StrOutputParser()

result = chain.invoke({
    "question": "什么是 LangChain？"
})

print(result)
```

这个过程可以理解为：

```text
用户输入
   |
   v
PromptTemplate
   |
   v
生成消息
   |
   v
Chat Model
   |
   v
AIMessage
   |
   v
StrOutputParser
   |
   v
字符串结果
```

为什么可以通过 `|` 连接？

因为 Prompt、Model、Output Parser 等组件都实现了 Runnable 接口，能够遵循统一的输入输出规范。

这样就可以将多个组件组合成一个完整的执行链。

除了 `invoke()`，Runnable 还提供了：

- `stream()`：流式执行。
- `batch()`：批量执行。
- `ainvoke()`：异步执行。
- `astream()`：异步流式执行。
- `abatch()`：异步批量执行。

因此，理解 Models 时，也需要理解 Runnable 的基本概念。

---

## 七、Models 的实际应用场景

| 应用场景      | 使用能力                       |
| --------- | -------------------------- |
| 普通 AI 问答  | Chat Models + invoke       |
| AI 聊天机器人  | Chat Models + 消息历史         |
| 实时聊天界面    | Chat Models + stream       |
| 批量文本分类    | Chat Models + batch        |
| RAG 知识库   | Embeddings + Chat Models   |
| Agent 智能体 | Chat Models + Tool Calling |
| 数据自动提取    | Structured Output          |
| 图片理解      | 支持视觉能力的 Chat Models        |

---

## 八、Models 高级主题

前面的章节覆盖了 Models 的基础用法。本节介绍实际开发、尤其是生产环境中经常会用到的高级主题。

这些主题可以简单概括为一句话：**让模型调用更快、更稳、更省、更可控。**

| 高级主题 | 解决的核心问题 |
| ---- | -------- |
| 异步调用      | 高并发场景下的吞吐量 |
| 模型缓存      | 相同输入重复调用带来的浪费 |
| Fallbacks | 单个供应商故障导致服务不可用 |
| Rate Limiter | 触发供应商限流（429） |
| Token 追踪 | 成本不可见、预算失控 |
| Chunk 合并   | 流式结果难以聚合处理 |
| Tool Calling 进阶 | 工具调用行为不可控 |
| 上下文窗口管理 | 上下文超长导致调用失败 |
| 可配置模型     | 模型与代码耦合，切换成本高 |
| Fake 模型    | 测试依赖真实 API，慢且昂贵 |
| 自定义 Chat Model | 私有模型无法接入统一接口 |

### 8.1 异步调用

在 Web 服务中，一个请求往往要同时处理多项工作。

如果全部使用同步调用，一个模型请求在等待返回时，整个线程都会被阻塞。

LangChain 为每个核心方法都提供了异步版本：

| 同步方法     | 异步方法        |
| -------- | ----------- |
| invoke() | ainvoke()   |
| stream() | astream()   |
| batch()  | abatch()    |

例如并发处理多个独立问题：

```python
import asyncio

async def main():
    results = await asyncio.gather(
        model.ainvoke("什么是 Python？"),
        model.ainvoke("什么是 Java？"),
        model.ainvoke("什么是 LangChain？")
    )
    for result in results:
        print(result.content)

asyncio.run(main())
```

三个请求会并发执行，整体耗时接近最慢的那一个，而不是三者之和。

在 FastAPI、Django 等异步 Web 框架中，建议优先使用异步方法，避免模型调用阻塞事件循环。

### 8.2 模型缓存

模型调用既慢又贵，但很多应用的输入是重复的。

例如同一个知识库，大量用户都会问相似的问题。如果每次都真实调用模型，成本会白白翻倍。

LangChain 提供了 LLM 缓存机制，相同输入直接返回缓存结果：

```python
from langchain_core.globals import set_llm_cache
from langchain_core.caches import InMemoryCache

set_llm_cache(InMemoryCache())

response = model.invoke("什么是 LangChain？")
# 第一次：真实调用模型，结果写入缓存
# 第二次：相同输入直接命中缓存，不再调用模型
```

`InMemoryCache` 保存在进程内存中，重启即失效。

生产环境中可以使用持久化缓存，例如通过 langchain_community 提供 SQLiteCache，或自行实现基于 Redis 的缓存。

需要注意几点：

- 缓存匹配基于完整的输入内容，输入稍有不同就无法命中。
- 缓存发生在请求层，与 temperature 等参数无关，命中的是同一份结果。
- 有时效性要求的数据（如实时行情）不适合缓存，或需要设置较短的过期时间。

### 8.3 Fallbacks：模型回退

任何单一模型供应商都可能出问题：接口故障、限流、超时。

如果应用只依赖一个模型，供应商故障就意味着整个功能不可用。

LangChain 提供 `with_fallbacks()`，当主模型调用失败时，自动切换到备用模型：

```python
from langchain.chat_models import init_chat_model

primary_model = init_chat_model("openai:gpt-5.5")
backup_model = init_chat_model("anthropic:claude-sonnet-4")

model_with_fallbacks = primary_model.with_fallbacks(
    [backup_model]
)

response = model_with_fallbacks.invoke("你好")
```

执行过程可以理解为：

```text
        用户请求
           |
           v
      主模型调用
           |
      成功？ --是--> 返回结果
           |
           否
           |
           v
      备用模型调用
           |
           v
        返回结果
```

备用模型可以配置多个，LangChain 会按顺序依次尝试。

Fallbacks 不仅可以作用于模型，也可以作用于整条 Chain：

```python
chain_with_fallbacks = main_chain.with_fallbacks(
    [backup_chain]
)
```

实践建议：主备模型尽量来自不同供应商，避免两家同时故障的场景。

### 8.4 Rate Limiter：客户端限流

模型供应商通常对 API 有频率限制，超过后会返回 429 错误。

`max_retries` 可以应对偶发的 429，但在高并发场景下，更主动的方式是在客户端控制请求速率。

LangChain 提供了内置的限流器：

```python
from langchain_core.rate_limiters import InMemoryRateLimiter

rate_limiter = InMemoryRateLimiter(
    requests_per_second=0.5,   # 每秒最多 0.5 个请求
    max_bucket_size=10         # 桶容量，允许的瞬时突发量
)

model = init_chat_model(
    "openai:gpt-5.5",
    rate_limiter=rate_limiter,
    max_retries=3
)
```

它的工作方式类似令牌桶：请求先从桶中取令牌，没有令牌就等待，从而把请求速率平滑地控制在供应商允许的范围内。

限流器和重试机制配合使用效果最好：限流减少 429 的发生，重试兜底处理偶发失败。

### 8.5 Token 追踪与成本控制

模型按 Token 计费，如果应用从不统计 Token 用量，成本就是一笔糊涂账。

前面提到过，AIMessage 中的 `usage_metadata` 记录了本次调用的 Token 消耗：

```python
response = model.invoke("什么是 LangChain？")

print(response.usage_metadata)
# 例如：
# {
#     "input_tokens": 15,
#     "output_tokens": 320,
#     "total_tokens": 335
# }
```

流式调用默认拿不到精确用量，需要显式开启：

```python
full = None
usage = None

for chunk in model.stream(
    "请详细介绍一下 LangChain",
    stream_usage=True
):
    full = chunk if full is None else full + chunk
    if chunk.usage_metadata:
        usage = chunk.usage_metadata

print(usage)
```

注意：用量信息通常出现在最后一个 chunk 中，不同供应商的默认行为可能不同，需要以对应集成的文档为准。

有了用量数据，就可以做进一步的成本控制：

- 记录每次调用的模型名、Token 数、时间，写入日志或数据库。
- 按用户、按业务模块统计成本。
- 设置单请求或单用户的 Token 预算上限，超出后拒绝或降级。

### 8.6 流式进阶：Chunk 合并

`stream()` 返回的是一个个 `AIMessageChunk` 片段。

很多场景下，我们既想流式展示内容，又想在流结束后拿到一个完整的消息对象做后续处理。

`AIMessageChunk` 支持使用 `+` 进行合并：

```python
full = None

for chunk in model.stream("什么是 LangChain？"):
    print(chunk.content, end="", flush=True)
    full = chunk if full is None else full + chunk

# 流结束后，full 已经聚合成接近完整 AIMessage 的对象
print(full.content)
```

另外，在复杂的 Chain 或 Agent 中，如果需要观察每个中间步骤的流式输出，可以使用 `astream_events()`，它会把链路中每个组件产生的事件按顺序推送出来，是调试和构建可视化界面的重要工具。

### 8.7 Tool Calling 进阶

基础用法中，模型自主决定是否调用工具。但实际业务经常需要更强的控制力。

**tool_choice：控制调用行为**

```python
model_with_tools = model.bind_tools(
    [get_weather],
    tool_choice="get_weather"   # 强制调用指定工具
)
```

常见取值：

| 取值           | 行为             |
| ------------ | -------------- |
| "auto"（默认） | 模型自主决定是否调用     |
| "any"        | 强制调用某个工具，具体由模型选择 |
| 指定工具名        | 强制调用该工具        |

强制模式适用于"这一步必须走工具"的确定性流程，例如查询订单前必须调用鉴权工具。

**parallel_tool_calls：禁止并行调用**

```python
model_with_tools = model.bind_tools(
    [get_weather, get_time],
    parallel_tool_calls=False   # 一次只生成一个工具调用
)
```

部分模型默认可能一次生成多个工具调用，关闭后每轮只产生一个，便于控制执行顺序。

**strict：严格参数校验**

```python
model_with_tools = model.bind_tools(
    [get_weather],
    strict=True
)
```

开启后，参数 Schema 会以更严格的方式约束模型生成，减少参数缺失或格式错误。

注意：strict 的支持情况因供应商而异。

**工具执行失败的处理**

工具执行后如果出错，不要静默忽略，应该把错误信息包装成 ToolMessage 回传给模型：

```text
模型生成 Tool Call
      |
      v
  执行工具失败
      |
      v
ToolMessage("错误：城市名称无效")
      |
      v
模型看到错误，向用户澄清或重试
```

模型往往能根据错误信息自行修正参数并重试，这比直接把异常抛给用户体验好得多。

### 8.8 上下文窗口管理

每个模型都有上下文窗口上限。多轮对话、RAG 检索结果、长文档都会不断挤占窗口，超出后调用会直接失败。

常见的应对策略有两类。

**裁剪（Trimming）**

保留最近的消息，裁掉较早的内容：

```python
from langchain_core.messages import trim_messages

trimmed = trim_messages(
    messages,
    max_tokens=3000,
    token_counter=model,     # 使用模型对应的计数器
    strategy="last",         # 保留最近的消息
    include_system=True      # 始终保留 SystemMessage
)

response = model.invoke(trimmed)
```

裁剪简单直接，适合大多数聊天场景，代价是丢失早期上下文。

**压缩（Summarization）**

当历史过长时，先用模型把旧对话总结成一段摘要，用摘要替换原始消息。

这样保留了关键信息，代价是多一次模型调用。复杂场景下还可以采用分层策略：近几轮保留原文，更早的内容替换为摘要。

实践建议：

- 不要等到报错才处理，应在每次组装输入前主动检查 Token 数量。
- 在 LangGraph / Agent 应用中，这项工作通常由 Memory、Checkpointer 等机制配合完成，Models 只负责最终接收合法的输入。

### 8.9 可配置模型与运行时路由

不同任务的模型需求不同：

| 任务类型       | 合适的模型选择      |
| ---------- | ------------ |
| 简单分类、格式化   | 小模型，追求低成本    |
| 复杂推理、Agent | 大模型，追求高质量    |
| 超长文档处理     | 长上下文模型       |

如果模型名硬编码在业务代码里，切换模型就要改代码、重新部署。

LangChain 支持把模型做成可配置项，在运行时动态指定：

```python
from langchain.chat_models import init_chat_model

configurable_model = init_chat_model(
    "openai:gpt-5.5",
    model_provider="openai",
    configurable_fields="model"    # 允许运行时切换模型
)

# 运行时切换到另一个供应商的模型
response = configurable_model.invoke(
    "你好",
    config={"configurable": {"model": "anthropic:claude-sonnet-4"}}
)
```

更进一步，可以把"任务类型 -> 模型"的路由逻辑写在代码里，形成简单的模型路由层：

```text
        用户任务
           |
           v
       路由逻辑
           |
   +-------+--------+
   |       |        |
   v       v        v
 小模型   大模型   长上下文模型
```

这一层不需要复杂框架，一个字典加少量判断就能覆盖大部分场景，但它能显著降低模型依赖和整体成本。

动态模型会在运行时根据当前状态和上下文进行选择。这支持复杂的路由逻辑和成本优化。

要使用动态模型，请使用@wrap_model_call装饰器创建中间件，并在请求中修改模型。

```python
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain.agents.middleware import wrap_model_call, ModelRequest, ModelResponse


basic_model = ChatOpenAI(model="gpt-5.4-mini")
advanced_model = ChatOpenAI(model="gpt-5.5")

@wrap_model_call
def dynamic_model_selection(request: ModelRequest, handler) -> ModelResponse:
    """Choose model based on conversation complexity."""
    message_count = len(request.state["messages"])

    if message_count > 10:
        # Use an advanced model for longer conversations
        model = advanced_model
    else:
        model = basic_model

    return handler(request.override(model=model))

agent = create_agent(
    model=basic_model,  # Default model
    tools=[],
    middleware=[dynamic_model_selection]
)
```

### 8.10 Fake 模型：不花钱的测试

单元测试不应该依赖真实模型 API：

- 调用慢，拖垮测试速度。
- 花钱，且结果不确定，无法断言。
- 离线环境无法运行。

LangChain 提供了 Fake 模型用于测试。

**FakeListChatModel：按预设顺序返回内容**

```python
from langchain_core.language_models.fake_chat_models import (
    FakeListChatModel
)

fake_model = FakeListChatModel(
    responses=["第一次回答", "第二次回答"]
)

print(fake_model.invoke("任意输入").content)
# 第一次调用：第一次回答
# 第二次调用：第二次回答
```

**GenericFakeChatModel：模拟流式输出**

```python
from itertools import repeat
from langchain_core.language_models.fake_chat_models import (
    GenericFakeChatModel
)

fake_model = GenericFakeChatModel(
    messages=repeat("流式测试内容")
)

for chunk in fake_model.stream("任意输入"):
    print(chunk.content, end="")
```

Fake 模型让 Chain、Agent 的流程逻辑可以被稳定断言，是构建可靠应用的重要工具。

### 8.11 自定义 Chat Model

当团队有内部模型或私有推理网关，而 LangChain 没有现成集成时，可以自己实现一个 Chat Model，让它接入统一的接口体系。

核心方式是继承 `BaseChatModel`，实现 `_generate` 方法：

```python
from typing import List, Optional
from langchain_core.callbacks import (
    CallbackManagerForLLMRun
)
from langchain_core.language_models.chat_models import (
    BaseChatModel
)
from langchain_core.messages import AIMessage, BaseMessage
from langchain_core.outputs import ChatGeneration, ChatResult


class MyChatModel(BaseChatModel):

    @property
    def _llm_type(self) -> str:
        return "my-chat-model"

    def _generate(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs
    ) -> ChatResult:
        # 1. 将 messages 转换成内部服务的请求格式
        # 2. 调用内部推理服务
        # 3. 将结果包装成 AIMessage
        content = call_internal_service(messages, stop)

        return ChatResult(
            generations=[
                ChatGeneration(message=AIMessage(content=content))
            ]
        )
```

实现后，`invoke()`、`stream()`、LCEL 组合等能力自动可用。

需要注意两点：

- 生产环境应同时实现 `_agenerate` 异步方法，否则异步调用会退化为同步执行。
- 如果不实现 `bind_tools` 相关逻辑，自定义模型无法参与 Tool Calling 流程，Agent 场景需要额外适配。

---

## 九、小结

Models 模块从基础到进阶，可以概括为以下层次：

| 层次    | 内容                            |
| ----- | ----------------------------- |
| 基础调用  | Chat Models、Embeddings、初始化、参数 |
| 核心方法  | invoke、stream、batch           |
| 核心能力  | Tool Calling、Structured Output、多模态 |
| 组合能力  | Runnable、LCEL                 |
| 生产能力  | 异步、缓存、Fallbacks、限流、成本控制、上下文管理、测试、自定义模型 |
