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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Getter
@AllArgsConstructor
public enum OscStepName {

    PRE_CHECK,
    TRANSFER_PRECHECK,
    PREPARE,
    STRUCT_TRANSFER,
    STRUCT_MIGRATION,
    INDEX_MIGRATION,
    INDEX_TRANSFER,
    STRUCT_SYNC,
    FULL_MIGRATION,
    FULL_TRANSFER,
    APP_SWITCH,
    TRANSFER_APP_SWITCH,
    REVERSE_INCR_SYNC,
    REVERSE_INCR_TRANSFER,
    FULL_VALIDATION,
    FULL_VERIFIER,
    INCR_LOG_PULL,
    TRANSFER_INCR_LOG_PULL,
    INCR_SYNC,
    INCR_TRANSFER,
    INCR_VERIFIER,
    SYNC_PREPARE,
    TRANSFER_PREPARE,
    SYNC_INCR_LOG_PULL,
    CONNECTOR_FULL_SYNC,
    CONNECTOR_INCR_SYNC,
    UNKNOWN

}
