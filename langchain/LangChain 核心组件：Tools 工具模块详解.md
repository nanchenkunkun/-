# LangChain 核心组件：Tools 工具模块详解

## 一、Tools 是什么？

### 1.1 基本概念

Tools 是 LangChain 的核心组件之一，主要负责让大语言模型与外部世界交互。

如果说 Models 是 Agent 的"大脑"，负责理解和决策，那么 Tools 就是 Agent 的"手脚"，负责真正去做事情。

Tools 让 Agent 能够：

- 获取实时数据：查询天气、搜索网页、获取行情。
- 执行代码：运行 Python 脚本、执行 SQL。
- 访问外部系统：查询数据库、调用内部 API。
- 对真实世界产生影响：发送邮件、写入文件、创建订单。

简单来说：

**Tools 就是把普通的 Python 函数，包装成模型能够理解和请求调用的"工具"。**

需要重点理解的一点：

模型本身并不能直接执行你定义的 Python 函数。模型只负责根据对话上下文，决定什么时候调用工具、传什么参数，并生成一个"工具调用请求"。真正的函数执行，由 Agent 或应用代码完成，执行结果再回传给模型。

### 1.2 一个工具的构成

一个 LangChain 工具通常由以下部分组成：

| 组成部分          | 作用                              |
| ------------- | ------------------------------- |
| name          | 工具名称，模型用它来标识和选择工具               |
| description   | 工具描述，告诉模型这个工具做什么、什么时候该用它        |
| args_schema   | 参数的输入模式（Schema），定义参数名称、类型、约束和说明 |
| 执行函数（func）    | 真正干活的 Python 函数                 |
| 返回值           | 工具执行的结果，会被回传给模型                 |

其中：

- name 和 args_schema 会一起转换成工具的输入模式，传给模型。
- description 和参数说明的质量，直接决定模型能否正确地选择和使用工具。
- 执行函数只在应用侧运行，模型永远看不到它的实现代码。

### 1.3 Tools 的工作位置

在 Agent 架构中，Tools 处于模型的"决策"与"执行"之间：

```text
          用户任务
              |
              v
          Models（大脑）
              |
              v
       生成 Tool Call
       （只是请求，不是执行）
              |
              v
       Agent / ToolNode（手脚）
              |
              v
          执行 Tools
              |
              v
       结果包装为 ToolMessage
              |
              v
          Models（大脑）
              |
              v
       继续决策，直到任务完成
```

这个"模型调用工具、结果回传、模型再决策"的循环，就是 Agent 的本质：

**Agent = 模型在循环中不断调用工具，直到任务完成。**

需要注意：模型负责"决定调用什么工具、传什么参数"，应用负责"真正执行工具和组织整个流程"。

---

## 二、创建工具

LangChain 提供了多种创建工具的方式，从简单到灵活依次是：

| 创建方式                    | 适用场景                |
| ----------------------- | ------------------- |
| @tool 装饰器               | 最常用，函数加装饰器即可        |
| @tool + args_schema     | 参数复杂、需要精确约束时        |
| StructuredTool.from_function | 已有现成函数，不想加装饰器时  |
| 继承 BaseTool             | 需要完全控制行为、实现异步、复杂逻辑时 |

### 2.1 @tool 装饰器：最常用的方式

最简单的创建方式是使用 `@tool` 装饰器。

默认情况下，函数名就是工具名，函数的 docstring 会成为工具描述，函数参数（配合类型注解）会自动生成参数模式：

```python
from langchain.tools import tool

@tool
def search_database(query: str, limit: int = 10) -> str:
    """在客户数据库中搜索匹配的记录。

    Args:
        query: 搜索关键词
        limit: 最大返回条数
    """
    return f"找到 {limit} 条与 '{query}' 相关的结果"
```

有几个非常重要的要求：

**类型注解是必须的。** 类型注解定义了工具的输入模式（输入模式），没有类型注解，模型就不知道该传什么类型的参数。

**docstring 就是工具描述。** 模型完全依赖 docstring 来判断"这个工具是干什么的、什么时候该用它"，所以要写得信息明确、简洁。

