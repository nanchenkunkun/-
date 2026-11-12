# LangChain 核心组件：Short-term Memory 短期记忆详解

## 一、Short-term Memory 是什么？

### 1.1 基本概念

Memory（记忆）是一个记住之前交互信息的系统。

对 AI Agent 而言，记忆让它能够：

- 记住之前的交互内容。
- 从用户反馈中学习。
- 适应用户的偏好和习惯。

这里要先澄清一个常见误解（也是 Models 模块文档中强调过的）：

**聊天模型本身没有内置的长期记忆。** 每次调用模型，应用都需要把相关历史重新组织并传递给模型。所谓"记忆"，全部是应用层自己管理、自己回传的。

Short-term Memory（短期记忆）是记忆的一种形式，官方定义是：

**让应用在单个 Thread（线程/会话）内记住之前的交互。**

例如同一个聊天窗口里，用户先说"我叫 Bob"，几轮之后问"我叫什么"，Agent 能回答"You are Bob"，这背后就是短期记忆在工作。

### 1.2 Thread：会话的组织单位

Thread（线程）负责把一个会话中的多次交互组织在一起，可以类比为邮件把多封消息归组到同一个对话中。

- 每个 Thread 通过 `thread_id` 唯一标识。
- 同一个 `thread_id` 的调用共享同一份会话状态。
- 不同 Thread 之间的状态相互隔离，互不可见。

```text
thread_id = "1"
┌─────────────────────────────┐
│ 用户：我叫 Bob               │
│ Agent：你好 Bob！            │
│ 用户：今天天气如何？          │
│ Agent：……                   │
└─────────────────────────────┘

thread_id = "2"（另一个用户/另一个窗口，状态完全隔离）
┌─────────────────────────────┐
│ 用户：帮我订机票             │
│ Agent：……                   │
└─────────────────────────────┘
```

### 1.3 短期记忆与长期记忆的边界

| 对比维度 | Short-term Memory     | Long-term Memory        |
| ---- | --------------------- | ----------------------- |
| 作用范围 | 单个 Thread（会话）内      | 跨 Thread、跨会话            |
| 典型形式 | 对话消息历史（messages）      | 用户画像、偏好、事实知识            |
| 存储载体 | Agent State + Checkpointer | Store（namespace + key）  |
| 生命周期 | 会话结束后按持久化策略保留或丢弃 | 长期保留，随时召回               |
| 典型问题 | "记住我刚才说的话"            | "记住用户的所有偏好"             |

本文聚焦短期记忆，长期记忆在第九节做简要对比。

### 1.4 短期记忆的工作位置

LangChain Agent 把短期记忆作为 **Agent State（图状态）** 的一部分来管理：

```text
            调用 Agent
                |
                v
   读取该 thread_id 的状态（含消息历史）
                |
                v
          Models（大脑）
                |
                v
           工具调用循环
                |
                v
   状态更新（新消息、工具结果写入）
                |
                v
     Checkpointer 持久化状态
                |
                v
        下一次调用可随时恢复
```

关键规则：状态在 Agent 被调用时或每一步（例如一次工具调用）完成后更新，并在每一步开始时读取。正是因为每一步都"先读后写"，多轮对话和多次工具调用才能连贯起来。

---

## 二、核心机制：Agent State 与 Checkpointer

### 2.1 状态即记忆

默认情况下，Agent 的短期记忆由默认的 `AgentState` 管理，对话历史存储在状态中的 `messages` 字段里。

`messages` 字段使用了 `add_messages` 这个 reducer（归约函数），它决定了消息如何合并进状态：

- 新消息追加到历史末尾，而不是覆盖。
- 更新已有 ID 的消息时，替换原消息。
- 支持 `RemoveMessage` 删除消息（见第六节）。

理解了"记忆 = 图状态中的 messages"，短期记忆的所有操作（读写、裁剪、删除、摘要）就都好理解了。

### 2.2 添加短期记忆：checkpointer 参数

给 Agent 添加短期记忆非常简单，只需要在创建时指定 `checkpointer`（检查点保存器）：

