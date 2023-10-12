# OceanBase Developer Center (ODC) CHANGELOG

## 4.2.0 (2023-08-18)

### 功能变化

基于项目的管控协同

- 新增基于项目的管控协同，简化协同场景的权限配置，内置 项目 OWNER、项目DBA 及 普通成员 3 个项目角色。
- 内置 开发、测试、生产、默认 4 个环境，SQL 检查和 SQL 窗口规范可根据环境分配配置。
- 新增风险等级识别，按照风险等级配置工单审批流程，项目成员角色可应用在审批流节点配置中。
- 项目成员拥有项目内数据库的查询权限，若需要进行变更操作需要发起工单。

SQL 检查

- SQL 检查全新改版, 内置 45 条 SQL 检查规则，检查规则可配置。
- SQL 检查会应用在 SQL 窗口及数据库变更工单中，覆盖 SQL 执行、对象管理、SQL 计划 等全部场景。

数据库变更

- 支持自动生成 DML 语句对应的回滚脚本，支持 UPDATE/DELETE 语句。

安全合规

- 支持动态脱敏：项目 OWNER 可对本项目内的数据进行敏感配置，保证敏感数据被访问（SQL 查询、导出）时是脱敏状态。
- 操作审计内容完善：动态脱敏、无锁结构变更、数据清理、数据归档功能纳入审计。

数据生命周期

- 新增数据清理功能：您可以定期清理掉业务表中的过期数据，实现在线库的瘦身。
- 新增数据归档功能：通过数据归档，您可以配置灵活的归档条件，实现冷热数据分离。

执行分析

- 提供 SQL 执行视角下的全链路诊断功能：OceanBase 4.1 及之后版本，支持在查询结果的「计划」中查看全链路 TRACE 信息。

SQL 窗口

- 对象树展示具有权限的所有数据库。
- 新增切换数据库功能。

PL

- PL 调试入参交互全面升级，支持指定当前时间等作为入参。
- 支持查看 sys_refcursor 类型的返回值内容。

SQL 执行

- 结果集根据查询会话的 nls_date_format 参数值展示日期值 。
- 解决 Oracle 模式浮点型数据显示问题（小于1时小数点前补0）。
- SQL 窗口增加右键粘贴功能（需要注意是安全的网络访问环境/https，否则剪切板能力会被禁用）。

导入导出

- 导入兼容外部工具 PL/SQL Developer。
- MySQL 模式支持 SEQUENCE 对象导出。

表

- 新增 DISABLE 外键功能。
- 创建表新增列默认值为 `null` 的单选框。

三方集成

- 外部审批集成产品化，包括审批集成、SQL 审核集成、SSO 集成。

桌面版

- 升级 h2 数据库，提高稳定性
- ODC 客户端版适配 Linux 桌面版，支持 Ubuntu20 系统。

其他体验改进

- 登录被锁定显示重试时间。
- 前端错误处理优化，解析结果出错场景展示更多相关信息。

技术改进

- ODC 获取 DBMS 输出的方式改为调用 `dbms_output.get_line`。

### 云服务

- 支持无锁结构变更：依赖 OMS 服务，通过 OMS 提供的全量数据迁移、增量同步及数据校验能力实现低业务影响的结构变更。
- 支持 kill session/kill query。
- 公有云 RAM 权限集成，支持只读账号访问 ODC。
- 支持聚石塔账号集成识别管理员身份。

### 非兼容性变更

- 连接配置概念调整为数据源，对应的连接配置批量导入模板调整为数据源批量导入模板。
- 数据脱敏重新实现，之前的数据脱敏配置不再生效。
- 审批流程配置重新实现，之前的审批流程是配置到任务类型的，新版本配置到风险等级，之前的审批流程配置不再生效。
- 堡垒机集成功能暂时不可用，会在下个小版本恢复支持。
- 回收站管理功能从 SQL 控制台调整为数据源管理，回收站是否开启的设置从 SESSION 级别调整为租户全局设置。
- 分区计划任务本期暂不支持，下个小版本恢复支持。
- 内置代码片段不支持查看。

### 缺陷修复

数据库变更

- 数据库变更任务包含耗时语句时，数据库变更任务终止没有取消对应的 SQL 执行。
- 多节点部署时，数据库变更下载查询结果有概率失败。

SQL 执行

- Oracle 表名是关键字时 select 带别名列的自动补全不可用。
- 格式化时 `=>` 中间会自动加空格。
- Oracle 模式执行 `CREATE USER xxx IDENTIFIED BY "";` 分句失败。
- PL 对象包含 mod 取余表达式时子程序/函数展示错误。
- binary number 列含有 Nan 或 Inf 时，查询报错。

结果集编辑

- 上传大对象失败报错消息缺乏可读性。
- Oracle 模式 部分场景修改数据 无法生成 UPDATE 语句。

桌面版

- Java 版本 9 以上未给出对应错误信息而是报 Java 进程异常退出。
- 桌面版启动卡在进度条一个多小时没有反应。

### 安全加固

- 修复 OSS 文件遍历风险。

## 4.1.3 (2023-05-24)

### 功能变化

- OceanBase Cloud 新增单独应用 Load Data
- 适配 OceanBase 4.1

### 依赖库升级

- 升级数据库驱动 oceanbase-client 版本到 2.4.3
- 升级导入导出组件 ob-loader-dumper 版本到 4.2.3

### 缺陷修复

云服务

- MySQL 模式下执行方式为定时任务时下载数据失败
- 连接筛选列表中包含了云服务不可用的类型
- 无法识别到正在进行变更的实例
- 主账户用户可编辑
- 定时执行导入任务偶现无法下载导入文件

管控协同

- 越权添加他人账号的资源管理权限
- 使用 system_admin 角色创建其他角色时报错权限不足
- 查询任务状态借口存在水平越权
- 编辑角色时存在垂直越权错误
- 任务无法手动终止，终止成功后依然会继续运行

SQL 执行

- 无法执行带有 language 标识符的 SQL
- 带有 hint 的 SQL 改写错误导致 hint 失效
- Oracle 模式下表名补全不可用
- BLOB/CLOB 等大字段列查看时数据产生跳变
- PL 创建语句中包含 WHILE-LOOP 时分句结果结尾多了分号
- 执行大规模 PL 创建语句时会 hang 住

导入导出

- 任务启动时失败找不到任何日志
- 导出对象列表中没有发现公共同义词
- 导出程序包中存在 ODC 内建的程序包
- 无法导出临时表

数据库变更

- 数据库变更“执行超时时间”设置无效

连接管理

- 测试连接时无法识别云服务的连接地址

数据库对象

- MySQL 模式下 mysql 库中内置的表对象无法查看详情
- 执行 PL 对象时报错超时，修改会话变量也没有作用

### 安全加固

- 云服务禁用用户创建 API
- 解决官网体验站 odc_back_url 任意 URL 跳转
- 加固 admin 账户初始密码，初始密码通过环境变量设置
- 升级 fastjson 版本至 1.2.83_noneautotype