**命名建议使用 snake_case。** 官方建议工具名使用小写下划线风格（例如 `web_search` 而不是 `Web Search`）。部分模型供应商会拒绝包含空格或特殊字符的工具名，坚持使用字母、数字、下划线和连字符可以获得最好的跨供应商兼容性。

**parse_docstring：解析 docstring 中的参数说明**

开启 `parse_docstring=True` 后，docstring 中 `Args:` 部分会被解析成每个参数的描述，随输入模式一起传给模型：

```python
@tool(parse_docstring=True)
def get_weather(city: str, unit: str = "celsius") -> str:
    """获取指定城市的当前天气。

    Args:
        city: 城市名称，例如"北京"
        unit: 温度单位，celsius 或 fahrenheit
    """
    return f"{city} 当前晴，25 度（{unit}）"
```

参数级别的描述在参数较多、含义不明显时非常有用。

### 2.2 自定义名称与描述

默认工具名来自函数名。当函数名不够清晰时，可以覆盖：

```python
@tool("web_search")            # 自定义工具名
def search(query: str) -> str:
    """搜索网络获取信息。"""
    return f"搜索结果：{query}"

print(search.name)   # web_search
```

也可以覆盖自动生成的描述：

```python
@tool("calculator", description="执行算术计算。遇到任何数学问题都可以使用此工具。")
def calc(expression: str) -> str:
    """计算数学表达式。"""
    return "计算结果"
```

注意：description 参数优先级高于 docstring，两者都写时模型看到的是 description 的内容。

### 2.3 args_schema：用 Pydantic 精确约束参数

当参数较多、有枚举值、有默认值、需要详细说明时，推荐用 Pydantic 模型定义输入模式：

```python
from pydantic import BaseModel, Field
from typing import Literal

class WeatherInput(BaseModel):
    """天气查询的输入。"""
    location: str = Field(description="城市名称或坐标")
    units: Literal["celsius", "fahrenheit"] = Field(
        default="celsius",
        description="温度单位偏好"
    )
    include_forecast: bool = Field(
        default=False,
        description="是否包含 5 天预报"
    )

@tool(args_schema=WeatherInput)
def get_weather(location: str, units: str = "celsius", include_forecast: bool = False) -> str:
    """获取当前天气和可选的预报。"""
    temp = 22 if units == "celsius" else 72
    return f"{location} 当前 {temp} 度（{units}）"
```

args_schema 相比裸函数参数的优势：

- 可以用 `Literal` 限定枚举值，减少模型传错参数。
- 可以设置默认值和详细的字段描述。
- 校验失败时，模型能收到具体的错误信息并自行修正。

除了 Pydantic，也可以直接传 JSON Schema 字典定义输入模式。

### 2.4 StructuredTool.from_function：包装现成函数

如果函数已经存在（例如来自第三方库或历史代码），不想修改它，可以用 `StructuredTool.from_function`：

```python
from langchain_core.tools import StructuredTool

def multiply(a: int, b: int) -> int:
    """计算两个整数的乘积。"""
    return a * b

multiply_tool = StructuredTool.from_function(
    func=multiply,
    name="multiply",
    description="计算两个整数的乘积，用于任何乘法运算",
)
```

效果与 @tool 装饰器基本等价，区别只是不需要改动原函数。

### 2.5 继承 BaseTool：完全控制

需要更细粒度控制（自定义校验逻辑、维护内部状态、同时实现同步与异步）时，可以继承 `BaseTool`：

```python
from typing import Any
from langchain_core.tools import BaseTool
from pydantic import BaseModel, Field


class CalculatorInput(BaseModel):
    expression: str = Field(description="要计算的数学表达式")


class CalculatorTool(BaseTool):
    name: str = "calculator"
    description: str = "计算数学表达式。遇到任何数学计算时使用。"
    args_schema: type[BaseModel] = CalculatorInput

    def _run(self, expression: str, **kwargs: Any) -> str:
        # 实际实现中应使用安全的表达式求值方式
        return f"计算结果：{expression} = 42"
```

### 2.6 异步工具

在异步应用（如 FastAPI 服务）中，工具应该实现异步版本，避免阻塞事件循环。

