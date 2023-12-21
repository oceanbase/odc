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

import org.apache.logging.log4j.ThreadContext;

/**
 * @Author：tinker
 * @Date: 2023/12/5 22:06
 * @Descripition:
 */
public class ScheduleTaskContextHolder {

    public static final String JOB_NAME = "jobName";
    public static final String JOB_GROUP = "jobGroup";
    public static final String SCHEDULE_TASK_ID = "scheduleTaskId";

    /**
     * 请求入口处，将任务日志meta信息写入上下文
     */
    public static void trace(String jobName, String jobGroup, Long scheduleTaskId) {
        ThreadContext.put(JOB_NAME, jobName);
        ThreadContext.put(JOB_GROUP, jobGroup);
        ThreadContext.put(SCHEDULE_TASK_ID, String.valueOf(scheduleTaskId));
    }

    /**
     * 清除任务日志meta信息上下文
     */
    public static void clear() {
        ThreadContext.remove(JOB_NAME);
        ThreadContext.remove(JOB_GROUP);
        ThreadContext.remove(SCHEDULE_TASK_ID);
    }
}
