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

    public final <T extends OrganizationIsolated> void checkCurrentOrganization(List<T> objects) {
        Validate.notNull(objects,
                "Resources can not be null for HorizontalDataPermissionValidator#checkCurrentOrganization");
        Long currentOrganizationId = authenticationFacade.currentOrganizationId();
        for (T item : objects) {
            Long organizationId = item.organizationId();
            Verify.notNull(organizationId, "organizationId");
            PreConditions.validExists(ResourceType.valueOf(item.resourceType()), "id", item.id(),
                    () -> currentOrganizationId.equals(organizationId));
        }
    }

}
