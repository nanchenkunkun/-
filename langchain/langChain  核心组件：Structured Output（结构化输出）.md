# LangChain 核心组件：结构化输出（Structured Output）



## 一、什么是结构化输出？

### 1.1 为什么需要结构化输出？

普通模型调用通常返回自然语言文本。例如：

``` python
response = model.invoke(
    "帮我分析一下这段评论：这个手机性能不错，但是价格有点贵"
)
print(response.content)
```

模型可能返回一段自然语言分析。人类容易理解，但程序很难稳定地从不同表达中提取情感、优缺点和评分。

### 1.2 定义

结构化输出是指：通过预先定义数据结构，约束大模型按照指定格式返回结果，使模型输出能够被程序直接读取和使用。

例如：

``` json
{
  "sentiment": "positive",
  "score": 4,
  "advantages": ["性能不错"],
  "disadvantages": ["价格较贵"],
  "summary": "整体评价偏正面"
}
```

程序便可以直接读取对应字段，而不需要从自然语言中提取信息。

### 1.3 核心价值

  优势           说明
-------------- --------------------------------
  格式统一       按照预定义的数据结构返回
  方便解析       可以直接获取字段
  类型约束       可以限制字段的数据类型
  数据验证       可以检查字段是否满足预设规则
  方便集成       可以用于数据库、API 和业务逻辑
  降低不确定性   减少输出格式不一致的问题

注意：结构化输出解决的是输出格式和数据结构问题，不保证模型提取的信息一定真实、准确。合法
JSON 中仍然可能包含错误信息。

------------------------------------------------------------------------

## 二、LangChain 结构化输出的整体实现方式

LangChain 有两种常见的结构化输出使用场景。

### 2.1 直接使用 Chat Model

``` text
用户输入
   |
   v
model.with_structured_output(Schema)
   |
   v
返回结构化数据
```

适合信息抽取、分类、数据转换等单次模型调用。

### 2.2 在 Agent 中使用

``` text
用户输入
   |
   v
create_agent(response_format=Schema)
   |
   v
Agent 执行模型调用、工具调用等流程
   |
   v
result["structured_response"]
```

适合需要工具调用、任务编排和复杂业务流程的 Agent。

  对比                  Chat Model                   Agent
--------------------- ---------------------------- --------------------------
  核心 API              `with_structured_output()`   `response_format`
  使用对象              Chat Model                   Agent
  是否需要 Agent 循环   不需要                       由 Agent 管理
  工具调用              可以单独组合                 可以与 Agent 工具结合
  结果获取              直接获取返回值               `structured_response`
  典型用途              数据提取、分类               Agent 最终结果、业务决策

------------------------------------------------------------------------

## 三、Schema：定义结构化数据的格式

Schema是数据结构规范，用来描述需要返回哪些字段、字段类型、字段含义、必需字段以及字段约束。

LangChain 支持多种 Schema 定义方式： - Pydantic - TypedDict -Dataclass - JSON Schema

### 3.1 Pydantic

Pydantic 是 Python 生态中常见的数据验证库，LangChain对它提供了良好支持。

``` python
from pydantic import BaseModel, Field


class Person(BaseModel):
    name: str = Field(description="用户姓名")
    age: int = Field(description="用户年龄")
    email: str = Field(description="用户邮箱")
```

  字段    类型   描述
------- ------ ----------
  name    str    用户姓名
  age     int    用户年龄
  email   str    用户邮箱

-   `BaseModel`：Pydantic 提供的基础模型类。
-   `Field`：定义字段的额外信息。
-   `description`：描述字段含义，帮助模型理解字段要求。

还可以增加数据约束：

``` python
from pydantic import BaseModel, Field


class Product(BaseModel):
    name: str = Field(description="商品名称")
    price: float = Field(description="商品价格", gt=0)
    rating: int = Field(description="商品评分", ge=1, le=5)
```

-   `gt=0`：价格必须大于 0。
-   `ge=1`：评分必须大于或等于 1。
-   `le=5`：评分必须小于或等于 5。

