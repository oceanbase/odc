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
package com.oceanbase.odc.service.iam.auth.ldap;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticator;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.integration.ldap.LdapConfigRegistrationManager;
import com.oceanbase.odc.service.integration.model.LdapContextHolder;
import com.oceanbase.odc.service.integration.model.LdapParameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

public class ODCLdapAuthenticator implements LdapAuthenticator {

    private final LdapConfigRegistrationManager ldapConfigRegistrationManager;

    public ODCLdapAuthenticator(LdapConfigRegistrationManager ldapConfigRegistrationManager) {
        this.ldapConfigRegistrationManager = ldapConfigRegistrationManager;
    }

    @Override
    public DirContextOperations authenticate(Authentication authentication) {
        Preconditions.checkArgument(authentication instanceof LdapPasswordAuthenticationToken,
                "ldapPasswordAuthenticationToken");

        SSOIntegrationConfig parameter = ldapConfigRegistrationManager.findByRegistrationId(
                LdapContextHolder.getRegistrationId());
        LdapParameter ssoParameter = (LdapParameter) parameter.getSsoParameter();
        BindAuthenticator bindAuthenticator = new BindAuthenticator(ssoParameter.acquireContextSource());
        bindAuthenticator.setUserSearch(new ODCLdapUserSearch(ldapConfigRegistrationManager));
        return bindAuthenticator.authenticate(authentication);
    }


}
