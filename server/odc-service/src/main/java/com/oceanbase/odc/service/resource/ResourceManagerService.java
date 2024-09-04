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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.model.ResourceID;
import com.oceanbase.odc.service.resource.model.ResourceOperatorTag;
import com.oceanbase.odc.service.resource.model.ResourceState;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ResourceManagerService}
 *
 * @author yh263208
 * @date 2024-09-02 16:11
 * @since ODC_release_4.3.2
 */
@Slf4j
@Service
public class ResourceManagerService {

    private ResourceRepository resourceRepository = new InMemoryResourceRepository();
    @Autowired(required = false)
    private List<ResourceOperatorBuilder<?>> resourceOperatorBuilders;

    @Transactional
    public Resource create(@NonNull Resource config) throws Exception {
        Object resourceConfig = config.getResourceConfig();
        ResourceOperatorTag resourceOperatorTag = config.getResourceOperatorTag();
        PreConditions.notNull(resourceConfig, "ResourceConfig");
        PreConditions.notNull(resourceOperatorTag, "ResourceOperatorTag");

        Resource resource = new Resource();
        resource.setResourceOperatorTag(resourceOperatorTag);
        resource.setResourceState(ResourceState.CREATING);

        ResourceEntity resourceEntity = modelToEntity(resource);
        resourceEntity = this.resourceRepository.save(resourceEntity);

        ResourceOperator<Object> operator = getResourceOperator(resourceConfig, resourceOperatorTag);
        resourceConfig = operator.create(resourceConfig);

        resource.setId(resourceEntity.getId());
        resource.setResourceConfig(resourceConfig);
        ResourceID resourceID = operator.getKey(resourceConfig);
        resource.setResourceID(resourceID);
        this.resourceRepository.updateResourceIDById(resourceEntity.getId(), JsonUtils.toJson(resourceID));
        log.info("Create resource succeed, id={}, resourceId={}", resourceEntity.getId(), resourceID);
        return resource;
    }

    public Resource nullSafeGet(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findById(id);
        ResourceEntity resourceEntity =
                optional.orElseThrow(() -> new IllegalStateException("No resource found by id " + id));
        Resource resource = entityToModel(resourceEntity);
        ResourceID resourceID = resource.getResourceID();
        if (resourceID == null) {
            throw new IllegalStateException("ResourceId is not found by id " + id);
        }
        ResourceOperator<?> resourceOperator =
                getResourceOperator(resourceID.getType(), resource.getResourceOperatorTag());
        Object resourceConfig = resourceOperator.get(resourceID)
                .orElseThrow(() -> new IllegalStateException("No resource found by resource key " + resourceID));
        resource.setResourceConfig(resourceConfig);
        ResourceState newState = moveToNextState(resource.getResourceState(), resourceOperator, resourceConfig);
        resource.setResourceState(newState);
        this.resourceRepository.updateResourceStateById(id, newState);
        return resource;
    }

    @Transactional
    public void destroy(@NonNull Long id) throws Exception {
        Resource resource = nullSafeGet(id);
        this.resourceRepository.updateResourceStateById(id, ResourceState.DESTROYING);
        ResourceID resourceID = resource.getResourceID();
        getResourceOperator(resourceID.getType(), resource.getResourceOperatorTag()).destroy(resourceID);
        log.info("Delete resource succeed, id={}, resourceId={}", id, resourceID);
    }

    private ResourceState moveToNextState(ResourceState current, ResourceOperator<?> resourceOperator, Object config) {
        return current;
    }

    private ResourceEntity modelToEntity(Resource resource) {
        ResourceEntity entity = new ResourceEntity();
        entity.setId(resource.getId());
        entity.setResourceIDJson(JsonUtils.toJson(resource.getResourceID()));
        entity.setResourceState(resource.getResourceState());
        entity.setResourceOperatorTagJson(JsonUtils.toJson(resource.getResourceOperatorTag()));
        return entity;
    }

    private Resource entityToModel(ResourceEntity entity) {
        Resource resource = new Resource();
        resource.setId(entity.getId());
        resource.setResourceState(entity.getResourceState());
        if (StringUtils.isNotBlank(entity.getResourceIDJson())) {
            resource.setResourceID(JsonUtils.fromJson(entity.getResourceIDJson(), ResourceID.class));
        }
        resource.setResourceOperatorTag(JsonUtils.fromJson(
                entity.getResourceOperatorTagJson(), ResourceOperatorTag.class));
        return resource;
    }

    @SuppressWarnings("all")
    private <T> ResourceOperator<T> getResourceOperator(
            T config, ResourceOperatorTag resourceOperatorTag) {
        return (ResourceOperator<T>) getResourceOperator(config.getClass(), resourceOperatorTag);
    }

    @SuppressWarnings("all")
    private <T> ResourceOperator<T> getResourceOperator(
            Class<T> clazz, ResourceOperatorTag resourceOperatorTag) {
        List<ResourceOperatorBuilder<?>> builders = this.resourceOperatorBuilders.stream()
                .filter(builder -> builder.supports(clazz)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(builders)) {
            throw new IllegalArgumentException("No builder found for config " + clazz);
        } else if (builders.size() != 1) {
            throw new IllegalStateException("There are more than one builder for the config " + clazz);
        }
        return ((ResourceOperatorBuilder<T>) builders.get(0)).build(resourceOperatorTag);
    }

}
