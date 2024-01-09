# ob-sql-parser

## 什么是 ob-sql-parser

`ob-sql-parser`是基于`antlr4`构建的 SQL 解析器，它的主要功能是将 SQL 语句文本翻译成 Java Pojo 类。

通常情况下，`antlr4`可以将 SQL 的形式化文本解析成一棵**抽象语法树（AST）**，然而 AST 是较为抽象的数据结构，与形式化文本内容的具体特征无关。用户如果想获取到 SQL 中的一些关键信息（例如`SELECT`中的具体了查询了哪些列），就必须对 AST 的结构有较为清晰的了解，然后通过遍历 AST 的方式在特定的节点上获取信息。

上述方式是复杂且低效的，且要求使用者必须有一定的语法分析背景知识。鉴于此，`ob-sql-parser`尝试解决这个问题，并主要做了两方面工作：

1. 对 SQL 进行抽象，将不同的 SQL 句型抽象成不同的 Java 类，把 SQL 中的关键信息封装在类中，使用者可以直接面向 pojo 类编程。
2. 编写适配器对 AST 进行遍历，将 AST 转化为对应的 Java 抽象类。

Sample SQL Statement:

```SQL
SELECT tb.col FROM tb WHERE col1='abc'  
```

Java Pojo Class:

```Text
SQL Text
  └──Statement: Select
       └──selectBody: SelectBody
            ├──selectItems: List<Projection>
            │    └──Projection
            |         └──column: Expression
            |              └──RelationReference: tb.col
            ├──froms: List<FromReference>
            │    └──NameReference: tb
            └──where: Expression
                 └──CompoundExpression: col1='abc'
```

## 支持的方言及语法

`ob-sql-parser`的目标是支持所有 OceanBase 方言类型以及全部 SQL 句型，任意不支持的 SQL 句型都可以被视为缺陷，将在未来被纳入到支持列表中。目前已经支持的方言及 SQL 句型如下：

|Dialects|Statements|
| :---- | :---- |
|OceanBase Oracle<br>OceanBase MySQL|`SELECT`<br>`WITH`<br>`CREATE TABLE`<br>`INSERT`,`UPDATE`,`DELETE`<br>`CREATE INDEX`<br>`ALTER_TABLE`<br>`RENAME_TABLE`|

## 安装部署