## 4.1.2 (2023-03-17)

### 功能变化

数据源支持

- 支持 OceanBase 4.1.0 版本

云服务

- 多云支持租户实例

数据库对象

- PL 执行、调试支持复杂对象：object, collection, cursor 等
- 优化 OceanBase Oracle 模式查看表对象的性能
- 视图数据查看支持设置数据条数
- PL 对象创建交互优化
- 增加 PL 调试的基准版本

SQL 执行

- 去除查看 clob/blob 对象 200KB 的大小限制
- SQL 执行提供获取列信息开关，表列数量较多场景下可关闭列信息展示以降低 SQL 执行总耗时
- SQL 窗口会话默认模式从共享 Session 调整为独立 Session
- 结果集展示优化滚动条交互，状态栏增加列注释等信息展示
- 结果集 CLOB 内容查看限制从 200K 提升到 2MB

导入导出

- 支持导出结构到一个文件
- 支持设置单表导出数据文件大小限制
- 导出日期类型 SQL 语句使用 TO_DATE, TO_TIMESTAMP 函数
- 优化 CSV 文件跳过列头提示文案使得不容易误解

连接管理

- 支持批量导入个人/公共连接

管控协同

- 支持批量导入用户

三方集成

- 同时支持账密登录和三方登录
- 单点登录（SSO）支持 OIDC

其它

- 桌面版默认开启埋点
- 错误信息中展示 RequestId
- 影子表选择源表增加全选框

### 缺陷修复

云服务

- 备库切主后无法连接
- 多云下载名称带中文的文件失败

数据库对象

- OceanBase 4.0 及之后版本公共同义词详情时间显示错误
- 创建程序包生成的 DDL 在某些场景下出错
- OceanBase Oracle 4.0 及之后版本的表对象 NUMBER 数据类型 的 precision 和 scale 展示错误
- 表名以 `:` 结尾时，左侧对象树展开表会报错
- 优化表对象不存在时的错误信息
- 带 varchar2 类型的 out 参数 的 PL 运行报错
- 程序包列表右键编译包体无结果返回
- OceanBase 1.4.79 MySQL 模式的 LIST/LIST COLUMNS 分区的分区值列表为空
- OceanBase MySQL 模式用户没有 `oceanbase` 库的 `SELECT` 权限时查看表会报错

SQL 执行

- blob/clob 列为空字符串时，查看该行数据报错
- PL 中包含 `sum(case when then else end) as` 时会导致分句错误
- 存储过程执行成功，但 SQL 检查报语法错误
- 结果集删除行数据时，SQL 检查提示风险
- OceanBase 4.1 查看执行计划出错
- from 子句中包含 pivot/unpivot 时执行 SQL 会失败
- 带 `Q` 转义的 SQL 执行出错
- PL 中有除号 `/` 且分隔符为 `/` 时，分句会出错导致执行失败
- PL 中带标签 `<<>>`时，分句会出错导致执行失败
- PL 后面紧跟注释和换行符时执行会失败
- 左连接有空值时结果集导出报错

导入导出

- 空值导出成了 `\E`
- 导出对象过多时报错
- 当导入导出任务未执行就被取消时，查看日志会报错
- 导出时产生的临时文件未删除，占用大量磁盘空间
- 批量提交数量的设置不生效

管控协同

- 可以在管控台看到只有访问权限的连接信息
- 可以对没有管理权限的资源进行新建、编辑
- 优化了任务列表的加载时间
- 少数场景下，任务可能会自动重试
- 资源名称没有校验是否合法

数据脱敏

- 使用字符替换规则时，脱敏结果会多出一个 `null` 字符串
- 使用字符替换-自定义分段时，错误地添加了分隔符

SQL 计划

- 连接被删除后，连接关联的 SQL 计划无法继续执行

工具

- Windows 客户端命令行工具中文可能乱码
- 多节点部署场景下命令行窗口 source 命令无法引用文件

安装部署

- 启动 docker 时设置 4G 内存大小会启动失败
- 公有云组织过多资源重复加载导致启动超时

### 安全加固

- hutool 版本升级到 5.8.12
- Spring 版本升级到 5.3.24
- Spring Security 版本升级到 5.7.5
- 修复水平越权问题
- 修复 swagger 依赖任意文件下载的问题

## 4.1.1 (2023-02-23)

### 缺陷修复

数据库对象

- 回收站删除、还原对象时生成的 SQL 无法执行
- OceanBase Oracle 模式主键对应的索引范围错误地展示为了 LOCAL
- 修改表的列时可能会由于字符集不匹配而失败

SQL 执行

- SQL 执行时查询 SQL 执行时间慢
- 执行 PL 时，JDBC Prepare 阶段时间计算错误
- 分隔符为 / 时，SQL 执行分句可能出错

连接

- 未保存密码时打开连接报错
- SQL 窗口中偶现断连的错误
- 用户缺少数据库权限，进入连接后有报错

云服务

- 公有云场景下载文件时文件名可能会错误
- 公有云 STS 账号登录时的逻辑错误
- 反复修改连接实例，保存后连接失效

其它

- 打开命令行工具，密码有特殊字符时无法连接
- 流程配置中找不到对应的角色
- 影子表同步时，ON UPDATE CURRENT_TIMESTAMP 没有指定精度
- 由于环境变量导致的桌面版启动失败

### 体验优化

- 优化了公有云错误消息展示
- OceanBase MySQL 模式大规模表列场景下的表信息获取性能优化

### 安全加固

- 升级 fastjson 依赖库版本为 1.2.83.noneautotype
- 修复潜在的越权问题

## 4.1.0 (2023-01-12)

### 功能变化

自动授权

- 支持管理自动授权规则
- 支持用户第三方登录时自动绑定权限，绑定角色

管控台

- 支持用户直接关联权限，去掉之前用户关联权限必须通过角色的限制
- 新增直接管理公共连接权限的能力
- 新增资源管理权限，系统操作权限的配置能力
- 操作记录入口移动至安全审计菜单下

结果集编辑

- Oracle 模式多表关联查询，查询列包含其中一张表的 rowid 字段时结果集支持编辑

导入导出

- 客户端模式下支持将导出文件输出至指定目录
- 导入导出支持 OceanBase 4.0 版本
- 剥离导入导出功能对 sys 账户的依赖，大部分数据库对象的导入导出不再依赖 sys 账户。无 sys 账户存在的限制如下：
    - MySQL 租户无法导出序列定义
    - OceanBase 2.2.70 之前的版本无法导出表组定义
    - OceanBase 2.2.30 及之前的版本 Oracle 租户下无法导出索引定义
    - OceanBase 2.2.70 之前的版本无法导出唯一索引的分区信息
    - OceanBase 2.2.70 ~ 4.0.0.0 版本的 Oracle 租户无法导出分区表的唯一索引定义

SQL 执行

