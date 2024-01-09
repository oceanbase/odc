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
package com.oceanbase.odc.service.connection.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.DatabaseSpecs;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.collaboration.project.model.QueryProjectParams;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.model.CreateDatabaseReq;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncProperties;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.DatabaseUser;
import com.oceanbase.odc.service.connection.database.model.DeleteDatabasesReq;
import com.oceanbase.odc.service.connection.database.model.QueryDatabaseParams;
import com.oceanbase.odc.service.connection.database.model.TransferDatabasesReq;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.DBSchemaService;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.onlineschemachange.ddl.DBUser;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscDBAccessor;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscDBAccessorFactory;
import com.oceanbase.odc.service.onlineschemachange.rename.OscDBUserUtil;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 14:34
 * @Description: []
 */
@Service
@Slf4j
@Validated
@Authenticated
public class DatabaseService {

    private final DatabaseMapper databaseMapper = DatabaseMapper.INSTANCE;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DBSchemaService dbSchemaService;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private DatabaseSyncProperties databaseSyncProperties;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public Database detail(@NonNull Long id) {
        return getDatabase(id);
    }

    private Database getDatabase(Long id) {
        Database database = entityToModel(databaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_DATABASE, "id", id)));
        if (!projectService.checkPermission(database.getProject().getId(),
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER, ResourceRoleName.DEVELOPER))) {
            throw new AccessDeniedException();
        }
        return database;
    }

    @SkipAuthorize("odc internal usage")
    @Transactional(rollbackFor = Exception.class)
    public ConnectionConfig findDataSourceForConnectById(@NonNull Long id) {
        DatabaseEntity database = databaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_DATABASE, "id", id));
        return connectionService.getForConnectionSkipPermissionCheck(database.getConnectionId());
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public Page<Database> listDatabasesByDataSource(@NonNull Long id, String name, @NonNull Pageable pageable) {
        Specification<DatabaseEntity> specs = DatabaseSpecs
                .connectionIdEquals(id)
                .and(DatabaseSpecs.nameLike(name));
        Page<DatabaseEntity> entities = databaseRepository.findAll(specs, pageable);
        Page<Database> databases = entitiesToModels(entities);
        horizontalDataPermissionValidator.checkCurrentOrganization(databases.getContent());
        return databases;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public Page<Database> list(@NonNull QueryDatabaseParams params, @NotNull Pageable pageable) {
        // TODO: Databases include permitted actions
        if (Objects.nonNull(params.getDataSourceId())
                && authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            try {
                internalSyncDataSourceSchemas(params.getDataSourceId());
            } catch (Exception ex) {
                log.warn("sync data sources in individual space failed when listing databases, errorMessage={}",
                        ex.getLocalizedMessage());
            }
            params.setContainsUnassigned(true);
        }
        Specification<DatabaseEntity> specs = DatabaseSpecs
                .environmentIdEquals(params.getEnvironmentId())
                .and(DatabaseSpecs.nameLike(params.getSchemaName()))
                .and(DatabaseSpecs.existedEquals(params.getExisted()))
                .and(DatabaseSpecs.organizationIdEquals(authenticationFacade.currentOrganizationId()));
        Set<Long> joinedProjectIds =
                projectService
                        .list(QueryProjectParams.builder().build(), Pageable.unpaged())
                        .getContent().stream()
                        .filter(Objects::nonNull).map(Project::getId).collect(Collectors.toSet());
        /**
         * not joined any projects and does not show unassigned databases
         */
        if (joinedProjectIds.isEmpty()
                && (Objects.isNull(params.getContainsUnassigned()) || !params.getContainsUnassigned())) {
            return Page.empty();
        }

        if (Objects.isNull(params.getProjectId())) {
            Specification<DatabaseEntity> projectSpecs = DatabaseSpecs.projectIdIn(joinedProjectIds);
            if (Objects.nonNull(params.getContainsUnassigned()) && params.getContainsUnassigned()) {
                projectSpecs = projectSpecs.or(DatabaseSpecs.projectIdIsNull());
            }
            specs = specs.and(projectSpecs);
        } else {
            if (!joinedProjectIds.contains(params.getProjectId())) {
                throw new AccessDeniedException();
            }
            specs = specs.and(DatabaseSpecs.projectIdEquals(params.getProjectId()));
        }

        if (Objects.nonNull(params.getDataSourceId())) {
            specs = specs.and(DatabaseSpecs.connectionIdEquals(params.getDataSourceId()));
        }
        Page<DatabaseEntity> entities = databaseRepository.findAll(specs, pageable);
        return entitiesToModels(entities);
    }

    @SkipAuthorize("internal authenticated")
    public List<ConnectionConfig> statsConnectionConfig() {
        QueryDatabaseParams params = QueryDatabaseParams.builder().build();
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return connectionService.listByOrganizationId(authenticationFacade.currentOrganizationId());
        }
        Page<Database> databases = list(params, Pageable.unpaged());
        if (CollectionUtils.isEmpty(databases.getContent())) {
            return Collections.emptyList();
        }
        return databases.stream().filter(database -> Objects.nonNull(database.getDataSource()))
                .map(Database::getDataSource)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ConnectionConfig::getId))),
                        ArrayList::new));
    }

    @SkipAuthorize("internal authenticated")
    public Database create(@NonNull CreateDatabaseReq req) {
        ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(req.getDataSourceId());
        if ((connection.getProjectId() != null && !connection.getProjectId().equals(req.getProjectId()))
                || !projectService.checkPermission(req.getProjectId(),
                        Arrays.asList(ResourceRoleName.OWNER, ResourceRoleName.DBA, ResourceRoleName.DEVELOPER))
                || !connectionService.checkPermission(req.getDataSourceId(), Collections.singletonList("update"))) {
            throw new AccessDeniedException();
        }
        DataSource dataSource = new OBConsoleDataSourceFactory(connection, true, false).getDataSource();
        try (Connection conn = dataSource.getConnection()) {
            createDatabase(req, conn, connection);
            DBDatabase dbDatabase = dbSchemaService.detail(connection.getDialectType(), conn, req.getName());
            DatabaseEntity database = new DatabaseEntity();
            database.setDatabaseId(dbDatabase.getId());
            database.setExisted(Boolean.TRUE);
            database.setName(dbDatabase.getName());
            database.setCharsetName(dbDatabase.getCharset());
            database.setCollationName(dbDatabase.getCollation());
            database.setConnectionId(req.getDataSourceId());
            database.setProjectId(req.getProjectId());
            database.setEnvironmentId(connection.getEnvironmentId());
            database.setSyncStatus(DatabaseSyncStatus.SUCCEEDED);
            database.setOrganizationId(authenticationFacade.currentOrganizationId());
            database.setLastSyncTime(new Date(System.currentTimeMillis()));
            DatabaseEntity saved = databaseRepository.saveAndFlush(database);
            return entityToModel(saved);
        } catch (Exception ex) {
            throw new BadRequestException(SqlExecuteResult.getTrackMessage(ex));
        } finally {
            if (dataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) dataSource).close();
                } catch (Exception e) {
                    log.warn("Failed to close datasource", e);
                }
            }
        }
    }

    @SkipAuthorize("internal usage")
    public Set<Long> listDatabaseIdsByProjectId(@NonNull Long projectId) {
        return databaseRepository.findByProjectId(projectId).stream().map(DatabaseEntity::getId)
                .collect(Collectors.toSet());
    }

    @SkipAuthorize("internal usage")
    public Set<Long> listDatabaseIdsByConnectionIds(@NotEmpty Collection<Long> connectionIds) {
        return databaseRepository.findByConnectionIdIn(connectionIds).stream().map(DatabaseEntity::getId)
                .collect(Collectors.toSet());
    }

    @SkipAuthorize("internal usage")
    public List<Database> listDatabasesByIds(@NotEmpty Collection<Long> ids) {
        return databaseRepository.findByIdIn(ids).stream().map(databaseMapper::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("internal usage")
    public List<Database> listDatabasesByConnectionIds(@NotEmpty Collection<Long> connectionIds) {
        return databaseRepository.findByConnectionIdIn(connectionIds).stream().map(databaseMapper::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("internal usage")
    public Set<Database> listDatabaseByNames(@NotEmpty Collection<String> names) {
        return databaseRepository.findByNameIn(names).stream().map(databaseMapper::entityToModel)
                .collect(Collectors.toSet());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public boolean transfer(@NonNull @Valid TransferDatabasesReq req) {
        List<DatabaseEntity> entities = databaseRepository.findAllById(req.getDatabaseIds());
        checkTransferable(entities, req.getProjectId());
        databaseRepository.setProjectIdByIdIn(req.getProjectId(), entities.stream().map(DatabaseEntity::getId)
                .collect(Collectors.toSet()));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public boolean deleteDatabases(@NonNull DeleteDatabasesReq req) {
        if (CollectionUtils.isEmpty(req.getDatabaseIds())) {
            return true;
        }
        List<DatabaseEntity> saved = databaseRepository.findByIdIn(req.getDatabaseIds()).stream()
                .filter(database -> !database.getExisted())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(saved)) {
            return false;
        }
        saved.forEach(database -> checkPermission(database.getProjectId(), database.getConnectionId()));
        databaseRepository.deleteAll(saved);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
    public Boolean syncDataSourceSchemas(@NonNull Long dataSourceId) throws InterruptedException {
        return internalSyncDataSourceSchemas(dataSourceId);
    }

    @SkipAuthorize("internal usage")
    public Boolean internalSyncDataSourceSchemas(@NonNull Long dataSourceId) throws InterruptedException {
        Lock lock = jdbcLockRegistry.obtain(connectionService.getUpdateDsSchemaLockKey(dataSourceId));
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceSynchronizing,
                    new Object[] {ResourceType.ODC_DATABASE.getLocalizedMessage()}, "Can not acquire jdbc lock");
        }
        try {
            ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
            horizontalDataPermissionValidator.checkCurrentOrganization(connection);
            organizationService.get(connection.getOrganizationId()).ifPresent(organization -> {
                if (organization.getType() == OrganizationType.INDIVIDUAL) {
                    syncIndividualDataSources(connection);
                } else {
                    syncTeamDataSources(connection);
                }
            });
            return true;
        } catch (Exception ex) {
            log.warn("Sync database failed, dataSourceId={}, errorMessage={}", dataSourceId, ex.getLocalizedMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void syncTeamDataSources(ConnectionConfig connection) {
        Long currentProjectId = connection.getProjectId();
        List<String> blockedDatabaseNames = listBlockedDatabaseNames(connection.getDialectType());
        DataSource teamDataSource = new OBConsoleDataSourceFactory(connection, true, false).getDataSource();
        try (Connection conn = teamDataSource.getConnection()) {
            List<DatabaseEntity> latestDatabases = dbSchemaService.listDatabases(connection.getDialectType(), conn)
                    .stream().map(database -> {
                        DatabaseEntity entity = new DatabaseEntity();
                        entity.setDatabaseId(com.oceanbase.odc.common.util.StringUtils.uuid());
                        entity.setExisted(Boolean.TRUE);
                        entity.setName(database.getName());
                        entity.setCharsetName(database.getCharset());
                        entity.setCollationName(database.getCollation());
                        entity.setTableCount(0L);
                        entity.setOrganizationId(connection.getOrganizationId());
                        entity.setEnvironmentId(connection.getEnvironmentId());
                        entity.setConnectionId(connection.getId());
                        entity.setSyncStatus(DatabaseSyncStatus.SUCCEEDED);
                        entity.setProjectId(currentProjectId);
                        if (databaseSyncProperties.isBlockInternalDatabase()
                                && blockedDatabaseNames.contains(database.getName())) {
                            entity.setProjectId(null);
                        }
                        return entity;
                    }).collect(Collectors.toList());
            Map<String, List<DatabaseEntity>> latestDatabaseName2Database =
                    latestDatabases.stream().filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(DatabaseEntity::getName));
            List<DatabaseEntity> existedDatabasesInDb =
                    databaseRepository.findByConnectionId(connection.getId()).stream()
                            .filter(DatabaseEntity::getExisted).collect(Collectors.toList());
            Map<String, List<DatabaseEntity>> existedDatabaseName2Database =
                    existedDatabasesInDb.stream().collect(Collectors.groupingBy(DatabaseEntity::getName));

            Set<String> existedDatabaseNames = existedDatabaseName2Database.keySet();
            Set<String> latestDatabaseNames = latestDatabaseName2Database.keySet();
            List<Object[]> toAdd = latestDatabases.stream()
                    .filter(database -> !existedDatabaseNames.contains(database.getName()))
                    .map(database -> new Object[] {
                            database.getDatabaseId(),
                            database.getOrganizationId(),
                            database.getName(),
                            database.getProjectId(),
                            database.getConnectionId(),
                            database.getEnvironmentId(),
                            database.getSyncStatus().name(),
                            database.getCharsetName(),
                            database.getCollationName(),
                            database.getTableCount(),
                            database.getExisted()
                    }).collect(Collectors.toList());

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            if (CollectionUtils.isNotEmpty(toAdd)) {
                jdbcTemplate.batchUpdate(
                        "insert into connect_database(database_id, organization_id, name, project_id, connection_id, environment_id, sync_status, charset_name, collation_name, table_count, is_existed) values(?,?,?,?,?,?,?,?,?,?,?)",
                        toAdd);
            }
            List<Object[]> toDelete = existedDatabasesInDb.stream()
                    .filter(database -> !latestDatabaseNames.contains(database.getName()))
                    .map(database -> new Object[] {getProjectId(database, currentProjectId, blockedDatabaseNames),
                            database.getId()})
                    .collect(Collectors.toList());
            /**
             * just set existed to false if the database has been dropped instead of deleting it directly
             */
            if (!CollectionUtils.isEmpty(toDelete)) {
                String deleteSql = "update connect_database set is_existed = 0, project_id=? where id = ?";
                jdbcTemplate.batchUpdate(deleteSql, toDelete);
            }
            List<Object[]> toUpdate = existedDatabasesInDb.stream()
                    .filter(database -> latestDatabaseNames.contains(database.getName()))
                    .map(database -> {
                        DatabaseEntity latest = latestDatabaseName2Database.get(database.getName()).get(0);
                        return new Object[] {latest.getTableCount(), latest.getCollationName(), latest.getCharsetName(),
                                getProjectId(database, currentProjectId, blockedDatabaseNames), database.getId()};
                    })
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(toUpdate)) {
                String update =
                        "update connect_database set table_count=?, collation_name=?, charset_name=?, project_id=? where id = ?";
                jdbcTemplate.batchUpdate(update, toUpdate);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            if (teamDataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) teamDataSource).close();
                } catch (Exception e) {
                    log.warn("Failed to close datasource", e);
                }
            }
        }
    }

    private Long getProjectId(DatabaseEntity database, Long currentProjectId, List<String> blockedDatabaseNames) {
        Long projectId;
        if (currentProjectId != null) {
            projectId = currentProjectId;
            if (databaseSyncProperties.isBlockInternalDatabase() && blockedDatabaseNames.contains(database.getName())) {
                projectId = database.getProjectId();
            }
        } else {
            projectId = database.getProjectId();
        }
        return projectId;
    }

    private void syncIndividualDataSources(ConnectionConfig connection) {
        DataSource individualDataSource = new OBConsoleDataSourceFactory(connection, true, false).getDataSource();
        try (Connection conn = individualDataSource.getConnection()) {
            Set<String> latestDatabaseNames = dbSchemaService.showDatabases(connection.getDialectType(), conn);
            List<DatabaseEntity> existedDatabasesInDb =
                    databaseRepository.findByConnectionId(connection.getId()).stream()
                            .filter(DatabaseEntity::getExisted).collect(Collectors.toList());
            Map<String, List<DatabaseEntity>> existedDatabaseName2Database =
                    existedDatabasesInDb.stream().collect(Collectors.groupingBy(DatabaseEntity::getName));
            Set<String> existedDatabaseNames = existedDatabaseName2Database.keySet();

            List<Object[]> toAdd = latestDatabaseNames.stream()
                    .filter(latestDatabaseName -> !existedDatabaseNames.contains(latestDatabaseName))
                    .map(latestDatabaseName -> new Object[] {
                            com.oceanbase.odc.common.util.StringUtils.uuid(),
                            connection.getOrganizationId(),
                            latestDatabaseName,
                            connection.getId(),
                            connection.getEnvironmentId(),
                            DatabaseSyncStatus.SUCCEEDED.name()
                    })
                    .collect(Collectors.toList());

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            if (CollectionUtils.isNotEmpty(toAdd)) {
                jdbcTemplate.batchUpdate(
                        "insert into connect_database(database_id, organization_id, name, connection_id, environment_id, sync_status) values(?,?,?,?,?,?)",
                        toAdd);
            }

            List<Object[]> toDelete =
                    existedDatabasesInDb.stream()
                            .filter(database -> !latestDatabaseNames.contains(database.getName()))
                            .map(database -> new Object[] {database.getId()})
                            .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toDelete)) {
                jdbcTemplate.batchUpdate("delete from connect_database where id = ?", toDelete);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            if (individualDataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) individualDataSource).close();
                } catch (Exception e) {
                    log.warn("Failed to close datasource", e);
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public int deleteByDataSourceIds(@NonNull Set<Long> dataSourceId) {
        return databaseRepository.deleteByConnectionIds(dataSourceId);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public int deleteByDataSourceId(@NonNull Long dataSourceId) {
        return databaseRepository.deleteByConnectionId(dataSourceId);
    }

    @SkipAuthorize("internal authorized")
    public Set<String> filterUnAuthorizedDatabaseNames(Set<String> databaseNames, @NonNull Long dataSourceId) {
        if (CollectionUtils.isEmpty(databaseNames)) {
            return Collections.emptySet();
        }
        Page<Database> databases = list(QueryDatabaseParams.builder().containsUnassigned(false).existed(true)
                .dataSourceId(dataSourceId).build(), Pageable.unpaged());
        Set<String> authorizedDatabaseNames = databases.stream().map(Database::getName).collect(Collectors.toSet());
        return databaseNames.stream().filter(name -> !authorizedDatabaseNames.contains(name))
                .collect(Collectors.toSet());
    }

    @SkipAuthorize("internal authorized")
    public Page<DatabaseUser> listUserForOsc(Long dataSourceId) {
        ConnectionConfig config = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
        horizontalDataPermissionValidator.checkCurrentOrganization(config);
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(config);
        ConnectionSession connSession = factory.generateSession();
        try {
            OscDBAccessor dbSchemaAccessor = new OscDBAccessorFactory().generate(connSession);
            List<DBUser> dbUsers = dbSchemaAccessor.listUsers(null);
            Set<String> whiteUsers = OscDBUserUtil.getLockUserWhiteList(config);

            return new PageImpl<>(dbUsers.stream()
                    .filter(u -> !whiteUsers.contains(u.getName()))
                    .map(d -> DatabaseUser.builder().name(d.getNameWithHost()).build())
                    .collect(Collectors.toList()));
        } finally {
            connSession.expire();
        }
    }

    @SkipAuthorize("odc internal usage")
    public List<String> listBlockedDatabaseNames(DialectType dialectType) {
        List<String> names = new ArrayList<>();
        if (dialectType.isOracle()) {
            names.add("SYS");
        }
        if (dialectType.isMysql()) {
            names.addAll(Arrays.asList("mysql", "information_schema", "test"));
        }
        if (dialectType.isOBMysql()) {
            names.add("oceanbase");
        }
        return names;
    }

    private void checkPermission(Long projectId, Long dataSourceId) {
        if (Objects.isNull(projectId) && Objects.isNull(dataSourceId)) {
            throw new AccessDeniedException("invalid projectId or dataSourceId");
        }
        boolean isProjectMember = false;
        if (Objects.nonNull(projectId)) {
            isProjectMember = projectService.checkPermission(projectId, ResourceRoleName.all());
        }

        boolean canUpdateDataSource = false;
        if (Objects.nonNull(dataSourceId)) {
            canUpdateDataSource = connectionService.checkPermission(dataSourceId, Arrays.asList("update"));
        }
        if (!isProjectMember && !canUpdateDataSource) {
            throw new AccessDeniedException("invalid projectId or dataSourceId");
        }
    }

    private void checkTransferable(@NonNull Collection<DatabaseEntity> databases, Long newProjectId) {
        if (CollectionUtils.isEmpty(databases)) {
            return;
        }
        PreConditions.validArgumentState(
                projectService.checkPermission(newProjectId,
                        Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER)),
                ErrorCodes.AccessDenied, null, "Only project's OWNER/DBA can transfer databases");
        List<Long> projectIds = databases.stream().map(DatabaseEntity::getProjectId).collect(Collectors.toList());
        List<Long> connectionIds = databases.stream().map(DatabaseEntity::getConnectionId).collect(Collectors.toList());
        PreConditions.validArgumentState(
                projectService.checkPermission(projectIds, Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER)),
                ErrorCodes.AccessDenied, null, "Only project's OWNER/DBA can transfer databases");
        PreConditions.validArgumentState(
                connectionService.checkPermission(connectionIds, Collections.singletonList("update")),
                ErrorCodes.AccessDenied, null, "Lack of update permission on current datasource");
        Map<Long, ConnectionConfig> id2Conn = connectionService.innerListByIds(connectionIds).stream()
                .collect(Collectors.toMap(ConnectionConfig::getId, c -> c, (c1, c2) -> c2));
        if (databaseSyncProperties.isBlockInternalDatabase()) {
            connectionIds = databases.stream().filter(database -> {
                ConnectionConfig connection = id2Conn.get(database.getConnectionId());
                return connection != null
                        && !listBlockedDatabaseNames(connection.getDialectType()).contains(database.getName());
            }).map(DatabaseEntity::getConnectionId).collect(Collectors.toList());
        }
        connectionIds.forEach(c -> {
            ConnectionConfig connection = id2Conn.get(c);
            if (connection == null) {
                throw new NotFoundException(ResourceType.ODC_CONNECTION, "id", c);
            }
            PreConditions.validArgumentState(connection.getProjectId() == null, ErrorCodes.AccessDenied, null,
                    "Cannot transfer databases in datasource which is bound to project");
        });
    }

    private Page<Database> entitiesToModels(Page<DatabaseEntity> entities) {
        if (CollectionUtils.isEmpty(entities.getContent())) {
            return Page.empty();
        }
        Map<Long, List<Project>> projectId2Projects = projectService.mapByIdIn(entities.stream()
                .map(DatabaseEntity::getProjectId).collect(Collectors.toSet()));
        Map<Long, List<ConnectionConfig>> connectionId2Connections = connectionService.mapByIdIn(entities.stream()
                .map(DatabaseEntity::getConnectionId).collect(Collectors.toSet()));
        return entities.map(entity -> {
            Database database = databaseMapper.entityToModel(entity);
            List<Project> projects = projectId2Projects.getOrDefault(entity.getProjectId(), new ArrayList<>());
            List<ConnectionConfig> connections =
                    connectionId2Connections.getOrDefault(entity.getConnectionId(), new ArrayList<>());
            database.setProject(CollectionUtils.isEmpty(projects) ? null : projects.get(0));
            database.setEnvironment(CollectionUtils.isEmpty(connections) ? null
                    : new Environment(connections.get(0).getEnvironmentId(), connections.get(0).getEnvironmentName(),
                            connections.get(0).getEnvironmentStyle()));
            database.setDataSource(CollectionUtils.isEmpty(connections) ? null : connections.get(0));
            return database;
        });
    }

    private Database entityToModel(DatabaseEntity entity) {
        Database model = databaseMapper.entityToModel(entity);
        if (Objects.nonNull(entity.getProjectId())) {
            model.setProject(projectService.detail(entity.getProjectId()));
        }
        model.setDataSource(connectionService.getForConnectionSkipPermissionCheck(entity.getConnectionId()));
        model.setEnvironment(environmentService.detailSkipPermissionCheck(model.getDataSource().getEnvironmentId()));
        return model;
    }

    private void createDatabase(CreateDatabaseReq req, Connection conn, ConnectionConfig connection) {
        DBDatabase db = new DBDatabase();
        db.setName(req.getName());
        db.setCharset(req.getCharsetName());
        db.setCollation(req.getCollationName());
        SchemaPluginUtil.getDatabaseExtension(connection.getDialectType()).create(conn, db, connection.getPassword());
    }

}
