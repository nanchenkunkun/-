# LangChain 核心组件：Event Streaming（事件流）

## 1. 什么是 Event Streaming？

Event Streaming（事件流）是 LangChain / LangGraph 中用于实时获取程序执行过程信息的一种机制。

在传统调用方式中，我们通常使用 `invoke()` 或 `ainvoke()` 执行一个 Runnable，等程序执行结束后，才能拿到最终结果。

例如：

```python
result = chain.invoke({
    "question": "什么是人工智能？"
})

print(result)
```

这种方式存在一个问题：我们只能拿到最终结果，却无法直接观察中间执行过程。

但在实际 Agent 应用中，一次任务可能包含多个步骤：

1. 用户提交问题。
2. Agent 调用大模型分析问题。
3. 大模型决定调用某个工具。
4. 工具执行并返回结果。
5. Agent 再次调用大模型。
6. 大模型生成最终回答。

如果我们只使用 `invoke()`，就只能在整个流程执行完成后拿到结果。

而 Event Streaming 可以让我们实时观察这些执行过程，例如：

- 大模型什么时候开始调用？
- 大模型当前生成了什么内容？
- Agent 调用了哪个工具？
- 工具执行是否完成？
- 某个 Runnable 是否发生异常？
- 当前执行到了哪个阶段？

**简单来说：Event Streaming 就是把程序执行过程中产生的信息，以事件流的形式实时暴露出来。**

### 1.1 为什么需要 Event Streaming？

Event Streaming 主要解决三个问题。

**第一：实时展示模型输出**

例如 ChatGPT 的对话界面，通常不是等模型生成完整答案之后才一次性显示，而是逐步展示生成的文本。

**第二：观察 Agent 执行过程**

Agent 可能执行多个步骤，也可能多次调用工具。通过事件流，可以将这些过程展示在前端。

**第三：监控和调试程序**

通过事件流可以观察 Runnable 的执行情况，帮助开发者定位问题，例如某个工具执行时间过长、模型调用失败等。

---

## 2. Event Streaming 在 LangChain 中的位置

在 LangChain 中，Runnable 是一个非常重要的抽象。

Prompt、Chat Model、Output Parser、Retriever、Tool，以及由这些组件组合而成的 Chain，都可以通过 Runnable 接口进行调用。

Runnable 提供了多种执行方式：

| 方法 | 作用 |
|---|---|
| `invoke()` | 同步执行，返回最终结果 |
| `ainvoke()` | 异步执行，返回最终结果 |
| `batch()` | 批量同步执行 |
| `abatch()` | 批量异步执行 |
| `stream()` | 同步流式获取输出 |
| `astream()` | 异步流式获取输出 |
| `astream_events()` | 异步获取执行事件 |
| `stream_events()` | 同步获取执行事件，支持的版本取决于 Runnable 实现 |

这里需要区分两个概念：

- **Output Streaming**：关注最终输出是如何逐步产生的。
- **Event Streaming**：关注整个 Runnable 执行过程中产生了哪些事件。

例如：

```python
for chunk in model.stream("介绍一下 LangChain"):
    print(chunk.content)
```

这里关注的是模型生成的文本。

而：

```python
async for event in chain.astream_events(
    {"question": "介绍一下 LangChain"},
    version="v2"
):
    print(event)
```

这里关注的是整个 Chain 执行期间产生的事件。

因此，Event Streaming 的观察范围通常比单纯的模型文本流更广。

官方文档：

- [Runnable API Reference](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable)
- [Runnable.astream_events API Reference](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/astream_events)

---

## 3. Runnable 的三种 Streaming API

LangChain 中有三个比较重要的 Streaming API：

- `stream()` / `astream()`
- `astream_events()`
- `astream_log()`

它们虽然都和 Streaming 有关，但关注点并不相同。

### 3.1 stream() / astream()

这两个方法主要用于获取 Runnable 的输出内容。

同步示例：

```python
for chunk in chain.stream(inputs):
    print(chunk)
```

异步示例：

```python
async for chunk in chain.astream(inputs):
    print(chunk)
```

它们的主要特点：

- 直接返回 Runnable 的输出片段。
- 适合模型文本的实时展示。
- 不会自动提供完整的执行事件信息。

需要注意，`stream()` 和 `astream()` 能否真正逐块输出，取决于 Runnable 及其内部组件是否实现了流式处理能力。

### 3.2 astream_events()

`astream_events()` 用于获取 Runnable 执行期间产生的事件。

例如：

```python
async for event in chain.astream_events(
    inputs,
    version="v2"
):
    print(event)
```

它可以观察：

- Runnable 开始执行。
- Runnable 产生中间输出。
- Runnable 执行结束。
- 模型开始调用、输出 Token、调用结束。
- Tool 开始执行、执行结束。
- Retriever 开始检索、返回文档。
- 自定义业务事件。

与 `astream()` 不同，它并不只关注最终输出，而是关注执行过程中产生的事件。

### 3.3 astream_log()

`astream_log()` 用于流式获取 Runnable 的执行日志和中间状态变化。

它返回的内容基于运行日志和 JSON Patch。

例如：

```python
async for patch in chain.astream_log(inputs):
    print(patch)
```

可以理解为：

- `astream()`：关注输出内容。
- `astream_events()`：关注执行事件。
- `astream_log()`：关注执行日志及其状态变化。

实际开发中，普通的事件监听通常优先考虑 `astream_events()`。

---

## 4. astream_events() 的基本使用

`astream_events()` 是理解 LangChain Event Streaming 的核心 API。

它的基本结构如下：

```python
async for event in runnable.astream_events(
    input,
    version="v2"
):
    print(event)
```

主要参数：

| 参数 | 作用 |
|---|---|
| `input` | Runnable 的输入数据 |
| `config` | Runnable 的执行配置 |
| `version` | 事件协议版本 |
| `include_names` | 只监听指定名称的 Runnable |
| `include_types` | 只监听指定类型的 Runnable |
| `include_tags` | 只监听带有指定标签的 Runnable |
| `exclude_names` | 排除指定名称的 Runnable |
| `exclude_types` | 排除指定类型的 Runnable |
| `exclude_tags` | 排除带有指定标签的 Runnable |

