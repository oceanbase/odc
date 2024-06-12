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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.CreateRoleReq;
import com.oceanbase.odc.service.iam.model.PermissionConfig;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.UpdateRoleReq;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.PermissionUtil;
import com.oceanbase.odc.service.iam.util.RoleMapper;
import com.oceanbase.odc.service.resourcegroup.ResourceGroupService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/6/25
 */

@Slf4j
@Service
@Validated
@Authenticated
public class RoleService {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ResourceGroupService resourceGroupService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;

    @Autowired
    private ResourcePermissionAccessor permissionAccessor;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private VerticalPermissionValidator verticalPermissionValidator;

    @Autowired
    private UserOrganizationService userOrganizationService;

    private RoleMapper roleMapper = RoleMapper.INSTANCE;
    private final List<Consumer<RoleDeleteEvent>> preRoleDeleteHooks = new ArrayList<>();
    private final List<Consumer<RoleEnableEvent>> preRoleDisableHooks = new ArrayList<>();

    @PreAuthenticate(actions = "create", resourceType = "ODC_ROLE", isForAll = true)
    public Boolean exists(String roleName) {
        PreConditions.notBlank(roleName, "role.name");
        Long organizationId = authenticationFacade.currentOrganizationId();
        return roleRepository.findByNameAndOrganizationId(roleName, organizationId).isPresent();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_ROLE", isForAll = true)
    public Role create(@NotNull @Valid CreateRoleReq createRoleReq) {
        CreateRoleReq.requestParamFilter(createRoleReq);
        PreConditions.notNull(createRoleReq, "createRoleRequest");
        PreConditions.notBlank(createRoleReq.getName(), "Role.name");
        Long organizationId = authenticationFacade.currentOrganizationId();
        Optional<RoleEntity> sameNameRole =
                roleRepository.findByNameAndOrganizationId(createRoleReq.getName(), organizationId);
        PreConditions.validNoDuplicated(ResourceType.ODC_ROLE, "name", createRoleReq.getName(),
                sameNameRole::isPresent);
        inspectHorizontalUnauthorized(createRoleReq);
        inspectVerticalUnauthorized(createRoleReq);
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(createRoleReq.getName());
        roleEntity.setEnabled(createRoleReq.isEnabled());
        roleEntity.setType(RoleType.CUSTOM);
        roleEntity.setOrganizationId(organizationId);
        Long creatorId = authenticationFacade.currentUserId();
        roleEntity.setCreatorId(creatorId);
        roleEntity.setDescription(createRoleReq.getDescription());
        roleEntity.setBuiltIn(false);
        roleEntity.setUserCreateTime(new Timestamp(System.currentTimeMillis()));
        roleEntity.setUserUpdateTime(new Timestamp(System.currentTimeMillis()));
        roleRepository.saveAndFlush(roleEntity);
        log.debug("New role has been inserted, role: {}", roleEntity);
        if (CollectionUtils.isNotEmpty(createRoleReq.getResourceManagementPermissions())) {
            insertManagementPermission(createRoleReq.getResourceManagementPermissions(), roleEntity);
        }
        if (CollectionUtils.isNotEmpty(createRoleReq.getSystemOperationPermissions())) {
            insertManagementPermission(createRoleReq.getSystemOperationPermissions(), roleEntity);
        }
        Role role = new Role(roleEntity);
        role.setResourceManagementPermissions(createRoleReq.getResourceManagementPermissions());
        role.setSystemOperationPermissions(createRoleReq.getSystemOperationPermissions());
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_ROLE", indexOfIdParam = 0)
    public Role delete(long id) {
        RoleEntity roleEntity = nullSafeGet(id);
        if (isBuiltin(roleEntity)) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"admin role"},
                    "Operation on admin role is not allowed");
        }
        for (Consumer<RoleDeleteEvent> hook : preRoleDeleteHooks) {
            hook.accept(new RoleDeleteEvent(id));
        }

        horizontalDataPermissionValidator.checkCurrentOrganization(new Role(roleEntity));
        roleRepository.deleteById(id);
        log.info("Role deleted, id={}", id);
        int userRoleEffectRows = userRoleRepository.deleteByRoleId(id);
        log.info("User role relations deleted, affectRows={}", userRoleEffectRows);
        deleteRelatedPermission(id);
        permissionService.deleteResourceRelatedPermissions(id, ResourceType.ODC_ROLE, PermissionType.SYSTEM);
        return new Role(roleEntity);
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_ROLE", indexOfIdParam = 0)
    public Role detail(long id) {
        RoleEntity roleEntity = nullSafeGet(id);
        Role role = new Role(roleEntity);
        horizontalDataPermissionValidator.checkCurrentOrganization(role);
        acquirePermissions(Collections.singleton(role));
        if (role.getCreatorId() != null) {
            Optional<UserEntity> userEntityOptional = userRepository.findById(role.getCreatorId());
            role.setCreatorName(userEntityOptional.isPresent() ? userEntityOptional.get().getAccountName() : "N/A");
        }
        return role;
    }