如果数据不满足 Pydantic 验证规则，就会产生验证错误。Pydantic
的优势是既能定义数据结构，也能进行运行时数据验证。

### 3.2 TypedDict

TypedDict 是 Python 提供的一种类型注解方式。

``` python
from typing_extensions import TypedDict


class Person(TypedDict):
    name: str
    age: int
    email: str
```

TypedDict 主要用于类型提示，不会像 Pydantic 一样自动进行运行时数据验证。也可以使用 `Annotated` 添加字段描述：

``` python
from typing_extensions import TypedDict, Annotated


class Person(TypedDict):
    name: Annotated[str, ..., "用户姓名"]
    age: Annotated[int, ..., "用户年龄"]
```

其中 `...` 表示该字段是必需的。

  对比           Pydantic        TypedDict
-------------- --------------- ----------------
  定义字段类型   支持            支持
  字段描述       Field           Annotated
  运行时验证     支持            不提供
  返回类型       Pydantic 实例   dict
  适用场景       需要数据验证    简单的数据结构

### 3.3 Dataclass

Dataclass 是 Python 标准库提供的数据类装饰器。

``` python
from dataclasses import dataclass


@dataclass
class Person:
    name: str
    age: int
    email: str
```

使用：

``` python
person = Person(
    name="张三",
    age=25,
    email="test@example.com"
)
```

Dataclass 可以快速定义数据类、减少样板代码，但本身不提供 Pydantic那样完整的运行时字段验证。Agent 结构化输出中不同 Schema的结果形式，应以当前 LangChain 版本文档为准。

### 3.4 JSON Schema

JSON Schema 是描述 JSON 数据结构的规范。

``` python
person_schema = {
    "title": "Person",
    "description": "用户基本信息",
    "type": "object",
    "properties": {
        "name": {
            "type": "string",
            "description": "用户姓名"
        },
        "age": {
            "type": "integer",
            "description": "用户年龄"
        },
        "email": {
            "type": "string",
            "description": "用户邮箱"
        }
    },
    "required": ["name", "age", "email"]
}
```

  属性          作用
------------- -------------
  title         Schema 名称
  description   Schema 描述
  type          数据类型
  properties    定义字段
  required      必填字段

JSON Schema 的优势是通用性，不依赖 Python的特定数据类，可以在不同语言和系统之间共享。

------------------------------------------------------------------------

## 四、with_structured_output()：Chat Model 的结构化输出

### 4.1 基本介绍

`with_structured_output()` 是 Chat Model 提供的方法，可以将普通 ChatModel 转换为具有结构化输出能力的模型。

基本语法：

``` python
structured_model = model.with_structured_output(
    schema
)
```

常见参数：

  参数            说明
--------------- --------------------------
  `schema`        定义期望的输出结构
  `method`        指定结构化输出实现方式
  `include_raw`   是否同时返回原始模型响应

具体支持的参数和实现方式取决于模型集成。

### 4.2 基本使用示例

``` python
from pydantic import BaseModel, Field
from langchain.chat_models import init_chat_model


class Movie(BaseModel):
    title: str = Field(description="电影名称")
    year: int = Field(description="上映年份")
    director: str = Field(description="导演")
    rating: float = Field(description="电影评分")


model = init_chat_model(
    "openai:YOUR_MODEL_NAME",
    temperature=0
)

structured_model = model.with_structured_output(Movie)

result = structured_model.invoke(
    "介绍一下电影《盗梦空间》"
)

print(result)
```

返回结果类似：

``` python
Movie(
    title="Inception",
    year=2010,
    director="Christopher Nolan",
    rating=8.8
)
```

普通模型调用通常返回 `AIMessage`，而使用 `with_structured_output()`后，得到的是经过解析的结构化结果。使用 Pydantic Schema 时，结果是Pydantic 对象：

``` python
print(result.title)
print(result.year)
print(result.director)
```

也可以转换成字典：

``` python
data = result.model_dump()
```

### 4.3 嵌套结构化输出

实际业务中的数据结构可能包含嵌套对象和列表。

