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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.LastSuccessLoginHistory;
import com.oceanbase.odc.metadb.iam.LoginHistoryRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.metadb.iam.UserSpecs;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.common.response.CustomPage;
import com.oceanbase.odc.service.common.response.PaginatedData;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.ChangePasswordReq;
import com.oceanbase.odc.service.iam.model.CreateUserReq;
import com.oceanbase.odc.service.iam.model.QueryUserParams;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.UpdateUserReq;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;
import com.oceanbase.odc.service.iam.util.PermissionUtil;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;

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
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private AuthorizationFacade authorizationFacade;

    @Autowired
    private ResourcePermissionExtractor permissionMapper;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @Autowired
    private ResourcePermissionAccessor permissionAccessor;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserHandler userHandler;

    @Autowired
    private ResourceRoleService resourceRoleService;

    @Autowired
    private UserOrganizationService userOrganizationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final List<Consumer<PasswordChangeEvent>> postPasswordChangeHooks = new ArrayList<>();
    private final List<Consumer<UserDeleteEvent>> postUserDeleteHooks = new ArrayList<>();
    private final List<Consumer<UserDeleteEvent>> preUserDeleteHooks = new ArrayList<>();
    private static final int FAILED_LOGIN_ATTEMPT_TIMES = 5;
    private static final long WITHOUT_ROLE_ID = 0L;
    /**
     * 10 minutes lock if failed login attempt FAILED_LOGIN_ATTEMPT_TIMES times <br>
     * 10 * 60 * 1000L
     */
    private static final long FAILED_LOGIN_ATTEMPT_LOCK_TIMEOUT = 10 * 60 * 1000L;
    private static final LoadingCache<Long, FailedLoginAttemptLimiter> userIdChangePasswordAttamptCache =
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofHours(1L))
                    .build(key -> new FailedLoginAttemptLimiter(FAILED_LOGIN_ATTEMPT_TIMES,
                            FAILED_LOGIN_ATTEMPT_LOCK_TIMEOUT));

    /**
     * Create user if not exists, or update user if extraPropertiesJson is different
     * 
     * @param userEntity user entity, must not be null
     * @param roleNames role names, only for create new user
     * @return created or updated user
     */
    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    public User upsert(UserEntity userEntity, List<String> roleNames) {
        User user;
        Optional<UserEntity> optionalUser = userRepository.findByAccountName(userEntity.getAccountName());
        if (optionalUser.isPresent()) {
            UserEntity existed = optionalUser.get();
            if (!Objects.equals(existed.getExtraPropertiesJson(), userEntity.getExtraPropertiesJson())) {
                existed.setExtraPropertiesJson(userEntity.getExtraPropertiesJson());
                user = save(existed);
                log.info("Update user extra properties successfully, accountName={}, new properties={}",
                        user.getAccountName(), user.getExtraProperties());
            } else {
                user = new User(existed);
            }
        } else {
            user = save(userEntity);
            if (CollectionUtils.isNotEmpty(roleNames)) {
                List<Long> roleIds = new ArrayList<>();
                List<Role> roles = new ArrayList<>();
                for (String roleName : roleNames) {
                    RoleEntity roleEntity =
                            roleRepository.findByNameAndOrganizationId(roleName, user.getOrganizationId())
                                    .orElseThrow(() -> new NotFoundException(ResourceType.ODC_ROLE, "name", roleName));
                    Verify.verify(roleEntity.getType() != RoleType.INTERNAL, "Internal role does not allow operation");
                    Long roleId = roleEntity.getId();
                    UserRoleEntity userRoleEntity = new UserRoleEntity();
                    userRoleEntity.setUserId(user.getId());
                    userRoleEntity.setRoleId(roleId);
                    userRoleEntity.setOrganizationId(user.getOrganizationId());
                    userRoleEntity.setCreatorId(user.getId());
                    userRoleRepository.saveAndFlush(userRoleEntity);
                    roleIds.add(roleId);
                    roles.add(new Role(roleEntity));
                }
                user.setRoleIds(roleIds);
                user.setRoles(roles);
            }
            log.info("Create user successfully, accountName={}", user.getAccountName());
        }
        return user;
    }

    @Transactional
    @SkipAuthorize
    public User save(UserEntity userEntity) {
        String eventName = userEntity.getId() == null ? TriggerEvent.USER_CREATED : TriggerEvent.USER_UPDATED;
        UserEntity save = userRepository.save(userEntity);
        User user = new User(save);
        publishTriggerEventAfterTx(user, eventName);
        return user;
    }

    /**
     * have to in a transaction
     */
    private void publishTriggerEventAfterTx(User user, String eventName) {
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    try {
                        SpringContextUtil.publishEvent(new TriggerEvent(eventName, user));
                    } catch (Exception e) {
                        log.error("can not publish user created event,userId=" + user.getId(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("registerSynchronization failed", e);
        }
    }

    @SkipAuthorize("for odc internal usage")
    public boolean exists(Long organizationId, String accountName) {
        return userRepository.findByOrganizationIdAndAccountName(organizationId, accountName).isPresent();
    }

    /**
     * 检查账号是否存在，适用于 Cloud 环境，多组织但是 accountName 全局唯一
     */
    @SkipAuthorize("for exists name judge while create user")
    public boolean exists(String accountName) {
        return userRepository.findByAccountName(accountName).isPresent();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_USER", isForAll = true)
    public User create(@NotNull @Valid CreateUserReq createUserReq) {
        PreConditions.notNull(createUserReq, "createUserRequest");
        PreConditions.notBlank(createUserReq.getName(), "user.name");
        PreConditions.notBlank(createUserReq.getAccountName(), "user.accountName");
        PreConditions.notBlank(createUserReq.getPassword(), "user.password");
        PreConditions.validPassword(createUserReq.getPassword());

        Optional<UserEntity> sameAccountNameUser = userRepository.findByAccountName(createUserReq.getAccountName());
        PreConditions.validNoDuplicated(ResourceType.ODC_USER, "accountName", createUserReq.getAccountName(),
                sameAccountNameUser::isPresent);

        UserEntity userEntity = new UserEntity();
        Long creatorId = authenticationFacade.currentUserId();
        userEntity.setBuiltIn(false);
        userEntity.setType(UserType.USER);
        userEntity.setName(createUserReq.getName());
        userEntity.setAccountName(createUserReq.getAccountName());
        userEntity.setPassword(encodePassword(createUserReq.getPassword()));
        userEntity.setCipher(Cipher.BCRYPT);
        userEntity.setCreatorId(creatorId);
        userEntity.setEnabled(createUserReq.isEnabled());
        userEntity.setDescription(createUserReq.getDescription());
        userEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        userEntity.setUserCreateTime(new Timestamp(System.currentTimeMillis()));
        userEntity.setUserUpdateTime(new Timestamp(System.currentTimeMillis()));
        User user = save(userEntity);

        userOrganizationService.bindUserToOrganization(user.getId(), user.getOrganizationId());

        log.debug("New user has been inserted, user: {}", userEntity);
        if (!Objects.isNull(createUserReq.getRoleIds()) && !createUserReq.getRoleIds().isEmpty()) {
            inspectVerticalUnauthorized(authenticationFacade.currentUser(), createUserReq.getRoleIds());
            for (Long roleId : createUserReq.getRoleIds()) {
                Role role = new Role(roleRepository.findById(roleId)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_ROLE, "id", roleId)));
                PreConditions.validArgumentState(role.getType() != RoleType.INTERNAL, ErrorCodes.BadArgument,
                        new Object[] {"Internal role does not allow operation"},
                        "Internal role does not allow operation");
                permissionValidator.checkCurrentOrganization(role);
                UserRoleEntity userRoleEntity = new UserRoleEntity();
                userRoleEntity.setOrganizationId(role.getOrganizationId());
                userRoleEntity.setUserId(userEntity.getId());
                userRoleEntity.setCreatorId(creatorId);
                userRoleEntity.setRoleId(roleId);
                userRoleRepository.saveAndFlush(userRoleEntity);
                log.debug("New user and role relation has been inserted, userToRole: {}", userRoleEntity);
            }
        }
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_USER", isForAll = true)
    public List<User> batchCreate(@NotNull @Valid List<CreateUserReq> createUserReqs) {
        List<User> userList = new ArrayList<>();
        for (CreateUserReq createUserReq : createUserReqs) {
            User user = create(createUserReq);
            userList.add(user);
        }
        return userList;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_USER", indexOfIdParam = 0)
    public User delete(long id) {
        PreConditions.validArgumentState(id != authenticationFacade.currentUserId(), ErrorCodes.BadRequest,
                new Object[] {"Can not delete yourself"}, "Can not delete yourself");
        UserEntity userEntity = nullSafeGet(id);
        if (isBuiltin(userEntity)) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"admin account"},
                    "Operation on admin account is not allowed");
        }
        permissionValidator.checkCurrentOrganization(new User(userEntity));

        UserDeleteEvent event = new UserDeleteEvent();
        event.setUserId(id);
        event.setAccountName(userEntity.getAccountName());
        event.setOrganizationId(authenticationFacade.currentOrganizationId());
        for (Consumer<UserDeleteEvent> hook : preUserDeleteHooks) {
            hook.accept(event);
        }

        userRepository.deleteById(id);
        userRoleRepository.deleteByUserId(id);
        deleteRelatedPermissions(id);
        permissionService.deleteResourceRelatedPermissions(id, ResourceType.ODC_USER, PermissionType.SYSTEM);

        for (Consumer<UserDeleteEvent> hook : postUserDeleteHooks) {
            hook.accept(event);
        }
        log.info("User deleted, id={}", id);
        return new User(userEntity);
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_USER", indexOfIdParam = 0)
    public User detail(long id) {
        return detailWithoutPermissionCheck(id);
    }

    @SkipAuthorize("odc internal usage")
    public User detailWithoutPermissionCheck(@NonNull Long id) {
        UserEntity userEntity = nullSafeGet(id);
        List<UserRoleEntity> userRoleEntities =
                userRoleRepository.findByUserId(id).stream()
                        .filter(userRoleEntity -> userRoleEntity.getOrganizationId().longValue() == authenticationFacade
                                .currentOrganizationId())
                        .collect(
                                Collectors.toList());
        List<Long> roleIds = new ArrayList<>();
        for (UserRoleEntity userRoleEntity : userRoleEntities) {
            roleIds.add(userRoleEntity.getRoleId());
        }

        User user = new User(userEntity);
        // acquire creator name for display
        if (Objects.nonNull(userEntity.getCreatorId())) {
            Optional<UserEntity> creatorEntityOptional = userRepository.findById(userEntity.getCreatorId());
            if (!creatorEntityOptional.isPresent()) {
                log.warn("Creator id={} for user id ={} is not exist", userEntity.getCreatorId(), userEntity.getId());
            } else {
                user.setCreatorName(creatorEntityOptional.get().getAccountName());
            }
        }
        user.setRoleIds(roleIds);
        return user;
    }

    @SkipAuthorize
    public User detailCurrentUser() {
        User user = authenticationFacade.currentUser();
        acquireRolesAndRoleIds(Collections.singletonList(user));
        acquirePermissions(Collections.singletonList(user));
        return userHandler.handle(user);
    }

    @SkipAuthorize("odc internal usage")
    public Set<Long> getCurrentUserRoleIds() {
        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndOrganizationIdAndEnabled(
                authenticationFacade.currentUserId(), authenticationFacade.currentOrganizationId(), true);
        if (CollectionUtils.isEmpty(roleEntities)) {
            return Collections.emptySet();
        }
        return roleEntities.stream().map(RoleEntity::getId).collect(Collectors.toSet());
    }

    public Set<String> getCurrentUserResourceRoleIdentifiers() {
        long currentUserId = authenticationFacade.currentUserId();
        long currentOrganizationId = authenticationFacade.currentOrganizationId();
        return resourceRoleService.getResourceRoleIdentifiersByUserId(currentOrganizationId, currentUserId);
    }

    private void acquirePermissions(@NonNull Collection<User> users) {
        List<PermissionEntity> managementPermissions = new ArrayList<>();
        List<PermissionEntity> operationPermissions = new ArrayList<>();
        acquireRolePermissions(users, managementPermissions, operationPermissions);
        acquireUserPermissions(users, managementPermissions, operationPermissions);

        for (User user : users) {
            user.setResourceManagementPermissions(
                    PermissionUtil.aggregateResourceManagementPermissions(managementPermissions));
            user.setSystemOperationPermissions(PermissionUtil.aggregatePermissions(operationPermissions));
        }
    }

    private void acquireRolePermissions(Collection<User> users,
            List<PermissionEntity> managementPermissions, List<PermissionEntity> operationPermissions) {
        Set<Long> roleIds = new HashSet<>();
        for (User user : users) {
            if (CollectionUtils.isNotEmpty(user.getRoles())) {
                roleIds.addAll(user.getRoleIds());
            }
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Map<Long, List<RolePermissionEntity>> roleId2RolePermissionEntities =
                rolePermissionRepository.findByRoleIdsAndEnabled(roleIds, true).stream()
                        .collect(Collectors.groupingBy(RolePermissionEntity::getRoleId));
        Map<Long, PermissionEntity> rolePermissionId2Entity = permissionRepository
                .findByIdIn(roleId2RolePermissionEntities.values().stream().flatMap(Collection::stream)
                        .map(RolePermissionEntity::getPermissionId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(PermissionEntity::getId, entity -> entity));
        if (CollectionUtils.isEmpty(roleId2RolePermissionEntities.keySet())
                || CollectionUtils.isEmpty(rolePermissionId2Entity.keySet())) {
            return;
        }
        for (User user : users) {
            if (CollectionUtils.isEmpty(user.getRoles())) {
                continue;
            }
            for (Role role : user.getRoles()) {
                if (!roleId2RolePermissionEntities.containsKey(role.getId())) {
                    continue;
                }
                for (RolePermissionEntity rolePermissionEntity : roleId2RolePermissionEntities.get(role.getId())) {
                    PermissionEntity permission = rolePermissionId2Entity.get(rolePermissionEntity.getPermissionId());
                    if (PermissionUtil.isResourceManagementPermission(permission)) {
                        managementPermissions.add(permission);
                    } else if (PermissionUtil.isSystemOperationPermission(permission)) {
                        operationPermissions.add(permission);
                    }
                }
            }
        }
    }

    private void acquireUserPermissions(@NotEmpty Collection<User> users,
            List<PermissionEntity> managementPermissions, List<PermissionEntity> operationPermissions) {
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<UserPermissionEntity>> userId2UserPermissionEntities = userPermissionRepository
                .findByUserIdIn(userIds).stream().collect(Collectors.groupingBy(UserPermissionEntity::getUserId));
        Map<Long, PermissionEntity> userPermissionId2Entity = permissionRepository
                .findByUserIdsAndOrganizationId(userIds, authenticationFacade.currentOrganizationId()).stream()
                .collect(Collectors.toMap(PermissionEntity::getId, entity -> entity));
        for (User user : users) {
            if (CollectionUtils.isEmpty(userId2UserPermissionEntities.get(user.getId()))) {
                continue;
            }
            for (UserPermissionEntity userPermission : userId2UserPermissionEntities.get(user.getId())) {
                if (Objects.isNull(userPermissionId2Entity.get(userPermission.getPermissionId()))) {
                    continue;
                }
                PermissionEntity permission = userPermissionId2Entity.get(userPermission.getPermissionId());
                if (PermissionUtil.isResourceManagementPermission(permission)) {
                    managementPermissions.add(permission);
                } else if (PermissionUtil.isSystemOperationPermission(permission)) {
                    operationPermissions.add(permission);
                }
            }
        }
    }

    /**
     * roleId is exclusive to authorizedResource & includePermissions
     *
     * @return
     */
    @SkipAuthorize("permission check inside")
    public PaginatedData<User> list(QueryUserParams queryUserParams, Pageable pageable) {
        if (Objects.nonNull(queryUserParams.getBasic()) && queryUserParams.getBasic()) {
            return listUserBasicsWithoutPermissionCheck();
        }
        if (StringUtils.isNotEmpty(queryUserParams.getAuthorizedResource())) {
            // get related users for some specific resource, such as ODC_CONNECTION:10 or ODC_RESOURCE_GROUP:15
            List<User> users = new ArrayList<>();
            CustomPage customPage = CustomPage.empty();
            ResourceContext resourceContext =
                    ResourceContextUtil.parseFromResourceIdentifier(queryUserParams.getAuthorizedResource());
            ResourceType resourceType = ResourceType.valueOf(resourceContext.getField());
            PreConditions.notNull(resourceContext.getId(), "id");
            String resourceId = resourceContext.getId().toString();
            Map<User, Set<String>> userActionMap =
                    permissionAccessor.permittedUserActions(resourceType, resourceId, permission -> true);
            Set<String> permittedUserIds =
                    getAuthorizedUserIds(MoreObjects.firstNonNull(queryUserParams.getMinPrivilege(), "read"));
            if (!permittedUserIds.contains("*")) {
                userActionMap = userActionMap.entrySet().stream()
                        .filter(entry -> permittedUserIds.contains(entry.getKey().getId().toString()))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            }
            acquireRolesAndRoleIds(userActionMap.keySet());
            for (Map.Entry<User, Set<String>> entry : userActionMap.entrySet()) {
                User user = entry.getKey();
                user.setAuthorizedActions(entry.getValue());
                users.add(user);
            }
            return new PaginatedData<>(users, customPage);
        }
        return listUserWithQueryParams(queryUserParams, pageable, true);
    }

    @SkipAuthorize
    public PaginatedData<User> listUserBasicsWithoutPermissionCheck() {
        List<User> users =
                userRepository.findByOrganizationId(authenticationFacade.currentOrganizationId()).stream()
                        .map(User::new).collect(Collectors.toList());
        return new PaginatedData<>(users, CustomPage.empty());
    }

    @SkipAuthorize("odc internal usage")
    public List<User> getUsersByFuzzyNameWithoutPermissionCheck(@NonNull String name) {
        QueryUserParams params = QueryUserParams.builder()
                .names(Collections.singletonList(name))
                .accountNames(Collections.singletonList(name))
                .organizationId(authenticationFacade.currentOrganizationId())
                .build();

        List<User> users = listUserWithQueryParams(params, Pageable.unpaged(), false).getContents();
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }
        return users;
    }

    @SkipAuthorize("odc internal usage")
    public PaginatedData<User> listUserWithQueryParams(QueryUserParams params, Pageable pageable,
            boolean preAuthenticate) {
        boolean findAllUsers = false;
        List<Long> queryUserIds = new ArrayList<>();
        List<Long> roleIds = params.getRoleIds();
        Set<String> permittedUserIds = getAuthorizedUserIds(MoreObjects.firstNonNull(params.getMinPrivilege(), "read"));
        if (!preAuthenticate || permittedUserIds.contains("*")) {
            if (CollectionUtils.isNotEmpty(roleIds)) {
                queryUserIds = getUserIdsByRoleIds(roleIds);
            } else {
                findAllUsers = true;
            }
        } else if (CollectionUtils.isNotEmpty(roleIds)) {
            queryUserIds = getUserIdsByRoleIds(roleIds).stream().filter(id -> permittedUserIds.contains(id.toString()))
                    .collect(Collectors.toList());
        } else {
            queryUserIds = permittedUserIds.stream().map(Long::parseLong).collect(Collectors.toList());
        }

        if (!findAllUsers && queryUserIds.isEmpty()) {
            return new PaginatedData<>(Collections.emptyList(), CustomPage.empty());
        }

        List<LastSuccessLoginHistory> loginHistories = loginHistoryRepository.lastSuccessLoginHistory();
        Map<Long, LastSuccessLoginHistory> userId2LoginHistory =
                loginHistories.stream().collect(Collectors.toMap(LastSuccessLoginHistory::getUserId, t -> t));
        List<Order> orderList = pageable.getSort().get().map(order -> {
            if ("lastLoginTime".equals(order.getProperty())) {
                StringBuilder orderByConditionSQL = new StringBuilder("(CASE");
                int index;
                for (index = 0; index < loginHistories.size(); index++) {
                    orderByConditionSQL.append(" WHEN id = ").append(loginHistories.get(index).getUserId())
                            .append(" THEN ").append(index);
                }
                orderByConditionSQL.append(" ELSE ").append(index).append(" END )");
                return JpaSort.unsafe(order.getDirection(), orderByConditionSQL.toString()).get()
                        .collect(Collectors.toList()).get(0);
            } else {
                return order;
            }
        }).collect(Collectors.toList());

        Specification<UserEntity> spec =
                Specification.where(UserSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                        .and(UserSpecs.enabledEqual(params.getEnabled()))
                        .and(UserSpecs.namesLike(params.getNames())
                                .or(UserSpecs.accountNamesLike(params.getAccountNames())));
        if (!findAllUsers) {
            spec = spec.and(UserSpecs.userIdIn(queryUserIds));
        }
        spec = spec.and(UserSpecs.sort(Sort.by(orderList)));
        Pageable page = pageable.equals(Pageable.unpaged()) ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<UserEntity> userEntityPage = userRepository.findAll(spec, page);
        List<User> users = userEntityPage.getContent().stream().map(User::new).collect(Collectors.toList());
        acquireRolesAndRoleIds(users);
        if (Objects.nonNull(params.getIncludePermissions()) && params.getIncludePermissions()) {
            acquirePermissions(users);
        }
        for (User user : users) {
            if (userId2LoginHistory.containsKey(user.getId())) {
                user.setLastLoginTime(
                        new Timestamp(userId2LoginHistory.get(user.getId()).getLastLoginTime().toEpochSecond() * 1000));
            }
        }
        return new PaginatedData<>(users, CustomPage.from(userEntityPage));
    }

    private void acquireRolesAndRoleIds(@NotEmpty Collection<User> users) {
        Map<Long, User> userId2User = users.stream().collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, List<UserRoleEntity>> userId2UserRoleEntities =
                userRoleRepository
                        .findByOrganizationIdAndUserIdIn(authenticationFacade.currentOrganizationId(),
                                userId2User.keySet())
                        .stream()
                        .collect(Collectors.groupingBy(UserRoleEntity::getUserId));
        for (Long userId : userId2UserRoleEntities.keySet()) {
            List<Role> roles = new ArrayList<>();
            List<Long> roleIds = new ArrayList<>();
            for (UserRoleEntity userRoleEntity : userId2UserRoleEntities.get(userId)) {
                RoleEntity role = roleRepository.findById(userRoleEntity.getRoleId()).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_ROLE, "id", userRoleEntity.getRoleId()));
                if (Objects.nonNull(role)) {
                    roles.add(new Role(role));
                    roleIds.add(role.getId());
                }
            }
            userId2User.get(userId).setRoleIds(roleIds);
            userId2User.get(userId).setRoles(roles);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public User update(long id, UpdateUserReq updateUserReq) {
        UserEntity userEntity = nullSafeGet(id);
        if (isBuiltin(userEntity)) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"admin account"},
                    "Operation on admin account is not allowed");
        }
        if (!StringUtils.isEmpty(updateUserReq.getName())) {
            userEntity.setName(updateUserReq.getName());
        }
        if (id == authenticationFacade.currentUserId()) {
            PreConditions.validArgumentState(updateUserReq.isEnabled(), ErrorCodes.BadRequest,
                    new Object[] {"Can not disable yourself"}, "Can not disable yourself");
        }
        userEntity.setEnabled(updateUserReq.isEnabled());
        userEntity.setDescription(updateUserReq.getDescription());
        permissionValidator.checkCurrentOrganization(new User(userEntity));
        userRepository.update(userEntity);
        publishTriggerEventAfterTx(new User(userEntity), TriggerEvent.USER_UPDATED);
        log.info("User has been updated: {}", userEntity);

        if (!Objects.isNull(updateUserReq.getRoleIds())) {
            List<UserRoleEntity> relations = userRoleRepository.findByOrganizationIdAndUserIdIn(
                    authenticationFacade.currentOrganizationId(), Arrays.asList(userEntity.getId()));
            Set<Long> attachedRoleIds = new HashSet<>();
            if (!CollectionUtils.isEmpty(relations)) {
                attachedRoleIds = relations.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet());
            }
            Long creatorId = authenticationFacade.currentUserId();
            inspectVerticalUnauthorized(authenticationFacade.currentUser(), updateUserReq.getRoleIds());
            userRoleRepository.deleteByOrganizationIdAndUserId(authenticationFacade.currentOrganizationId(), id);
            userRoleRepository.flush();
            for (Long roleId : updateUserReq.getRoleIds()) {
                Role role = new Role(roleRepository.findById(roleId)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_ROLE, "id", roleId)));
                permissionValidator.checkCurrentOrganization(role);
                if (role.getType() == RoleType.INTERNAL && !attachedRoleIds.contains(role.getId())) {
                    /**
                     * 要关联一个内置角色，且这个内置角色以前没有关联过（新增角色），这种情况需要报错。
                     */
                    String errMsg = "Internal role does not allow operation";
                    throw new BadArgumentException(ErrorCodes.BadArgument, new Object[] {errMsg}, errMsg);
                }
                UserRoleEntity userRoleEntity = new UserRoleEntity();
                userRoleEntity.setOrganizationId(role.getOrganizationId());
                userRoleEntity.setUserId(userEntity.getId());
                userRoleEntity.setCreatorId(creatorId);
                userRoleEntity.setRoleId(roleId);
                userRoleRepository.saveAndFlush(userRoleEntity);
                log.debug("User to role relation has been updated: {}", userRoleEntity);
            }
        }
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public User setEnabled(long id, Boolean enabled) {
        PreConditions.validArgumentState(id != authenticationFacade.currentUserId(), ErrorCodes.BadRequest,
                new Object[] {"Can not enable or disable yourself"}, "Can not enable or disable yourself");
        UserEntity userEntity = nullSafeGet(id);
        if (isBuiltin(userEntity)) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"admin account"},
                    "Operation on admin account is not allowed");
        }
        permissionValidator.checkCurrentOrganization(new User(userEntity));
        userEntity.setEnabled(enabled);
        userRepository.update(userEntity);
        User user = new User(userEntity);
        publishTriggerEventAfterTx(user, TriggerEvent.USER_UPDATED);
        return user;
    }

    /**
     * change password only refer to current user
     */
    @Transactional(rollbackFor = Exception.class)
    public User changePassword(ChangePasswordReq req) {
        PreConditions.validPassword(req.getNewPassword());
        UserEntity userEntity;
        if (req.getUsername() != null) {
            userEntity = nullSafeGet(req.getUsername());
            PreConditions.validRequestState(
                    !passwordEncoder.matches(req.getNewPassword(), userEntity.getPassword()),
                    ErrorCodes.UserIllegalNewPassword, new Object[] {},
                    "New password has to be different from old password");
            SecurityContextUtils.setCurrentUser(new User(userEntity));
        } else {
            userEntity = nullSafeGet(authenticationFacade.currentUserId());
            permissionValidator.checkCurrentOrganization(authenticationFacade.currentUser());
        }
        String previousPassword = userEntity.getPassword();
        FailedLoginAttemptLimiter attemptLimiter = userIdChangePasswordAttamptCache.get(userEntity.getId());
        Verify.notNull(attemptLimiter, "AttemptLimiter");
        Boolean validateResult = attemptLimiter.attempt(
                () -> passwordEncoder.matches(req.getCurrentPassword(), userEntity.getPassword()));
        PreConditions.validRequestState(validateResult, ErrorCodes.UserWrongPasswordOrNotFound,
                new Object[] {attemptLimiter.getRemainAttempt() < 0 ? "unlimited" : attemptLimiter.getRemainAttempt()},
                "currentPassword is not correct");

        userEntity.setPassword(encodePassword(req.getNewPassword()));
        userEntity.setActive(true);
        userRepository.updatePassword(userEntity);

        PasswordChangeEvent event = new PasswordChangeEvent();
        event.setUserId(userEntity.getId());
        event.setPreviousPassword(previousPassword);
        event.setNewPassword(userEntity.getPassword());
        for (Consumer<PasswordChangeEvent> hook : postPasswordChangeHooks) {
            hook.accept(event);
        }
        return new User(userEntity);
    }

    @SkipAuthorize("odc internal usage")
    public void addPostPasswordChangeHook(Consumer<PasswordChangeEvent> hook) {
        postPasswordChangeHooks.add(hook);
    }

    @SkipAuthorize("odc internal usage")
    public void addPostUserDeleteHook(Consumer<UserDeleteEvent> hook) {
        postUserDeleteHooks.add(hook);
    }

    @SkipAuthorize("odc internal usage")
    public void addPreUserDeleteHook(Consumer<UserDeleteEvent> hook) {
        preUserDeleteHooks.add(hook);
    }

    /**
     * reset password only refer to admin user
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public User resetPassword(long id, String password) {
        UserEntity userEntity = nullSafeGet(id);
        permissionValidator.checkCurrentOrganization(new User(userEntity));
        String previousPassword = userEntity.getPassword();

        PreConditions.validPassword(password);
        userEntity.setPassword(encodePassword(password));
        userRepository.updatePassword(userEntity);
        userEntity.setActive(false);
        userRepository.update(userEntity);

        PasswordChangeEvent event = new PasswordChangeEvent();
        event.setUserId(userEntity.getId());
        event.setPreviousPassword(previousPassword);
        event.setNewPassword(userEntity.getPassword());
        for (Consumer<PasswordChangeEvent> hook : postPasswordChangeHooks) {
            hook.accept(event);
        }
        return new User(userEntity);
    }

    @SkipAuthorize("odc internal usage")
    public UserEntity nullSafeGet(long id) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(id);
        PreConditions.validExists(ResourceType.ODC_USER, "id", id, userEntityOptional::isPresent);
        return userEntityOptional.get();
    }

    @SkipAuthorize("odc internal usage")
    public UserEntity nullSafeGet(String accountName) {
        return userRepository.findByAccountName(accountName)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_USER, "accountName", accountName));
    }

    @SkipAuthorize("odc internal usage")
    public List<User> batchNullSafeGet(@NonNull Collection<Long> ids) {
        List<UserEntity> entities = userRepository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(UserEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_USER, "id", absentIds);
        }
        return entities.stream().map(User::new).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public List<User> getByOrganizationId(@NonNull Long organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream().map(User::new).collect(Collectors.toList());
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private boolean isBuiltin(UserEntity userEntity) {
        return userEntity.getBuiltIn();
    }

    @Data
    public static class PasswordChangeEvent {
        private Long userId;
        private String previousPassword;
        private String newPassword;
    }
    @Data
    public static class UserDeleteEvent {
        private Long organizationId;
        private Long userId;
        private String accountName;
    }

    private void inspectVerticalUnauthorized(User operator, List<Long> roleIdsToBeAttached) {
        Validate.notNull(roleIdsToBeAttached,
                "RoleIdsToBeAttached can not be null for UserServcei#inspectVerticalUnauthorized");
        List<Permission> permissions = new LinkedList<>(
                permissionMapper.getResourcePermissions(permissionRepository.findByRoleIds(roleIdsToBeAttached)));
        boolean checkResult =
                authorizationFacade.isImpliesPermissions(operator, permissions);
        if (!checkResult) {
            String errMsg = "Cannot grant permissions that the current user does not have";
            throw new BadRequestException(ErrorCodes.GrantPermissionFailed, new Object[] {errMsg}, errMsg);
        }
    }

    private List<Long> getUserIdsByRoleIds(@NonNull List<Long> roleIds) {
        List<Long> userIds = new ArrayList<>();
        if (CollectionUtils.isEmpty(roleIds)) {
            return userIds;
        }
        userIds = userRoleRepository.findByRoleIdIn(roleIds).stream().map(UserRoleEntity::getUserId).distinct()
                .collect(Collectors.toList());
        if (roleIds.contains(WITHOUT_ROLE_ID)) {
            userIds.addAll(userRepository.findByWithoutAnyRoles().stream().map(UserEntity::getId)
                    .collect(Collectors.toList()));
        }
        return userIds.stream().distinct().collect(Collectors.toList());
    }

    private void deleteRelatedPermissions(Long userId) {
        List<UserPermissionEntity> userPermissionEntities = userPermissionRepository.findByUserId(userId);
        List<Long> permissionIds =
                userPermissionEntities.stream().map(UserPermissionEntity::getPermissionId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(permissionIds)) {
            int affectNum = permissionRepository.deleteByIds(permissionIds);
            log.info("User permissions have been deleted, affectNum={}, userId={}", affectNum, userId);
        }
        int affectNum = userPermissionRepository.deleteByUserIds(Collections.singleton(userId));
        log.info("User permission relations have been deleted, affectNum={}, roleId={}", affectNum, userId);
    }

    private Set<String> getAuthorizedUserIds(@NonNull String action) {
        Map<String, Set<String>> permittedUserId2Actions = permissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_USER, permission -> {
                    ResourcePermission minPrivilege = new ResourcePermission(permission.getResourceId(),
                            ResourceType.ODC_USER.name(), action);
                    return permission.implies(minPrivilege);
                });
        return permittedUserId2Actions.keySet();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_USER", isForAll = true)
    public List<User> batchImport(@NotEmpty @Valid List<CreateUserReq> createUserReqs) {
        List<UserEntity> entities = new ArrayList<>();
        Long creatorId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        Map<String, UserEntity> map = new HashMap<>();
        for (CreateUserReq createUserReq : createUserReqs) {
            UserEntity userEntity = new UserEntity();
            userEntity.setBuiltIn(false);
            userEntity.setType(UserType.USER);
            userEntity.setName(createUserReq.getName());
            userEntity.setAccountName(createUserReq.getAccountName());
            userEntity.setPassword(encodePassword(createUserReq.getPassword()));
            userEntity.setCipher(Cipher.BCRYPT);
            userEntity.setCreatorId(creatorId);
            userEntity.setEnabled(createUserReq.isEnabled());
            userEntity.setDescription(createUserReq.getDescription());
            userEntity.setOrganizationId(organizationId);
            userEntity.setUserCreateTime(new Timestamp(System.currentTimeMillis()));
            userEntity.setUserUpdateTime(new Timestamp(System.currentTimeMillis()));
            map.put(userEntity.getAccountName(), userEntity);
            entities.add(userEntity);
        }
        Set<String> createUserAccountName = new HashSet<>();
        for (CreateUserReq createUserReq : createUserReqs) {
            createUserAccountName.add(createUserReq.getAccountName());
        }
        if (createUserAccountName.size() != createUserReqs.size()) {
            throw new BadRequestException("Cannot import user with the same accountName");
        }
        userRepository.saveAll(entities);
        userRepository.flush();
        batchBindOrganization(map);
        batchBindRoles(createUserReqs, map);
        List<User> users = new ArrayList<>();
        for (UserEntity entity : entities) {
            users.add(new User(entity));
        }
        users.forEach(u -> publishTriggerEventAfterTx(u, TriggerEvent.USER_CREATED));
        return users;
    }

    private void batchBindOrganization(@NonNull Map<String, UserEntity> map) {
        for (UserEntity entity : map.values()) {
            userOrganizationService.bindUserToOrganization(entity.getId(), entity.getOrganizationId());
        }
    }

    private void batchBindRoles(List<CreateUserReq> createUserReqs, Map<String, UserEntity> map) {
        Set<Long> roleIds = new HashSet<>();
        for (CreateUserReq createUserReq : createUserReqs) {
            if (CollectionUtils.isNotEmpty(createUserReq.getRoleIds())) {
                roleIds.addAll(createUserReq.getRoleIds());
            }
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        inspectVerticalUnauthorized(authenticationFacade.currentUser(), new ArrayList<>(roleIds));
        Map<Long, RoleEntity> roleId2Entity = roleRepository.findByIdIn(roleIds).stream()
                .collect(Collectors.toMap(RoleEntity::getId, entity -> entity));
        Verify.equals(roleIds.size(), roleId2Entity.keySet().size(), "roleIds.size()");
        Long creatorId = authenticationFacade.currentUserId();
        for (CreateUserReq createUserReq : createUserReqs) {
            if (CollectionUtils.isNotEmpty(createUserReq.getRoleIds())) {
                List<UserRoleEntity> userRoleEntities = new ArrayList<>();
                for (Long id : createUserReq.getRoleIds()) {
                    Role role = new Role(roleId2Entity.get(id));
                    PreConditions.validArgumentState(role.getType() != RoleType.INTERNAL, ErrorCodes.BadArgument,
                            new Object[] {"Internal role does not allow operation"},
                            "Internal role does not allow operation");
                    permissionValidator.checkCurrentOrganization(role);
                    UserRoleEntity userRoleEntity = new UserRoleEntity();
                    userRoleEntity.setOrganizationId(role.getOrganizationId());
                    userRoleEntity.setUserId(map.get(createUserReq.getAccountName()).getId());
                    userRoleEntity.setCreatorId(creatorId);
                    userRoleEntity.setRoleId(role.id());
                    userRoleEntities.add(userRoleEntity);
                }
                userRoleRepository.saveAll(userRoleEntities);
            }
        }
    }

    @SkipAuthorize
    public <E> void assignCreatorNameByCreatorId(Collection<E> elements, Function<E, Long> creatorIdMapper,
            BiConsumer<E, String> creatorNameSetter) {
        List<Long> ids = elements.stream().map(creatorIdMapper).collect(Collectors.toList());
        List<UserEntity> userEntities = userRepository.partitionFindById(ids, 100);
        Map<Long, UserEntity> userEntityMap =
                userEntities.stream().collect(Collectors.toMap(UserEntity::getId, t -> t));
        elements.forEach(c -> {
            UserEntity userEntity = userEntityMap.get(creatorIdMapper.apply(c));
            if (userEntity != null) {
                creatorNameSetter.accept(c, userEntity.getName());
            }
        });
    }

}
