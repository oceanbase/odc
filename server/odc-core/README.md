# odc-core

odc-core 模块是 ODC 的核心模块，其中包含了 ODC 核心功能的实现。通常来说，代码如果满足以下要求即可放到此 Module 中：

1. 是一个独立的功能模块。
2. 不和 ODC 的具体业务直接相关。
3. 和 spring 的上下文无关。

## shared 业务相关

业务相关的工具类和常量

### 子 package 划分规划

- constant 常量，可以包括
    - 整形、字符串，定义在 interface class 中
    - 枚举值常量
- exception 异常

### 资源文件说明

- `i18n` 国际化文案资源，详见 `resources/i18n/README.md`
- `migrate_schema_history_table_template.sql` 迁移框架历史记录表模板

### 设计思考

#### constant 放到 shared 的原因

部分 constant 可能会被不同层级的类型引用，比如 DAO 层的 entity 类、service 层的 model 类、controller 层的 dto 类可能引用相同的枚举值。
如果这些常量在每一层都定义，会造成定义重复，更有可能导致定义不一致，所以把共享的常量统一放到 shared.constant package 下。

#### 错误码设计思考

- 错误码是否需要使用数字？站在调用端的角度来看待，字符串更容易理解，数字并没有实际的作用，去掉数字反而简化了代码
- Top Level 的错误码不宜过多，调用端只需要了解少量固定的错误码就可以进行错误处理
- 详细的错误信息可以包含到 nested 对象里

#### 异常设计思考

- OdcException 作为 base exception，用于体现这是 ODC 产生的异常
- 对于后台任务而言，在异常处理逻辑中，普遍的处理方式是重试，但是并不是所以异常都可以重试的，TransientException 可重试，NonTransientException 不可重试
- 对于体现在 RESTful API response 的异常
    - 提供 httpStatus 字段
    - 命名尽可能 HTTP status code 规范保持一致 比如 NotFoundException 对应 404 Not Found
    - 如果有更符合业界通用命名则采用，比如 使用 AccessDenyException 而不是 ForbiddenException 对应 403 Forbidden

## 数据库连接会话管理模块

## SQL 执行模块

## 查询缓存模块

## 异步任务执行模块

## 安全框架模块

## 流程引擎模块

## 对象存储模块

## 数据迁移模块