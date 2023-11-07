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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-11-06
 * @since 4.2.3
 */
@Data
public class OscLockDatabaseUserInfo implements Serializable {

    @JsonProperty(access = Access.READ_ONLY)
    private String databaseId;

    @JsonProperty(access = Access.READ_ONLY)
    private boolean lockDatabaseUserRequired;
}
