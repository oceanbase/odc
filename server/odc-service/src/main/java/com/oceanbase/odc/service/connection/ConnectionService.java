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
package com.oceanbase.odc.service.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectionStatus;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.ConnectionAttributeEntity;
import com.oceanbase.odc.metadb.connection.ConnectionAttributeRepository;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryRepository;
import com.oceanbase.odc.metadb.connection.ConnectionSpecs;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.environment.model.QueryEnvironmentParam;
import com.oceanbase.odc.service.collaboration.project.ProjectMapper;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.response.CustomPage;
import com.oceanbase.odc.service.common.response.PageAndStats;
import com.oceanbase.odc.service.common.response.PaginatedData;
import com.oceanbase.odc.service.connection.ConnectionStatusManager.CheckState;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.DatabaseSyncManager;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.QueryConnectionParams;
import com.oceanbase.odc.service.connection.ssl.ConnectionSSLAdaptor;
import com.oceanbase.odc.service.connection.util.ConnectionIdList;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncProperties;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.PermissionService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.UserPermissionService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/23 20:33
 * @Description: []
 */
@Slf4j
@Service
@Validated
@Authenticated
public class ConnectionService {

    @Autowired
    private ConnectionConfigRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ConnectionEncryption connectionEncryption;

    @Autowired
    private ConnectionStatusManager statusManager;

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private AuthorizationFacade authorizationFacade;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @Autowired
    private ConnectionPermissionFilter connectionFilter;

    @Autowired
    private ConnectProperties connectProperties;

    @Autowired
    private ConnectionAdapter environmentAdapter;

    @Autowired
    private ConnectionSSLAdaptor connectionSSLAdaptor;

    @Autowired
    private ConnectionValidator connectionValidator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    @Lazy
    private UserPermissionService userPermissionService;

    @Autowired
    @Lazy
    private DatabaseSyncManager databaseSyncManager;

    @Autowired
    @Lazy
    private DatabaseService databaseService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private ConnectionAttributeRepository attributeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ConnectionHistoryRepository connectionHistoryRepository;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    @Autowired
    private DBSchemaSyncProperties dbSchemaSyncProperties;

    @Autowired
    private TransactionTemplate txTemplate;

    private final ConnectionMapper mapper = ConnectionMapper.INSTANCE;

    public static final String DEFAULT_MIN_PRIVILEGE = "read";

    private static final String UPDATE_DS_SCHEMA_LOCK_KEY_PREFIX = "update-ds-schema-lock-";

    @PreAuthenticate(actions = "create", resourceType = "ODC_CONNECTION", isForAll = true)
    public ConnectionConfig create(@NotNull @Valid ConnectionConfig connection) {
        return create(connection, currentUserId(), false);
    }

