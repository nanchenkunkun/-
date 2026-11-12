# LangChain 核心组件：Event Streaming 流式事件详解



## 一、Event Streaming 是什么？

### 1.1 基本概念

Event Streaming（流式事件）是 LangChain / LangGraph 提供的一套实时输出机制。

大模型生成内容需要时间，如果必须等完整响应生成完毕再展示，用户将面临明显的等待延迟。流式系统的价值在于：

**在完整响应就绪之前，渐进式地把运行过程中的中间结果实时推送出来。**

一个 Agent 的运行过程远不止"模型输出文字"，还包括：

- 模型逐个生成的 token。
- 每一步 Agent 节点产生的状态更新。
- 工具调用的发起与结果。
- 工具内部的自定义进度。
- 人工中断与恢复。

Event Streaming 把这些过程全部暴露为可以实时消费的事件流，应用可以按需订阅其中的部分。

### 1.2 两个入口：stream() 与 astream()

LangGraph 图（`create_agent` 返回的就是编译后的图）暴露两个方法产出流式迭代器：

| 方法                | 类型 | 说明              |
| ----------------- | -- | --------------- |
| `graph.stream()`  | 同步 | 产出流式输出的迭代器      |
| `graph.astream()` | 异步 | 异步版本，用法一致       |

通过 `stream_mode` 参数指定订阅哪些类型的事件。可以传单个模式，也可以传列表同时订阅多个模式。

### 1.3 Event Streaming 的工作位置

```text
        用户输入
           |
           v
     graph.stream(...)
           |
     +-----+------------------------+
     |     |       |       |        |
     v     v       v       v        v
  token  状态    完整    自定义   检查点
  流    更新    状态    事件     事件
  (messages)(updates)(values)(custom)(checkpoints...)
     |     |       |       |        |
     +-----+-------+-------+--------+
                   |
                   v
            应用逐块消费事件
                   |
                   v
              用户实时看到
```

### 1.4 与 invoke() 的对比

呼应 Models 文档中 `invoke()` 与 `stream()` 的区别，这里是图/Agent 层面的对比：

| 对比维度 | invoke()           | stream()                  |
| ---- | ------------------ | ------------------------- |
| 返回方式 | 一次返回最终状态           | 逐块返回过程中的事件                |
| 可观察性 | 只有结果               | 每一步、每个 token、每次工具调用都能看到    |
| 适用场景 | 后台任务、不关心过程的调用      | 聊天界面、进度展示、调试              |
| 中断处理 | 结果中包含 `__interrupt__` | 流中会出现中断事件，恢复后继续流式       |

注意：流式输出不代表 Agent 执行得更快，而是把执行过程逐步暴露出来，改善等待体验和可调试性。

---

## 二、Stream Mode 全览

### 2.1 七种模式

LangGraph 支持七种流式模式，覆盖从 token 到调试的完整光谱：

| 模式            | 内容                        | 前提条件              |
| ------------- | ------------------------- | ----------------- |
| `updates`     | 每个节点执行后的状态**更新**（增量）      | 无                 |
| `values`      | 每个节点执行后的**完整状态**快照        | 无                 |
| `messages`    | LLM 逐 token 输出 + 元数据       | 节点中调用 LLM         |
| `custom`      | 节点/工具内通过 writer 发出的自定义数据  | 节点中使用 stream writer |
| `checkpoints` | 检查点事件（格式同 get_state()）    | 需要 checkpointer   |
| `tasks`       | 任务开始/结束事件，含结果和错误          | 需要 checkpointer   |
| `debug`       | checkpoints + tasks + 额外元数据 | 需要 checkpointer   |

日常应用最常组合的是 `updates` + `messages` + `custom`；后三种主要用于调试和深度可观测。

### 2.2 v2 统一输出格式

**LangGraph 1.1+** 引入了 v2 统一格式，通过 `version="v2"` 启用。启用后，每个块都是统一结构的 `StreamPart` 字典：

```python
{
    "type": "values" | "updates" | "messages" | "custom" | ...,
    "ns": (),        # 命名空间元组，子图事件时填充，根图为 ()
    "data": ...,     # 实际负载，结构随模式变化
}
```

v1 与 v2 的输出格式差异：

