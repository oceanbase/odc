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

import com.oceanbase.odc.metadb.resource.ResourceID;
import com.oceanbase.odc.metadb.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.ResourceTag;
import com.oceanbase.odc.service.resource.k8s.DefaultResourceOperatorBuilder;
import com.oceanbase.odc.service.resource.k8s.K8sPodResource;
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
    // temporary use given resource tag
    // TODO(): config it
    public static final ResourceTag DEFAULT_TASK_RESOURCE_TAG = new ResourceTag(
            new ResourceLocation("default", "default"), DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE);

    /**
     * base job config
     */
    private final PodConfig defaultPodConfig;
    private final ResourceManager resourceManager;

    public K8sJobCaller(PodConfig podConfig, ResourceManager resourceManager) {
        this.defaultPodConfig = podConfig;
        this.resourceManager = resourceManager;
    }

    @Override
    public ExecutorIdentifier doStart(JobContext context) throws JobException {
        K8sPodResource resource =
                resourceManager.createResource(DEFAULT_TASK_RESOURCE_TAG, buildK8sResourceContext(context));
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
    protected void doFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID)
            throws JobException {
        resourceManager.release(resourceID);
        updateExecutorDestroyed(ji);
    }

    @Override
    protected boolean canBeFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID) {
        return resourceManager.canBeDestroyed(DEFAULT_TASK_RESOURCE_TAG, resourceID);
    }

    @Override
    protected boolean isExecutorExist(ExecutorIdentifier identifier, ResourceID resourceID)
            throws JobException {
        Optional<K8sPodResource> executorOptional =
                resourceManager.query(DEFAULT_TASK_RESOURCE_TAG, resourceID);
        return executorOptional.isPresent() && !ResourceState.isDestroying(executorOptional.get().getResourceState());
    }
}