使用 BaseTool 时，实现 `_arun` 方法：

```python
class AsyncSearchTool(BaseTool):
    name: str = "async_search"
    description: str = "异步搜索工具"

    def _run(self, query: str) -> str:
        # 同步实现
        return f"同步搜索结果：{query}"

    async def _arun(self, query: str) -> str:
        # 异步实现，例如使用 httpx.AsyncClient
        return f"异步搜索结果：{query}"
```

使用 @tool 装饰器时，也可以直接装饰 async 函数：

```python
@tool
async def asearch(query: str) -> str:
    """异步搜索信息。"""
    return f"搜索结果：{query}"
```

注意：只提供同步实现的工具，在异步执行时会退化为在线程池中运行，高并发场景下应尽量提供原生异步实现。

---

## 三、工具的核心属性与直接调用

工具创建后，它是一个普通的 Python 对象，可以单独检查和调用，完全不依赖模型。

### 3.1 查看工具的属性

```python
@tool
def get_weather(city: str) -> str:
    """获取指定城市的天气。"""
    return f"{city} 当前晴。"

print(get_weather.name)          # get_weather
print(get_weather.description)   # 获取指定城市的天气。
print(get_weather.args)
# {'city': {'title': 'City', 'type': 'string'}}
```

`args` 属性就是自动生成的参数模式，它会随工具定义一起传给模型。

### 3.2 直接调用工具

工具可以直接 `invoke()`，不需要经过模型：

```python
result = get_weather.invoke({"city": "北京"})
print(result)   # 北京 当前晴。

# 直接传字符串时，会尝试按位置匹配到第一个参数
result = get_weather.invoke("北京")
```

这个特性很有用：工具可以先独立开发、独立测试，再接入 Agent。工具的 bug 不需要通过跑一整轮 Agent 才能发现。

---

## 四、工具与模型的连接

单独的工具只是函数，真正的威力在于把工具接入模型，让模型自主决策调用。

### 4.1 bind_tools：绑定工具到模型

模型的 `bind_tools()` 会把工具定义（名称、描述、参数模式）转换成模型供应商要求的格式并随请求发送：

```python
from langchain.chat_models import init_chat_model

model = init_chat_model("openai:gpt-5.5")
model_with_tools = model.bind_tools([get_weather])

response = model_with_tools.invoke("北京今天的天气怎么样？")
```

模型判断需要查天气时，返回的 AIMessage 中会带有 `tool_calls`：

```python
print(response.tool_calls)
# [{
#     "name": "get_weather",
#     "args": {"city": "北京"},
#     "id": "call_abc123",
#     "type": "tool_call"
# }]
```

每一条 tool_call 包含：

| 字段     | 含义                   |
| ------ | -------------------- |
| name   | 要调用的工具名              |
| args   | 模型生成的参数              |
| id     | 调用 ID，回传结果时必须原样带上    |
| type   | 固定为 tool_call        |

再次强调：**这只是一个请求，函数并没有被执行。**

### 4.2 完整的一轮工具调用

理解工具调用最好的方式，是脱离 Agent 手动走一遍完整流程：

```python
from langchain.messages import HumanMessage, ToolMessage

tools = [get_weather]
tools_by_name = {t.name: t for t in tools}

model_with_tools = model.bind_tools(tools)

messages = [HumanMessage("北京今天的天气怎么样？")]

# 第一轮：模型生成工具调用请求
response = model_with_tools.invoke(messages)
messages.append(response)

# 应用侧执行工具
for tool_call in response.tool_calls:
    tool = tools_by_name[tool_call["name"]]
    result = tool.invoke(tool_call["args"])
    # 结果包装为 ToolMessage，tool_call_id 必须对应
    messages.append(ToolMessage(
        content=result,
        tool_call_id=tool_call["id"],
    ))

# 第二轮：模型看到工具结果，生成最终回答
final = model_with_tools.invoke(messages)
print(final.content)
```

整个流程：

```text
用户问题
   |
   v
模型 + bind_tools
   |
   v
AIMessage(tool_calls=[...])     <-- 决策
   |
   v
应用执行工具，得到结果            <-- 执行
   |
   v
ToolMessage(content, tool_call_id)
   |
   v
模型 + 完整消息历史
   |
   v
AIMessage(最终回答)
```