| 场景          | v1（默认）                | v2                              |
| ----------- | ---------------------- | ------------------------------- |
| 单一模式        | 直接返回原始数据               | StreamPart 字典（type/ns/data）     |
| 多模式         | `(mode, data)` 元组      | 统一字典，按 `chunk["type"]` 区分      |
| 子图流         | `(namespace, data)` 元组 | 统一字典，检查 `chunk["ns"]`           |
| 多模式 + 子图    | `(namespace, mode, data)` 三元组 | 统一字典                    |
| invoke() 返回 | 普通状态字典                 | `GraphOutput`（`.value` / `.interrupts`） |

**建议新项目一律使用 `version="v2"`**：输出结构统一、类型可收窄（按 `type` 过滤后 `data` 的类型随之确定），代码不再需要对元组长度做分支判断。

本文后续示例均使用 v2 格式。

### 2.3 多模式同时订阅

`stream_mode` 传列表即可同时订阅多种事件：

```python
for chunk in graph.stream(
    inputs,
    stream_mode=["updates", "custom"],
    version="v2",
):
    if chunk["type"] == "updates":
        print(f"状态更新：{chunk['data']}")
    elif chunk["type"] == "custom":
        print(f"自定义事件：{chunk['data']}")
```

---

## 三、updates 模式：Agent 进度

### 3.1 基本概念

`updates` 模式在每个 Agent 步骤执行完后，流式输出该步骤产生的状态**增量更新**。

一个调用了一次工具的 Agent，典型的事件序列是：

```text
LLM 节点：AIMessage（带 tool_calls 请求）
   |
   v
工具节点：ToolMessage（执行结果）
   |
   v
LLM 节点：AIMessage（最终回答）
```

每一步都会产生一个独立的事件块。

### 3.2 完整示例

```python
from langchain.agents import create_agent
from langchain_core.utils.uuid import uuid7
from langgraph.checkpoint.memory import InMemorySaver


def get_weather(city: str) -> str:
    """Get weather for a given city."""
    return f"It's always sunny in {city}!"


agent = create_agent(
    model="openai:gpt-5.5",
    tools=[get_weather],
    checkpointer=InMemorySaver(),
)

config = {"configurable": {"thread_id": str(uuid7())}}

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    config=config,
    stream_mode="updates",
    version="v2",
):
    if chunk["type"] == "updates":
        for source, update in chunk["data"].items():
            print(f"节点 {source} 更新：{list(update.keys())}")
```

预期输出：

```text
节点 model 更新：['messages']
节点 tools 更新：['messages']
节点 model 更新：['messages']
```

注意：**生产环境务必传入 `thread_id` 并配置 checkpointer**，否则无法建立对话检查点，后续轮次无法恢复历史（LangSmith 部署会自动配置 checkpointer）。

---

## 四、values 模式：完整状态快照

`values` 模式与 `updates` 的区别只有一个词：**全量 vs 增量**。

- `updates`：这一步**改了什么**。
- `values`：这一步结束后，**完整状态长什么样**。

```python
for chunk in graph.stream(
    {"topic": "ice cream"},
    stream_mode="values",
    version="v2",
):
    if chunk["type"] == "values":
        print(f"当前状态：{chunk['data']}")
```

预期输出（每步一份全量快照，字段逐步填充）：

```text
当前状态：{'topic': 'ice cream', 'joke': ''}
当前状态：{'topic': 'ice cream and cats', 'joke': ''}
当前状态：{'topic': 'ice cream and cats', 'joke': 'This is a joke about ice cream and cats'}
```

适用场景：前端需要在每一步后刷新完整 UI 状态（如整个对话列表）；调试时确认状态的完整演变。

代价：每一步都输出全量状态，状态较大时事件体积也大。多数实时 UI 场景优先选 `updates` + `messages`。

---

## 五、messages 模式：LLM Token 流

### 5.1 基本概念

`messages` 模式从图中**任何调用 LLM 的位置**（节点、工具、子图、任务）逐 token 流式输出结果。每个事件的数据是二元组：

```python
(message_chunk, metadata)
```

- `message_chunk`：LLM 产出的 token 或消息片段（AIMessageChunk 等）。
- `metadata`：包含来源节点（`langgraph_node`）、标签（`tags`）等信息的字典。

