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
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionRepository;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.DefaultConnectionSessionManager;
import com.oceanbase.odc.core.session.InMemorySessionRepository;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
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
import com.oceanbase.odc.service.config.model.UserConfig;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.CreateSessionResp;
import com.oceanbase.odc.service.connection.model.DBSessionResp;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.db.DBCharsetService;
import com.oceanbase.odc.service.db.session.DBSessionService;
import com.oceanbase.odc.service.feature.VersionDiffConfigService;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.lab.model.LabProperties;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

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
@Authenticated
public class ConnectSessionService {

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private UserConfigFacade userConfigFacade;
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

    @PostConstruct
    public void init() {
        log.info("Start to initialize the connection session module");
        this.monitorTaskManager = new ExecuteMonitorTaskManager();
        ConnectionSessionRepository repository = new InMemorySessionRepository();
        this.connectionSessionManager = new DefaultConnectionSessionManager(
                new DefaultTaskManager("connection-session-management"), repository);
        this.connectionSessionManager.addListener(new SessionLimitListener(limitService));
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

    @PreAuthenticate(actions = "update", resourceType = "ODC_CONNECTION", indexOfIdParam = 0)
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
        Database database = databaseService.detail(databaseId);
        if (Objects.isNull(database.getProject())
                && authenticationFacade.currentUser().getOrganizationType() == OrganizationType.TEAM) {
            throw new AccessDeniedException();
        }
        ConnectionSession session = create(database.getDataSource().getId(), database.getName());
        return CreateSessionResp.builder()
                .sessionId(session.getId())
                .supports(configService.getSupportFeatures(session))
                .dataTypeUnits(configService.getDatatypeList(session))
                .charsets(charsetService.listCharset(session))
                .collations(charsetService.listCollation(session))
                .build();
    }

    @SkipAuthorize("only for unit test")
    protected ConnectionSession createForTest(@NotNull Long dataSourceId, String schemaName) {
        return create(dataSourceId, schemaName);
    }

    private ConnectionSession create(@NotNull Long dataSourceId, String schemaName) {
        preCheckSessionLimit();

        ConnectionConfig connection = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
        horizontalDataPermissionValidator.checkCurrentOrganization(connection);
        log.info("Begin to create session, connection id={}, name={}", connection.id(), connection.getName());
        Set<String> actions = authorizationFacade.getAllPermittedActions(authenticationFacade.currentUser(),
                ResourceType.ODC_CONNECTION, "" + dataSourceId);
        connection.setPermittedActions(actions);
        ConnectionTestResult result = connectionTesting.test(connection, ConnectionAccountType.MAIN);
        Verify.verify(result.isActive(), result.getErrorMessage());

        UserConfig userConfig = userConfigFacade.queryByCache(authenticationFacade.currentUserId());
        SqlExecuteTaskManagerFactory factory =
                new SqlExecuteTaskManagerFactory(this.monitorTaskManager, "console", 1);
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(
                connection, ConnectionAccountType.MAIN, getAutoCommit(connection, userConfig), factory);
        long timeoutMillis = TimeUnit.MILLISECONDS.convert(sessionProperties.getTimeoutMins(), TimeUnit.MINUTES);
        timeoutMillis = timeoutMillis + this.connectionSessionManager.getScanIntervalMillis();
        sessionFactory.setSessionTimeoutMillis(timeoutMillis);
        sessionFactory.setBackendQueryTimeoutMicros(sessionProperties.getBackendQueryTimeoutMicros());
        ConnectionSession session = connectionSessionManager.start(sessionFactory);
        try {
            initSession(session, connection, userConfig);
            ConnectionSessionUtil.setPermittedActions(session, actions);
            ConnectionSessionUtil.setConnectionAccountType(session, ConnectionAccountType.MAIN);
            DatasourceColumnAccessor accessor = new DatasourceColumnAccessor(session);
            ConnectionSessionUtil.setColumnAccessor(session, accessor);
            if (StringUtils.isNotEmpty(schemaName)) {
                ConnectionSessionUtil.setCurrentSchema(session, schemaName);
            }
            log.info("Connect session created, connectionId={}, session={}", dataSourceId, session);
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
        ConnectionSession session = connectionSessionManager.getSession(sessionId);
        if (session == null) {
            throw new NotFoundException(ResourceType.ODC_SESSION, "ID", sessionId);
        }
        if (!Objects.equals(ConnectionSessionUtil.getUserId(session), authenticationFacade.currentUserId())) {
            throw new NotFoundException(ResourceType.ODC_SESSION, "ID", sessionId);
        }
        checkPermission(session);
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
        String fileName =
                String.format("%s_%d_user_defined_file.data", connectionSession.getId(), System.currentTimeMillis());
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
        ConnectionSession connectionSession = nullSafeGet(SidUtils.getSessionId(sessionId));
        return DBSessionResp.builder()
                .settings(settingsService.getSessionSettings(connectionSession))
                .session(dbSessionService.currentSession(connectionSession))
                .build();
    }

    private Boolean getAutoCommit(ConnectionConfig connectionConfig, UserConfig userConfig) {
        if (DialectType.OB_ORACLE.equals(connectionConfig.getDialectType())) {
            return "ON".equalsIgnoreCase(userConfig.getOracleAutoCommitMode());
        }
        return "ON".equalsIgnoreCase(userConfig.getMysqlAutoCommitMode());
    }

    private void initSession(ConnectionSession connectionSession, ConnectionConfig connectionConfig,
            UserConfig userConfig) {
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true, true);
        processor.setDelimiter(userConfig.getDefaultDelimiter());
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);

        ConnectionSessionUtil.setQueryLimit(connectionSession, userConfig.getDefaultQueryLimit());
        ConnectionSessionUtil.setUserId(connectionSession, authenticationFacade.currentUserId());
        ConnectionSessionUtil.setConnectionConfig(connectionSession, connectionConfig);
        if (connectionSession.getDialectType() == DialectType.OB_ORACLE) {
            ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession, connectProperties.getDefaultTimeZone());
            log.info("Init time zone completed.");
        }
        Long envId = connectionConfig.getEnvironmentId();
        if (envId != null) {
            Optional<EnvironmentEntity> optional = this.environmentRepository.findById(envId);
            if (optional.isPresent() && optional.get().getRulesetId() != null) {
                ConnectionSessionUtil.setRuleSetId(connectionSession, optional.get().getRulesetId());
            }
        }
    }

    private void checkPermission(ConnectionSession session) {
        // Only allow access to sessions created by the user himself/herself
        if (ConnectionSessionUtil.getUserId(session) == authenticationFacade.currentUserId()) {
            return;
        }
        throw new BadRequestException(ErrorCodes.UnauthorizedSessionAccess, null, "Unauthorized");
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

}
