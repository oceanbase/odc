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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.schedule.processor.ScheduleChangePreprocessor;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/6/25 19:34
 * @Descripition:
 */

@Data
public class ScheduleChangeParams {

    private Long scheduleChangeLogId;

    private Long scheduleId;

    private OperationType operationType;

    private CreateScheduleReq createScheduleReq;

    private UpdateScheduleReq updateScheduleReq;

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

    public static ScheduleChangeParams with(Long id, OperationType type) {
        ScheduleChangeParams req = new ScheduleChangeParams();
        req.setScheduleId(id);
        req.setOperationType(type);
        return req;
    }

    public static ScheduleChangeParams with(CreateScheduleReq createScheduleReq) {
        ScheduleChangeParams req = new ScheduleChangeParams();
        req.setOperationType(OperationType.CREATE);
        req.setCreateScheduleReq(createScheduleReq);
        return req;
    }

    public static ScheduleChangeParams with(Long id, UpdateScheduleReq updateScheduleReq) {
        ScheduleChangeParams req = new ScheduleChangeParams();
        req.setScheduleId(id);
        req.setOperationType(OperationType.UPDATE);
        req.setUpdateScheduleReq(updateScheduleReq);
        return req;
    }


}