- 去掉 ODC 产品层面对于 SQL 执行的超时限制
- 增加会话状态指示器，提示当前事务状态，提交回滚按钮在处于事务中时自动激活 （OceanBase 4.0 以上版本支持）
- 对 SQL 执行的耗时信息进行细化，增加 OBServer 执行耗时和网络传输耗时统计
- 增强生僻字支持，在 SQL 编辑器、结果集查看时支持常用国标生僻字，PUA 码支持显码字展示
- 对象拖拽生成语句交互支持记住选项不再提示选择语句类型
- 结果集编辑支持 shift 键多行选择

连接管理

- 更改首页连接列表的数据组织形式
- 支持对连接的批量操作
- 支持公共连接的置顶，打标签操作
- 支持配置 SSL 数据库连接
- 历史连接入口调整为一级菜单

数据库对象

- 增加对数据库表的记录数以及数据大小信息的展示

SQL 检查

- 增加 SQL 检查功能，对用户的 SQL 进行静态规则检查
- 增加用户执行 SQL 时的静态检查能力，可以通过个人配置/系统配置来控制 ODC 的行为
- 公共只读连接的数据库变更任务上增加 SQL 检查节点，展示用户数据库变更脚本的静态检查结果

自动运行

- 支持定时执行 SQL 脚本，模拟 Oracle 中 Job 的能力

### 缺陷修复

任务中心

- 公共只读连接关联流程执行策略显示错误
- 公共只读连接关联流程详情打开缓慢
- 存量任务数据量很大的情况下，任务列表刷新缓慢

数据库变更

- 回滚数据库变更任务前后下载得到的查询结果不同

导入导出

- 无法导入导出表组
- 无法使用 ISO8859-1 字符集导入/导出数据
- 导出带有 Float 数据类型的表存在 Float 精度损失

管控台

- 无法验证已存在的公共连接的权限

SQL 执行

- 无法执行含有 emoji 等特殊字符的 SQL 语句
- SELECT 语句的 WHERE 条件中如果带有 `*` 且指定投影字段会执行报错
- 执行某些特定 PL 场景下 SQL 控制台无法正常分句导致执行错误且没有绕过方法
- MySQL 模式下值为 0000-00-00 00:00:00 的 datetime 数据显示为`null`
- DML 语句执行成功后影响行数为 0

结果集

- 下载结果集时使用数据脱敏，查询结果集元数据出错
- 结果集数据编辑过程中，如果修改某个数据后再改回去，点击确认后将会报错
- 编辑 Clob 类型的列时，上传数据中存在`;`导致编辑报错

数据库对象

- OceanBase 3.1.2 之前的版本，如果表注释为空，则查看表详情时报错"预期外异常"
- 对象详情中无法查看表以及视图对象位于 1000 条以后的记录
- Oracle 模式查看表 DDL，如果有唯一索引，DDL 中会重复输出一次
- 访问无权限的 MySQL 数据库时，存储过程，函数等数据对象报错
- OceanBase 1.4.79 之前的版本查看表对象报错
- OceanBase 2.2.50 之前的版本查看表/视图数据时白屏
- 大容量表/视图查看数据内容时缓慢，可能会导致连接的 OceanBase 租户 CPU 使用率过高
- 回收站开启的情况下，白屏删除的表对象无法进入回收站
- 创建视图时如果关联表的表名中含有`&`则无法显示`&`后的表名
- MySQL 租户，`information_schema`数据库下的任意视图对象的"基本信息"为空
- 左侧对象树无法展示程序包中名称含有中文的子程序

影子表

- 影子表结构分析，取消/选择一个源表，点击下一步后一直处于加载状态
- 全局唯一索引没有包含所有一级分区键，导致生成的 SQL 不正确

PL 调试

- 程序包调试过程中，如果存在子程序互相调用，跳入后打断点将抛出异常

用户管理

- ODC 4.0.2 版本用户无法修改自己的密码

### 安全加固

- 部分场景下可能出现敏感信息明文透出；
- 升级 spring-cloud-config-server 版本到 2.2.6.RELEASE
- 升级 jackson 相关组件版本到 2.14.1
- 升级 jettison 版本到 1.5.2
- 阿里专有云接入 rass 模块
- 去掉 java-jwt 依赖
- 去掉 javacsv 依赖
- 升级 hadoop-common 组件版本至 3.3.3
- 升级 netty-all 组件版本至 4.1.44.Final
- 升级 mina-core 组件版本至 2.1.1
- 剥离 log4j 依赖库

## 4.0.2（2022-11-22）

### 功能变化

云服务

- 支持公有云按租户售卖
- 新建连接交互优化
- 多云达到可发布的状态

### 缺陷修复

云服务

- 多云公共连接数据库账密权限校验失败
- 多云部分接口会报 403 无权限
- 多云含有特殊字符或中文的脚本上传后显示有误
- 公有云定时导入任务可能执行失败
- 公有云下载导入文件可能失败

数据库对象

- OceanBase MySQL 模式数据库用户没有 `oceanbase` schema 访问权限时，窗口会卡住
- 重命名约束时生成的 SQL 为空

PL 执行

- PL 执行超时后未正确返回错误消息并自动重新建连
- 部分场景下执行 PL 报空指针异常
- PL 调试无法识别 `NULL` 与 `default` 的参数

导入导出

- 导入导出任务在部分场景下数据库连接未释放
- 数据类型 datetime 结果集导出错误
- 结果集导出脱敏失效

数据库变更

- 数据库变更任务中 year 和 datetime 类型的数据展示内容错误
- 定时执行数据库变更任务，回滚内容可能不会执行
- 定时执行数据库变更任务，查询结果可能报错 `query result file not found`
- 数据库变更下载查询结果，回滚前和回滚后下载内容不一致

管控协同

- ODC 多节点部署场景下，外部审批集成状态有时会无法同步
- 连接没有对应的审批流程的错误信息国际化
- 公共连接读写权限验证错误

其它

- ODC 多节点部署场景下，请求转发时可能造成循环调用

### 安全加固

- 修复云服务明文数据库账密

### 其它

- 公有云场景 session 不再持久化，避免频繁的数据库操作

## 4.0.1 (2022-10-28)

### 缺陷修复

- 部分场景测试连通性失败，导致无法创建连接
- 名称包含特殊字符的回收站对象还原失败
- 分区计划可能会扫描出已删除的分区表
- 连接 MySQL 数据源错误提示优化
- 修复公有云/多云场景下脚本名称包含 unicode 字符时显示错误
- 公有云/多云场景下验证公共连接数据库账号权限失败
- 多云场景下名称名包含中文的文件上传失败

## 4.0.0 (2022-10-20)

### 功能变化

影子表同步

- 新增影子表同步功能，支持根据源表的表结构在同一个数据库中自动创建和同步影子表结构
- 支持所有版本的 OceanBase MySQL 模式
- 支持选择影子表表名的生成方式，可选添加前缀或者后缀
- 支持预览源表、影子表 以及 结构对比的 DDL
- 支持批量 跳过/取消跳过 影子表的同步

分区计划

- 新增分区计划功能，支持自动预创建和删除过期的 RANGE 分区 和 RANGE COLUMNS 分区
- 支持所有版本的 OceanBase MySQL 模式
- 支持自定义配置分区策略，可选预创建分区数量、分区间隔、保留时长和命名规则等

