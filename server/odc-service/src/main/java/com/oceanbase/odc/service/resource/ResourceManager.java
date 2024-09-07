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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.metadb.resource.ResourceSpecs;
import com.oceanbase.odc.service.resource.k8s.DefaultResourceOperatorBuilder;
import com.oceanbase.odc.service.resource.k8s.model.QueryResourceParams;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * resource manager to holds resource allocate and free
 * 
 * @author longpeng.zlp
 * @date 2024/8/26 20:17
 */
@Slf4j
@Service
public class ResourceManager {

    @Autowired
    private ResourceRepository resourceRepository;
    private final List<ResourceOperatorBuilder<?, ?>> resourceOperatorBuilders = new ArrayList<>();

    public ResourceManager(@Autowired(required = false) List<ResourceOperatorBuilder<?, ?>> builders) {
        if (CollectionUtils.isNotEmpty(builders)) {
            this.resourceOperatorBuilders.addAll(builders);
        }
    }

    /**
     * register or update k8s operator
     */
    @SkipAuthorize("odc internal usage")
    public void registerResourceOperator(ResourceOperatorBuilder<?, ?> operator) {
        resourceOperatorBuilders.add(operator);
        log.info("Resource operator builder registered for operator={}", operator);
    }

    /**
     * directly create resource
     *
     * @param resourceLocation location of the resource
     * @param type type of the resource
     * @param resourceContext to create pod
     * @return
     */
    @SuppressWarnings("all")
    @Transactional
    @SkipAuthorize("odc internal usage")
    public <RC extends ResourceContext, R extends Resource> ResourceWithID<R> create(
            @NonNull ResourceLocation resourceLocation,
            @NonNull String type, @NonNull RC resourceContext) throws Exception {
        // get builder and operator and create
        ResourceOperatorBuilder<RC, R> operatorBuilder =
                (ResourceOperatorBuilder<RC, R>) getOperatorBuilder(type);
        ResourceOperator<RC, R> resourceOperator = operatorBuilder.build(resourceLocation);
        R resource = resourceOperator.create(resourceContext);
        // if save resource to db failed, rollback it
        ResourceEntity savedEntity;
        try {
            savedEntity = operatorBuilder.toResourceEntity(resource);
            savedEntity.setStatus(ResourceState.CREATING);
            savedEntity = this.resourceRepository.save(savedEntity);
        } catch (Exception e) {
            log.info("Save resource failed, resourceID={}", resource.resourceID(), e);
            // release resource if save db failed
            try {
                resourceOperator.destroy(resource.resourceID());
            } catch (Exception ex) {
                log.warn("Failed to destroy resource, resourceID={}", resource.resourceID(), ex);
            }
            throw e;
        }
        return new ResourceWithID<>(savedEntity.getId(), resource);
    }

    @SuppressWarnings("all")
    @Transactional
    @SkipAuthorize("odc internal usage")
    public <R extends Resource> Page<ResourceWithID<R>> list(
            @NonNull QueryResourceParams params, @NonNull Pageable pageable) throws Exception {
        Specification<ResourceEntity> spec = Specification
                .where(ResourceSpecs.idIn(params.getIds()));
        Page<ResourceEntity> resourceEntities = this.resourceRepository.findAll(spec, pageable);

        Map<String, List<ResourceEntity>> type2Resource = resourceEntities.stream()
                .collect(Collectors.groupingBy(ResourceEntity::getResourceType));
        Map<ResourceID, R> resourceId2Resource = new HashMap<>();
        Map<ResourceState, List<Long>> status2ResourceIds = new HashMap<>();
        for (Entry<String, List<ResourceEntity>> entry : type2Resource.entrySet()) {
            ResourceOperatorBuilder<?, R> resourceOperatorBuilder =
                    (ResourceOperatorBuilder<?, R>) getOperatorBuilder(entry.getKey());
            List<R> rs = resourceOperatorBuilder.toResources(entry.getValue());
            for (int i = 0; i < rs.size(); i++) {
                R r = rs.get(i);
                resourceId2Resource.put(r.resourceID(), r);
                List<Long> ids = status2ResourceIds.computeIfAbsent(
                        r.resourceState(), k -> new ArrayList<>());
                ids.add(entry.getValue().get(i).getId());
            }
        }
        Page<ResourceWithID<R>> returnVal =
                resourceEntities.map(e -> new ResourceWithID<>(e.getId(), resourceId2Resource.get(new ResourceID(e))));
        status2ResourceIds.forEach((key, value) -> resourceRepository.updateStatusByIdIn(value, key));
        return returnVal;
    }

