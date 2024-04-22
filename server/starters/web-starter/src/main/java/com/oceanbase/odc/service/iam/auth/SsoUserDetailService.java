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

import static com.oceanbase.odc.core.shared.constant.ResourceType.ODC_ORGANIZATION;
import static com.oceanbase.odc.service.automation.model.TriggerEvent.OAUTH_2_FIRST_TIME_LOGIN;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.oauth2.MappingResult;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/10/12 下午3:57
 * @Description: [This class is responsible for integrating BUC user into ODC]
 */
@Service
@ConditionalOnProperty(value = "odc.iam.auth.type", havingValues = {"local"})
@Slf4j
@SkipAuthorize("odc internal usage")
public class SsoUserDetailService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApplicationContext applicationContext;


    public User getOrCreateUser(@NonNull MappingResult mappingResult) {

        Map<String, Object> sourceUserInfoMap = mappingResult.getSourceUserInfoMap();
        String accountName = mappingResult.getUserAccountName();
        String nickName = mappingResult.getUserNickName();
        if (accountName == null) {
            log.error("Get or create user failed, user infoMap={}, mappingResult={}", sourceUserInfoMap,
                    JsonUtils.toJson(mappingResult));
        }
        TraceContextHolder.setAccountName(accountName);
        OrganizationEntity organization = organizationRepository.findById(mappingResult.getOrganizationId())
                .orElseThrow(() -> new NotFoundException(ODC_ORGANIZATION, "id", mappingResult.getOrganizationId()));
        UserEntity entity = UserEntity.autoCreatedEntity(accountName, MoreObjects.firstNonNull(nickName, accountName),
                organization.getId());
        entity.setDescription("Auto generated user for OAuth2 integration");
        entity.setExtraPropertiesJson(mappingResult.getExtraInfo());
        User user = userService.upsert(entity, null);
        sourceUserInfoMap.put("odcUserId", user.getId());
        sourceUserInfoMap.put("organizationId", user.getOrganizationId());
        applicationContext.publishEvent(new TriggerEvent(OAUTH_2_FIRST_TIME_LOGIN, sourceUserInfoMap));
        return user;
    }

}
