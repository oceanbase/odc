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
package com.oceanbase.odc.service.pldebug.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.core.sql.util.OdcDBSessionRowMapper;
import com.oceanbase.odc.plugin.connect.api.HostAddress;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.pldebug.model.PLDebugODPSpecifiedRoute;
import com.oceanbase.odc.service.pldebug.util.CallProcedureCallBack;
import com.oceanbase.odc.service.pldebug.util.OBOracleCallFunctionCallBack;
import com.oceanbase.odc.service.pldebug.util.PLUtils;
import com.oceanbase.odc.service.session.initializer.BackupInstanceInitializer;
import com.oceanbase.odc.service.session.initializer.DataSourceInitScriptInitializer;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/11/18
 */

@Data
@Slf4j
public abstract class AbstractDebugSession implements AutoCloseable {
    public static final Long DEBUG_TIMEOUT_MS = 10 * 60 * 1000L;
    public static final int PL_LOG_CACHE_SIZE = 1000000;
    protected String debugId;
    protected ConnectionSession connectionSession;
    protected Connection connection;
    protected DebugDataSource newDataSource;
    protected JdbcOperations jdbcOperations;
    protected DialectType dialectType;
    protected PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute;
    private static final String OB_JDBC_PROTOCOL = "oceanbase";

    public abstract boolean detectSessionAlive();

    public List<DBPLParam> executeProcedure(DBProcedure procedure) {
        try {
            // -1 means statement queryTimeout will be default 0,
            // By default there is no limit on the amount of time allowed for a running statement to complete
            CallProcedureCallBack callProcedureCallBack;
            if (this.plDebugODPSpecifiedRoute == null) {
                callProcedureCallBack =
                        new CallProcedureCallBack(procedure, -1, getSqlBuilder());
            } else {
                callProcedureCallBack =
                        new CallProcedureCallBack(procedure, -1, getSqlBuilder(), this.plDebugODPSpecifiedRoute);
            }
            return getJdbcOperations().execute(callProcedureCallBack);
        } catch (Exception e) {
            throw OBException.executePlFailed(String.format("Error occurs when calling procedure={%s}, message=%s",
                    procedure.getProName(), e.getMessage()));
        }
    }

    public DBFunction executeFunction(DBFunction dbFunction) {
        // -1 means statement queryTimeout will be default 0,
        // By default there is no limit on the amount of time allowed for a running statement to complete
        OBOracleCallFunctionCallBack obOracleCallFunctionCallBack;
        if (this.plDebugODPSpecifiedRoute == null) {
            obOracleCallFunctionCallBack =
                    new OBOracleCallFunctionCallBack(dbFunction, -1);
        } else {
            obOracleCallFunctionCallBack =
                    new OBOracleCallFunctionCallBack(dbFunction, -1, this.plDebugODPSpecifiedRoute);
        }
        try {
            return getJdbcOperations().execute(obOracleCallFunctionCallBack);
        } catch (Exception e) {
            throw OBException.executePlFailed(String.format("Error occurs when calling dbFunction={%s}, message=%s",
                    dbFunction.getFunName(), e.getMessage()));
        }
    }