管控协同

- 集成 阿里巴巴集团 BPMS，打通了 ODC 的变更流程和 BPMS 的审批流
- 外部 SQL 审批集成支持批量 check API 方式

数据源适配

- 支持 OceanBase 4.0 版本
- 支持 OceanBase 4.0 CE 版本

新建连接交互优化

- 支持不输入数据库账号密码即可保存连接
- 优化 SYS 租户账号设置交互
- 测试/保存连接交互优化，显示具体的错误信息
- 界面交互优化

云服务

- 解除阿里云公有云 POP 网关对请求和返回数据大小的限制，解决了长 SQL 在公有云无法执行等问题
- 集成 Amazon S3 服务，支持 ODC 多云文件上传下载场景

### 缺陷修复

数据库对象

- `information_schema` 下的系统视图不显示
- 无法查看 `oceanbase` 下的表结构
- RANGE 分区的表达式包含函数时显示错误
- 新建表，分区名称带有特殊字符时生成的 sql 执行失败
- 新建表，优化 HASH 分区数小数规整
- OceanBase Oracle 模式查看临时表报错
- OceanBase MySQL 模式版本小于 2.2.77 时，查看表结构报错
- OceanBase Oracle 模式下，视图的检查项实际为只读，但是界面为 NONE
- PL 调试无法识别 NULL 参数
- 部分场景下 SQL 窗口创建 PL 失败

工具

- 连接密码带有特殊字符导致命令行窗口打开失败
- 导入文件格式为 CSV 时，选择导入前清空数据无法生效
- SYS 账密错误导致的查看导入导出任务日志报错

变更流程

- 多节点部署时, 导入任务定时执行可能会失败

其它

- 连接断开情况下执行 sql 未报错
- 表对象拖拽生成的 insert/update 语句变量展示为原始的标签
- 执行单条语句失败，高亮行显示错误
- SQL 窗口编辑、格式化超长 SQL 导致页面崩溃

### 安全加固

- 升级了 spring-security 等三方依赖的版本以修复一些安全漏洞
- 修复文件上传扩展名检测不生效的问题

### 部署变化

- ODC 映射端口号的环境变量名 从 ODC_PORT 改为 ODC_MAPPING_PORT

## 3.4.0 BP1 (2022-09-20)

### 缺陷修复

- 登录界面排版异常，不符合预期的问题
- 单表导入使用 txt 后缀的 csv 格式失败
- Oracle 模式函数索引未显示具体的函数信息
- Oracle 模式 NOT NULL 列更新失败
- Oracle 模式账号无 v$nls_parameter 权限时表对象查询报错

## 3.3.4 BP2 (2022-09-16)

### 功能变化

新增一些系统配置项, 支持堡垒机场景配置以下功能

- 禁止用户新建连接配置
- 禁用不受金库管控的功能
- 连接配置的最小连接超时时间

## 3.4.0 (2022-09-09)

### 功能变化

#### 基础功能

界面改版

- 全新的暗黑模式，新世代数据库开发者工具体验
    - 默认主题和暗黑主题支持一键开启无缝切换
- 图标体系重新设计
    - 数据库对象和窗口页签采用面性图标，更具辨识度
    - SQL 窗口和 PL 窗口使用线性图标，功能操作表意更强灵活度高

SQL 执行

- 优化语法高亮，完整高亮当前行
- 支持快速定位到错误语句并高亮
- SQL 自动补全排序优化

表对象

- 全面改版，提升功能完整度和交互易用度
- 交互升级，给使用者带来更具操纵力的功能体验
- 对 Oracle 和 MySQL 模式下表对象的区别进行了细粒度的区分
    - 支持设置虚拟列、表达式、索引类型、是否可见
    - 列支持 `SET` 和 `ENUM` 数据类型
    - 列属性支持设置 字符集、符号选择、日期是否自动更新 `ON UPDATE CURRENT_TIMESTAMP`
- 增加了一系列适配 OceanBase 的规则判断，如
    - 新建表时如果没有新建列，提示用户无法新建表
    - 根据字段类型自动生成该字段的默认长度
    - 已有字段自增属性可以取消不可新增
    - 内置字段属性间的互斥关系，如字段自增则不可设置默认值

结果集

- 查看格式全面优化，包括 数字右对齐、列宽自适应、精度匹配
- 支持在指定位置添加新的数据行
- 结果集导出数据条数可配置
- CSV 格式导出默认配置为 Excel 兼容配置
- 结果集导出 Excel 支持配置是否包含列头、是否导出查询 SQL 语句
- Windows 环境支持生僻字显示

导入导出

- 导入导出交互改版，单页设置升级为引导式多步骤设置，默认设置优化降低配置成本
- 增加导入文件校验，上传文件不匹配时错误提示更友好
- 兼容性提升，之前版本 ODC 导出的文件也可以可以导出到新版本 ODC
- 支持整库导出，不再需要选择全部对象
- 导出时支持配置数据脱敏策略
- 桌面版去除导入导出文件大小限制

体验改进

- 对象管理、数据修改等场景下的 SQL 预览支持 SQL 语句格式化
- SQL 窗口页签增加正在执行 SQL 语句的标识，全局一览清晰明了
- 任务日志展示格式优化，日志区段用不同颜色区分
- Windows 模式命令行窗口支持 `Ctrl + C` 复制内容
- 任务日志支持下载

#### 管控协同

变更管控

- 用户可以主动发起数据库连接权限申请，管理员审批后自动授权，管理员也可以按需授权和撤销授权
- 管理员可以为不同数据库连接配置不同的审批流，数据库变更、导入、导出、模拟数据等不同变更场景可以分别配置
- 审批节点支持配置为自动审批，配置为自动审批的节点，审批人可以看到相关的审批任务，但是不需要手动执行审批确认操作
- 发起数据库变更任务时，支持设置执行时间
- SQL 窗口自动识别需要审批的 SQL 语句，执行时提示发起数据库变更流程

数据脱敏

- 数据脱敏支持数据导出、结果集导出场景
- 提供 邮箱、银行卡号、固定电话、手机号、身份证号、姓名 等内置脱敏规则
- 脱敏策略通过 `库名.表名.字段名` 通配匹配对应的脱敏规则
- 支持自定义脱敏规则，提供 遮掩、字符替换、保留格式、哈希加密、取整、置空 等常用脱敏算法

系统集成

- 支持 OAuth2 账号集成

### 缺陷修复

导入导出

- 导入导出多任务并行执行时日志串扰
- 导入导出 MySQL 模式用户没有 mysql schema 权限时报错
- 单表导入 CSV 列映射有时会失败
- 导出数据时如果未选择任何表报错

SQL/PL 执行

