# LangChain 核心组件：Messages（消息）

## 一、什么是 Messages？

在 LangChain 中，Messages（消息）是模型与外部世界进行交互的基本数据结构。

我们平时调用大模型，通常会这样提问：

```python
response = model.invoke("你好，请介绍一下 LangChain")
```

但实际上，大模型并不只是接收一段普通字符串。

尤其是 Chat Model，它通常以一组具有不同角色和属性的消息作为输入。

例如：

```python
from langchain.messages import SystemMessage, HumanMessage

messages = [
    SystemMessage(content="你是一个专业的 Python 助手"),
    HumanMessage(content="什么是 LangChain？")
]

response = model.invoke(messages)
```

这些消息不仅包含文本，还包含消息角色、附加信息、工具调用信息等。

**可以把 Messages 理解为：LangChain 用来统一表示模型输入和输出的标准消息格式。**

它解决了不同模型提供商之间消息格式不统一的问题。例如 OpenAI、Anthropic 等模型的底层消息表示可能不同，但在 LangChain 中，可以通过统一的 Message 对象进行交互。

---

## 二、Messages 的核心消息类型

LangChain 中常见的消息类型主要有以下几种：

| 消息类型 | 作用 | 对应角色 |
|---|---|---|
| `SystemMessage` | 定义模型的行为和约束 | system |
| `HumanMessage` | 表示用户输入 | user |
| `AIMessage` | 表示模型输出 | assistant |
| `ToolMessage` | 表示工具执行结果 | tool |
| `ChatMessage` | 自定义角色的消息 | 自定义 |

### 1. SystemMessage

SystemMessage 用于设置模型的整体行为、身份和约束。

```python
from langchain.messages import SystemMessage

message = SystemMessage(
    content="你是一个专业的 Python 开发工程师，请使用中文回答问题。"
)
```

常见用途：

- 设置模型的身份。
- 规定模型的回答风格。
- 设置模型需要遵守的规则。
- 限定模型的行为范围。

需要注意：SystemMessage 是传递给模型的指令，并不意味着模型一定能够严格遵守。

### 2. HumanMessage

HumanMessage 表示用户发送给模型的消息。

```python
from langchain.messages import HumanMessage

message = HumanMessage(
    content="请介绍一下 Python 的装饰器"
)
```

在实际开发中，用户的问题通常通过 HumanMessage 传递。

它不仅支持普通文本，也可以承载图片等多模态内容。

### 3. AIMessage

AIMessage 表示模型返回的消息。

```python
from langchain.messages import AIMessage

message = AIMessage(
    content="Python 装饰器本质上是一种高阶函数。"
)
```

需要特别注意：

**AIMessage 不仅仅用于保存模型生成的文本，还可以保存模型的工具调用信息。**

例如模型决定调用一个搜索工具：

```python
AIMessage(
    content="",
    tool_calls=[
        {
            "name": "search",
            "args": {"query": "LangChain"},
            "id": "call_123",
            "type": "tool_call"
        }
    ]
)
```

这意味着模型的输出不一定是给用户看的自然语言，也可能是一个要求 Agent 执行工具的指令。

这是理解 LangChain Agent 工作机制的重要基础。

### 4. ToolMessage

ToolMessage 用于保存工具执行之后返回的结果。

例如：

```python
from langchain.messages import ToolMessage

message = ToolMessage(
    content="搜索结果：LangChain 是一个大模型应用开发框架。",
    tool_call_id="call_123"
)
```

其中 `tool_call_id` 非常重要，它用于关联之前 AIMessage 中的工具调用。

一个典型的工具调用过程：

1. AIMessage：模型决定调用搜索工具。
2. 工具执行：程序实际调用搜索 API。
3. ToolMessage：保存搜索工具返回的结果。
4. AIMessage：模型根据搜索结果生成最终回答。

注意：ToolMessage 本身不负责执行工具，它只是用于表示工具执行后的结果。

### 5. ChatMessage

ChatMessage 是一种可以指定自定义角色的消息类型。

```python
from langchain.messages import ChatMessage

message = ChatMessage(
    role="reviewer",
    content="这段代码存在潜在的空指针问题。"
)
```

当内置消息角色不能满足需求时，可以使用它。

不过在常规的 Chat Model 开发中，通常优先使用 SystemMessage、HumanMessage 和 AIMessage 等标准类型。

---

## 三、Message 的内部结构

理解消息类型之后，还需要了解一个问题：