### 4.1 version 参数

目前官方 API 提供了 `v1`、`v2` 和 `v3` 三种协议版本。

| 版本 | 说明 |
|---|---|
| v1 | 早期事件协议，主要用于兼容旧代码 |
| v2 | 标准事件字典协议，支持自定义事件 |
| v3 | 新版类型化、以内容块为中心的流式协议，支持范围受限 |

一般情况下：

- 普通 Runnable 事件监听，优先使用 `v2`。
- 需要新版 Chat Model 内容块流式能力，或者使用支持 v3 的 LangGraph 图，可以研究 `v3`。
- 新项目不建议依赖旧版 `v1`。

需要注意：`v3` 目前仍处于 Beta 阶段，并不是所有 Runnable 都支持。

官方文档：[astream_events](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/astream_events)

---

## 5. Event 事件结构

在 `astream_events(version="v2")` 中，事件通常是一个字典。

例如：

```python
{
    "event": "on_chat_model_stream",
    "name": "ChatOpenAI",
    "run_id": "123456",
    "parent_ids": ["parent-123"],
    "tags": [],
    "metadata": {},
    "data": {
        "chunk": AIMessageChunk(content="Hello")
    }
}
```

一个标准事件主要包含以下字段：

| 字段 | 含义 |
|---|---|
| `event` | 事件名称 |
| `name` | 产生事件的 Runnable 名称 |
| `run_id` | 当前 Runnable 本次运行的唯一 ID |
| `parent_ids` | 当前 Runnable 的父级运行 ID |
| `tags` | 当前 Runnable 的标签 |
| `metadata` | 当前 Runnable 的元数据 |
| `data` | 当前事件携带的数据 |

### 5.1 event

`event` 用来标识当前发生了什么事情。

例如：

```python
"on_chat_model_start"
"on_chat_model_stream"
"on_chat_model_end"
```

通过这个字段，可以判断当前事件属于哪种执行阶段。

### 5.2 name

`name` 表示产生事件的 Runnable 名称。

例如：

```python
"name": "ChatOpenAI"
```

如果我们给某个 Runnable 设置了 `run_name`，事件中也可以使用自定义名称。

### 5.3 run_id

每次 Runnable 执行都会产生一个独立的运行 ID。

例如：

```text
Chain
  run_id = A

  ChatModel
    run_id = B

  OutputParser
    run_id = C
```

即使是同一个模型，多次调用也会产生不同的 `run_id`。

它可以帮助我们区分不同的运行实例。

### 5.4 parent_ids

`parent_ids` 用于记录当前 Runnable 的父级执行关系。

例如：

```text
Chain (run_id=A)
  |
  +-- ChatModel (run_id=B)
  |
  +-- Tool (run_id=C)
```

那么：

- Chain 的 `parent_ids` 是空列表。
- ChatModel 的 `parent_ids` 是 `[A]`。
- Tool 的 `parent_ids` 是 `[A]`。

如果存在多层嵌套，`parent_ids` 会从最外层根 Runnable 开始，依次记录到直接父级。

注意：`parent_ids` 是 v2 协议的重要特性；v1 中该字段为空。

通过 `run_id` 和 `parent_ids`，我们就可以还原 Runnable 之间的调用关系。

### 5.5 tags 和 metadata

这两个字段用于给 Runnable 附加额外信息。

例如：

```python
chain = chain.with_config({
    "run_name": "question_chain",
    "tags": ["agent", "question"],
    "metadata": {
        "module": "chat"
    }
})
```

这些信息可以帮助我们在事件流中识别某个 Runnable，也可以用于过滤事件和调试。

### 5.6 data

`data` 是当前事件携带的具体数据。

需要注意：不同事件的 `data` 结构并不完全相同。

例如：

- 模型开始事件可能包含输入消息。
- 模型流式事件可能包含 `AIMessageChunk`。
- 模型结束事件可能包含完整输出。
- Tool 开始事件可能包含工具参数。
- Tool 结束事件可能包含工具执行结果。

因此，不能假设所有事件的 `data` 都具有相同结构。

---

## 6. 常见事件类型

LangChain 根据 Runnable 的类型和执行阶段，产生不同的事件。

一般形式：

```text
on_[runnable_type]_(start | stream | end)
```

其中：

- `start`：开始执行。
- `stream`：产生流式输出。
- `end`：执行完成。

### 6.1 Chat Model 事件

Chat Model 是最常见的事件来源。

| 事件 | 含义 |
|---|---|
| `on_chat_model_start` | Chat Model 开始调用 |
| `on_chat_model_stream` | Chat Model 产生消息块 |
| `on_chat_model_end` | Chat Model 调用结束 |

例如：

```python
async for event in model.astream_events(
    "你好",
    version="v2"
):
    if event["event"] == "on_chat_model_stream":
        chunk = event["data"]["chunk"]
        print(chunk.content, end="")
```

这个示例只处理模型生成的文本片段。

### 6.2 LLM 事件

对于传统 LLM 接口，可能出现：

| 事件 | 含义 |
|---|---|
| `on_llm_start` | LLM 开始调用 |
| `on_llm_stream` | LLM 产生文本片段 |
| `on_llm_end` | LLM 调用结束 |

Chat Model 和传统 LLM 的事件结构并不完全相同。

### 6.3 Chain 事件

Chain 也会产生对应的生命周期事件：

| 事件 | 含义 |
|---|---|
| `on_chain_start` | Chain 开始执行 |
| `on_chain_stream` | Chain 产生流式输出 |
| `on_chain_end` | Chain 执行结束 |
| `on_chain_error` | Chain 执行发生异常 |

例如：

```python
async for event in chain.astream_events(
    inputs,
    version="v2"
):
    if event["event"] == "on_chain_start":
        print("Chain 开始执行")

    elif event["event"] == "on_chain_end":
        print("Chain 执行完成")
```

### 6.4 Tool 事件

Tool 是 Agent 系统中非常重要的组件。

常见事件：

