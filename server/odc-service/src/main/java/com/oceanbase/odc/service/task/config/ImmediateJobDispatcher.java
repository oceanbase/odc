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

package com.oceanbase.odc.service.task.config;

import com.oceanbase.odc.core.task.context.JobContext;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobException;

/**
 * Dispatch job to JobCaller immediately
 *
 * @author yaobin
 * @date 2023-11-20
 * @since 4.2.4
 */
public class ImmediateJobDispatcher implements JobDispatcher {

    private final JobCaller jobCaller;

    public ImmediateJobDispatcher(JobCaller jobCaller) {
        this.jobCaller = jobCaller;
    }

    @Override
    public void dispatch(JobContext context) throws JobException {
        jobCaller.start(context);
    }
}
