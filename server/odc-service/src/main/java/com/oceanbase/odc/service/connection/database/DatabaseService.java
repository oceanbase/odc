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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
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
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.DeleteDatabasesReq;
import com.oceanbase.odc.service.connection.database.model.QueryDatabaseParams;
import com.oceanbase.odc.service.connection.database.model.TransferDatabasesReq;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.DBIdentitiesService;
import com.oceanbase.odc.service.db.DBSchemaService;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.onlineschemachange.rename.OscDBUserUtil;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

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
    private DatabaseMapper databaseMapper = DatabaseMapper.INSTANCE;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private AuthorizationFacade authorizationFacade;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DBSchemaService dbSchemaService;

    @Autowired
    private DBIdentitiesService dbIdentitiesService;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrganizationService organizationService;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public Database detail(@NonNull Long id) {
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
        Page<Database> databases = entitiesToModels(entities);

        Map<String, Pair<ConnectionSession, Boolean>> connectionId2LockUserRequired = new HashMap<>();
        databases.forEach(d -> {
            if (Objects.equals(TaskType.ONLINE_SCHEMA_CHANGE.name(), params.getType()) && d.getDataSource() != null) {
                Pair<ConnectionSession, Boolean> pair =
                        getConnectionSessionLockUserRequiredPair(connectionId2LockUserRequired, d);
                d.setLockDatabaseUserRequired(pair.right);
            }

        });
        connectionId2LockUserRequired.forEach((k, v) -> {
            if (v != null) {
                try {
                    v.left.expire();
                } catch (Exception ex) {
                    // ignore exception
                }
            }
        });
        return databases;
    }


    @SkipAuthorize("internal authenticated")
    public List<ConnectionConfig> statsConnectionConfig() {
        QueryDatabaseParams params = QueryDatabaseParams.builder().build();
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return connectionService.listByOrganizationId(authenticationFacade.currentOrganizationId());
        }
        Page<Database> databases = list(params, Pageable.unpaged());
        if (CollectionUtils.isEmpty(databases.getContent())) {
            return Collections.EMPTY_LIST;
        }
        return databases.stream().filter(database -> Objects.nonNull(database.getDataSource()))
                .map(database -> {
                    ConnectionConfig connection = database.getDataSource();
                    Environment environment = database.getEnvironment();
                    if (environment.getName().startsWith("${") && environment.getName().endsWith("}")) {
                        connection
                                .setEnvironmentName(I18n.translate(
                                        environment.getName().substring(2, environment.getName().length() - 1), null,
                                        LocaleContextHolder.getLocale()));
                    } else {
                        connection.setEnvironmentName(environment.getName());
                    }
                    connection.setEnvironmentStyle(database.getEnvironment().getStyle());
                    return connection;
                })
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ConnectionConfig::getId))),
                        ArrayList::new));
    }

    @SkipAuthorize("internal authenticated")
    public Database create(@NonNull CreateDatabaseReq req) {
        if (!projectService.checkPermission(req.getProjectId(), ResourceRoleName.all())
                || !connectionService.checkPermission(req.getDataSourceId(), Arrays.asList("update"))) {
            throw new AccessDeniedException();
        }
        ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(req.getDataSourceId());
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connection);
        ConnectionSession session = factory.generateSession();
        try {
            DBDatabase dBdatabase = new DBDatabase();
            dBdatabase.setName(req.getName());
            dBdatabase.setCharset(req.getCharsetName());
            dBdatabase.setCollation(req.getCollationName());
            session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                    .execute((ConnectionCallback<Void>) con -> {
                        SchemaPluginUtil.getDatabaseExtension(connection.getDialectType()).create(con, dBdatabase,
                                connection.getPassword());
                        return null;
                    });
            DBDatabase dbDatabase = dbSchemaService.detail(session, req.getName());
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
            session.expire();
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

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public void updateEnvironmentByDataSourceId(@NonNull Long dataSourceId, @NonNull Long environmentId) {
        List<DatabaseEntity> databases = databaseRepository.findByConnectionId(dataSourceId);
        databases.stream().forEach(database -> database.setEnvironmentId(environmentId));
        databaseRepository.saveAll(databases);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public boolean transfer(@NonNull TransferDatabasesReq req) {
        if (!projectService.checkPermission(req.getProjectId(),
                Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER))) {
            throw new AccessDeniedException();
        }
        List<DatabaseEntity> entities = databaseRepository.findAllById(req.getDatabaseIds());
        List<DatabaseEntity> transferred = entities.stream().map(database -> {
            /**
             * current user should be source project's OWNER/DBA, and should have update permission on this
             * DataSource
             */
            if (!projectService.checkPermission(database.getProjectId(),
                    Arrays.asList(ResourceRoleName.DBA, ResourceRoleName.OWNER))
                    || !connectionService.checkPermission(database.getConnectionId(), Arrays.asList("update"))) {
                throw new AccessDeniedException();
            }
            database.setProjectId(req.getProjectId());
            return database;
        }).collect(Collectors.toList());
        databaseRepository.saveAll(transferred);
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
        saved.stream().forEach(database -> checkPermission(database.getProjectId(), database.getConnectionId()));
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
        Lock lock = jdbcLockRegistry.obtain(getLockKey(dataSourceId));
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
        horizontalDataPermissionValidator.checkCurrentOrganization(connection);
        try {
            organizationService.get(connection.getOrganizationId()).ifPresent(organization -> {
                if (organization.getType() == OrganizationType.INDIVIDUAL) {
                    syncIndividualDataSources(connection);
                } else {
                    syncTeamDataSources(connection);
                }
            });
            return true;
        } catch (Exception ex) {
            log.info("sync database failed, dataSourceId={}, error message={}", dataSourceId,
                    ex.getLocalizedMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void syncTeamDataSources(ConnectionConfig connection) {
        ConnectionSession connectionSession = null;
        try {
            DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connection);
            connectionSession = factory.generateSession();
            List<DatabaseEntity> latestDatabases =
                    dbSchemaService.listDatabases(connectionSession).stream().map(database -> {
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
                        entity.setProjectId(null);
                        return entity;
                    }).collect(Collectors.toList());

            Map<String, List<DatabaseEntity>> latestDatabaseName2Database =
                    latestDatabases.stream().filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(DatabaseEntity::getName));
            List<DatabaseEntity> existedDatabasesInDb =
                    databaseRepository.findByConnectionId(connection.getId()).stream()
                            .filter(database -> database.getExisted()).collect(
                                    Collectors.toList());
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
                    })
                    .collect(Collectors.toList());

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(toAdd)) {
                jdbcTemplate.batchUpdate(
                        "insert into connect_database(database_id, organization_id, name, project_id, connection_id, environment_id, sync_status, charset_name, collation_name, table_count, is_existed) values(?,?,?,?,?,?,?,?,?,?,?)",
                        toAdd);
            }
            List<Object[]> toDelete =
                    existedDatabasesInDb.stream()
                            .filter(database -> !latestDatabaseNames.contains(database.getName()))
                            .map(database -> new Object[] {database.getId()})
                            .collect(Collectors.toList());
            /**
             * just set existed to false if the database has been dropped instead of deleting it directly
             */
            if (!CollectionUtils.isEmpty(toDelete)) {
                String deleteSql =
                        "update connect_database set is_existed = 0 where id = ?";
                jdbcTemplate.batchUpdate(deleteSql, toDelete);
            }

            List<Object[]> toUpdate = existedDatabasesInDb.stream()
                    .filter(database -> latestDatabaseNames.contains(database.getName()))
                    .map(database -> {
                        DatabaseEntity latest = latestDatabaseName2Database.get(database.getName()).get(0);
                        return new Object[] {latest.getTableCount(), latest.getCollationName(), latest.getCharsetName(),
                                database.getId()};
                    })
                    .collect(Collectors.toList());

            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(toUpdate)) {
                String update =
                        "update connect_database set table_count=?, collation_name=?, charset_name=? where id = ?";
                jdbcTemplate.batchUpdate(update, toUpdate);
            }
        } finally {
            if (Objects.nonNull(connectionSession)) {
                connectionSession.expire();
            }
        }
    }

    private void syncIndividualDataSources(ConnectionConfig connection) {
        ConnectionSession connectionSession = null;
        try {
            DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connection);
            connectionSession = factory.generateSession();
            Set<String> latestDatabaseNames = dbSchemaService.showDatabases(connectionSession).stream().collect(
                    Collectors.toSet());
            List<DatabaseEntity> existedDatabasesInDb =
                    databaseRepository.findByConnectionId(connection.getId()).stream()
                            .filter(database -> database.getExisted()).collect(
                                    Collectors.toList());
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
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(toAdd)) {
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
        } finally {
            if (Objects.nonNull(connectionSession)) {
                connectionSession.expire();
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
    public List<String> listUsers(Long dataSourceId) {
        ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
        horizontalDataPermissionValidator.checkCurrentOrganization(connection);
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connection);
        ConnectionSession connSession = factory.generateSession();
        try {
            DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connSession);
            List<DBObjectIdentity> dbUsers = dbSchemaAccessor.listUsers();
            List<String> whiteUsers = OscDBUserUtil.getLockUserWhiteList(connection);
            return dbUsers.stream().map(DBObjectIdentity::getName)
                    .filter(whiteUsers::contains).collect(Collectors.toList());
        } finally {
            connSession.expire();
        }
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

    private Page<Database> entitiesToModels(Page<DatabaseEntity> entities) {
        if (CollectionUtils.isEmpty(entities.getContent())) {
            return Page.empty();
        }
        Map<Long, List<Project>> projectId2Projects = projectService.mapByIdIn(entities.stream()
                .map(DatabaseEntity::getProjectId).collect(Collectors.toSet()));
        Map<Long, List<Environment>> environmentId2Environments = environmentService.mapByIdIn(entities.stream()
                .map(DatabaseEntity::getEnvironmentId).collect(Collectors.toSet()));
        Map<Long, List<ConnectionConfig>> connectionId2Connections = connectionService.mapByIdIn(entities.stream()
                .map(DatabaseEntity::getConnectionId).collect(Collectors.toSet()));

        return entities.map(entity -> {
            Database database = databaseMapper.entityToModel(entity);
            List<Project> projects = projectId2Projects.getOrDefault(entity.getProjectId(), new ArrayList<>());
            List<Environment> environments =
                    environmentId2Environments.getOrDefault(entity.getEnvironmentId(), new ArrayList<>());
            List<ConnectionConfig> connections =
                    connectionId2Connections.getOrDefault(entity.getConnectionId(), new ArrayList<>());
            database.setProject(CollectionUtils.isEmpty(projects) ? null : projects.get(0));
            database.setEnvironment(CollectionUtils.isEmpty(environments) ? null : environments.get(0));
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
        model.setEnvironment(environmentService.detailSkipPermissionCheck(entity.getEnvironmentId()));
        return model;
    }

    private String getLockKey(@NonNull Long connectionId) {
        return "DataSource_" + connectionId;
    }

    private Pair<ConnectionSession, Boolean> getConnectionSessionLockUserRequiredPair(
            Map<String, Pair<ConnectionSession, Boolean>> connectionId2LockUserRequired, Database d) {
        return connectionId2LockUserRequired.computeIfAbsent(d.getDataSource().getId() + "", k -> {
            ConnectionConfig connConfig =
                connectionService.getForConnectionSkipPermissionCheck(d.getDataSource().getId());
            DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connConfig);
            ConnectionSession connSession = factory.generateSession();
            return new Pair<>(connSession, OscDBUserUtil.isLockUserRequired(connSession.getDialectType(),
                    ConnectionSessionUtil.getVersion(connSession)));
        });
    }
}
