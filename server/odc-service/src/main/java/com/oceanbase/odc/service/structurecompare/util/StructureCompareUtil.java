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
package com.oceanbase.odc.service.structurecompare.util;

/**
 * @author jingtian
 * @date 2024/1/9
 * @since ODC_release_4.2.4
 */
public class StructureCompareUtil {
    private static final String DEFAULT_SQL_DELIMITER = ";";

    public static String appendDelimiterIfNotExist(String sql) {
        String returnVal = sql.trim();
        if (!returnVal.endsWith(DEFAULT_SQL_DELIMITER)) {
            return returnVal + DEFAULT_SQL_DELIMITER + "\n";
        }
        return sql;
    }
}
