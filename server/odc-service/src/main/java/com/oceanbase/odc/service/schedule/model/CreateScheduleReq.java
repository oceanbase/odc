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
package com.oceanbase.odc.service.schedule.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.schedule.processor.ScheduleChangePreprocessor;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/5/27 18:42
 * @Descripition:
 */


@Data
public class CreateScheduleReq {

    @NotBlank
    private String name;

    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = DataArchiveParameters.class, name = "DATA_ARCHIVE"),
            @JsonSubTypes.Type(value = DataDeleteParameters.class, name = "DATA_DELETE"),
            @JsonSubTypes.Type(value = DatabaseChangeParameters.class, name = "SQL_PLAN")
    })
    @NotNull
    private ScheduleTaskParameters parameters;

    // DATA_ARCHIVE、DATA_DELETE、PARTITION_PLAN、SQL_PLAN
    @NotNull
    private ScheduleType type;

    @NotNull
    private TriggerConfig triggerConfig;

    private String description;

    /**
     * Followings are filled by aspect {@link ScheduleChangePreprocessor}
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long projectId;
    @JsonProperty(access = Access.READ_ONLY)
    private String projectName;
    @JsonProperty(access = Access.READ_ONLY)
    private Long databaseId;
    @JsonProperty(access = Access.READ_ONLY)
    private String databaseName;
    @JsonProperty(access = Access.READ_ONLY)
    private Long connectionId;
    @JsonProperty(access = Access.READ_ONLY)
    private String connectionName;
    @JsonProperty(access = Access.READ_ONLY)
    private Long environmentId;
    @JsonProperty(access = Access.READ_ONLY)
    private String environmentName;

}

