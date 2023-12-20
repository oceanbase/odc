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
package com.oceanbase.odc.service.onlineschemachange.ddl;

/**
 * @author yaobin
 * @date 2023-06-10
 * @since 4.2.0
 */
public class DdlConstants {

    public static final String OSC_TABLE_NAME_PREFIX = "_";
    public static final String OSC_TABLE_NAME_PREFIX_OB_ORACLE = "";

    public static final String NEW_TABLE_NAME_SUFFIX = "_osc_new_";

    public static final String RENAMED_TABLE_NAME_SUFFIX = "_osc_old_";

    public static final String NEW_TABLE_NAME_SUFFIX_OB_ORACLE = "_OSC_NEW_";

    public static final String RENAMED_TABLE_NAME_SUFFIX_OB_ORACLE = "_OSC_OLD_";

    public static final String TABLE_NAME_WRAPPER = "`";

    public static final String TABLE_NAME_WRAPPED_QUOTE = "\"";

    public static final String PRIMARY_KEY = "PRIMARY KEY";

    public static final String UNIQUE = "UNIQUE";

    public static final String OMS_GROUP_PREFIX = "ob_oms";

    public static final String MDC_CONTEXT = "MDC_CONTEXT";


}
