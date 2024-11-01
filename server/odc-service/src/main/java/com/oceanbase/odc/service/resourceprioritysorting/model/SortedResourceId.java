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
package com.oceanbase.odc.service.resourceprioritysorting.model;

import com.oceanbase.odc.core.shared.PreConditions;

import lombok.Getter;

/**
 * @author keyang
 * @date 2024/11/01
 * @since 4.3.2
 */
@Getter
public class SortedResourceId extends SortedResourceType {
    private final Long sortedResourceId;

    public SortedResourceId(String resourceType, Long resourceId, String sortedResourceType, Long sortedResourceId) {
        super(resourceType, resourceId, sortedResourceType);
        PreConditions.notNull(sortedResourceId, "sortedResourceId");
        this.sortedResourceId = sortedResourceId;
    }

    public SortedResourceId(SortedResourceType sortedResourceType, Long sortedResourceId) {
        this(sortedResourceType.getResourceType(), sortedResourceType.getResourceId(),
                sortedResourceType.getSortedResourceType(), sortedResourceId);
    }
}
