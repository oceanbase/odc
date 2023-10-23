# OceanBase Developer Center (ODC) CHANGELOG

## 4.2.2 (2023-10-31)

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

### 缺陷修复

PL 调试

- 调试过程中无法跳入程序包中定义的子存储过程/函数

SQL 执行

- 执行 SQL 过程中持续执行 "DROP PACKAGE" 语句导致大量报错
- 连接 OceanBase MySQL 租户时自动调用 "obodc_procedure_feature_test" 存储过程导致报错或进入连接缓慢

SQL-Check

- OceanBase Oracle 租户下创建 type 时，如果子存储过程/函数无参数列表，SQL Check 报语法错误

数据脱敏

- SELECT 语句中含有多表 JOIN 的场景下脱敏失败
- 大小写敏感的 OceanBase MySQL 模式下无法识别到敏感列导致脱敏失效 

数据库对象管理

- 没有 show create view 权限用户查看视图详情时报错
- 查看表对象时所有字符类型的长度无法显示

数据库变更

- 数据库变更任务超时时间设置无效

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