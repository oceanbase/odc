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
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.ResourceState;
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
public class K8SResourceOperator implements ResourceOperator<K8sResourceContext, K8sPodResource, K8sPodResourceID> {
    private final K8sResourceOperatorContext context;

    @Override
    public K8sPodResource create(K8sResourceContext k8sResourceContext) throws JobException {
        Preconditions.checkArgument(null != k8sResourceContext.region());
        K8sPodResource ret = context.getK8sJobClient().create(k8sResourceContext);
        ret.setRegion(k8sResourceContext.getRegion());
        ret.setGroup(k8sResourceContext.resourceGroup());
        return ret;
    }

    @Override
    public Optional<K8sPodResource> query(K8sPodResourceID resourceID) throws JobException {
        Preconditions.checkArgument(null != resourceID.getRegion());
        Optional<K8sPodResource> ret = context.getK8sJobClient().get(resourceID.getNamespace(), resourceID.getName());
        if (ret.isPresent()) {
            ret.get().setRegion(resourceID.getRegion());
        }
        return ret;
    }

    @Override
    public String destroy(K8sPodResourceID resourceID) throws JobException {
        Preconditions.checkArgument(null != resourceID.getRegion());
        // first destroy
        return context.getK8sJobClient().delete(resourceID.getNamespace(), resourceID.getName());
    }

    @Override
    public boolean canBeDestroyed(K8sPodResourceID resourceID) {
        return canBeDestroyed(resourceID, context.getCreateElapsedTimeFunc());
    }

    public boolean canBeDestroyed(K8sPodResourceID resourceID, Function<K8sPodResourceID, Long> createElapsedTimeFunc) {
        Preconditions.checkArgument(null != resourceID.getRegion());
        Optional<K8sPodResource> query;

        try {
            query = context.getK8sJobClient().get(resourceID.getNamespace(), resourceID.getName());
        } catch (JobException e) {
            log.warn("Get k8s pod occur error, resource={}", resourceID, e);
            return false;
        }
        if (query.isPresent()) {
            if (ResourceState.isPreparing(query.get().getResourceState())) {
                if (createElapsedTimeFunc.apply(resourceID) <= context.getPodPendingTimeoutSeconds()) {
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
}