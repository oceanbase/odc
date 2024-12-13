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
package com.oceanbase.odc.service.session.factory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.event.ConnectionResetEvent;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.session.ConnectionSessionIdGenerator;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.DefaultConnectionSession;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManager;
import com.oceanbase.odc.core.task.TaskManagerFactory;
import com.oceanbase.odc.plugin.connect.api.JdbcUrlParser;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.DBClientInfo;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.CreateSessionReq;
import com.oceanbase.odc.service.connection.util.ConnectionInfoUtil;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.monitor.datasource.GetConnectionFailedEventListener;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.initializer.SwitchSchemaInitializer;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A database session factory specifically for oceanbase, used to generate customized database
 * sessions
 *
 * @author yh263208
 * @date 2021-11-17 13:56
 * @see ConnectionSessionFactory
 * @since ODC_release_3.2.2
 */
@Slf4j
public class DefaultConnectSessionFactory implements ConnectionSessionFactory {
    public static final String DEFAULT_MODULE = "ODC";
    public static final String CONNECT_SESSION_SQL_CONSOLE = "ConnectSession-SqlConsole";

    private final ConnectionConfig connectionConfig;
    private final TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory;
    private final Boolean autoCommit;
    private final EventPublisher eventPublisher;
    private final boolean autoReconnect;
    private final boolean keepAlive;
    @Setter
    private long sessionTimeoutMillis;
    @Setter
    private ConnectionSessionIdGenerator<CreateSessionReq> idGenerator;

    public DefaultConnectSessionFactory(@NonNull ConnectionConfig connectionConfig,
            Boolean autoCommit, TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory, boolean autoReconnect,
            boolean keepAlive) {
        this.sessionTimeoutMillis = TimeUnit.MILLISECONDS.convert(
                ConnectionSessionConstants.SESSION_EXPIRATION_TIME_SECONDS, TimeUnit.SECONDS);
        this.connectionConfig = connectionConfig;
        this.taskManagerFactory = taskManagerFactory;
        this.autoCommit = autoCommit == null || autoCommit;
        this.eventPublisher = new LocalEventPublisher();
        this.idGenerator = new DefaultConnectSessionIdGenerator();
        this.autoReconnect = autoReconnect;
        this.keepAlive = keepAlive;
    }

    public DefaultConnectSessionFactory(@NonNull ConnectionConfig connectionConfig,
            Boolean autoCommit, TaskManagerFactory<SqlExecuteTaskManager> taskManagerFactory) {
        this(connectionConfig, autoCommit, taskManagerFactory, true, true);
    }

    public DefaultConnectSessionFactory(@NonNull ConnectionConfig connectionConfig) {
        this(connectionConfig, null, null, true, false);
    }

    @Override
    public ConnectionSession generateSession() {
        ConnectionSession session = createSession();
        registerSysDataSource(session);
        registerConsoleDataSource(session);
        registerBackendDataSource(session);
        initSession(session);
        return session;
    }

    private void registerConsoleDataSource(ConnectionSession session) {
        OBConsoleDataSourceFactory dataSourceFactory =
                new OBConsoleDataSourceFactory(connectionConfig, autoCommit, true, autoReconnect, keepAlive);
        try {
            JdbcUrlParser urlParser = ConnectionPluginUtil
                    .getConnectionExtension(connectionConfig.getDialectType())
                    .getConnectionInfo(dataSourceFactory.getJdbcUrl(), dataSourceFactory.getUsername());
            String connectSchema = urlParser.getSchema();
            if (StringUtils.isNotBlank(connectSchema)) {
                connectSchema = ConnectionSessionUtil.getUserOrSchemaString(connectSchema, session.getDialectType());
                ConnectionSessionUtil.setConnectSchema(session, connectSchema);
            }
            ConnectionSessionUtil.setCurrentSchema(session, connectionConfig.getDefaultSchema());
        } catch (Exception e) {
            if (StringUtils.isNotBlank(connectionConfig.getDefaultSchema())) {
                ConnectionSessionUtil.setConnectSchema(session, connectionConfig.getDefaultSchema());
                ConnectionSessionUtil.setCurrentSchema(session, connectionConfig.getDefaultSchema());
            }
        }
        dataSourceFactory.setEventPublisher(eventPublisher);
        ProxyDataSourceFactory proxyFactory = new ProxyDataSourceFactory(dataSourceFactory);
        session.register(ConnectionSessionConstants.CONSOLE_DS_KEY, proxyFactory);
        proxyFactory.setInitializer(new SwitchSchemaInitializer(session));
    }

    private void registerBackendDataSource(ConnectionSession session) {
        DruidDataSourceFactory dataSourceFactory = new DruidDataSourceFactory(connectionConfig);
        ProxyDataSourceFactory proxyFactory = new ProxyDataSourceFactory(dataSourceFactory);
        session.register(ConnectionSessionConstants.BACKEND_DS_KEY, proxyFactory);
        proxyFactory.setInitializer(new SwitchSchemaInitializer(session));
    }