**一个 Message 对象内部究竟保存了哪些数据？**

以 AIMessage 为例，它并不是一个简单的字符串，而是包含多个属性的对象。

常见属性如下：

| 属性 | 作用 |
|---|---|
| `content` | 消息主体内容 |
| `type` | 消息类型 |
| `id` | 消息标识符 |
| `name` | 消息名称，可选 |
| `additional_kwargs` | 模型提供商相关的附加数据 |
| `response_metadata` | 模型响应元数据 |
| `usage_metadata` | Token 使用统计 |
| `tool_calls` | 模型请求执行的工具调用 |
| `invalid_tool_calls` | 无效的工具调用信息 |

例如：

```python
response = model.invoke("什么是 LangChain？")

print(response.content)
print(response.type)
print(response.response_metadata)
print(response.usage_metadata)
```

### 1. content：消息主体

content 是消息最核心的属性，用于保存消息内容。

普通文本场景：

```python
AIMessage(
    content="LangChain 是一个大模型应用开发框架。"
)
```

但是需要注意，content 不一定是字符串。

在多模态场景下，它也可以是由多个内容块组成的列表，例如：

```python
HumanMessage(
    content=[
        {"type": "text", "text": "这张图片里面是什么？"},
        {
            "type": "image",
            "url": "https://example.com/cat.jpg"
        }
    ]
)
```

实际使用时，图片、音频和视频的具体数据格式取决于模型提供商及其适配器。

### 2. additional_kwargs：附加信息

这个属性主要用于保存模型提供商相关的原始附加信息。

例如，某些模型返回的工具调用数据，或者提供商特有的响应字段。

```python
print(response.additional_kwargs)
```

需要注意：

- 它不是消息主体。
- 它不一定在所有模型响应中都有内容。
- 不同模型提供商返回的数据可能不同。

在实际开发中，如果需要使用 LangChain 已经标准化的属性，优先使用对应的标准属性，而不是直接依赖 `additional_kwargs`。

### 3. response_metadata：响应元数据

用于保存模型调用过程中的一些信息。

例如：

- 使用的模型名称。
- 模型结束生成的原因。
- 提供商返回的其他响应信息。

```python
print(response.response_metadata)
```

这部分在日志记录、模型调用监控和问题排查时比较有用。

### 4. usage_metadata：Token 使用统计

用于记录本次模型调用的 Token 消耗情况。

例如：

```python
print(response.usage_metadata)
```

可能包含：

```python
{
    "input_tokens": 100,
    "output_tokens": 50,
    "total_tokens": 150
}
```

通过这些信息，可以统计模型的输入和输出 Token 数量，进一步估算 API 调用成本。

注意：不同模型和集成的统计字段、详细程度可能不同。

---

## 四、Messages 的多模态支持

现在的大模型已经不局限于处理文本，还可以处理图片、音频和视频。

因此，LangChain 的 Messages 也支持多模态内容。

主要包括：

| 类型 | 作用 |
|---|---|
| Text | 普通文本 |
| Image | 图片 |
| Audio | 音频 |
| Video | 视频 |
| Structured data | 结构化数据 |

例如，用户发送一张图片：

```python
from langchain.messages import HumanMessage

message = HumanMessage(
    content=[
        {"type": "text", "text": "请描述这张图片"},
        {
            "type": "image",
            "url": "https://example.com/image.jpg"
        }
    ]
)

response = model.invoke([message])
print(response.content)
```

这里需要理解一个关键点：

**Messages 负责组织和传递多模态数据，但真正能否识别图片、理解音频，取决于底层模型是否支持对应的模态，以及模型集成是否正确实现了数据转换。**

例如，纯文本模型不能因为接收了一个 Image 类型的消息，就自动获得图片识别能力。

---



## 五、Messages 的实际使用

### 1. 将 Messages 传递给 Chat Model

最常见的方式就是将消息列表传递给模型。

```python
from langchain_openai import ChatOpenAI
from langchain.messages import SystemMessage, HumanMessage

model = ChatOpenAI(model="gpt-4o-mini")

messages = [
    SystemMessage(
        content="你是一个专业的 Python 助手"
    ),
    HumanMessage(
        content="什么是 Python 装饰器？"
    )
]

response = model.invoke(messages)

print(response.content)
```

### 2. 使用消息列表实现多轮对话

很多初学者会有一个疑问：

为什么模型能够记住之前的对话？