两个容易踩的坑：

- 每一条 tool_call 都必须有对应的 ToolMessage，且 `tool_call_id` 要一一匹配，否则消息历史不合法，模型调用会报错。
- 回传给模型时要把完整的消息历史（包括 AIMessage 和 ToolMessage）一起传入，模型才知道之前发生了什么。

### 4.3 在 create_agent 中使用

实际项目中，上述循环通常由 Agent 自动完成。

LangChain 的 `create_agent` 是官方推荐的 Agent 构建入口，它把"模型循环调用工具"的整套机制封装好：

```python
from langchain.agents import create_agent
from langchain.tools import tool


@tool
def search(query: str) -> str:
    """搜索信息。"""
    return f"搜索结果：{query}"


agent = create_agent(
    model="openai:gpt-5.5",
    tools=[search],
    system_prompt="你是一个乐于助人的助手",
)

result = agent.invoke({
    "messages": [{"role": "user", "content": "北京今天天气怎么样？"}]
})
print(result["messages"][-1].content)
```

Agent 内部自动完成了：把工具传给模型 -> 解析 tool_calls -> 执行工具 -> 包装 ToolMessage -> 回传模型 -> 循环，直到模型不再请求工具。

`tools` 参数非常宽松，可以接受：

- 任何 Python 可调用对象（自动转换成工具）。
- LangChain 工具对象（@tool、BaseTool 等）。
- 工具字典（自定义的供应商格式）。

### 4.4 LangGraph 中的 ToolNode

如果使用 LangGraph 自行编排工作流，可以使用预置的 `ToolNode` 节点执行工具。

它会自动从最近一条 AIMessage 中取出所有 tool_calls，逐个执行并把结果作为 ToolMessage 写回状态，相当于把 4.2 中的"执行"部分组件化。

```text
Agent 节点（模型决策）
   |
   v
ToolNode（执行所有 tool_calls）
   |
   v
回到 Agent 节点
```

---

## 五、Tools 进阶主题

### 5.1 InjectedToolArg：对模型隐藏参数

有些参数不应该由模型生成，例如用户身份、数据库连接、会话信息。这些参数在调用时由应用注入，模型不应该看到它们。

使用 `InjectedToolArg` 注解：

```python
from typing import Annotated
from langchain_core.tools import InjectedToolArg, tool

@tool
def list_user_files(
    user_id: Annotated[str, InjectedToolArg],
    keyword: str,
) -> str:
    """按关键词列出当前用户的文件。"""
    return f"用户 {user_id} 的文件中匹配 '{keyword}' 的结果"
```

行为特点：

- `user_id` 不会出现在工具的参数模式中，模型看不到它。
- 模型只会生成 `keyword` 参数。
- `user_id` 在调用时由应用侧负责传入。

这是实现"工具能力开放、敏感数据隔离"的基础手段。

### 5.2 ToolRuntime：运行时上下文注入

LangChain 新版本提供了更系统的运行时注入机制。在工具签名中加上 `runtime: ToolRuntime` 参数，工具执行时会被自动注入运行时上下文，且不会暴露给模型：

```python
from langchain.tools import tool, ToolRuntime
from langchain.messages import HumanMessage

@tool
def get_last_user_message(runtime: ToolRuntime) -> str:
    """获取用户最近的一条消息。"""
    messages = runtime.state["messages"]

    for message in reversed(messages):
        if isinstance(message, HumanMessage):
            return message.content

    return "没有找到用户消息"

@tool
def get_user_preference(pref_name: str, runtime: ToolRuntime) -> str:
    """获取用户偏好设置。"""
    preferences = runtime.state.get("user_preferences", {})
    return preferences.get(pref_name, "未设置")
```

常用能力：

| 属性                       | 作用                       |
| ------------------------ | ------------------------ |
| runtime.state            | 读取当前 Agent 的状态（如消息历史、自定义字段） |
| runtime.tool_call_id     | 当前工具调用的 ID，构造 ToolMessage 时使用 |
| runtime.execution_info   | 线程 ID、运行 ID、重试次数等执行信息    |

