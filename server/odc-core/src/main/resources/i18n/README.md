# i18n 资源目录

## 资源概述

包含2个 Resource Bundle

- BusinessMessages，业务消息
- ErrorMessages，错误消息

> 注意 MessageSource 需配置 setFallbackToSystemLocale(false)， 否则语言匹配可能不符合预期，比如 en_US 会映射到 zh_CN

## 关于枚举值的 i18n key 约定

使用 `com.oceanbase.odc.{SimpleClassName}.{ValueName}` 作为 i18n key，如 `ErrorCodes.Success` 的 key 为 `ErrorCodes.Success`。

## 关于字符串值的 i18n 使用（TODO）

如果一个字符串字段需要执行 i18n，可标注注解为 `@I18nField`，在 json 序列化过程中，自动进行 i18n 替换，业务代码无需关注 i18n 的细节。
