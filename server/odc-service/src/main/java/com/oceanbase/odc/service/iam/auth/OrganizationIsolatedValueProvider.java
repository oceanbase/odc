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
package com.oceanbase.odc.service.iam.auth;

import java.util.Collection;
import java.util.Iterator;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.AuthorizerManager;
import com.oceanbase.odc.core.authority.auth.DefaultReturnValueProvider;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.OrganizationIsolated;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Provider object, used to filter the return value of a method
 *
 * @author yh263208
 * @date 2021-08-02 21:28
 * @since ODC_release_3.2.0
 * @see ReturnValueProvider
 */
@Slf4j
public class OrganizationIsolatedValueProvider extends DefaultReturnValueProvider {

    private final AuthenticationFacade authenticationFacade;

    public OrganizationIsolatedValueProvider(@NonNull AuthorizerManager authorizerManager,
            @NonNull PermissionStrategy strategy,
            @NonNull AuthenticationFacade authenticationFacade) {
        super(authorizerManager, strategy);
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    public Object decide(Subject subject, Object returnValue, SecurityContext context)
            throws AccessDeniedException {
        if (returnValue instanceof Collection) {
            Collection collection = (Collection) returnValue;
            Iterator iterator = collection.iterator();
            while (iterator.hasNext()) {
                checkOrganizationIsolated(iterator.next());
            }
        }
        checkOrganizationIsolated(returnValue);
        return super.decide(subject, returnValue, context);
    }

    private void checkOrganizationIsolated(Object returnValue) {
        if (returnValue instanceof OrganizationIsolated) {
            long expected = authenticationFacade.currentOrganizationId();
            long actual = ((OrganizationIsolated) returnValue).organizationId().longValue();
            if (expected != actual) {
                log.warn("organizationId not matched, expected={}, actual={}", expected, actual);
                throw new AccessDeniedException();
            }
        }
    }

}