- PL 执行 DBMS_OUTPUT 输出长度超过 1MB 时无法展示
- SQL 查询后切换数据库，结果集导出失败
- SQL 窗口执行 PL，当 PL 块内部的 SQL 语句包含 `CASE... WHEN` 时拆句出错
- Oracle 模式 timestamp 精度展示错误
- MySQL 模式 datetime 字段内容多显示了 `'.0'`
- 结果集导出 Excel 选择不导出列头不生效，Excel 还是会带上列头
- 结果集查看列模式下大字段显示错误
- 切换为独立 session 时表数据内的点击提交和回滚会有弹窗

其他

- 连接已断开场景，点了暂不连接之后还会反复连接已断开提示
- 数据库变更任务中的 delimiter 命令不生效
- 新建视图，自定义字段会被转换成字符串
- 序列 DDL 格式化无效
- Oracle 模式表的 DDL 查看 COMMENT ON TABLE 语句没有加上用户名
- MySQL 模式修改字段注释会丢掉字段的 on update timestamp 属性
- 任务详情中切换查看日志卡顿
- 部分国际化文案翻译错误

### 安全加固

- 登录验证支持验证码校验
- 部分资源水平越权加固
- 增加数据库类型判断

### 部署变化

- 去除了内部 RPC 监听端口

## 3.3.4 BP1 (2022-09-06)

> 非发行版本，用于阿里云专有云渠道问题修复。

### 缺陷修复

- 增加 ASO 鉴权适配
- 升级权限配置

## 3.3.4 (2022-08-17)

> 非发行版本，新增学习模块支持交互式学习、移动 4A 堡垒机集成问题修复。

### 功能变化

- 体验学习增加教程功能
- 实验资源管理支持多集群和负载均衡
- 体验学习增加会话数量限制
- SQL 执行增加语句长度和语句数量限制
- 文件上传增加后缀校验

### 缺陷修复

- 堡垒机集成场景连接名称可能会超限导致连接配置创建失败

### 其他

- RPM 包区分 x86 / aarch

## 3.3.3 (2022-08-02)

对PL调试进行了改进，优化了 Windows 桌面版启动时间。

### 功能变化

- 适配 OceanBase 3.2.3.1 version 变量行为变化

### 缺陷修复

SQL 窗口

- MySQL 模式 数据库名称包含大写字母时进入连接报错
- 表名称包含 `$` 或 `/` 时改写失效
- Oracle 模式 部分语句查询结果集编辑自动匹配 ROWID 不生效

PL 对象

- 部分场景下调试报错
- 用户自定义类型入参的程序包运行报错

部署升级

- 从 2.4.0 版本升级到更高版本可能会失败
- 使用 OceanBase 1.4.79 版本的 MetaDB 可能会升级失败

任务流程

- 并发任务数量较多时可能出现任务一直等待不执行

其他

- 部分场景下 Windows 桌面版启动超时导致启动失败
- 管控协同，流程配置权限依赖用户管理权限
- 堡垒机集成，部分场景下账号集成出错
- 体验学习，部分场景下体验资源回收会失效

### 安全加固

通用

- 修复部分场景下的 SSRF 风险
- 升级 common-configuration2 依赖库版本，修复 CVE-2022-23305 安全漏洞
- 修复部分功能的水平越权风险

公有云

- 云服务集成 RASP 防护

## 3.3.2 (2022-06-30)

### 新增和优化

SQL 执行

- SQL 窗口语句自动补全支持 `oceanbase`/`information_schema` 下系统视图

PL 对象

- 新增 PL 对象批量编译
- 新增 PL 对象列表按照状态筛选
- PL 对象 DDL 查看支持下载为本地文件

外部集成

- 新增 WEB 版堡垒机集成
    - 支持外部账号服务集成实现自动登录
    - 支持通过 url 参数传递数据库连接参数跳转后直接进入 SQL 窗口
- 新增 WEB 版审批集成
    - 支持移动 4A 审批系统集成

交互优化

- 浏览器 Tab 和桌面版窗口名称包含 `@{数据库名称}` 信息
- 导入任务日志中增加了执行出错的具体 SQL 语句
- 会话管理鼠标 hover 展示完整的 SQL 语句
- SQL 窗口执行 SQL 超时时，提示索引可能仍在创建
- 编辑角色权限时，公共资源权限需要至少配置 1 个

部署升级

- 新增降级判断，识别到版本回退会启动失败报错

### 缺陷修复

PL 对象

- 调试流程和逻辑完善，修复一系列不支持的场景，注意需要升级 OBServer 到 3.2.3 最新版
- 编译过程加固，修复部分场景下编译错误不准确和未包含行号等问题，注意需要升级 OBServer 到 3.2.3 最新版

数据库对象

- 表、视图、PL 对象列表未按照字母顺序排序
- 部分分区表对象查看报错
- 序列编辑出错时错误消息未展现

SQL 执行

- Oracle 模式 `select * from table` 无唯一性约束场景生成变更语句没有包含 ROWID
- Oracle 模式修改 delimiter 后执行 PL 报错
- 结果集查看 CLOB 类型选中态放大查看文本内容无法复制
- 结果集导出文件名中文被替换为 `+` 号

导入导出

- Windows 环境导入 ZIP 文件出错

其他

- 用户、角色修改后 修改时间 值未更新
- 会话变量页面在部分场景下会崩溃

## 3.3.1 (2022-05-23)

### 新增功能

SQL/PL 执行

- SQL 执行时间精细化
- PL 编译错误信息优化，增加行号和错误位置显示
- 对超大 `query limit` 增加限制，防止结果集预分配空间导致的OOM
- SQL 窗口增加对多表关联使用表别名时自动提示表名的支持

导入导出

- 支持对导出包解压修改后重新压缩的包进行导入

结果集

- oracle 模式下 `clob`，`blob`，`raw` 类型支持200KB以内的：
    - 直接数据编辑
    - 16进制编辑
    - 文件上传编辑
- mysql 模式下 `tinyblob`，`mediumblob`，`longblob`，`blob` 类型支持200KB以内的：
    - 直接数据编辑
    - 16进制编辑
    - 文件上传编辑

工具

- 命令行窗口支持arm环境下运行

体验改进

- ODC 会话过期时间可配置
- ODC 镜像内安装 `tcpdump`，支持在 docker 镜像内抓包，方便排查问题
- 帮助文档增加锚点，一键定位至指定章节

### 安全加固

- 基础镜像加固，基础镜像从 `centos7` 改为 `anolisos 8`
- 镜像和jar包安全漏洞修复
- 提供数据库白名单配置功能，防止SSRF攻击

### 缺陷修复

变更流程管控

- 终止回滚的数据库变更任务报错

SQL 执行

- PL 语句中的/被识别为结束符
- SQL 窗口 `sys_guid()` 查询结果展示值修复
- oracle 模式下 delimiter 修改为 `$` 后执行 SQL 报异常
- 查询语句带有中文别名时格式化后中文出现空格

PL 对象

- 修复程序包列表刷新过慢的问题
- 修复 `Type` 对象不能显示编译异常标识的问题
- 修复 `Procedure` 对象参数默认值缺失的问题

结果集

- 修复结果集中别名列数据编辑不生效的问题
- 修复从 excel 复制单元格内容粘贴到 ODC 时字段值多出换行的问题

