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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.resource.builder.ResourceOperatorBuilder;
import com.oceanbase.odc.service.resource.model.ResourceID;
import com.oceanbase.odc.service.resource.model.ResourceTag;
import com.oceanbase.odc.service.resource.operator.ResourceOperator;

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

    public <T> T create(@NonNull T config, @NonNull ResourceTag resourceTag) throws Exception {
        return getResourceOperator(config, resourceTag).create(config);
    }

    public <T> Object getKey(@NonNull T config, @NonNull ResourceTag resourceTag) throws Exception {
        return getResourceOperator(config, resourceTag).getKey(config);
    }

    @SuppressWarnings("all")
    public <T> Optional<T> query(@NonNull ResourceID key, @NonNull ResourceTag resourceTag) throws Exception {
        return getResourceOperator((Class<T>) key.getType(), resourceTag).query(key);
    }

    @SuppressWarnings("all")
    public <T> void destroy(@NonNull ResourceID key, @NonNull ResourceTag resourceTag) throws Exception {
        getResourceOperator((Class<T>) key.getType(), resourceTag).destroy(key);
    }

    public <T> List<T> list(@NonNull Class<T> clazz, @NonNull ResourceTag resourceTag) throws Exception {
        return getResourceOperator(clazz, resourceTag).list();
    }

    @SuppressWarnings("all")
    protected <T, ID extends ResourceID> ResourceOperator<T, ID> getResourceOperator(
            T config, ResourceTag resourceTag) {
        return (ResourceOperator<T, ID>) getResourceOperator(config.getClass(), resourceTag);
    }

    @SuppressWarnings("all")
    protected <T, ID extends ResourceID> ResourceOperator<T, ID> getResourceOperator(
            Class<T> clazz, ResourceTag resourceTag) {
        List<ResourceOperatorBuilder<?, ?>> builders = this.resourceOperatorBuilders.stream()
                .filter(builder -> builder.supports(clazz)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(builders)) {
            throw new IllegalArgumentException("No builder found for config " + clazz);
        } else if (builders.size() != 1) {
            throw new IllegalStateException("There are more than one builder for the config " + clazz);
        }
        return ((ResourceOperatorBuilder<T, ID>) builders.get(0)).build(resourceTag);
    }

}
