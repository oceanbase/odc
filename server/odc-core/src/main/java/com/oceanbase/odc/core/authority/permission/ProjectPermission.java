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
package com.oceanbase.odc.core.authority.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.model.SecurityResource;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/11/11 19:26
 * @Description: []
 */
@Getter
public class ProjectPermission extends ResourcePermission {
    private final String resourceId;
    private final String resourceType;
    private final List<String> actions;

    public ProjectPermission(@NonNull SecurityResource resource, String action) {
        super(resource.resourceId(), resource.resourceType(), action);
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        Validate.notNull(resourceId, "ResourceId can not be null");
        Validate.notNull(resourceType, "ResourceType can not be null");
        Validate.notEmpty(action);
        this.actions = Arrays.asList(StringUtils.split(action, ",")).stream().map(e -> e.trim().toUpperCase())
                .collect(Collectors.toList());
    }

    public ProjectPermission(@NonNull SecurityResource resource, List<String> actions) {
        super(resource.resourceId(), resource.resourceType(), actions.stream().collect(Collectors.joining(",")));
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        Validate.notNull(resourceId, "ResourceId can not be null");
        Validate.notNull(resourceType, "ResourceType can not be null");
        Validate.notEmpty(actions);
        this.actions = actions;
    }


    @Override
    public boolean implies(Permission permission) {
        if (this == permission) {
            return true;
        }
        if (!(permission instanceof ProjectPermission)) {
            return false;
        }
        ProjectPermission that = (ProjectPermission) permission;
        return (this.resourceId.equals(that.resourceId) || "*".equals(this.resourceId))
                && (this.resourceType.equals(that.resourceType) || "*".equals(this.resourceType))
                && !Collections.disjoint(this.actions, that.getActions());
    }
}