## 3.3.0 (2022-04-02)

### 重要说明

ODC 3.3.0 开始提供变更流程和操作审计等企业级管控能力， WEB 版 metadb 要求有变化

- 需要 OceanBase v2.2.76 及以上版本
- 所在 OceanBase 租户规格推荐 2C8G 及以上配置

ODC 在启动时会监测 metadb 的 OceanBase 版本号，如果校验不通过会启动失败，如果您的 metadb 集群版本号小于 2.2.76，需要首先升级 metadb 集群。

### 新增功能

管控协同

- 数据库变更流程管控
    - 管理员可以配置审批流，公共连接只读权限用户可以通过审批流发起数据库变更、导入、导出、模拟数据等任务
    - 支持风险等级判定
    - 之前的异步执行升级为数据库变更，之前的导入、导出、模拟数据功能升级为和数据库变更一致的支持审批的任务流程
- 操作审计和 SQL 审计
    - 用户在 ODC 上的操作全都有迹可循，记录支持检索和导出
    - 普通用户和桌面版个人开发者和可以查看自己的操作记录

SQL/PL 执行

- SQL 窗口和命令行窗口的脚本管理统一，脚本大小限制从 `1MB` 提升到 `250MB`, `20M` 以内的脚本支持在 SQL 窗口查看编辑
- SQL 窗口支持左侧对象拖拽生成 `INSERT`/`UPDATE`/`DELETE`/`SELECT` 语句
- SQL 窗口支持独立 Session 模式
- OB 3.2.1 以上版本，PL 执行报错返回行号信息

数据源

- 支持 ODP Sharding OB MySQL 模式

工具

- MySQL 模式下支持导出函数和存储过程
- 数据库变更（原异步执行）支持上传多个脚本

对象管理

- 表对象分区查看去除 sys 租户依赖
- 优化创建索引 DDL 语句超时场景下的错误消息

数据库管理

- 回收站支持管理设置，一键开关当前会话是否打开回收站
- 回收站支持选择全部对象

体验改进

- 提供内置 HTML 文档
- 公共连接配置也支持命令行智能解析填写配置
- 支持在连接内部快速切换到其它连接
- 连接列表支持调整列宽

### 缺陷修复

对象管理

- 表对象新增约束编辑状态下，无法查看表原有的约束信息
- 新建表表名称包含特殊字符时创建索引失败
- 模拟数据切换 schema 后无法选择导出对象
- 表修改注释为空有时不生效
- 表索引的 range（GLOBAL、LOCAL）有时显示不准确
- 连接配置复制为命令行在租户为空时生成的命令行语法错误

SQL 执行

- Oracle 模式 IN 值转换使用了双引号
- Oracle 模式 SQL/PL 注释内容中包含分号时拆句错误导致报语法错误
- 结果集编辑部分场景新增列生成的 insert 语句带有 ODC 内部字段 `__ODC_INTERNAL_ROWID__`
- 导出空结果集有时会报错
- OB 3.2.3 执行计划无法查看

PL 对象

- OB 3.2.1 存储过程无法修改
- `dbms_job` 在使用 run 任务时不能通过查询结果传递的方式运行
- 部分场景下 PL、匿名块 无法调试
- 存储过程带 merge 时运行报错

工具

- Oracle 模式 username 为小写时命令行窗口无法连接

其它

- 修改密码处填写错误的原密码弹窗报错显示具体剩余次数
- 连续激活用户会被限制登录
- 公有云连接配置复制为 obclient 命令行出错

---

## 3.2.3-patch2 (2022-02-24)

> 此版本为 OceanBase 内部版本

### 新增

- 支持 OceanBase 官网在线体验
    - 增加实验室模块
    - OceanBase 官网账号集成
- API 限流，默认不开启，可按需开启
- 聚石塔行业云禁用数据导出入口
- 增加阿里云 STS 访问支持

### 缺陷修复

- 部分场景 BUC 登录会报错
- 部分高版本 chrome 浏览器下 SQL 窗口输入无法自动补全

### 安全加固

- 部分 API 存在文件路径穿越漏洞

## 3.1.3 (2022-01-28)

> 此版本为 山东移动专用版本

### 新增

- 支持视图查询结果集编辑

### 缺陷修复

- 结果集编辑生成的 DML 未包含 schema 可能会导致数据误编辑
- 升级 JDBC 驱动，修复 GBK 字符集下数据导入错误

### 安全加固

- log4j2 升级至 2.17.1 版本 [CVE-2021-44832]

---

## 3.2.3 (2022-01-06)

### 新增

- 增加新功能引导，更新核心功能介绍
- PL package 解析能力增强，可以处理更多场景
- oracle 模式结果集编辑引入隐藏 ROWID 避免数据误更新
- 高可用部署 Nginx 配置模板增加中文注释

### 功能变更

- 去除 mysql 模式下 同义词、类型 数据库对象支持
- oracle 模式 DATE 类型使用 `yyyy-MM-dd HH:mm:ss`格式，不再展示小数位

### 缺陷修复

连接会话

- Oracle 模式未赋予 dba 权限时候查询可能会 hung 住
- 变量修改未作用到 SQL 窗口
- auto commit 变量未自动同步

数据库对象

- 点击表基本属性各框后，表数据框会不停抖动
- MySQL 模式数据库名包含中划线，无法显示数据库下的对象信息
- Oracle 模式存储过程带 for 循环时解析错误导致无法执行和调试
- 函数存在参数类型相同只有参数名称不同的重载时无法执行
- 部分包含注释的存储过程解析出错导致无法执行

导入导出

- 表数据导出会包含 ROWID
- 导入时上传文件数量较多时（200+）可能会卡住
- 带库名的查询结果集修改生成的 DML 语句缺少库名导致数据无法更新
- 部分场景下 CSV 文件导入列顺序不匹配导致导入出错

其它

- 部分场景错误消息未国际化
- Nginx 配置不支持大文件上传（修改了 Nginx 配置模板）
- 模拟数据在 OB 3.x 以上的版本不支持 not null 列

### 安全加固

- log4j2 升级至 2.17.1 版本 [CVE-2021-44832]
- 敏感数据传输加密，HTTP 部署也可以防止数据在传输过程中截取

### 其它

- OSS 集成重构

## 3.2.2 patch2 (2021-12-23)

### 缺陷修复

- 表数据和结果集筛选后进行编辑生成 sql 错误可能造成数据误更新
- 列字段数量较多时列模式可能显示不全的

### 安全加固

- log4j2 升级至 2.17.0 版本 [CVE-2021-45046，CVE-2021-45105]

---

## 3.2.2 patch1 (2021-12-14)

### 安全加固

- log4j2 升级至 2.15.0 版本 [CVE-2021-44228]

---

## 3.2.2 (2021-12-08)

### 新增

SQL 执行

- SQL 执行重新实现，API 异步化，连接更稳定，连接断开时支持自动重连，长耗时语句执行网络更友好
- 执行记录支持清理
- 格式化性能优化
- 自动补全表名称支持跨 user/schema
- 执行结果 TAB 分类，非查询类结果汇总到 "日志" TAB

