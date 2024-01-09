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

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.service.iam.auth.MappingRuleConvert;
import com.oceanbase.odc.service.iam.auth.SSOUserDetailService;
import com.oceanbase.odc.service.iam.auth.oauth2.MappingResult;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LdapUserDetailsContextMapper implements UserDetailsContextMapper {

    @Autowired
    MappingRuleConvert mappingRuleConvert;

    @Autowired
    SSOUserDetailService ssoUserDetailService;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {
        MappingResult mappingResult = mappingRuleConvert.resolveLdapMappingResult(ctx, username);
        User user = ssoUserDetailService.getOrCreateUser(mappingResult);
        TraceContextHolder.setUserId(user.getId());
        TraceContextHolder.setOrganizationId(user.getOrganizationId());
        return user;
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {

    }
}
