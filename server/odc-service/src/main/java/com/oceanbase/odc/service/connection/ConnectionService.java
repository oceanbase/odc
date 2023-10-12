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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.i18n.I18n;
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
import com.oceanbase.odc.core.shared.constant.ConnectionStatus;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionSpecs;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
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
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.PermissionService;
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
    private EncryptionFacade encryptionFacade;

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
    private ConnectionEnvironmentAdapter environmentAdapter;

    @Autowired
    private ConnectionSSLAdaptor connectionSSLAdaptor;

    @Autowired
    private ConnectionValidator connectionValidator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private EnvironmentRepository environmentRepository;

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
    private PlatformTransactionManager transactionManager;


    private final ConnectionMapper mapper = ConnectionMapper.INSTANCE;

    public static final String DEFAULT_MIN_PRIVILEGE = "read";


    @PreAuthenticate(actions = "create", resourceType = "ODC_CONNECTION", isForAll = true)
    public ConnectionConfig create(@NotNull @Valid ConnectionConfig connection) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
        ConnectionConfig saved;
        try {
            saved = innerCreate(connection);
            userPermissionService.bindUserAndDataSourcePermission(currentUserId(), currentOrganizationId(),
                    saved.getId(),
                    Arrays.asList("read", "update", "delete"));
            transactionManager.commit(transactionStatus);
        } catch (Exception e) {
            transactionManager.rollback(transactionStatus);
            throw e;
        }
        databaseSyncManager.submitSyncDataSourceTask(saved);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_CONNECTION", isForAll = true)
    public List<ConnectionConfig> batchCreate(@NotEmpty @Valid List<ConnectionConfig> connections) {
        List<ConnectionConfig> connectionConfigs = new ArrayList<>();
        for (ConnectionConfig connection : connections) {
            ConnectionConfig saved = innerCreate(connection);
            databaseSyncManager.submitSyncDataSourceTask(saved);
            userPermissionService.bindUserAndDataSourcePermission(currentUserId(), currentOrganizationId(),
                    saved.getId(),
                    Arrays.asList("read", "update", "delete"));
            connectionConfigs.add(saved);
        }
        return connectionConfigs;
    }

    @SkipAuthorize("odc internal usage")
    public ConnectionConfig innerCreate(@NotNull @Valid ConnectionConfig connection) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
        ConnectionConfig created;
        try {

            environmentAdapter.adaptConfig(connection);
            connectionSSLAdaptor.adapt(connection);

            connectionValidator.validateForUpsert(connection);
            connectionValidator.validatePrivateConnectionTempOnly(connection.getTemp());

            connection.setOrganizationId(currentOrganizationId());
            connection.setCreatorId(currentUserId());
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
            }
            connectionEncryption.encryptPasswords(connection);

            ConnectionEntity entity = modelToEntity(connection);
            ConnectionEntity savedEntity = repository.saveAndFlush(entity);
            created = entityToModel(savedEntity, true);
        } catch (Exception ex) {
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        log.info("Connection created, connection={}", created);
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig delete(@NotNull Long id) {
        ConnectionConfig connection = internalGet(id);
        repository.deleteById(id);
        permissionService.deleteResourceRelatedPermissions(id, ResourceType.ODC_CONNECTION,
                PermissionType.PUBLIC_RESOURCE);
        log.info("Delete datasource-related permission entity, id={}", id);
        int affectRows = databaseService.deleteByDataSourceId(id);
        log.info("delete datasource-related databases successfully, affectRows={}, id={}", affectRows, id);
        return connection;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public List<ConnectionConfig> delete(@NotNull Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ConnectionConfig> connections = this.repository
                .findAll(ConnectionSpecs.idIn(ids)).stream().map(connection -> entityToModel(connection, false))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(connections)) {
            return Collections.emptyList();
        }
        List<Permission> permissions = connections.stream()
                .map(c -> securityManager.getPermissionByActions(c, Collections.singleton("delete")))
                .collect(Collectors.toList());
        this.securityManager.checkPermission(permissions);

        List<PermissionEntity> permissionEntities = this.permissionService.deleteResourceRelatedPermissions(
                ids, ResourceType.ODC_CONNECTION);
        log.info("Delete datasource-related permission entity, affectEntities={}", permissionEntities.size());
        int affectRows = databaseService.deleteByDataSourceIds(ids);
        log.info("delete datasource-related databases successfully, affectRows={}", affectRows);
        affectRows = repository.deleteByIds(ids);
        log.info("delete datasources successfully, affectRows={}", affectRows);
        return connections;
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig detail(@NotNull Long id) {
        return getWithoutPermissionCheck(id);
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
        return repository.findByOrganizationId(organizationId).stream().map(ds -> entityToModel(ds, true))
                .collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public List<ConnectionConfig> listByOrganizationIdWithoutEnvironment(@NonNull Long organizationId) {
        return repository.findByOrganizationId(organizationId).stream().map(ds -> entityToModel(ds, false))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, DEVELOPER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public PaginatedData<ConnectionConfig> listByProjectId(@NotNull Long projectId, @NotNull Boolean basic) {
        List<ConnectionConfig> connections;
        if (basic) {
            connections = repository.findByProjectId(projectId).stream().map(e -> {
                ConnectionConfig c = new ConnectionConfig();
                c.setId(e.getId());
                c.setName(e.getName());
                return c;
            }).collect(Collectors.toList());
        } else {
            connections = repository.findByProjectId(projectId).stream().map(mapper::entityToModel)
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
        Map<Long, ConnectionConfig> connMap = repository.findAll(spec).stream()
                .map(connection -> entityToModel(connection, false))
                .collect(Collectors.toMap(ConnectionConfig::getId, c -> c));
        Map<SecurityResource, Set<String>> res2Actions = authorizationFacade.getRelatedResourcesAndActions(user)
                .entrySet().stream().collect(Collectors.toMap(e -> {
                    SecurityResource s = e.getKey();
                    return new DefaultSecurityResource(s.resourceId(), s.resourceType());
                }, Entry::getValue));
        List<Permission> granted = res2Actions.entrySet().stream()
                .map(e -> securityManager.getPermissionByActions(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        Map<Long, CheckState> connId2State = new HashMap<>();
        for (Long connId : ids) {
            ConnectionConfig conn = connMap.get(connId);
            if (conn == null) {
                // 没有找到对应的连接，可能是非法的 connId，保险起见将状态设置为 unknown
                connId2State.put(connId, CheckState.of(ConnectionStatus.UNKNOWN));
                continue;
            }
            boolean grant;
            String connIdStr = connId + "";
            Permission p = new ConnectionPermission(connIdStr, ResourcePermission.READ);
            grant = granted.stream().anyMatch(g -> g.implies(p));
            SecurityResource allKey = new DefaultSecurityResource("ODC_CONNECTION");
            Set<String> actions = res2Actions.getOrDefault(allKey, new HashSet<>());
            SecurityResource thisKey = new DefaultSecurityResource(connIdStr, "ODC_CONNECTION");
            actions.addAll(res2Actions.getOrDefault(thisKey, new HashSet<>()));
            conn.setPermittedActions(actions);
            if (!grant) {
                // 不具备 connId 对应连接的读权限，此时需要将连接状态置为 unknown 避免越权
                connId2State.put(connId, CheckState.of(ConnectionStatus.UNKNOWN));
            } else {
                // 有权限，从 statusManager 中获取
                connId2State.put(connId, this.statusManager.getAndRefreshStatus(conn));
            }
        }
        return connId2State;
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
        return entities.stream().map(connectionEntity -> entityToModel(connectionEntity, false))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public Set<Long> findIdsByHost(String host) {
        return repository.findIdsByHost(host);
    }


    @SkipAuthorize("internal usage")
    public List<ConnectionConfig> listAllConnections() {
        return repository.findAll().stream().map(mapper::entityToModel).collect(Collectors.toList());
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

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public ConnectionConfig update(@NotNull Long id, @NotNull @Valid ConnectionConfig connection) {
        environmentAdapter.adaptConfig(connection);
        connectionSSLAdaptor.adapt(connection);

        ConnectionConfig savedConnectionConfig = internalGet(id);

        connectionValidator.validateForUpdate(connection, savedConnectionConfig);

        if (StringUtils.isBlank(connection.getSysTenantUsername())) {
            // sys 用户没有设的情况下，相应地，密码要设置为空
            connection.setSysTenantPassword("");
        }
        connection.setId(id);
        connection.setCreatorId(savedConnectionConfig.getCreatorId());
        connection.setOrganizationId(savedConnectionConfig.getOrganizationId());
        connection.setType(savedConnectionConfig.getType());
        connection.setCipher(savedConnectionConfig.getCipher());
        connection.setSalt(savedConnectionConfig.getSalt());
        connection.setTemp(savedConnectionConfig.getTemp());
        connection.setPasswordEncrypted(null);
        connection.setSysTenantPasswordEncrypted(null);

        connectionValidator.validateForUpsert(connection);

        // validate same name while rename connection
        repository.findByOrganizationIdAndName(connection.getOrganizationId(), connection.getName())
                .ifPresent(sameNameEntity -> {
                    if (!id.equals(sameNameEntity.getId())) {
                        throw new BadRequestException(ErrorCodes.ConnectionDuplicatedName,
                                new Object[] {connection.getName()}, "same datasource name exists");
                    }
                });

        connectionEncryption.encryptPasswords(connection);
        connection.fillEncryptedPasswordFromSavedIfNull(savedConnectionConfig);

        ConnectionEntity entity = modelToEntity(connection);
        ConnectionEntity savedEntity = repository.saveAndFlush(entity);

        if (ObjectUtils.notEqual(connection.getEnvironmentId(), savedConnectionConfig.getEnvironmentId())) {
            databaseService.updateEnvironmentByDataSourceId(savedEntity.getId(), savedEntity.getEnvironmentId());
        }

        // for workaround createTime/updateTime not refresh in server mode,
        // seems JPA bug, it works while UT
        entityManager.refresh(savedEntity);

        ConnectionConfig updated = entityToModel(savedEntity, true);
        databaseSyncManager.submitSyncDataSourceTask(updated);
        log.info("Connection updated, connection={}", updated);
        return updated;
    }

    @SkipAuthorize("internal usage")
    public Map<Long, List<ConnectionConfig>> mapByIdIn(Set<Long> ids) {
        if (org.springframework.util.CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return repository.findAllById(ids).stream().map(mapper::entityToModel)
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

    @SkipAuthorize("odc internal usage")
    public boolean existsById(@NotNull Long id) {
        return repository.existsById(id);
    }

    @SkipAuthorize("internal usage")
    public ConnectionConfig getForConnectionSkipPermissionCheck(@NotNull Long id) {
        ConnectionConfig connection = internalGetSkipUserCheck(id, false);

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
        try {
            ConnectionConfig connection = internalGetSkipUserCheck(connectionId, false);
            permissionValidator.checkCurrentOrganization(connection);
            securityManager.checkPermission(
                    securityManager.getPermissionByActions(connection, actions));
        } catch (Exception ex) {
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
        spec = spec.and(ConnectionSpecs.sort(pageable.getSort()));
        Pageable page = pageable.equals(Pageable.unpaged()) ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return this.repository.findAll(spec, page).map(connection -> entityToModel(connection, true));
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
        return repository.exists(Example.of(entity));
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
        ConnectionConfig connection = internalGetSkipUserCheck(id, true);
        permissionValidator.checkCurrentOrganization(connection);
        return connection;
    }

    private ConnectionConfig internalGetSkipUserCheck(Long id, boolean withEnvironment) {
        return entityToModel(getEntity(id), withEnvironment);
    }

    private ConnectionEntity getEntity(@NonNull Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(ResourceType.ODC_CONNECTION, "id", id));
    }

    private ConnectionConfig entityToModel(@NonNull ConnectionEntity entity, @NonNull Boolean withEnvironment) {
        ConnectionConfig connection = mapper.entityToModel(entity);
        connection.setStatus(CheckState.of(ConnectionStatus.TESTING));
        if (withEnvironment) {
            Environment environment = environmentService.detailSkipPermissionCheck(entity.getEnvironmentId());
            connection.setEnvironmentStyle(environment.getStyle());
            String environmentName = environment.getName();
            if (environmentName.startsWith("${") && environmentName.endsWith("}")) {
                connection
                        .setEnvironmentName(
                                I18n.translate(environmentName.substring(2, environmentName.length() - 1), null,
                                        LocaleContextHolder.getLocale()));
            } else {
                connection.setEnvironmentName(environmentName);
            }
        }
        return connection;
    }

    private ConnectionEntity modelToEntity(@NonNull ConnectionConfig model) {
        return mapper.modelToEntity(model);
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
}