在第一个例子中，模型只看到"这个工具不需要参数"，而 runtime 是隐藏的。

### 5.3 return_direct：直接返回工具结果

默认流程中，工具执行完结果会回传给模型，由模型再组织一次回答。

`return_direct=True` 会跳过这最后一次模型调用，把工具结果直接作为最终答案返回：

```python
@tool(return_direct=True)
def get_order_status(order_id: str) -> str:
    """查询订单状态。"""
    return f"订单 {order_id}：已发货，预计 2 天后送达"
```

适用场景：

- 工具结果已经是最终答案，模型再"转述"一次只是浪费。
- 对延迟敏感的链路（少一次模型调用就少几秒）。

代价是模型无法对结果做补充、润色或结合上下文解释，需要根据业务权衡。

### 5.4 工具的返回值类型

官方支持三种返回值形式，按需选择：

**返回字符串**

最常见的形式，工具结果直接作为 ToolMessage 的内容回传给模型：

```python
@tool
def get_weather(city: str) -> str:
    """获取城市天气。"""
    return f"{city} 当前晴。"
```

适合结果本身就是可读文本的场景。

**返回对象（如 dict）**

返回结构化数据，模型可以按字段阅读和推理：

```python
@tool
def get_weather_data(city: str) -> dict:
    """获取结构化的天气数据。"""
    return {
        "city": city,
        "temperature_c": 22,
        "conditions": "sunny",
    }
```

适合后续推理需要明确字段、而不是自由文本的场景。

**返回 Command：更新 Agent 状态**

工具不仅要返回数据，还想直接修改 Agent 状态（如设置用户偏好）时，返回 `Command`：

```python
from langchain.messages import ToolMessage
from langchain.tools import ToolRuntime, tool
from langgraph.types import Command

@tool
def set_language(language: str, runtime: ToolRuntime) -> Command:
    """设置首选回复语言。"""
    return Command(
        update={
            "preferred_language": language,
            "messages": [
                ToolMessage(
                    content=f"语言已设置为 {language}。",
                    tool_call_id=runtime.tool_call_id,
                )
            ],
        }
    )
```

注意：Command 面向当前图更新时，必须在 update 中包含一条 ToolMessage，且 `tool_call_id` 与当前调用匹配，否则 ToolNode 会直接抛出 ValueError——因为消息历史中每个 tool_call 都必须有对应的结果。

### 5.5 工具错误处理

工具执行可能失败：参数错误、网络超时、权限不足。

关键原则：**不要让异常直接打断 Agent，而是把错误信息回传给模型，让模型自行修正重试。**

**Agent 中间件方式（推荐）**

LangChain 新版本通过中间件统一处理工具调用错误：

```python
from collections.abc import Callable

from langchain.agents import create_agent
from langchain.agents.middleware import wrap_tool_call
from langchain.messages import ToolMessage
from langchain.tools.tool_node import ToolCallRequest


@wrap_tool_call
def handle_tool_errors(
    request: ToolCallRequest,
    handler: Callable[[ToolCallRequest], ToolMessage],
) -> ToolMessage:
    """把工具异常转换成模型可以理解的 ToolMessage。"""
    try:
        return handler(request)
    except Exception as e:
        return ToolMessage(
            content=f"工具出错：请检查输入后重试。({e})",
            tool_call_id=request.tool_call["id"],
        )


agent = create_agent(
    model="openai:gpt-5.5",
    tools=[get_weather],
    middleware=[handle_tool_errors],
)
```

错误回传后的典型流程：

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
模型读到错误信息，修正参数重试或向用户澄清
```

模型往往能根据错误信息自动修正，这比把异常抛给用户的体验好得多。

**ToolNode 方式（LangGraph）**

在 LangGraph 的 ToolNode 中，可以通过 `handle_tool_errors` 配置错误处理：设为 True 时自动把异常转成错误消息回传，也可以传入自定义函数生成更友好的错误文本。

### 5.6 动态工具选择

工具数量过多会带来两个问题：

- 模型的选择准确率下降，容易选错工具或传错参数。
- 每次请求都要携带全部工具定义，Token 开销增大。

当工具数量较大时，可以按上下文动态筛选工具：根据用户身份、当前任务阶段、对话内容，只把相关工具注入本次请求。LangChain 的中间件机制支持在每轮请求前动态调整工具列表。

```python
from langchain.agents.middleware import wrap_model_call, ModelRequest, ModelResponse
from typing import Callable

