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
package com.oceanbase.odc.service.iam.auth.bastion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.bastion.BastionAccountService;
import com.oceanbase.odc.service.bastion.model.BastionAccount;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("bastionUserDetailService")
@SkipAuthorize("odc internal usage")
public class BastionUserDetailService implements AuthenticationUserDetailsService<BastionAuthenticationToken> {

    @Autowired
    private UserService userService;

    @Autowired
    private BastionAccountService bastionAccountService;

    @Autowired
    private ProjectService projectService;

    @Override
    public UserDetails loadUserDetails(BastionAuthenticationToken token) throws UsernameNotFoundException {
        PreConditions.notNull(token, "token");
        PreConditions.notNull(token.getCredentials(), "token.credentials");
        String apiToken = (String) token.getCredentials();
        BastionAccount bastionAccount = bastionAccountService.query(apiToken);
        String username = bastionAccount.getUsername();
        String nickName = bastionAccount.getNickName();
        TraceContextHolder.setAccountName(username);
        UserEntity entity = UserEntity.autoCreatedEntity(username, nickName, OdcConstants.DEFAULT_ORGANIZATION_ID);
        entity.setDescription("Auto generated user for bastion integration");
        User user = userService.upsert(entity, null);
        TraceContextHolder.setUserId(user.getId());
        TraceContextHolder.setOrganizationId(user.getOrganizationId());
        return user;
    }
}
