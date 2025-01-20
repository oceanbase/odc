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
package com.oceanbase.odc.agent.runtime;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
class DefaultTaskResultBuilder {
    public static TaskResult build(TaskContainer<?> taskContainer) {
        TaskResult result = new TaskResult();
        Task<?> task = taskContainer.getTask();
        result.setResultJson(JsonUtils.toJson(task.getTaskResult()));
        result.setStatus(taskContainer.getStatus());
        result.setProgress(task.getProgress());
        result.setJobIdentity(task.getJobContext().getJobIdentity());
        result.setExecutorEndpoint(JobUtils.getExecutorPoint());
        return result;
    }

    public static void assignErrorMessage(TaskResult result, Throwable e) {
        result.setErrorMessage(null == e ? null : ExceptionUtils.getRootCauseReason(e, 3));
    }
}
