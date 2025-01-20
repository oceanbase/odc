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
package com.oceanbase.odc.config;

import java.util.Collection;
import java.util.Collections;

import org.springframework.context.annotation.Bean;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.auth.Authenticator;
import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.auth.AuthorizerManager;
import com.oceanbase.odc.core.authority.auth.DefaultAuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.DefaultAuthorizerManager;
import com.oceanbase.odc.core.authority.auth.DefaultPermissionStrategy;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.model.LoginSecurityManagerConfig;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.session.factory.DefaultSecuritySessionFactory;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.ResourcePermissionExtractor;
import com.oceanbase.odc.service.iam.ResourceRoleBasedPermissionExtractor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.DefaultPermissionProvider;
import com.oceanbase.odc.service.iam.auth.EmptyAuthenticator;
import com.oceanbase.odc.service.iam.auth.EmptySessionManager;
import com.oceanbase.odc.service.iam.auth.OdcSecurityManager;
import com.oceanbase.odc.service.iam.auth.OrganizationIsolatedValueProvider;

/**
 * {@link BaseAuthConfiguration}
 *
 * @author yh263208
 * @date 2022-07-07 16:47
 * @since ODC_release_3.4.0
 */
public abstract class BaseAuthConfiguration {

    @Bean
    public SecurityManager servletSecurityManager(PermissionRepository permissionRepository,
            ResourcePermissionExtractor permissionMapper, UserResourceRoleRepository resourceRoleRepository,
            ResourceRoleBasedPermissionExtractor resourceRoleBasedPermissionExtractor,
            AuthenticationFacade authenticationFacade) {
        Collection<Authorizer> authorizers = authorizers(permissionRepository, permissionMapper, resourceRoleRepository,
                resourceRoleBasedPermissionExtractor);
        DefaultAuthorizerManager authorizerManager = new DefaultAuthorizerManager(authorizers);
        PermissionStrategy permissionStrategy = permissionStrategy();
        AuthenticatorManager authenticatorManager = new DefaultAuthenticatorManager(authenticators());
        EmptySessionManager sessionManager = new EmptySessionManager(authenticationFacade, authenticatorManager,
                new DefaultSecuritySessionFactory());
        LoginSecurityManagerConfig config = LoginSecurityManagerConfig.builder()
                .permissionStrategy(permissionStrategy)
                .authenticatorManager(authenticatorManager)
                .authorizerManager(authorizerManager)
                .sessionManager(sessionManager)
                .permissionProvider(permissionProvider())
                .returnValueProvider(returnValueProvider(authorizerManager, permissionStrategy, authenticationFacade))
                .build();
        return new OdcSecurityManager(config, authenticationFacade);
    }

    protected Collection<Authenticator> authenticators() {
        return Collections.singletonList(new EmptyAuthenticator());
    }

    protected PermissionProvider permissionProvider() {
        return new DefaultPermissionProvider();
    }

    protected PermissionStrategy permissionStrategy() {
        return new DefaultPermissionStrategy();
    }

    protected ReturnValueProvider returnValueProvider(AuthorizerManager manager, PermissionStrategy strategy,
            AuthenticationFacade authenticationFacade) {
        return new OrganizationIsolatedValueProvider(manager, strategy, authenticationFacade);
    }

    protected abstract Collection<Authorizer> authorizers(PermissionRepository permissionRepository,
            ResourcePermissionExtractor resourcePermissionExtractor, UserResourceRoleRepository resourceRoleRepository,
            ResourceRoleBasedPermissionExtractor resourceRoleBasedPermissionExtractor);

}