``` python
from pydantic import BaseModel, Field


class Actor(BaseModel):
    name: str = Field(description="演员姓名")
    role: str = Field(description="演员角色")


class MovieDetails(BaseModel):
    title: str = Field(description="电影名称")
    year: int = Field(description="上映年份")
    cast: list[Actor] = Field(description="演员列表")
    genres: list[str] = Field(description="电影类型")
    budget: float | None = Field(
        default=None,
        description="电影预算"
    )
```

调用：

``` python
structured_model = model.with_structured_output(
    MovieDetails
)

result = structured_model.invoke(
    "介绍电影《盗梦空间》，包括演员、类型和预算"
)
```

返回结果可能是：

``` python
MovieDetails(
    title="Inception",
    year=2010,
    cast=[
        Actor(name="Leonardo DiCaprio", role="Cobb")
    ],
    genres=["科幻", "动作", "悬疑"],
    budget=160.0
)
```

`MovieDetails` 内部包含 `Actor`类型的列表，说明结构化输出支持复杂的嵌套结构。

### 4.4 include_raw 参数

默认情况下，`with_structured_output()`主要返回解析后的结构化数据。在调试和生产环境中，有时还需要查看模型原始响应，例如查看Token 使用情况、分析工具调用信息、定位解析失败原因。

``` python
structured_model = model.with_structured_output(
    Movie,
    include_raw=True
)

result = structured_model.invoke(
    "介绍一下电影《盗梦空间》"
)
```

返回结果结构类似：

``` python
{
    "raw": AIMessage(...),
    "parsed": Movie(...),
    "parsing_error": None
}
```

  字段              说明
----------------- --------------------------
  `raw`             模型原始返回的 AIMessage
  `parsed`          解析后的结构化对象
  `parsing_error`   解析过程中发生的错误

``` python
parsed = result["parsed"]
raw = result["raw"]
error = result["parsing_error"]
```

该参数适合用于监控结构化输出质量，同时保存原始结果和解析结果。

------------------------------------------------------------------------

## 五、结构化输出的实现策略

虽然使用的是同一个 `with_structured_output()`API，但不同模型提供商可能采用不同的底层实现方式。常见方式包括：

1.  JSON Schema
2.  Function Calling
3.  JSON Mode

### 5.1 JSON Schema


方式通常利用模型提供商原生支持的结构化输出能力，要求模型按照指定 Schema返回数据。

``` python
structured_model = model.with_structured_output(
    Movie,
    method="json_schema"
)
```

流程可以理解为：

``` text
应用程序
   |
   v
LangChain
   |
   v
将 Schema 转换为模型 API 所需格式
   |
   v
模型提供商原生结构化输出接口
   |
   v
模型返回符合 Schema 的数据
   |
   v
LangChain 解析结果
   |
   v
Pydantic 对象
```

特点： - 使用模型提供商原生能力。 - 可以在模型生成阶段约束输出结构。 -通常具有较高的格式可靠性。 - 不需要将结构化输出模拟成普通工具调用。并不是所有模型都支持原生 JSON Schema结构化输出，需要根据模型提供商文档确认。

### 5.2 Function Calling

Function Calling 也叫 ToolCalling。本质上是利用模型的工具调用能力实现结构化输出。

例如定义 Schema：

``` python
class Person(BaseModel):
    name: str
    age: int
```

LangChain 可以将 Schema 转换成类似工具定义的结构：

``` json
{
  "name": "Person",
  "description": "用户信息",
  "parameters": {
    "type": "object",
    "properties": {
      "name": {"type": "string"},
      "age": {"type": "integer"}
    },
    "required": ["name", "age"]
  }
}
```

然后要求模型按照这个工具的参数结构生成结果。模型返回的工具调用参数会被LangChain 解析成 Pydantic 对象。

**Function Calling在结构化输出场景下，不一定意味着真的要执行某个外部函数。**它只是借用了模型的工具调用机制，让模型按照指定的参数 Schema 返回数据。

### 5.3 JSON Mode

JSON Mode 是一种要求模型返回合法 JSON 的方式。