实际上，普通的 Chat Model 并不会天然保存跨请求的对话历史。

我们可以通过维护一个 Messages 列表，将之前的对话一起传递给模型。

```python
from langchain.messages import HumanMessage, AIMessage

messages = [
    HumanMessage(content="我叫小明"),
    AIMessage(content="你好，小明！"),
    HumanMessage(content="我叫什么名字？")
]

response = model.invoke(messages)

print(response.content)
```

模型能够回答小明，是因为本次调用时，之前的对话也被放进了输入消息列表中。

典型的多轮对话结构：

```python
messages = [
    HumanMessage("我叫小明"),
    AIMessage("你好，小明！"),

    HumanMessage("我喜欢 Python"),
    AIMessage("Python 是一门很实用的语言"),

    HumanMessage("我叫什么名字？")
]
```

因此：

**Messages 本身只是消息数据结构，不负责长期记忆。对话历史的保存、读取和管理，需要由应用程序或 LangGraph 等状态管理机制来完成。**

### 3. 使用消息模板简化开发

在实际项目中，我们不一定需要手动创建每一条消息。

LangChain 还提供了消息模板机制，可以动态填充变量。

```python
from langchain_core.prompts import ChatPromptTemplate

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个专业的 {role}"),
    ("human", "{question}")
])

messages = prompt.format_messages(
    role="Python 开发工程师",
    question="什么是装饰器？"
)

response = model.invoke(messages)
print(response.content)
```

这里的 `ChatPromptTemplate` 可以理解为 Messages 的模板生成器。

它负责根据变量生成最终的消息列表。

两者的区别：

- Messages：真正的消息数据。
- ChatPromptTemplate：用于生成消息的模板。

---

## 六、Messages 与 Agent 的关系

这一部分非常重要，因为 Messages 是理解 Agent 执行流程的基础。

Agent 并不是简单地调用一次模型就结束了，而是一个不断交互的过程。

其中 Messages 承担了记录模型决策、工具调用和工具执行结果的作用。

### 1. Agent 工具调用的完整消息流程

假设我们有一个天气查询工具，用户询问：

```text
北京今天的天气怎么样？
```

Agent 的执行过程大致如下：

**第一步：HumanMessage**

用户提出问题：

```python
HumanMessage(
    content="北京今天的天气怎么样？"
)
```

**第二步：AIMessage**

模型决定调用天气工具：

```python
AIMessage(
    content="",
    tool_calls=[
        {
            "name": "get_weather",
            "args": {"city": "北京"},
            "id": "call_123"
        }
    ]
)
```

**第三步：ToolMessage**

天气工具执行完成，返回：

```python
ToolMessage(
    content="北京今天晴天，气温 20℃",
    tool_call_id="call_123"
)
```

**第四步：AIMessage**

模型根据工具结果生成最终回答：

```python
AIMessage(
    content="北京今天晴天，气温 20℃，适合外出。"
)
```

这里可以看到，Agent 的一次完整交互，实际上由多条不同类型的 Message 组成。

需要特别注意：

- AIMessage 负责表达模型的决策和响应。
- ToolMessage 负责记录工具执行结果。
- Agent 或工具执行框架负责真正执行工具。
- 模型根据新的消息上下文决定下一步操作。

这就是 Agent 的核心工作机制之一。

### 2. Messages 与 LangGraph State 的关系

结合 LangGraph，可以进一步理解两者的关系。

例如：

```python
from langgraph.graph import MessagesState

class AgentState(MessagesState):
    pass
```

这里的 `MessagesState` 本质上就是一个以 `messages` 为核心字段的状态结构。

它允许 LangGraph 在图的执行过程中持续维护消息历史。

可以这样理解：

| 概念 | 职责 |
|---|---|
| Message | 表示一条消息 |
| Messages | 多条消息组成的消息列表 |
| MessagesState | 用于管理消息列表的图状态 |
| Checkpointer | 持久化图状态，支持后续恢复 |

因此，Messages 是消息数据结构，而 MessagesState 是对消息历史进行状态管理的一种方式。

两者不要混淆。

---

## 七、其他值得了解的功能

### 1. AIMessageChunk：流式消息

在调用模型时，我们经常会使用流式输出：

```python
for chunk in model.stream("介绍一下 LangChain"):
    print(chunk.content, end="", flush=True)
```

这里返回的并不是完整的 AIMessage，而是 AIMessageChunk。

可以理解为：