```python
from langchain.agents import create_agent


def get_weather(city: str) -> str:
    """Get weather for a given city."""
    return f"It's always sunny in {city}!"


agent = create_agent(model="openai:gpt-5.5", tools=[get_weather])

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    stream_mode="messages",
    version="v2",
):
    if chunk["type"] == "messages":
        token, metadata = chunk["data"]
        if token.content:
            print(token.content, end="", flush=True)
```

这就是聊天界面"打字机效果"的数据来源。

注意：即使节点中用 `model.invoke()` 而非 `model.stream()` 调用 LLM，messages 事件依然会发出——流的生成由图运行时统一处理。

### 5.2 工具调用块

模型生成工具调用时，参数是逐块流出的，`content_blocks` 中会出现 `tool_call_chunk` 类型：

```text
node: model  →  {'type': 'tool_call_chunk', 'name': 'get_weather', 'args': ''}
node: model  →  {'type': 'tool_call_chunk', 'args': 'city'}
node: model  →  {'type': 'tool_call_chunk', 'args': '": "San Francisco'}
node: tools  →  {'type': 'text', 'text': "It's always sunny in San Francisco!"}
node: model  →  {'type': 'text', 'text': 'The'}
node: model  →  {'type': 'text', 'text': ' weather'}
...
```

前端可以据此展示"正在调用 get_weather…"之类的进度提示。

### 5.3 过滤特定来源的 Token

**按标签过滤**：给不同模型打 tag，只消费特定调用的 token：

```python
model_1 = init_chat_model("openai:gpt-5.4-mini", tags=["joke"])
model_2 = init_chat_model("openai:gpt-5.4-mini", tags=["poem"])

async for chunk in graph.astream(
    {"topic": "cats"},
    stream_mode="messages",
    version="v2",
):
    if chunk["type"] == "messages":
        msg, metadata = chunk["data"]
        if metadata["tags"] == ["joke"]:
            print(msg.content, end="", flush=True)
```

**按节点过滤**：

```python
if msg.content and metadata["langgraph_node"] == "write_poem":
    print(msg.content, end="", flush=True)
```

**排除内部调用**：某些 LLM 调用（如内部结构化输出）不应流给前端，用 `nostream` 标签抑制：

```python
internal_model = ChatAnthropic(model_name="claude-sonnet-4-6").with_config(
    {"tags": ["nostream"]}
)
```

被标记的调用正常执行并产出结果，只是 token 不出现在 messages 流中。

### 5.4 流式推理 Token（Reasoning）

部分模型在给出最终答案前会进行内部推理（如 Claude 的 thinking、OpenAI 的 reasoning）。LangChain 把各供应商的格式统一规范化为标准 `"reasoning"` 内容块：

```python
from langchain_anthropic import ChatAnthropic

model = ChatAnthropic(
    model_name="claude-sonnet-4-6",
    thinking={"type": "enabled", "budget_tokens": 5000},
)
agent = create_agent(model=model, tools=[get_weather])

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    stream_mode="messages",
    version="v2",
):
    if chunk["type"] == "messages":
        token, metadata = chunk["data"]
        for block in token.content_blocks:
            if block["type"] == "reasoning":
                print(f"[thinking] {block['reasoning']}", end="")
            elif block["type"] == "text":
                print(block["text"], end="")
```

注意：推理输出需要先在模型上启用（如 Claude 的 `thinking` 配置）。

### 5.5 Chunk 聚合

如果既要逐 token 展示，又要在流结束后拿到完整消息，使用 `+` 合并（与 Models 文档 8.6 节一致）：

```python
full_message = None

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in Boston?"}]},
    stream_mode="messages",
    version="v2",
):
    if chunk["type"] == "messages":
        token, metadata = chunk["data"]
        if isinstance(token, AIMessageChunk):
            full_message = token if full_message is None else full_message + token
            if token.chunk_position == "last":
                if full_message.tool_calls:
                    print(f"工具调用：{full_message.tool_calls}")
                full_message = None
```

`chunk_position == "last"` 标记当前消息的最后一个块，是"本条消息流完了"的信号。

---

## 六、custom 模式：自定义事件

### 6.1 基本概念

内置模式覆盖不了业务特有的进度时（例如"正在查询 100 条记录中的第 42 条"），节点或工具内部可以用 `get_stream_writer()` 主动发出任意自定义数据。

