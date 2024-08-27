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
package com.oceanbase.odc.service.resource;

import java.util.Optional;

import com.oceanbase.odc.service.resource.k8s.K8SResourceOperator;
import com.oceanbase.odc.service.resource.k8s.K8sResource;
import com.oceanbase.odc.service.resource.k8s.K8sResourceContext;
import com.oceanbase.odc.service.task.exception.JobException;

import lombok.extern.slf4j.Slf4j;

/**
 * resource manager to holds resource allocate and free
 * 
 * @author longpeng.zlp
 * @date 2024/8/26 20:17
 */
@Slf4j
public abstract class K8sResourceManager {
    /**
     * meta store of resource
     */
    protected final ResourceMetaStore resourceMetaStore;

    public K8sResourceManager(ResourceMetaStore resourceMetaStore) {
        this.resourceMetaStore = resourceMetaStore;
    }

    /**
     * register or update k8s operator
     */
    public abstract void registerK8sOperator(String region, K8SResourceOperator operator);

    /**
     * directly create k8s resource
     * 
     * @param k8sResourceContext context to create pod
     * @return
     */
    public Resource createK8sResource(K8sResourceContext k8sResourceContext) throws JobException {
        K8SResourceOperator operator = getK8sOperator(k8sResourceContext.getRegion());
        Resource k8sResource = operator.create(k8sResourceContext);
        // if save resource to db failed, rollback it
        try {
            resourceMetaStore.saveResource(k8sResource);
        } catch (Throwable e) {
            log.info("save resource={} failed, rollback creation", k8sResource);
            // release resource if save db failed
            operator.destroy(k8sResource.id());
            throw new JobException("save resource to meta store failed", e);
        }
        return k8sResource;
    }

    /**
     * query resource state with resource id
     * 
     * @param resourceID
     * @return
     * @throws JobException
     */
    public Optional<K8sResource> query(ResourceID resourceID) throws JobException {
        K8SResourceOperator operator = getK8sOperator(resourceID.getRegion());
        return operator.query(resourceID);
    }

    /**
     * release resource, currently mark resource as destroying
     *
     * @param resourceID
     */
    public void release(ResourceID resourceID) {
        // update resource state to destroying
        resourceMetaStore.updateResourceState(resourceID, ResourceState.DESTROYING);
    }

    public String destroy(ResourceID resourceID) throws JobException {
        K8SResourceOperator operator = getK8sOperator(resourceID.getRegion());
        String ret = operator.destroy(resourceID);
        // then update db status
        resourceMetaStore.updateResourceState(resourceID, ResourceState.DESTROYED);
        log.info("resourceID={} destroyed success", resourceID);
        return ret;
    }

    /**
     * detect if resource can be destroyed
     * 
     * @param resourceID
     * @return
     */
    public boolean canBeDestroyed(ResourceID resourceID) {
        K8SResourceOperator operator = getK8sOperator(resourceID.getRegion());
        return operator.canBeDestroyed(resourceID, this::getResourceCreateTimeInSeconds);
    }

    /**
     * query k8s resource create time from meta store
     * 
     * @param resourceID
     * @return
     */
    private long getResourceCreateTimeInSeconds(ResourceID resourceID) {
        Resource resource = resourceMetaStore.findResource(resourceID);
        return (System.currentTimeMillis() - resource.createDate().getTime()) / 1000;
    }

    /**
     * find resource operator by region
     * 
     * @param region
     * @return
     */
    protected abstract K8SResourceOperator getK8sOperator(String region);
}
