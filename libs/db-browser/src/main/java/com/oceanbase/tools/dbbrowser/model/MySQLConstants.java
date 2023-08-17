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
package com.oceanbase.tools.dbbrowser.model;

/**
 * @Author: Lebie
 * @Date: 2022/6/23 下午8:12
 * @Description: []
 */
public class MySQLConstants {

    public static final String TABLE_INFORMATION_SCHEMA = "information_schema";

    public static final String META_TABLE_ENGINES = TABLE_INFORMATION_SCHEMA + ".engines";
    public static final String META_TABLE_SCHEMATA = TABLE_INFORMATION_SCHEMA + ".schemata";
    public static final String META_TABLE_TABLES = TABLE_INFORMATION_SCHEMA + ".tables";
    public static final String META_TABLE_ROUTINES = TABLE_INFORMATION_SCHEMA + ".routines";
    public static final String META_TABLE_TRIGGERS = TABLE_INFORMATION_SCHEMA + ".triggers";
    public static final String META_TABLE_COLUMNS = TABLE_INFORMATION_SCHEMA + ".columns";
    public static final String META_TABLE_TABLE_CONSTRAINTS = TABLE_INFORMATION_SCHEMA + ".table_constraints";
    public static final String META_TABLE_KEY_COLUMN_USAGE = TABLE_INFORMATION_SCHEMA + ".key_column_usage";
    public static final String META_TABLE_STATISTICS = TABLE_INFORMATION_SCHEMA + ".statistics";
    public static final String META_TABLE_PARTITIONS = TABLE_INFORMATION_SCHEMA + ".partitions";
    public static final String META_TABLE_VIEWS = TABLE_INFORMATION_SCHEMA + ".views";

    public static final String COL_SCHEMA_NAME = "SCHEMA_NAME";
    public static final String COL_DEFAULT_CHARACTER_SET_NAME = "DEFAULT_CHARACTER_SET_NAME";
    public static final String COL_DEFAULT_COLLATION_NAME = "DEFAULT_COLLATION_NAME";
    public static final String COL_CHARACTER_SET_NAME = "CHARACTER_SET_NAME";
    public static final String COL_COLLATION_NAME = "COLLATION_NAME";

    public static final String COL_TABLE_SCHEMA = "TABLE_SCHEMA";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_TABLE_TYPE = "TABLE_TYPE";
    public static final String COL_ENGINE = "ENGINE";
    public static final String COL_VERSION = "VERSION";
    public static final String COL_ROWS = "ROWS";
    public static final String COL_TABLE_ROWS = "TABLE_ROWS";
    public static final String COL_AUTO_INCREMENT = "AUTO_INCREMENT";
    public static final String COL_TABLE_COMMENT = "COMMENT";
    public static final String COL_COLUMNS_NAME = "COLUMNS_NAME";
    public static final String COL_ORDINAL_POSITION = "ORDINAL_POSITION";
    public static final String COL_CREATE_TIME = "CREATE_TIME";
    public static final String COL_UPDATE_TIME = "UPDATE_TIME";
    public static final String COL_CHECK_TIME = "CHECK_TIME";
    public static final String COL_COLLATION = "COLLATION";
    public static final String COL_NULLABLE = "NULLABLE";
    public static final String COL_SUB_PART = "SUB_PART";
    public static final String COL_AVG_ROW_LENGTH = "AVG_ROW_LENGTH";
    public static final String COL_DATA_LENGTH = "DATA_LENGTH";
    public static final String COL_INDEX_NAME = "INDEX_NAME";
    public static final String COL_INDEX_TYPE = "INDEX_TYPE";
    public static final String COL_NON_UNIQUE = "NON_UNIQUE";
    public static final String COL_COMMENT = "COMMENT";
    public static final String COL_CHECK_CLAUSE = "CHECK_CLAUSE";