    @SkipAuthorize("odc internal usage")
    public ConnectionConfig create(@NotNull @Valid ConnectionConfig connection, @NotNull Long creatorId,
            boolean skipPermissionCheck) {
        ConnectionConfig saved = txTemplate.execute(status -> {
            try {
                ConnectionConfig created = innerCreate(connection, creatorId, skipPermissionCheck);
                userPermissionService.bindUserAndDataSourcePermission(creatorId, currentOrganizationId(),
                        created.getId(), Arrays.asList("read", "update", "delete"));
                return created;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
        databaseSyncManager.submitSyncDataSourceAndDBSchemaTask(saved);
        return saved;
    }

    @PreAuthenticate(actions = "create", resourceType = "ODC_CONNECTION", isForAll = true)
    public List<ConnectionConfig> batchCreate(@NotEmpty @Valid List<ConnectionConfig> connections) {
        List<ConnectionConfig> saved = txTemplate.execute(status -> {
            try {
                List<ConnectionConfig> created = new ArrayList<>();
                for (ConnectionConfig connection : connections) {
                    ConnectionConfig config = innerCreate(connection, currentUserId(), false);
                    userPermissionService.bindUserAndDataSourcePermission(currentUserId(), currentOrganizationId(),
                            config.getId(), Arrays.asList("read", "update", "delete"));
                    created.add(config);
                }
                return created;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
        saved.forEach(databaseSyncManager::submitSyncDataSourceAndDBSchemaTask);
        return saved;
    }

    @SkipAuthorize("odc internal usage")
    public ConnectionConfig innerCreate(@NotNull @Valid ConnectionConfig connection, @NotNull Long creatorId,
            boolean skipPermissionCheck) {
        ConnectionConfig created = txTemplate.execute(status -> {
            try {
                environmentAdapter.adaptConfig(connection);
                connectionSSLAdaptor.adapt(connection);
                if (!connection.getType().isDefaultSchemaRequired()) {
                    connection.setDefaultSchema(null);
                }
                connectionValidator.validateForUpsert(connection);
                connectionValidator.validatePrivateConnectionTempOnly(connection.getTemp());
                if (!skipPermissionCheck) {
                    checkProjectOperable(connection.getProjectId());
                }
                connection.setOrganizationId(currentOrganizationId());
                connection.setCreatorId(creatorId);
                if (Objects.isNull(connection.getProperties())) {
                    connection.setProperties(new HashMap<>());
                }
                if (Objects.isNull(connection.getTemp())) {
                    connection.setTemp(false);
                }
                String name = connection.getName();
                Long organizationId = currentOrganizationId();
                PreConditions.validNoDuplicated(ResourceType.ODC_CONNECTION, "organizationId,name",
                        org.apache.commons.lang3.StringUtils.joinWith(",", organizationId, name),
                        () -> exists(organizationId, name));
                if (connection.getPasswordSaved()) {
                    PreConditions.notNull(connection.getPassword(), "connection.password");
                } else {
                    connection.setPassword(null);
                    connection.setPasswordEncrypted(null);
                }
                connectionEncryption.encryptPasswords(connection);
                ConnectionEntity entity = modelToEntity(connection);
                ConnectionEntity savedEntity = repository.saveAndFlush(entity);
                ConnectionConfig config = entityToModel(savedEntity, true, true);
                config.setAttributes(connection.getAttributes());
                List<ConnectionAttributeEntity> attrEntities = connToAttrEntities(config);
                attrEntities = this.attributeRepository.saveAll(attrEntities);
                config.setAttributes(attrEntitiesToMap(attrEntities));
                return config;
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
        log.info("Connection created, connection={}", created);
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig delete(@NotNull Long id) {
        ConnectionConfig connection = internalGet(id);
        log.info("Delete related metadata entity, id={}", id);
        int affectRows = databaseService.deleteByDataSourceId(id);
        log.info("delete related databases successfully, affectRows={}, id={}", affectRows, id);
        affectRows = attributeRepository.deleteByConnectionId(id);
        log.info("delete related attributes successfully, affectRows={}, id={}", affectRows, id);
        affectRows = connectionHistoryRepository.deleteByConnectionId(id);
        log.info("delete related session access history successfully, affectRows={}, id={}", affectRows, id);
        repository.deleteById(id);
        permissionService.deleteResourceRelatedPermissions(id, ResourceType.ODC_CONNECTION,
                PermissionType.PUBLIC_RESOURCE);
        return connection;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public List<ConnectionConfig> delete(@NotNull Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ConnectionConfig> connections = entitiesToModels(this.repository
                .findAll(ConnectionSpecs.idIn(ids)), currentOrganizationId(), false, false);
        if (CollectionUtils.isEmpty(connections)) {
            return Collections.emptyList();
        }
        List<Permission> permissions = connections.stream()
                .map(c -> securityManager.getPermissionByActions(c, Collections.singleton("delete")))
                .collect(Collectors.toList());
        this.securityManager.checkPermission(permissions);

        List<PermissionEntity> permissionEntities = this.permissionService.deleteResourceRelatedPermissions(
                ids, ResourceType.ODC_CONNECTION);
        log.info("Delete datasource-related metadata entity, affectEntities={}", permissionEntities.size());
        int affectRows = databaseService.deleteByDataSourceIds(ids);
        log.info("delete datasource-related databases successfully, affectRows={}", affectRows);
        affectRows = repository.deleteByIds(ids);
        log.info("delete datasources successfully, affectRows={}", affectRows);
        affectRows = this.attributeRepository.deleteByConnectionIds(ids);
        log.info("delete related attributes successfully, affectRows={}", affectRows);
        return connections;
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig detail(@NotNull Long id) {
        ConnectionConfig conn = getWithoutPermissionCheck(id);
        conn.setDbObjectLastSyncTime(getEarliestObjectSyncTime(conn.getId()));
        return conn;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public ConnectionConfig getWithoutPermissionCheck(@NotNull Long id) {
        ConnectionConfig connection = internalGet(id);
        if (connection.getCreatorId() != null) {
            Optional<UserEntity> userEntity = userRepository.findById(connection.getCreatorId());
            connection.setCreatorName(userEntity.isPresent() ? userEntity.get().getAccountName() : "N/A");
        }
        attachPermittedActions(connection);
        maskConnectionHost(connection);
        setSupportedOperations(connection);
        return connection;
    }

    @SkipAuthorize("odc internal usage")
    public List<ConnectionConfig> listByOrganizationId(@NonNull Long organizationId) {
        return entitiesToModels(repository.findByOrganizationId(organizationId), organizationId, true, true);
    }

    @SkipAuthorize("odc internal usage")
    public List<ConnectionConfig> listByOrganizationIdWithoutEnvironment(@NonNull Long organizationId) {
        return entitiesToModels(repository.findByOrganizationId(organizationId), organizationId, false, false);
    }

    @SkipAuthorize("odc internal usage")
    public List<ConnectionConfig> listByOrganizationIdAndEnvironmentId(@NonNull Long organizationId,
            @NonNull Long environmentId) {
        return repository.findByOrganizationIdAndEnvironmentId(organizationId, environmentId).stream()
                .map(mapper::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public List<ConnectionConfig> listByOrganizationIdIn(@NonNull Collection<Long> organizationIds) {
        return repository.findByOrganizationIdIn(organizationIds).stream()
                .map(mapper::entityToModel).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, DEVELOPER, SECURITY_ADMINISTRATOR"},
            resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public PaginatedData<ConnectionConfig> listByProjectId(@NotNull Long projectId, @NotNull Boolean basic) {
        List<ConnectionConfig> connections;
        if (basic) {
            connections = repository.findByDatabaseProjectId(projectId).stream().map(e -> {
                ConnectionConfig c = new ConnectionConfig();
                c.setId(e.getId());
                c.setName(e.getName());
                c.setType(e.getType());
                return c;
            }).collect(Collectors.toList());
        } else {
            connections = repository.findByDatabaseProjectId(projectId).stream().map(mapper::entityToModel)
                    .collect(Collectors.toList());
        }
        return new PaginatedData<>(connections, CustomPage.empty());
    }

    @SkipAuthorize("public readonly info")
    public boolean exists(@NotNull String connectionName) {
        return exists(currentOrganizationId(), connectionName);
    }

    @SkipAuthorize("permission check inside")
    public Map<Long, CheckState> getStatus(@NonNull Set<Long> ids) {
        Set<Long> connIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(connIds)) {
            return new HashMap<>();
        }
        User user = authenticationFacade.currentUser();
        Specification<ConnectionEntity> spec = Specification
                .where(ConnectionSpecs.organizationIdEqual(currentOrganizationId()))
                .and(ConnectionSpecs.idIn(connIds));
        Map<Long, ConnectionConfig> connMap =
                entitiesToModels(repository.findAll(spec), currentOrganizationId(), false, false).stream()
                        .collect(Collectors.toMap(ConnectionConfig::getId, c -> c));

        if (authenticationFacade.currentOrganization().getType() == OrganizationType.INDIVIDUAL) {
            return getIndividualSpaceStatus(ids, connMap);
        } else {
            return getTeamSpaceStatus(ids, connMap, user);
        }
    }

    private Map<Long, CheckState> getIndividualSpaceStatus(Set<Long> ids, Map<Long, ConnectionConfig> connMap) {
        Map<Long, CheckState> connId2State = new HashMap<>();
        for (Long connId : ids) {
            ConnectionConfig conn = connMap.get(connId);
            if (conn == null) {
                connId2State.put(connId, CheckState.of(ConnectionStatus.UNKNOWN));
            } else {
                connId2State.put(connId, this.statusManager.getAndRefreshStatus(conn));
            }
        }
        return connId2State;
    }

    private Map<Long, CheckState> getTeamSpaceStatus(Set<Long> ids, Map<Long, ConnectionConfig> connMap, User user) {
        Map<SecurityResource, Set<String>> res2Actions = authorizationFacade.getRelatedResourcesAndActions(user)
                .entrySet().stream().collect(Collectors.toMap(e -> {
                    SecurityResource s = e.getKey();
                    return new DefaultSecurityResource(s.resourceId(), s.resourceType());
                }, Entry::getValue));
        List<Permission> granted = res2Actions.entrySet().stream()
                .map(e -> securityManager.getPermissionByActions(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        Set<Long> projectRelatedConnectionIds = databaseService.statsConnectionConfig().stream()
                .map(ConnectionConfig::getId).collect(Collectors.toSet());
        Map<Long, CheckState> connId2State = new HashMap<>();
        for (Long connId : ids) {
            ConnectionConfig conn = connMap.get(connId);
            if (conn == null) {
                // may invalid connection id, set the status to UNKNOWN
                connId2State.put(connId, CheckState.of(ConnectionStatus.UNKNOWN));
                continue;
            }
            Permission p = new ConnectionPermission(connId + "", ResourcePermission.READ);
            boolean hasReadPermission = granted.stream().anyMatch(g -> g.implies(p));
            boolean joinedProject = projectRelatedConnectionIds.contains(connId);
            if (hasReadPermission || joinedProject) {
                connId2State.put(connId, this.statusManager.getAndRefreshStatus(conn));
            } else {
                connId2State.put(connId, CheckState.of(ConnectionStatus.UNKNOWN));
            }
        }
        return connId2State;
    }

    @SkipAuthorize("odc internal usage")
    public ConnectionConfig getBasicWithoutPermissionCheck(@NonNull Long id) {
        return repository.findById(id).map(mapper::entityToModel)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_CONNECTION, "id", id));
    }

    @SkipAuthorize("odc internal usage")
    public List<ConnectionConfig> batchNullSafeGet(@NonNull Collection<Long> ids) {
        List<ConnectionEntity> entities = repository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(ConnectionEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_CONNECTION, "id", absentIds);
        }
        return entitiesToModels(entities, currentOrganizationId(), false, false);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public Set<Long> findIdsByHost(String host) {
        return repository.findIdsByHost(host);
    }

    @SkipAuthorize("internal usage")
    public List<ConnectionConfig> listByVisibleScope(ConnectionVisibleScope visibleScope) {
        return repository.findByVisibleScope(visibleScope).stream().map(mapper::entityToModel)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public PageAndStats<ConnectionConfig> list(@Valid QueryConnectionParams params, @NotNull Pageable pageable) {
        ConnectionIdList connectionIdList;
        User user = authenticationFacade.currentUser();
        Long userId = user.getId();
        if (params.getRelatedUserId() != null) {
            userId = params.getRelatedUserId();
        }
        connectionIdList = getConnectionIdList(userId, params.getMinPrivilege(), params.getPermittedActions());
        if (CollectionUtils.isEmpty(connectionIdList.getConnectionIds())) {
            return PageAndStats.empty();
        }
        params.setIds(
                connectionIdList.getConnectionIds().stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        final ConnectionIdList finalList = connectionIdList;
        Page<ConnectionConfig> connections = innerList(params, pageable).map(conn -> {
            Long id = conn.getId();
            Set<String> actions = finalList.getActions(id);
            conn.setPermittedActions(actions);
            conn.setStatus(statusManager.getAndRefreshStatus(conn));
            maskConnectionHost(conn);
            return conn;
        });
        List<ConnectionConfig> conns = connections.stream().filter(c -> !c.getTemp()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(conns)) {
            return PageAndStats.of(connections, new Stats());
        }
        Set<String> clusterNames = conns.stream()
                .filter(c -> StringUtils.isNotEmpty(c.getClusterName()))
                .map(ConnectionConfig::getClusterName).collect(Collectors.toSet());
        Set<String> tenantNames = conns.stream()
                .filter(c -> StringUtils.isNotEmpty(c.getTenantName()))
                .map(ConnectionConfig::getTenantName).collect(Collectors.toSet());
        Stats stats = new Stats()
                .andDistinct("tenantName", tenantNames)
                .andDistinct("clusterName", clusterNames);
        return PageAndStats.of(connections, stats);
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig update(@NotNull Long id, @NotNull @Valid ConnectionConfig connection)
            throws InterruptedException {
        ConnectionConfig config = txTemplate.execute(status -> {
            try {
                environmentAdapter.adaptConfig(connection);
                connectionSSLAdaptor.adapt(connection);
                ConnectionConfig saved = internalGet(id);
                connectionValidator.validateForUpdate(connection, saved);
                checkProjectOperable(connection.getProjectId());
                if (StringUtils.isBlank(connection.getSysTenantUsername())) {
                    // sys 用户没有设的情况下，相应地，密码要设置为空
                    connection.setSysTenantPassword("");
                }
                connection.setId(id);
                connection.setCreatorId(saved.getCreatorId());
                connection.setOrganizationId(saved.getOrganizationId());
                connection.setType(saved.getType());
                connection.setCipher(saved.getCipher());
                connection.setSalt(saved.getSalt());
                connection.setTemp(saved.getTemp());
                connection.setPasswordEncrypted(null);
                connection.setSysTenantPasswordEncrypted(null);
                if (!connection.getType().isDefaultSchemaRequired()) {
                    connection.setDefaultSchema(null);
                }
                connectionValidator.validateForUpsert(connection);
                // validate same name while rename connection
                repository.findByOrganizationIdAndName(connection.getOrganizationId(), connection.getName())
                        .ifPresent(sameNameEntity -> {
                            if (!id.equals(sameNameEntity.getId())) {
                                throw new BadRequestException(ErrorCodes.ConnectionDuplicatedName,
                                        new Object[] {connection.getName()}, "same datasource name exists");
                            }
                        });
                if (Boolean.FALSE.equals(connection.getPasswordSaved())) {
                    connection.setPassword(null);
                }
                connectionEncryption.encryptPasswords(connection);
                connection.fillEncryptedPasswordFromSavedIfNull(saved);

                ConnectionEntity entity = modelToEntity(connection);
                ConnectionEntity savedEntity = repository.saveAndFlush(entity);

                // for workaround createTime/updateTime not refresh in server mode,
                // seems JPA bug, it works while UT
                entityManager.refresh(savedEntity);
                ConnectionConfig updated = entityToModel(savedEntity, true, true);
                this.attributeRepository.deleteByConnectionId(updated.getId());
                updated.setAttributes(connection.getAttributes());
                List<ConnectionAttributeEntity> attrEntities = connToAttrEntities(updated);
                attrEntities = this.attributeRepository.saveAll(attrEntities);
                updated.setAttributes(attrEntitiesToMap(attrEntities));
                log.info("Connection updated, connection={}", updated);
                if (saved.getProjectId() != null && updated.getProjectId() == null) {
                    // Remove databases from project when unbind project from connection
                    try {
                        updateDatabaseProjectId(savedEntity, null, false);
                    } catch (InterruptedException e) {
                        throw new UnexpectedException("Failed to update database project id", e);
                    }
                }
                return updated;
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
        databaseSyncManager.submitSyncDataSourceAndDBSchemaTask(config);
        return config;
    }

    @SkipAuthorize("odc internal usage")
    public void updateDatabaseProjectId(Collection<ConnectionEntity> connectionIds, Long projectId,
            boolean blockInternalDatabase) throws InterruptedException {
        if (CollectionUtils.isEmpty(connectionIds)) {
            return;
        }
        for (ConnectionEntity entity : connectionIds) {
            updateDatabaseProjectId(entity, projectId, blockInternalDatabase);
        }
    }

    private void updateDatabaseProjectId(ConnectionEntity entity, Long projectId, boolean blockInternalDatabase)
            throws InterruptedException {
        Lock lock = jdbcLockRegistry.obtain(getUpdateDsSchemaLockKey(entity.getId()));
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            List<DatabaseEntity> entities = databaseRepository.findByConnectionId(entity.getId());
            List<String> blockDatabaseNames = dbSchemaSyncProperties.getExcludeSchemas(entity.getDialectType());
            entities.forEach(e -> {
                if (!blockInternalDatabase || !blockDatabaseNames.contains(e.getName())) {
                    e.setProjectId(projectId);
                }
            });
            databaseRepository.saveAll(entities);
        } finally {
            lock.unlock();
        }
    }

    @SkipAuthorize("odc internal usage")
    public String getUpdateDsSchemaLockKey(@NonNull Long datasourceId) {
        return UPDATE_DS_SCHEMA_LOCK_KEY_PREFIX + datasourceId;
    }

    @SkipAuthorize("internal usage")
    public Map<Long, List<ConnectionConfig>> mapByIdIn(Set<Long> ids) {
        if (org.springframework.util.CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return entitiesToModels(repository.findAllById(ids), authenticationFacade.currentOrganizationId(), true, true)
                .stream()
                .collect(Collectors.groupingBy(ConnectionConfig::getId));
    }

    @SkipAuthorize("odc internal usages")
    public List<Long> innerListIdByOrganizationIdAndNames(@NonNull Long organizationId,
            @NotEmpty Collection<String> names) {
        return repository.findByOrganizationIdAndNameIn(organizationId, names).stream().map(ConnectionEntity::getId)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usages")
    public List<ConnectionConfig> innerListByIds(@NotEmpty Collection<Long> ids) {
        return repository.findByIdIn(ids).stream().map(mapper::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("internal usage")
    public ConnectionConfig getForConnectionSkipPermissionCheck(@NotNull Long id) {
        ConnectionConfig connection = internalGetSkipUserCheck(id, false, false);

        int queryTimeoutSeconds = connection.queryTimeoutSeconds();
        Integer minQueryTimeoutSeconds = connectProperties.getMinQueryTimeoutSeconds();
        if (queryTimeoutSeconds < minQueryTimeoutSeconds) {
            connection.setQueryTimeoutSeconds(minQueryTimeoutSeconds);
            log.debug("queryTimeoutSeconds less than minQueryTimeoutSeconds, use {} instead", minQueryTimeoutSeconds);
        }
        connectionEncryption.decryptPasswords(connection);
        // Adapter should be called after decrypting passwords.
        environmentAdapter.adaptConfig(connection);
        connectionSSLAdaptor.adapt(connection);
        return connection;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig getForConnect(@NotNull Long id) {
        ConnectionConfig connection = getForConnectionSkipPermissionCheck(id);
        permissionValidator.checkCurrentOrganization(connection);
        return connection;
    }

    @SkipAuthorize("check permission inside")
    public boolean checkPermission(@NotNull Long connectionId, @NotEmpty List<String> actions) {
        return checkPermission(Collections.singleton(connectionId), actions);
    }

    @SkipAuthorize("check permission inside")
    public boolean checkPermission(@NotNull Collection<Long> connectionIds, @NotNull List<String> actions) {
        if (connectionIds.isEmpty() || actions.isEmpty()) {
            return true;
        }
        connectionIds = connectionIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        try {
            List<ConnectionConfig> connections = innerListByIds(connectionIds);
            permissionValidator.checkCurrentOrganization(connections);
            List<Permission> permissions = connections.stream()
                    .map(c -> securityManager.getPermissionByActions(c, actions)).collect(Collectors.toList());
            securityManager.checkPermission(permissions);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Page<ConnectionConfig> innerList(@NotNull QueryConnectionParams params, @NotNull Pageable pageable) {
        Specification<ConnectionEntity> spec = Specification
                .where(ConnectionSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()));
        String[] hostPort = getHostPort(params.getHostPort());
        String[] fuzzyHostPort = getHostPort(params.getFuzzySearchKeyword());

        // 业务筛选条件拼接
        spec = spec.and(ConnectionSpecs.enabledEqual(params.getEnabled()))
                .and(ConnectionSpecs.typeIn(params.getTypes()))
                .and(ConnectionSpecs.dialectTypeIn(params.getDialectTypes()))
                .and(ConnectionSpecs.tenantNamesLike(params.getTenantNames()))
                .and(ConnectionSpecs.clusterNamesLike(params.getClusterNames()))
                .and(ConnectionSpecs.nameLike(params.getFuzzySearchKeyword())
                        .or(ConnectionSpecs.hostLike(fuzzyHostPort[0]))
                        .or(ConnectionSpecs.portLike(fuzzyHostPort[1]))
                        .or(ConnectionSpecs.clusterNameLike(params.getFuzzySearchKeyword()))
                        .or(ConnectionSpecs.tenantNameLike(params.getFuzzySearchKeyword()))
                        .or(ConnectionSpecs.idLike(params.getFuzzySearchKeyword())))
                .and(ConnectionSpecs.isNotTemp())
                .and(ConnectionSpecs.portLike(hostPort[1])
                        .or(ConnectionSpecs.hostLike(hostPort[0])))
                .and(ConnectionSpecs.nameLike(params.getName()));
        if (CollectionUtils.isNotEmpty(params.getIds())) {
            spec = spec.and(ConnectionSpecs.idIn(params.getIds()));
        }
        if (Objects.nonNull(params.getUsername())) {
            spec = spec.and(ConnectionSpecs.usernameEqual(params.getUsername()));
        }
        spec = spec.and(ConnectionSpecs.sort(pageable.getSort()));
        Pageable page = pageable.equals(Pageable.unpaged()) ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<ConnectionEntity> entities = this.repository.findAll(spec, page);
        List<ConnectionConfig> models = entitiesToModels(entities.getContent(), currentOrganizationId(), true, true);
        return new PageImpl<>(models, page, entities.getTotalElements());
    }

    private String[] getHostPort(String hostPort) {
        if (hostPort == null) {
            return new String[] {null, null};
        }
        if (hostPort.isEmpty()) {
            return new String[] {"", ""};
        }
        int index = hostPort.indexOf(":");
        if (index == -1) {
            return new String[] {hostPort, hostPort};
        }
        return new String[] {hostPort.substring(0, index), hostPort.substring(index + 1)};
    }

    private ConnectionIdList getConnectionIdList(@NonNull Long userId, String minPrivilege,
            List<String> permittedActions) {
        Map<Long, Set<String>> id2Actions = connectionFilter.permittedConnectionActions(userId, c -> {
            ConnectionPermission min = new ConnectionPermission(c.getResourceId(), minPrivilege);
            if (CollectionUtils.isEmpty(permittedActions)) {
                return c.implies(min);
            }
            SecurityResource r = new DefaultSecurityResource(c.getResourceId(), c.getResourceType());
            return c.implies(min) && c.implies(securityManager.getPermissionByActions(r, permittedActions));
        });

        return new ConnectionIdList() {
            private final List<String> acceptedActions = Arrays.asList(
                    ResourcePermission.RESOURCE_CREATE_ACTION,
                    ResourcePermission.RESOURCE_READ_ACTION,
                    ResourcePermission.RESOURCE_UPDATE_ACTION,
                    ResourcePermission.RESOURCE_DELETE_ACTION);

            @Override
            public List<Long> getConnectionIds() {
                return new ArrayList<>(id2Actions.keySet());
            }

            @Override
            public Set<String> getActions(@NonNull Long connectionId) {
                return id2Actions.getOrDefault(connectionId, Collections.emptySet()).stream()
                        .filter(acceptedActions::contains).collect(Collectors.toSet());
            }
        };
    }

    private void setSupportedOperations(ConnectionConfig connection) {
        Boolean temp = connection.getTemp();
        boolean isTemp = !Objects.isNull(temp) && temp;
        Set<String> supportedOperations =
                connectProperties.getConnectionSupportedOperations(isTemp, connection.getPermittedActions());
        connection.setSupportedOperations(supportedOperations);
    }

    private boolean exists(Long organizationId, String name) {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setOrganizationId(organizationId);
        entity.setName(name);
        entity.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        return repository.exists(Example.of(entity));
    }

    private void checkProjectOperable(Long projectId) {
        if (Objects.isNull(projectId)) {
            return;
        }
        Project project = ProjectMapper.INSTANCE.entityToModel(projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_PROJECT, "id", projectId)));
        permissionValidator.checkCurrentOrganization(project);
        projectPermissionValidator.checkProjectRole(projectId,
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER));
    }

    private void attachPermittedActions(ConnectionConfig connection) {
        Set<String> actions = authorizationFacade.getAllPermittedActions(authenticationFacade.currentUser(),
                ResourceType.valueOf(connection.resourceType()), connection.getId() + "");
        if (actions.contains("*") && ResourceType.ODC_CONNECTION.name().equals(connection.resourceType())) {
            connection.setPermittedActions(ConnectionPermission.getAllActions());
        } else {
            connection.setPermittedActions(actions);
        }
    }

    private ConnectionConfig internalGet(Long id) {
        ConnectionConfig connection = internalGetSkipUserCheck(id, true, true);
        permissionValidator.checkCurrentOrganization(connection);
        return connection;
    }

    @SkipAuthorize("odc internal usage")
    public ConnectionConfig internalGetSkipUserCheck(Long id, boolean withEnvironment, boolean withProject) {
        ConnectionConfig config = entityToModel(getEntity(id), withEnvironment, withProject);
        List<ConnectionAttributeEntity> entities = this.attributeRepository.findByConnectionId(config.getId());
        config.setAttributes(attrEntitiesToMap(entities));
        return config;
    }

    private ConnectionEntity getEntity(@NonNull Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(ResourceType.ODC_CONNECTION, "id", id));
    }

    private List<ConnectionConfig> entitiesToModels(@NonNull List<ConnectionEntity> entities,
            @NonNull Long organizationId, @NonNull Boolean withEnvironment, @NonNull Boolean withProject) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        Map<Long, Environment> id2Environment;
        if (withEnvironment) {
            id2Environment = environmentService.list(organizationId, QueryEnvironmentParam.builder().build()).stream()
                    .collect(Collectors.toMap(Environment::getId, environment -> environment));
        } else {
            id2Environment = new HashMap<>();
        }
        Map<Long, String> id2ProjectName;
        if (withProject) {
            List<Long> projectIds = entities.stream().map(ConnectionEntity::getProjectId).filter(Objects::nonNull)
                    .collect(Collectors.toList());
            id2ProjectName = projectRepository.findByIdIn(projectIds).stream()
                    .collect(Collectors.toMap(ProjectEntity::getId, ProjectEntity::getName));
        } else {
            id2ProjectName = new HashMap<>();
        }
        return entities.stream().map(entity -> {
            ConnectionConfig connection = mapper.entityToModel(entity);
            connection.setStatus(CheckState.of(ConnectionStatus.TESTING));
            if (withEnvironment) {
                Environment environment = id2Environment.getOrDefault(connection.getEnvironmentId(), null);
                if (Objects.isNull(environment)) {
                    throw new UnexpectedException("environment not found, id=" + connection.getEnvironmentId());
                }
                connection.setEnvironmentStyle(environment.getStyle());
                connection.setEnvironmentName(environment.getName());
            }
            if (withProject && connection.getProjectId() != null) {
                String projectName = id2ProjectName.getOrDefault(connection.getProjectId(), null);
                if (Objects.isNull(projectName)) {
                    throw new UnexpectedException("project not found, id=" + connection.getProjectId());
                }
                connection.setProjectName(projectName);
            }
            return connection;
        }).collect(Collectors.toList());
    }

    private ConnectionConfig entityToModel(@NonNull ConnectionEntity entity, @NonNull Boolean withEnvironment,
            @NonNull Boolean withProject) {
        return entitiesToModels(Collections.singletonList(entity), entity.getOrganizationId(), withEnvironment,
                withProject).stream().findFirst()
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_CONNECTION, "id", entity.getId()));
    }

    private ConnectionEntity modelToEntity(@NonNull ConnectionConfig model) {
        return mapper.modelToEntity(model);
    }

    private List<ConnectionAttributeEntity> connToAttrEntities(@NonNull ConnectionConfig model) {
        Verify.notNull(model.getId(), "ConnectionId");
        Map<String, Object> attributes = model.getAttributes();
        if (attributes == null || attributes.size() == 0) {
            return Collections.emptyList();
        }
        return attributes.entrySet().stream().map(entry -> {
            ConnectionAttributeEntity entity = new ConnectionAttributeEntity();
            entity.setConnectionId(model.getId());
            entity.setName(entry.getKey());
            entity.setContent(JsonUtils.toJson(entry.getValue()));
            return entity;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> attrEntitiesToMap(@NonNull List<ConnectionAttributeEntity> entities) {
        Map<Long, List<ConnectionAttributeEntity>> map = entities.stream().collect(
                Collectors.groupingBy(ConnectionAttributeEntity::getConnectionId));
        Verify.verify(map.size() <= 1, "Attributes's size is illegal, actual: " + map.size());
        return entities.stream().filter(e -> JsonUtils.fromJson(e.getContent(), Object.class) != null)
                .collect(Collectors.toMap(ConnectionAttributeEntity::getName, e -> JsonUtils.fromJson(
                        e.getContent(), Object.class)));
    }

    private Long currentOrganizationId() {
        return authenticationFacade.currentOrganizationId();
    }

    private Long currentUserId() {
        return authenticationFacade.currentUserId();
    }

    private void maskConnectionHost(ConnectionConfig connection) {
        if (Objects.isNull(connection)) {
            return;
        }
        Map<String, String> properties = connection.getProperties();
        if (Objects.nonNull(properties) && StringUtils.equals(properties.get("maskHost"), "true")) {
            connection.setHost("trial_connection_host");
        }
    }

    private Date getEarliestObjectSyncTime(@NotNull Long connectionId) {
        List<DatabaseEntity> entities = databaseRepository.findByConnectionIdAndExisted(connectionId, true);
        if (CollectionUtils.isEmpty(entities)) {
            return null;
        }
        Set<Date> syncTimes = entities.stream().map(DatabaseEntity::getObjectLastSyncTime).collect(Collectors.toSet());
        if (syncTimes.contains(null)) {
            return null;
        }
        return syncTimes.stream().min(Date::compareTo).orElse(null);
    }

}
