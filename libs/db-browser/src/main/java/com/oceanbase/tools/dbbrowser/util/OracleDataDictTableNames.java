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
package com.oceanbase.tools.dbbrowser.util;

/**
 * Oracle 模式数据字典表名称
 */
public interface OracleDataDictTableNames {
    /**
     * may DBA_ / USER_ / ALL_
     */
    String prefix();

    /**
     * 表
     */
    String TABLES();

    /**
     * 视图
     */
    String VIEWS();

    /**
     * 表的列，和 TAB_COLS 的差别在于 TAB_COLUMNS 不包含系统自动生成的列
     */
    String TAB_COLUMNS();

    /**
     * 表和视图的列的注释
     */
    String COL_COMMENTS();

    /**
     * 表的列，和 TAB_COLUMNS 的差别在于 TAB_COLS 包含系统自动生成的列
     */
    String TAB_COLS();

    /**
     * 表和视图的注释
     */
    String TAB_COMMENTS();

    /**
     * 约束
     */
    String CONSTRAINTS();

    /**
     * 约束的列
     */
    String CONS_COLUMNS();

    /**
     * 索引
     */
    String INDEXES();

    /**
     * 索引的列
     */
    String IND_COLUMNS();

    /**
     * 函数索引列表达式
     */
    String IND_EXPRESSIONS();

    /**
     * 分区
     */
    String TAB_PARTITIONS();

    /**
     * 分区的列
     */
    String PART_KEY_COLUMNS();

    /**
     * 分区表
     */
    String PART_TABLES();

    /**
     * PL 对象的 TEXT 部分，包括 FUNCTION, PACKAGE, PACKAGE BODY, PROCEDURE, TRIGGER, TYPE, TYPE BODY
     */
    String SOURCE();

    /**
     * 所有对象
     */
    String OBJECTS();

    /**
     * PL 对象的参数
     */
    String ARGUMENTS();

    /**
     * 序列
     */
    String SEQUENCES();

    /**
     * 同义词
     */
    String SYNONYMS();

    /**
     * 触发器
     */
    String TRIGGERS();

    /**
     * 类型
     */
    String TYPES();

    String SEGMENTS();

    /**
     * 用户
     */
    String USERS();
}