```python
from langchain.agents import create_agent
from langgraph.config import get_stream_writer


def get_weather(city: str) -> str:
    """Get weather for a given city."""
    writer = get_stream_writer()
    writer(f"Looking up data for city: {city}")
    writer(f"Acquired data for city: {city}")
    return f"It's always sunny in {city}!"


agent = create_agent(model="openai:gpt-5.5", tools=[get_weather])

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    stream_mode="custom",
    version="v2",
):
    if chunk["type"] == "custom":
        print(chunk["data"])
```

预期输出：

```text
Looking up data for city: San Francisco
Acquired data for city: San Francisco
```

### 6.2 结构化进度事件

自定义数据可以是任意可序列化对象，结构化 dict 更利于前端消费：

```python
@tool
def query_database(query: str) -> str:
    """查询数据库。"""
    writer = get_stream_writer()
    writer({"data": "Retrieved 0/100 records", "type": "progress"})
    # ...执行查询...
    writer({"data": "Retrieved 100/100 records", "type": "progress"})
    return "some-answer"
```

### 6.3 两条重要规则

- **订阅要求**：消费 custom 事件时，`stream_mode` 至少有一项必须是 `"custom"`，可以与其它模式组合（如 `["updates", "custom"]`）。
- **调用限制**：使用了 `get_stream_writer` 的工具或节点，**无法在 LangGraph 执行上下文之外被直接调用**——写进度需要流上下文支撑。这类工具的独立单测需要调整（mock 或不走 writer 路径）。

### 6.4 转发任意 LLM 集成

`custom` 模式还有一个妙用：流式转发不实现 LangChain 接口的 LLM API。例如在节点内直接使用原生 OpenAI 客户端，把每个 chunk 用 `writer(msg_chunk)` 转发出来，从而把第三方模型纳入统一的事件流。

---

## 七、checkpoints / tasks / debug 模式

这三种模式面向调试与深度可观测，前提都是**配置了 checkpointer**。

| 模式            | 内容                                | 典型用途            |
| ------------- | --------------------------------- | --------------- |
| `checkpoints` | 每一步的检查点快照（格式同 `get_state()`）      | 状态回放、时间旅行调试     |
| `tasks`       | 任务的开始与结束事件，含节点名、结果和错误            | 排查某节点失败原因       |
| `debug`       | checkpoints + tasks + 额外元数据（信息最全） | 全面诊断             |

```python
graph = builder.compile(checkpointer=MemorySaver())
config = {"configurable": {"thread_id": "1"}}

for chunk in graph.stream(
    {"topic": "ice cream"},
    config=config,
    stream_mode="checkpoints",
    version="v2",
):
    if chunk["type"] == "checkpoints":
        print(chunk["data"])
```

实践建议：日常只需要 `tasks` 排错时就订阅 `tasks`，不要默认开 `debug`——信息量大，生产环境的日志里会刷屏。

---

## 八、高级场景

### 8.1 流式工具调用

一个常见的复杂需求：**既要在工具调用生成时逐块展示部分 JSON，又要在工具执行完成后拿到解析好的完整调用和结果。**

这需要组合 `["messages", "updates"]`：

```python
from langchain.agents import create_agent
from langchain.messages import AIMessage, AIMessageChunk, AnyMessage, ToolMessage


agent = create_agent("openai:gpt-5.5", tools=[get_weather])


def _render_message_chunk(token: AIMessageChunk) -> None:
    """渲染逐块的流式内容。"""
    if token.text:
        print(token.text, end="|")
    if token.tool_call_chunks:
        print(token.tool_call_chunks)


def _render_completed_message(message: AnyMessage) -> None:
    """渲染已完成的消息。"""
    if isinstance(message, AIMessage) and message.tool_calls:
        print(f"Tool calls: {message.tool_calls}")
    if isinstance(message, ToolMessage):
        print(f"Tool response: {message.content_blocks}")


for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in Boston?"}]},
    stream_mode=["messages", "updates"],
    version="v2",
):
    if chunk["type"] == "messages":
        token, metadata = chunk["data"]
        if isinstance(token, AIMessageChunk):
            _render_message_chunk(token)          # 逐块展示
    elif chunk["type"] == "updates":
        for source, update in chunk["data"].items():
            if source in ("model", "tools"):
                _render_completed_message(update["messages"][-1])  # 完整消息
```

要点：消息**生成过程**在 `messages` 流里（AIMessageChunk），消息**完成后的成品**在 `updates` 流里（完整 AIMessage / ToolMessage）。

### 8.2 Human-in-the-loop 中断流