    @SkipAuthorize("permission check inside")
    public Page<Role> list(Pageable pageable) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        Map<String, Set<String>> permittedRole2Actions = permissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_ROLE, permission -> {
                    ResourcePermission minPermission =
                            new ResourcePermission(permission.getResourceId(), ResourceType.ODC_ROLE.name(), "read");
                    return permission.implies(minPermission);
                });
        if (permittedRole2Actions.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Role> roles;
        if (permittedRole2Actions.containsKey("*")) {
            roles = roleRepository.findByOrganizationId(organizationId, pageable)
                    .map(entity -> roleMapper.entityToModel(entity));
        } else {
            roles = roleRepository.findByOrganizationIdAndRoleIds(organizationId,
                    permittedRole2Actions.keySet().stream().map(Long::parseLong).collect(Collectors.toList()),
                    pageable).map(entity -> roleMapper.entityToModel(entity));
        }
        acquirePermissions(roles.getContent());
        return roles;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_ROLE", indexOfIdParam = 0)
    public Role update(long id, @NotNull @Valid UpdateRoleReq updateRoleRequest) {
        RoleEntity roleEntity = nullSafeGet(id);
        if (isBuiltin(roleEntity)) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"admin role"},
                    "Operation on admin role is not allowed");
        }
        horizontalDataPermissionValidator.checkCurrentOrganization(new Role(roleEntity));
        if (!StringUtils.isEmpty(updateRoleRequest.getName())) {
            roleEntity.setName(updateRoleRequest.getName());
        }
        roleEntity.setEnabled(updateRoleRequest.isEnabled());
        if (!StringUtils.isEmpty(updateRoleRequest.getDescription())) {
            roleEntity.setDescription(updateRoleRequest.getDescription());
        }
        roleRepository.update(roleEntity);
        log.info("Role has been updated: {}", roleEntity);
        CreateRoleReq filteredReq = filteringExistsPermissions(id, updateRoleRequest);
        inspectHorizontalUnauthorized(filteredReq);
        inspectVerticalUnauthorized(filteredReq);
        deleteRelatedPermission(id);
        if (CollectionUtils.isNotEmpty(updateRoleRequest.getResourceManagementPermissions())) {
            insertManagementPermission(updateRoleRequest.getResourceManagementPermissions(), roleEntity);
        }
        if (CollectionUtils.isNotEmpty(updateRoleRequest.getSystemOperationPermissions())) {
            insertManagementPermission(updateRoleRequest.getSystemOperationPermissions(), roleEntity);
        }

        // delete related user
        if (Objects.nonNull(updateRoleRequest.getUnbindUserIds())) {
            PreConditions.validArgumentState(roleEntity.getType() != RoleType.INTERNAL, ErrorCodes.BadArgument,
                    new Object[] {"Internal role does not allow operation"}, "Internal role does not allow operation");
            inspectResourceHorizontalUnauthorized(new HashSet<>(updateRoleRequest.getUnbindUserIds()),
                    ResourceType.ODC_USER);
            for (Long userId : updateRoleRequest.getUnbindUserIds()) {
                userRoleRepository.deleteByUserIdAndRoleId(userId, id);
            }
            log.info("Users have been unbinded from role id={}, affected num={}", id,
                    updateRoleRequest.getUnbindUserIds().size());
        }

        // add related user
        if (Objects.nonNull(updateRoleRequest.getBindUserIds())) {
            PreConditions.validArgumentState(roleEntity.getType() != RoleType.INTERNAL, ErrorCodes.BadArgument,
                    new Object[] {"Internal role does not allow operation"}, "Internal role does not allow operation");
            inspectResourceHorizontalUnauthorized(new HashSet<>(updateRoleRequest.getBindUserIds()),
                    ResourceType.ODC_USER);
            for (Long userId : updateRoleRequest.getBindUserIds()) {
                UserRoleEntity userRoleEntity = new UserRoleEntity();
                userRoleEntity.setUserId(userId);
                userRoleEntity.setCreatorId(authenticationFacade.currentUserId());
                userRoleEntity.setRoleId(id);
                userRoleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
                userRoleRepository.saveAndFlush(userRoleEntity);
            }
            log.info("Users have been bound to role id={}, affected num={}", id,
                    updateRoleRequest.getBindUserIds().size());
        }
        // get updated info
        roleEntity = nullSafeGet(id);
        Role role = new Role(roleEntity);
        acquirePermissions(Collections.singleton(role));
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_ROLE", indexOfIdParam = 0)
    public Role setEnabled(long id, Boolean enabled) {
        RoleEntity roleEntity = nullSafeGet(id);
        if (isBuiltin(roleEntity)) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"admin role"},
                    "Operation on admin role is not allowed");
        }
        for (Consumer<RoleEnableEvent> hook : preRoleDisableHooks) {
            hook.accept(new RoleEnableEvent(id, enabled));
        }
        horizontalDataPermissionValidator.checkCurrentOrganization(new Role(roleEntity));
        roleEntity.setEnabled(enabled);
        roleRepository.update(roleEntity);
        return new Role(roleEntity);
    }

    @SkipAuthorize("odc internal usage")
    public RoleEntity nullSafeGet(@NonNull Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new NotFoundException(ResourceType.ODC_ROLE, "id", id));
    }

    @SkipAuthorize("odc internal usage")
    public List<RoleEntity> batchNullSafeGet(@NonNull Collection<Long> ids) {
        List<RoleEntity> entities = roleRepository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(RoleEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_ROLE, "id", absentIds);
        }
        return entities;
    }

    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    public UserRoleEntity bindUserRole(@NonNull Long userId, @NonNull Long roleId, @NonNull Long creatorId,
            Long organizationId) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);
        PreConditions.validExists(ResourceType.ODC_USER, "id", userId, userEntityOptional::isPresent);
        UserEntity userEntity = userEntityOptional.get();
        Optional<RoleEntity> roleEntityOptional = roleRepository.findById(roleId);
        PreConditions.validExists(ResourceType.ODC_ROLE, "id", roleId, roleEntityOptional::isPresent);
        if (!userOrganizationService.userBelongsToOrganization(userId, roleEntityOptional.get().getOrganizationId())) {
            throw new UnsupportedOperationException(String.format(
                    "Can not bind user and role from different organization, userId=%s, roleId=%s", userId, roleId));
        }
        if (Objects.isNull(organizationId)) {
            organizationId = userEntity.getOrganizationId();
        }
        List<UserRoleEntity> userRoleEntities = userRoleRepository.findByUserIdAndRoleIdAndOrganizationId(userId,
                roleId, organizationId);
        if (CollectionUtils.isNotEmpty(userRoleEntities)) {
            log.warn("The association between user and role already exists, userId={}, roleId={}, userRoleEntity={}",
                    userId, roleId, userRoleEntities.get(0));
            return userRoleEntities.get(0);
        }
        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setUserId(userId);
        userRoleEntity.setRoleId(roleId);
        userRoleEntity.setCreatorId(creatorId);
        userRoleEntity.setOrganizationId(organizationId);
        return userRoleRepository.save(userRoleEntity);
    }

    private void acquirePermissions(@NotEmpty Collection<Role> roles) {
        List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
        List<RolePermissionEntity> relations = rolePermissionRepository.findByRoleIds(roleIds);
        if (relations.isEmpty()) {
            return;
        }
        Map<Long, List<RolePermissionEntity>> roleId2Relation = relations.stream().collect(
                Collectors.groupingBy(RolePermissionEntity::getRoleId));

        Map<Long, PermissionEntity> permissionId2Permission = permissionRepository.findByIdIn(
                relations.stream().map(RolePermissionEntity::getPermissionId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(PermissionEntity::getId, entity -> entity));

        for (Role role : roles) {
            List<PermissionEntity> managementPermissions = new ArrayList<>();
            List<PermissionEntity> operationPermissions = new ArrayList<>();
            List<RolePermissionEntity> rolePermissionEntities =
                    roleId2Relation.getOrDefault(role.getId(), new LinkedList<>());
            for (RolePermissionEntity rolePermissionEntity : rolePermissionEntities) {
                PermissionEntity permission = permissionId2Permission.get(rolePermissionEntity.getPermissionId());
                if (Objects.nonNull(permission)) {
                    if (PermissionUtil.isResourceManagementPermission(permission)) {
                        managementPermissions.add(permission);
                    } else if (PermissionUtil.isSystemOperationPermission(permission)) {
                        operationPermissions.add(permission);
                    } else {
                        log.info("Permission classification filed, permissionEntity={}", permission);
                    }
                }
            }
            role.setResourceManagementPermissions(
                    PermissionUtil.aggregateResourceManagementPermissions(managementPermissions));
            role.setSystemOperationPermissions(PermissionUtil.aggregatePermissions(operationPermissions));
        }
    }

    private void deleteRelatedPermission(@NonNull Long roleId) {
        List<RolePermissionEntity> rolePermissionEntities = rolePermissionRepository.findByRoleId(roleId);
        for (RolePermissionEntity relation : rolePermissionEntities) {
            rolePermissionRepository.deleteById(relation.getId());
            permissionRepository.deleteByIds(Collections.singleton(relation.getId()));
        }
        log.info("Role permission relations have been deleted, affectNum={}, roleId={}", rolePermissionEntities.size(),
                roleId);
        log.info("Permissions associated with role have been deleted, affectNum={}, roleId={}",
                rolePermissionEntities.size(), roleId);
    }

    private void insertManagementPermission(List<PermissionConfig> permissionConfigs, RoleEntity role) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        for (PermissionConfig config : permissionConfigs) {
            ResourceType resourceType = config.getResourceType();
            String resourceId = Objects.nonNull(config.getResourceId()) ? config.getResourceId().toString() : "*";
            for (String action : config.getActions()) {
                PermissionEntity permission = new PermissionEntity();
                permission.setAction(action);
                permission.setResourceIdentifier(resourceType.name() + ":" + resourceId);
                if (resourceType == ResourceType.ODC_CONNECTION) {
                    permission.setType(PermissionType.PUBLIC_RESOURCE);
                } else {
                    permission.setType(PermissionType.SYSTEM);
                }
                permission.setCreatorId(authenticationFacade.currentUserId());
                permission.setOrganizationId(organizationId);
                permission.setBuiltIn(false);
                permission.setExpireTime(TimeUtils.getMySQLMaxDatetime());
                permission.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
                permissionRepository.saveAndFlush(permission);

                RolePermissionEntity rolePermissionEntity = new RolePermissionEntity();
                rolePermissionEntity.setRoleId(role.getId());
                rolePermissionEntity.setOrganizationId(organizationId);
                rolePermissionEntity.setPermissionId(permission.getId());
                rolePermissionEntity.setCreatorId(role.getCreatorId());
                rolePermissionRepository.saveAndFlush(rolePermissionEntity);
            }
        }
    }

    private boolean isBuiltin(RoleEntity roleEntity) {
        return roleEntity.getBuiltIn();
    }

    /**
     * Only when the operator has the "update" permission of the resource, so that he or she can grant
     * permissions of the resource to the role. As for system operation permission, just grant what the
     * current user have.
     */
    private <T extends CreateRoleReq> void inspectVerticalUnauthorized(T createRoleReq) {
        List<SecurityResource> resources = new ArrayList<>();
        List<PermissionConfig> permissionConfigs = Stream.of(
                createRoleReq.nullSafeGetResourceManagementPermissions()).flatMap(Collection::stream)
                .collect(Collectors.toList());
        permissionConfigs.forEach(config -> {
            String resourceId = Objects.nonNull(config.getResourceId()) ? config.getResourceId().toString() : "*";
            resources.add(new DefaultSecurityResource(resourceId, config.getResourceType().name()));
        });
        verticalPermissionValidator.checkResourcePermissions(
                resources.stream().filter(resource -> StringUtils.equals(resource.resourceType(), "ODC_PROJECT"))
                        .collect(Collectors.toList()),
                Collections.singletonList("create"));
        verticalPermissionValidator.checkResourcePermissions(
                resources.stream().filter(resource -> !StringUtils.equals(resource.resourceType(), "ODC_PROJECT"))
                        .collect(Collectors.toList()),
                Collections.singletonList("update"));
        createRoleReq.nullSafeGetSystemOperationPermissions().forEach(config -> {
            String resourceId = Objects.nonNull(config.getResourceId()) ? config.getResourceId().toString() : "*";
            verticalPermissionValidator.checkResourcePermissions(
                    new DefaultSecurityResource(resourceId, config.getResourceType().name()), config.getActions());
        });
    }

    private <T extends CreateRoleReq> void inspectHorizontalUnauthorized(T createRoleReq) {
        List<PermissionConfig> permissionConfigs = Stream.of(
                createRoleReq.nullSafeGetConnectionAccessPermissions(),
                createRoleReq.nullSafeGetResourceManagementPermissions()).flatMap(Collection::stream)
                .filter(config -> Objects.nonNull(config.getResourceId())).collect(Collectors.toList());
        Set<Long> userIds = new HashSet<>();
        Set<Long> roleIds = new HashSet<>();
        Set<Long> resourceGroupIds = new HashSet<>();
        Set<Long> connectionIds = new HashSet<>();
        permissionConfigs.forEach(config -> {
            switch (config.getResourceType()) {
                case ODC_USER:
                    userIds.add(config.getResourceId());
                    break;
                case ODC_ROLE:
                    roleIds.add(config.getResourceId());
                    break;
                case ODC_CONNECTION:
                    connectionIds.add(config.getResourceId());
                    break;
                case ODC_RESOURCE_GROUP:
                    resourceGroupIds.add(config.getResourceId());
                    break;
                default:
                    break;
            }
        });
        inspectResourceHorizontalUnauthorized(userIds, ResourceType.ODC_USER);
        inspectResourceHorizontalUnauthorized(roleIds, ResourceType.ODC_ROLE);
        inspectResourceHorizontalUnauthorized(connectionIds, ResourceType.ODC_CONNECTION);
        inspectResourceHorizontalUnauthorized(resourceGroupIds, ResourceType.ODC_RESOURCE_GROUP);
    }

    private void inspectResourceHorizontalUnauthorized(Set<Long> ids, ResourceType resourceType) {
        switch (resourceType) {
            case ODC_USER:
                List<User> users = userService.batchNullSafeGet(ids);
                horizontalDataPermissionValidator.checkCurrentOrganization(users);
                break;
            case ODC_ROLE:
                List<RoleEntity> roleEntities = batchNullSafeGet(ids);
                List<Role> roles = roleEntities.stream().map(Role::new).collect(Collectors.toList());
                horizontalDataPermissionValidator.checkCurrentOrganization(roles);
                break;
            case ODC_CONNECTION:
                List<ConnectionConfig> connectionConfigs = connectionService.batchNullSafeGet(ids);
                horizontalDataPermissionValidator.checkCurrentOrganization(connectionConfigs);
                break;
            default:
                break;
        }
    }

    CreateRoleReq filteringExistsPermissions(Long roleId, UpdateRoleReq updateRoleRequest) {
        Role role = detail(roleId);
        CreateRoleReq filtered = new CreateRoleReq();
        filtered.setResourceManagementPermissions(
                filteringByResourcePermission(updateRoleRequest.nullSafeGetResourceManagementPermissions(),
                        CreateRoleReq.nullSafeGet(role.getResourceManagementPermissions())));
        filtered.setSystemOperationPermissions(
                filteringByResourcePermission(updateRoleRequest.nullSafeGetSystemOperationPermissions(),
                        CreateRoleReq.nullSafeGet(role.getSystemOperationPermissions())));
        return filtered;
    }

    private List<PermissionConfig> filteringByResourcePermission(List<PermissionConfig> newConfig,
            List<PermissionConfig> existsConfig) {
        List<ResourcePermission> existsResourcePermissions = existsConfig.stream().map(config -> {
            String resourceId = Objects.nonNull(config.getResourceId()) ? config.getResourceId().toString() : "*";
            return new ResourcePermission(resourceId, config.getResourceType().name(),
                    StringUtils.join(config.getActions(), ","));
        }).collect(Collectors.toList());
        return newConfig.stream().filter(config -> {
            String resourceId = Objects.nonNull(config.getResourceId()) ? config.getResourceId().toString() : "*";
            ResourcePermission newPermission = new ResourcePermission(resourceId, config.getResourceType().name(),
                    StringUtils.join(config.getActions(), ","));
            for (ResourcePermission existPermission : existsResourcePermissions) {
                if (existPermission.implies(newPermission)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public void addPreRoleDeleteHook(Consumer<RoleDeleteEvent> hook) {
        preRoleDeleteHooks.add(hook);
    }

    @SkipAuthorize("odc internal usage")
    public void addPreRoleDisableHook(Consumer<RoleEnableEvent> hook) {
        preRoleDisableHooks.add(hook);
    }

    @Data
    @AllArgsConstructor
    public static class RoleDeleteEvent {
        private Long roleId;
    }

    @Data
    @AllArgsConstructor
    public static class RoleEnableEvent {
        private Long roleId;
        private Boolean enabled;
    }

}
