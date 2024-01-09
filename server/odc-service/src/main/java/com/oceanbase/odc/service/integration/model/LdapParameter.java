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
package com.oceanbase.odc.service.integration.model;

import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseOrganizationId;

import org.springframework.ldap.core.AuthenticationSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class LdapParameter implements SSOParameter {

    private String registrationId;

    private String server;

    private String managerDn;

    @JsonProperty(access = Access.WRITE_ONLY)
    private String managerPassword;

    private String userSearchBase;

    private String userSearchFilter;

    private String groupSearchBase;

    private String groupSearchFilter;

    private Boolean groupSearchSubtree = false;

    public DefaultSpringSecurityContextSource acquireContextSource() {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                server);
        if (this.managerDn != null) {
            contextSource.setUserDn(this.managerDn);
            if (this.managerPassword == null) {
                throw new IllegalStateException("managerPassword is required if managerDn is supplied");
            }
            contextSource.setPassword(this.managerPassword);
        }
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    public void amendTest() {
        registrationId = parseOrganizationId(registrationId) + "-" + "test";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    static class SimpleAuthenticationSource implements AuthenticationSource {

        private String userDn;

        private String password;

        public String getPrincipal() {
            return userDn;
        }

        public String getCredentials() {
            return password;
        }
    }
}