```python
from langchain.agents import create_agent
from langgraph.checkpoint.memory import InMemorySaver

agent = create_agent(
    model="openai:gpt-5.5",
    tools=[],
    checkpointer=InMemorySaver(),
)
```

`InMemorySaver` 把状态保存在进程内存中，适合开发调试。

### 2.3 thread_id：指定会话

调用 Agent 时，通过 `thread_id` 指定本次交互属于哪个会话：

```python
thread_config = {"configurable": {"thread_id": "1"}}

# 第一轮：告诉 Agent 名字
response = agent.invoke(
    {"messages": [{"role": "user", "content": "你好，我叫 Bob"}]},
    thread_config,
)
print(response["messages"][-1].content)

# 第二轮：同一 thread_id，Agent 记得上下文
response = agent.invoke(
    {"messages": [{"role": "user", "content": "我叫什么名字？"}]},
    thread_config,
)
print(response["messages"][-1].content)
# 输出类似："你叫 Bob，你之前告诉过我。"
```

如果第二轮换了新的 `thread_id`，Agent 就完全不知道 Bob 是谁——因为那是另一个会话的状态。

### 2.4 生产环境的持久化

`InMemorySaver` 重启即失效，生产环境应使用数据库支持的 checkpointer。

以 PostgreSQL 为例：

```bash
pip install -U langgraph-checkpoint-postgres "psycopg[binary]"
```

```python
from langchain.agents import create_agent
from langgraph.checkpoint.postgres import PostgresSaver

DB_URI = "postgresql://postgres:postgres@localhost:5432/db?sslmode=disable"

with PostgresSaver.from_conn_string(DB_URI) as checkpointer:
    checkpointer.setup()   # 首次使用时自动建表
    agent = create_agent(
        "openai:gpt-5.5",
        tools=[],
        checkpointer=checkpointer,
    )
```

官方生态还提供 SQLite、Azure Cosmos DB 等多种 checkpointer 实现，可以按现有技术栈选择。

| Checkpointer    | 适用场景          |
| --------------- | ------------- |
| InMemorySaver   | 开发调试、单元测试、示例  |
| SqliteSaver     | 单机轻量应用        |
| PostgresSaver   | 生产环境主流选择      |
| 其他（Cosmos 等）    | 按云环境选择        |

### 2.5 持久化的意义

有了 checkpointer，短期记忆不只是"本次进程内的变量"：

- 应用重启后，会话可以随时恢复。
- 用户关闭页面后再回来，历史对话还在。
- Human-in-the-loop 等中断场景（如工具审批）依赖状态暂停与恢复。

---

## 三、自定义记忆状态

默认 `AgentState` 只有一个 `messages` 字段。如果想在会话中记住更多业务数据（如 user_id、偏好设置），可以继承 `AgentState` 扩展状态结构：

```python
from langchain.agents import create_agent, AgentState


class CustomAgentState(AgentState):
    user_id: str
    preferences: dict


agent = create_agent(
    "openai:gpt-5.5",
    tools=[],
    state_schema=CustomAgentState,
    checkpointer=InMemorySaver(),
)

result = agent.invoke(
    {
        "messages": [{"role": "user", "content": "Hello"}],
        "user_id": "user_123",
        "preferences": {"theme": "dark"}
    },
    {"configurable": {"thread_id": "1"}}
)
```

扩展后的自定义字段同样由 checkpointer 按 thread 持久化，随消息历史一起恢复。

注意：自定义状态字段不会自动传给模型。模型只能看到消息历史；要让它感知状态数据，需要通过工具读取或动态提示注入（见第八节）。

---

## 四、为什么需要管理消息历史

消息历史是最常见的短期记忆形式，但它天然会不断增长，带来两个问题：

**1. 超出上下文窗口**

完整历史可能超过模型的上下文窗口上限，导致调用失败或上下文被截断丢失。

**2. 长上下文性能劣化**

官方文档特别指出：即使模型支持很长的上下文，大多数 LLM 在长上下文中表现也不佳——

- 会被过时或无关的内容"分散注意力"，回答质量下降。
- 响应更慢。
- 成本更高（Token 按输入计费）。

