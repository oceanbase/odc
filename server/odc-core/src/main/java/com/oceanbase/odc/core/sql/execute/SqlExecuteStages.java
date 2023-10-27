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
package com.oceanbase.odc.core.sql.execute;

/**
 * {@link SqlExecuteStages}
 *
 * @author yh263208
 * @date 2022-05-09 20:02
 * @since ODC_release_3.3.1
 */
public class SqlExecuteStages {
    public static final String PARSE_SQL = "ODC Parse SQL";
    public static final String REWRITE_SQL = "ODC Rewrite SQL";
    public static final String APPLY_SQL = "Apply SQL";
    public static final String VALIDATE_SEMANTICS = "Validate SQL semantics";
    public static final String EXECUTE = "Execute";
    public static final String GET_RESULT_SET = "Get result-set";
    public static final String QUERY_DBMS_OUTPUT = "Query DBMS output";
    public static final String INIT_SQL_TYPE = "Init SQL type";
    public static final String INIT_COLUMN_INFO = "Init column info";
    public static final String INIT_EDITABLE_INFO = "Init editable info";
    public static final String INIT_WARNING_MESSAGE = "Init warning message";
    public static final String JDBC_PREPARE = "Jdbc prepare";
    public static final String NETWORK_CONSUMPTION = "Network consumption";
    public static final String OBSERVER_WAIT = "OBServer wait";
    public static final String SQL_CHECK = "SQL Check";
    public static final String DATA_MASKING = "Data Masking";
    public static final String DATABASE_PERMISSION_CHECK = "DB Permission Check";
    public static final String SET_NLS_FORMAT = "Set Nls Format";
    public static final String SQL_CONSOLE_RULE = "SQL Console Rule Check";
    public static final String DB_SERVER_EXECUTE_SQL = "DB Server Execute SQL";
    public static final String SQL_INTERCEPT_PRE_CHECK = "Sql intercept pre-check";
    public static final String SQL_INTERCEPT_AFTER_CHECK = "Sql intercept after-check";
    public static final String CALCULATE_DURATION = "Calculate duration";
}

