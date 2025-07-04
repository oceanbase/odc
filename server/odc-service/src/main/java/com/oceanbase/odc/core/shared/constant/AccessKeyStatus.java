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

/**
 * Access Key Status Enum
 * 
 * @author your-name
 * @date 2024/01/01
 */
public enum AccessKeyStatus {

    /**
     * Active - Access key is enabled and can be used
     */
    ACTIVE,

    /**
     * Suspended - Access key is disabled/suspended and cannot be used
     */
    SUSPENDED,

    /**
     * Deleted - Access key is deleted (soft delete)
     */
    DELETED;


    public boolean isValid() {
        return this == ACTIVE;
    }


}
