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
package com.oceanbase.odc.core.authority;

import java.util.Map;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.AuthorizerManager;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;

import lombok.Getter;
import lombok.NonNull;

/**
 * The abstract return value manager {@link BaseValueFilterSecurityManager}, the security manager
 * manages the return value manager {@link ReturnValueProvider}, and the return value filtering
 * operation of the proxy return value manager {@link ReturnValueProvider}
 *
 * @author yh263208
 * @date 2021-07-21 17:08
 * @since ODC-release_3.2.0
 * @see BaseAuthorizerSecurityManager
 */
abstract class BaseValueFilterSecurityManager extends BaseAuthorizerSecurityManager {

    @Getter
    private final ReturnValueProvider returnValueProvider;

    public BaseValueFilterSecurityManager(AuthenticatorManager authenticatorManager,
            AuthorizerManager authorizerManager, PermissionStrategy permissionStrategy,
            @NonNull ReturnValueProvider returnValueProvider) {
        super(authenticatorManager, authorizerManager, permissionStrategy);
        this.returnValueProvider = returnValueProvider;
    }

    /**
     * Judgment method by which to determine whether an operating subject has operating authority for
     * the target resource. If there is no operation permission, the user decides whether to return a
     * null value or throw an {@link AccessDeniedException}
     *
     * @param subject Authentication subject
     * @param returnValue return value for method invocation
     * @param context Authentication context
     * @exception AccessDeniedException exception will be thrown when caller throw it
     */
    @Override
    public Object decide(Subject subject, Object returnValue, SecurityContext context)
            throws AccessDeniedException {
        if (returnValue instanceof SecurityResource || returnValue instanceof Iterable
                || returnValue instanceof Map || returnValue instanceof OrganizationIsolated) {
            return this.returnValueProvider.decide(subject, returnValue, context);
        }
        return returnValue;
    }

    @Override
    public void close() throws Exception {
        Exception thrown = null;
        try {
            super.close();
        } catch (Exception e) {
            thrown = e;
        }
        if (this.returnValueProvider instanceof AutoCloseable) {
            ((AutoCloseable) this.returnValueProvider).close();
        }
        if (thrown != null) {
            throw thrown;
        }
    }

}
