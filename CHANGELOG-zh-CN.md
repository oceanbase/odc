# OceanBase Developer Center (ODC) CHANGELOG
## 4.3.3 (2025-01-13)

### 功能变化

数据生命周期管理

- 新增 Oracle 到对象存储的归档链路
- 新增 MySQL 到对象存储的归档链路
- 新增 OceanBase MySQL 到对象存储的归档链路
- 新增 OceanBase Oracle 到对象存储的归档链路
- 新增 PostgreSQL 到对象存储的归档链路
- 支持回溯编辑历史，支持查看编辑内容前后对比
- 支持定义动态目标表，解决按日、月等单独存放历史数据的诉求
- 支持删除数据归档、清理任务，当任务已完成或已终止时支持对其进行删除操作
- 优化回滚逻辑，仅回滚当次任务的归档数据

无锁结构变更

- 支持失败重试，为各环节可能导致失败的场景补充重试逻辑
- 支持无锁结构变更状态展示，可以查看运行中任务进度

变更风险管控

- 增加全局项目角色，包括全局项目管理员、全局安全管理员以及全局 DBA
- 增加项目归档检测机制，归档前会检测项目中是否存在未结束的工单及周期任务
- 支持删除项目，对于已归档的项目支持对其进行删除操作
- 支持用户申请视图权限，对用户访问视图做了更细粒度的权限控制
- SQL 窗口拓展了可执行的 SQL 类型，新支持 `call`、`comment`、`set session` 等类型
- SQL 检查规范支持原生 Oracle 数据源
- 支持原生 Oracle 数据源的变更走变更审批流程
- 新增 2 条 SQL 检查规则，支持规范 `create like` 及 `create as` 建表语句

SQL 开发

- 支持 OceanBase 外表白屏化管理
- 支持 OceanBase 分区表的二级分区展示
- 支持编辑 OceanBase MySQL 模式的函数和存储过程
- 支持通过 OBProxy 进行 PL 调试

其他

- 支持 SAML 的单点登录方式
- 支持查杀原生 Oracle 数据源的会话
- 适配 OceanBase 4.2.5、4.3.3 版本
- 适配 OBKV SQL 模式
- 启用 Secure Cookie 机制，加固数据传输安全
- 平台表单（含工单列表、数据库列表）列宽支持拉伸

### 易用性改进
- 支持固化项目搜索条件，避免频繁搜索高频操作项目
- 支持用户登出再登入后仍旧可以定位在最近使用的项目下，简化用户操作路径
- 风险识别规则中判断条件文案优化，统一采用运算符及英文表达，以避免歧义
- 优化连接保活逻辑，每3分钟会主动发送一次数据库请求，保障连接的稳定性
- 项目外工单模块增加项目列，方便用户快速识别工单所属项目
- 除逻辑库变更, 分区计划, 影子表外，所有工单类型支持再次发起功能，再次发起后支持二次编辑工单参数
- 工单可被管理及查看范围调整，管理员和 DBA 可管理项目内所有工单，其它角色仅可管理自己发起的工单。同时项目内所有成员均可查看项目内所有工单


### 缺陷修复

数据源

- 堡垒机集成场景不会同步 `information_schema` 等内置数据库到项目内
- 数据库同步异常挂起时无法恢复

工单

- 创建数据归档工单在个人空间仍产生审批流程
- 数据归档/清理任务执行成功但执行记录状态异常
- 非当前账号创建结构对比任务无法正常执行
- Oracle 导出表结构存在虚拟列时导出会失败
- OceanBase MySQL 源端库或目标库里若有一张表的 DDL 里指定全文索引的分词器，结构比对任务失败
- 定时任务如果有太多的子任务，查看操作记录失败问题
- 导出任务保留当前配置不生效

变更管控

- 没有导出权限也能导出视图
- 分区计划无法禁用导致无法归档项目

SQL 开发

- SQL Check 特定场景下产生 NPE 异常
- DROP PL 需要数据库变更的权限
- 函数返回值类型为 Year 时无法正常显示
- 当 PL 名称包含 @ 时 create 和 drop 语句将失败
- 查看原生 Oracle 扩展了统计信息（`DBMS_STATS.CREATE_EXTENDED_STATS`）的表详情失败
- 限制 SQL 影响的行数时，insert 语句不生效
- 导出数组函数结果集时，空指针异常问题
- 在 Chrome 118 版本的浏览器中，右键单击软件包子程序时没有运行按钮
- 查看程序包包头中的子程序时报错

其他
- 用户再次进入 ODC 时没有打开上次使用的项目

## 4.3.2 (2024-09-27)

### 功能变化

逻辑库管理

- 新增逻辑库管理功能，可选择物理库配置为逻辑库，选择物理库时会根据选择的第一个物理库自动匹配其他物理库
- 支持根据物理库中物理表的拓扑分布自动提取已经存在的逻辑表
- 可通过表达式创建各类场景的逻辑表，表达式支持按步长递增、均分、重复、枚举等语法，覆盖 拆库、拆表、拆库拆表 等场景
- 新增逻辑库 DDL 变更任务，变更任务会根据物理库的分布实现智能任务并行，变更步骤逐语句执行简化异常处理
- 支持逻辑表结构一致性检查，及时发现结构不一致的物理表
- 现有的数据库、表级别权限管控同样适用于逻辑库，当获得逻辑库访问权限时自动授予相关物理表的权限

变更风险管控

- SQL 检查规则新增 SQL 影响行数检查，支持 OceanBase MySQL 和 MySQL 数据源

数据生命周期管理

- 新增 Oracle 到 OceanBase Oracle 的归档链路
- 新增 Oracle 到 Oracle 的归档链路
- 新增 OceanBase Oracle 到 Oracle 的归档链路
- 新增 PostgreSQL 到 OceanBase MySQL 的归档链路
- 新增 Oracle 数据清理
- 新增 PostgreSQL 清理

导入导出

- 桌面版支持导入时直接选择文件夹导入

