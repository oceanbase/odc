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

/**
 * @Author：tinker
 * @Date: 2024/6/19 15:35
 * @Descripition:
 */
public class ScheduleTaskOverviewMapper {

    public static ScheduleTaskOverview map(ScheduleTask scheduleTask) {
        ScheduleTaskOverview t = new ScheduleTaskOverview();
        t.setId(scheduleTask.getId());
        t.setScheduleId(scheduleTask.getJobName());
        t.setStatus(scheduleTask.getStatus());
        t.setType(ScheduleTaskType.valueOf(scheduleTask.getJobGroup()));
        t.setCreateTime(scheduleTask.getCreateTime());
        t.setUpdateTime(scheduleTask.getUpdateTime());
        t.setLastFireTime(scheduleTask.getFireTime());
        return t;
    }

}
