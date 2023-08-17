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
package com.oceanbase.odc.service.iam.auth.oauth2;

import static com.oceanbase.odc.core.shared.constant.ResourceType.ODC_ORGANIZATION;
import static com.oceanbase.odc.service.automation.model.TriggerEvent.OAUTH_2_FIRST_TIME_LOGIN;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;
import com.oceanbase.odc.service.iam.UserOrganizationService;
import com.oceanbase.odc.service.iam.UserService;
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
public class OAuth2UserDetailService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserOrganizationService userOrganizationService;

    public User getOrCreateUser(@NonNull MappingResult mappingResult) {

        Map<String, Object> sourceUserInfoMap = mappingResult.getSourceUserInfoMap();
        String userAccountName = mappingResult.getUserAccountName();
        if (userAccountName == null) {
            log.error("get or create user failed,user info map = " + sourceUserInfoMap + " mapping resulet = "
                    + JsonUtils.toJson(mappingResult));
        }
        TraceContextHolder.setAccountName(userAccountName);

        Optional<UserEntity> userEntityOptional =
                userRepository.findByAccountName(userAccountName);
        if (userEntityOptional.isPresent()) {
            String parseExtraInfo = mappingResult.getExtraInfo();
            UserEntity userEntity = userEntityOptional.get();
            if (!Objects.equals(userEntity.getExtraPropertiesJson(), parseExtraInfo)) {
                userEntity.setExtraPropertiesJson(parseExtraInfo);
                userService.save(userEntity);
                log.info("update user entity , accountName=" + userEntity.getAccountName() + " , old extra properties="
                        + userEntity.getExtraPropertiesJson()
                        + "  and parse properties = " + parseExtraInfo);
            }
            log.info("User already exists, accountName={}", userEntityOptional.get().getAccountName());
            return new User(userEntityOptional.get());
        }

        User user = transactionTemplate.execute(status -> {
            try {
                OrganizationEntity organization =
                        organizationRepository.findById(mappingResult.getOrganizationId()).orElseThrow(
                                () -> new NotFoundException(ODC_ORGANIZATION, "id", mappingResult.getOrganizationId()));
                UserEntity userEntity =
                        initUser(userAccountName, mappingResult.getUserNickName(), organization.getId(),
                                mappingResult.getExtraInfo());
                userService.save(userEntity);
                return new User(userRepository.findByAccountName(userEntity.getAccountName()).get());
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });


        sourceUserInfoMap.put("odcUserId", user.getId());
        sourceUserInfoMap.put("organizationId", user.getOrganizationId());
        applicationContext.publishEvent(new TriggerEvent(OAUTH_2_FIRST_TIME_LOGIN, sourceUserInfoMap));
        return user;
    }

    private UserEntity initUser(String accountName, @Nullable String name, Long orgId, String extraInfo) {

        UserEntity userEntity = new UserEntity();
        userEntity.setAccountName(accountName);
        // 兜底策略，当所有名称字段均为空时，展示账户名称。
        userEntity.setName(MoreObjects.firstNonNull(name, accountName));
        userEntity.setType(UserType.USER);
        userEntity.setPassword(encoder.encode(PasswordUtils.random()));
        userEntity.setEnabled(true);
        userEntity.setCipher(Cipher.BCRYPT);
        userEntity.setActive(true);
        userEntity.setCreatorId(1L);
        userEntity.setBuiltIn(false);
        userEntity.setOrganizationId(orgId);
        userEntity.setDescription("Auto generated user when login from OAuth2");
        userEntity.setUserCreateTime(new Timestamp(System.currentTimeMillis()));
        userEntity.setUserUpdateTime(new Timestamp(System.currentTimeMillis()));
        userEntity.setExtraPropertiesJson(extraInfo);
        return userEntity;
    }
}
