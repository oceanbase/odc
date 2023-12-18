# task-plugin-mysql

当前插件是 ODC 任务插件的 MySQL 类型实现层。

## datatransfer

该 Module 为导数模块相关代码，导数功能涉及数据与结构的导入和导出，其中

- CSV 数据的导入和导出
- SQL 数据的导出

功能基于调用外部进程 [DataX](https://github.com/alibaba/DataX) 实现。
datax 工具包维护在 https://github.com/oceanbase/odc-build-resource/tree/main/datax 。
压缩后的 DataX 工具包存放于 `src/main/resources/datax.zip` ，运行时解压至工作目录用于调用。
