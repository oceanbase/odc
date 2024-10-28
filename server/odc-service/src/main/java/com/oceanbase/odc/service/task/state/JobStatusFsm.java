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
package com.oceanbase.odc.service.task.state;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.enums.JobStatus;

/**
 * default fsm for job status transfer according job current status and task status
 * 
 * @author longpeng.zlp
 * @date 2024/10/22 14:15
 */
public class JobStatusFsm {
    /**
     * determinate job status by task status and current job status
     * 
     * @param currentStatus
     * @param taskStatus
     * @return
     */
    public JobStatus determinateJobStatus(JobStatus currentStatus, TaskStatus taskStatus) {
        switch (taskStatus) {
            // prepare is task init status, job should expected in running status
            // running Job status is expected, if job = canceling, outside may set to cancel when task timeout
            case PREPARING:
                // running meaning task has running normally, job should expected in running status
            case RUNNING:
                return currentStatus;
            // if task status is canceled, it must be canceled by job who owns it
            case CANCELED:
                return JobStatus.CANCELED;
            // any task failed will cause the failed status of job
            case ABNORMAL:
            case FAILED:
                return JobStatus.FAILED;
            // task done will drive job status to done
            case DONE:
                return JobStatus.DONE;
            default:
                throw new IllegalStateException("status " + taskStatus + " not handled");
        }
    }
}
