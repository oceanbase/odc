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

package com.oceanbase.odc.service.task.executor.task;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTask;
import com.oceanbase.odc.service.task.schedule.ScheduleSourceType;

/**
 * @author gaoda.xy
 * @date 2023/11/24 11:01
 */
public class TaskFactory {

    public static Task create(JobContext jobContext) {
        if (jobContext.getJobIdentity().getSourceType() == ScheduleSourceType.TASK_TASK) {
            switch (TaskType.valueOf(jobContext.getJobIdentity().getTaskType())) {
                case SAMPLE:
                    return new SampleTask(jobContext);
                case ASYNC:
                case IMPORT:
                case EXPORT:
                case MOCKDATA:
                case PARTITION_PLAN:
                case ALTER_SCHEDULE:
                case SHADOWTABLE_SYNC:
                case EXPORT_RESULT_SET:
                case ONLINE_SCHEMA_CHANGE:
                case APPLY_PROJECT_PERMISSION:
                default:
                    throw new UnsupportedException("Unsupported task type.");
            }
        } else {
            // todo job type
            throw new UnsupportedException("Unsupported job type.");
        }
    }


}
