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

import org.apache.commons.lang3.tuple.Pair;

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceID;
import com.oceanbase.odc.metadb.resource.ResourceLocation;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.k8s.DefaultResourceOperatorBuilder;
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
    public <RC extends ResourceContext, R extends Resource> ResourceWithUniqueSeq<R> createResource(
            ResourceTag resourceTag,
            RC k8sResourceContext) throws JobException {
        // get builder and operator and create
        ResourceOperatorBuilder<RC, R> operatorBuilder =
                (ResourceOperatorBuilder<RC, R>) getOperatorBuilder(resourceTag);
        ResourceOperator<RC, R> resourceOperator = operatorBuilder.build(resourceTag);
        R k8sResource = resourceOperator.create(k8sResourceContext);
        // if save resource to db failed, rollback it
        ResourceEntity savedEntity = null;
        try {
            savedEntity = resourceRepository.save(operatorBuilder.toResourceEntity(k8sResource));
        } catch (Throwable e) {
            log.info("save resource={} failed, rollback creation", k8sResource);
            // release resource if save db failed
            resourceOperator.destroy(k8sResource.resourceID());
            throw new JobException("save resource to meta store failed", e);
        }
        return new ResourceWithUniqueSeq<>(savedEntity.getId(), k8sResource);
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
     * query resource state with unique seq equals {@link ResourceEntity#getId()}
     *
     * @return
     * @throws JobException
     */
    public <R extends Resource> Optional<R> query(long uniqueSeq) throws JobException {
        Pair<ResourceTag, ResourceID> resourceTagResourceIDPair = queryByUniqueSeq(uniqueSeq);
        if (null == resourceTagResourceIDPair) {
            return Optional.empty();
        } else {
            return query(resourceTagResourceIDPair.getLeft(), resourceTagResourceIDPair.getRight());
        }
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
            resourceEntity.setResourceType(DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE);
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

    /**
     * destroy by uniqueSeq equals {@link ResourceEntity#getId()}
     * 
     * @param uniqueSeq
     * @return
     * @throws JobException
     */
    public String destroy(long uniqueSeq) throws JobException {
        Pair<ResourceTag, ResourceID> resourceTagResourceIDPair = queryByUniqueSeq(uniqueSeq);
        if (null == resourceTagResourceIDPair) {
            return null;
        } else {
            return destroy(resourceTagResourceIDPair.getLeft(), resourceTagResourceIDPair.getRight());
        }
    }

    /**
     * real destroy by resource tag and resource id
     * 
     * @param resourceTag
     * @param resourceID
     * @return
     * @throws JobException
     */
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

    protected Pair<ResourceTag, ResourceID> queryByUniqueSeq(long seq) {
        Optional<ResourceEntity> resourceEntity = resourceRepository.findById(seq);
        if (resourceEntity.isPresent()) {
            ResourceEntity tmp = resourceEntity.get();
            ResourceLocation resourceLocation = new ResourceLocation(tmp.getRegion(), tmp.getGroupName());
            return Pair.of(
                    new ResourceTag(resourceLocation, tmp.getResourceType()),
                    new ResourceID(resourceLocation, tmp.getNamespace(), tmp.getResourceName()));
        } else {
            return null;
        }
    }
}
