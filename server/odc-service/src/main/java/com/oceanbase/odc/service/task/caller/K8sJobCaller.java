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

import java.util.Optional;

import com.oceanbase.odc.metadb.resource.GlobalUniqueResourceID;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.K8sPodResource;
import com.oceanbase.odc.service.resource.k8s.K8sResourceContext;
import com.oceanbase.odc.service.resource.k8s.K8sResourceManager;
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
        K8sPodResource resource = resourceManager.createK8sResource(buildK8sResourceContext(context));
        String arn = resource.id().getName();
        return DefaultExecutorIdentifier.builder().namespace(defaultPodConfig.getNamespace())
                .executorName(arn).build();
    }

    protected K8sResourceContext buildK8sResourceContext(JobContext context) {
        String jobName = JobUtils.generateExecutorName(context.getJobIdentity());
        // TODO(tianke): confirm is this correct?
        String region = ResourceIDUtil.checkAndGetJobProperties(context.getJobProperties(),
                ResourceIDUtil.DEFAULT_REGION_PROP_NAME, ResourceIDUtil.DEFAULT_PROP_VALUE);
        String group = ResourceIDUtil.checkAndGetJobProperties(context.getJobProperties(),
                ResourceIDUtil.DEFAULT_GROUP_PROP_NAME, ResourceIDUtil.DEFAULT_PROP_VALUE);
        return new K8sResourceContext(defaultPodConfig, jobName, region, group, context);
    }

    @Override
    public void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doFinish(JobIdentity ji, ExecutorIdentifier ei, GlobalUniqueResourceID resourceID)
            throws JobException {
        resourceManager.release(ResourceIDUtil.wrapToK8sResourceID(resourceID));
        updateExecutorDestroyed(ji);
    }

    @Override
    protected boolean canBeFinish(JobIdentity ji, ExecutorIdentifier ei, GlobalUniqueResourceID resourceID) {
        return resourceManager.canBeDestroyed(ResourceIDUtil.wrapToK8sResourceID(resourceID));
    }

    @Override
    protected boolean isExecutorExist(ExecutorIdentifier identifier, GlobalUniqueResourceID resourceID)
            throws JobException {
        Optional<K8sPodResource> executorOptional =
                resourceManager.query(ResourceIDUtil.wrapToK8sResourceID(resourceID));
        return executorOptional.isPresent() && !ResourceState.isDestroying(executorOptional.get().getResourceState());
    }
}
