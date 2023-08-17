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
package com.oceanbase.odc.service.resourcegroup.model;

import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link ResourceIdentifier} to identify a resource by resource type and resource id
 *
 * @author yh263208
 * @date 2021-07-29 14:37
 * @since ODC-release_3.2.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(exclude = "name")
public class ResourceIdentifier {

    private Long resourceId;
    private String name;
    private ResourceType resourceType;

    public ResourceIdentifier(Long resourceId, @NonNull ResourceType resourceType) {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }
}