    protected void acquireNewConnection(ConnectionSession connectionSession,
            Supplier<DebugDataSource> dataSourceSupplier) throws Exception {
        this.connectionSession = connectionSession;
        ConnectionConfig connectionConfig =
                (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        this.dialectType = connectionConfig.getDialectType();
        this.newDataSource = dataSourceSupplier.get();
        this.jdbcOperations = new JdbcTemplate(this.newDataSource);
        this.connection = newDataSource.getConnection();
    }

    protected DebugDataSource acquireDataSource(@NonNull ConnectionSession connectionSession,
            @NonNull List<String> initSqls) {
        ConnectionConfig config = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        Verify.equals(DialectType.OB_ORACLE, config.getDialectType(), "Only support OB_ORACLE");
        if (connectionSession.getConnectType() == ConnectType.OB_ORACLE
                && StringUtils.isBlank(config.getClusterName())) {
            // current connection is a direct observer
            return buildDataSource(config, initSqls, null,
                    buildJdbcUrl(new HostAddress(config.getHost(), config.getPort()), schema));
        } else {
            Optional<DebugDataSource> odpSpecifiedRouteDataSource =
                    tryGetODPSpecifiedRouteDataSource(connectionSession, initSqls, config,
                            schema);
            if (odpSpecifiedRouteDataSource.isPresent()) {
                return odpSpecifiedRouteDataSource.get();
            }
            // cloud ob oracle not support direct connection to observer
            if (connectionSession.getConnectType() == ConnectType.CLOUD_OB_ORACLE) {
                throw new IllegalStateException(String.format(
                        "ODP specified route is not supported for cloud ob oracle connection, ODP version: %s",
                        ConnectionSessionUtil.getObProxyVersion(connectionSession)));
            }
            // use direct connection observer
            return buildDataSource(config, initSqls, null,
                    buildJdbcUrl(getOBServerHostAddress(connectionSession), schema));
        }
    }

    private Optional<DebugDataSource> tryGetODPSpecifiedRouteDataSource(ConnectionSession connectionSession,
            List<String> initSqls,
            ConnectionConfig config, String schema) {
        String obProxyVersion = ConnectionSessionUtil.getObProxyVersion(connectionSession);
        if (ConnectionSessionUtil.isSupportObProxyRoute(obProxyVersion)) {
            HostAddress directServerIp = getOBServerHostAddress(connectionSession);
            this.plDebugODPSpecifiedRoute =
                    new PLDebugODPSpecifiedRoute(directServerIp.getHost(), directServerIp.getPort());
            return Optional.of(buildDataSource(config, initSqls, this.plDebugODPSpecifiedRoute,
                    buildJdbcUrl(new HostAddress(config.getHost(), config.getPort()), schema)));
        }
        return Optional.empty();
    }

    private String buildJdbcUrl(HostAddress hostAddress, String schema) {
        return String.format("jdbc:%s://%s:%d/\"%s\"", OB_JDBC_PROTOCOL, hostAddress.getHost(), hostAddress.getPort(),
                schema);
    }

    private DebugDataSource buildDataSource(ConnectionConfig config, List<String> initSqls,
            PLDebugODPSpecifiedRoute route, String url) {
        DebugDataSource dataSource = new DebugDataSource(config, initSqls, route);
        dataSource.setUrl(url);
        dataSource.setUsername(buildUserName(config));
        dataSource.setPassword(config.getPassword());
        dataSource.setDriverClassName(OdcConstants.DEFAULT_DRIVER_CLASS_NAME);
        return dataSource;
    }

    private HostAddress getOBServerHostAddress(ConnectionSession connectionSession) {
        List<OdcDBSession> sessions =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                        .query("show full processlist", new OdcDBSessionRowMapper());
        if (CollectionUtils.isEmpty(sessions)) {
            throw new UnexpectedException("Empty db session list");
        }
        String directServerIp = null;
        for (OdcDBSession odcDbSession : sessions) {
            if (StringUtils.isNotBlank(odcDbSession.getSvrIp())) {
                directServerIp = odcDbSession.getSvrIp();
                break;
            }
        }
        if (StringUtils.isEmpty(directServerIp)) {
            throw new UnexpectedException("Empty direct server ip and port from 'show full processlist'");
        }
        String[] ipParts = directServerIp.split(":");
        return new HostAddress(ipParts[0], Integer.parseInt(ipParts[1]));
    }

    private String buildUserName(ConnectionConfig connectionConfig) {
        StringBuilder userNameBuilder = new StringBuilder(connectionConfig.getUsername());
        if (StringUtils.isNotBlank(connectionConfig.getTenantName())) {
            userNameBuilder.append("@").append(connectionConfig.getTenantName());
        }
        return userNameBuilder.toString();
    }

    public JdbcOperations getJdbcOperations() {
        return jdbcOperations;
    }

    private SqlBuilder getSqlBuilder() {
        return new OracleSqlBuilder();
    }

    public DialectType getDialectType() {
        return dialectType;
    }

    @Override
    public void close() {
        if (newDataSource != null) {
            newDataSource.destroy();
        }
    }

    protected void enableDbmsOutput(Statement statement) {
        try {
            statement.execute(String.format("%s call dbms_output.enable(%s);",
                    PLUtils.getSpecifiedRoute(this.plDebugODPSpecifiedRoute), PL_LOG_CACHE_SIZE));
        } catch (Exception e) {
            log.warn("enable dbms output failed, dbms_output may not exists, sid={}, reason={}",
                    connectionSession.getId(),
                    ExceptionUtils.getRootCauseMessage(e));
        }
    }

    static class DebugDataSource extends SingleConnectionDataSource {

        private final List<String> initSqls;
        private final List<ConnectionInitializer> initializers;
        private final PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute;

        public DebugDataSource(@NonNull ConnectionConfig connectionConfig, List<String> initSqls,
                PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute) {
            this.initSqls = initSqls;
            this.initializers = Arrays.asList(new BackupInstanceInitializer(connectionConfig),
                    new DataSourceInitScriptInitializer(connectionConfig, true));
            this.plDebugODPSpecifiedRoute = plDebugODPSpecifiedRoute;
        }

        @Override
        protected void prepareConnection(Connection con) throws SQLException {
            super.prepareConnection(con);
            if (CollectionUtils.isNotEmpty(this.initSqls)) {
                try (Statement statement = con.createStatement()) {
                    for (String stmt : this.initSqls) {
                        statement.execute(PLUtils.getSpecifiedRoute(plDebugODPSpecifiedRoute) + stmt);
                    }
                }
            }
            for (ConnectionInitializer initializer : this.initializers) {
                initializer.init(con);
            }
        }
    }

}
