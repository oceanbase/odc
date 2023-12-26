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

package com.oceanbase.odc.core.alarm;

public final class AlarmEventNames {

    private AlarmEventNames() {}

    /**
     * alarm
     */
    public static final String SYSTEM_CONFIG_CHANGED = "SYSTEM_CONFIG_CHANGED";
    public static final String REST_API_CALL_FAILED = "REST_API_CALL_FAILED";

    /**
     * warn
     */
    public static final String UNKNOWN_API_EXCEPTION = "UNKNOWN_API_EXCEPTION";

    /**
     * info
     */
    public static final String IS_HEALTHY = "IS_HEALTHY";

    /**
     * Druid alarm
     */
    public static final String SQL_TOO_LONG_SQL_PARAMETERS = "SQL_TOO_LONG_SQL_PARAMETERS";
    public static final String SQL_TOO_LONG_EXECUTE_TIME = "SQL_TOO_LONG_EXECUTE_TIME";
    public static final String SQL_EXECUTE_ERROR = "SQL_EXECUTE_ERROR";
    public static final String METHOD_TOO_LONG_EXECUTE_TIME = "METHOD_TOO_LONG_EXECUTE_TIME";
    public static final String METHOD_TOO_MUCH_JDBC_EXECUTE_COUNT = "METHOD_TOO_MUCH_JDBC_EXECUTE_COUNT";
    public static final String DRUID_MONITOR_ERROR = "DRUID_MONITOR_ERROR";
    public static final String DRUID_WAIT_THREAD_COUNT_MORE_THAN_0 = "DRUID_WAIT_THREAD_COUNT_MORE_THAN_0";
    public static final String DRUID_ACTIVE_COUNT_MORE_THAN_80_PERCENT = "DRUID_ACTIVE_COUNT_MORE_THAN_80_PERCENT";

}