分区计划

- 针对日期类型字段分区设置，新增分区名和分区内容范围一致的分区命名选项

外部集成

- SSO 集成支持 Azure AD


### 易用性改进

- SQL 窗口内提供返回首页快捷键
- SQL 窗口内被定位的数据库会自动展示在可视区内
- SQL 结果集页签展示 DB 耗时
- SQL 窗口资源树按照名称排序（数据源、项目、数据库、表、视图等）
- SQL 窗口资源树上的数据库支持右键快速发起所有类型的工单
- 数据源详情页，支持按照是否可用、是否已分配项目筛选数据库列表
- 数据源连接初始化脚本功能提供常用脚本的引导
- 统一了所有功能（数据源、项目、脚本、资源树等）空数据状态的交互
- 统一优化所有功能（数据库运维、敏感列扫描、项目新增数据库）的数据源选择交互
- 部分表单支持鼠标悬停展示列的内容详情（工单列表、数据库列表、用户列表等）
- 意见反馈页增加 github issue 地址
- 更新用户登录密码的强度要求为：长度 8~32 位, 包含以下四种类型字符至少三种及以上：数字（0~9）、大写字母（A~Z）、小写字母(a~z)、特殊符号：全部的英文特殊字符


### 缺陷修复

变更管控

- 结构对比任务生成的变更 SQL 里，`tinyint` 列类型（MySQL 数据源）的精度不正确
- 分区名被误识别为表名，导致报没有对应表权限
- `rename table old_table to new_table` 中的 new_table 被误判断为需要 new_table 的表权限
- MySQL 5.7 版本模拟数据任务可能失败
- 点击手动执行后，手动执行的按钮依然处于可点击状态
- 风险识别规则过多时，创建或者更新会失败
- 周期任务允许设置小于十分钟的调度周期

导入导出

- 将 PL 和 表 的 DDL 合并成一个 SQL 文件导出后，再次导入时会由于分句错误导致失败
- Windows 桌面版可能由于路径问题导致导出失败
- OceanBase Oracle 模式导出结果集，如果表名为小写，导出文件为空


数据归档

- 修改数据归档限流参数可能报错

数据源

- 测试 OceanBase SYS 租户数据源，选择了 OceanBase Oracle 类型，依然可以测试连接成功

其它

- 在部署了负载均衡服务的场景下，SSO 登录可能失败
- 启动参数 `ODC_APP_EXTRA_ARGS` 无效


## 4.3.1 (2024-07-31)

### 功能变化

变更风险管控

- 新增表级别权限管控，允许项目成员对不同表拥有不同操作权限，包括对表的查询、变更和导出操作，进一步加强管控协同能力

会话管理

- 关闭会话/查询支持非直连 OBServer 的更多场景
  - 当连接为 OceanBase 4.2.3 和 obproxy 4.2.5 或更高的版本时，使用 Client session 能力进行会话管理
  - 在 OceanBase 4.2.1 oracle 模式下，使用匿名块进行会话管理

AP 开发

- 新增实时执行剖析，可视化、交互式呈现 sql_plan_monitor
  - 此功能需要数据源版本 OceanBase 4.2.4+
  - 不仅支持对已完成的 SQL 执行进行分析，也支持对执行中的 SQL 进行实时分析
  - 提供执行计划的图形视图、表格视图和文本视图，直观展示算子之间的连接关系和步骤顺序
  - 全局视图提供 Top5 耗时算子的排序和各项执行耗时阶段的全局汇总，快速定位性能瓶颈
  - 算子节点包含算子的执行状态和详情信息，详细信息包括 CPU、内存、磁盘、吐行和节点属性
  - 对于并行执行节点，支持按照 DB 耗时、IO 内存和吐行行数排序，快速定位数据倾斜，不仅支持分析单机执行计划也支持分析分布式执行计划
  - 全新设计的一体化实时诊断页面，可以结合执行计划、全链路诊断，在一个页面完成执行剖析

SQL 开发

- SQL 执行时支持查看执行进度，包括：总执行条数、当前执行条数及当前正在执行 SQL 的 trace id；支持实时查看已完成的执行结果
- 支持以图形格式查看 OceanBase 的逻辑 SQL 执行计划

数据源

- 全功能适配 OB 4.2.4、OB 4.3.1、OB 4.3.2


### 易用性改进

- 数据清理任务支持编辑任务配置
- 数据源模块，批量导入支持 mysql、oracle 和 doris 数据源

### 缺陷修复