因此，很多应用需要主动"删除或遗忘"过时信息。官方给出的三大策略：

| 策略 | 做法           | 优点         | 代价           |
| ---- | ------------ | ---------- | ------------ |
| 裁剪 | 只保留最近的消息     | 实现简单、零额外成本 | 丢失早期信息       |
| 删除 | 按规则删除指定消息    | 控制精确       | 同样丢失信息       |
| 摘要 | 用模型把历史总结成摘要  | 保留关键信息     | 多一次模型调用、有损压缩 |

---

## 五、裁剪消息

### 5.1 基本思路

裁剪（Trimming）就是当消息接近上下文上限时，只保留最近的消息。常见做法是**保留第一条消息（往往承载关键设定）+ 最近 N 条消息**。

### 5.2 用 @before_model 中间件实现

在 LangChain Agent 中，通过 `@before_model` 中间件在每次模型调用前处理消息：

```python
from typing import Any
from langchain.agents.middleware import before_model
from langchain.agents.middleware.types import AgentState
from langchain.messages import RemoveMessage
from langgraph.graph.message import REMOVE_ALL_MESSAGES


@before_model
def trim_messages(state: AgentState, runtime) -> dict[str, Any] | None:
    """只保留最近几条消息以适应上下文窗口。"""
    messages = state["messages"]
    if len(messages) <= 3:
        return None  # 无需更改

    first_msg = messages[0]
    recent_messages = messages[-3:] if len(messages) % 2 == 0 else messages[-4:]
    new_messages = [first_msg] + recent_messages

    return {"messages": [
        RemoveMessage(id=REMOVE_ALL_MESSAGES),  # 先清除全部
        *new_messages,                          # 再写回保留的消息
    ]}


agent = create_agent(
    "openai:gpt-5.5",
    tools=[],
    middleware=[trim_messages],
    checkpointer=InMemorySaver(),
)
```

### 5.3 操作细节

- `RemoveMessage(id=REMOVE_ALL_MESSAGES)` 表示删除全部消息，然后再把保留的消息写回，这是一种"整体重写"的固定写法。
- 保留成对的消息（human/ai 交替）有助于保持历史结构合法，这也是示例中按奇偶取不同条数的原因。
- LangChain 也提供了独立的 `trim_messages` 工具函数，可在不使用 Agent 的场景中直接裁剪消息列表。

---

## 六、删除消息

### 6.1 RemoveMessage：按 ID 删除

`add_messages` reducer 支持通过 `RemoveMessage` 删除消息。注意前提：消息字段必须使用 `add_messages` reducer（默认 `AgentState` 已经内置）。

删除最早的两条消息：

```python
from langchain.messages import RemoveMessage


def delete_messages(state):
    messages = state["messages"]
    if len(messages) > 2:
        return {"messages": [RemoveMessage(id=m.id) for m in messages[:2]]}
```

删除全部消息：

```python
from langgraph.graph.message import REMOVE_ALL_MESSAGES


def delete_messages(state):
    return {"messages": [RemoveMessage(id=REMOVE_ALL_MESSAGES)]}
```

### 6.2 用 @after_model 在模型响应后清理

删除操作也可以放在 `@after_model` 中间件里，在模型响应之后执行，例如删除旧消息、或过滤包含敏感信息的响应：

```python
from typing import Any
from langchain.agents.middleware import after_model
from langchain.agents.middleware.types import AgentState
from langchain.messages import RemoveMessage


@after_model
def delete_old_messages(state: AgentState, runtime) -> dict[str, Any] | None:
    """模型响应后删除最早的两条消息。"""
    messages = state["messages"]
    if len(messages) > 2:
        return {"messages": [RemoveMessage(id=m.id) for m in messages[:2]]}
    return None
```

官方示例展示了删除的副作用：最早的用户消息（"我叫 Bob"）被删除后，再问"我叫什么"，Agent 会回答"I don't know your name"——**信息确实被永久删掉了，这是刻意的遗忘。**

### 6.3 删除时的有效性约束

