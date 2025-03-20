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
package com.oceanbase.odc.service.session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.session.ConnectionSessionRepository;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.DefaultConnectionSessionManager;
import com.oceanbase.odc.core.session.InMemoryConnectionSessionRepository;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManagerFactory;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.task.DefaultTaskManager;
import com.oceanbase.odc.core.task.ExecuteMonitorTaskManager;
import com.oceanbase.odc.metadb.collaboration.EnvironmentEntity;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.config.UserConfigFacade;
import com.oceanbase.odc.service.connection.CloudMetadataClient;
import com.oceanbase.odc.service.connection.CloudMetadataClient.CloudPermissionAction;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.CreateSessionReq;
import com.oceanbase.odc.service.connection.model.CreateSessionResp;
import com.oceanbase.odc.service.connection.model.DBSessionResp;
import com.oceanbase.odc.service.connection.model.DBSessionResp.DBSessionRespDelegate;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.db.DBCharsetService;
import com.oceanbase.odc.service.db.session.DBSessionService;
import com.oceanbase.odc.service.feature.VersionDiffConfigService;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.lab.model.LabProperties;
import com.oceanbase.odc.service.monitor.session.ConnectionSessionMonitorListener;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionIdGenerator;
import com.oceanbase.odc.service.session.factory.LogicalConnectionSessionFactory;
import com.oceanbase.odc.service.session.factory.StateHostGenerator;
import com.oceanbase.tools.dbbrowser.model.DBSession;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 10:35
 * @Description: []
 */