- AIMessage：完整的模型响应。
- AIMessageChunk：模型响应过程中的一个片段。

多个 Chunk 可以通过 `+` 操作组合成完整消息。

```python
full_message = None

for chunk in model.stream("你好"):
    if full_message is None:
        full_message = chunk
    else:
        full_message += chunk

print(full_message)
```

这个功能在聊天窗口实时展示模型回答时非常有用。

### 2. RemoveMessage：删除消息

在 LangGraph 中，还可以使用 RemoveMessage 表示删除指定的消息。

例如：

```python
from langchain.messages import RemoveMessage

message = RemoveMessage(id="message_123")
```

它通常配合 `MessagesState` 的消息管理机制使用。

注意：创建 RemoveMessage 并不等于直接从 Python 列表中删除元素，而是通过消息状态更新机制表达删除操作。

### 3. 消息格式的简写方式

实际上，LangChain 不一定要求我们每次都实例化消息对象。

也可以使用元组：

```python
messages = [
    ("system", "你是一个 Python 助手"),
    ("human", "什么是装饰器？"),
    ("ai", "装饰器是一种高阶函数")
]

response = model.invoke(messages)
```

也可以使用字典：

```python
messages = [
    {"role": "system", "content": "你是一个 Python 助手"},
    {"role": "user", "content": "什么是装饰器？"}
]

response = model.invoke(messages)
```

另外，直接传入字符串：

```python
response = model.invoke("你好")
```

通常相当于传入一条 HumanMessage。

这些都是为了简化开发而提供的不同输入形式。

---

## 八、实际开发中的注意事项

### 1. 不要把 Message 和普通字符串混为一谈

字符串主要用于简单的文本输入，而 Message 可以承载：

- 角色信息。
- 多模态内容。
- 工具调用信息。
- Token 使用信息。
- 模型响应元数据。

因此，涉及 Agent、多轮对话和多模态任务时，推荐使用 Message。

### 2. 不要随意丢弃 AIMessage 中的 tool_calls

这是 Agent 开发中比较容易出现的问题。

例如，模型返回：

```python
response = model.invoke(messages)

print(response.content)
```

如果只保存 `response.content`，可能会丢失工具调用信息。

因为模型有可能返回：

```python
AIMessage(
    content="",
    tool_calls=[...]
)
```

此时真正重要的信息在 `tool_calls` 中，而不是 `content` 中。

所以，建议在 Agent 开发中尽可能保留完整的 AIMessage 对象。

### 3. 注意消息历史长度

随着对话不断进行，Messages 列表可能越来越长。

而模型的上下文窗口是有限的。

因此，在实际项目中，通常需要结合以下机制管理消息历史：

- 消息裁剪（Trimming）。
- 消息摘要（Summarization）。
- 历史消息持久化。
- 上下文编辑（Context Editing）。

这些也是 LangChain Middleware 和 LangGraph 状态管理中非常重要的应用场景。

---

## 九、总结：Messages 在 LangChain 中的定位

| 知识点 | 需要掌握的内容 |
|---|---|
| Message | 统一的消息数据结构 |
| SystemMessage | 设置模型行为 |
| HumanMessage | 表示用户输入 |
| AIMessage | 表示模型响应及工具调用 |
| ToolMessage | 表示工具执行结果 |
| content | 消息主体，可以是文本或多模态数据 |
| metadata | 消息附加信息及模型调用信息 |
| AIMessageChunk | 流式输出片段 |
| RemoveMessage | 消息删除标记 |
| MessagesState | LangGraph 中管理消息历史的状态结构 |

**最重要的一个理解是：**

Messages 并不是一个负责调用大模型的组件，也不是一个负责保存长期记忆的组件。

它更像是整个 LangChain 生态中的一套标准通信协议（从工程抽象的角度理解）。

- 模型通过它接收输入、返回结果。
- Agent 通过它记录决策和工具调用。
- LangGraph 通过它维护执行过程中的消息状态。

这也是为什么 Messages 虽然看起来只是一些简单的数据类型，却是 LangChain 中非常基础且重要的组件。

---

## 十、官方文档

建议结合官方文档继续学习：

- [LangChain Messages 官方文档](https://docs.langchain.com/oss/python/langchain/messages)
- [LangChain Models 官方文档](https://docs.langchain.com/oss/python/langchain/models)
- [LangChain Tools 官方文档](https://docs.langchain.com/oss/python/langchain/tools)
