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
package com.oceanbase.odc.service.iam.auth.play;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/12/21 下午8:29
 * @Description: [This class is responsible for acquiring OB Official Website users via calling Open
 *               API and load corresponding ODC user into Spring Security Framework]
 */
@Slf4j
@Service("alipayUserDetailService")
@ConditionalOnProperty(value = "odc.iam.auth.type", havingValue = "alipay")
@SkipAuthorize("odc internal usage")
public class PlaysiteUserDetailService
        implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        PreConditions.notNull(token, "token");
        log.info("Start authentication by calling Alipay OpenAPI");
        String accountName = token.getName();
        // 如果是新用户，则用 passportId 作为 accountName 创建一个新的 ODC 用户
        // 官网账号 API 不提供昵称，使用 accountName 作为 name
        UserEntity entity =
                UserEntity.autoCreatedEntity(accountName, accountName, OdcConstants.DEFAULT_ORGANIZATION_ID);
        entity.setDescription("Auto generated user for play site");
        User user = userService.upsert(entity, null);
        TraceContextHolder.setUserId(user.getId());
        TraceContextHolder.setOrganizationId(user.getOrganizationId());
        return user;
    }
}
