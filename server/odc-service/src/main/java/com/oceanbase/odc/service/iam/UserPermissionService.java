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
package com.oceanbase.odc.service.iam;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.tools.loaddump.utils.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2022/12/19 10:33
 */

@Service
@Slf4j
@Validated
public class UserPermissionService {

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConnectionService connectionService;

    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    public void bindUserAndConnectionAccessPermission(@NonNull Long userId, Long connectionId, @NonNull String action,
            @NonNull Long creatorId) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);
        if (!userEntityOptional.isPresent()) {
            log.warn("User is invalid, id={}", userId);
            return;
        }
        UserEntity userEntity = userEntityOptional.get();
        if (Objects.nonNull(connectionId)) {
            try {
                ConnectionConfig connectionConfig = connectionService.getWithoutPermissionCheck(connectionId);
                if (!Objects.equals(userEntity.getOrganizationId(), connectionConfig.getOrganizationId())) {
                    throw new UnsupportedOperationException(String.format(
                            "Can not bind user and connection from different organization, userId=%s, connectionId=%s",
                            userId, connectionId));
                }
            } catch (NotFoundException ex) {
                log.warn("Connection is invalid, id={}", connectionId);
                return;
            }
        }
        String resourceIdentifier =
                ResourceType.ODC_CONNECTION + ":" + (Objects.nonNull(connectionId) ? connectionId : "*");
        List<UserPermissionEntity> userPermissionEntities =
                userPermissionRepository.findByUserIdAndOrganizationIdAndResourceIdentifierAndAction(userId,
                        userEntity.getOrganizationId(), resourceIdentifier, action);
        if (CollectionUtils.isNotEmpty(userPermissionEntities)) {
            UserPermissionEntity userPermissionEntity = userPermissionEntities.get(0);
            log.info(
                    "The association between user and connection already exists, userPermissionEntityId={}",
                    userPermissionEntity.getId());
            return;
        }

        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setAction(action);
        permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
        permissionEntity.setResourceIdentifier(resourceIdentifier);
        permissionEntity.setOrganizationId(userEntity.getOrganizationId());
        permissionEntity.setCreatorId(creatorId);
        permissionEntity.setBuiltIn(false);
        permissionEntity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
        permissionRepository.saveAndFlush(permissionEntity);

        UserPermissionEntity userPermission = new UserPermissionEntity();
        userPermission.setUserId(userId);
        userPermission.setPermissionId(permissionEntity.getId());
        userPermission.setCreatorId(creatorId);
        userPermission.setOrganizationId(userEntity.getOrganizationId());
        userPermissionRepository.saveAndFlush(userPermission);
    }

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public void bindUserAndDataSourcePermission(@NonNull Long userId, @NonNull Long organizationId,
            @NonNull Long dataSourceId, @NotEmpty List<String> actions) {
        actions.stream().filter(action -> StringUtils.notEquals(action, "create")).distinct().forEach(action -> {
            PermissionEntity permission = new PermissionEntity();
            permission.setAction(action);
            permission.setType(PermissionType.PUBLIC_RESOURCE);
            permission.setResourceIdentifier("ODC_CONNECTION:" + dataSourceId);
            permission.setOrganizationId(organizationId);
            permission.setCreatorId(userId);
            permission.setBuiltIn(false);
            permission.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
            PermissionEntity saved = permissionRepository.saveAndFlush(permission);

            UserPermissionEntity userPermission = new UserPermissionEntity();
            userPermission.setUserId(userId);
            userPermission.setPermissionId(saved.getId());
            userPermission.setCreatorId(userId);
            userPermission.setOrganizationId(organizationId);
            userPermissionRepository.saveAndFlush(userPermission);
        });
    }


}
