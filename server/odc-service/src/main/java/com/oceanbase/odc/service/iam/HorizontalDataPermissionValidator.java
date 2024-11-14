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
package com.oceanbase.odc.service.iam;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

@Component
public class HorizontalDataPermissionValidator {

    @Autowired
    private AuthenticationFacade authenticationFacade;

    public final <T extends OrganizationIsolated> void checkCurrentOrganization(T object) {
        checkCurrentOrganization(Collections.singletonList(object));
    }

    /**
     * 检查当前组织机构是否有权限访问给定的对象列表
     *
     * @param objects 对象列表
     * @param <T>     对象类型，必须实现OrganizationIsolated接口
     */
    public final <T extends OrganizationIsolated> void checkCurrentOrganization(List<T> objects) {
        // 检查对象列表是否为空
        Validate.notNull(objects,
            "Resources can not be null for HorizontalDataPermissionValidator#checkCurrentOrganization");
        // 获取当前组织机构ID
        Long currentOrganizationId = authenticationFacade.currentOrganizationId();
        // 遍历对象列表
        for (T item : objects) {
            // 获取对象的组织机构ID
            Long organizationId = item.organizationId();
            // 检查组织机构ID是否为空
            Verify.notNull(organizationId, "organizationId");
            // 检查当前组织机构ID是否与对象的组织机构ID相同
            PreConditions.validExists(ResourceType.valueOf(item.resourceType()), "id", item.id(),
                () -> currentOrganizationId.equals(organizationId));
        }
    }

}
