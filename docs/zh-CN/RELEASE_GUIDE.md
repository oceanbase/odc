## 发布检查清单

checklist

- [ ] all features has been verified pass
- [ ] all critical bugs has been fixed
- ...

## 变更日志 CHANGELOG 维护

CHANGELOG 维护在 CHANGELOG.md

- 通过 gitchangelog 工具自动生成草稿 CHANGELOG.rst，用于辅助 CHANGELOG.md 编写
- 通过手工维护 CHANGELOG.md 文件提供产品文档级别的变更日志

安装 gitchangelog

```shell
pip install gitchangelog
```

删除所有本地 tag，拉取远端 tag

```shell
git tag -l | xargs git tag -d && git fetch -t
```

生成 CHANGELOG.rst 文件

```shell
gitchangelog > CHANGELOG.rst
```

## 使用 Github CI 发起构建

> bla bla bla 

