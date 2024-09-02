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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.resource.builder.ResourceOperatorBuilder;
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

    public <T> T create(@NonNull T config) throws Exception {
        return getResourceOperatorByConfig(config).create(config);
    }

    public <T> Optional<T> query(@NonNull Object key, @NonNull Class<T> clazz) throws Exception {
        return getResourceOperatorByConfig(clazz).query(key);
    }

    public <T> void destroy(@NonNull Object key, @NonNull Class<T> clazz) throws Exception {
        getResourceOperatorByConfig(clazz).destroy(key);
    }

    public <T> List<T> list(@NonNull Class<T> clazz) throws Exception {
        return getResourceOperatorByConfig(clazz).list();
    }

    protected Map<String, Object> getParameterForOperatorBuilder(@NonNull ResourceOperatorBuilder<?, ?> builder) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put(ResourceConstants.DEFAULT_NAMESPACE_PARAMETER_KEY, "shanlu");
        return parameter;
    }

    @SuppressWarnings("all")
    protected <T, ID> ResourceOperator<T, ID> getResourceOperatorByConfig(T config) {
        return (ResourceOperator<T, ID>) getResourceOperatorByConfig(config.getClass());
    }

    @SuppressWarnings("all")
    protected <T, ID> ResourceOperator<T, ID> getResourceOperatorByConfig(Class<T> clazz) {
        List<ResourceOperatorBuilder<?, ?>> builders = this.resourceOperatorBuilders.stream()
                .filter(builder -> builder.supports(clazz)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(builders)) {
            throw new IllegalArgumentException("No builder found for config " + clazz);
        } else if (builders.size() != 1) {
            throw new IllegalStateException("There are more than one builder for the config " + clazz);
        }
        ResourceOperatorBuilder<T, ID> builder = (ResourceOperatorBuilder<T, ID>) builders.get(0);
        return builder.build(getParameterForOperatorBuilder(builder));
    }

}
