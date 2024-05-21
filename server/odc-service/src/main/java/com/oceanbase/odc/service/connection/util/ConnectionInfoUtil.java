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
package com.oceanbase.odc.service.connection.util;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.GeneralSyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-03-23
 * @since 4.2.0
 */
@Slf4j
public class ConnectionInfoUtil {

    public static void initConsoleConnectionId(@NonNull ConnectionSession connectionSession) {
        try {
            getSyncJdbcExecutor(connectionSession).execute((StatementCallback<Void>) stmt -> {
                initConnectionId(stmt, connectionSession);
                return null;
            });
            log.debug("Init connection id completed.");
        } catch (Exception e) {
            log.warn("Failed to get database session ID, session={}", connectionSession, e);
        }
    }

    public static void initConnectionId(@NonNull Statement statement, @NonNull ConnectionSession connectionSession) {
        try {
            String sessionId = queryConnectionId(statement, connectionSession.getDialectType());
            Verify.notNull(sessionId, "SessionId");
            connectionSession.setAttribute(ConnectionSessionConstants.CONNECTION_ID_KEY, sessionId);
            if (connectionSession.getDialectType().isOceanbase() && VersionUtils.isGreaterThanOrEqualsTo(
                    ConnectionSessionUtil.getVersion(connectionSession), "4.2")) {
                String proxySessId =
                        OBUtils.queryOBProxySessId(statement, connectionSession.getDialectType(), sessionId);
                connectionSession.setAttribute(ConnectionSessionConstants.OB_PROXY_SESSID_KEY, proxySessId);
            }
        } catch (Exception exception) {
            log.warn("Failed to get database session ID, session={}", connectionSession, exception);
        }
    }

    public static String queryConnectionId(@NonNull Statement statement, @NonNull DialectType dialectType)
            throws SQLException {
        return ConnectionPluginUtil.getSessionExtension(dialectType).getConnectionId(statement.getConnection());
    }

    public static void initSessionVersion(@NonNull ConnectionSession connectionSession) {
        InformationExtensionPoint point =
                ConnectionPluginUtil.getInformationExtension(connectionSession.getDialectType());
        String version = getSyncJdbcExecutor(connectionSession).execute(point::getDBVersion);
        if (version == null) {
            throw new IllegalStateException("DB version can not be null");
        }
        connectionSession.setAttribute(ConnectionSessionConstants.OB_VERSION, version);
        log.debug("Init DB version completed.");
    }

    public static void killQuery(@NonNull String connectionId,
            @NonNull DataSourceFactory dataSourceFactory, DialectType dialectType) throws Exception {
        DataSource dataSource = dataSourceFactory.getDataSource();
        try {
            SyncJdbcExecutor jdbcExecutor = new GeneralSyncJdbcExecutor(dataSource);
            jdbcExecutor.execute((ConnectionCallback<Void>) con -> {
                ConnectionPluginUtil.getSessionExtension(dialectType).killQuery(con, connectionId);
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to kill query, connectionId={}", connectionId, e);
            throw e;
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
    }

    private static SyncJdbcExecutor getSyncJdbcExecutor(ConnectionSession session) {
        return session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
    }

}