@wrap_model_call
def filter_tools_by_role(
    request: ModelRequest,
    handler: Callable[[ModelRequest], ModelResponse],
) -> ModelResponse:
    """根据用户角色过滤可用工具。"""
    user_role = request.runtime.context.user_role  # 从运行时上下文获取用户角色

    if user_role == "admin":
        tools = request.tools  # 管理员：看到全部工具
    elif user_role == "analyst":
        # 数据分析师：看到读取工具和查询工具
        tools = [t for t in request.tools if t.name.startswith(("read_", "query_"))]
    else:
        # 普通用户：只看到只读工具
        tools = [t for t in request.tools if t.name.startswith("read_")]

    # 用过滤后的工具列表重写本次请求
    return handler(request.override(tools=tools))

# Agent 创建时仍然注册所有可能的工具
agent = create_agent(
    model="gpt-4o",
    tools=[read_data, write_data, delete_data, query_sql, ...],
    middleware=[filter_tools_by_role],
)
```

实践建议：单个 Agent 的工具数量尽量控制在少量核心工具；同类工具可以合并为一个带 `action` 参数的工具；不常用能力优先用动态加载。

### 5.7 Human-in-the-loop：人工介入

对于删除文件、转账付款、发送对外邮件等高风险操作，完全自动执行并不合适。

LangChain 提供 `HumanInTheLoopMiddleware`，可以指定哪些工具在执行前必须经过人工审批：

```python
from langchain.agents import create_agent
from langchain.agents.middleware import HumanInTheLoopMiddleware


@tool
def write_file(path: str, content: str) -> str:
    """写入文件。"""
    return f"已写入 {path}"


agent = create_agent(
    model="openai:gpt-5.5",
    tools=[write_file],
    middleware=[
        HumanInTheLoopMiddleware(interrupt_on={"write_file": True})
    ],
)
```

流程上，Agent 会在执行 write_file 前暂停，等待人工批准、编辑或拒绝后再继续。这是生产环境保障安全的重要机制。

### 5.8 Headless Tools：模式与执行分离

还有一种特殊模式：工具只定义模式（name、description、args_schema），不提供本地实现：

```python
from langchain.tools import tool

# 只有模式，没有函数体
headless_tool = tool(
    name="submit_form",
    description="在浏览器页面中提交表单",
    args_schema=FormInput,
)
```

当模型调用这种工具时，执行不会发生在服务进程内，而是产生一个中断（interrupt），由前端页面、其他服务或人工环节完成实际操作后，再把结果回填恢复执行。

典型场景：浏览器自动化（操作发生在用户浏览器里）、跨服务协作、人工操作环节。

### 5.9 MCP：工具生态的标准化

Model Context Protocol（MCP）是一个开放协议，让外部系统以标准方式向 Agent 暴露工具。

LangChain 支持把 MCP 服务器提供的工具直接加载进 Agent，例如数据库操作、文件系统访问、第三方 SaaS 等能力，都可以通过接入对应 MCP 服务器获得，而不需要自己逐一实现。

可以把 MCP 理解为"工具领域的 USB 接口"：一次实现，处处可用。

---

## 六、预置工具与工具包

### 6.1 Tools 与 Toolkits

除了自己写工具，LangChain 官方维护了大量预置工具（Prebuilt tools）。

相关概念：

| 概念          | 含义                  |
| ----------- | ------------------- |
| Tool        | 单个工具，完成一件具体的事       |
| Toolkit     | 工具包，一组协同完成某类任务的工具集合 |

例如 Gmail Toolkit 通常同时包含读邮件、搜邮件、发邮件、起草邮件等多个工具。

### 6.2 官方工具分类概览

官方集成目录（docs.langchain.com - Tool integrations）中的主要类别：

| 类别         | 代表性集成                                        |
| ---------- | -------------------------------------------- |
| 搜索         | Tavily Search、Google Search、Exa、Brave Search |
| 代码解释器      | Amazon Bedrock AgentCore、Azure Container Apps、Capsule |
| 数据库        | MCP Toolbox、Sail SQL Toolkit、Stardog         |
| 网页浏览       | AgentQL、Browser Use、Firecrawl、Stagehand      |
| 生产力        | Gmail Toolkit、Google Calendar Toolkit        |
| 金融         | DexPaprika、Uniswap、Privy                     |
| 安全         | URLCheck、Tonic Textual（PII 脱敏）               |
| 媒体生成       | Magic Hour、PopNow                            |
| 集成平台       | Composio（500+ 集成）、Scalekit                   |

### 6.3 使用示例

以 Tavily 搜索为例：

```python
# pip install langchain-tavily
from langchain.agents import create_agent
from langchain_tavily import TavilySearch