结果集

- 结果集支持 BLOB 大对象查看和下载
- 结果集支持多行、多列数据选择
- 结果集支持对行数据进行锁定，轻松比较数据
- 结果集列模式展示字段的备注，异步执行的结果集也支持查看
- 结果集导出 支持更多数据类型，支持大查询结果导出，支持 excel 格式导出

PL 和调试

- PL 调试重新实现，调试更顺滑，出现问题可以看到明确的错误信息，支持 proxy 连接调试
- PL 代码块自动识别，不再需要配置自定义分隔符 delimiter 了
- PL 无效对象展示详细的错误原因
- PL 编译限制告警信息

对象管理

- 表结构 支持外键展示、创建，字段名支持搜索

连接管理

- MySQL 模式默认 schema 调整为 information_schema
- 连接列表 支持按照集群、租户进行筛选，支持 “无” 筛选
- 公共连接 支持复制

安全加固

- 管理员重置用户密码后用户需要修改密码才可以激活使用
- 用户激活账号时需设置不同的密码

其它

- 脚本和异步执行长度限制 4000 调整为 500,000
- 命令行窗口选中文字背景加深
- 适配 OceanBase-CE 版本

### 缺陷修复

连接管理

- 连接管理最小化切换为最大化分页显示异常
- 连接测试不支持 oracle 小写用户名称
- 连接超时设置有时候不生效（连接仍然可能因为 server 端阻塞而无法退出）
- SQL 执行取消有时候会失败
- 公共连接只读模式也可以对部分写功能进行操作

数据库对象管理

- 创建分区不能立刻回显
- 外键约束列在跨 schema 场景下无法展现
- oracle 模式创建已存在的表失败同名表可能会被删除
- oracle 模式外键创建名称重叠
- oracle 模式 WITH TIME ZONE 数据类型查看错误
- mysql 模式 bit 类型字段值展示错误
- mysql 列的默认值未识别 NOW()/UTC_TIMESTAMP/LOCALTIME 等时间函数
- mysql 模式 oceanbase/information_schema 下的视图 DDL 不展示
- 包含 BODY 的类型内成员结构未展开
- 部分页面会出现表格抖动

SQL/PL 执行

- 超长 SQL（超过30000个字符）执行无响应
- 切换 schema 后运行存储过程报错（升级驱动修复）

异步执行

- 异步执行上传带 BOM 头的文件执行报错
- 异步执行任务日志有时无法查看
- 切换 schema 后执行异步任务，schema 切换不生效
- 服务重启时异步执行任务自动加载失败

导入导出

- mysql 模式 查询包含列别名时结果集导出会失败
- 导出任务的 空字符串转换为空值 选项不生效

命令行窗口

- 命令行窗口在粘贴内容超过 8k 时连接断开
- Windows 桌面版命令行窗口无法打开

模拟数据

- 模拟数据当连接密码为空时无法启动

其它模块

- 未激活用户用户密码输入错误也会进入激活页面
- odc-server 频繁打印 Druid 连接池错误日志

### 安全加固

- 修复最新的 CVE 高位漏洞
- WEB 版加固 docker OS 配置，禁用 root 帐密、禁用 ssh 密码登录等

### 其他变更

- 完成第一个 OAuth2 账号体系集成

---

## 3.2.1 (2021-10-26)

### 新增

- feat(security): add privilege filter.

### 其它变更

- refactor(i18n): add locale settings by request param.
- refactor(connection): refactor for setTop.

### 缺陷修复

- fix: procedure&&function name in package.
- fix(session): user can access the other's session.
- fix(websocket): add user check.
- fix(sdk): add socket info log for connection create/exception.
- fix(config): fix some issues when updating system configuration.
- fix(resourceGroup): fix issue about filter status.
- fix(privateAliyun): add security-related http headers in response.
- fix: remove IN_OUT param type and use INOUT as general.
- fix: function ddl quotation && package info source table.
- fix(buc): fix OAuth2 client auto-configuration in publicAliyun.
- fix(ram): primary user organization name fix.
- fix(iam): adjust security filter to make authWhiteList take effect.

---

## 3.2.0 (2021-09-22)

### 新增

- feat(task): add general file download API.
- feat(task): add new api to show async task result set.
- feat(iam): record login attempt, and show last login time while list users.
- feat(config): migration from odc_configuration to config_system_configuration.
- feat: add login rediector for private Aliyun ops site and role bounding.
- feat(table): table ddl contains table comment and column comment, to

# 33791235.

- feat(migrate): split migration script for support public aliyun.
- feat: add connection and query with cursor.
- feat(integration): generic API for public aliyun.
- feat(sdk): upgrade oceanbase-client from 1.1.9 to 2.2.4.
- feat(dbvariable): remove beforeSource request parameter, to

# 35441728, fix #35883769.

- feat(connect): remove OdcSessionService references.
- feat: 获取资源相关用户.
- feat(connect): decide account type from permission while create public connection.
- feat: login verification by Spring security.
- feat(config): 4A API timeout config.
- feat(connect): use organization secret for public connetion encryption.
- feat: add resource context util.
- feat(connect): add connection status check.
- feat: user role api implement.
- feat(config): implement organization configuration feature.
- feat(connect): implement session and test API.
- feat(connect): integration password encryption, implement recalculate/deleteByUserId API.
- feat(connect): implement setTop/cancelSetTop/parseCommandLine API.
- feat(connect):  implement connection persistent layer.
- feat(connect): add test and session related API.
- feat(connect): initial new connection management framework.
- feat(config): refactor application.yml to bootstrap.yml.
- feat(config): import Spring Cloud Config to manage configurations.
- feat(aci): build jar while push iterator branch, for OneAPI.
- feat(connect): 公共连接以及资源组模块的api设计.
- feat: add user&role&permission api.

### 其它变更

- refactor(admin): modify the default password for admin.
- refactor: remove druid parser and use ob parser instead.
- refactor(config): replace OdcSystemConfigService with Spring @Value.
- refactor(security): remove hardcode sensitive info from vcs.
- refactor(api): remove userId parameter from uri.
- refactor: 使用spring security自带csrf配置，删除自定义csrf filter.
- refactor: result set edit sql and info.
- refactor(connection): refactor of filtercontext.
- refactor: result set editable judgement.
- refactor: upgrade ob sql parser.

### 缺陷修复