| 事件 | 含义 |
|---|---|
| `on_tool_start` | 工具开始执行 |
| `on_tool_end` | 工具执行结束 |
| `on_tool_error` | 工具执行发生异常 |

例如：

```python
async for event in agent.astream_events(
    inputs,
    version="v2"
):
    if event["event"] == "on_tool_start":
        print("工具开始执行：", event["name"])

    elif event["event"] == "on_tool_end":
        print("工具执行完成：", event["name"])
```

注意：这里的示例表达的是事件监听逻辑。具体 Agent 对象是否支持直接调用该 Runnable API，需要根据使用的 Agent 实现确认；LangGraph Agent 也可以使用其自身的图流式接口。

### 6.5 Retriever 事件

Retriever 负责检索相关文档。

常见事件：

| 事件 | 含义 |
|---|---|
| `on_retriever_start` | 开始检索 |
| `on_retriever_end` | 检索完成 |
| `on_retriever_error` | 检索发生异常 |

例如：

```python
async for event in retriever.astream_events(
    "什么是 RAG？",
    version="v2"
):
    if event["event"] == "on_retriever_end":
        documents = event["data"]["output"]
        print(documents)
```

### 6.6 Prompt 事件

PromptTemplate 也可以被观察。

| 事件 | 含义 |
|---|---|
| `on_prompt_start` | Prompt 开始格式化 |
| `on_prompt_end` | Prompt 格式化完成 |

这对于调试 Prompt 输入和最终消息结构很有帮助。

### 6.7 事件类型汇总

| Runnable 类型 | Start | Stream | End |
|---|---|---|---|
| Chat Model | `on_chat_model_start` | `on_chat_model_stream` | `on_chat_model_end` |
| LLM | `on_llm_start` | `on_llm_stream` | `on_llm_end` |
| Chain | `on_chain_start` | `on_chain_stream` | `on_chain_end` |
| Tool | `on_tool_start` | 不保证有标准 Stream 事件 | `on_tool_end` |
| Retriever | `on_retriever_start` | 不保证有标准 Stream 事件 | `on_retriever_end` |
| Prompt | `on_prompt_start` | 不保证有标准 Stream 事件 | `on_prompt_end` |

注意：

1. 并不是所有 Runnable 都有 `stream` 事件。
2. 某些组件只会产生 start 和 end 事件。
3. 具体事件取决于组件的实现。
4. 发生异常时，还可能产生对应的 error 事件。

官方参考：[Runnable Event Reference](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/astream_events)

---

## 7. 事件过滤

在实际开发中，整个 Agent 执行过程可能产生大量事件。

如果我们只关心模型输出，或者只想观察工具调用，就可以通过过滤参数减少不必要的事件。

### 7.1 include_names

只监听指定名称的 Runnable。

```python
async for event in chain.astream_events(
    inputs,
    version="v2",
    include_names=["ChatOpenAI"]
):
    print(event)
```

### 7.2 include_types

只监听指定类型的 Runnable。

```python
async for event in chain.astream_events(
    inputs,
    version="v2",
    include_types=["chat_model"]
):
    print(event)
```

### 7.3 include_tags

只监听具有指定标签的 Runnable。

```python
async for event in chain.astream_events(
    inputs,
    version="v2",
    include_tags=["important"]
):
    print(event)
```

### 7.4 exclude 参数

除了 include，还可以使用 exclude 排除指定事件来源：

```python
async for event in chain.astream_events(
    inputs,
    version="v2",
    exclude_types=["retriever"]
):
    print(event)
```

常见过滤参数：

| 参数 | 作用 |
|---|---|
| `include_names` | 按 Runnable 名称过滤 |
| `include_types` | 按 Runnable 类型过滤 |
| `include_tags` | 按标签过滤 |
| `exclude_names` | 排除指定名称 |
| `exclude_types` | 排除指定类型 |
| `exclude_tags` | 排除指定标签 |

实际使用时，可以组合这些过滤条件。

---

## 8. 自定义事件

除了 LangChain 内置的事件，开发者还可以主动发送自定义事件。

例如：

一个 Agent 任务需要执行三个步骤，我们希望在执行过程中向前端报告进度：

```text
任务开始
  |
  +-- 第一步完成
  |
  +-- 第二步完成
  |
  +-- 第三步完成
  |
任务结束
```

这时就可以使用自定义事件。

### 8.1 发送自定义事件

异步 Runnable 可以使用 `adispatch_custom_event()`：

```python
import asyncio

from langchain_core.runnables import RunnableLambda
from langchain_core.callbacks.manager import adispatch_custom_event


async def process_task(input, config):
    await asyncio.sleep(1)

    await adispatch_custom_event(
        "progress_event",
        {
            "message": "第一步执行完成",
            "progress": 33
        },
        config=config
    )

    await asyncio.sleep(1)

    await adispatch_custom_event(
        "progress_event",
        {
            "message": "第二步执行完成",
            "progress": 66
        },
        config=config
    )

    await asyncio.sleep(1)

    await adispatch_custom_event(
        "progress_event",
        {
            "message": "第三步执行完成",
            "progress": 100
        },
        config=config
    )

    return "任务完成"


task = RunnableLambda(process_task)
```

这里的 `config` 用于确保自定义事件能够关联到当前 Runnable 的执行上下文。对于 Python 3.10 以下版本，尤其需要显式传递。

### 8.2 监听自定义事件

```python
async for event in task.astream_events(
    None,
    version="v2"
):
    if event["event"] == "on_custom_event":
        print(event["name"])
        print(event["data"])
```

自定义事件的名称和数据由开发者决定。

例如：

```python
{
    "event": "on_custom_event",
    "name": "progress_event",
    "data": {
        "message": "第一步执行完成",
        "progress": 33
    }
}
```

自定义事件适合：

- 任务进度通知。
- Agent 执行阶段通知。
- 业务状态变化。
- 长时间任务的中间结果。
- 自定义监控指标。

注意：Runnable 的自定义事件通过 `astream_events(version="v2")` 可以正常监听。新版 v3 有自己的类型化事件协议，不能直接把 v2 的事件字典结构套用到 v3。