``` python
structured_model = model.with_structured_output(
    Movie,
    method="json_mode"
)
```

JSON Mode 主要保证输出是 JSON 格式，但不一定保证 JSON 内部字段符合指定Schema。

例如期望：

``` json
{
  "name": "张三",
  "age": 25
}
```

模型可能返回：

``` json
{
  "username": "张三",
  "age": 25,
  "address": "北京"
}
```

这仍然是合法 JSON，但字段结构与预期不同。因此 JSON Mode 通常需要通过Prompt 明确描述输出格式，并在应用程序中进行额外验证。

### 5.4 三种策略对比

-------------------------------------------------------------------------------
  对比              JSON Schema              Function Calling   JSON Mode
----------------- ------------------------ ------------------ -----------------
  主要原理          提供商原生结构化能力     借助工具调用       生成合法 JSON

  Schema 约束       原生支持时较强           通过工具参数       主要依赖 Prompt
                                             Schema             

  输出格式          结构化数据               工具调用参数       JSON

  适用情况          模型支持原生结构化输出   模型支持工具调用   模型只支持 JSON模式

实际开发建议：优先考虑模型提供商原生结构化输出能力；如果模型不支持，可以考虑Function Calling；JSON Mode 适合模型只提供 JSON 输出能力的情况。

具体支持情况、参数名称以及行为取决于模型集成，不能认为所有模型都支持全部方式。

------------------------------------------------------------------------

## 六、Agent 中的结构化输出

前面介绍的是直接使用 Chat Model 的结构化输出。如果使用 LangChain Agent，也可以使用结构化输出。这里涉及一个新的参数：`response_format`。

### 6.1 基本使用

官方推荐使用 `create_agent()` 创建 Agent，并通过 `response_format`
指定结构化输出格式。

``` python
from pydantic import BaseModel, Field
from langchain.agents import create_agent


class ContactInfo(BaseModel):
    name: str = Field(description="联系人姓名")
    email: str = Field(description="联系人邮箱")
    phone: str = Field(description="联系人电话")


agent = create_agent(
    model="openai:YOUR_MODEL_NAME",
    tools=[],
    response_format=ContactInfo
)


result = agent.invoke({
    "messages": [
        {
            "role": "user",
            "content": "提取以下信息：张三，邮箱 test@example.com，电话 13800000000"
        }
    ]
})
```

获取结果：

``` python
structured_response = result["structured_response"]
print(structured_response)
```

返回结果类似：

``` python
ContactInfo(
    name="张三",
    email="test@example.com",
    phone="13800000000"
)
```

Agent 的结构化输出结果不是直接从 `messages` 中获取，而是通过
`result["structured_response"]` 获取。

### 6.2 response_format 支持什么？

常见 Schema 类型包括：

  类型          说明
------------- ----------------------------------
  Pydantic      支持字段验证，返回 Pydantic 实例
  Dataclass     Python 数据类
  TypedDict     类型字典
  JSON Schema   JSON 数据结构规范

还可以使用两种策略： - `ProviderStrategy` - `ToolStrategy`

------------------------------------------------------------------------

## 七、ProviderStrategy 和 ToolStrategy

它们分别代表 Agent 结构化输出的两种不同实现策略。

### 7.1 ProviderStrategy

ProviderStrategy 表示使用模型提供商原生支持的结构化输出能力。

``` python
from langchain.agents import create_agent
from langchain.agents.structured_output import ProviderStrategy
from pydantic import BaseModel, Field


class ProductInfo(BaseModel):
    name: str = Field(description="商品名称")
    price: float = Field(description="商品价格")


agent = create_agent(
    model="openai:YOUR_MODEL_NAME",
    tools=[],
    response_format=ProviderStrategy(
        schema=ProductInfo
    )
)
```

`ProviderStrategy(schema=ProductInfo)`
表示明确指定使用提供商原生结构化输出策略。

主要特点： 1. 使用模型提供商原生能力。 2. 模型 API 会根据 Schema
约束输出。 3. 不需要将结构化输出转换为普通工具调用。 4.
在支持的模型上通常具有较好的可靠性。