    /**
     * query resource state with resource id
     * 
     * @param resourceID
     * @return
     * @throws Exception
     */
    @SuppressWarnings("all")
    @SkipAuthorize("odc internal usage")
    public <R extends Resource> Optional<R> query(@NonNull ResourceID resourceID) throws Exception {
        ResourceOperatorBuilder<?, R> operatorBuilder =
                (ResourceOperatorBuilder<?, R>) getOperatorBuilder(resourceID.getType());
        ResourceOperator<?, R> resourceOperator = operatorBuilder.build(resourceID.getResourceLocation());
        return resourceOperator.query(resourceID);
    }

    /**
     * query resource state with unique seq equals {@link ResourceEntity#getId()}
     *
     * @return
     * @throws Exception
     */
    @SuppressWarnings("all")
    @Transactional
    @SkipAuthorize("odc internal usage")
    public <R extends Resource> Optional<R> query(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findById(id);
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        ResourceEntity resourceEntity = optional.get();
        ResourceOperatorBuilder<?, ?> builder = getOperatorBuilder(resourceEntity.getResourceType());
        R resource = (R) builder.toResources(Collections.singletonList(resourceEntity)).get(0);
        this.resourceRepository.updateStatusById(id, resource.resourceState());
        return Optional.of(resource);
    }

    /**
     * release resource, currently mark resource as destroying
     *
     * @param resourceID
     */
    @Transactional
    @SkipAuthorize("odc internal usage")
    public void release(@NonNull ResourceID resourceID) {
        // first detect if resourceID is created, cause may be it's resource create by old task version
        // update job destroyed, let scheduler DestroyExecutorJob scan and destroy it
        Optional<ResourceEntity> savedResource = resourceRepository.findByResourceID(resourceID);
        if (!savedResource.isPresent()) {
            // create resource_resource with DESTROYING state
            ResourceEntity resourceEntity = new ResourceEntity();
            resourceEntity.setResourceType(DefaultResourceOperatorBuilder.CLOUD_K8S_POD_TYPE);
            resourceEntity.setEndpoint("unknown");
            resourceEntity.setCreateTime(new Date(System.currentTimeMillis()));
            resourceEntity.setRegion(resourceID.getResourceLocation().getRegion());
            resourceEntity.setGroupName(resourceID.getResourceLocation().getGroup());
            resourceEntity.setNamespace(resourceID.getNamespace());
            resourceEntity.setResourceName(resourceID.getIdentifier());
            resourceEntity.setStatus(ResourceState.DESTROYING);
            resourceRepository.save(resourceEntity);
        } else {
            // update resource state to destroying
            resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYING);
        }
    }

    @SkipAuthorize("odc internal usage")
    public String destroy(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalStateException("Resource is not found by id " + id);
        }
        return destroy(new ResourceID(optional.get()));
    }

    /**
     * real destroy by resource id
     * 
     * @param resourceID
     * @return
     * @throws Exception
     */
    @Transactional
    @SkipAuthorize("odc internal usage")
    public String destroy(@NonNull ResourceID resourceID) throws Exception {
        ResourceOperatorBuilder<?, ?> operatorBuilder = getOperatorBuilder(resourceID.getType());
        ResourceOperator<?, ?> resourceOperator = operatorBuilder.build(resourceID.getResourceLocation());
        String ret = resourceOperator.destroy(resourceID);
        // then update db status
        this.resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYING);
        log.info("Delete resource succeed, resourceID={}", resourceID);
        return ret;
    }

    /**
     * detect if resource can be destroyed
     *
     * @param resourceID
     * @return
     */
    @SkipAuthorize("odc internal usage")
    public boolean canBeDestroyed(@NonNull ResourceID resourceID) {
        ResourceOperatorBuilder<?, ?> operatorBuilder = getOperatorBuilder(resourceID.getType());
        ResourceOperator<?, ?> resourceOperator = operatorBuilder.build(resourceID.getResourceLocation());
        return resourceOperator.canBeDestroyed(resourceID);
    }

    /**
     * find resource operator by resource location and type
     *
     * @return
     */
    protected ResourceOperatorBuilder<?, ?> getOperatorBuilder(String type) {
        for (ResourceOperatorBuilder<?, ?> candidate : resourceOperatorBuilders) {
            if (candidate.match(type)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Resource operator builder is not found by " + type);
    }

}
