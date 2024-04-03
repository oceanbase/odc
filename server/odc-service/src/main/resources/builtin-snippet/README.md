this directory contains the built-in snippets

## How to add a new snippet

here is an example of a snippet

```yaml
-   name:
    dialect_type:
    tags: [ 'dba','developer', 'sys', 'mysql' ]
    type: DQL
    min_version: 4.0.0
    max_version: ~
    description: ''
    prefix: short_name_for_auto_complete_in_sql_editor
    body: |
        SELECT 1 FROM DUAL:
```

some notes

- dialect_type: refer DialectType enum in odc-commons
- tags: for some special scenarios match
    - e.g. 'sys' tag for match OceanBase sys tenant
- type: optional values are DQL, DML, DDL, DCL, COMMON
- min_version: the minimum version of the database that the snippet can be used
- max_version: the maximum version of the database that the snippet can be used
- prefix: the prefix of the snippet, which will be used for auto-completion in the SQL editor
- body: the content of the snippet
    - while use character `$`, it should be escaped by `\` in the body