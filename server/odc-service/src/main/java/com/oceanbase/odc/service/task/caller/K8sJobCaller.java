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

import static com.oceanbase.odc.service.task.constants.JobConstants.ODC_EXECUTOR_CANNOT_BE_DESTROYED;

import java.util.Optional;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.exception.JobException;
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
        String jobName = JobUtils.generateExecutorName(context.getJobIdentity());

        String name = client.create(podConfig.getNamespace(), jobName, podConfig.getImage(),
                podConfig.getCommand(), podConfig);

        return DefaultExecutorIdentifier.builder().namespace(podConfig.getNamespace())
                .executorName(name).build();
    }

    @Override
    public void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doDestroy(JobIdentity ji, ExecutorIdentifier ei) throws JobException {
        updateExecutorDestroyed(ji);
        Optional<K8sJobResponse> k8sJobResponse = client.get(ei.getNamespace(), ei.getExecutorName());
        if (k8sJobResponse.isPresent()) {
            if (PodStatus.PENDING == PodStatus.of(k8sJobResponse.get().getResourceStatus())) {
                JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
                JobEntity jobEntity = jobConfiguration.getTaskFrameworkService().find(ji.getId());
                if ((System.currentTimeMillis() - jobEntity.getStartedTime().getTime()) / 1000 > podConfig
                        .getPodPendingTimeoutSeconds()) {
                    log.info("Pod pending timeout, will be deleted, jobId={}, pod={}, "
                            + "podPendingTimeoutSeconds={}.", ji.getId(), ei.getExecutorName(),
                            podConfig.getPodPendingTimeoutSeconds());
                } else {
                    // Pod cannot be deleted when pod pending is not timeout,
                    // so throw exception representative delete failed
                    throw new JobException(ODC_EXECUTOR_CANNOT_BE_DESTROYED +
                            "Destroy pod failed, jodId={0}, identifier={1}, podStatus={2}",
                            ji.getId(), ei.getExecutorName(), k8sJobResponse.get().getResourceStatus());
                }
            }
            log.info("Found pod, delete it, jobId={}, pod={}.", ji.getId(), ei.getExecutorName());
            destroyInternal(ei);
        }
    }

    @Override
    protected void doDestroyInternal(ExecutorIdentifier identifier) throws JobException {
        client.delete(podConfig.getNamespace(), identifier.getExecutorName());
    }

    @Override
    protected boolean isExecutorExist(ExecutorIdentifier identifier) throws JobException {
        Optional<K8sJobResponse> executorOptional = client.get(identifier.getNamespace(), identifier.getExecutorName());
        return executorOptional.isPresent() &&
                PodStatus.of(executorOptional.get().getResourceStatus()) != PodStatus.TERMINATING;
    }
}
