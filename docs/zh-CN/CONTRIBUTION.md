# 参与到 OceanBase Developer Center 项目

👀 哇哦！很高兴你打开这个文档。

如果你有兴趣参与到 OceanBase Developer Center 项目，不管是提意见和建议甚至批评，或者是提报 bug，参与代码贡献，我们都非常欢迎 👏👏。

> 本文尝试为参与 ODC 项目提供一个指导，只是指导不是强制的规则，如果你有更好的建议，也可以直接提 PR 修改这个文档。

#### 目录

- [行为准则](#code-of-conduct)
- [问题咨询或讨论](#questions-or-discussion)
- [参与贡献代码需要先了解](#before-contribution)
- [如何报告缺陷？](#reporting-bugs)
- [如何 Pull Requests](#pull-requests)

# 1. 行为准则 <a id='code-of-conduct'></a>

项目的所有参与者都需要遵守 [行为准则](CODE_OF_CONDUCT.md)，参与贡献本项目表示您同意遵守此行为准则。
如果遇到不可接受的行为，请联系 [yizhou.xw@oceanbase.com](mailto:yizhou.xw@oceanbase.com) 报告。

# 2. 问题咨询或讨论 <a id='questions-or-discussion'></a>

**注意:** 请不要通过提交 github issue 来提问，我们推荐通过以下途径，您可以更快的得到结果或响应。

* 如果是在使用产品过程中遇到问题，请首先查看 [常见问题文档](https://github.com/oceanbase/odc-doc/blob/V4.2.0/zh-CN/1300.troubleshooting/100.general-troubleshooting.md) 是否有相同问题的解答。
* 对产品的意见和建议，请通过 [Discussions](https://github.com/oceanbase/odc/discussions) 发起讨论。

# 3. 参与贡献代码需要先了解 <a id='before-contribution'></a>

## 3.1 代码仓库和结构 <a id='git-repo-structure'></a>

本仓库的目录结构说明如下。

```shell
.
├── builds                 构建环境配置
│   └── code-style         代码格式化配置
├── client                 前端代码
├── distribution           发布配置
│   ├── docker
│   ├── jar
│   ├── plugins
│   └── rpm
├── docs                   文档
│   ├── en-US      
│   └── zh-CN
├── import                 通过可执行文件方式引入的外部组件
├── libs                   ODC 使用的基础类库，也可以同时给其他项目使用
│   ├── db-browser
│   └── ob-sql-parser
├── script                 常用脚本，用于准备开发环境、构建程序、启动服务等
├── server                 后端代码
│   ├── 3rd-party          通过源码引入的第三方程序包
│   │   └── Libinjection   来源 https://github.com/jeonglee/Libinjection 因源库未发布 jar 到 maven 仓库只能源码引用
│   ├── integration-test   集成测试
│   ├── odc-common         工具类库
│   ├── odc-core           偏技术框架和具体业务无关的核心模块
│   ├── odc-server         Server 模块，包含 WEB 框架和请求路由配置
│   ├── odc-service        Service 模块，包含业务功能实现
│   ├── odc-test           测试工具类库，做成单独的模块是为了方便其他模块的测试代码复用
│   ├── plugins            插件的接口定义和具体插件实现
│   │   ├── connect-plugin-api
│   │   ├── connect-plugin-mysql
│   │   ├── connect-plugin-ob-mysql
│   │   ├── connect-plugin-ob-oracle
│   │   ├── sample-plugin
│   │   ├── sample-plugin-api
│   │   ├── schema-plugin-api
│   │   ├── schema-plugin-mysql
│   │   ├── schema-plugin-ob-mysql
│   │   ├── schema-plugin-ob-oracle
│   │   └── sample-plugin-odp-sharding-ob-mysql
│   ├──  starters           不同发行版本的启动配置
│   │   ├── desktop-starter 桌面版启动配置
│   │   └── web-starter     WEB 版启动配置
│   └── test-script         常用测试脚本，用于执行、分析测试用例
```

## 3.2 学习开发指南 <a id='learn-developer-guide'></a>

请学习 [开发指南](./DEVELOPER_GUIDE.md)，了解如何搭建开发环境、构建、启动、调试。

## 4. 报告缺陷 <a id='reporting-bugs'></a>

> **注意：**如果您发现一个**已关闭**的问题与您遇到的问题相同，
> 打开一个新问题，并在新问题的正文中包含指向原始问题的链接。

## 4.1 先确认问题

1. 检查 [常见问题文档](https://github.com/oceanbase/odc-doc/blob/V4.2.0/zh-CN/1300.troubleshooting/100.general-troubleshooting.md) ，确认这是否是预期行为。
1. 做一些分析甚至可以调试程序，尝试找出问题的根本原因。
1. 确定应该在哪个仓库中报告问题。
1. 检查已有的问题，确认这是否是一个新的错误。

## 4.2 提交缺陷的最佳实践

提供准确、完整的场景描述信息和复现步骤对于缺陷的沟通非常重要。

### 4.2.1 基于问题现象的缺陷标题

一个准确的标题是理解缺陷的第一步。很多时候我们会忽略这一点，一些不佳实践可能包括：

- 基于原因而不是现象。
- 过于简单。

我们建议

- 标题应使用英文。
- 缺陷的标题应当基于问题现象进行描述，并且尽可能准确。
- 标题可包含模块、版本等名称，使用方括号包围，方便快速判断问题范围。

几个正确的示例

```text
Windows App crashed while startup, got 'chunk not found' error in odc.log
[PL] PL execute got 'timeout of 3000000ms exceeded' error
[Table][OceanBase Oracle] NullPointerException while view temporary table
```

几个错误的示例

```text
新建数据源报序列化异常 (没有使用英文)
Crashed (过于简单，需要具体一些，如 )
Remove heartbeat mechanism （描述的是方案，而不是问题）
```

### 4.2.2 正确完整的描述缺陷

请使用以下缺陷模板提报 issue。

```markdown
[问题概述]

[环境和版本]

- ODC 版本：
- 数据库版本：
- 操作系统版本：
- 数据库配置信息：

[复现步骤]
问题复现的完整步骤。

[相关信息]
包括错误截图、程序日志等信息，日志内容如果较大请使用附件方式上传。
```

## 5. 提 Pull request <a id='pull-requests'></a>

Pull Request（以下简称 PR）是 ODC 开发过程中，个人代码合并到迭代分支、迭代分支和主干分支同步的 必须过程。

有效且高效的 PR 过程并不容易，期望本文描述的规范能够有助于达到以下效果：

- PR 的 review 过程，提出人可以清晰的描述 PR 内容，评审人可以容易读懂 PR；
- PR 可以和迭代的工作项（主要是需求和缺陷）形成清晰的关联关系；
- 质量同学通过 PR 描述可以了解到代码变更涉及的范围，容易识别出对应的测试覆盖范围；
- PR 的讨论过程氛围严肃又活泼，促进相互学习和成长；
- 最终合并到 迭代分支、主干 的 commit 历史记录可读性非常好，可以自动化生成 CHANGELOG。

### 5.1 提 PR 之前

无论是缺陷修复还是功能实现，每个 PR 都需要关联 github issue。

- 首先请确认是否已经创建了相关 issue，如果还没有，则先创建 issue。
- 在决定处理某个 issue 前请先与项目成员沟通，确定认领 issue，assign 给自己。
- 对于缺陷修复，确保已有测试用例全部可以通过，并且相关缺陷完成自测验证。
- 对于功能实现，确保需求文档和概要设计文档已经通过评审。

### 5.2 PR 标题

Pull Request 有 3 个场景

- fix bug
- new feature
- merge from branch to another

#### 5.2.1 fix bug/new feature 标题

其中 fix bug/new feature 的 PR，遵循 Conventional Commits 给出的 [git commit template](https://www.conventionalcommits.org/en/v1.0.0/#summary) 。

PR 标题格式为 `<type>[optional scope]: <description>`，
注意这里的 `<type>` 尖括号是不需要的，尖括号只是表示变量，
其可选值是 feat/fix/refactor/style/ci/docs/test/...

> IDEA 可安装插件辅助生成 符合规范的 git commit message，
> 推荐使用插件 [git-commit-template](https://plugins.jetbrains.com/plugin/9861-git-commit-template) 。

正确的标题示例

```text
fix(table): fix get partition failed if connect to public aliyun address
feat(security): add parameter validation to protect from sql injection
```

错误的标题示例

```text
table constraint
4.2.0 delimiter fix someusername
feat<table>: support column rename
```

#### 5.2.2 分支间合并 PR 标题

标题模板为 `merge  <source_branch> into <targer_branch>` 。

样例

- merge 3.1.x_dev into master
- merge master into 3.2.x_dev
- merge 3.1.1_feature into 3.2.x_dev

### 5.2.3 缺陷修复 PR 描述模板

缺陷修复 PR 描述请参考此模板。

```markdown
#### 缺陷描述

<< 描述缺陷内容、原因、涉及版本等信息 >>

#### 修复方案

<< 概述修复方案，如果修复的代码涉及其他模块，在这里给出说明 >>

#### 测试说明

<< 已测试case、测试方法、建议覆盖场景 >>

#### 规避方案

<< 此缺陷如果能够规避，给出说明，如果不需要说明填写“无” >>

#### 其它信息

<< 其他需要说明的信息，如果不需要说明填写“无” >>
```

### 5.2.4 功能实现 PR 描述模板

功能实现 PR 描述请参考此模板。

```markdown
#### 实现概述

<< 一段或多段讲述 PR 详情，对于复杂的实现，可包含对应设计文档的链接 >>

#### 相关模块

<< PR 涉及的相关模块，QA 关注这部分内容判断测试覆盖范围 >>

#### 测试建议

<< 测试注意事项和场景建议 >>

#### TODO

<< 仍待解决的问题 >>
```

### 5.3 高效 PR 注意事项

#### 5.3.1 规模和范围

PR 是需要其它同学 Review 的，单个 PR 的变更规模不宜太大

- 单个 PR 的 Review 耗时应控制在2 小时以内
- PR 变更代码行数不做强制要求，尽量不超过 500 行
- 对于缺陷修复，不相关的缺陷修复不应合并到同一个 PR 内
    - 即便有的缺陷只是修改一行代码，也应当单独提 PR
    - 如果多个缺陷的修复是相关的， 技术上无法拆分或者拆分 PR 将导致更难理解，则合并为一个 PR

#### 5.3.2 礼貌且聚焦

PR 讨论过程，可以有不同的收获，绝对不仅是找茬的过程，更是学习交流的过程

我们鼓励

- 称赞觉得不错的代码
- 提问看不懂的代码
- 交流解决问题的更佳方案
- 分享编程的更佳实践
- 探讨更好的命名
- 接受 PR 意见后给予响应回复
- ...

我们不鼓励

- 纠结代码格式，比如空行、缩进、换行 等等，这个部分交给自动格式化来做
- 不仔细 review 直接点通过
- ...

#### 5.3.3 协作和反馈

PR 是一个协作的过程，需要及时响应 Reviewer 的反馈

- 提高响应速度：PR 提交后，应该尽快响应 Reviewer 的反馈，避免 Reviewer 等待过长时间。
- 沟通和讨论：如果有任何疑问或者需要讨论的问题，应该及时进行沟通和讨论，避免出现误解或者不必要的延误。
- 解决问题：如果 Reviewer 提出了问题或者改进意见，应该积极解决问题，确保 PR 的质量和有效性。