还可以配置：

``` python
ProviderStrategy(
    schema=ProductInfo,
    strict=True
)
```

`strict` 用于要求更加严格地遵循 Schema。该参数的可用性取决于 LangChain版本和模型提供商的支持情况，使用前应核对当前官方 API 文档。

### 7.2 ToolStrategy

ToolStrategy 表示使用模型的 Tool Calling 能力实现结构化输出。

``` python
from langchain.agents import create_agent
from langchain.agents.structured_output import ToolStrategy
from pydantic import BaseModel, Field


class ProductInfo(BaseModel):
    name: str = Field(description="商品名称")
    price: float = Field(description="商品价格")


agent = create_agent(
    model="openai:YOUR_MODEL_NAME",
    tools=[],
    response_format=ToolStrategy(
        schema=ProductInfo
    )
)
```

主要特点： 1. 适用于支持 Tool Calling 的模型。 2. 将 Schema转换为工具定义。 3. 模型通过工具调用参数返回结构化数据。 4. LangChain负责解析并验证结果。 5. 支持错误处理和重试。

### 7.3 两种策略的区别

  对比                     ProviderStrategy           ToolStrategy
------------------------ -------------------------- --------------------
  实现方式                 模型提供商原生能力         Tool Calling
  是否依赖原生结构化输出   是                         否
  是否需要支持工具调用     不一定                     需要
  适用范围                 支持原生结构化输出的模型   支持工具调用的模型

### 7.4 自动选择策略

开发时不一定需要手动指定 ProviderStrategy 或 ToolStrategy，可以直接传入
Schema：

``` python
agent = create_agent(
    model="openai:YOUR_MODEL_NAME",
    tools=[],
    response_format=ProductInfo
)
```

LangChain 会根据模型能力自动选择合适的策略。官方文档介绍的规则是： -模型支持原生结构化输出时，优先使用 ProviderStrategy。 - 不支持时，使用ToolStrategy。

从 LangChain 1.1 开始，相关模型能力可以通过 Model Profile 动态读取。

如果传入的是 JSON Schema 字典，应显式使用 `ProviderStrategy` 或`ToolStrategy` 包装，不能直接将 JSON Schema 字典作为 `response_format`传入。具体行为请以当前版本官方文档为准。

------------------------------------------------------------------------

## 八、ToolStrategy 的高级参数

ToolStrategy 除了可以定义 Schema，还提供额外配置参数，主要包括： -`tool_message_content` - `handle_errors`

### 8.1 tool_message_content

这个参数用于自定义结构化输出成功后，写入对话历史的 ToolMessage 内容。

``` python
response_format = ToolStrategy(
    schema=ProductInfo,
    tool_message_content="商品信息提取成功"
)
```

它修改的是对话历史中的工具消息内容，不是修改最终的 `structured_response`数据结构。

### 8.2 handle_errors

模型生成结构化数据时，也可能出现错误。

例如：

``` python
class ProductRating(BaseModel):
    rating: int = Field(
        ge=1,
        le=5,
        description="商品评分，范围为1到5"
    )
    comment: str
```

但是模型返回：

``` python
{
    "rating": 10,
    "comment": "商品不错"
}
```

由于评分超过允许范围，所以 Pydantic 验证失败。

ToolStrategy 提供 `handle_errors` 来处理这种情况。默认值通常为`True`，表示发生结构化输出错误时，LangChain会将错误反馈给模型，并要求模型重新生成。

#### handle_errors 支持的配置

  配置             行为
---------------- ----------------------------
  `True`           捕获错误，使用默认错误提示
  `False`          不处理错误，直接抛出
  字符串           使用自定义错误提示
  Exception 类型   只处理指定类型的异常
  Exception 元组   处理指定的多种异常
  Callable         使用自定义函数生成错误提示

默认错误处理：

``` python
response_format = ToolStrategy(
    schema=ProductRating,
    handle_errors=True
)
```

自定义错误提示：

``` python
response_format = ToolStrategy(
    schema=ProductRating,
    handle_errors="请提供1到5之间的评分，并填写评论内容。"
)
```