---

## 9. Runnable Config 与事件流

在 LangChain 中，Runnable 可以通过 `with_config()` 设置执行配置。

例如：

```python
chain = chain.with_config({
    "run_name": "my_chain",
    "tags": ["agent", "chat"],
    "metadata": {
        "module": "assistant"
    }
})
```

这些配置可以帮助我们识别事件来源。

| 配置 | 作用 |
|---|---|
| `run_name` | 指定 Runnable 的运行名称 |
| `tags` | 给 Runnable 添加标签 |
| `metadata` | 添加额外元数据 |

例如：

```python
async for event in chain.astream_events(
    inputs,
    version="v2"
):
    print("名称：", event["name"])
    print("标签：", event["tags"])
    print("元数据：", event["metadata"])
```

合理使用 `run_name`、`tags` 和 `metadata`，可以让事件流更容易被识别和管理。

Runnable 配置在调用链中可以向子 Runnable 传播，具体结果还会受到组件配置合并方式的影响。

---

## 10. Tool Calling 与 Streaming

在 Agent 应用中，有一个容易混淆的地方：

**模型生成工具调用参数的过程，与工具真正执行的过程，是两个不同阶段。**

例如：

```text
用户提问
  |
  v
大模型
  |
  | 生成工具调用请求
  v
Tool Call
  |
  | 工具真正执行
  v
Tool Result
  |
  v
大模型生成最终回答
```

### 10.1 模型生成 Tool Call

当模型决定调用工具时，可能会逐步生成工具调用名称和参数。

例如：

```python
AIMessageChunk(
    tool_call_chunks=[
        {
            "name": "search",
            "args": '{"query":',
            "id": None,
            "index": 0
        }
    ]
)
```

这些片段可能需要进行累积，最终才能形成完整的工具调用参数。

### 10.2 Tool 真正执行

当模型完成工具调用请求后，Agent 才会根据请求执行对应的 Tool。

例如：

```text
on_tool_start
    |
    v
执行工具
    |
    v
on_tool_end
```

所以：

- `on_chat_model_stream`：模型正在生成内容，内容可能包括工具调用片段。
- `on_tool_start`：工具开始真正执行。
- `on_tool_end`：工具执行完成。

这三个阶段不能混为一谈。

### 10.3 为什么需要区分？

如果我们在前端开发 Agent 执行过程展示功能，就需要明确区分：

1. 模型正在思考或生成内容。
2. 模型决定调用工具。
3. 工具正在执行。
4. 工具执行完成。
5. 模型继续生成回答。

这样才能构建更加准确的 Agent 执行过程可视化界面。

---

## 11. 一个完整的 LangChain Event Streaming 示例

下面使用 LCEL 组合 Prompt、Chat Model 和 Output Parser，构建一个简单的 Chain。

### 11.1 构建 Chain

```python
from langchain.chat_models import init_chat_model
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser


model = init_chat_model(
    "your-model-name",
    model_provider="your-provider"
)

prompt = ChatPromptTemplate.from_template(
    "请用通俗的语言介绍一下：{topic}"
)

chain = (
    prompt
    | model
    | StrOutputParser()
)
```

其中：

- PromptTemplate：负责构建模型输入。
- Chat Model：负责生成回答。
- StrOutputParser：负责将模型消息解析为字符串。

### 11.2 使用 stream() 获取文本

```python
for chunk in chain.stream({
    "topic": "人工智能"
}):
    print(chunk, end="", flush=True)
```

这个示例只关注最终文本的流式输出。

### 11.3 使用 astream_events() 监听执行过程

```python
import asyncio


async def main():
    inputs = {
        "topic": "人工智能"
    }

    async for event in chain.astream_events(
        inputs,
        version="v2"
    ):
        event_type = event["event"]

        if event_type == "on_chain_start":
            print("Chain 开始：", event["name"])

        elif event_type == "on_chat_model_start":
            print("模型开始调用")

        elif event_type == "on_chat_model_stream":
            chunk = event["data"]["chunk"]
            print(chunk.content, end="", flush=True)

        elif event_type == "on_chat_model_end":
            print("\n模型调用结束")

        elif event_type == "on_chain_end":
            print("\nChain 执行完成")


asyncio.run(main())
```

这个示例能够同时观察：

- Chain 的执行生命周期。
- 模型的调用过程。
- 模型生成的内容。
- 整个 Chain 的结束状态。

这也是 `astream_events()` 的主要价值。

---

## 12. Callback 与 Event Streaming 的区别

Callback 和 Event Streaming 都可以观察 Runnable 的执行过程，但它们的使用方式和定位不同。

### 12.1 Callback

Callback 是 LangChain 的回调机制。

开发者可以通过实现 Callback Handler，在指定生命周期事件发生时执行自己的逻辑。

例如：

```python
from langchain_core.callbacks import BaseCallbackHandler


class MyHandler(BaseCallbackHandler):

    def on_llm_start(self, *args, **kwargs):
        print("LLM 开始调用")

    def on_llm_end(self, *args, **kwargs):
        print("LLM 调用结束")

    def on_llm_new_token(self, token, **kwargs):
        print(token)
```

然后在 Runnable 调用时传入 Callback Handler。

### 12.2 Event Streaming

Event Streaming 则是通过迭代器的方式，主动消费执行期间产生的事件。

```python
async for event in chain.astream_events(
    inputs,
    version="v2"
):
    print(event)
```

### 12.3 两者的区别

| 对比项 | Callback | Event Streaming |
|---|---|---|
| 使用方式 | 注册回调函数 | 迭代消费事件 |
| 触发方式 | 事件发生时自动调用 | 开发者主动读取 |
| 主要用途 | 执行钩子、监控、日志 | 实时事件消费 |
| 数据处理 | 在回调函数中处理 | 在事件循环中处理 |
| 灵活性 | 适合自定义处理逻辑 | 适合统一处理事件流 |

可以这样理解：

**Callback 更像是事件发生时自动执行的钩子，而 Event Streaming 更像是把执行过程中产生的事件整理成一个可以持续读取的数据流。**

