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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            @NonNull ResourceLocation resourceLocation, @NonNull RC resourceContext) throws Exception {
        // get builder and operator and create
        ResourceOperatorBuilder<RC, R> operatorBuilder =
                (ResourceOperatorBuilder<RC, R>) getOperatorBuilder(resourceContext.type());
        ResourceOperator<RC, R> resourceOperator = operatorBuilder.build(resourceLocation);
        R resource = resourceOperator.create(resourceContext);
        // if save resource to db failed, rollback it
        ResourceEntity savedEntity;
        try {
            savedEntity = operatorBuilder.toResourceEntity(resource);
            savedEntity.setStatus(ResourceState.CREATING);
            savedEntity.setRegion(resourceLocation.getRegion());
            savedEntity.setGroupName(resourceLocation.getGroup());
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
    public Page<ResourceWithID<Resource>> list(
            @NonNull QueryResourceParams params, @NonNull Pageable pageable) throws Exception {
        Specification<ResourceEntity> spec = Specification
                .where(ResourceSpecs.idIn(params.getIds()));
        Map<String, Map<ResourceLocation, List<Resource>>> cache = new HashMap<>();
        Map<ResourceState, List<Long>> status2ResourceIds = new HashMap<>();
        Page<ResourceWithID<Resource>> returnVal = this.resourceRepository.findAll(spec, pageable).map(e -> {
            Optional<Resource> optional;
            try {
                optional = query(new ResourceID(e), cache);
            } catch (Exception ex) {
                log.warn("Failed to query resource, id={}", e.getId());
                throw new IllegalStateException(ex);
            }
            ResourceOperatorBuilder<?, Resource> builder =
                    (ResourceOperatorBuilder<?, Resource>) getOperatorBuilder(e.getResourceType());
            ResourceState newState = moveToNextState(e.getStatus(), builder, optional);
            List<Long> ids = status2ResourceIds.computeIfAbsent(
                    newState, k -> new ArrayList<>());
            ids.add(e.getId());
            return new ResourceWithID<>(e.getId(), optional.orElseGet(() -> builder.toResource(e)));
        });
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
    @Transactional
    @SkipAuthorize("odc internal usage")
    public <R extends Resource> Optional<R> query(@NonNull ResourceID resourceID) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findByResourceID(resourceID);
        return optional.isPresent() ? Optional.of(doQuery(optional.get())) : Optional.empty();
    }

    /**
     * query resource state with unique seq equals {@link ResourceEntity#getId()}
     *
     * @return
     * @throws Exception
     */
    @Transactional
    @SkipAuthorize("odc internal usage")
    public <R extends Resource> Optional<ResourceWithID<R>> query(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findById(id);
        return optional.isPresent() ? Optional.of(new ResourceWithID<>(id, doQuery(optional.get()))) : Optional.empty();
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
        Optional<ResourceEntity> optional = this.resourceRepository.findByResourceID(resourceID);
        if (!optional.isPresent()) {
            log.warn("Resource is not found, resourceID={}", resourceID);
        }
        return doDestroy(resourceID);
    }

    @Transactional
    @SkipAuthorize("odc internal usage")
    public String destroy(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalStateException("Resource not found by id " + id);
        }
        return doDestroy(new ResourceID(optional.get()));
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
    private ResourceOperatorBuilder<?, ?> getOperatorBuilder(String type) {
        for (ResourceOperatorBuilder<?, ?> candidate : resourceOperatorBuilders) {
            if (candidate.match(type)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Resource operator builder is not found by " + type);
    }

    private String doDestroy(@NonNull ResourceID resourceID) throws Exception {
        ResourceOperatorBuilder<?, ?> operatorBuilder = getOperatorBuilder(resourceID.getType());
        ResourceOperator<?, ?> resourceOperator = operatorBuilder.build(resourceID.getResourceLocation());
        String ret = resourceOperator.destroy(resourceID);
        // then update db status
        this.resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYING);
        log.info("Delete resource succeed, resourceID={}, ret={}", resourceID, ret);
        return ret;
    }

    @SuppressWarnings("all")
    private <R extends Resource> R doQuery(@NonNull ResourceEntity resourceEntity) throws Exception {
        ResourceOperatorBuilder<?, ?> builder = getOperatorBuilder(resourceEntity.getResourceType());
        Map<String, Map<ResourceLocation, List<Resource>>> cache = new HashMap<>();
        Optional<R> optional = query(new ResourceID(resourceEntity), cache);
        R resource = optional.orElseGet(() -> (R) builder.toResource(resourceEntity));
        ResourceState newState = moveToNextState(resourceEntity.getStatus(), builder, optional);
        this.resourceRepository.updateStatusById(resourceEntity.getId(), newState);
        return resource;
    }

    private <R extends Resource> ResourceState moveToNextState(ResourceState current,
            ResourceOperatorBuilder<?, ?> builder, Optional<R> optional) {
        return current;
    }

    @SuppressWarnings("all")
    private <R extends Resource> Optional<R> query(ResourceID resourceID,
            Map<String, Map<ResourceLocation, List<Resource>>> cache) throws Exception {
        ResourceLocation location = resourceID.getResourceLocation();
        Map<ResourceLocation, List<Resource>> location2Resource = cache
                .computeIfAbsent(resourceID.getType(), c -> new HashMap<>());
        List<Resource> resourceList = location2Resource.get(location);
        ResourceOperator<?, ?> resourceOperator = getOperatorBuilder(resourceID.getType()).build(location);
        if (resourceList == null) {
            resourceList = new ArrayList<>(resourceOperator.list());
            location2Resource.put(location, resourceList);
        }
        List<Resource> list = resourceList.stream()
                .filter(o -> resourceID.equals(o.resourceID()))
                .collect(Collectors.toList());
        if (list.size() > 1) {
            throw new IllegalStateException("There are more than one resource found by id " + resourceID);
        }
        return list.size() == 1 ? Optional.of((R) list.get(0)) : Optional.empty();
    }

}