@Service
@Slf4j
@Validated
@SkipAuthorize("personal resource")
public class ConnectSessionService {

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private AuthorizationFacade authorizationFacade;
    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private ConnectProperties connectProperties;
    @Autowired
    private LabProperties labProperties;
    @Autowired
    private ConnectionTesting connectionTesting;
    @Autowired
    private SessionLimitService limitService;
    private ExecuteMonitorTaskManager monitorTaskManager;
    private DefaultConnectionSessionManager connectionSessionManager;
    @Autowired
    private VersionDiffConfigService configService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private SessionSettingsService settingsService;
    @Autowired
    private DBCharsetService charsetService;
    @Autowired
    private DBSessionService dbSessionService;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;
    @Autowired
    private SecurityManager securityManager;
    @Autowired
    private DBResourcePermissionHelper permissionHelper;
    @Autowired
    private CloudMetadataClient cloudMetadataClient;
    @Autowired
    private UserConfigFacade userConfigFacade;
    @Autowired
    private LogicalDatabaseService logicalDatabaseService;
    @Autowired
    private StateHostGenerator stateHostGenerator;
    @Autowired
    private DBSessionManageFacade dbSessionManageFacade;
    private final Map<String, Lock> sessionId2Lock = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Start to initialize the connection session module");
        this.monitorTaskManager = new ExecuteMonitorTaskManager();
        ConnectionSessionRepository repository = new InMemoryConnectionSessionRepository();
        this.connectionSessionManager = new DefaultConnectionSessionManager(
                new DefaultTaskManager("connection-session-management"), repository);
        this.connectionSessionManager.addListener(new SessionLimitListener(limitService));
        this.connectionSessionManager.addListener(new SessionLockRemoveListener(this.sessionId2Lock));
        this.connectionSessionManager.addListener(new ConnectionSessionMonitorListener());
        this.connectionSessionManager.enableAsyncRefreshSessionManager();
        this.connectionSessionManager.addSessionValidator(
                new SessionValidatorPredicate(sessionProperties.getTimeoutMins(), TimeUnit.MINUTES));
        log.info("Initialization of the connection session module is complete");
    }

    @PreDestroy
    public void destory() {
        log.info("Start destroying the connection session module");
        try {
            this.connectionSessionManager.close();
            log.info("The connection session manager closed successfully");
        } catch (Exception e) {
            log.warn("The connection session manager failed to close", e);
        }
        try {
            this.monitorTaskManager.close();
            log.info("The monitoring task manager is closed successfully");
        } catch (Exception e) {
            log.warn("Failed to close the monitoring task manager", e);
        }
        try {
            File file = ConnectionSessionUtil.getSessionWorkingDir();
            if (file.exists()) {
                FileUtils.forceDelete(file);
                log.info("Successfully delete connection session working dir, path={}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Failed to delete connection session working dir", e);
        } finally {
            log.info("Connection session module destruction completed");
        }
    }

    public CreateSessionResp createByDataSourceId(@NotNull Long dataSourceId) {
        ConnectionSession session = create(dataSourceId, null);
        return CreateSessionResp.builder()
                .sessionId(session.getId())
                .supports(configService.getSupportFeatures(session))
                .dataTypeUnits(configService.getDatatypeList(session))
                .charsets(charsetService.listCharset(session))
                .collations(charsetService.listCollation(session))
                .build();
    }

    @SkipAuthorize("check permission internally")
    public CreateSessionResp createByDatabaseId(@NotNull Long databaseId) {
        ConnectionSession session = create(null, databaseId);
        if (ConnectionSessionUtil.isLogicalSession(session)) {
            Long dataSourceId = logicalDatabaseService.listDataSourceIds(databaseId).stream().findFirst()
                    .orElseThrow(() -> new NotFoundException(ResourceType.ODC_DATABASE, "ID", databaseId));
            ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
            ConnectionSessionFactory sessionFactory = new DefaultConnectSessionFactory(connection);
            ConnectionSession physicalSession = sessionFactory.generateSession();
            try {
                return CreateSessionResp.builder()
                        .sessionId(session.getId())
                        .supports(configService.getSupportFeatures(physicalSession))
                        .dataTypeUnits(configService.getDatatypeList(physicalSession))
                        .charsets(charsetService.listCharset(physicalSession))
                        .collations(charsetService.listCollation(physicalSession))
                        .build();
            } finally {
                physicalSession.expire();
            }
        }
        return CreateSessionResp.builder()
                .sessionId(session.getId())
                .supports(configService.getSupportFeatures(session))
                .dataTypeUnits(configService.getDatatypeList(session))
                .charsets(charsetService.listCharset(session))
                .collations(charsetService.listCollation(session))
                .build();
    }

    @SkipAuthorize("check permission internally")
    public ConnectionSession create(Long dataSourceId, Long databaseId) {
        return create(new CreateSessionReq(dataSourceId, databaseId, null));
    }

    @SkipAuthorize("check permission internally")
    public ConnectionSession create(@NotNull CreateSessionReq req) {
        Long dataSourceId;
        String schemaName;
        if (req.getDbId() != null) {
            // create session by database id
            Database database = databaseService.detail(req.getDbId());
            checkDBPermission(database);
            if (database.getType() == DatabaseType.LOGICAL) {
                req.setLogicalSession(true);
                return createLogicalConnectionSession(req, database.getConnectType(),
                        database.getEnvironment().getId());
            }
            schemaName = database.getName();
            dataSourceId = database.getDataSource().getId();
        } else {
            // create session by datasource id
            PreConditions.notNull(req.getDsId(), "DatasourceId");
            Permission requiredPermission = this.securityManager.getPermissionByActions(
                    new DefaultSecurityResource(req.getDsId().toString(), ResourceType.ODC_CONNECTION.name()),
                    Collections.singletonList("update"));
            this.securityManager.checkPermission(requiredPermission);
            schemaName = null;
            dataSourceId = req.getDsId();
        }
        preCheckSessionLimit();
        ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
        cloudMetadataClient.checkPermission(OBTenant.of(connection.getClusterName(),
                connection.getTenantName()), connection.getInstanceType(), false, CloudPermissionAction.READONLY);
        PreConditions.validArgumentState(Objects.nonNull(connection.getPassword()),
                ErrorCodes.ConnectionPasswordMissed, null, "password required for connection without password saved");
        if (StringUtils.isNotBlank(schemaName) && connection.getDialectType().isOracle()) {
            schemaName = com.oceanbase.odc.common.util.StringUtils.quoteOracleIdentifier(schemaName);
        }
        horizontalDataPermissionValidator.checkCurrentOrganization(connection);
        log.info("Begin to create session, connectionId={}, name={}", connection.id(), connection.getName());
        Set<String> actions = authorizationFacade.getAllPermittedActions(authenticationFacade.currentUser(),
                ResourceType.ODC_CONNECTION, "" + dataSourceId);
        connection.setPermittedActions(actions);
        if (StringUtils.isNotEmpty(schemaName)) {
            connection.setDefaultSchema(schemaName);
        }
        return createPhysicalConnectionSession(connection, req);
    }

    public ConnectionSession createPhysicalConnectionSession(@NotNull ConnectionConfig connection,
            @NotNull CreateSessionReq req) {
        return createPhysicalConnectionSession(connection, req, 1);
    }

    public ConnectionSession createPhysicalConnectionSession(@NotNull ConnectionConfig connection,
            @NotNull CreateSessionReq req, int maxConcurrentTaskCount) {
        SqlExecuteTaskManagerFactory factory =
                new SqlExecuteTaskManagerFactory(this.monitorTaskManager, "console", maxConcurrentTaskCount);
        // TODO: query from use config service
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(
                connection, getAutoCommit(connection), factory);
        sessionFactory.setIdGenerator(getIdGenerator(req));
        sessionFactory.setSessionTimeoutMillis(getDefaultSessionTimeoutMillis());
        return startConnectionSession(sessionFactory, connection.getDialectType(), connection.getEnvironmentId());
    }

    public ConnectionSession createLogicalConnectionSession(@NotNull CreateSessionReq req,
            @NotNull ConnectType connectType,
            Long environmentId) {
        LogicalConnectionSessionFactory sessionFactory = new LogicalConnectionSessionFactory(req, connectType);
        sessionFactory.setIdGenerator(getIdGenerator(req));
        sessionFactory.setSessionTimeoutMillis(getDefaultSessionTimeoutMillis());
        return startConnectionSession(sessionFactory, connectType.getDialectType(), environmentId);
    }

    public void close(@NotNull String sessionId) {
        ConnectionSession connectionSession = getWithCreatorCheck(sessionId);
        connectionSession.expire();
    }

    public void close(@NotNull String sessionId, long delay, @NotNull TimeUnit timeUnit) {
        ConnectionSession connectionSession = getWithCreatorCheck(sessionId);
        connectionSessionManager.expire(connectionSession, delay, timeUnit);
    }

    public Set<String> close(@NotNull Set<String> sessionIds, long delay, @NotNull TimeUnit timeUnit) {
        Set<String> closedSessionIds = new HashSet<>();
        sessionIds.forEach(s -> {
            try {
                close(s, delay, timeUnit);
                closedSessionIds.add(s);
            } catch (Exception e) {
                log.warn("Failed to close a session, sessionId={}", s, e);
            }
        });
        return closedSessionIds;
    }

    public ConnectionSession nullSafeGet(@NotNull String sessionId) {
        return nullSafeGet(sessionId, false);
    }

    public ConnectionSession nullSafeGet(@NotNull String sessionId, boolean autoCreate) {
        return nullSafeGet(sessionId, autoCreate, null);
    }

    /**
     * get a ConnectionSession of sessionId. If not exist and autoCreate is true, create a new session.
     * If createConnectionSessionSupplier is not null, use it to create a new session; otherwise, create
     * a new session by default.
     * 
     * @param sessionId session id for {@link ConnectionSession}
     * @param autoCreate whether to auto create a new session if not exist
     * @param createConnectionSessionSupplier a supplier to create a new session
     * @return {@link ConnectionSession} of sessionId
     */
    public ConnectionSession nullSafeGet(@NotNull String sessionId, boolean autoCreate,
            Supplier<ConnectionSession> createConnectionSessionSupplier) {
        ConnectionSession session = connectionSessionManager.getSession(sessionId);
        if (session == null) {
            CreateSessionReq req = new DefaultConnectSessionIdGenerator().getKeyFromId(sessionId);
            if (!autoCreate
                    || (!StringUtils.equals(req.getFrom(), stateHostGenerator.getHost()) && !isOBCloudEnvironment())) {
                throw new NotFoundException(ResourceType.ODC_SESSION, "ID", sessionId);
            }
            Lock lock = this.sessionId2Lock.computeIfAbsent(sessionId, s -> new ReentrantLock());
            try {
                if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Session is creating, please wait and retry later");
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            try {
                session = connectionSessionManager.getSession(sessionId);
                if (session != null) {
                    connectionSessionManager.cancelExpire(session);
                    return session;
                }
                if (createConnectionSessionSupplier != null) {
                    session = createConnectionSessionSupplier.get();
                } else {
                    session = create(req);
                    ConnectionSessionUtil.setConsoleSessionResetFlag(session, true);
                }
                return session;
            } finally {
                lock.unlock();
            }
        }
        if (!Objects.equals(ConnectionSessionUtil.getUserId(session), authenticationFacade.currentUserId())) {
            throw new NotFoundException(ResourceType.ODC_SESSION, "ID", sessionId);
        }
        connectionSessionManager.cancelExpire(session);
        return session;
    }

    public Collection<ConnectionSession> listAllSessions() {
        return this.connectionSessionManager.retrieveAllSessions();
    }

    /**
     * Common upload method
     *
     * @param sessionId session id for {@link ConnectionSession}
     * @param inputStream {@link InputStream} file to be uploaded
     * @return file name
     * @throws IOException some errors may happened
     */
    public String uploadFile(@NotNull String sessionId, @NotNull InputStream inputStream) throws IOException {
        ConnectionSession connectionSession = nullSafeGet(sessionId);
        String key = ConnectionSessionUtil.getUniqueIdentifier(connectionSession);
        String fileName = String.format("%s_%d_user_defined_file.data", key, System.currentTimeMillis());
        File uploadDir = ConnectionSessionUtil.getSessionUploadDir(connectionSession);
        if (!uploadDir.exists() || uploadDir.isFile()) {
            log.warn("The upload directory does not exist or the directory is a file, file={}", uploadDir);
            throw new InternalServerError("Unkonwn Error");
        }
        File savedFile = new File(uploadDir.getAbsolutePath() + "/" + fileName);
        if (savedFile.exists()) {
            log.warn("Upload file already exists, file={}", savedFile);
            throw new FileExistsException("The file already exists");
        }
        try {
            FileUtils.copyInputStreamToFile(inputStream, savedFile);
        } catch (IOException exception) {
            log.warn("File write failed, file={}", savedFile, exception);
            throw new InternalServerError(ErrorCodes.FileWriteFailed, "Failed to write file content", exception);
        }
        log.info("File upload successfully, file={}", savedFile);
        return fileName;
    }

    public DBSessionResp currentDBSession(@NotNull String sessionId) {
        ConnectionSession connectionSession = nullSafeGet(SidUtils.getSessionId(sessionId), true);
        DBSession dbSession = dbSessionService.currentSession(connectionSession);
        DBSessionRespDelegate dbSessionRespDelegate = DBSessionRespDelegate.of(dbSession);
        if (dbSessionRespDelegate != null) {
            dbSessionRespDelegate
                    .setKillCurrentQuerySupported(dbSessionManageFacade.supportKillConsoleQuery(connectionSession));
        }
        return DBSessionResp.builder()
                .settings(settingsService.getSessionSettings(connectionSession))
                .session(dbSessionRespDelegate)
                .build();
    }

    private ConnectionSession startConnectionSession(@NotNull ConnectionSessionFactory sessionFactory,
            @NotNull DialectType dialectType, Long environmentId) {
        ConnectionSession session = connectionSessionManager.start(sessionFactory);
        if (session == null) {
            throw new BadRequestException("Failed to create a session");
        }
        try {
            initSession(session, dialectType, environmentId);
            log.info("Connect session created, session={}", session);
            return session;
        } catch (Exception e) {
            log.warn("Failed to create a session", e);
            if (sessionProperties.getSingleSessionMaxCount() >= 0) {
                limitService.decrementSessionCount(authenticationFacade.currentUserIdStr());
            }
            session.expire();
            throw e;
        }
    }

    private long getDefaultSessionTimeoutMillis() {
        long timeoutMillis = TimeUnit.MILLISECONDS.convert(sessionProperties.getTimeoutMins(), TimeUnit.MINUTES);
        timeoutMillis = timeoutMillis + this.connectionSessionManager.getScanIntervalMillis();
        return timeoutMillis;
    }

    private Boolean getAutoCommit(ConnectionConfig connectionConfig) {
        if (connectionConfig.getDialectType().isOracle()) {
            return "ON".equalsIgnoreCase(userConfigFacade.getOracleAutoCommitMode());
        }
        return "ON".equalsIgnoreCase(userConfigFacade.getMysqlAutoCommitMode());
    }

    private void initSession(ConnectionSession connectionSession, DialectType dialectType, Long envId) {
        SqlCommentProcessor processor = new SqlCommentProcessor(dialectType, true, true);
        processor.setDelimiter(userConfigFacade.getDefaultDelimiter());
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);

        ConnectionSessionUtil.setQueryLimit(connectionSession, userConfigFacade.getDefaultQueryLimit());
        ConnectionSessionUtil.setUserId(connectionSession, authenticationFacade.currentUserId());
        if (connectionSession.getDialectType().isOracle()) {
            ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession, connectProperties.getDefaultTimeZone());
        }
        if (envId != null) {
            Optional<EnvironmentEntity> optional = this.environmentRepository.findById(envId);
            if (optional.isPresent() && optional.get().getRulesetId() != null) {
                ConnectionSessionUtil.setRuleSetId(connectionSession, optional.get().getRulesetId());
            }
        }
    }

    private ConnectionSession getWithCreatorCheck(@NonNull String sessionId) {
        ConnectionSession session = connectionSessionManager.getSession(sessionId);
        if (session == null) {
            throw new NotFoundException(ResourceType.ODC_SESSION, "ID", sessionId);
        }
        if (!Objects.equals(ConnectionSessionUtil.getUserId(session), authenticationFacade.currentUserId())) {
            throw new NotFoundException(ResourceType.ODC_SESSION, "ID", sessionId);
        }
        connectionSessionManager.cancelExpire(session);
        return session;
    }

    private void preCheckSessionLimit() {
        long maxCount = sessionProperties.getUserMaxCount();
        if (labProperties.isSessionLimitEnabled()) {
            if (!limitService.allowCreateSession(authenticationFacade.currentUserIdStr())) {
                String errMsg = String.format("Actual %s exceeds limit: %s, please wait, current waiting number: %s",
                        LimitMetric.USER_COUNT.getLocalizedMessage(), maxCount,
                        limitService.queryWaitingNum(authenticationFacade.currentUserIdStr()));
                throw new OverLimitException(LimitMetric.USER_COUNT, (double) maxCount, errMsg);
            }
            limitService.updateTotalUserCountMap(authenticationFacade.currentUserIdStr());
        }
        long sessMaxCount = sessionProperties.getSingleSessionMaxCount();
        if (sessMaxCount >= 0) {
            try {
                PreConditions.lessThanOrEqualTo("sessionCount", LimitMetric.SESSION_COUNT,
                        limitService.incrementSessionCount(authenticationFacade.currentUserIdStr()), sessMaxCount);
            } catch (OverLimitException ex) {
                limitService.decrementSessionCount(authenticationFacade.currentUserIdStr());
                throw ex;
            }
        }
    }

    private void checkDBPermission(Database database) {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.TEAM) {
            if (Objects.isNull(database.getProject())) {
                throw new AccessDeniedException();
            }
            Map<Long, Set<DatabasePermissionType>> id2PermissionTypes =
                    permissionHelper.getDBPermissions(Collections.singleton(database.getId()));
            if (!id2PermissionTypes.containsKey(database.getId())
                    || id2PermissionTypes.get(database.getId()).isEmpty()) {
                throw new AccessDeniedException();
            }
        }
    }

    private DefaultConnectSessionIdGenerator getIdGenerator(@NotNull CreateSessionReq req) {
        DefaultConnectSessionIdGenerator idGenerator = new DefaultConnectSessionIdGenerator();
        idGenerator.setDatabaseId(req.getDbId());
        idGenerator.setFixRealId(StringUtils.isBlank(req.getRealId()) ? null : req.getRealId());
        if (Objects.nonNull(req.getFrom()) && isOBCloudEnvironment()) {
            idGenerator.setHost(req.getFrom());
        } else {
            idGenerator.setHost(stateHostGenerator.getHost());
        }
        return idGenerator;
    }

    private boolean isOBCloudEnvironment() {
        return cloudMetadataClient.supportsCloudMetadata()
                && Boolean.FALSE.equals(cloudMetadataClient.supportsCloudParentUid());
    }

    public Integer getActiveSession() {
        return this.connectionSessionManager.getActiveSessionCount();
    }

}