两者并不是互斥关系，Event Streaming 本身也依赖底层的回调和事件机制。

---

# 第二部分：LangGraph Event Streaming

## 13. LangGraph Streaming

前面介绍的 `astream_events()` 主要属于 LangChain Runnable 体系。

而 LangGraph 是建立在 LangChain Runnable 等基础能力之上的图执行框架，它有自己的一套 Streaming 机制。

LangGraph 的 Streaming 不只是为了展示模型生成的文本，还需要观察整个图的执行过程。

例如：

```text
用户输入
   |
   v
Agent Node
   |
   v
模型决定下一步
   |
   +-------> Tool Node
   |             |
   |             v
   |          工具结果
   |             |
   +-------------+
   |
   v
模型生成回答
   |
   v
最终结果
```

在这个过程中，我们可能关心：

- 当前执行了哪个节点？
- 节点更新了哪些 State？
- 模型生成了哪些消息？
- 当前调用了什么工具？
- 子图执行到了哪个阶段？
- 最终 State 是什么？

LangGraph 的 Streaming 就是为这些需求提供支持。

官方文档：

[LangGraph Streaming](https://docs.langchain.com/oss/python/langgraph/streaming)

---

## 14. LangGraph 的 stream() / astream()

LangGraph 传统的 Streaming 接口主要是：

```python
graph.stream()
graph.astream()
```

通过 `stream_mode` 可以决定需要获取哪一种数据。

### 14.1 values

返回每一步执行后的完整 State。

```python
for chunk in graph.stream(
    inputs,
    stream_mode="values"
):
    print(chunk)
```

适合观察整个图的状态变化。

### 14.2 updates

返回节点执行产生的状态更新。

```python
for chunk in graph.stream(
    inputs,
    stream_mode="updates"
):
    print(chunk)
```

例如：

```python
{
    "agent": {
        "messages": [...]
    }
}
```

它关注的是某个节点本次执行产生的更新，而不是完整 State。

### 14.3 messages

用于获取模型生成的消息块和相关元数据。

```python
for message_chunk, metadata in graph.stream(
    inputs,
    stream_mode="messages"
):
    print(message_chunk.content)
```

适合实时展示模型生成的文本。

### 14.4 custom

用于获取开发者自定义的数据。

可以在节点内部通过 `get_stream_writer()` 写入自定义事件。

### 14.5 checkpoints

用于获取图执行过程中产生的 Checkpoint。

需要图配置了 Checkpointer。

### 14.6 tasks

用于观察任务执行事件，例如任务开始、完成及其结果。

### 14.7 debug

用于获取更加详细的调试信息。

通常会包含 Checkpoint 和 Task 相关信息。

### 14.8 stream_mode 汇总

| 模式 | 作用 |
|---|---|
| `values` | 获取每一步执行后的完整 State |
| `updates` | 获取节点产生的 State 更新 |
| `messages` | 获取模型消息块 |
| `custom` | 获取自定义数据 |
| `checkpoints` | 获取 Checkpoint |
| `tasks` | 获取任务执行信息 |
| `debug` | 获取更详细的调试信息 |

注意：具体支持的模式和返回结构需要以当前安装版本为准。

---

## 15. LangGraph 新版 Event Streaming：stream_events()

除了传统的 `stream()` / `astream()`，LangGraph 还提供了新版 `stream_events()`。

它的主要目标是：将图执行过程中产生的事件统一组织起来，并通过类型化投影（Typed Projections）提供给开发者。

根据当前 LangGraph Python API Reference，`stream_events()` 从 LangGraph 1.2 开始提供。

它支持 `v1`、`v2` 和 `v3` 三种版本。其中 v3 是新版类型化事件流协议，目前仍属于实验性 API。

官方参考：

[LangGraph stream_events API Reference](https://reference.langchain.com/python/langgraph/pregel/main/Pregel/stream_events)

### 15.1 基本使用方式

同步调用：

```python
run = graph.stream_events(
    inputs,
    version="v3"
)

for message in run.messages:
    for token in message.text:
        print(token, end="", flush=True)

final_state = run.output
```

异步调用：

```python
run = await graph.astream_events(
    inputs,
    version="v3"
)

async for message in run.messages:
    async for token in message.text:
        print(token, end="", flush=True)

final_state = await run.output
```

这里需要理解：

1. `graph` 是编译后的 LangGraph 图。
2. `version="v3"` 表示使用新版事件流协议。
3. `run` 是一个 GraphRunStream 或 AsyncGraphRunStream 对象。
4. `run.messages` 是消息流投影。
5. `run.output` 用于获取最终输出。

与传统的事件字典不同，v3 返回的是一个具有多个类型化投影的运行流对象。

### 15.2 Typed Projections（类型化投影）

这是新版 `stream_events()` 最重要的概念。

它将一次图执行过程中产生的不同信息，拆分成多个独立的消费入口。

常见投影如下：

| 投影 | 作用 |
|---|---|
| `run.messages` | 获取模型消息流 |
| `run.values` | 获取图执行过程中的 State 快照 |
| `run.updates` | 获取节点产生的状态更新 |
| `run.custom` | 获取自定义事件 |
| `run.subgraphs` | 获取子图执行入口 |
| `run.checkpoints` | 获取 Checkpoint 事件 |
| `run.tasks` | 获取任务执行事件 |
| `run.debug` | 获取调试事件 |
| `run.lifecycle` | 获取子图生命周期事件 |
| `run.output` | 获取最终输出 |
| `run.interrupts` | 获取中断信息 |
| `run.interrupted` | 判断本次执行是否被中断 |
| `run.extensions` | 获取自定义 Transformer 提供的扩展投影 |

注意：部分投影依赖对应的事件来源。例如，没有配置 Checkpointer 时，`run.checkpoints` 不会产生 Checkpoint 数据。

### 15.3 为什么需要类型化投影？

假设我们需要同时完成以下工作：

1. 将模型生成的 Token 实时推送到前端。
2. 监听 Agent 的 State 变化。
3. 监听 Tool 执行过程。
4. 观察子图的执行状态。
5. 获取最终结果。

传统方式通常需要使用 `stream_mode`，然后在一个统一的循环中判断不同的数据类型。

新版接口则可以将不同类型的数据拆分到不同的投影入口。

例如：

```python
run = graph.stream_events(
    inputs,
    version="v3"
)

for state in run.values:
    print("当前状态：", state)

for update in run.updates:
    print("节点更新：", update)
```

异步方式可以分别消费不同投影：

```python
import asyncio


async def consume_messages(run):
    async for message in run.messages:
        async for token in message.text:
            print(token, end="", flush=True)


async def consume_values(run):
    async for state in run.values:
        print("当前状态：", state)


async def main():
    run = await graph.astream_events(
        inputs,
        version="v3"
    )

    await asyncio.gather(
        consume_messages(run),
        consume_values(run)
    )

    final_state = await run.output
    print(final_state)
```

需要注意：投影是单消费者流，不应重复迭代同一个投影。如果确实需要多个消费者，可以使用对应投影提供的 `tee()` / `atee()` 进行分流。

另外，v3 的图执行由调用方消费投影时驱动，并不是默认启动一个后台线程持续执行。

### 15.4 run.messages

`run.messages` 用于获取模型生成的消息流。

与传统 `stream_mode="messages"` 返回的消息块有所不同，v3 的 `run.messages` 会为每次模型调用提供一个消息流对象。

例如：

```python
async for message in run.messages:
    async for token in message.text:
        print(token, end="", flush=True)
```

消息流对象还提供其他内容投影，例如：

- `text`：文本内容。
- `reasoning`：推理内容（取决于模型支持情况）。
- `tool_calls`：工具调用相关内容。
- `usage`：Token 使用情况。
- `output`：最终消息输出。

这种设计更适合处理现代模型的多模态和结构化内容。

### 15.5 run.values 和 run.updates

这两个投影分别对应完整 State 和节点更新。

`run.values`：

```python
async for state in run.values:
    print(state)
```

用于观察每个步骤后的完整状态快照。

`run.updates`：

```python
async for update in run.updates:
    print(update)
```

用于观察节点本次执行产生的状态更新。

两者的区别：

| 对比 | values | updates |
|---|---|---|
| 数据内容 | 完整 State | 节点更新 |
| 关注点 | 当前整体状态 | 某个节点做了什么修改 |
| 适用场景 | 状态观察 | 执行过程展示 |

### 15.6 run.subgraphs

如果一个 LangGraph 中包含子图，可以通过 `run.subgraphs` 获取子图的执行入口。

例如：

```python
async for subgraph in run.subgraphs:
    print("发现子图")

    async for state in subgraph.values:
        print(state)
```

子图还可以继续包含其他子图。

通过这种方式，可以逐层观察嵌套图的执行过程。

这对于复杂 Agent 系统尤其有用，例如：

```text
主 Agent
   |
   +-- Research Agent
   |       |
   |       +-- Search Tool
   |
   +-- Writing Agent
           |
           +-- LLM
```

### 15.7 run.interrupts 和 run.interrupted

LangGraph 支持人工介入（Human-in-the-loop）。

当图执行到需要人工确认的位置时，可能会产生中断。

可以通过：

- `run.interrupts` 获取中断信息。
- `run.interrupted` 判断执行是否发生中断。

这对于需要人工审核的 Agent 系统非常重要。

例如：

- 需要人工确认是否发送邮件。
- 需要人工确认是否执行某项操作。
- 需要人工审核模型生成的内容。

### 15.8 run.output

`run.output` 用于获取最终执行结果。

同步方式：

```python
final_state = run.output
```

异步方式：

```python
final_state = await run.output
```

它与 `run.values` 不同：

- `run.values`：观察执行过程中的 State 快照。
- `run.output`：获取本次运行的最终输出。

### 15.9 原始协议事件

除了使用类型化投影，也可以直接遍历 `run`，获取底层协议事件。

```python
run = graph.stream_events(
    inputs,
    version="v3"
)

for event in run:
    print(event.method)
    print(event.params.namespace)
    print(event.params.data)
```

常见字段：

| 字段 | 说明 |
|---|---|
| `seq` | 事件序号 |
| `method` | 事件通道名称 |
| `params.namespace` | 当前事件所属的图执行层级 |
| `params.timestamp` | 事件产生时间 |
| `params.node` | 相关节点信息（适用时） |
| `params.data` | 事件数据 |

其中，`namespace` 用于区分主图和子图的执行范围。

例如：

```python
[
    "researcher:6f4d",
    "tools:91ac"
]
```

表示当前事件来自嵌套执行路径中的相应作用域。

### 15.10 Custom Transformers

新版 Event Streaming 还提供了自定义 Transformer 机制。

Transformer 可以理解为一个事件转换器：

```text
原始事件流
    |
    v
Transformer
    |
    +----> 提取消息
    |
    +----> 提取状态
    |
    +----> 统计 Token
    |
    +----> 自定义业务数据
    |
    v
新的类型化投影
```

例如，我们希望统计 Agent 执行期间的 Token 数量，就可以开发一个 Transformer，监听模型消息事件，并累计 Token 使用量。

调用图时可以传入：

```python
run = graph.stream_events(
    inputs,
    version="v3",
    transformers=[
        stats_transformer
    ]
)
```

也可以在编译图时注册：

```python
graph = builder.compile(
    stream_transformers=[
        stats_transformer
    ]
)
```

具体注册参数和 Transformer 实现方式应以当前版本 API 为准。

自定义 Transformer 可以提供新的投影，并通过 `run.extensions` 访问。

官方参考：

- [LangGraph stream_events](https://reference.langchain.com/python/langgraph/pregel/main/Pregel/stream_events)
- [LangGraph Transformers](https://reference.langchain.com/python/langgraph/stream/transformers)

### 15.11 使用注意事项

新版 `stream_events(version="v3")` 有一些需要注意的地方：

1. v3 目前仍是实验性接口，后续可能发生变化。
2. `run.messages`、`run.values` 等投影通常是单消费者流。
3. 读取某个投影会驱动图执行，不需要后台线程。
4. 如果需要多个消费者，可以使用 `tee()` / `atee()`。
5. 子图内部的数据需要通过对应的子图执行入口观察。
6. 并不是所有传统流式模型调用都能自动提供 v3 的内容块事件。
7. 如果在 v3 图中嵌套使用旧版 `stream_mode="messages"`，可能无法获得预期的 Token 级流式效果。

---

## 16. LangChain astream_events 与 LangGraph stream_events 的区别

这是整篇文档最需要明确的知识点。

两个 API 名称非常相似，但它们的使用场景和返回结构并不完全相同。

### 16.1 LangChain astream_events()

主要属于 Runnable 体系。

```python
async for event in chain.astream_events(
    inputs,
    version="v2"
):
    print(event)
```

它关注的是 Runnable 的执行事件。

例如：

- Model 开始调用。
- Model 产生 Token。
- Tool 开始执行。
- Tool 执行结束。
- Chain 执行结束。

v2 返回的是标准事件字典。

### 16.2 LangGraph stream_events()

属于 LangGraph 图执行体系。

```python
run = graph.stream_events(
    inputs,
    version="v3"
)

for message in run.messages:
    for token in message.text:
        print(token, end="")
```

它关注的是图执行过程中产生的多种数据。

例如：

- 模型消息。
- 图状态变化。
- 节点更新。
- 子图执行。
- Checkpoint。
- 人工中断。
- 最终输出。

v3 返回的是具有多个类型化投影的运行流对象。

### 16.3 两者对比

| 对比项 | LangChain `astream_events()` v2 | LangGraph `stream_events()` v3 |
|---|---|---|
| 所属体系 | LangChain Runnable | LangGraph 图执行 |
| 核心目标 | 观察 Runnable 执行事件 | 观察图执行过程 |
| 返回结果 | 事件字典 | GraphRunStream |
| 事件消费 | 统一迭代事件 | 多个类型化投影 |
| 模型输出 | `on_chat_model_stream` | `run.messages` |
| State 变化 | 需要结合具体 Runnable 处理 | `run.values` / `run.updates` |
| 子图观察 | 需要分析父子运行关系 | `run.subgraphs` |
| 最终结果 | 通过结束事件获取 | `run.output` |
| 自定义扩展 | 自定义事件 | 自定义 Transformer |

### 16.4 还需要理解 v3 的关系

这里还有一个容易忽略的细节：

LangChain 的 `Runnable.astream_events()` 本身也已经提供了 v3 版本。

官方 API 对 v3 的说明是：

- v3 是类型化、以内容块为中心的流式协议。
- 目前只支持实现了该协议的特定 Runnable，例如 `BaseChatModel` 和 LangGraph `CompiledGraph`。
- 对普通、不支持 v3 的 Runnable 调用，会出现 `NotImplementedError`。
- v3 目前属于 Beta。

因此，不能简单地认为：

`astream_events()` 永远是旧版事件字典，`stream_events()` 才是新版接口。

更准确的理解是：

- `astream_events(version="v2")`：Runnable 事件字典协议。
- `astream_events(version="v3")`：支持 v3 的 Runnable 的新协议。
- `graph.stream_events(version="v3")`：LangGraph 提供的同步图事件流接口。
- `graph.astream_events(version="v3")`：LangGraph 提供的异步图事件流接口。

对于 LangGraph 图，v1/v2 会委托给 Runnable 事件流实现；同步的 v1/v2 在当前 LangChain Core 中尚未实现，因此需要使用异步版本。v3 则返回 GraphRunStream。

官方参考：

- [Runnable.astream_events](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/astream_events)
- [Runnable.stream_events](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/stream_events)
- [LangGraph stream_events](https://reference.langchain.com/python/langgraph/pregel/main/Pregel/stream_events)

---

## 17. 实际开发中应该如何选择？

可以根据实际业务需求选择对应的 API。

| 需求 | 推荐方式 |
|---|---|
| 实时展示模型生成的文本 | `stream()` / `astream()` |
| 监听 Runnable 执行事件 | `astream_events(version="v2")` |
| 监听 Runnable 自定义事件 | `astream_events(version="v2")` |
| 观察 Agent 节点执行过程 | LangGraph `astream()` |
| 获取 Agent 状态更新 | `stream_mode="updates"` |
| 获取 Agent 完整状态 | `stream_mode="values"` |
| 获取模型消息块 | `stream_mode="messages"` |
| 同时观察多种图执行数据 | LangGraph `stream_events(version="v3")` |
| 观察子图的执行过程 | `stream_events(version="v3")` |
| 扩展事件处理逻辑 | 自定义 Transformer |

如果只是普通的 LCEL Chain，没有必要为了监听事件而引入 LangGraph。

如果是复杂 Agent，尤其是包含多个节点、子图、工具调用和状态管理的系统，就可以考虑 LangGraph 的 Streaming 机制。

---

## 18. Event Streaming 在实际项目中的应用

### 18.1 聊天对话

最常见的场景是实时展示模型生成的文本。

```text
用户输入
   |
   v
后端调用模型
   |
   v
模型逐步生成 Token
   |
   v
后端接收流式数据
   |
   v
SSE / WebSocket
   |
   v
前端逐步展示
```

如果只需要展示模型文本，使用 `astream()` 通常就够了。

如果还需要展示模型调用状态、工具调用状态等信息，则可以考虑 `astream_events()`。

### 18.2 Agent 执行过程可视化

Agent 可能会经历多个步骤：

```text
Agent 开始
   |
   v
分析用户问题
   |
   v
调用搜索工具
   |
   v
获取搜索结果
   |
   v
再次调用模型
   |
   v
生成最终回答
```

通过事件流，可以把每个步骤实时推送到前端。

例如：

- 正在分析问题。
- 正在调用搜索工具。
- 搜索完成。
- 正在生成回答。
- 任务完成。

### 18.3 RAG 执行过程展示

RAG 也可以使用 Event Streaming。

```text
用户提问
   |
   v
Embedding
   |
   v
向量检索
   |
   v
获取相关文档
   |
   v
构建 Prompt
   |
   v
调用大模型
   |
   v
生成回答
```

通过监听事件，可以展示：

- 正在进行向量检索。
- 检索到了多少文档。
- 正在构建 Prompt。
- 正在调用模型。
- 正在生成最终答案。

这样可以让用户更清楚地了解系统正在执行什么操作。

---

## 19. 常见问题与注意事项

### 19.1 使用 stream() 就一定能逐 Token 输出吗？

不一定。

是否支持真正的逐 Token 流式输出，取决于模型和 Runnable 的实现。

例如，某个自定义 Runnable 如果内部先完整调用模型，再一次性返回结果，那么它也无法真正实现逐 Token 输出。

### 19.2 Event Streaming 会不会改变模型的执行逻辑？

通常不会。

Event Streaming 的主要职责是观察和传递执行过程中产生的信息，而不是决定 Agent 应该执行什么任务。

它不会替代：

- Agent 决策。
- Tool 调用。
- State 管理。
- Checkpoint。
- Memory。

### 19.3 Event Streaming 和 Memory 有关系吗？

两者不是同一个概念。

Memory 主要用于保存和读取历史信息。

Event Streaming 主要用于实时观察程序执行过程。

例如：

- Memory 负责保存对话历史。
- Event Streaming 负责展示模型当前生成了什么内容。

### 19.4 Event Streaming 和 LangGraph State 有什么关系？

State 是 LangGraph 图执行过程中的状态数据。

Event Streaming 可以把 State 的变化暴露出来。

例如：

- State 是数据本身。
- `updates` 是节点对 State 的更新。
- `values` 是某个步骤之后的完整 State。
- `stream_events()` 则可以将这些信息组织成类型化的消费入口。

### 19.5 Event Streaming 和 LangSmith 有什么关系？

两者也不是同一个概念。

Event Streaming 主要解决程序运行时如何实时获取执行数据。

LangSmith 则提供更完整的追踪、调试、评估和监控能力。

两者可以结合使用，但并不是必须绑定在一起。

### 19.6 前端应该使用 SSE 还是 WebSocket？

如果只是后端向前端单向推送模型输出，SSE 通常已经足够。

如果前后端需要双向实时通信，例如用户可以在任务执行期间持续发送控制指令，则可以考虑 WebSocket。

一个常见架构是：

```text
前端
 |
 | HTTP 请求
 v
后端 API
 |
 v
LangChain / LangGraph
 |
 v
Event Streaming
 |
 v
后端事件转换层
 |
 v
SSE / WebSocket
 |
 v
前端实时展示
```

注意：LangChain / LangGraph 的事件流不等于 SSE 或 WebSocket。前者负责在 Python 应用内部提供事件，后者负责网络层的数据传输。

---

## 20. 核心知识点总结

### 20.1 LangChain Streaming

| 知识点 | 核心作用 |
|---|---|
| Runnable | LangChain 统一的执行抽象 |
| `stream()` | 同步流式获取输出 |
| `astream()` | 异步流式获取输出 |
| `astream_events()` | 获取 Runnable 执行事件 |
| `astream_log()` | 获取执行日志和中间状态变化 |
| `run_id` | 标识一次 Runnable 执行 |
| `parent_ids` | 表示 Runnable 的父子调用关系 |
| `tags` | 事件标签 |
| `metadata` | 附加元数据 |
| Custom Events | 开发者自定义事件 |
| Event Filtering | 根据名称、类型和标签过滤事件 |

### 20.2 LangGraph Streaming

| 知识点 | 核心作用 |
|---|---|
| `stream()` / `astream()` | 获取图执行数据流 |
| `stream_mode` | 决定获取哪种执行数据 |
| `values` | 获取完整 State |
| `updates` | 获取节点状态更新 |
| `messages` | 获取模型消息块 |
| `custom` | 获取自定义事件 |
| `checkpoints` | 获取 Checkpoint |
| `tasks` | 获取任务执行信息 |
| `debug` | 获取调试信息 |
| `stream_events()` | 新版同步图事件流接口 |
| `astream_events()` | 新版异步图事件流接口 |
| Typed Projections | 将不同事件组织成独立消费入口 |
| Transformers | 转换和扩展事件流 |

---

## 21. 学习建议

建议按照以下顺序学习：

1. 先理解 Runnable 的 `invoke()`、`ainvoke()`、`stream()`、`astream()`。
2. 理解 `astream_events(version="v2")` 的事件结构。
3. 掌握常见事件类型和事件过滤。
4. 理解自定义事件和 Runnable Config。
5. 学习 LangGraph 的 `stream_mode`。
6. 最后学习 LangGraph 新版 `stream_events(version="v3")` 和 Typed Projections。

这样学习会比较自然。

**最重要的是理解不同 API 的职责边界：**

- `stream()` / `astream()`：获取执行结果的流式片段。
- `astream_events()`：观察 Runnable 的执行事件。
- LangGraph `stream()` / `astream()`：观察图执行过程中的状态、消息和节点更新。
- LangGraph `stream_events(v3)`：通过类型化投影，更系统地消费图执行事件。

---

## 22. 官方参考资料

以下资料均为 LangChain / LangGraph 官方文档或 API Reference：

1. [LangChain Streaming](https://docs.langchain.com/oss/python/langchain/streaming)
2. [Runnable API Reference](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable)
3. [Runnable.astream_events](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/astream_events)
4. [Runnable.stream_events](https://reference.langchain.com/python/langchain-core/runnables/base/Runnable/stream_events)
5. [LangGraph Streaming](https://docs.langchain.com/oss/python/langgraph/streaming)
6. [LangGraph Event Streaming](https://docs.langchain.com/oss/python/langgraph/event-streaming)
7. [LangGraph stream_events API Reference](https://reference.langchain.com/python/langgraph/pregel/main/Pregel/stream_events)
8. [LangGraph Transformers API Reference](https://reference.langchain.com/python/langgraph/stream/transformers)

> 版本说明：LangChain 和 LangGraph 都在持续迭代，尤其是新版 v3 Event Streaming 仍处于 Beta 阶段。学习时应以实际安装版本对应的官方 API Reference 为准。