删除消息后，历史必须仍然满足模型供应商的格式要求，否则下次调用会报错。两个最常见的约束：

1. 消息历史必须以 user 消息开头（部分供应商强制要求）。
2. 带 tool_calls 的 AI 消息之后，必须紧跟对应的 ToolMessage（工具结果）。如果只删了 AI 消息而留下了 ToolMessage，或者反之，历史就不合法了。

因此批量删除时，建议按"消息对"为单位删除，并在删除后检查历史结构。

---

## 七、摘要消息

### 7.1 基本思路

裁剪和删除的共同缺点是**信息丢失**。摘要（Summarization）的思路是：当历史过长时，先用一个模型把旧消息总结成一段摘要，用摘要替换原始消息，从而保留关键信息。

### 7.2 SummarizationMiddleware：内置中间件

官方提供了现成的 `SummarizationMiddleware`，一行配置即可启用：

```python
from langchain.agents import create_agent
from langchain.agents.middleware import SummarizationMiddleware

agent = create_agent(
    model="openai:gpt-5.5",
    tools=[],
    middleware=[
        SummarizationMiddleware(
            model="openai:gpt-5.4-mini",   # 用更便宜的模型生成摘要
            trigger=("tokens", 4000),      # 触发条件：历史达到 4000 tokens
            keep=("messages", 20),         # 摘要后保留最近 20 条原始消息
        )
    ],
    checkpointer=checkpointer,
)
```

三个关键参数：

| 参数       | 含义                          |
| -------- | --------------------------- |
| model    | 执行摘要的模型，通常选更小更便宜的模型         |
| trigger  | 触发阈值，达到后开始摘要（如 4000 tokens） |
| keep     | 摘要之外，保留最近多少条原始消息            |

### 7.3 三种策略怎么选

| 场景                 | 建议策略                |
| ------------------ | ------------------- |
| 短会话、消息量小           | 不需要处理，或简单裁剪兜底       |
| 普通聊天机器人            | 裁剪 + 保留首条消息         |
| 长会话、早期信息很重要（如需求描述） | 摘要（SummarizationMiddleware） |
| 有合规要求，必须删除特定内容     | 删除（@after_model）    |
| 成本敏感                | 摘要用小模型，或纯裁剪         |

实践中也常组合使用：日常裁剪控制增长，超过阈值后触发一次摘要。

---

## 八、访问与修改记忆

短期记忆存储在 Agent State 中，工具、中间件和动态提示都可以访问它。本节每个示例都是完整可运行的。

### 8.1 在工具中读取状态

工具通过 `runtime: ToolRuntime` 参数访问当前会话状态，该参数对模型隐藏。完整示例如下：

```python
from langchain.agents import create_agent, AgentState
from langchain.tools import tool, ToolRuntime
from langgraph.checkpoint.memory import InMemorySaver


# 1. 扩展状态：加入 user_id 字段
class CustomAgentState(AgentState):
    user_id: str


# 2. 工具通过 runtime 读取状态，user_id 不会暴露给模型
@tool
def get_user_info(runtime: ToolRuntime) -> str:
    """查询当前用户信息。"""
    user_id = runtime.state["user_id"]
    return "User is John Smith" if user_id == "user_123" else "Unknown user"


# 3. 创建 Agent：传入自定义状态结构 + checkpointer
agent = create_agent(
    "openai:gpt-5.5",
    tools=[get_user_info],
    state_schema=CustomAgentState,
    checkpointer=InMemorySaver(),
)

# 4. 调用：自定义状态字段随 invoke 一起传入
result = agent.invoke(
    {
        "messages": [{"role": "user", "content": "请查询当前用户信息"}],
        "user_id": "user_123",
    },
    {"configurable": {"thread_id": "1"}},
)

print(result["messages"][-1].content)
# 预期输出：User is John Smith
```

执行要点：

- 模型只看到"一个不需要参数的查询工具"，user_id 完全来自会话状态。
- 自定义状态字段（user_id）通过 invoke 的字典传入，并随 thread 持久化。

### 8.2 在工具中写入状态

工具可以直接返回 `Command` 更新状态，把中间结果持久化，供后续步骤使用。完整示例如下：