search = TavilySearch(max_results=2)

agent = create_agent(
    model="openai:gpt-5.5",
    tools=[search],
)

result = agent.invoke({
    "messages": [{"role": "user", "content": "LangChain 最新版本是什么？"}]
})
```

使用预置工具的一般步骤：

1. 安装对应的集成包（如 `langchain-tavily`）。
2. 配置该服务需要的 API 密钥。
3. 实例化工具，按需调整参数。
4. 加入 `create_agent` 的 `tools` 列表。

实践建议：优先使用官方预置工具，而不是自己重新实现搜索、浏览等通用能力。自己写的代码应该聚焦在真正业务独有的部分。

---

## 七、工具设计最佳实践

结合官方文档的建议，设计工具时注意以下几点：

**1. 命名规范**

使用 snake_case，只包含字母、数字、下划线和连字符。避免空格和特殊字符，保证跨供应商兼容。

**2. 描述决定成败**

模型完全依赖 description 和参数描述来选择工具。描述应该：

- 说清楚"做什么"，也说清楚"什么时候该用、什么时候不该用"。
- 参数含义不明显时，务必在 args_schema 或 docstring 中写明。

**3. 参数尽可能少**

每个参数都是模型出错的机会。能用一个参数解决的，不要拆成五个；能设默认值的，加上默认值。

**4. 类型注解完整**

类型注解就是约束。能用 Literal 枚举的不要用自由字符串，能限定数字范围的加约束。

**5. 返回对模型有用的信息**

- 成功时：返回模型下一步需要的信息，而不是完整的原始数据 dump。
- 失败时：返回具体、可操作的错误信息，让模型能自我修正。
- 超长结果：先做摘要或截断，避免撑爆上下文。

**6. 高风险操作加护栏**

破坏性操作（删除、写入、支付、对外发送）应配合 Human-in-the-loop 审批，或设计成"先确认、后执行"的两步工具。

**7. 工具可以独立测试**

工具本质是函数，先用 `tool.invoke()` 单测，再接入 Agent。不要把工具调试和 Agent 调试混在一起。

**8. 用可观测性工具追踪**

官方推荐配合 LangSmith 追踪每一次工具调用：何时被调用、传了什么参数、返回了什么、耗时多少。这是定位 Agent 行为问题的最有效手段。

---

## 八、小结

| 主题       | 内容                                        |
| -------- | ----------------------------------------- |
| 基础概念     | 工具 = 名称 + 描述 + 参数模式 + 执行函数                |
| 创建方式     | @tool、args_schema、StructuredTool、BaseTool |
| 与模型连接    | bind_tools、tool_calls、ToolMessage、tool_call_id |
| 执行主体     | create_agent、ToolNode                     |
| 进阶能力     | InjectedToolArg、ToolRuntime、return_direct、Command |
| 可靠性      | 错误回传、动态工具选择、人工审批、Headless Tools、MCP       |
| 生态       | 预置工具与工具包（搜索、代码执行、数据库、浏览、生产力等）             |

与 Models 模块的关系可以总结为一句话：

**Models 负责决定"做什么"，Tools 负责"真正去做"，Agent 把两者放进同一个循环，直到任务完成。**
