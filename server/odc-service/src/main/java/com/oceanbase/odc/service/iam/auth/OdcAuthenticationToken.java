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

import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.service.iam.model.User;

/**
 * AuthenticationToken just for ODC application
 *
 * @author yh263208
 * @date 2021-08-02 16:07
 * @since ODC_release_3.2.0
 */
public class OdcAuthenticationToken extends BaseAuthenticationToken<User, String> {

    /**
     * Base Constructor for authentication token, used to fullfill the contents of principal and
     * credential
     *
     * @param principal see
     *        <code>com.oceanbase.odc.authority.core.model.AuthenticationInfo#getPrincipal</code>
     * @param credential credential to verify yourself
     * @throws IllegalArgumentException principal and credential can not be null
     */
    public OdcAuthenticationToken(User principal, String credential) {
        super(principal, credential);
    }

}
