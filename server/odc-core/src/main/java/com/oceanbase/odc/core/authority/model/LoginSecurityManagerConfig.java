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

import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.AuthorizerManager;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;

import lombok.Builder;
import lombok.Getter;

/**
 * {@link LoginSecurityManagerConfig}
 *
 * @author yh263208
 * @date 2022-06-13 16:50
 * @since ODC_release_3.4.0
 */
@Getter
@Builder
public class LoginSecurityManagerConfig {
    private final AuthenticatorManager authenticatorManager;
    private final AuthorizerManager authorizerManager;
    private final PermissionStrategy permissionStrategy;
    private final ReturnValueProvider returnValueProvider;
    private final SecuritySessionManager sessionManager;
    private final PermissionProvider permissionProvider;
}
