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

package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.task.context.JobContext;
import com.oceanbase.odc.core.task.context.JobEnvConstants;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class K8sJobCaller extends BaseJobCaller {

    private final K8sJobClient client;
    private final PodConfig podConfig;

    public K8sJobCaller(K8sJobClient client, PodConfig podConfig) {
        this.client = client;
        this.podConfig = podConfig;
    }

    @Override
    public void doStart(JobContext context) throws JobException {
        String jobName = JobUtils.generateJobName(context.getTaskId());
        PodParam podParam = new PodParam();
        podParam.getEnvironments().put(JobEnvConstants.JOB_ENV_NAME, JsonUtils.toJson(context));

        client.create(podConfig.getNamespace(), jobName, podConfig.getImage(),
                podConfig.getCommand(), podParam);
    }

    @Override
    public void doStop(Long taskId) throws JobException {
        String jobName = JobUtils.generateJobName(taskId);
        client.delete(podConfig.getNamespace(), jobName);
    }
}
