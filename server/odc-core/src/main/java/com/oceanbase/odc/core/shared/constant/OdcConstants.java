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
package com.oceanbase.odc.core.shared.constant;

import java.util.Collections;
import java.util.List;

public class OdcConstants {

    public static final Integer DEFAULT_QUERY_LIMIT = 1000;
    public static final Integer DEFAULT_QUERY_TIMEOUT_SECONDS = 60;
    /**
     * Admin user name
     */
    public static final String ADMIN_ACCOUNT_NAME = "admin";
    public static final List<String> RESERVED_ACCOUNT_NAMES = Collections.singletonList(ADMIN_ACCOUNT_NAME);
    public static final String ADMIN_ROLE_NAME = "system_admin";
    public static final Long DEFAULT_ADMIN_USER_ID = 1L;
    public static final Long DEFAULT_ORGANIZATION_ID = 1L;
    /**
     * Constant Package Name for Odc Usage
     */
    public static final String PL_DEBUG_PACKAGE = "OBODC_PL_DEBUG_PACKAGE";
    /**
     * Constant Procedure Name for Odc Usage
     */
    public static final String PROCEDURE_SET_BREAKPOINT = "OBODC_PROCEDURE_SET_BREAKPOINT";
    public static final String PROCEDURE_SET_BREAKPOINT_ANONYMOUS = "OBODC_PROCEDURE_SET_BREAKPOINT_ANONYMOUS";
    public static final String PROCEDURE_SHOW_BREAKPOINTS = "OBODC_PROCEDURE_SHOW_BREAKPOINTS";
    public static final String PROCEDURE_PRINT_BACKTRACE = "OBODC_PROCEDURE_PRINT_BACKTRACE";
    public static final String PROCEDURE_CNT_NEXT_LINE = "OBODC_PROCEDURE_CNT_NEXT_LINE";
    public static final String PROCEDURE_CNT_NEXT_BREAKPOINT = "OBODC_PROCEDURE_CNT_NEXT_BREAKPOINT";
    public static final String PROCEDURE_CNT_STEP_IN = "OBODC_PROCEDURE_CNT_STEP_IN";
    public static final String PROCEDURE_CNT_ABORT = "OBODC_PROCEDURE_CNT_ABORT";
    public static final String PROCEDURE_CNT_STEP_OUT = "OBODC_PROCEDURE_CNT_STEP_OUT";
    public static final String PROCEDURE_CNT_EXIT = "OBODC_PROCEDURE_CNT_EXIT";
    public static final String PROCEDURE_GET_VALUES = "OBODC_PROCEDURE_GET_VALUES";
    public static final String PROCEDURE_GET_VALUE = "OBODC_PROCEDURE_GET_VALUE";
    public static final String PROCEDURE_GET_RUNTIME_INFO = "OBODC_PROCEDURE_GET_RUNTIME_INFO";
    public static final String PROCEDURE_SYNCHRONIZE = "OBODC_PROCEDURE_SYNCHRONIZE";
    public static final String PROCEDURE_GET_LINE = "OBODC_PROCEDURE_GET_LINE";

    public static final String VALIDATE_DDL_TABLE_POSTFIX = "___ODC___TMP";

    public static final String PL_OBJECT_STATUS_VALID = "VALID";
    public static final String PL_OBJECT_STATUS_INVALID = "INVALID";
    public static final String DB_VARIABLE_TYPE_STRING = "string";
    public static final String DB_VARIABLE_TYPE_NUMERIC = "numeric";
    public static final String DB_VARIABLE_TYPE_ENUM = "enum";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String ODC_INTERNAL_ROWID = "__ODC_INTERNAL_ROWID__";
    public static final String ODC_INTERNAL_RESULT_SET = "__ODC_INTERNAL_RESULT_SET__";
    public static final String ROWID = "ROWID";
    public static final String PRIMARY_KEY_NAME = "PRIMARY";

    public static final String MYSQL_DEFAULT_SCHEMA = "information_schema";
    public static final String ODC_BACK_URL_PARAM = "odc_back_url";

    public static final String TEST_LOGIN_ID_PARAM = "test_login_id";
    public static final String TEST_LOGIN_TYPE = "type";

    /**
     * Oceanbase driver class name
     */
    public static final String DEFAULT_DRIVER_CLASS_NAME = "com.oceanbase.jdbc.Driver";
    /**
     * MySQL driver class name
     */
    public static final String MYSQL_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    /**
     * Parameters name
     */
    public static final String CREATOR_ID = "CREATOR_ID";
    public static final String FLOW_TASK_ID = "FLOW_TASK_ID";
    public static final String SCHEDULE_ID = "SCHEDULE_ID";
    public static final String SCHEDULE_TASK_ID = "SCHEDULE_TASK_ID";
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";

    /**
     * OB ErrorCodes
     */
    public static final String QUERY_EXECUTION_INTERRUPTED = "70100";

    /**
     * Jdbc Parameters
     */
    public static final String DEFAULT_ZERO_DATE_TIME_BEHAVIOR = "round";

}