只处理指定异常：

``` python
response_format = ToolStrategy(
    schema=ProductRating,
    handle_errors=ValueError
)
```

自定义异常处理函数：

``` python
def custom_error_handler(error: Exception) -> str:
    return f"结构化输出发生错误：{str(error)}"


response_format = ToolStrategy(
    schema=ProductRating,
    handle_errors=custom_error_handler
)
```

这种方式适合需要统一管理错误提示的业务场景。

------------------------------------------------------------------------

## 九、Union：支持多种结构化输出类型

在实际业务中，模型返回的数据可能不只有一种类型。例如，需要识别用户提交的内容：如果是商品评价，就提取商品评价信息；如果是用户投诉，就提取投诉信息。

可以使用 Union 定义多种可能的 Schema。

``` python
from typing import Literal, Union
from pydantic import BaseModel, Field


class ProductReview(BaseModel):
    rating: int | None = Field(
        default=None,
        description="商品评分"
    )
    sentiment: Literal["positive", "negative"]
    key_points: list[str]


class CustomerComplaint(BaseModel):
    issue_type: Literal[
        "product",
        "service",
        "shipping",
        "billing"
    ]
    severity: Literal["low", "medium", "high"]
    description: str
```

然后：

``` python
from langchain.agents import create_agent
from langchain.agents.structured_output import ToolStrategy


agent = create_agent(
    model="openai:YOUR_MODEL_NAME",
    tools=[],
    response_format=ToolStrategy(
        Union[
            ProductReview,
            CustomerComplaint
        ]
    )
)
```

用户输入商品评价时，模型可能返回：

``` python
ProductReview(
    rating=None,
    sentiment="positive",
    key_points=["性能不错", "价格较贵"]
)
```

用户输入投诉时，模型可能返回：

``` python
CustomerComplaint(
    issue_type="shipping",
    severity="high",
    description="订单长时间未发货，客服没有及时回复"
)
```

Union
的作用是让模型在多个预定义的数据结构中选择一个合适的结构。它适合意图识别、多类型信息抽取、客服工单分类等场景。

------------------------------------------------------------------------

## 十、结构化输出的错误处理机制

结构化输出虽然可以约束模型的数据格式，但并不意味着完全不会出现错误。常见问题包括： 1.缺少必需字段。 2. 字段类型错误。 3. 字段值不符合约束。 4.模型返回多个结构化结果。 5. 模型拒绝执行请求。

### 10.1 Schema 验证错误

例如：

``` python
class ProductRating(BaseModel):
    rating: int = Field(ge=1, le=5)
```

模型返回：

``` python
{
    "rating": 10
}
```

验证失败。如果使用
ToolStrategy，默认会将验证错误反馈给模型，要求模型重新生成。

``` text
模型生成结构化数据
   |
   v
rating = 10
   |
   v
Pydantic 验证
   |
   v
发现评分超过 5，验证失败
   |
   v
LangChain 将错误反馈给模型
   |
   v
模型根据错误提示重新生成
   |
   v
再次验证
   |
   v
验证成功后返回结构化数据
```

这种自动重试是 Agent 的 ToolStrategy
错误处理机制，并不是所有结构化输出方式都具有完全相同的重试行为。

### 10.2 MultipleStructuredOutputsError

当使用 Union 时，模型理论上应该选择其中一种Schema，但模型可能错误地同时返回多个结构化结果。这时会产生`MultipleStructuredOutputsError`。ToolStrategy可以向模型反馈错误，让模型重新选择一个合适的结果。

### 10.3 ModelRefusalError

模型也可能拒绝执行请求，这种情况下模型可能不会返回结构化数据。需要注意： - 使用 ProviderStrategy 时，拒绝可能表现为
`StructuredOutputValidationError`。 - 使用 ToolStrategy 时，Agent
可能结束执行，但没有 `structured_response`。 - 官方还提供了
`ModelRefusalMiddleware`，可以统一将拒绝行为转换为 `ModelRefusalError`。

