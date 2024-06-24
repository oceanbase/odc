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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.schedule.flowtask.OperationType;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/5/27 18:42
 * @Descripition:
 */


@Data
public class ScheduleChangeReq {


    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = DataArchiveParameters.class, name = "DATA_ARCHIVE"),
            @JsonSubTypes.Type(value = DataDeleteParameters.class, name = "DATA_DELETE")
    })
    private ScheduleTaskParameters parameters;

    private Long scheduleId;

    private OperationType operationType;

    // DATA_ARCHIVE、DATA_DELETE、PARTITION_PLAN
    private ScheduleType type;

    private TriggerConfig triggerConfig;

    private String description;

    public static ScheduleChangeReq with(Long scheduleId, OperationType operationType) {
        ScheduleChangeReq req = new ScheduleChangeReq();
        req.setScheduleId(scheduleId);
        req.setOperationType(operationType);
        return req;
    }

}

