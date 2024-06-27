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

import java.util.Date;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/6/8 13:24
 * @Descripition:
 */

@Data
public class ScheduleChangeLog {

    private Long id;

    private Long scheduleId;

    private Long flowInstanceId;

    private OperationType type;

    private String oldScheduleParameterJson;

    private String newScheduleParameterJson;

    private ScheduleChangeStatus status;

    private Date createTime;

    private Date updateTime;

    public static ScheduleChangeLog build(Long scheduleId, OperationType type, String oldParameters,
            String newParameters) {

        return build(scheduleId, type, oldParameters, newParameters, null);
    }

    public static ScheduleChangeLog build(Long scheduleId, OperationType type, String oldParameters,
            String newParameters, Long flowInstanceId) {
        ScheduleChangeLog log = new ScheduleChangeLog();
        log.setScheduleId(scheduleId);
        log.setStatus(ScheduleChangeStatus.PREPARING);
        log.setType(type);
        log.setOldScheduleParameterJson(oldParameters);
        log.setNewScheduleParameterJson(newParameters);
        log.setFlowInstanceId(flowInstanceId);
        return log;
    }
}
