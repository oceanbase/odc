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
package com.oceanbase.odc.service.resource.builder;

import com.oceanbase.odc.service.resource.model.ResourceID;
import com.oceanbase.odc.service.resource.model.ResourceTag;
import com.oceanbase.odc.service.resource.operator.ResourceOperator;

import lombok.NonNull;

/**
 * {@link ResourceOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-02 16:32
 * @since ODC_release_4.3.2
 */
public interface ResourceOperatorBuilder<T, ID extends ResourceID> {
    /**
     * Indicates whether the current builder supports a certain type of resource
     *
     * @param clazz resource type
     */
    boolean supports(@NonNull Class<?> clazz);

    /**
     * We cannot know the parameters for constructing a {@link ResourceOperator} in advance, so we use a
     * map as a parameter.
     *
     * @param resourceTag parameters to build a {@link ResourceOperator}
     */
    ResourceOperator<T, ID> build(@NonNull ResourceTag resourceTag);

}