配置了 `HumanInTheLoopMiddleware` 后，审批中断也会以事件形式出现在流里。处理流程分两段：先收集中断，再恢复执行。

**第一段：流式执行并收集中断**

```python
from langchain.agents import create_agent
from langchain.agents.middleware import HumanInTheLoopMiddleware
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import Command

agent = create_agent(
    "openai:gpt-5.5",
    tools=[get_weather],
    middleware=[HumanInTheLoopMiddleware(interrupt_on={"get_weather": True})],
    checkpointer=InMemorySaver(),
)

config = {"configurable": {"thread_id": "some_id"}}
interrupts = []

for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    config=config,
    stream_mode=["messages", "updates"],
    version="v2",
):
    if chunk["type"] == "messages":
        token, metadata = chunk["data"]
        if isinstance(token, AIMessageChunk):
            _render_message_chunk(token)
    elif chunk["type"] == "updates":
        for source, update in chunk["data"].items():
            if source in ("model", "tools"):
                _render_completed_message(update["messages"][-1])
            if source == "__interrupt__":       # 中断事件
                interrupts.extend(update)
```

**第二段：给出审批决策并恢复**

```python
decisions = {}
for interrupt in interrupts:
    decisions[interrupt.id] = {"decisions": _get_interrupt_decisions(interrupt)}

for chunk in agent.stream(
    Command(resume=decisions),                # 用 Command 恢复执行
    config=config,
    stream_mode=["messages", "updates"],
    version="v2",
):
    ...  # 消费循环与第一段完全一致
```

注意：**多个中断的决策顺序必须与动作顺序一致**。恢复后的流式循环代码与首次完全相同，可以封装成共用函数。

### 8.3 子图与子 Agent

多 Agent 系统中，需要区分 token 来自哪个 Agent。两个关键配置：

1. 创建 Agent 时传 `name`，该名称会出现在元数据的 `lc_agent_name` 键中，也会附加到它生成的所有 AIMessage 上。
2. 流式消费时传 `subgraphs=True`，否则父图拿不到子 Agent 内部的事件。

```python
weather_agent = create_agent(
    model=weather_model, tools=[get_weather], name="weather_agent",
)


def call_weather_agent(query: str) -> str:
    """查询天气。"""
    result = weather_agent.invoke({"messages": [{"role": "user", "content": query}]})
    return result["messages"][-1].text


agent = create_agent(model=supervisor_model, tools=[call_weather_agent], name="supervisor")

current_agent = None
for chunk in agent.stream(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    stream_mode=["messages", "updates"],
    subgraphs=True,
    version="v2",
):
    if chunk["type"] == "messages":
        token, metadata = chunk["data"]
        if agent_name := metadata.get("lc_agent_name"):
            if agent_name != current_agent:
                print(f"[{agent_name}]: ")
                current_agent = agent_name
        if isinstance(token, AIMessageChunk):
            _render_message_chunk(token)
```

补充说明：`chunk["ns"]` 是命名空间元组，根图为 `()`，子图事件会带上子图的命名空间（如 `("agent:<task_id>",)`），可以用它区分事件归属的图层级。

### 8.4 禁用流式

有些模型不支持流式，或者某些内部模型调用不应流给客户端。禁用方式：

```python
from langchain_openai import ChatOpenAI

model = ChatOpenAI(model="gpt-5.5", streaming=False)
```

如果模型集成不支持 `streaming` 参数，使用基类提供的：

```python
model = SomeChatModel(model="...", disable_streaming=True)
```

### 8.5 异步注意事项

**Python < 3.11** 的 asyncio 任务不支持 context 参数，带来两个限制：

1. 异步节点中调用 LLM 时，必须**显式传入 config**，否则消息流会断：

```python
async def call_model(state, config):
    response = await model.ainvoke(
        [{"role": "user", "content": f"Write a joke about {state['topic']}"}],
        config,   # 必须显式传
    )
    return {"joke": response.content}
```

2. 异步节点/工具中**不能使用 `get_stream_writer()`**，改为在函数签名中声明 `writer` 参数，由 LangGraph 自动注入：

```python
from langgraph.types import StreamWriter


async def generate_joke(state, writer: StreamWriter):
    writer({"custom_key": "Streaming custom data..."})
    return {"joke": f"This is a joke about {state['topic']}"}
```

