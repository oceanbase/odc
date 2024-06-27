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

import com.oceanbase.odc.service.schedule.flowtask.OperationType;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/6/25 19:34
 * @Descripition:
 */

@Data
public class ScheduleChangeParams {

    private Long scheduleId;

    private OperationType operationType;

    private CreateScheduleReq createScheduleReq;

    private UpdateScheduleReq updateScheduleReq;

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
