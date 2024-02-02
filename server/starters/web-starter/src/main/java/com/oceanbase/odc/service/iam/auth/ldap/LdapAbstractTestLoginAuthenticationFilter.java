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

import javax.servlet.http.HttpServletRequest;

import com.oceanbase.odc.service.iam.auth.local.AbstractTestLoginAuthenticationFilter;
import com.oceanbase.odc.service.integration.model.LdapContextHolder;
import com.oceanbase.odc.service.integration.model.LdapContextHolder.LdapContext;
import com.oceanbase.odc.service.integration.oauth2.TestLoginManager;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LdapAbstractTestLoginAuthenticationFilter extends AbstractTestLoginAuthenticationFilter {

    TestLoginManager testLoginManager;

    @Override
    protected Boolean isTestRequest(HttpServletRequest request) {
        LdapContext ldapContext = testLoginManager.loadLdapContext(request);
        return ldapContext != null && ldapContext.isTest();
    }

    @Override
    protected void finallyCallback() {
        LdapContextHolder.clear();
    }
}
