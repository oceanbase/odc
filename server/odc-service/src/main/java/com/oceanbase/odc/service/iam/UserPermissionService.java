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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.BatchUpdateUserPermissionsReq;
import com.oceanbase.odc.service.iam.model.BatchUpdateUserPermissionsReq.UserAction;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.model.UserPermissionResp;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;
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
    private SecurityManager securityManager;

    @Autowired
    private AuthorizationFacade authorizationFacade;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @SkipAuthorize("inside method permission check")
    public List<UserPermissionResp> list(@NonNull String resourceIdentifier) {
        List<UserPermissionResp> returnValue = new ArrayList<>();
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(resourceIdentifier);
        if (Objects.isNull(resourceContext.getId())
                || !Objects.equals(resourceContext.getField(), ResourceType.ODC_CONNECTION.name())) {
            log.warn("Failed to query user permissions, resourceIdentifier={}", resourceIdentifier);
            return returnValue;
        }
        inspectConnectionPermissions(resourceContext.getId(), "read");
        List<UserPermissionEntity> userPermissionEntities =
                userPermissionRepository.findByOrganizationIdAndResourceIdentifier(
                        authenticationFacade.currentOrganizationId(), resourceIdentifier);
        if (CollectionUtils.isEmpty(userPermissionEntities)) {
            return returnValue;
        }
        List<Long> permissionIds = userPermissionEntities.stream().map(UserPermissionEntity::getPermissionId).distinct()
                .collect(Collectors.toList());
        List<Long> userIds = userPermissionEntities.stream().map(UserPermissionEntity::getUserId).distinct()
                .collect(Collectors.toList());
        Map<Long, PermissionEntity> permissionId2Entity = permissionRepository.findByIdIn(permissionIds).stream()
                .collect(Collectors.toMap(PermissionEntity::getId, entity -> entity));
        Map<Long, UserEntity> userId2Entity = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, entity -> entity));

        // delete duplicated userPermissionEntity
        Set<Long> userPermissionIds = new HashSet<>();
        Map<Long, List<UserPermissionEntity>> userId2UserPermissions =
                userPermissionEntities.stream().collect(Collectors.groupingBy(UserPermissionEntity::getUserId));
        for (Long userId : userId2UserPermissions.keySet()) {
            List<UserPermissionEntity> userPermissions = userId2UserPermissions.get(userId);
            UserPermissionEntity entity = userPermissions.get(0);
            for (int i = 1; i < userPermissions.size(); i++) {
                ConnectionPermission existsPrivilege = new ConnectionPermission(resourceContext.getId().toString(),
                        permissionId2Entity.get(entity.getPermissionId()).getAction());
                ConnectionPermission currentPrivilege = new ConnectionPermission(resourceContext.getId().toString(),
                        permissionId2Entity.get(userPermissions.get(i).getPermissionId()).getAction());
                if (currentPrivilege.implies(existsPrivilege)) {
                    entity = userPermissions.get(i);
                }
            }
            userPermissionIds.add(entity.getId());
        }
        userPermissionEntities = userPermissionEntities.stream()
                .filter(entity -> userPermissionIds.contains(entity.getId())).collect(Collectors.toList());

        return userPermissionEntities.stream()
                .map(entity -> new UserPermissionResp(entity,
                        userId2Entity.getOrDefault(entity.getUserId(), new UserEntity()),
                        permissionId2Entity.getOrDefault(entity.getPermissionId(), new PermissionEntity())))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("inside method permission check")
    public List<UserPermissionResp> batchUpdateForConnection(@NonNull BatchUpdateUserPermissionsReq req) {
        List<UserPermissionResp> userPermissionRespList = new ArrayList<>();
        if (Objects.isNull(req.getResourceIdentifier())) {
            return userPermissionRespList;
        }
        ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(req.getResourceIdentifier());
        if (Objects.isNull(resourceContext.getId())
                || !Objects.equals(resourceContext.getField(), ResourceType.ODC_CONNECTION.name())) {
            return userPermissionRespList;
        }
        inspectConnectionPermissions(resourceContext.getId(), "update");

        // intercept horizontal authorizing
        List<Long> userIds = req.getUserActions().stream().map(UserAction::getUserId).collect(Collectors.toList());
        List<User> users = userRepository.findByIdIn(userIds).stream().map(User::new).collect(Collectors.toList());
        permissionValidator.checkCurrentOrganization(users);
        ConnectionConfig connection = connectionService.getWithoutPermissionCheck(resourceContext.getId());
        permissionValidator.checkCurrentOrganization(connection);

        // delete exists user permission relations and permissions records
        List<UserPermissionEntity> userPermissionEntities =
                userPermissionRepository.findByOrganizationIdAndResourceIdentifier(
                        authenticationFacade.currentOrganizationId(), req.getResourceIdentifier());
        List<Long> userPermissionIds =
                userPermissionEntities.stream().map(UserPermissionEntity::getId).collect(Collectors.toList());
        List<Long> permissionIds =
                userPermissionEntities.stream().map(UserPermissionEntity::getPermissionId).collect(Collectors.toList());
        userPermissionRepository.deleteByIds(userPermissionIds);
        permissionRepository.deleteByIds(permissionIds);

        Map<Long, UserEntity> userId2Entity = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, entity -> entity));

        // insert new user permissions and permissions records
        for (UserAction userAction : req.getUserActions()) {
            PreConditions.notNull(userAction.getUserId(), "userActions.userId");
            PreConditions.notEmpty(userAction.getAction(), "userActions.action");
            PermissionEntity permissionEntity = new PermissionEntity();
            permissionEntity.setAction(userAction.getAction());
            permissionEntity.setResourceIdentifier(req.getResourceIdentifier());
            permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
            permissionEntity.setCreatorId(authenticationFacade.currentUserId());
            permissionEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
            permissionEntity.setBuiltIn(false);
            permissionRepository.save(permissionEntity);

            UserPermissionEntity userPermissionEntity = new UserPermissionEntity();
            userPermissionEntity.setUserId(userAction.getUserId());
            userPermissionEntity.setPermissionId(permissionEntity.getId());
            userPermissionEntity.setCreatorId(authenticationFacade.currentUserId());
            userPermissionEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
            userPermissionRepository.save(userPermissionEntity);

            userPermissionRespList.add(new UserPermissionResp(userPermissionEntity,
                    userId2Entity.getOrDefault(userAction.getUserId(), new UserEntity()), permissionEntity));
        }
        return userPermissionRespList;
    }

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
        permissionRepository.saveAndFlush(permissionEntity);

        UserPermissionEntity userPermission = new UserPermissionEntity();
        userPermission.setUserId(userId);
        userPermission.setPermissionId(permissionEntity.getId());
        userPermission.setCreatorId(creatorId);
        userPermission.setOrganizationId(userEntity.getOrganizationId());
        userPermissionRepository.saveAndFlush(userPermission);
    }

    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    public void deleteExistsUserPermissions(User user, List<PermissionEntity> permissionEntities) {
        List<String> resourceIdentifiers =
                permissionEntities.stream().map(PermissionEntity::resourceIdentifier).collect(Collectors.toList());
        List<UserPermissionEntity> existsUserPermissions =
                userPermissionRepository.findByUserIdAndResourceIdentifiers(user.getId(), resourceIdentifiers);
        if (CollectionUtils.isEmpty(existsUserPermissions)) {
            return;
        }
        int affectNum1 = permissionRepository.deleteByIds(
                existsUserPermissions.stream().map(UserPermissionEntity::getPermissionId).collect(Collectors.toList()));
        log.info("Delete exists permissionEntities, affectNum={}", affectNum1);
        int affectNum2 = userPermissionRepository.deleteByIds(
                existsUserPermissions.stream().map(UserPermissionEntity::getId).collect(Collectors.toList()));
        log.info("Delete exists userPermissionEntities, user={}, affectNum={}", user, affectNum2);
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
            PermissionEntity saved = permissionRepository.saveAndFlush(permission);

            UserPermissionEntity userPermission = new UserPermissionEntity();
            userPermission.setUserId(userId);
            userPermission.setPermissionId(saved.getId());
            userPermission.setCreatorId(userId);
            userPermission.setOrganizationId(organizationId);
            userPermissionRepository.saveAndFlush(userPermission);
        });
    }

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public void bindUserAndCreateDataSourcePermission(@NonNull Long userId, @NonNull Long organizationId) {
        PermissionEntity permission = new PermissionEntity();
        permission.setAction("create");
        permission.setType(PermissionType.PUBLIC_RESOURCE);
        permission.setResourceIdentifier("ODC_CONNECTION:*");
        permission.setOrganizationId(organizationId);
        permission.setCreatorId(userId);
        permission.setBuiltIn(false);
        PermissionEntity saved = permissionRepository.saveAndFlush(permission);

        UserPermissionEntity userPermission = new UserPermissionEntity();
        userPermission.setUserId(userId);
        userPermission.setPermissionId(saved.getId());
        userPermission.setCreatorId(userId);
        userPermission.setOrganizationId(organizationId);
        userPermissionRepository.saveAndFlush(userPermission);
    }


    private void inspectConnectionPermissions(@NonNull Long connectionId, @NonNull String action) {
        List<Permission> permissions = new ArrayList<>();
        permissions.add(securityManager.getPermissionByActions(
                new DefaultSecurityResource(connectionId.toString(), ResourceType.ODC_CONNECTION.name()),
                Collections.singleton(action)));
        boolean checkResult = authorizationFacade.isImpliesPermissions(authenticationFacade.currentUser(), permissions);
        if (!checkResult) {
            throw new AccessDeniedException(
                    "The current user dose not have " + action + " permission for ODC_CONNECTION:" + connectionId);
        }
    }

}