    private void registerSysDataSource(ConnectionSession session) {
        OBSysUserDataSourceFactory dataSourceFactory = new OBSysUserDataSourceFactory(connectionConfig);
        session.register(ConnectionSessionConstants.SYS_DS_KEY, dataSourceFactory);
    }

    private ConnectionSession createSession() {
        try {
            return new DefaultConnectionSession(idGenerator.generateId(CreateSessionReq.from(connectionConfig)),
                    taskManagerFactory, sessionTimeoutMillis, connectionConfig.getType(), autoCommit,
                    ConnectionPluginUtil.getSessionExtension(connectionConfig.getDialectType()));
        } catch (Exception e) {
            log.warn("Failed to create connection session", e);
            throw new IllegalStateException(e);
        }
    }

    private void initSession(ConnectionSession session) {
        this.eventPublisher.addEventListener(new ConsoleConnectionResetListener(session));
        this.eventPublisher.addEventListener(new GetConnectionFailedEventListener());
        ConnectionSessionUtil.initArchitecture(session);
        ConnectionInfoUtil.initSessionVersion(session);
        ConnectionSessionUtil.setConsoleSessionResetFlag(session, false);
        ConnectionInfoUtil.initConsoleConnectionId(session);
        ConnectionInfoUtil.initOdpVersionIfExists(session);
        ConnectionSessionUtil.setConnectionConfig(session, connectionConfig);
        ConnectionSessionUtil.setColumnAccessor(session, new DatasourceColumnAccessor(session));
        if (StringUtils.isNotBlank(connectionConfig.getTenantName())) {
            ConnectionSessionUtil.setTenantName(session, connectionConfig.getTenantName());
        }
        if (StringUtils.isNotBlank(connectionConfig.getClusterName())) {
            ConnectionSessionUtil.setClusterName(session, connectionConfig.getClusterName());
        }
        setNlsFormat(session);
        setClientInfo(session);
    }

    private static void setNlsFormat(ConnectionSession session) {
        if (!session.getDialectType().isOracle()) {
            return;
        }
        log.info("Begin to set nls format.");
        String nlsDateFormat = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                (ConnectionCallback<String>) con -> ConnectionPluginUtil
                        .getSessionExtension(session.getDialectType()).getVariable(con, "nls_date_format"));
        ConnectionSessionUtil.setNlsDateFormat(session, Objects.isNull(nlsDateFormat) ? "DD-MON-RR" : nlsDateFormat);

        String nlsTimestampFormat = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                (ConnectionCallback<String>) con -> ConnectionPluginUtil
                        .getSessionExtension(session.getDialectType()).getVariable(con, "nls_timestamp_format"));
        ConnectionSessionUtil.setNlsTimestampFormat(session,
                Objects.isNull(nlsTimestampFormat) ? "DD-MON-RR" : nlsTimestampFormat);

        String nlsTimestampTZFormat = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                (ConnectionCallback<String>) con -> ConnectionPluginUtil
                        .getSessionExtension(session.getDialectType()).getVariable(con, "nls_timestamp_tz_format"));
        ConnectionSessionUtil.setNlsTimestampTZFormat(session,
                Objects.isNull(nlsTimestampTZFormat) ? "DD-MON-RR" : nlsTimestampTZFormat);

        log.info("Set nls format completed.");
    }

    private void setClientInfo(ConnectionSession session) {
        SessionExtensionPoint extensionPoint = ConnectionPluginUtil.getSessionExtension(session.getDialectType());
        String clientInfo = UUID.randomUUID().toString();
        SyncJdbcExecutor consoleJdbcExecutor =
                session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        consoleJdbcExecutor.execute((ConnectionCallback<Void>) con -> {
            boolean setSuccess = extensionPoint.setClientInfo(con,
                    new DBClientInfo(DEFAULT_MODULE, CONNECT_SESSION_SQL_CONSOLE, clientInfo));
            if (setSuccess) {
                ConnectionSessionUtil.setConsoleSessionClientInfo(session, clientInfo);
                log.info("Set client info completed. sid={}, clientInfo={}", session.getId(), clientInfo);
            }
            return null;
        });

    }

    /**
     * {@link ConsoleConnectionResetListener}
     *
     * @author yh263208
     * @date 2022-10-10 22:20
     * @since ODC_release_3.5.0
     * @see AbstractEventListener
     */
    @Slf4j
    static class ConsoleConnectionResetListener extends AbstractEventListener<ConnectionResetEvent> {

        private final ConnectionSession connectionSession;

        public ConsoleConnectionResetListener(@NonNull ConnectionSession connectionSession) {
            this.connectionSession = connectionSession;
        }

        @Override
        public void onEvent(ConnectionResetEvent event) {
            ConnectionSessionUtil.setConsoleSessionResetFlag(connectionSession, true);
            try (Statement statement = ((Connection) event.getSource()).createStatement()) {
                ConnectionInfoUtil.initConnectionId(statement, connectionSession);
            } catch (Exception e) {
                log.warn("Failed to init connection id", e);
            }
        }
    }

}
