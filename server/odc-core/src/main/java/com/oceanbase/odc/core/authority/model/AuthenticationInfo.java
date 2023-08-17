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

import java.io.Serializable;
import java.security.Principal;

/**
 * AuthenticationInfo for Authenticator, used to display the verification information of a subject
 *
 * @author yh263208
 * @date 2021-07-08 15:47
 * @since ODC_release_3.2.0
 */
public interface AuthenticationInfo<T extends Principal, V> extends Principal, Serializable {
    /**
     * A user or an account may have multi principals. A user named David may have different identities,
     * such as an engineer of a company or a member of his family.
     *
     * These different identities called {@link Principal} can be authenticated by different
     * Authenticators. Each authenticator can provide us one {@link Principal}
     *
     * @return principal object
     */
    T getPrincipal();

    /**
     * If a user wants to get an identity called <code>Principal</code>, and he needs a credential to
     * authenticate himself. This <code>credential</code> can be a password or private key.
     *
     * @return credential object
     */
    V getCredential();

}
