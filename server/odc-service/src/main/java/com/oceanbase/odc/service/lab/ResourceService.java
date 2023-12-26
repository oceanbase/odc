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
package com.oceanbase.odc.service.lab;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;
import com.oceanbase.odc.service.lab.OBMySQLResourceService.DbResourceInfo;
import com.oceanbase.odc.service.lab.model.LabProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/12/16 下午8:05
 * @Description: [This class is responsible for creating and revoking Trial Lab users and their
 *               corresponding ODC resources]
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class ResourceService {

    private final Map<Long, Semaphore> userId2Semaphore = new ConcurrentHashMap<>();
    @Autowired
    private OBMySQLResourceService obResourceService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;
    @Autowired
    private AuthorizationFacade authorizationFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private LabProperties labProperties;

    @Transactional(rollbackFor = Exception.class)
    public void createResource(User user) {
        if (!labProperties.isLabEnabled()) {
            return;
        }
        Semaphore semaphore = getSemaphoreByUserId(user.getId());
        boolean acquireSuccess = false;
        try {
            acquireSuccess = semaphore.tryAcquire(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("resource semaphore acquisition waiting to be interrupted, reason=", e);
        }
        if (!acquireSuccess) {
            log.warn("acquire create resource semaphore failed");
            throw new ConflictException(ErrorCodes.ResourceCreating, "resource is creating, please wait");
        }
        try {
            tryCreateResourceOf(user);
        } catch (SQLException ex) {
            log.warn("Create resource failed, reason={}", ex.getMessage());
            throw new UnexpectedException("Create resource failed", ex);
        } finally {
            semaphore.release();
        }
    }

    private Semaphore getSemaphoreByUserId(Long userId) {
        return userId2Semaphore.computeIfAbsent(userId, key -> new Semaphore(1));
    }

    private Long createPublicConnection(DbResourceInfo dbResourceInfo, User user) {
        PreConditions.notNull(dbResourceInfo, "dbResourceInfo");
        PreConditions.notNull(user, "user");
        log.info("Create a public connection, username={}, dbname={}", dbResourceInfo.dbUsername,
                dbResourceInfo.getDbName());
        ConnectionConfig connection = new ConnectionConfig();
        connection.setOwnerId(user.getOrganizationId());
        connection.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        connection.setName("oceanbase_mysql_" + user.getAccountName() + "_" + RandomStringUtils.randomAlphabetic(6));
        String[] ipHost = dbResourceInfo.getHost().split(":");
        connection.setHost(ipHost[0]);
        if (ipHost.length == 2) {
            connection.setPort(Integer.parseInt(ipHost[1]));
        }
        connection.setDefaultSchema(dbResourceInfo.getDbName());
        connection.setUsername(dbResourceInfo.getDbUsername());
        connection.setPassword(dbResourceInfo.getPassword());
        connection.setClusterName(dbResourceInfo.getClusterName());
        connection.setTenantName(dbResourceInfo.getTenantName());
        connection.setType(dbResourceInfo.getConnectType());
        connection.setQueryTimeoutSeconds(60);
        connection.setEnabled(true);
        connection.setPasswordSaved(true);
        Map<String, String> properties = new HashMap<>();
        properties.put("maskHost", "true");
        connection.setProperties(properties);
        ConnectionConfig saved = connectionService.innerCreate(connection, authenticationFacade.currentUserId(), false);
        log.info("Connection created, connection id={}", saved.getId());
        return saved.getId();
    }

    private void tryCreateResourceOf(User user) throws SQLException {
        if (!isBondToPublicConnection(user)) {
            log.info("not bond to public connection, start create resource for user={}", user.getAccountName());
            DbResourceInfo dbResourceInfo = createDbResource(user);
            Long connectionId = createPublicConnection(dbResourceInfo, user);
            Role role = createRoleIfNecessary(connectionId, user);
            bindRoleToUserIfNecessary(role, user);
        }
    }

    private void bindRoleToUserIfNecessary(Role role, User user) {
        PreConditions.notNull(role, "role");
        PreConditions.notNull(user, "user");
        PreConditions.validArgumentState(role.getType() != RoleType.INTERNAL, ErrorCodes.BadArgument,
                new Object[] {"Internal role does not allow operation"}, "Internal role does not allow operation");
        List<UserRoleEntity> relations = userRoleRepository.findByUserId(user.getId());

        // for system_admin users
        if (IterableUtils.contains(labProperties.getAdminIds(), user.getAccountName())
                && relations.stream().noneMatch(userRoleRelation -> userRoleRelation.getRoleId().equals(1L))) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setOrganizationId(user.getOrganizationId());
            relation.setRoleId(1L);
            relation.setUserId(user.getId());
            relation.setCreatorId(user.getId());
            userRoleRepository.saveAndFlush(relation);
        }
        // for all users
        if (relations.stream().noneMatch(userRoleRelation -> userRoleRelation.getRoleId().equals(role.getId()))) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setOrganizationId(user.getOrganizationId());
            relation.setRoleId(role.getId());
            relation.setUserId(user.getId());
            relation.setCreatorId(user.getId());
            userRoleRepository.saveAndFlush(relation);
        }

    }

    private Role createRoleIfNecessary(Long connectionId, User user) {
        PreConditions.notNull(connectionId, "connectionId");
        PreConditions.notNull(user, "user");
        Optional<RoleEntity> roleEntityOptional =
                roleRepository.findByNameAndOrganizationId("role_" + user.getAccountName(), 1L);
        RoleEntity roleEntity = roleEntityOptional.orElseGet(() -> {
            RoleEntity roleInDb = new RoleEntity();
            roleInDb.setName("role_" + user.getAccountName());
            roleInDb.setEnabled(true);
            roleInDb.setType(RoleType.CUSTOM);
            roleInDb.setOrganizationId(user.getOrganizationId());
            roleInDb.setCreatorId(1L);
            roleInDb.setBuiltIn(true);
            roleInDb.setDescription("Auto generated role for Ob Official Website user");
            roleInDb.setUserCreateTime(new Timestamp(System.currentTimeMillis()));
            roleInDb.setUserUpdateTime(new Timestamp(System.currentTimeMillis()));
            RoleEntity created = roleRepository.saveAndFlush(roleInDb);
            log.info("New role has been inserted, roleId={}", created.getId());
            return created;
        });
        bindPublicConnectionPermissions(connectionId, user.getOrganizationId(), roleEntity);
        return new Role(roleEntity);
    }

    private void bindPublicConnectionPermissions(Long connectionId, Long organizationId, RoleEntity role) {
        PreConditions.notNull(connectionId, "connectionId");
        PreConditions.notNull(organizationId, "organizationId");
        PreConditions.notNull(role, "role");

        PermissionEntity permission = new PermissionEntity();
        permission.setAction("connect");
        permission.setResourceIdentifier(ResourceContextUtil.generateResourceIdentifierString(
                connectionId, ResourceType.ODC_CONNECTION));
        permission.setType(PermissionType.PUBLIC_RESOURCE);
        permission.setCreatorId(1L);
        permission.setOrganizationId(organizationId);
        permission.setBuiltIn(false);
        permissionRepository.saveAndFlush(permission);

        RolePermissionEntity rolePermissionEntity = new RolePermissionEntity();
        rolePermissionEntity.setRoleId(role.getId());
        rolePermissionEntity.setOrganizationId(organizationId);
        rolePermissionEntity.setPermissionId(permission.getId());
        rolePermissionEntity.setCreatorId(1L);
        rolePermissionRepository.saveAndFlush(rolePermissionEntity);
    }

    private void unBindPublicConnectionPermissions(Long connectionId) {
        PreConditions.notNull(connectionId, "connectionId");
        List<PermissionEntity> permissions = permissionRepository.findByOrganizationIdAndResourceIdentifier(1L,
                ResourceContextUtil.generateResourceIdentifierString(
                        connectionId, ResourceType.ODC_CONNECTION));
        for (PermissionEntity permission : permissions) {
            log.info("start to delete permission, permission id={}", permission.getId());
            rolePermissionRepository.deleteByPermissionId(permission.getId());
            permissionRepository.deleteById(permission.getId());
            log.info("delete permission successfully, permission id={}", permission.getId());
        }
    }


    private DbResourceInfo createDbResource(User user) throws SQLException {
        PreConditions.notNull(user, "user");
        PreConditions.notNull(user.getAccountName(), "user.accountName");
        String dbUsername = "user_" + user.getAccountName();
        String password = RandomStringUtils.randomAlphanumeric(16);
        String dbName = "db_" + user.getAccountName();
        log.info("start creating Db resource, username={}, dbName={}", dbUsername, dbName);
        return obResourceService.createObResource(dbUsername, password, dbName, user.getId());
    }

    private boolean isBondToPublicConnection(User user) {
        PreConditions.notNull(user, "user");
        PreConditions.notNull(user.getId(), "user.id");
        Map<SecurityResource, Set<String>> relatedResourcesAndActions =
                authorizationFacade.getRelatedResourcesAndActions(user);
        for (SecurityResource resource : relatedResourcesAndActions.keySet()) {
            if (resource instanceof DefaultSecurityResource
                    && ResourceType.ODC_CONNECTION.name().equals(resource.resourceType())
                    && !"*".equals(resource.resourceId())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized boolean revokeResource(Long connectionId) {
        if (!labProperties.isLabEnabled()) {
            return false;
        }
        if (Objects.isNull(connectionId)) {
            log.warn("skip revoke resource due to null connectionId");
            return false;
        }
        Optional<ConnectionEntity> opt = connectionConfigRepository.findById(connectionId);
        if (opt.isPresent()) {
            ConnectionEntity connection = opt.get();
            if (connection.getName().startsWith("oceanbase_mysql_") || connection.getName().startsWith("trial_")) {
                connectionConfigRepository.deleteById(connection.getId());
                log.info("revoke connection config successfully, connectionId={}", connection.getId());
                unBindPublicConnectionPermissions(connection.getId());
                log.info("unbind public connection permission successfully, connectionId={}", connection.getId());
                obResourceService.revokeObResource(connection.getUsername(), connection.getDefaultSchema(),
                        connection.getCreatorId());
                log.info("revoke database and user successfully, connectionId={}", connection.getId());
                return true;
            }
        }
        return false;
    }
}

