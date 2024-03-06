/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.sqlcheck.model;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.i18n.Translatable;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link SqlCheckRuleType}
 *
 * @author yh263208
 * @date 2022-11-16 17:14
 * @since ODC_release_4.1.0
 */
@Getter
public enum SqlCheckRuleType implements Translatable {
    /**
     * 存在语法错误
     */
    SYNTAX_ERROR("syntax-error"),
    /**
     * 索引列计算，{@code select * from tab where id+1=10} 如果 id 是索引列的话这样的计算会导致索引失效。
     */
    INDEX_COLUMN_CALCULATION("index-column-calculation"),
    /**
     * 索引列上使用左模糊匹配，{@code select * from tab where id like '%xxxxx'} 如果 id 是索引的话可能会导致索引失效。
     */
    INDEX_COLUMN_FUZZY_MATCH("index-column-fuzzy-match"),
    /**
     * 索引列上存在隐式类型转换, 这会导致索引失效。
     */
    INDEX_COLUMN_IMPLICIT_CONVERSION("index-column-implicit-conversion"),
    /**
     * in 条件中存在过多表达式（超过 200 个）造成性能下降。
     */
    TOO_MANY_IN_EXPR("too-many-in-expr"),
    /**
     * 一个查询中 join 的表数量太多（超过 10 个），会导致查询计划无法最优。
     */
    TOO_MANY_TABLE_JOIN("too-many-table-join"),
    /**
     * not in 子句需要添加 not null 标记，避免出现嵌套循环。
     */
    NO_NOT_NULL_EXISTS_NOT_IN("no-not-null-exists-not-in"),
    /**
     * update/delete 语句中 where 条件恒为真/假。
     */
    NO_VALID_WHERE_CLAUSE("no-valid-where-clause"),
    /**
     * update/delete 语句中没有 where 条件。
     */
    NO_WHERE_CLAUSE_EXISTS("no-where-clause-exists"),
    /**
     * insert/replace 语句中没有具体的列。
     */
    NO_SPECIFIC_COLUMN_EXISTS("no-specific-column-exists"),
    /**
     * update/delete/select 语句不带索引键，导致 tableScan，性能变差。
     */
    NO_INDEX_KEY_EXISTS("no-index-key-exists"),
    /**
     * 分区表的操作不带分区键，导致无法进行分区剪裁，造成性能差。
     */
    NO_PARTITION_KEY_EXISTS("no-partition-key-exists"),
    /**
     * 索引过多，反而导致性能下降。
     */
    TOO_MANY_INDEX_KEYS("too-many-index-keys"),
    /**
     * 推荐局部索引。
     */
    PREFER_LOCAL_INDEX("prefer-local-index"),
    /**
     * 建表语句中存在过多的列。
     */
    TOO_MANY_COLUMNS("too-many-columns"),
    /**
     * 表的 DDL 定义中 {@code char} 类型的长度过长
     */
    TOO_LONG_CHAR_LENGTN("too-long-char-length"),
    /**
     * 限制唯一索引名称的命名格式
     */
    RESTRICT_UNIQUE_INDEX_NAMING("restrict-unique-index-naming"),
    /**
     * 建表 DDL 中不允许有外键存在
     */
    FOREIGN_CONSTRAINT_EXISTS("foreign-constraint-exists"),
    /**
     * 建表 DDL 中不存在主键约束
     */
    NO_PRIMARY_KEY_EXISTS("no-primary-key-exists"),
    /**
     * 建表 DDL 中没有写表注释
     */
    NO_TABLE_COMMENT_EXISTS("no-table-comment-exists"),
    /**
     * 表的命名在黑名单中
     */
    TABLE_NAME_IN_BLACK_LIST("table-name-in-black-list"),
    /**
     * 限制建表的字符集
     */
    RESTRICT_TABLE_CHARSET("restrict-table-charset"),
    /**
     * 限制表的排序规则
     */
    RESTRICT_TABLE_COLLATION("restrict-table-collation"),
    /**
     * 单个索引中引用列数目的上限值
     */
    TOO_MANY_COL_REFS_IN_INDEX("too-many-column-refs-in-index"),
    /**
     * 主键索引/约束允许的数据类型
     */
    RESTRICT_PK_DATATYPES("restrict-pk-datatypes"),
    /**
     * 索引允许的数据类型
     */
    RESTRICT_INDEX_DATATYPES("restrict-index-datatypes"),
    /**
     * 单个主键中引用列数目的上限值
     */
    TOO_MANY_COL_REFS_IN_PRIMARY_KEY("too-many-column-refs-in-primary-key"),
    /**
     * 限制主键自增
     */
    RESTRICT_PK_AUTO_INCREMENT("restrict-pk-auto-increment"),
    /**
     * 限制普通索引名称的命名格式
     */
    RESTRICT_INDEX_NAMING("restrict-index-naming"),
    /**
     * 限制主键索引名称的命名格式
     */
    RESTRICT_PK_NAMING("restrict-pk-naming"),
    /**
     * 索引没有命名
     */
    NO_INDEX_NAME_EXISTS("no-index-name-exists"),
    /**
     * 列定义中包含 zerofill
     */
    ZEROFILL_EXISTS("zerofill-exists"),
    /**
     * 列定义中包含字符集配置
     */
    COLUMN_CHARSET_EXISTS("column-charset-exists"),
    /**
     * 列定义中包含排序规则配置
     */
    COLUMN_COLLATION_EXISTS("column-collation-exists"),
    /**
     * 列为空
     */
    COLUMN_IS_NULLABLE("column-is-nullable"),
    /**
     * 列没有默认值
     */
    NO_DEFAULT_VALUE_EXISTS("no-default-value-exists"),
    /**
     * 列没有注释
     */
    NO_COLUMN_COMMENT_EXISTS("no-column-comment-exists"),
    /**
     * 列命名在黑名单中
     */
    COLUMN_NAME_IN_BLACK_LIST("column-name-in-black-list"),
    /**
     * 列命名的大小写设置
     */
    RESTRICT_COLUMN_NAME_CASE("restrict-column-name-case"),
    /**
     * 表命名的大小写设置
     */
    RESTRICT_TABLE_NAME_CASE("restrict-table-name-case"),
    /**
     * 限制表子增列初始值
     */
    RESTRICT_TABLE_AUTO_INCREMENT("restrict-table-auto-increment"),
    /**
     * select 语句中存在 *
     */
    SELECT_STAR_EXISTS("select-star-exists"),
    /**
     * 必要的列名不存在
     */
    MISSING_REQUIRED_COLUMNS("missing-required-columns"),
    /**
     * 限制自增列必须是无符号类型
     */
    RESTRICT_AUTO_INCREMENT_UNSIGNED("restrict-auto-increment-unsigned"),
    /**
     * 限制自增列必须是无符号类型
     */
    TOO_MANY_ALTER_STATEMENT("too-many-alter-statement"),
    /**
     * 标记为 not null 的列没有默认值
     */
    NOT_NULL_COLUMN_WITHOUT_DEFAULT_VALUE("not-null-column-without-default-value"),
    /**
     * 禁用数据类型存在
     */
    PROHIBITED_DATATYPE_EXISTS("prohibited-datatype-exists"),
    /**
     * 不允许执行删除的数据库对象类型
     */
    RESTRICT_DROP_OBJECT_TYPES("restrict-drop-object-types"),
    /**
     * 没有主键约束的命名
     */
    NO_PRIMARY_KEY_NAME_EXISTS("no-primary-key-name-exists"),
    /**
     * 限制被声明为 auto-increment 的列的类型
     */
    RESTRICT_AUTO_INCREMENT_DATATYPES("restrict-auto-increment-datatypes");

    private final String name;
    private static final String NAME_CODE = "name";
    private static final String DESP_CODE = "description";
    private static final String MESSAGE_CODE = "message";

    SqlCheckRuleType(@NonNull String name) {
        this.name = name;
    }

    @Override
    public String code() {
        return this.name;
    }

    public String getLocalizedMessage(Object[] args) {
        return translate(args, MESSAGE_CODE);
    }

    public String getLocalizedName() {
        return translate(new Object[] {}, NAME_CODE);
    }

    public String getName() {
        return prefix() + "." + NAME_CODE;
    }

    public String getLocalizedDescription() {
        return translate(new Object[] {}, DESP_CODE);
    }

    /**
     * get translated message
     *
     * @param args args referenced by i18n message template defined in i18n resource files
     * @return translated message
     */
    private String translate(Object[] args, String subtype) {
        String key = prefix() + "." + subtype;
        return I18n.translate(key, args, key, LocaleContextHolder.getLocale());
    }

    private String prefix() {
        return I18N_KEY_PREFIX + "builtin-resource.regulation.rule.sql-check." + code();
    }

}
