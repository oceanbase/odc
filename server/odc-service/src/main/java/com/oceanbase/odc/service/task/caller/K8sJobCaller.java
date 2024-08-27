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

import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.service.resource.K8sResourceManager;
import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.K8sResource;
import com.oceanbase.odc.service.resource.k8s.K8sResourceContext;
import com.oceanbase.odc.service.resource.k8s.PodConfig;
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

    /**
     * base job config
     */
    private final PodConfig defaultPodConfig;
    private final K8sResourceManager resourceManager;

    public K8sJobCaller(PodConfig podConfig, K8sResourceManager resourceManager) {
        this.defaultPodConfig = podConfig;
        this.resourceManager = resourceManager;
    }

    @Override
    public ExecutorIdentifier doStart(JobContext context) throws JobException {
        Resource resource = resourceManager.createK8sResource(buildK8sResourceContext(context));
        String arn = resource.id().getName();

        return DefaultExecutorIdentifier.builder().namespace(defaultPodConfig.getNamespace())
                .executorName(arn).build();
    }

    protected K8sResourceContext buildK8sResourceContext(JobContext context) {
        String jobName = JobUtils.generateExecutorName(context.getJobIdentity());
        // TODO(tianke): confirm is this correct?
        String region = checkAndGetJobProperties(context, "regionName");
        String group = checkAndGetJobProperties(context, "cloudProvider");
        K8sResourceContext k8sResourceContext =
                new K8sResourceContext(defaultPodConfig, jobName, region, group);
        return k8sResourceContext;
    }

    @Override
    public void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID) throws JobException {
        // update job destroyed, let scheduler DestroyExecutorJob scan and destroy it
        resourceManager.release(resourceID);
        updateExecutorDestroyed(ji);
        // resourceManager.destroy(resourceID);
    }

    @Override
    protected boolean canBeFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID) {
        return resourceManager.canBeDestroyed(resourceID);
    }

    @Override
    protected boolean isExecutorExist(ExecutorIdentifier identifier) throws JobException {
        ResourceID resourceID =
                new ResourceID(identifier.getRegion(), identifier.getGroup(), identifier.getNamespace(),
                        identifier.getExecutorName());
        Optional<K8sResource> executorOptional = resourceManager.query(resourceID);
        return executorOptional.isPresent() && !ResourceState.isDestroying(executorOptional.get().getResourceState());
    }

    protected String checkAndGetJobProperties(JobContext context, String propName) {
        Map<String, String> jobParameters = context.getJobProperties();
        if (null == jobParameters) {
            throw new IllegalStateException("get " + propName + " failed from job context =" + context);
        }
        String ret = jobParameters.get(propName);
        if (null == ret) {
            throw new IllegalStateException("get " + propName + " failed from job context =" + context);
        }
        return ret;
    }

}
