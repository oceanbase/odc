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
package com.oceanbase.odc.service.quartz.util;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

/**
 * @Author：tinker
 * @Date: 2023/6/25 14:23
 * @Descripition:
 */
public class ScheduleTaskUtils {

    public static final String id = "ODC_SCHEDULE_TASK_OERPATION_ID";

    public static JobDataMap buildTriggerDataMap(Long taskId) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(id, taskId);
        return dataMap;
    }

    public static Long getTargetTaskId(JobExecutionContext context) {
        JobDataMap triggerDataMap = context.getTrigger().getJobDataMap();
        if (triggerDataMap != null && !triggerDataMap.isEmpty()) {
            return Long.parseLong(triggerDataMap.get(id).toString());
        }
        return null;
    }

    public static Long getScheduleId(JobExecutionContext context) {
        String name = context.getJobDetail().getKey().getName();
        return Long.parseLong(name);
    }
}
