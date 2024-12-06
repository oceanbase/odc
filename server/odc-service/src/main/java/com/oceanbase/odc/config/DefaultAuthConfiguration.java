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

import java.util.Arrays;
import java.util.Collection;

import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.ResourcePermissionExtractor;
import com.oceanbase.odc.service.iam.ResourceRoleBasedPermissionExtractor;
import com.oceanbase.odc.service.iam.auth.ComposedAuthorizer;
import com.oceanbase.odc.service.iam.auth.DefaultAuthorizer;
import com.oceanbase.odc.service.iam.auth.ResourceRoleAuthorizer;

/**
 * {@link DefaultAuthConfiguration}
 *
 * @author yh263208
 * @date 2022-06-07 17:22
 * @since ODC_release_3.4.0
 * @see BaseAuthConfiguration
 */
public class DefaultAuthConfiguration extends BaseAuthConfiguration {

    @Override
    protected Collection<Authorizer> authorizers(PermissionRepository permissionRepository,
            ResourcePermissionExtractor resourcePermissionExtractor, UserResourceRoleRepository resourceRoleRepository,
            ResourceRoleBasedPermissionExtractor resourceRoleBasedPermissionExtractor) {
        DefaultAuthorizer defaultAuthorizer = new DefaultAuthorizer(permissionRepository, resourcePermissionExtractor);
        ResourceRoleAuthorizer resourceRoleAuthorizer =
                new ResourceRoleAuthorizer(resourceRoleRepository, resourceRoleBasedPermissionExtractor);
        ComposedAuthorizer composedAuthorizer = new ComposedAuthorizer(defaultAuthorizer, resourceRoleAuthorizer);
        return Arrays.asList(defaultAuthorizer, resourceRoleAuthorizer, composedAuthorizer);
    }

}
