# task-plugin-mysql

当前插件是 ODC 任务插件的 MySQL 类型实现层。

## datatransfer

该 Module 为导数模块相关代码，导数功能涉及数据与结构的导入和导出，其中

- CSV 数据的导入和导出
- SQL 数据的导出

功能基于调用外部进程 [DataX](https://github.com/alibaba/DataX) 实现。
压缩后的 DataX 工具包存放于 `src/main/resources/datax.zip` ，运行时解压至工作目录用于调用。

### DataX 压缩包更新

1. 下载 [DataX](https://datax-opensource.oss-cn-hangzhou.aliyuncs.com/202309/datax.tar.gz) 工具包，解压至本地目录；

2. 由于工具包内包含了所有 reader 和 writer 插件，体积较大，需要删掉不必要的文件，筛选得到的 datax 目录结构如下：

```text
    datax
    ├── conf
    │   ├── core.json
    │   └── logback.xml
    ├── lib
    └── plugin
        ├── reader
        │   ├── mysqlreader
        │   └── txtfilereader
        └── writer
            ├── mysqlwriter
            └── txtfilewriter
```

`plugin` 目录下保留 ODC 导数依赖的插件即可；

3. 使用本 Module 中 `datax-conf` 目录下的 `core.json` 与 `logback.xml` 文件替换上一步得到的 `datax/conf` 目录下的对应文件；

4. 在 `datax` 父目录下执行 `zip -r datax.zip datax/` ，将产物替换项目中的压缩包即可；

5. 由于 DataX 工具在运行时会扫描该工作目录，所以在压缩时需注意清理掉所有操作系统生成的临时文件及隐藏文件，如 macos 生成的 `.DS_Store` 等。