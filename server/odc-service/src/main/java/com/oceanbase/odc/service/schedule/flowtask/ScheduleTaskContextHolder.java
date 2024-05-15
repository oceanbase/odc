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

    public static final String SCHEDULE_ID = "scheduleId";
    public static final String JOB_TYPE = "jobType";
    public static final String SCHEDULE_TASK_ID = "scheduleTaskId";

    /**
     * 请求入口处，将任务日志meta信息写入上下文
     */

    public static void trace(Long scheduleId, String jobType, Long scheduleTaskId) {
        ThreadContext.put(SCHEDULE_ID, String.valueOf(scheduleId));
        ThreadContext.put(JOB_TYPE, jobType);
        ThreadContext.put(SCHEDULE_TASK_ID, String.valueOf(scheduleTaskId));
    }

    /**
     * 清除任务日志meta信息上下文
     */
    public static void clear() {
        ThreadContext.remove(SCHEDULE_ID);
        ThreadContext.remove(JOB_TYPE);
        ThreadContext.remove(SCHEDULE_TASK_ID);
    }
}
