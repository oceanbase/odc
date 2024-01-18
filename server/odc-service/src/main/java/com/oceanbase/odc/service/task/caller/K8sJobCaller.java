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

import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
@Slf4j
public class K8sJobCaller extends BaseJobCaller {

    private final K8sJobClient client;
    private final PodConfig podConfig;

    public K8sJobCaller(K8sJobClient client, PodConfig podConfig) {
        this.client = client;
        this.podConfig = podConfig;
    }

    @Override
    public ExecutorIdentifier doStart(JobContext context) throws JobException {
        String jobName = JobUtils.generateJobName(context.getJobIdentity());

        String name = client.create(podConfig.getNamespace(), jobName, podConfig.getImage(),
                podConfig.getCommand(), podConfig.getPodParam());

        return DefaultExecutorIdentifier.builder().namespace(podConfig.getNamespace())
                .executorName(name).build();
    }

    @Override
    public void doStop(JobIdentity ji) throws JobException {


    }

    @Override
    protected void doDestroy(ExecutorIdentifier identifier) throws JobException {
        client.delete(podConfig.getNamespace(), identifier.getExecutorName());
    }
}
