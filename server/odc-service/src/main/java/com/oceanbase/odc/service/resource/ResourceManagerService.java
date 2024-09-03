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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.PreConditions;
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

    @Autowired(required = false)
    private List<ResourceOperatorBuilder<?, ?>> resourceOperatorBuilders;

    public Resource create(@NonNull Resource config) throws Exception {
        Object resourceConfig = config.getResourceConfig();
        ResourceOperatorTag resourceOperatorTag = config.getResourceOperatorTag();
        PreConditions.notNull(resourceConfig, "ResourceConfig");
        PreConditions.notNull(resourceOperatorTag, "ResourceOperatorTag");

        ResourceOperator<Object, ? extends ResourceID> operator =
                getResourceOperator(resourceConfig, resourceOperatorTag);
        resourceConfig = operator.create(resourceConfig);
        Resource resource = new Resource();
        resource.setResourceConfig(resourceConfig);
        resource.setResourceOperatorTag(resourceOperatorTag);
        resource.setResourceState(ResourceState.CREATING);
        resource.setResourceID(operator.getKey(resourceConfig));
        return resource;
    }

    public Resource get(@NonNull Long id) throws Exception {
        Resource resource = findById(id).orElseThrow(() -> new IllegalStateException("No resource found by id " + id));
        ResourceID resourceID = resource.getResourceID();
        ResourceOperator<?, ResourceID> resourceOperator =
                getResourceOperator(resourceID.getType(), resource.getResourceOperatorTag());
        resource.setResourceConfig(resourceOperator.get(resourceID)
                .orElseThrow(() -> new IllegalStateException("No resource found by resource key " + resourceID)));
        return resource;
    }

    public int destroy(@NonNull Long id) throws Exception {
        Resource resource = get(id);
        ResourceID resourceID = resource.getResourceID();
        getResourceOperator(resourceID.getType(), resource.getResourceOperatorTag()).destroy(resourceID);
        return deleteById(id);
    }

    public List<Resource> list() throws Exception {
        return Collections.emptyList();
    }

    @SuppressWarnings("all")
    private <T, ID extends ResourceID> ResourceOperator<T, ID> getResourceOperator(
            T config, ResourceOperatorTag resourceOperatorTag) {
        return (ResourceOperator<T, ID>) getResourceOperator(config.getClass(), resourceOperatorTag);
    }

    @SuppressWarnings("all")
    private <T, ID extends ResourceID> ResourceOperator<T, ID> getResourceOperator(
            Class<T> clazz, ResourceOperatorTag resourceOperatorTag) {
        List<ResourceOperatorBuilder<?, ?>> builders = this.resourceOperatorBuilders.stream()
                .filter(builder -> builder.supports(clazz)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(builders)) {
            throw new IllegalArgumentException("No builder found for config " + clazz);
        } else if (builders.size() != 1) {
            throw new IllegalStateException("There are more than one builder for the config " + clazz);
        }
        return ((ResourceOperatorBuilder<T, ID>) builders.get(0)).build(resourceOperatorTag);
    }

    private Optional<Resource> findById(@NonNull Long id) {
        return Optional.of(new Resource());
    }

    private int deleteById(@NonNull Long id) {
        return 1;
    }

}
