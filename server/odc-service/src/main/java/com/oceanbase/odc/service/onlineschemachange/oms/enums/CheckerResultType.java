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
package com.oceanbase.odc.service.onlineschemachange.oms.enums;

/**
 * @author yaobin
 * @date 2023-06-05
 * @since 4.2.0
 */
public enum CheckerResultType {

    RUNNING,
    SCHEMA_MISMATCH,
    INDEX_CONFLICT,
    TARGET_TABLE_NOT_EXIST,
    RUNTIME_EXCEPTION,
    BOTH_EMPTY,
    CONSISTENT,
    INCONSISTENT,
    SOURCE_EMPTY,
    TARGET_EMPTY,
    MISMATCH_LIMIT,
    MISMATCH_LIMIT_NO_PK,
    NOT_CATEGORIZED,

    UNKNOWN

}
