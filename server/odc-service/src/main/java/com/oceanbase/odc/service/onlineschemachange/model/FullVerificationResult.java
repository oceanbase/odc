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
package com.oceanbase.odc.service.onlineschemachange.model;

/**
 * Indicate full verification result between origin and new table
 *
 * @author yaobin
 * @date 2023-05-26
 * @since 4.2.0
 */
public enum FullVerificationResult {

    /**
     * Verify origin table data with new table, their data is consistent
     */
    CONSISTENT,

    /**
     * Verify origin table data with new table, their data is inconsistent
     */
    INCONSISTENT,

    /**
     * uncheck data consistent because system configuration disable full verify
     */
    UNCHECK
}