实践建议：升级到 Python 3.11+ 可以避免这两类限制。

---

## 九、Event Streaming API（stream_events）与 v2 invoke

### 9.1 stream_events：类型化投影 API

LangChain 新版本引入 `stream_events()`，为不同投影（messages、tool calls、subgraphs）提供独立迭代器，不需要再对事件类型做大量分支判断：

```python
stream = agent.stream_events(
    {"messages": [{"role": "user", "content": "What is the weather in SF?"}]},
    config=config,
    version="v3",
)

for kind, item in stream.interleave("messages", "tool_calls"):
    if kind == "messages":
        for token in item.text:
            print(token, end="", flush=True)
    elif kind == "tool_calls":
        print(f"\nTool call: {item.tool_name}({item.input})")
        print(f"\nTool result: {item.output}")

final_state = stream.output
```

相比 stream() + 分支判断，它的优势：

| 对比维度 | stream()            | stream_events()        |
| ---- | ------------------- | ---------------------- |
| 事件组织 | 扁平块，需手动判断 type      | 按投影分组，独立迭代器            |
| 消费方式 | for 循环 + if 分支      | interleave 交错消费多类事件    |
| 最终状态 | 需要手动收集 updates      | `stream.output` 直接获取   |
| 适用   | 底层控制、自定义处理          | 新应用首选，代码更简洁            |

### 9.2 invoke() 的 v2 输出

v2 格式下，`invoke()` 返回 `GraphOutput` 对象：

```python
result = agent.invoke(
    {"messages": [{"role": "user", "content": "Hello"}]},
    version="v2",
)

print(result.value)       # 最终状态
print(result.interrupts)  # 中断对象元组，无中断则为空
```

注意：字典式访问（`result["key"]`、`result["__interrupt__"]`）在 v2 中仍兼容，但已标记为弃用。

---

## 十、实战建议

### 10.1 场景与模式选型

| 前端需求              | 推荐订阅                          |
| ----------------- | ----------------------------- |
| 聊天打字机效果           | `messages`                    |
| 显示"正在调用 xxx 工具"   | `messages`（tool_call_chunk）或 `updates` |
| 显示工具执行进度条         | `custom`                      |
| 每步刷新完整对话 UI       | `values`                      |
| 审批弹窗（HITL）        | `["messages", "updates"]` + 收集 `__interrupt__` |
| 区分多 Agent 输出来源    | 任意模式 + `subgraphs=True` + Agent 命名 |
| 线上问题排查            | `tasks` / `debug`             |

### 10.2 常见坑

**1. 子 Agent 的 token 流不出来**

检查是否传了 `subgraphs=True`。不设它，父图的 messages 模式不会包含子图内部 LLM 调用的 token。

**2. custom 事件收不到**

检查 `stream_mode` 中是否包含 `"custom"`；writer 调用只会在订阅了 custom 模式的流中生效。

**3. 用了 writer 的工具无法单测**

工具内调用 `get_stream_writer` 后，就依赖图执行上下文。单元测试要么通过 `agent.stream()` 驱动，要么把 writer 逻辑隔离成可替换的依赖。

**4. v1 / v2 格式混淆**

同一份代码不要在 v1 的元组解包和 v2 的字典访问之间混用。新项目统一 `version="v2"`。

**5. 异步图里 token 流断了**

Python < 3.11 时检查两点：ainvoke 是否显式传了 config；自定义数据是否误用了 `get_stream_writer`（应改为 `writer` 参数注入）。

**6. 中断恢复后决策错乱**

多个中断的 decisions 顺序必须与动作顺序一致；且恢复时要用同一个 `thread_id` 的 config。

---

## 十一、小结

| 主题       | 内容                                             |
| -------- | ---------------------------------------------- |
| 核心 API   | `stream()` / `astream()` / `stream_events()`   |
| 七种模式     | updates（增量）、values（全量）、messages（token）、custom（自定义）、checkpoints、tasks、debug |
| 统一格式     | v2 StreamPart（type/ns/data），新项目首选              |
| 高级场景     | 流式工具调用、HITL 中断恢复、子图/子 Agent、禁用流式、异步注意事项       |
| 新 API    | `stream_events` 类型化投影（interleave / .output）    |
| 工程要点     | thread_id + checkpointer 必备、子图要 subgraphs=True、用 writer 的工具不可脱离图上下文 |