`ob-sql-parser`的代码目前存放于 [OceanBase Developer Center](https://github.com/oceanbase/odc.git) 的代码仓库中，用户可以通过如下 shell 脚本获取`ob-sql-parser`的代码：

```shell script
$ git clone https://github.com/oceanbase/odc.git
$ cd ob-odc/libs/ob-sql-parser
```

除了源码级别的使用外，`ob-sql-parser`目前已经上传到 maven 中央仓库中，使用者可以通过直接增加 maven dependency 的方式引用该模块：

```
<dependency>
    <groupId>com.oceanbase</groupId>
    <artifactId>ob-sql-parser</artifactId>
    <version>1.2.0</version>
</dependency>
```

## 解析 SQL 语句

将`SELECT`语句解析成 Java 对象：

```Java
String sql = "select tb.col from tb where col1='123'";

Select select = (Select) new OBMySQLParser().parse(new StringReader(sql));

Projection projection = select.getSelectBody().getSelectItems().get(0);
Assert.assertEquals(new ColumnReference(null, "tb", "col"), projection.getColumn());

NameReference table = (NameReference) select.getSelectBody().getFroms().get(0);
Assert.assertEquals("tb", table.getRelation());

CompoundExpression where = (CompoundExpression) select.getSelectBody().getWhere();
Assert.assertEquals(new ColumnReference(null, null, "col1"), where.leftExpr());
Assert.assertEquals(new ConstExpression("'123'"), where.rightExpr());
Assert.assertEquals(Operator.EQ, where.operator());
```

将`CREATE_TABLE`语句解析成 Java 对象：

```Java
String sql = "create table non_partrange_hash_pri ("
             + "    col1 int,"
             + "    col2 numeric(10,2),"
             + "    col3 varchar(10),"
             + "    col4 blob,"
             + "    col5 year,"
             + "    constraint pk_non_partrange_hash_pri primary key(col1,col6)"
             + ") partition by range(col2) subpartition by hash(col1) ("
             + "    partition p1 values less than(0)("
             + "        subpartition p1_1,"
             + "        subpartition p1_2"
             + "    ),"
             + "    partition p2 values less than(10000)("
             + "        subpartition p2_1,"
             + "        subpartition p2_2"
             + "    ),"
             + "    partition p3 values less than(100000)("
             + "        subpartition p3_1,"
             + "        subpartition p3_2"
             + "    ),"
             + "    partition p4 values less than (maxvalue)("
             + "        subpartition p4_1"
             + "    )"
             + ");";
CreateTable createTable = (CreateTable) new OBMySQLParser().parse(new StringReader(sql));

Assert.assertEquals("non_partrange_hash_pri", createTable.getTableName());

List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();
Assert.assertEquals(5, columnDefinitions.size());

ColumnDefinition columnDefinition = columnDefinitions.get(1);
Assert.assertEquals("col2", columnDefinition.getColumnReference().getColumn());
Assert.assertEquals("numeric", columnDefinition.getDataType().getName());
Assert.assertEquals(Arrays.asList("10", "2"), columnDefinition.getDataType().getArguments());

Partition partition = createTable.getPartition();
Assert.assertEquals(Collections.singletonList(new ColumnReference(null, null, "col2")), partition.getPartitionTargets());

List<? extends PartitionElement> partitionElements = partition.getPartitionElements();
Assert.assertEquals(4, partitionElements.size());
PartitionElement partitionElement = partitionElements.get(0);

List<SubPartitionElement> subPartitionElements = partitionElement.getSubPartitionElements();
Assert.assertEquals(2, subPartitionElements.size());
SubPartitionElement subPartitionElement = subPartitionElements.get(0);
Assert.assertEquals("p1_1", subPartitionElement.getRelation());
```

## Reference

1. 本项目的`src/main/resources/oracle`目录中的 g4 文件来源于 antlr4 语法库：[antlr/grammars-v4](https://github.com/antlr/grammars-v4/tree/master/sql/plsql)

## FAQ

> Q: 支持哪个版本的 OceanBase 语法？

目前，`ob-sql-parser`兼容 OceanBase 4.2.1 版本的语法。

> Q: 支持哪些 OceanBase 方言？

目前，`ob-sql-parser`支持 OceanBase MySQL 以及 OceanBase Oracle 两种方言类型。

> Q: 对不同 SQL 句型的支持现状是怎么样的？

截止目前，支持将所有类型的 SQL 转化为 AST；从 SQL 文本到 Java Pojo 类的转化来说，目前支持力度如下：

|SQL 句型|支持力度|备注|
|:----|:----|:----|
|`SELECT`|95%|`SELECT...INTO...`句型不支持|
|`DELETE`|50%|-|
|`UPDATE`|50%|-|
|`INSERT`|100%|语法文件上的所有分支全部支持|
|`CREATE_TABLE`|100%|语法文件上的所有分支全部支持|
|`CREATE_INDEX`|100%|语法文件上的所有分支全部支持|
|`ALTER_TABLE`|100%|语法文件上的所有分支全部支持|
|`RENAME_TABLE`|100%|语法文件上的所有分支全部支持|

> Q: 是否支持 SQL 脚本解析（脚本中包含多句 SQL）？

不支持，目前仅支持单句 SQL 解析，如果是包含多句 SQL 的脚本，需要使用者自行拆句。

> Q: 如何从抽象 Java 类获取对应的 SQL 文本？

从抽象 Java 类到 SQL 文本有 2 种方式：

1. 调用 Java 类的 `Object#toString()` 方法。需要注意的是：该方法返回的语句是根据 Pojo 类中封装的信息动态生成的，无法保证和原始 SQL 完全等效。
2. 调用 Java 类的 `Statement#getText()`方法，该方法获取到的是该 Pojo 类对应的原始 SQL 文本。

> Q: OceanBase MySQL 以及 OceanBase Oracle 两种方言下产生的 Java 封装类是同一套吗？还是不同的方言有不同的实现？

不同的方言下，`ob-sql-parser`产生的 Java Pojo 类是同一套，没有分开抽象，不同方言的差异部分是同时体现在 Pojo 类的抽象中的。仅在很少情况下会发生 OceanBase MySQL 和 OceanBase Oracle 方言在同一场景下返回不同的 Pojo 抽象，这主要是因为二者确实存在的且无法兼容的差异性导致，可以具体情况具体分析。