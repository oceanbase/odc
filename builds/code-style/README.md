# code style

## 文件说明

- `eclipse-java-oceanbase-style.xml` 本项目使用的代码格式化配置，eclipse code formatter 配置文件
    - 基于 [eclipse-java-google-style.xml](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml) 配置修改得到
- `eclipse-java-oceanbase.importorder` eclipse code formatter importorder 配置文件
- `IDEA_code_style_oceanbase.xml` IDEA code style 配置, 和上述 eclipse code formatter 配置兼容

## code format 配置指南

为统一代码格式，采用 Eclipse Code Formatter 格式化代码

- Eclipse可直接使用
- IDEA 通过 Eclipse Code Formatter 插件使用
- 编译时通过 maven 插件格式化代码

### IntelliJ IDEA 配置

> 首先需安装 Eclipse Code Formatter 插件

配置界面位于 Other Settings > Eclipse Code Formatter

- 选择 Use the Eclipse code formatter
- Eclipse java Formatter config file 选择 `build/code-style/eclipse-java-oceanbase-style.xml`
    - Profile 选择 `oceanbase-java-format`
- [x] Optimize Imports
    - From file，选择 `builds/code-style/eclipse-java-oceanbase.importorder`

### maven plugin 配置

本项目使用以下 maven plugin 在 compile 阶段执行代码格式化

- [import sort 插件](https://code.revelc.net/impsort-maven-plugin/)
- [formatter 插件](https://code.revelc.net/formatter-maven-plugin/)

maven plugin 的配置位于 [${project.parent.basedir}/pom.xml](../../pom.xml)

> 注意 import 插件在前 formatter 插件在后，在 formatter 插件配置了换行符统一为 Unix 风格的 `LF`。