- fix(table): fix no index ddl in Oracle mode.
- fix: name of procedure and function inside package.
- fix(package): fix enum name typo.
- fix: some character escape issue in synonym and type operation.
- fix: add several object recognize in parser listener; work around dbms output.
- fix(package): fix delete package failed when there exists only package body.
- fix: stop fetching when reaching limit.
- fix(iam): private Aliyun logout problem.
- fix(debug): refine error message while got unknown error from observer, to #36625041.
- fix(column): fix alter table add column handle default value error, fix #36581753.
- fix(migrate): fix migration failed while odc_session_manager contains illegal data row or duplicated name exists.
- fix(cloud): temporary skip authorization for public aliyun env.
- fix: script name check.
- fix(task): fix async task logic.
- fix(security): set allowLoadLocalInfile to false in Oracle mode.
- fix: jdbc execute with cursor and execute one by one.
- fix(cloud): fix error response invalid and RequestId not log issue.
- fix: upgrade oceanbase-client to 2.2.5-snapshot and supportPlDebug to 2.2.73.
- fix(sdk): refine message while sql execute failed, fix #36541831.
- fix(constraint): fix mysql mode create constraint miss name, fix# 35625478.
- fix(column): fix oracle mode invalid blank default value, fix# 35600795.
- fix(obsdk): fix connection closed judge.
- fix(connect): fix oracle mode connect failed while lowercase username and with quote in username.
- fix(column): fix oracle mode change comment failed whil nullable attribute not changed, fix #35660712.
- fix(table): fix add column failed in oracle mode, fix #36095401.
- fix(clientMode): fix odc client mode bootstrap failed.
- fix(config): sql console config not being synchronously updated after updating system config.
- fix: variable typo in user.
- fix: data type conflict and test case not cleaning.
- fix: forbid delete/update/setEnable builtin user and role.
- fix: page does not redirect to login page after logout.
- fix(connect): fix several connect bugs.
- fix: update role and migrate sqls.
- fix(framework): fix druid connection pool configuration not works issue.
- fix: delete related permission record when deleting resource_group or connection.
- fix: fix connect create session error, fix query user config error.
- fix: fix controller typo.
- fix(auth): current user related error.
- fix(connect): fix recalculate failed while reset password.
- fix(connect): fix issues found in integration debug.
- fix(config): fix merge logic and add new UT.
- fix: migrate sql about odc_user_info to iam_user.
- fix(aci): fix branch filter not works issue.
- fix(config): fix schedule check service not work.
- fix: remove cursor connection usage.

---

## 3.1.1 (2021-07-15)

### 新增

- feat(ci): release pipeline add obcloud docker push.
- feat(security): add parameter validation to protect from sql injection.
- feat(info): time api response wrap with OdcResult.
- feat(security): initial version of 3rd-party library 'Libinjection' for sql injection protection.
- feat(aci): build jar while push iterator branch, for OneAPI.
- feat(connect): connection management support passwordSaved=false, to

# 35221249.

- feat(aliyun): adapt API for public aliyun.

### 其它变更

- refactor: 优化视图创建语句部分规则.
- refactor: 优化ODC内部使用Package和Procedure名称.

### 缺陷修复

- fix(column): fix default value handle error in oracle mode.
- fix(login): fix login page error message, fix #35569105.
- fix(column): fix oracle mode handle default value does not match dict, fix #35569435.
- fix(config): fix NPE while queryLimit not set, fix #35551168.
- fix(pl_run_package): 修复在切换schema场景下PL_RUN_PACKAGE程序包无法自动创建的缺陷.
- fix(security): set allowLoadLocalInfile = false when creating connection with mysql server for security consideration.
- fix(table): fix get partition failed if connect to public aliyun address, fix #35297836.
- fix: Procedure ddl parse failed due to unproperly ddl string trim.
- fix(aci): fix branch filter not works issue.

## 3.1.0 (2021-06-24)

### 新增

- feat(deployment): add nginx conf template.
- feat(login): add failed login attempt limit.
- feat(user): add password strength validation, to #34496137.
- feat(connect): migrate empty connect password, fix #34160819.
- feat(connect): involve aes 256 encrypt for password.
- feat: metadb migrate framework.
- feat(security): involve BCrypt algorithm for odc user password.
- feat: disable user for private aliyun sub account, and fix issues found in integration.
- feat(i18n): error message i18n.
- feat(ci): integration frontend with cdn resource while develop stage.
- feat(trace): read requestId from header for alipay/privateAliyun env.
- feat(framework): attach trace info for response.
- feat(framework): initial i18n framework, expect to support error message and business message.
- feat(pop): add GetInfo API.
- feat(info): add info API.
- feat(build): add arm arch docker build in release pipeline.
- feat(distribution): refine rpm and docker verison for dev and release.
- feat(build): refine rpm and docker build.
- feat(session): session resource isolation by user.

### 其它变更

- refactor: 修复csrf token不变的问题.
- refactor: 加入csrf开关启动参数.
- refactor(build): reduce docker size.
- refactor(user): refactor user cache usage, evict cache while delete.
- refactor: refactor metadb init, change to use migrate framework.
- refactor: remove LegacyErrorCodes, use ErrorCodes instead.
- refactor(deploy): refine start-odc script.
- refactor(connection): refactor connection url parser
- refactor: refine log4j2 configuration.

### 缺陷修复

- fix(login): fix login page replaced to spring security page issue.
- fix: 修改查询类型ddl时没有传递schema参数问题；修复issue34654425.
- fix: 修复结果集编辑生成sql语法错误的缺陷.
- fix: 新建视图时无法获取其它schema下表的列信息.
- fix: 修复数据类型识别的缺陷.
- fix(procedure): 修复存储过程参数defaultValue会自动加单引号的bug.
- fix(desktop): fix x86 desktop distribution start failed, fix 34929297.
- fix(auth): refine login failed error message.
- fix(resultset): handle quote in column name and value
- fix(column): fix list column result empty for tables in non-default database, fix #34449676.
- fix: downgrade openjdk version, identify 1.8.0.262 due wrong cpu core calculate in 1.8.0.292.
- fix(dbsession): fix kill current query behind obproxy sometimes failed issue.
- fix: fix exit failed issue caused by druid hang.
- fix(migrate): fix sys user password migrate error.
- fix(aci): fix rpm_release not works, seems aci cannot read multiple }}
- fix: fix user id=0 not found and connect password not encrypted in desktop version, fix #34009391, fix #34009370.
- fix(connect): fix sys user password invalid after migrate, fix
- fix: fix UT failed due task manger init before migrate.
- fix: fix locale resolver cannot handle http header 'accept-language' issue.
- fix(aci): fix wrong RPM_RELEASE.
- fix: use openjdk for avoid start crash in KylinOS
- fix: fix issues for private aliyun integration.
- fix: fix startup failed due wrong filter implementation.
- fix(package): fix list package contains other user's object issue
- fix: upload file size config.
- fix(table): fix column info empty for table name contains special character
- fix: 优化view创建语句.
- fix: several bug fix about type, view and obclient.
- fix(trigger): fix url encode not work for path variable
- fix(connection): fix connection list page cannot show time.
- fix(deploy): fix --obclient.work.dir not works issue.
- fix(log): fix wrong log4j configuration file identify.
- fix(auth): fix private aliyun auth performance issue, and refine ut.
- fix(logout): fix logout does not clear session issue.
- fix(docker): fix oracle mode connect error.
- fix(connect): fix connection url parse error
- fix(security): remove password related info from response
- fix(sdk): remove characterEncoding setting in connection url. 