生产环境中不能默认认为 `result["structured_response"]`
一定存在，应该考虑异常处理和拒绝处理。

------------------------------------------------------------------------

## 十一、完整案例：商品评价分析系统

需求：用户输入一段商品评价，模型自动提取商品评分、情感倾向、商品优点、商品缺点和总结。

### 11.1 定义 Schema

``` python
from pydantic import BaseModel, Field
from typing import Literal


class ProductReview(BaseModel):
    """商品评价分析结果"""

    rating: int | None = Field(
        default=None,
        description="商品评分，范围1到5"
    )

    sentiment: Literal[
        "positive",
        "negative",
        "neutral"
    ] = Field(
        description="评价的情感倾向"
    )

    advantages: list[str] = Field(
        description="商品的优点"
    )

    disadvantages: list[str] = Field(
        description="商品的缺点"
    )

    summary: str = Field(
        description="对商品评价进行简短总结"
    )
```

### 11.2 初始化模型

``` python
from langchain.chat_models import init_chat_model


model = init_chat_model(
    "openai:YOUR_MODEL_NAME",
    temperature=0
)
```

### 11.3 创建结构化模型

``` python
structured_model = model.with_structured_output(
    ProductReview
)
```

### 11.4 调用模型

``` python
review = """
这个手机使用起来非常流畅，拍照效果也不错，
但是价格比较贵，而且电池续航一般。
"""

result = structured_model.invoke(
    f"请分析以下商品评价：\n{review}"
)
```

### 11.5 获取结果

``` python
print(result)
```

返回结果示例：

``` python
ProductReview(
    rating=None,
    sentiment="positive",
    advantages=["运行流畅", "拍照效果不错"],
    disadvantages=["价格较贵", "电池续航一般"],
    summary="整体评价偏正面，性能和拍照表现较好，但价格和续航存在不足。"
)
```

如果需要将结果存入数据库：

``` python
data = result.model_dump()

# 保存到数据库
# product_review_repository.save(data)
```

整个过程不需要自己编写 JSON 解析逻辑。

------------------------------------------------------------------------

## 十二、结构化输出与普通 JSON 输出有什么区别？

Prompt 也可以要求模型返回 JSON：

``` python
response = model.invoke(
    "请以JSON格式返回用户姓名和年龄"
)
```

模型可能返回：

``` json
{
  "name": "张三",
  "age": 25
}
```

然后通过：

``` python
import json

data = json.loads(response.content)
```

也可以得到字典。

它们的区别：

  对比           Prompt + JSON            Structured Output
-------------- ------------------------ ------------------------------
  输出格式要求   通过 Prompt 描述         通过 Schema 定义
  格式约束       主要依赖模型遵循指令     可利用模型原生能力或工具调用
  数据验证       需要自己实现             Pydantic 可自动验证
  类型约束       需要自己检查             Schema 可声明类型
  字段约束       需要额外实现             可通过 Pydantic 定义
  开发复杂度     需要自行处理解析和验证   LangChain 提供统一接口

普通 JSON 输出并不是一定不可靠。如果模型支持 JSON
Mode，也可以保证输出是合法 JSON。但 JSON 合法不代表字段一定符合业务
Schema。结构化输出进一步解决了这个问题。

------------------------------------------------------------------------

## 十三、实际开发中的最佳实践

### 13.1 优先使用 Pydantic

如果使用 Python 开发，推荐优先考虑 Pydantic，原因： - 定义结构简单。 -支持字段描述。 - 支持嵌套结构。 -支持数据验证。 - 支持转换成 dict 和JSON。 - 与 LangChain 集成良好。

### 13.2 尽量提供清晰的字段描述

不推荐：

``` python
class User(BaseModel):
    status: str
```

推荐：

``` python
from typing import Literal
from pydantic import BaseModel, Field


class User(BaseModel):
    status: Literal[
        "active",
        "inactive",
        "disabled"
    ] = Field(
        description="用户状态，只能从指定枚举值中选择"
    )
```

字段描述越清晰，模型越容易理解输出要求。对于枚举值，尽量使用 `Literal`限定范围。

