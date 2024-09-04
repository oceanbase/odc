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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceID;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.task.exception.JobException;

import lombok.extern.slf4j.Slf4j;

/**
 * resource manager to holds resource allocate and free
 * 
 * @author longpeng.zlp
 * @date 2024/8/26 20:17
 */
@Slf4j
public class ResourceManager {
    /**
     * meta store of resource
     */
    protected final ResourceRepository resourceRepository;

    private final List<ResourceOperatorBuilder<?, ?>> resourceOperatorBuilders = new ArrayList<>();

    public ResourceManager(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * register or update k8s operator
     */
    public void registerResourceOperator(ResourceOperatorBuilder<?, ?> operator) {
        resourceOperatorBuilders.add(operator);
        log.info("operator builder registered for operator={}", operator);
    }

    /**
     * directly create k8s resource
     * 
     * @param k8sResourceContext context to create pod
     * @return
     */
    public <RC extends ResourceContext, R extends Resource> R createResource(ResourceTag resourceTag,
            RC k8sResourceContext) throws JobException {
        // get builder and operator and create
        ResourceOperatorBuilder<RC, R> operatorBuilder =
                (ResourceOperatorBuilder<RC, R>) getOperatorBuilder(resourceTag);
        ResourceOperator<RC, R> resourceOperator = operatorBuilder.build(resourceTag);
        R k8sResource = resourceOperator.create(k8sResourceContext);
        // if save resource to db failed, rollback it
        try {
            resourceRepository.save(operatorBuilder.toResourceEntity(k8sResource));
        } catch (Throwable e) {
            log.info("save resource={} failed, rollback creation", k8sResource);
            // release resource if save db failed
            resourceOperator.destroy(k8sResource.id());
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
    public <R extends Resource> Optional<R> query(ResourceTag resourceTag, ResourceID resourceID) throws JobException {
        ResourceOperatorBuilder<?, R> operatorBuilder = (ResourceOperatorBuilder<?, R>) getOperatorBuilder(resourceTag);
        ResourceOperator<?, R> resourceOperator = operatorBuilder.build(resourceTag);
        return resourceOperator.query(resourceID);
    }

    /**
     * release resource, currently mark resource as destroying
     *
     * @param resourceID
     */
    public void release(ResourceID resourceID) {
        // first detect if resourceID is created, cause may be it's resource create by old task version
        // update job destroyed, let scheduler DestroyExecutorJob scan and destroy it
        Optional<ResourceEntity> savedResource = resourceRepository.findByResourceID(resourceID);
        if (!savedResource.isPresent()) {
            // create task_resource with DESTROYING state
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceMode(ResourceMode.REMOTE_K8S);
            resourceEntity.setEndpoint("unknown");
            resourceEntity.setCreateTime(new Date(System.currentTimeMillis()));
            resourceEntity.setRegion(resourceID.getResourceLocation().getRegion());
            resourceEntity.setGroupName(resourceID.getResourceLocation().getGroup());
            resourceEntity.setNamespace(resourceID.getNamespace());
            resourceEntity.setResourceName(resourceID.getName());
            resourceEntity.setStatus(ResourceState.DESTROYING);
            resourceRepository.save(resourceEntity);
        } else {
            // update resource state to destroying
            resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYING.name());
        }
    }


    public String destroy(ResourceTag resourceTag, ResourceID resourceID) throws JobException {
        ResourceOperatorBuilder<?, ?> operatorBuilder = getOperatorBuilder(resourceTag);
        ResourceOperator<?, ?> resourceOperator = operatorBuilder.build(resourceTag);
        String ret = resourceOperator.destroy(resourceID);
        // then update db status
        resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYED.name());
        log.info("resourceID={} destroyed success", resourceID);
        return ret;
    }

    /**
     * detect if resource can be destroyed
     *
     * @param resourceID
     * @return
     */
    public boolean canBeDestroyed(ResourceTag resourceTag, ResourceID resourceID) {
        ResourceOperatorBuilder<?, ?> operatorBuilder = getOperatorBuilder(resourceTag);
        ResourceOperator<?, ?> resourceOperator = operatorBuilder.build(resourceTag);
        return resourceOperator.canBeDestroyed(resourceID);
    }

    /**
     * find resource operator by resource tag
     *
     * @return
     */
    protected ResourceOperatorBuilder<?, ?> getOperatorBuilder(ResourceTag resourceTag) {
        for (ResourceOperatorBuilder<?, ?> candidate : resourceOperatorBuilders) {
            if (candidate.match(resourceTag)) {
                return candidate;
            }
        }
        throw new IllegalStateException("resource operator not found for " + resourceTag);
    }
}
