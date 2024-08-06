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

package com.oceanbase.odc.service.task.executor.server;

import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.task.BaseTask;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author gaoda.xy
 * @date 2023/11/24 11:18
 */
public interface TaskExecutor {

    void execute(BaseTask<?> task, JobContext jc);

    boolean cancel(JobIdentity ji);

    BaseTask<?> getTask(JobIdentity ji);

}