### 13.3 合理使用 Optional

不是所有信息都能从用户输入中提取。

``` python
class User(BaseModel):
    name: str
    age: int | None = Field(
        default=None,
        description="用户年龄，如果没有提供则返回None"
    )
```

这样可以避免模型为了填满字段而随意编造信息。

注意：字段允许为空和字段是否必填是两个不同概念。

### 13.4 结构化输出不等于业务验证

例如：

``` python
class Order(BaseModel):
    amount: float
    status: str
```

即使模型返回的数据满足类型要求，也不代表订单真实存在。实际业务中还需要进行： -数据库校验。 - 权限校验。 - 业务规则校验。 - 数据一致性校验。不要把模型生成的数据直接当成可信的业务数据。

### 13.5 注意不同模型的能力差异

不同模型提供商对结构化输出的支持程度不同。有的模型支持原生 JSONSchema，有的模型主要依赖 Tool Calling，有的模型只支持 JSON Mode。更换模型时，需要检查： - 是否支持 `with_structured_output()`。 -
是否支持指定的 `method`。 - 是否支持严格 Schema 约束。 -是否支持嵌套结构。 - 是否支持对应的字段类型。

------------------------------------------------------------------------

## 十四、核心 API 总结

----------------------------------------------------------------------------------
  API / 类                     所属模块                 作用
---------------------------- ------------------------ ----------------------------
  `with_structured_output()`   Chat Model               将模型转换为结构化输出模型

  `create_agent()`             Agents                   创建 Agent

  `response_format`            create_agent             指定 Agent 的结构化输出格式

  `ProviderStrategy`           structured_output        使用提供商原生结构化输出

  `ToolStrategy`               structured_output        使用工具调用实现结构化输出

  `BaseModel`                  Pydantic                 定义数据模型

  `Field`                      Pydantic                 定义字段描述及约束

  `TypedDict`                  Python                   定义字典类型结构

  `dataclass`                  Python                   定义数据类

  `Union`                      typing                   定义多种可能的数据结构

  `include_raw`                with_structured_output   同时返回原始响应

  `tool_message_content`       ToolStrategy             自定义结构化输出对应的
                                                        ToolMessage

  `handle_errors`              ToolStrategy             配置结构化输出错误处理

## 十五、知识点总结

### Structured Output 知识体系

``` text
Structured Output
│
├── Schema 定义
│   ├── Pydantic
│   ├── TypedDict
│   ├── Dataclass
│   └── JSON Schema
│
├── Chat Model
│   ├── with_structured_output()
│   └── include_raw
│
├── 实现策略
│   ├── JSON Schema
│   ├── Function Calling
│   └── JSON Mode
│
├── Agent
│   ├── response_format
│   ├── ProviderStrategy
│   └── ToolStrategy
│
└── 错误处理与数据验证
    ├── Pydantic Validation
    ├── handle_errors
    └── ModelRefusalError
```

最重要的是理解以下三个问题：

**1. 为什么需要结构化输出？**

因为自然语言不适合直接作为程序的数据接口。结构化输出可以让模型返回程序容易处理的数据。

**2. with_structured_output() 和 response_format 有什么区别？**

前者用于直接调用 Chat Model，后者用于在 Agent 中定义结构化输出要求。

**3. ProviderStrategy 和 ToolStrategy 有什么区别？**

前者利用模型提供商原生结构化输出能力，后者利用模型的工具调用能力实现结构化输出。



## 版本与兼容性说明

LangChain 的结构化输出 API 会随着版本迭代而变化。本文以 LangChain 1.x官方文档组织知识点，示例中的 `YOUR_MODEL_NAME`需要替换成当前环境支持的模型标识。实际项目中，请重点核对： - 当前安装的 `langchain` 版本。 -对应模型集成是否实现 `with_structured_output()`。 -模型提供商支持哪些结构化输出方法。 - `ProviderStrategy`、`ToolStrategy`及其参数在当前版本中的具体行为。官方文档优先于本文示例代码；模型名称、参数支持和部分错误类型可能因版本及提供商而异。
