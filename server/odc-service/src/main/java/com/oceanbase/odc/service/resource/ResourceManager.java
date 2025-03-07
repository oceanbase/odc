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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.metadb.resource.ResourceSpecs;
import com.oceanbase.odc.service.resource.k8s.model.QueryResourceParams;

import lombok.Getter;
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

    @Getter
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        ResourceLocation createdLocation = resource.resourceID().getResourceLocation();
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
    public Page<ResourceWithID<Resource>> list(
            @NonNull QueryResourceParams params, @NonNull Pageable pageable) throws Exception {
        Specification<ResourceEntity> spec = Specification
                .where(ResourceSpecs.idIn(params.getIds()));
        Map<String, Map<ResourceLocation, List<Resource>>> cache = new HashMap<>();
        Map<ResourceState, List<Long>> status2ResourceIds = new HashMap<>();
        Page<ResourceEntity> resourceEntities = this.resourceRepository.findAll(spec, pageable);
        Map<ResourceID, Resource> resourceIDResourceMap = findAll(resourceEntities.getContent().stream()
                .map(ResourceID::new).collect(Collectors.toList()));
        Page<ResourceWithID<Resource>> returnVal = resourceEntities.map(e -> {
            Optional<Resource> optional;
            if (resourceIDResourceMap.get(new ResourceID(e)) != null) {
                optional = Optional.of(resourceIDResourceMap.get(new ResourceID(e)));
            } else {
                optional = Optional.empty();
            }
            ResourceOperatorBuilder<?, Resource> builder =
                    (ResourceOperatorBuilder<?, Resource>) getOperatorBuilder(e.getResourceType());
            Resource resource = builder.toResource(e, optional);
            List<Long> ids = status2ResourceIds.computeIfAbsent(resource.resourceState(), k -> new ArrayList<>());
            ids.add(e.getId());
            return new ResourceWithID<>(e.getId(), resource);
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
    @SuppressWarnings("all")
    @Transactional
    @SkipAuthorize("odc internal usage")
    public <R extends Resource> Optional<R> query(@NonNull ResourceID resourceID) throws Exception {
        Optional<ResourceEntity> re = this.resourceRepository.findByResourceID(resourceID);
        if (!re.isPresent()) {
            log.warn("Resource is not found, resourceID={}", resourceID);
        }
        ResourceOperatorBuilder<?, R> resourceOperatorBuilder =
                (ResourceOperatorBuilder<?, R>) getOperatorBuilder(resourceID.getType());
        Optional<R> optional = resourceOperatorBuilder.build(resourceID.getResourceLocation()).query(resourceID);
        if (re.isPresent()) {
            Resource resource = resourceOperatorBuilder.toResource(re.get(), optional);
            if (!Objects.equals(resource.resourceState(), re.get().getStatus())) {
                this.resourceRepository.updateResourceStatus(resourceID, resource.resourceState());
            }
        }
        return optional;
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
    public <R extends Resource> Optional<ResourceWithID<R>> query(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> re = this.resourceRepository.findById(id);
        if (!re.isPresent()) {
            return Optional.empty();
        }
        ResourceEntity entity = re.get();
        ResourceOperatorBuilder<?, R> builder =
                (ResourceOperatorBuilder<?, R>) getOperatorBuilder(entity.getResourceType());
        ResourceID resourceID = new ResourceID(entity);
        Optional<R> optional = builder.build(resourceID.getResourceLocation()).query(resourceID);
        R resource = builder.toResource(entity, optional);
        if (!Objects.equals(resource.resourceState(), entity.getStatus())) {
            this.resourceRepository.updateStatusById(entity.getId(), resource.resourceState());
        }
        return Optional.of(new ResourceWithID<>(id, resource));
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
            resourceEntity.setResourceType(resourceID.getType());
            resourceEntity.setEndpoint("unknown");
            resourceEntity.setCreateTime(new Date(System.currentTimeMillis()));
            resourceEntity.setRegion(resourceID.getResourceLocation().getRegion());
            resourceEntity.setGroupName(resourceID.getResourceLocation().getGroup());
            resourceEntity.setNamespace(resourceID.getNamespace());
            resourceEntity.setResourceName(resourceID.getIdentifier());
            resourceEntity.setStatus(ResourceState.ABANDONED);
            resourceRepository.save(resourceEntity);
        } else {
            // update resource state to destroying
            resourceRepository.updateResourceStatus(resourceID, ResourceState.ABANDONED);
        }
    }

    /**
     * real destroy by resource id
     *
     * @param resourceID
     * @return
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SkipAuthorize("odc internal usage")
    public String destroy(@NonNull ResourceID resourceID) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findByResourceID(resourceID);
        if (!optional.isPresent()) { // may old version job
            log.warn("Resource is not found, resourceID={}", resourceID);
        } else if (optional.get().getStatus() == ResourceState.DESTROYING) {
            log.warn("Resource is already in destroying state, resourceID={}", resourceID);
            return null;
        }
        return doDestroy(resourceID);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
            if (candidate.matches(type)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Resource operator builder is not found by " + type);
    }

    private String doDestroy(@NonNull ResourceID resourceID) throws Exception {
        ResourceOperator<?, ?> resourceOperator = getOperatorBuilder(resourceID.getType())
                .build(resourceID.getResourceLocation());
        String ret = resourceOperator.destroy(resourceID);
        // then update db status
        this.resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYING);
        log.info("Delete resource succeed, resourceID={}, ret={}", resourceID, ret);
        return ret;
    }

    @SuppressWarnings("all")
    private Map<ResourceID, Resource> findAll(@NonNull List<ResourceID> ids) {
        Map<String, List<ResourceID>> type2ResourceIDs = ids.stream()
                .collect(Collectors.groupingBy(ResourceID::getType));
        Map<ResourceID, Resource> returnVal = new HashMap<>();
        type2ResourceIDs.forEach((type, rIds) -> {
            ResourceOperatorBuilder<?, Resource> operatorBuilder =
                    (ResourceOperatorBuilder<?, Resource>) getOperatorBuilder(type);
            Map<ResourceLocation, List<ResourceID>> location2ResourceIDs = rIds
                    .stream().collect(Collectors.groupingBy(ResourceID::getResourceLocation));
            location2ResourceIDs.keySet().forEach(location -> {
                try {
                    operatorBuilder.build(location).list().forEach(
                            resource -> returnVal.put(resource.resourceID(), resource));
                } catch (Exception ex) {
                    log.warn("Failed to list resources", ex);
                    throw new IllegalStateException(ex);
                }
            });
        });
        return returnVal;
    }

}
