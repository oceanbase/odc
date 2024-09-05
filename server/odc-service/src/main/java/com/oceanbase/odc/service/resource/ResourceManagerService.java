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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.model.CreateResourceRequest;
import com.oceanbase.odc.service.resource.model.QueryResourceParams;
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
    public Resource create(@NonNull CreateResourceRequest request) throws Exception {
        Object resourceConfig = request.getResourceConfig();
        ResourceOperatorTag resourceOperatorTag = request.getResourceOperatorTag();
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

    public Page<Resource> list(@NonNull QueryResourceParams params, @NonNull Pageable pageable) throws Exception {
        Map<Class<?>, Map<ResourceOperatorTag, List<Object>>> typeAndTag2Resources = new HashMap<>();
        Map<ResourceState, List<Long>> state2Resources = new HashMap<>();
        Page<Resource> resources = this.resourceRepository.findAll(params, pageable).map(this::entityToModel);
        for (Resource resource : resources.getContent()) {
            ResourceID resourceID = resource.getResourceID();
            if (resourceID == null) {
                resource.setResourceState(ResourceState.UNKNOWN);
                continue;
            }
            ResourceOperatorTag tag = resource.getResourceOperatorTag();
            Optional<Object> optional = get(resourceID, tag, typeAndTag2Resources);
            if (!optional.isPresent()) {
                resource.setResourceState(ResourceState.UNKNOWN);
                continue;
            }
            resource.setResourceConfig(optional.get());
            ResourceState newState = moveToNextState(resource.getResourceState(),
                    getResourceOperator(resourceID.getType(), tag), optional.get());
            List<Long> list = state2Resources.computeIfAbsent(newState, key -> new ArrayList<>());
            list.add(resource.getId());
        }
        state2Resources.forEach((key, value) -> {
            resourceRepository.updateResourceStateIdIn(value, key);
        });
        return resources;
    }

    public List<Resource> list(@NonNull Map<ResourceID, ResourceOperatorTag> parameters) throws Exception {
        Map<Class<?>, Map<ResourceOperatorTag, List<Object>>> typeAndTag2Resources = new HashMap<>();
        List<Resource> returnVal = new ArrayList<>();
        for (Map.Entry<ResourceID, ResourceOperatorTag> entry : parameters.entrySet()) {
            ResourceID id = entry.getKey();
            ResourceOperatorTag tag = entry.getValue();
            Resource item = new Resource();
            item.setResourceID(id);
            Object cfg = get(id, tag, typeAndTag2Resources)
                    .orElseThrow(() -> new IllegalStateException("No Resource found by id " + id + " and tag " + tag));
            item.setResourceConfig(cfg);
            item.setResourceOperatorTag(tag);
            returnVal.add(item);
        }
        return returnVal;
    }

    public Resource nullSafeGet(@NonNull Long id) throws Exception {
        Optional<ResourceEntity> optional = this.resourceRepository.findById(id);
        ResourceEntity resourceEntity =
                optional.orElseThrow(() -> new IllegalStateException("No resource found by id " + id));
        Resource resource = entityToModel(resourceEntity);
        ResourceID resourceID = resource.getResourceID();
        if (resourceID == null) {
            this.resourceRepository.updateResourceStateById(id, ResourceState.UNKNOWN);
            resource.setResourceState(ResourceState.UNKNOWN);
            return resource;
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

    public Resource nullSafeGet(@NonNull ResourceID resourceID, @NonNull ResourceOperatorTag tag) throws Exception {
        ResourceOperator<?> resourceOperator = getResourceOperator(resourceID.getType(), tag);
        Object resourceConfig = resourceOperator.get(resourceID)
                .orElseThrow(() -> new IllegalStateException("No resource found by resource key " + resourceID));
        Resource resource = new Resource();
        resource.setResourceOperatorTag(tag);
        resource.setResourceID(resourceID);
        resource.setResourceConfig(resourceConfig);
        return resource;
    }

    @Transactional
    public Resource destroy(@NonNull Long id) throws Exception {
        Resource resource = nullSafeGet(id);
        this.resourceRepository.updateResourceStateById(id, ResourceState.DESTROYING);
        ResourceID resourceID = resource.getResourceID();
        getResourceOperator(resourceID.getType(), resource.getResourceOperatorTag()).destroy(resourceID);
        log.info("Delete resource succeed, id={}, resourceId={}", id, resourceID);
        return resource;
    }

    public Resource destroy(@NonNull ResourceID resourceID, @NonNull ResourceOperatorTag tag) throws Exception {
        Resource resource = nullSafeGet(resourceID, tag);
        ResourceID rId = resource.getResourceID();
        getResourceOperator(rId.getType(), resource.getResourceOperatorTag()).destroy(rId);
        log.info("Delete resource succeed, resourceId={}, tag={}", rId, tag);
        return resource;
    }

    @SuppressWarnings("all")
    private Optional<Object> get(ResourceID resourceID, ResourceOperatorTag tag,
            Map<Class<?>, Map<ResourceOperatorTag, List<Object>>> typeAndTag2Resources) throws Exception {
        Class<?> type = resourceID.getType();
        Map<ResourceOperatorTag, List<Object>> tag2Operator = typeAndTag2Resources
                .computeIfAbsent(type, c -> new HashMap<>());
        List<Object> resourceList = tag2Operator.get(tag);
        ResourceOperator<Object> resourceOperator = (ResourceOperator<Object>) getResourceOperator(type, tag);
        if (resourceList == null) {
            resourceList = new ArrayList<>(resourceOperator.list());
            tag2Operator.put(tag, resourceList);
        }
        List<Object> list = resourceList.stream()
                .filter(o -> resourceID.equals(resourceOperator.getKey(o)))
                .collect(Collectors.toList());
        if (list.size() > 1) {
            throw new IllegalStateException("There are more than one resource found by id " + resourceID);
        }
        return list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
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
