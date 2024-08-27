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
package com.oceanbase.odc.service.resource.k8s;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClient;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClientSelector;
import com.oceanbase.odc.service.task.exception.JobException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * operator to manipulate resource for k8s cluster of given region
 * 
 * @author longpeng.zlp
 * @date 2024/8/13 10:29
 */
@AllArgsConstructor
@Slf4j
public class K8SResourceOperator implements ResourceOperator<K8sResourceContext> {
    private final K8sJobClientSelector k8sJobClientSelector;
    private final long podPendingTimeoutSeconds;

    @Override
    public Resource create(K8sResourceContext k8sResourceContext) throws JobException {
        Preconditions.checkArgument(null != k8sResourceContext.region());
        PodConfig podConfig = k8sResourceContext.getPodConfig();
        K8sJobClient k8sJobClient = selectK8sClient(k8sResourceContext.resourceGroup());
        K8sResource ret = k8sJobClient.create(k8sResourceContext.resourceNamespace(), k8sResourceContext.resourceName(),
                podConfig.getImage(),
                podConfig.getCommand(), podConfig);
        ret.setRegion(k8sResourceContext.getRegion());
        ret.setGroup(k8sResourceContext.resourceGroup());
        return ret;
    }

    @Override
    public Optional<K8sResource> query(ResourceID resourceID) throws JobException {
        Preconditions.checkArgument(null != resourceID.getRegion());
        K8sJobClient k8sJobClient = selectK8sClient(resourceID.getGroup());
        Optional<K8sResource> ret = k8sJobClient.get(resourceID.getNamespace(), resourceID.getName());
        if (ret.isPresent()) {
            ret.get().setRegion(resourceID.getRegion());
        }
        return ret;
    }

    @Override
    public String destroy(ResourceID resourceID) throws JobException {
        Preconditions.checkArgument(null != resourceID.getRegion());
        // first destroy
        K8sJobClient k8sJobClient = selectK8sClient(resourceID.getGroup());
        return k8sJobClient.delete(resourceID.getNamespace(), resourceID.getName());
    }

    @Override
    public boolean canBeDestroyed(ResourceID resourceID, Function<ResourceID, Long> createElapsedTimeFunc) {
        Preconditions.checkArgument(null != resourceID.getRegion());
        Optional<K8sResource> query;
        K8sJobClient k8sJobClient = selectK8sClient(resourceID.getGroup());

        try {
            query = k8sJobClient.get(resourceID.getNamespace(), resourceID.getName());
        } catch (JobException e) {
            log.warn("Get k8s pod occur error, resource={}", resourceID, e);
            return false;
        }
        if (query.isPresent()) {
            if (ResourceState.isPreparing(query.get().getResourceState())) {
                if (createElapsedTimeFunc.apply(resourceID) <= podPendingTimeoutSeconds) {
                    // Pod cannot be deleted when pod pending is not timeout,
                    // so throw exception representative cannot delete
                    log.warn("Cannot destroy pod, pending is not timeout, resourceID={},  podStatus={}",
                            resourceID, query.get().getResourceState());
                    return false;
                }
            }
        }
        return isPodIdle();
    }

    /**
     * if pod is idle, currently always return true if pod is reusable, task counter should impl,
     * rewrite this method
     * 
     * @return true if there is no task running on it
     */
    private boolean isPodIdle() {
        return true;
    }

    private K8sJobClient selectK8sClient(String resourceGroup) {
        return k8sJobClientSelector.select(resourceGroup);
    }
}