    public static final String COL_COLUMN_NAME = "COLUMN_NAME";
    public static final String COL_COLUMN_KEY = "COLUMN_KEY";
    public static final String COL_DATA_TYPE = "DATA_TYPE";
    public static final String COL_CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH";
    public static final String COL_CHARACTER_OCTET_LENGTH = "CHARACTER_OCTET_LENGTH";
    public static final String COL_NUMERIC_PRECISION = "NUMERIC_PRECISION";
    public static final String COL_NUMERIC_SCALE = "NUMERIC_SCALE";
    public static final String COL_DATETIME_SCALE = "DATETIME_PRECISION";
    public static final String COL_COLUMN_DEFAULT = "COLUMN_DEFAULT";
    public static final String COL_IS_NULLABLE = "IS_NULLABLE";
    public static final String COL_IS_UPDATABLE = "IS_UPDATABLE";
    public static final String COL_COLUMN_COMMENT = "COLUMN_COMMENT";
    public static final String COL_COLUMN_EXTRA = "EXTRA";
    public static final String COL_COLUMN_TYPE = "COLUMN_TYPE";

    public static final String COL_ROUTINE_SCHEMA = "ROUTINE_SCHEMA";
    public static final String COL_ROUTINE_NAME = "ROUTINE_NAME";
    public static final String COL_ROUTINE_TYPE = "ROUTINE_TYPE";
    public static final String COL_DTD_IDENTIFIER = "DTD_IDENTIFIER";
    public static final String COL_ROUTINE_BODY = "ROUTINE_BODY";
    public static final String COL_ROUTINE_DEFINITION = "ROUTINE_DEFINITION";
    public static final String COL_COLUMN_GENERATION_EXPRESSION = "GENERATION_EXPRESSION"; //$NON-NLS-1$
    public static final String COL_EXTERNAL_NAME = "EXTERNAL_NAME";
    public static final String COL_EXTERNAL_LANGUAGE = "EXTERNAL_LANGUAGE";
    public static final String COL_PARAMETER_STYLE = "PARAMETER_STYLE";
    public static final String COL_IS_DETERMINISTIC = "IS_DETERMINISTIC";
    public static final String COL_SQL_DATA_ACCESS = "SQL_DATA_ACCESS";
    public static final String COL_SECURITY_TYPE = "SECURITY_TYPE";
    public static final String COL_ROUTINE_COMMENT = "ROUTINE_COMMENT";
    public static final String COL_DEFINER = "DEFINER";
    public static final String COL_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";

    public static final String EXTRA_AUTO_INCREMENT = "auto_increment";
    public static final String EXTRA_ON_UPDATE_CURRENT_TIMESTAMP = "on update current_timestamp";
    public static final String EXTRA_STORED_GENERATED = "stored generated";
    public static final String EXTRA_VIRTUAL_GENERATED = "virtual generated";

    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    public static final String TYPE_NAME_ENUM = "enum";
    public static final String TYPE_NAME_SET = "set";

    public static final String IDX_NAME = "Key_name";
    public static final String IDX_CARDINALITY = "Cardinality";
    public static final String IDX_COMMENT = "Index_comment";
    public static final String IDX_COL_COMMENT = "Comment";
    public static final String IDX_COL_NON_UNIQUE = "Non_unique";
    public static final String IDX_VISIBLE = "Visible";
    public static final String IDX_COLLATION = "Collation";
    public static final String IDX_TYPE = "Index_type";
    public static final String IDX_COLUMN_NAME = "Column_name";
    public static final String IDX_PRIMARY_KEY = "PRIMARY";
    public static final String IDX_TABLE_NAME = "Table";

    public static final String CONS_NAME = "CONSTRAINT_NAME";
    public static final String CONS_COL_NAME = "COLUMN_NAME";
    public static final String CONS_CONSTRAINT_SCHEMA = "CONSTRAINT_SCHEMA";

    public static final String CONS_REFERENCED_TABLE_SCHEMA = "REFERENCED_TABLE_SCHEMA";
    public static final String CONS_REFERENCED_TABLE_NAME = "REFERENCED_TABLE_NAME";
    public static final String CONS_REFERENCED_COLUMN_NAME = "REFERENCED_COLUMN_NAME";

    public static final String CONS_TYPE = "CONSTRAINT_TYPE";

}
