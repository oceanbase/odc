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
package com.oceanbase.odc.core.authority.model;

/**
 * Security framework is designed or worked for a resource. If a java object is not resource,
 * security framework will not work. This interface is to mark a java object as a resource to enable
 * security framework to manage this resource
 *
 * @author yh263208
 * @date 2021-07-12 16:26
 * @since ODC_release_3.2.0
 */
public interface SecurityResource {
    /**
     * Get resource id
     *
     * @return resource ID
     */
    String resourceId();

    /**
     * Get resource type, we use {@code resourceId} and {@code resourceType} to identify a resource in
     * database or file
     *
     * @return resource type
     */
    String resourceType();

}