```python
from langchain.agents import create_agent, AgentState
from langchain.messages import ToolMessage
from langchain.tools import ToolRuntime, tool
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import Command


# 1. 扩展状态：加入 user_name 字段，提供默认值
class CustomAgentState(AgentState):
    user_name: str = "Unknown"


# 2. 写状态的工具：返回 Command，同时回传 ToolMessage
@tool
def update_user_info(user_id: str, runtime: ToolRuntime) -> Command:
    """查询用户信息并更新到会话状态。"""
    name = "John Smith" if user_id == "user_123" else "Unknown"
    return Command(update={
        "user_name": name,
        "messages": [
            ToolMessage(
                content=f"已查询并更新用户信息：{name}",
                tool_call_id=runtime.tool_call_id,
            )
        ],
    })


# 3. 读状态的工具：读取上一步写入的 user_name
@tool
def greet(runtime: ToolRuntime) -> str:
    """根据会话状态中的用户名向用户打招呼。"""
    return f"Hello, {runtime.state['user_name']}!"


# 4. 创建 Agent
agent = create_agent(
    "openai:gpt-5.5",
    tools=[update_user_info, greet],
    state_schema=CustomAgentState,
    checkpointer=InMemorySaver(),
)

# 5. 调用：模型会先后调用两个工具，第二个工具读到第一个工具写入的状态
config = {"configurable": {"thread_id": "1"}}

result = agent.invoke(
    {"messages": [{"role": "user", "content": "先查询 user_123 的信息，然后打个招呼"}]},
    config,
)

print(result["messages"][-1].content)
# 预期输出：Hello, John Smith!

print(result["user_name"])
# 预期输出：John Smith  -- 状态字段已被工具持久化
```

这个示例展示了状态写入的完整闭环：

```text
update_user_info 工具
      |
      v
Command 更新 user_name（持久化到状态）
      |
      v
greet 工具读取 runtime.state["user_name"]
      |
      v
最终结果使用到更新后的状态
```

注意与 Tools 文档中一致的规则：通过 Command 更新当前图时，必须在 update 中包含 tool_call_id 匹配的 ToolMessage，否则 ToolNode 会抛出 ValueError。

### 8.3 在提示中访问：动态 Prompt

要让模型"看到"状态或上下文数据，可以用 `@dynamic_prompt` 中间件在每次模型调用前动态构建系统提示。完整示例如下：

```python
from dataclasses import dataclass
from langchain.agents import create_agent
from langchain.agents.middleware import dynamic_prompt


# 1. 定义上下文结构
@dataclass
class Context:
    user_name: str


# 2. 动态系统提示：从 context 中读取用户名
@dynamic_prompt
def dynamic_system_prompt(request) -> str:
    user_name = request.runtime.context["user_name"]
    return f"你是一个乐于助人的助手。请称呼用户为 {user_name}。"


# 3. 创建 Agent：注册中间件和上下文结构
agent = create_agent(
    "openai:gpt-5.5",
    tools=[],
    middleware=[dynamic_system_prompt],
    context_schema=Context,
)

# 4. 调用：context 随本次调用传入
result = agent.invoke(
    {"messages": [{"role": "user", "content": "你好"}]},
    context=Context(user_name="张三"),
)

print(result["messages"][-1].content)
# 预期输出：你好，张三！……
```

注意区分 state 和 context：

| 维度       | state                      | context                  |
| -------- | -------------------------- | ------------------------ |
| 传入方式     | invoke 的字典字段               | invoke 的 context 参数      |
| 生命周期     | 随 thread 持久化，跨多轮保留          | 仅本次调用有效，不持久化             |
| 典型内容     | user_id、中间计算结果、对话消息        | 运行时配置，如用户名、角色权限          |
| 修改方式     | 工具可通过 Command 修改           | 工具内只读                    |

### 8.4 两个注入时机

| 中间件            | 执行时机       | 典型用途             |
| -------------- | ---------- | ---------------- |
| @before_model  | 每次模型调用之前   | 裁剪消息、注入上下文       |
| @after_model   | 每次模型响应之后   | 删除旧消息、过滤敏感内容     |

