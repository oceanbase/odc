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
package com.oceanbase.odc.service.schedule.flowtask;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2022/11/14 19:58
 * @Descripition:
 */
@Data
public class AlterScheduleParameters implements Serializable, TaskParameters {

    private static final long serialVersionUID = -5159606799885558548L;

    private Long taskId;

    private JobType type;

    private OperationType operationType;

    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = DatabaseChangeParameters.class, name = "SQL_PLAN"),
            @JsonSubTypes.Type(value = DataArchiveParameters.class, name = "DATA_ARCHIVE"),
            @JsonSubTypes.Type(value = DataDeleteParameters.class, name = "DATA_DELETE")
    })
    private TaskParameters scheduleTaskParameters;

    private TriggerConfig triggerConfig;

    private Boolean allowConcurrent = false;

    private MisfireStrategy misfireStrategy = MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING;

    private String description;
}