数据生命周期管理
- 未启用表结构同步时也进行了表结构比较 [#3014](https://github.com/oceanbase/odc/pull/3014)                                                                                                            

变更风险管控
- 自动授权规则对 LoginSuccess 事件不生效 [#3003](https://github.com/oceanbase/odc/pull/3003)

导入导出
- 桌面版模式下，重新安装 ODC 场景，导入任务可能会受到历史执行任务导入文件影响 [#3006](https://github.com/oceanbase/odc/pull/3006)

SQL 检查

- 启用 SQL 窗口规范时，SQL 窗口的提交、回滚按钮可能会失效 [#2985](https://github.com/oceanbase/odc/pull/2985)

SQL 开发
- PL 调试期间可能会报 NPE 异常 [#2930](https://github.com/oceanbase/odc/pull/2930)
- oracle 数据源修改会话变量的 sql 错误 [#2872](https://github.com/oceanbase/odc/pull/2872)

模拟数据
- 模拟数据任务无法终止 [#2850](https://github.com/oceanbase/odc/pull/2850)

全局对象检索
- 对象同步无法停止 [#2928](https://github.com/obase/odc/pull/2928)

工单
- 检查结果文件不存在本机时，无法获取 SQL 检查的结果 [#2943](https://github.com/oceanbase/odc/pull/2943)

审计
- `content_ip_address`列值的实际长度超过了该列的长度限制 [#2863](https://github.com/oceanbase/odc/pull/2863)

其他
- 多节点部署使用进程模式调度任务时，所有任务会被调度到同一个节点 [#2408](https://github.com/oceanbase/odc/pull/2408)


## 4.3.0_bp1（2024-06-24）

### 易用性改进

- 优化多库变更产生的子任务的描述信息，突出批次号及所属的多库变更任务 [#2762](https://github.com/oceanbase/odc/pull/2762)
- 允许计划任务的创建者修改计划任务 [#2772](https://github.com/oceanbase/odc/pull/2772)
- 消息通知支持配置网络超时 [#2782](https://github.com/oceanbase/odc/pull/2782)

### 缺陷修复

SQL 检查

- 禁止将"id"作为列名 [#2796](https://github.com/oceanbase/odc/pull/2796)

导入导出

- 导入空数据文件将会导致导数任务失败 [#2779](https://github.com/oceanbase/odc/pull/2779)

数据库对象管理

- 使用非 4.3.0 版本的 ODC 打开 4.3.x 版本的 OceanBase 的表对象可能会引发空指针异常 [#2776](https://github.com/oceanbase/odc/pull/2776)

SQL 开发

- SQL 语句的`like`及`replace`字句中包含反斜杠时将会引发格式化错误
- SQL 确认界面格式化按钮不生效

数据源

- 批量导入数据源时未能全部展示待导入的数据源

## 4.3.0 （2024-06-07）

### 功能变化

表对象管理

- 支持 OceanBase Oracle 模式 GIS 数据类型
- 支持 OceanBase v4.3.0 列存，包括表的存储模式和索引的存储模式

变更风险管控

- 新增多库变更任务，相比数据库变更任务，多库变更支持在发起任务时配置变更流水线，流水线支持多个批次，每个批次支持多个库，流水线可以保存为变更顺序模板。
- 新增数据库管理员，项目管理员可以为项目内的库配置库管理员，库管理员可以在工单审批节点中引用，库管理员信息也会包含在 WebHook 事件中用于外部审批集成。
- 发起工单时支持配置为手动执行，避免审批通过任务自动执行发生在不符合预期的时间点。

数据归档/清理

- 数据清理/归档提供常用的任务指标，包括：开始时间、结束时间、过滤条件、处理行数、扫描行数及实时性能
- 数据清理/归档支持分区条件
- 数据清理/归档支持配置执行超时时间，执行耗时达到超时时间设定任务会退出，保障业务高峰数据库性能
- 数据归档支持增量结构同步，当源表的结构发生变化时，任务会自动同步表结构，启用结构同步时可自定义是否同步分区和索引
- 数据清理/归档实现了 OceanBase MySQL 模式字段类型兼容，新增支持等字段类型: bit、set、enum 以及空间数据类型
- 数据清理/归档实现了 OceanBase Oracle 模式字段类型兼容，新增支持等字段类型: BINARY_FLOAT、BINARY_DOUBLE、TIMESTAMP WITH TIME ZONE、TIMESTAMP WITH LOCAL TIME ZONE、INTERVAL YEAR TO MONTH、INTERVAL DAY TO SECOND、ROW 、 ROWID、UROWID、BLOB
- 数据清理支持联动历史库校验

全局对象检索
- 新增全局对象检索，支持在项目范围内全局检索，库表再多也能瞬间直达
- 支持库、表、列、视图、函数、存储过程、程序包、触发器 等几乎全部对象
- 支持快捷键 Ctrl/Cmd+J 快速唤起全局对象检索


### 易用性改进

- 数据源配置中的 JDBC 参数和初始化脚本也会应用于导入导出任务，给导入导出任务提供更多灵活性 [#2587](https://github.com/oceanbase/odc/pull/2587)
- 归档项目时，将检查定时任务是否全部关闭 [#2562](https://github.com/oceanbase/odc/pull/2562)
- 优化了查询表详情的请求时间 [#2626](https://github.com/oceanbase/odc/pull/2626)
- 优化流程任务取消的错误提示 [#2624](https://github.com/oceanbase/odc/pull/2624)

### 缺陷修复

数据源

- 连接备集群查询表结构失败 [#2648](https://github.com/oceanbase/odc/pull/2648)
- 重置连接时的并发异常 [#2528](https://github.com/oceanbase/odc/pull/2528)

PL 对象管理

- 修存储过程和函数列表不按名称排序 [#2636](https://github.com/oceanbase/odc/pull/2636)
- OceanBase Oracle 模式批量编译失败的问题 [#2606](https://github.com/oceanbase/odc/pull/2606)

SQL 窗口

- SQL 窗口中数据库超 2000 时无法切换 [#2520](https://github.com/oceanbase/odc/pull/2520)
- SQL 窗口设置执行失败时不继续执行不生效 [#2259](https://github.com/oceanbase/odc/pull/2259)
- 使用 obclient 关闭连接后 SQL 窗口执行报错 [#2528](https://github.com/oceanbase/odc/pull/2528)
- ORACLE 数据源无法在 SQL 窗口中设置 nls 参数 [#2501](https://github.com/oceanbase/odc/pull/2501)

数据归档/清理

- 关闭任务框架时，消息通知失效 [#2445](https://github.com/oceanbase/odc/pull/2445)

项目和工单

- 项目编辑时报错项目已经存在 [#2642](https://github.com/oceanbase/odc/pull/2642)
- 工单描述的国际化失效 [#2579](https://github.com/oceanbase/odc/pull/2579)
- 审批内容超长时审批失败 [#2565](https://github.com/oceanbase/odc/pull/2565)

结构比对

- 结构比对，目标表不存在时结果异常 [#2638](https://github.com/oceanbase/odc/pull/2638)

导入导出

- 导入表结构设置成跳过时不生效 [#2587](https://github.com/oceanbase/odc/pull/2587)


表对象管理

- OceanBase 租户配置 lower_case_table_names=2 时，报错表对象不存在 [#2298](https://github.com/oceanbase/odc/pull/2298)
- 表对象信息中，分区表的唯一索引不可见 [#2297](https://github.com/oceanbase/odc/pull/2297)

其他

- swagger-ui.html 访问失败的问题 [#2160](https://github.com/oceanbase/odc/pull/2160)

### 安全加固

- 升级 spring-security 组件到 5.7.12 版本 [#2690](https://github.com/oceanbase/odc/pull/2690)

## 4.2.4_bp2（2024-05-14）

### 缺陷修复

数据源

- 无法连接 ODP-Sharding 数据源 [#2339](https://github.com/oceanbase/odc/pull/2339)

用户管理

- 无法删除已归档项目的 OWNER 或 DBA 用户 [#2359](https://github.com/oceanbase/odc/pull/2359)

影子表同步

- 列存在默认值时生成的语句可能出现语法错误 [#2388](https://github.com/oceanbase/odc/pull/2388)

数据脱敏

- 存在重名的无效数据库时，脱敏失效 [#2385](https://github.com/oceanbase/odc/pull/2385)

命令行窗口

- 输入较长 SQL 时，无法完全回显语句 [#2353](https://github.com/oceanbase/odc/pull/2353)

数据库归档/清理

- 任务被终止时状态可能被错误地设置为成功 [#2340](https://github.com/oceanbase/odc/pull/2340)
- 数据归档指定非同名目标表时，任务执行报错表不存在 [#2313](https://github.com/oceanbase/odc/pull/2313)

结果集导出

- 数据库访问较慢时导出结果集会因为超时而失败 [#2315](https://github.com/oceanbase/odc/pull/2315)

消息通知

- 编辑消息通道会导致签名密钥丢失 [#2314](https://github.com/oceanbase/odc/pull/2314)

分区计划

- 在 OceanBase 4.x 版本下漏删分区以及少建分区 [#2327](https://github.com/oceanbase/odc/pull/2327)
- 无法在 OceanBase 3.x 以下的版本发起分区计划任务 [#2323](https://github.com/oceanbase/odc/pull/2323)
- 分区计划详情中不显示分区间隔

系统集成

- 第三方用户集成中，用户如果修改了 extra_properties 将无法再登录 ODC [#2336](https://github.com/oceanbase/odc/pull/2336)
- 同时使用 OAuth 和堡垒机集成时无法打开 SQL 控制台 [#2253](https://github.com/oceanbase/odc/pull/2253)

其他

- 生命周期横跨发布过程的工单无法正常推进 [#2065](https://github.com/oceanbase/odc/pull/2065)
- 无法手动执行或终止任务 [#2272](https://github.com/oceanbase/odc/pull/2272)
- 运行一段时间之后无法使用帐密登陆需要重启 ODCServer 才能恢复 [#2389](https://github.com/oceanbase/odc/pull/2389)
- Basic 认证方式调用 API 和 CSRF 防护存在冲突 [#2370](https://github.com/oceanbase/odc/pull/2370)

### 易用性改进

- 分区计划针对没有分区预创建语句或分区删除语句生成的情况下增加文案提示 [#2351](https://github.com/oceanbase/odc/pull/2351)
- OceanBase 4.2 之前的版本禁用全链路诊断 [#2219](https://github.com/oceanbase/odc/pull/2219)

### 依赖库升级

- 升级 data-lifecycle-manager 版本至 1.1.1 [#2281](https://github.com/oceanbase/odc/pull/2281)

### 安全加固

- 移除 snappy-java 依赖 [#2317](https://github.com/oceanbase/odc/pull/2317)
- 数据脱敏增加校验，避免因为 BigDecimal 导致的 DDos 风险 [#2271](https://github.com/oceanbase/odc/pull/2271)

## 4.2.4_bp1（2024-04-12）

### 缺陷修复

PL 调试

- PL 调试超时参数无法通过连接初始化脚本设置 [#2179](https://github.com/oceanbase/odc/pull/2179)

其他

- 存在分区计划历史任务时 ODC Server 启动失败 [#2158](https://github.com/oceanbase/odc/pull/2158)

### 安全加固

- 升级 okio-jvm 版本至 3.4.0 [#2200](https://github.com/oceanbase/odc/pull/2200)

## 4.2.4 （2024-04-03）

### 功能变化

数据源

- OceanBase 增加 4.2.2 版本适配
- 新增 Oracle 数据源, 支持 SQL 开发、导入导出、数据脱敏、对象管理、变更审批
- 新增 Doris 数据源, 支持 SQL 开发、导入导出、数据脱敏、表对象管理、会话管理、命令行窗口、变更审批

SQL 开发

- 内置常用的运维代码片段 71 个，内置片段可以在 SQL 窗口中自动补全，并且会匹配数据库类型和版本
- SQL 自动补全支持补齐数据字典/性能视图
- Oracle 模式 schema 名、表名、列名大小写输入，行为和 PL/SQL Developer 一致

结构比对

- 新增结构比对功能，支持对同构数据库（OBOracle、OBMySQL、MySQL）进行结构比对
- 支持的范围为表对象，比对属性范围包含列、主键、约束、索引、分区、表属性
- 结构比对结果提供 DIFF 预览和变更脚本预览
- SQL 预览结果可下载，也可以直接发起结构同步任务

无锁结构变更

- 支持给表添加中间列
- 支持表结构变更时包含索引变更（OceanBase Oracle 模式）
- 表包含唯一性约束时，支持删除主键（OceanBase Oracle 模式）

分区计划

- 支持为 OBOracle 模式数据库配置分区计划
- 分区字段字段类型不仅支持 DATE/TIMESTAMP，还支持 NUMBER/CHAR/VARCHAR2 等任意字段类型
- 重新设计了分区计划的策略配置页面，自定义模式可以通过表达式配置任意分区上界计算逻辑，支持预览分区计划的执行 SQL
- 删除分区的执行周期支持和创建分区的执行周期独立配置

数据归档/清理

- 支持对 OBOracle 模式数据库配置清理任务
- 支持对 OBOracle -> OBOracle 链路配置归档任务
- 支持任务发起时，预览数据归档/清理任务实际执行的 SQL
- 数据归档支持自定义目标表名称
- 数据清理支持配置是否使用主键，不使用主键时会直接基于索引条件匹配删除数据，任务执行过程不需要按照主键进行任务分片，特定场景下可显著提升清理效率

安全规范

- 支持自定义环境
    - 可以根据自己的业务场景，通过自定义环境配置不同的 SQL 窗口规范和 SQL 检查规则
    - 创建自定义环境时，可以选择已有环境初始化配置
    - 支持配置标签样式，方便区分不同环境
- 新增 3 条 SQL 检查规则
    - 保留字不应作为对象名
    - 存在 offline (锁表) 结构变更语句，offline 结构变更会导致表锁定，影响业务
    - 存在 TRUNCATE 语句，生产环境 TRUNCATE 表风险较大
- 优化安全规范中风险识别规则默认值，生产环境可以开箱即用
- SQL 窗口规范增加生产环境开启 PL 调试风险提示

库级别访问权限控制

- 项目协同增加库访问权限控制
- 库访问权限类型包括查询、导出和变更，支持按类型授权，支持设置权限有效期
- 项目开发者默认有项目内全部库的访问权限，和之前版本行为保持一致
- 新增项目参与者角色，参与者默认没有任何库的访问权限
- 参与者可以通过工单申请库访问权限
- 管理员可以直接给参与者授权库访问权限
- 管理员可以撤销参与者的库访问权限

消息通知

- 项目协同增加消息通知功能
- 支持的事件类型包括工单审批状态变化、任务执行状态变化，以及任务调度失败
- 消息推送范围可通过规则配置，消息内容可通过模板配置
- 消息通道支持配置常用的 webhook 通道，比如钉钉、飞书、企业微信，也支持自定义 HTTP 请求，消息发送支持限流

系统集成

- SSO 增加 LDAP 协议支持

### 易用性改进

- 优化数据库选择组件，统一了各产品页面的库选择交互，增加了对项目名、数据源名和数据库名的模糊匹配功能
- 增加资源树定位键，SQL 窗口新加资源树定位键，便于快速找到当前数据库在资源树中的位置
- 升级偏好设置，将偏好设置提升为一级功能，直接通过【设置】入口访问，新增配置项包括
    - SQL 执行时是否开启全链路追踪
    - SQL 执行遇到错误是否继续
    - 自定义编辑器主题、字体大小
    - 配置编辑器运行 SQL 快捷键
    - 设置默认工作空间
    - 是否开启用户行为分析
    - 桌面版支持通过 JVM 参数配置内存大小
- 增加数据库可用性标识，项目下的数据库列表现在会显示不可用状态及原因
- 优化工单发起交互
    - 支持直接从资源树的数据库发起各类工单
    - 常用的任务工单（模拟数据、数据库变更、数据归档、数据清理、申请数据库权限）支持再次发起，发起时可编辑任务参数
- 数据库变更增加索引变更语句检测，如遇索引变更自动调整超时设置（120h），以避免索引变更语句执行因超时而失败
- 桌面版个人设置支持自定义 JVM 配置，内存占用可以控制到 1G 以内
- 桌面版支持导出大于 4G 的数据
- 优化了产品英文文案

### 缺陷修复

连接会话

- 连接会话过期后未能及时清除引用，导致资源泄漏，可能造成内存消耗上升 [#2125](https://github.com/oceanbase/odc/pull/2125)
- 高频使用场景下，执行 SQL 或查看表数据会遇到接口卡死无法响应的问题 [#1914](https://github.com/oceanbase/odc/pull/1914)
- 在数据源配置修改用户名大小写之后，连接 OceanBase Oracle 可能会出错 [#1797](https://github.com/oceanbase/odc/pull/1797)
- 打开 SQL 控制台时偶现 404 错误 [#1809](https://github.com/oceanbase/odc/pull/1809)

SQL 执行

- OceanBase v4.2 提交/回滚按钮状态与实际事务状态不同步 [#2097](https://github.com/oceanbase/odc/pull/2097)
- 带有单行注释的 SQL 语句执行失败 [#2085](https://github.com/oceanbase/odc/pull/2085)
- 最后一个 SQL 命令若不以分隔符结束将导致偏移量计算错误 [#1970](https://github.com/oceanbase/odc/pull/1970)
- DBMS 执行输出不正确，没有将空格完全输出 [#1051](https://github.com/oceanbase/odc/issues/1970)
- `#` `$` 在 SQL 窗口格式化后消失 [#1490](https://github.com/oceanbase/odc/issues/1490)
- MySQL 数据源 SQL 窗口自动补全不可用 [#1718](https://github.com/oceanbase/odc/issues/1718)

结果集

- 结果集中同时修改多行数据时耗时较久 [#2007](https://github.com/oceanbase/odc/pull/2007)
- OceanBase MySQL 模式下 DATETIME 数据类型显示时精度丢失 [#1411](https://github.com/oceanbase/odc/pull/1411)
- 查看 BLOB 字段文本和十六进制图像之间来回切换可能会导致界面冻结 [#300](https://github.com/oceanbase/odc/issues/300)

表对象

- 索引和约束视图中列名顺序不一致 [#1948](https://github.com/oceanbase/odc/pull/1948)
- 无法查看 MySQL v5.6 表详情 [#1635](https://github.com/oceanbase/odc/pull/1635)
- 无法查看 Sofa ODP 表详情 [#2043](https://github.com/oceanbase/odc/pull/2043)
- 表结构编辑无法把 NOT NULL 字段改为 NULL [#1441](https://github.com/oceanbase/odc/issues/1441)
- 分区表有多个最大值时只显示了一个最大值 [#1501](https://github.com/oceanbase/odc/issues/1501)
- 删除表主键的按钮被置灰无法点击 [#1874](https://github.com/oceanbase/odc/issues/1874)

导入导出

- 当注释包含 `;` 时，SQL 语句拆分出错导致任务异常 [#417](https://github.com/oceanbase/odc/issues/417)
- 类型名称小写时导出任务失败 [#631](https://github.com/oceanbase/odc/issues/631)
- 导出触发器对象时导出失败 [#750](https://github.com/oceanbase/odc/issues/750)
- 函数名称包含特殊字符导出任务失败 [#1331](https://github.com/oceanbase/odc/issues/1331)
- Oracle 模式导出索引时，索引名称添加了数据库名前缀 [#1491](https://github.com/oceanbase/odc/issues/1491)
- 导出存储过程的结构，`DELIMITER $$` 分隔符与表名连在一起 [#1746](https://github.com/oceanbase/odc/issues/1746)
- 导出任务创建完成后终止导出任务，任务状态显示执行成功 [#1752](https://github.com/oceanbase/odc/issues/1752)
- 导出程序包时，任务详细信息中的对象类型未显示包体 [#1755](https://github.com/oceanbase/odc/issues/1755)
- 导入 CSV 文件包含 DATE 类型时导入失败 [#2079](https://github.com/oceanbase/odc/issues/2079)

无锁结构变更

- 当输入语句包含注释时，OSC 任务会因为语法异常而失败 [#1597](https://github.com/oceanbase/odc/pull/1597)

项目和工单

- 任务创建成功提示文案错误，改为工单创建成功 [#1320](https://github.com/oceanbase/odc/issues/1320)
- SQL 窗口下拉切换项目页面崩溃 [#1512](https://github.com/oceanbase/odc/issues/1512)

数据库变更

- 当回滚内容是附件时，回滚过程中没有展示 [#1379](https://github.com/oceanbase/odc/issues/1379)

SQL 检查

- 存在虚拟列时发生空指针异常 [#2031](https://github.com/oceanbase/odc/pull/2031)
- 通过 DROP CONSTRAINT 删除主键的操作未能检测到 [#1879](https://github.com/oceanbase/odc/pull/1879)
- 特定 ALTER 语句会触发的空指针异常 [#1865](https://github.com/oceanbase/odc/pull/1865)

SQL 计划

- 点击终止 SQL 计划无效  [#1528](https://github.com/oceanbase/odc/issues/1528)
- 预检查失败时工单的审批状态显示为预检查失败了 [#218](https://github.com/oceanbase/odc/issues/218)

分区计划

- 当模式或表名为小写时，无法执行分区 DDL [#2088](https://github.com/oceanbase/odc/pull/2088)

数据归档/清理

- 数据归档完成之后触发回滚任务，子任务仍然在运行但是工单状态显示为已完成 [#690](https://github.com/oceanbase/odc/issues/690)
- 并发调度数据归档任务时取消任务，任务状态未更新 [#721](https://github.com/oceanbase/odc/issues/721)
- 使用数据库保留字作为列名时归档失败 [#1040](https://github.com/oceanbase/odc/issues/1040)
- 创建大量表的数据归档任务时更新执行记录会失败 [#1338](https://github.com/oceanbase/odc/issues/1338)
- 任务退出时未释放数据库连接池，任务数量较多时会占用较多线程
- 获取连接失败时连接池会无限重试产生大量日志
- 使用唯一键清理时没有使用正确索引产生慢 SQL

用户与权限

- 批量导入用户包含角色名称会失败 [#1908](https://github.com/oceanbase/odc/pull/1908)

数据安全

- 使用嵌套 CASE-WHEN 子句时数据掩码不一致 [#1410](https://github.com/oceanbase/odc/pull/1410)

系统集成

- 请求正文中包含中文内容时出现乱码 [#1625](https://github.com/oceanbase/odc/pull/1625)

DB Browser

- 包含索引的表创建语句未被识别为创建类型 [#2063](https://github.com/oceanbase/odc/pull/2063)
- 在 Oracle 模式中 DDL 前后多余空格 [#2050](https://github.com/oceanbase/odc/pull/2050)
- 在 Oracle 11g 模式下无法检索有默认值列的表 [#1733](https://github.com/oceanbase/odc/pull/1733)
- listTables 未能一致地准确返回指定模式下的表 [#1632](https://github.com/oceanbase/odc/pull/1632)
- 在 OceanBase 版本 < V2.2.30 时无法 listTables [#1478](https://github.com/oceanbase/odc/pull/1478)
- MySQL 表结构设计的可视化不足，特别是对于单引号括起的字符串 [#1401](https://github.com/oceanbase/odc/pull/1401)

OB SQL Parser

- 解析命名为 'json_table' 的表的 INSERT 语句出错 [#1968](https://github.com/oceanbase/odc/pull/1968)

## 4.2.3_bp1 (2024-02-01)

### 功能变化

- 数据库变更：数据库变更任务适配流式读取 SQL 文件 [#1437](https://github.com/oceanbase/odc/pull/1437)
- 数据归档/清理：支持使用唯一索引进行分片 [#1327](https://github.com/oceanbase/odc/pull/1327)

### 缺陷修复

SQL 执行

- 在 OceanBase v2.2.30 上 执行 SQL 失败 [#1487](https://github.com/oceanbase/odc/pull/1487)
- 在团队空间执行匿名块导致 NPE [#1474](https://github.com/oceanbase/odc/pull/1474)
- 个人设置启用手动提交时，回滚操作没有成功执行 [#1468](https://github.com/oceanbase/odc/pull/1468)
- 不能设置超过 2 个字符的分隔符 [#1414](https://github.com/oceanbase/odc/pull/1414)
- 在 SQL 窗口查询请求期间，注销时前端崩溃。

结果集

- 当结果集中有多列时，滑动后无法选择，且前端在随机点偶尔崩溃。
- 过滤结果集后，列模式中没有内容。

表对象

- 查询表数据时没有列注释 [#1488](https://github.com/oceanbase/odc/pull/1488)

数据导出

- 导出包体和同义词时，不显示对象类型 [#1464](https://github.com/oceanbase/odc/pull/1464)

任务

- 创建没有连接信息的工单时 NPE [#1479](https://github.com/oceanbase/odc/pull/1479)
- 并发创建任务时无法正确设置任务状态 [#1419](https://github.com/oceanbase/odc/pull/1419)
- 当任务创建者和审批者不是当前用户时，查看任务审批节点时发生错误

无锁结构变更

- OSC 任务在全量迁移完成时不显示手动交换表名 [#1357](https://github.com/oceanbase/odc/pull/1357)

数据库变更

- 部分场景查询任务详情抛出流程实例未找到异常 [#1325](https://github.com/oceanbase/odc/pull/1325)
- 任务日志过期时查询任务详情抛出文件未找到异常 [#1316](https://github.com/oceanbase/odc/pull/1316)

分区计划

- 如果关联的调度配置不存在，删除作业失败 [#1495](https://github.com/oceanbase/odc/pull/1495)

数据归档/清理

- 编辑速率限制配置后，数据清理任务调度失败 [#1438](https://github.com/oceanbase/odc/pull/1438)

桌面版本

- Ubuntu 桌面版本无法打开命令行窗口。

其他

- 连接到 OBOracle 的小写模式时，SQL 检查失败 [#1341](https://github.com/oceanbase/odc/pull/1341)
- MetaDB 默认字符集为 GBK 时，执行带有生僻字的 SQL 语句失败 [#1486](https://github.com/oceanbase/odc/pull/1486)

### 安全加固

- 升级 aliyun-oss-sdk 版本 [#1393](https://github.com/oceanbase/odc/pull/1393)
- 无锁结构变更在手动交换表时存在水平越权访问数据权限 [#1405](https://github.com/oceanbase/odc/pull/1405)

## 4.2.3 (2023-12-22)

### 功能变化

数据源

- 允许数据源绑定到项目
- 支持 OceanBase Sharding MySQL 数据源
- 支持克隆数据源
- 团队空间中支持在对象树中展示数据源状态

导入导出

- 支持原生 MySQL 数据源的导入导出
- OceanBase 数据源的导入导出任务配置页面中不再提供 SYS 账户配置

数据库对象管理

- OceanBase MySQL 以及原生 MySQL 模式下支持 GIS 数据类型
- 白屏创建或删除索引时给出高风险操作提示

项目

- 增加 2 个内置项目角色：安全管理员，参与者；安全管理员被允许管理项目的敏感列和参与审批，参与者被允许参与审批
- 允许用户申请项目权限
- 禁止删除以 DBA 或 项目 OWNER 角色归属到任意项目中的用户

SQL 开发规范

- 优化了 SQL 拦截交互
- 增加了问题定位功能，支持快速定位原始 SQL 中的具体问题

数据库连接会话

- 增加了自动重连机制，避免长时间不使用场景下会话销毁导致的报错及易用性问题

分区计划

- 支持定时调度

SQL 执行

- SQL 片段最大支持 65535 大小的内容
- 支持 Ctrl+Enter 快捷键执行当前语句

模拟数据

- 模拟数据支持最大行数从 1 百万行提升至 1 亿行

堡垒机集成

- 支持 SQL Check

DLM

- 支持日志查看
- 新增三个任务参数配置：查询超时时间、分片大小、分片策略
- 优化了 MYSQL 5.6 的性能表现
- 优化了 OceanBase 数据清理的性能表现

全链路诊断

- 支持导出 Jaeger 兼容的 JSON 文件
- 视觉效果优化
- 增加了结果的列表视图，支持搜索和排序

工单

- 项目管理员能查看该项目下的所有工单，其它角色能查看自己审批过的工单

### 缺陷修复

数据源

- 用户离开项目且不属于任何项目和角色时依然可以创建数据源
- OceanBase MySQL 以及原生 MySQL 模式下数据库会话中"执行时间"栏目为 0
- OceanBase Oracle 模式下通过会话变量管理功能进行时间输出格式的修改在 SQL 执行窗口不生效
- OceanBase Oracle 模式下无法连接小写的 schema
- 无法连接 percona 分支构建的 MySQL 数据源

SQL 执行

- SQL 执行过程中抛出错误没有国际化
- 无法在团队空间下执行带有 dblink 的 SQL
- 安全规则允许的前提下，无法在团队空间中执行 desc 语句
- OceanBase Oracle 模式下执行 SELECT ... ORDER BY 1 样式的语句时 ORDER BY 会失效
- 禁用"SQL 窗口允许执行的 SQL 类型"规则不生效

数据库对象管理

- OceanBase MySQL 模式下左侧对象树上展示的表的分区等对象的名字带有反引号包围符

结果集导出

- 任务没有日志打印
- 以 excel 格式导出后没有数据

PL 对象

- 交互式创建函数过程中，无法通过下拉菜单定义 sys_refcursor 类型的返回值
- OceanBase MySQL 模式下 PL 参数值没有对单引号转义

DLM

- 数据库连接池过小导致任务执行失败

分区计划

- 在 OceanBase 1.4.79 版本的 MySQL 模式下创建任务失败
- 不设置分区策略的表依然会执行分区计划变更

SQL 开发规范

- 无法识别`alter table xxx drop index`语句为 DROP INDEX 语句

外部审批集成

- 无法识别索引集合中的数据的表达式
- 外部系统返回的 xml 形式的数据在反序列化时会丢失原始 xml 的根 tag

数据脱敏

- 当扫描到重复列时会导致敏感列添加失败

项目

- 用户被赋予"个人空间"权限后必须重新登陆才能生效
- 同步大量数据库或 schema 到项目时发生事务超时
- 无法以项目维度过滤工单
- 项目 OWNER 可以将项目中所有 DBA 角色的用户全部移除

堡垒机集成

- 不活跃连接没有被清理

回收站

- 无法删除回收站中的特定对象

模拟数据

- 任务占用过多内存
- 不支持 ZHSGB232 编码
- OceanBase MySQL 以及原生 MySQL 模式下无法对宽度在 8 以下的 bit 类型生成任务
- 无法跳过自增的主键列
- OceanBase MySQL 以及原生 MySQL 模式下 bit 类型宽度显示错误

数据库变更任务

- 上传大文件场景下出现内存溢出错误

全链路诊断

- 将驱动加入全链路诊断后导致的内存溢出问题

影子表同步

- 工单被同意或拒绝后审批者无法查看任务详情

obclient 集成

- 重复创建同名操作系统用户导致报错

工单

- 创建工单耗时过久
- 一个项目的"待审批"工单列表中存在另一个项目的"待审批"工单

操作记录

- 操作记录中"数据源"栏目为空
- SQL 执行事件没有被记录
- 打开 SQL 窗口事件没有被记录

### 改进

- 提升 SQL 执行性能，减少不必要的耗时操作
- 允许用户配置登录失败场景下的最大重试次数以及账户锁定时间
- 只允许用户白屏修改带有主键约束，唯一键约束以及 rowid 的表数据
- 优化同步数据库出错时的报错文案

### 依赖库升级

- 升级 obclient 版本到 2.2.4
- 升级 spring security 版本到 5.7.10
- 升级 hutool 版本到 5.8.23
- 升级 pf4j 版本到 3.10.0
- 升级 netty 版本到 4.1.94.FINAL

## 4.2.2 bp (2023-11-23)

### 缺陷修复

数据归档

- 数据归档子任务开始运行后更新限流器配置无法生效
- 数据清理任务没有运行

数据脱敏

- 自动扫描敏感列场景下输入恶意识别规则会导致正则表达式拒绝服务

SQL 执行

- MySQL 模式下以科学计数法展示 NUMBER 类型的数据

PL 运行

- 无法查看游标的内容

SQL-Check

- 数据库没有报语法错的时候 SQL-Check 依然会提示语法错误

## 4.2.2 (2023-11-07)

### 功能变化

数据源

- 支持 MySQL 数据源
- 适配 OceanBase 4.2.0/4.2.1
- 数据源增加初始化脚本以及自定义 JDBC 连接参数设置

数据脱敏

- 支持视图脱敏

数据归档

- 支持 OceanBase 4.x 版本
- 支持 MySQL 到 OceanBase 链路
- 支持白屏限流配置
- 支持断点恢复

导入导出

- 支持类型对象的导入导出

### 改进

- 优化了大规模表列场景下的对象管理性能，通过 ob-sql-parser 解析索引/约束的元数据
- 优化了数据库对象树交互，项目和数据源的选择交互区域折叠到顶部，数据库列表展示更清晰
- 优化了 SQL 窗口新建和 SQL 窗口切换数据库的交互，切换数据库更快速了，SQL 窗口增加复制操作
- 优化了数据脱敏配置交互，选择敏感列更方便
- 优化了存在大量数据源场景下获取数据源列表缓慢以及获取数据源状态缓慢的问题
- 优化以错误参数运行 PL 时的报错文案

### 缺陷修复

PL 调试

- 调试过程中无法跳入程序包中定义的子存储过程/函数

SQL 执行

- 执行 SQL 过程中持续执行 "DROP PACKAGE" 语句导致大量报错
- 连接 OceanBase MySQL 租户时自动调用 "obodc_procedure_feature_test" 存储过程导致报错或进入连接缓慢
- SQL 执行耗时统计各子项耗时之和不等于父项
- SQL 执行耗时统计中， "SQL 预检查"及"SQL 后置检查"缺乏详细子项耗时统计

SQL-Check

- OceanBase Oracle 租户下创建 type 时，如果子存储过程/函数无参数列表，SQL Check 报语法错误
- 无法关闭"语法错误"规则

数据脱敏

- SELECT 语句中含有多表 JOIN 的场景下脱敏失败
- 大小写敏感的 OceanBase MySQL 模式下无法识别到敏感列导致脱敏失效

数据库对象管理

- 没有 show create view 权限用户查看视图详情时报错
- 查看表对象时所有字符类型的长度无法显示

数据库变更

- 数据库变更任务超时时间设置无效
- 个人空间下查看定时执行的带有自动回滚节点的数据库变更任务详情失败

导入导出

- Oracle 模式下导出程序包不包含包体
- 无法将含有制表符的结果集导出为 Excel

操作审计

- 没有将"SQL 检查规范"以及"SQL 窗口规范"改变纳入操作审计范围

### 依赖库升级

- 升级 ob-loader-dumper 版本到 4.2.5-RELEASE
- 升级 oceanbase-client 版本到 2.4.5

### 安全加固

- 前后端敏感字段传输使用非对称加密

## 4.2.1 (2023-09-25)

### 缺陷修复

SQL 执行

- OceanBase 4.0 之前版本时无法打印 DBMS 输出
- 编辑结果集时 DML 语句生成速度缓慢

导入导出

- 任务执行过程中导入/导出对象在任务详情中不显示

PL 调试

- OceanBase 多节点部署时 PL 调试偶现无法连接到数据库
- OceanBase Oracle 模式小写 schema 下调试匿名块时获取数据库连接错误

数据源管理

- 回收站模块生成的针对索引的 flashback 语句执行报错
- 会话管理界面无法显示会话正在执行的 SQL
- 批量导入连接时模版文件中存在空行或空列时引发空指针异常

数据脱敏

- OceanBase MySQL 模式配置为大小写敏感时，敏感列无法区分大小写

工单管理

- 工单提交后工单状态长期处于“排队中”不更新且工单不报错

第三方集成

- 审批流未包含外部审批节点时也会尝试获取外部审批工单的 ID

SQL-Check

- OceanBase Oracle 模式下无法检测表或列的注释是否存在

### 安全加固

- 解决第三方集成过程中可能发生的 SSRF 攻击