这两个钩子是短期记忆管理的主要扩展点：裁剪、删除、摘要、脱敏，本质上都是在模型调用的前后对 messages 做加工。

---

## 九、与长期记忆的边界

短期记忆解决"本会话内记住"，跨会话、跨 Thread 的记忆由 Long-term Memory 承担。

### 9.1 长期记忆简述

长期记忆基于 LangGraph Store 实现，把数据保存为 JSON 文档，按 namespace（类似文件夹）和 key（类似文件名）组织：

```python
from langgraph.store.memory import InMemoryStore

store = InMemoryStore()

# 写入：(namespace, key, 数据)
store.put(
    ("users",),          # namespace
    "user_123",          # key
    {"name": "John Smith", "language": "English"},
)

# 读取
item = store.get(("users",), "user_123")
```

使用时把 store 传给 `create_agent`，工具内通过 `runtime.store` 读写，实现跨会话的用户画像、偏好记录等。

官方概念文档中，长期记忆通常分为三类：

| 类型          | 内容             | 示例              |
| ----------- | -------------- | --------------- |
| Semantic    | 事实与知识          | "用户是 Python 开发者" |
| Episodic    | 经历与事件          | "用户上周抱怨过响应慢"    |
| Procedural  | 行为规则           | "回复用户时始终保持简洁"   |

### 9.2 什么时候用哪个

| 需求                    | 用短期记忆  | 用长期记忆 |
| --------------------- | ----- | ----- |
| "我刚才说的那段话再展开讲讲"       | ✅     |       |
| "记住：这个项目用 Flask"（下次会话也要记得） |    | ✅     |
| 当前对话的多轮上下文            | ✅     |       |
| 用户画像、全局偏好             |       | ✅     |

一个常见的组合模式：会话结束时，用工具或后台任务把会话中的关键信息从短期记忆提炼、写入长期记忆 store，供未来所有会话使用。

---

## 十、常见问题与最佳实践

**1. Agent 忘记了上下文，排查顺序**

- 是否给 `create_agent` 传了 `checkpointer`？（没传 = 无记忆）
- 两轮调用是否用了同一个 `thread_id`？（换了 = 新会话）
- 是否部署了裁剪/删除逻辑，把关键消息删掉了？
- 生产环境是否误用了 `InMemorySaver`？（重启即失忆）

**2. 消息历史必须保持合法**

任何裁剪/删除操作后，检查两点：历史是否以 user 消息开头；每条带 tool_calls 的 AI 消息后面是否紧跟对应的 ToolMessage。

**3. 不要只依赖"窗口够大"**

长上下文不仅贵、慢，还会让模型被无关内容干扰。主动管理消息历史是质量优化，不只是"防报错"。

**4. 摘要要用便宜的模型**

摘要任务是典型的"小模型足够"场景，用主力模型做摘要纯属浪费。

**5. 生产必须用数据库 checkpointer**

InMemorySaver 只适合开发调试。生产环境按技术栈选择 Postgres / SQLite / CosmosDB 等，并注意首次使用时执行 `setup()` 建表。

**6. 用可观测性追踪记忆行为**

官方推荐配合 LangSmith 观察每次模型调用实际收到的消息列表——裁剪和摘要是否生效、上下文是否超预期，一看便知。

---

## 十一、小结

| 主题       | 内容                                            |
| -------- | --------------------------------------------- |
| 定义       | 单个 Thread 内记住之前的交互，本质是 Agent State 中的消息历史     |
| 核心 API   | checkpointer（InMemorySaver / PostgresSaver）+ thread_id |
| 状态扩展     | 继承 AgentState，通过 state_schema 添加自定义字段         |
| 上下文管理    | 裁剪（@before_model）、删除（RemoveMessage）、摘要（SummarizationMiddleware） |
| 访问与修改    | ToolRuntime（工具内读写状态）、Command、@dynamic_prompt、@before_model/@after_model |
| 与长期记忆的边界 | 短期记忆限会话内；跨会话用 Store（namespace + key）          |
