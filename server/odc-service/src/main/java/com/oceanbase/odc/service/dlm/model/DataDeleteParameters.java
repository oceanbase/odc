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
package com.oceanbase.odc.service.dlm.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2023/7/13 17:21
 * @Descripition:
 */
@Data
public class DataDeleteParameters extends DLMBaseParameters {

    @NotNull
    private Long databaseId;

    private Long targetDatabaseId;
    // inner init
    @JsonProperty(access = Access.READ_ONLY)
    private Database database;
    // inner init
    @JsonProperty(access = Access.READ_ONLY)
    private Database targetDatabase;

    private Boolean deleteByUniqueKey = true;

    private Boolean needCheckBeforeDelete = false;

}
