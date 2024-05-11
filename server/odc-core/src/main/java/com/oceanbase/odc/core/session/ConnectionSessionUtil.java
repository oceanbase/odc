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
package com.oceanbase.odc.core.session;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.jdbc.internal.protocol.Protocol;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.core.sql.execute.GeneralSyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualTable;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.util.OdcDBSessionRowMapper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Tools for {@code JDBC}
 *
 * @author yh263208
 * @date 2021-11-10 17:06
 * @since ODC_release_3.2.2
 */
@Slf4j
public class ConnectionSessionUtil {

    public static void logSocketInfo(Connection connection, String scenario) {
        if (!(connection instanceof OceanBaseConnection)) {
            log.debug("skip log connection socket info due not an OceanBaseConnection, className={}, scenario={}",
                    connection.getClass().getSimpleName(), scenario);
            return;
        }
        OceanBaseConnection obConnection = (OceanBaseConnection) connection;
        long serverThreadId = obConnection.getServerThreadId();
        Socket socket = getSocket(connection);
        if (socket == null) {
            log.warn("skip log connection socket info due null socket, scenario={}, serverThreadId={}",
                    scenario, serverThreadId);
            return;
        }
        log.info("Connection socket info, scenario={}, serverThreadId={}, remoteAddress={}, localAddress={}",
                scenario, serverThreadId, socket.getRemoteSocketAddress(), socket.getLocalSocketAddress());
    }

    public static Socket getSocket(Connection connection) {
        if (Objects.isNull(connection)) {
            return null;
        }
        if (!(connection instanceof OceanBaseConnection)) {
            return null;
        }
        OceanBaseConnection obConnection = (OceanBaseConnection) connection;
        Protocol protocol = getProtocol(obConnection);
        if (protocol == null) {
            log.warn("skip log connection info due protocol null, className={}",
                    connection.getClass().getSimpleName());
            return null;
        }
        return protocol.getSocket();
    }

    public static Protocol getProtocol(OceanBaseConnection connection) {
        try {
            Method getProtocolMethod = OceanBaseConnection.class.getDeclaredMethod("getProtocol");
            getProtocolMethod.setAccessible(true);
            return (Protocol) getProtocolMethod.invoke(connection);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.warn("get protocol for connection failed, connectionId={}, reason={}",
                    connection.getServerThreadId(), e.getMessage());
            return null;
        }
    }

    /**
     * remove the double quotation in header of a string, and to uppercase when a string without double
     * quotation eg. "abc"dff" -> abc"dff abcd -> ABCD <br>
     * TODO: hard to understand here, rename it!
     *
     * @param input input string
     * @param dialectType dialect type
     * @return string without double quotation
     */
    public static String getUserOrSchemaString(String input, DialectType dialectType) {
        if (!(Objects.nonNull(dialectType) && dialectType.isOracle())) {
            return input;
        }
        if (input == null) {
            return null;
        }
        if (!input.startsWith("\"") || !input.endsWith("\"")) {
            return input.toUpperCase();
        }
        return input.substring(1, input.length() - 1);
    }

    @SuppressWarnings("all")
    public static <T> String setFutureJdbc(@NonNull ConnectionSession connectionSession,
            @NonNull Future<T> futureResult, Map<String, Object> context) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.FUTURE_JDBC_RESULT_KEY);
        Map<String, Pair<Future<T>, Map<String, Object>>> id2FutureResult;
        if (value instanceof Map) {
            id2FutureResult = (Map<String, Pair<Future<T>, Map<String, Object>>>) value;
        } else {
            id2FutureResult = new ConcurrentHashMap<>();
            connectionSession.setAttribute(ConnectionSessionConstants.FUTURE_JDBC_RESULT_KEY, id2FutureResult);
        }
        String uuid = UUID.randomUUID().toString();
        id2FutureResult.putIfAbsent(uuid, new Pair<>(futureResult, context));
        return uuid;
    }

    @SuppressWarnings("all")
    public static <T> Future<T> getFutureJdbcResult(
            @NonNull ConnectionSession connectionSession, @NonNull String requestId) {
        return (Future<T>) innerGetResult(connectionSession, requestId).left;
    }

    public static Map<String, Object> getFutureJdbcContext(
            @NonNull ConnectionSession connectionSession, @NonNull String requestId) {
        return innerGetResult(connectionSession, requestId).right;
    }

    @SuppressWarnings("all")
    private static <T> Pair<Future<T>, Map<String, Object>> innerGetResult(
            @NonNull ConnectionSession connectionSession, @NonNull String requestId) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.FUTURE_JDBC_RESULT_KEY);
        Map<String, Pair<Future<T>, Map<String, Object>>> id2FutureResult;
        if (value instanceof Map) {
            id2FutureResult = (Map<String, Pair<Future<T>, Map<String, Object>>>) value;
        } else {
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "session id", connectionSession.getId());
        }
        Pair<Future<T>, Map<String, Object>> resultList = id2FutureResult.get(requestId);
        if (resultList == null) {
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "request id", requestId);
        }
        return resultList;
    }

    @SuppressWarnings("all")
    public static <T> void removeFutureJdbc(@NonNull ConnectionSession connectionSession,
            @NonNull String requestId) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.FUTURE_JDBC_RESULT_KEY);
        Map<String, Pair<Future<T>, Map<String, Object>>> id2FutureResult;
        if (value == null) {
            return;
        } else if (!(value instanceof Map)) {
            throw new NullPointerException("Result not found by session id " + connectionSession.getId());
        } else {
            id2FutureResult = (Map<String, Pair<Future<T>, Map<String, Object>>>) value;
        }
        id2FutureResult.remove(requestId);
    }

    @SuppressWarnings("all")
    public static <T> String setExecuteContext(@NonNull ConnectionSession connectionSession,
            @NonNull Object context) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.ASYNC_EXECUTE_CONTEXT_KEY);
        Map<String, Object> id2ExecuteContext;
        if (value instanceof Map) {
            id2ExecuteContext = (Map<String, Object>) value;
        } else {
            id2ExecuteContext = new ConcurrentHashMap<>();
            connectionSession.setAttribute(ConnectionSessionConstants.ASYNC_EXECUTE_CONTEXT_KEY, id2ExecuteContext);
        }
        String uuid = UUID.randomUUID().toString();
        id2ExecuteContext.putIfAbsent(uuid, context);
        return uuid;
    }

    @SuppressWarnings("all")
    public static <T> Object getExecuteContext(@NonNull ConnectionSession connectionSession,
            @NonNull String requestId) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.ASYNC_EXECUTE_CONTEXT_KEY);
        Map<String, Object> id2ExecuteContext;
        if (value instanceof Map) {
            id2ExecuteContext = (Map<String, Object>) value;
        } else {
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "session id", connectionSession.getId());
        }
        Object context = id2ExecuteContext.get(requestId);
        if (context == null) {
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "request id", requestId);
        }
        return context;
    }

    @SuppressWarnings("all")
    public static <T> void removeExecuteContext(@NonNull ConnectionSession connectionSession,
            @NonNull String requestId) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.ASYNC_EXECUTE_CONTEXT_KEY);
        Map<String, Object> id2ExecuteContext;
        if (!(value instanceof Map)) {
            throw new NullPointerException("Result not found by session id " + connectionSession.getId());
        } else {
            id2ExecuteContext = (Map<String, Object>) value;
        }
        id2ExecuteContext.remove(requestId);
    }

    public static void setUserId(@NonNull ConnectionSession connectionSession, @NonNull Long userId) {
        connectionSession.setAttribute(ConnectionSessionConstants.USER_ID_KEY, userId);
    }

    public static Long getUserId(@NonNull ConnectionSession connectionSession) {
        return (Long) connectionSession.getAttribute(ConnectionSessionConstants.USER_ID_KEY);
    }

    public static void setTenantName(@NonNull ConnectionSession connectionSession, @NonNull String tenantName) {
        connectionSession.setAttribute(ConnectionSessionConstants.TENANT_NAME, tenantName);
    }

    public static String getTenantName(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.TENANT_NAME);
    }

    public static void setConnectSchema(@NonNull ConnectionSession connectionSession, @NonNull String schema) {
        connectionSession.setAttribute(ConnectionSessionConstants.CONNECT_SCHEMA_KEY, schema);
    }

    public static String getConnectSchema(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.CONNECT_SCHEMA_KEY);
    }

    public static void setClusterName(@NonNull ConnectionSession connectionSession, @NonNull String tenantName) {
        connectionSession.setAttribute(ConnectionSessionConstants.CLUSTER_NAME, tenantName);
    }

    public static String getClusterName(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.CLUSTER_NAME);
    }

    public static void setCurrentSchema(@NonNull ConnectionSession connectionSession, @NonNull String schema) {
        connectionSession.setAttribute(ConnectionSessionConstants.CURRENT_SCHEMA_KEY, schema);
    }

    public static String getCurrentSchema(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.CURRENT_SCHEMA_KEY);
    }

    public static void setQueryLimit(@NonNull ConnectionSession connectionSession, @NonNull Integer queryLimit) {
        connectionSession.setAttribute(ConnectionSessionConstants.QUERY_LIMIT_KEY, queryLimit);
    }

    public static Integer getQueryLimit(@NonNull ConnectionSession connectionSession) {
        return (Integer) connectionSession.getAttribute(ConnectionSessionConstants.QUERY_LIMIT_KEY);
    }

    public static void setSqlCommentProcessor(@NonNull ConnectionSession connectionSession,
            @NonNull SqlCommentProcessor commentProcessor) {
        connectionSession.setAttribute(ConnectionSessionConstants.SQL_COMMENT_PROCESSOR_KEY, commentProcessor);
    }

    public static SqlCommentProcessor getSqlCommentProcessor(@NonNull ConnectionSession connectionSession) {
        return (SqlCommentProcessor) connectionSession
                .getAttribute(ConnectionSessionConstants.SQL_COMMENT_PROCESSOR_KEY);
    }

    public static void setConnectionConfig(@NonNull ConnectionSession connectionSession,
            @NonNull Object connectionConfig) {
        connectionSession.setAttribute(ConnectionSessionConstants.CONNECTION_CONFIG_KEY, connectionConfig);
    }

    public static Object getConnectionConfig(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getAttribute(ConnectionSessionConstants.CONNECTION_CONFIG_KEY);
    }

    public static void setConsoleSessionResetFlag(@NonNull ConnectionSession connectionSession,
            @NonNull Boolean reset) {
        connectionSession.setAttribute(ConnectionSessionConstants.CONNECTION_RESET_KEY, reset);
    }

    public static boolean isConsoleSessionReset(@NonNull ConnectionSession connectionSession) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.CONNECTION_RESET_KEY);
        if (value == null) {
            return false;
        }
        return (Boolean) value;
    }

    public static void setConsoleSessionKillQueryFlag(@NonNull ConnectionSession connectionSession,
            @NonNull Boolean killQuery) {
        connectionSession.setAttribute(ConnectionSessionConstants.CONNECTION_KILLQUERY_KEY, killQuery);
    }

    public static boolean isConsoleSessionKillQuery(@NonNull ConnectionSession connectionSession) {
        Object value = connectionSession.getAttribute(ConnectionSessionConstants.CONNECTION_KILLQUERY_KEY);
        if (value == null) {
            return false;
        }
        return (Boolean) value;
    }

    public static void setColumnAccessor(@NonNull ConnectionSession connectionSession, @NonNull Object columnAccessor) {
        connectionSession.setAttribute(ConnectionSessionConstants.COLUMN_ACCESSOR_KEY, columnAccessor);
    }

    public static Object getColumnAccessor(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getAttribute(ConnectionSessionConstants.COLUMN_ACCESSOR_KEY);
    }

    public static String getConsoleSessionTimeZone(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.SESSION_TIME_ZONE);
    }

    public static void setQueryCache(@NonNull ConnectionSession connectionSession, @NonNull VirtualTable virtualTable) {
        setQueryCache(connectionSession, virtualTable, virtualTable.tableId());
    }

    @SuppressWarnings("all")
    public static void setQueryCache(@NonNull ConnectionSession connectionSession, @NonNull VirtualTable virtualTable,
            @NonNull String tableId) {
        Map<String, VirtualTable> queryCache =
                (Map<String, VirtualTable>) connectionSession.getAttribute(ConnectionSessionConstants.QUERY_CACHE_KEY);
        if (queryCache == null) {
            queryCache = new HashMap<>();
        }
        queryCache.put(tableId, virtualTable);
        connectionSession.setAttribute(ConnectionSessionConstants.QUERY_CACHE_KEY, queryCache);
    }

    @SuppressWarnings("all")
    public static VirtualTable getQueryCache(@NonNull ConnectionSession connectionSession, @NonNull String sqlId) {
        Map<String, VirtualTable> queryCache =
                (Map<String, VirtualTable>) connectionSession.getAttribute(ConnectionSessionConstants.QUERY_CACHE_KEY);
        if (queryCache == null) {
            return null;
        }
        return queryCache.get(sqlId);
    }

    public static void setBinaryDataManager(@NonNull ConnectionSession connectionSession,
            @NonNull BinaryDataManager dataManager) {
        connectionSession.setAttribute(ConnectionSessionConstants.BINARY_FILE_MANAGER_KEY, dataManager);
    }

    public static BinaryDataManager getBinaryDataManager(@NonNull ConnectionSession connectionSession) {
        return (BinaryDataManager) connectionSession.getAttribute(ConnectionSessionConstants.BINARY_FILE_MANAGER_KEY);
    }

    @SuppressWarnings("all")
    public static void setBinaryContentMetadata(@NonNull ConnectionSession connectionSession, String key,
            BinaryContentMetaData data) {
        Map<String, BinaryContentMetaData> metadataCache = (Map<String, BinaryContentMetaData>) connectionSession
                .getAttribute(ConnectionSessionConstants.BINARY_CONTENT_METADATA_CACHE);
        if (metadataCache == null) {
            metadataCache = new HashMap<>();
            connectionSession.setAttribute(ConnectionSessionConstants.BINARY_CONTENT_METADATA_CACHE, metadataCache);
        }
        metadataCache.put(key, data);
    }

    @SuppressWarnings("all")
    public static BinaryContentMetaData getBinaryContentMetadata(@NonNull ConnectionSession connectionSession,
            String key) {
        Map<String, BinaryContentMetaData> attribute = (Map<String, BinaryContentMetaData>) connectionSession
                .getAttribute(ConnectionSessionConstants.BINARY_CONTENT_METADATA_CACHE);
        return attribute == null ? null : attribute.getOrDefault(key, null);
    }

    public static File getSessionWorkingDir() throws IOException {
        return new File(getOrCreateFullPathAppendingSuffixToDataPath(""));
    }

    public static File getSessionWorkingDir(@NonNull ConnectionSession connectionSession) throws IOException {
        return new File(getOrCreateFullPathAppendingSuffixToDataPath(getUniqueIdentifier(connectionSession)));
    }

    public static File getSessionUploadDir(@NonNull ConnectionSession connectionSession) throws IOException {
        return getSessionWorkingDir(connectionSession, ConnectionSessionConstants.SESSION_UPLOAD_DIR_NAME);
    }

    public static File getSessionDataManagerDir(@NonNull ConnectionSession connectionSession) throws IOException {
        return getSessionWorkingDir(connectionSession, ConnectionSessionConstants.SESSION_DATABINARY_DIR_NAME);
    }

    public static DialectType getDialectType(@NonNull Statement statement) throws SQLException {
        String dialectQuerySql = "SHOW VARIABLES LIKE 'ob_compatibility_mode'";
        try (ResultSet resultSet = statement.executeQuery(dialectQuerySql)) {
            if (!resultSet.next()) {
                return DialectType.OB_MYSQL;
            }
            String str = resultSet.getString("VALUE").toUpperCase();
            return DialectType.valueOf(StringUtils.startsWithIgnoreCase(str, "ob") ? str : "OB_" + str);
        }
    }

    public static void initArchitecture(@NonNull ConnectionSession connectionSession) {
        if (!connectionSession.getDialectType().isOracle()) {
            return;
        }
        String sql = "select dbms_utility.port_string() from dual";
        try {
            String arch = getSyncJdbcExecutor(connectionSession).query(sql, rs -> {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            });
            Verify.notNull(arch, "Architecture");
            connectionSession.setAttribute(ConnectionSessionConstants.OB_ARCHITECTURE, arch);
            log.debug("Init architecture completed.");
        } catch (Exception e) {
            log.warn("Query architecture failed, errMsg={}", e.getMessage());
        }
    }

    public static String getNlsDateFormat(@NonNull ConnectionSession session) {
        Object value = session.getAttribute(ConnectionSessionConstants.NLS_DATE_FORMAT_NAME);
        return value == null ? null : value.toString();
    }

    public static String queryNlsDateFormat(@NonNull ConnectionSession session) {
        JdbcOperations jdbc = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        return jdbc.queryForObject("SHOW VARIABLES LIKE 'nls_date_format'", (rs, rowNum) -> rs.getString(2));
    }

    public static void setNlsDateFormat(@NonNull ConnectionSession session, @NonNull String format) {
        session.setAttribute(ConnectionSessionConstants.NLS_DATE_FORMAT_NAME, format);
    }

    public static String getNlsTimestampFormat(@NonNull ConnectionSession session) {
        Object value = session.getAttribute(ConnectionSessionConstants.NLS_TIMESTAMP_FORMAT_NAME);
        return value == null ? null : value.toString();
    }

    public static Map<String, String> queryAllSessionVariables(@NonNull ConnectionSession session) {
        JdbcOperations jdbc = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        Map<String, String> map = new HashMap<>();
        jdbc.query("SHOW SESSION VARIABLES", (rs, rowNum) -> new String[] {
                rs.getString(1),
                rs.getString(2)
        }).forEach(strings -> map.putIfAbsent(strings[0].toLowerCase(), strings[1]));
        return map;
    }

    public static String queryNlsTimestampFormat(@NonNull ConnectionSession session) {
        JdbcOperations jdbc = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        return jdbc.queryForObject("SHOW VARIABLES LIKE 'nls_timestamp_format'", (rs, rowNum) -> rs.getString(2));
    }

    public static void setNlsTimestampFormat(@NonNull ConnectionSession session, @NonNull String format) {
        session.setAttribute(ConnectionSessionConstants.NLS_TIMESTAMP_FORMAT_NAME, format);
    }

    public static String getNlsTimestampTZFormat(@NonNull ConnectionSession session) {
        Object value = session.getAttribute(ConnectionSessionConstants.NLS_TIMESTAMP_TZ_FORMAT_NAME);
        return value == null ? null : value.toString();
    }

    public static String queryNlsTimestampTZFormat(@NonNull ConnectionSession session) {
        JdbcOperations jdbc = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        return jdbc.queryForObject("SHOW VARIABLES LIKE 'nls_timestamp_tz_format'", (rs, rowNum) -> rs.getString(2));
    }

    public static void setNlsTimestampTZFormat(@NonNull ConnectionSession session, @NonNull String format) {
        session.setAttribute(ConnectionSessionConstants.NLS_TIMESTAMP_TZ_FORMAT_NAME, format);
    }

    public static void initConsoleSessionTimeZone(@NonNull ConnectionSession connectionSession,
            @NonNull String defaultTimeZone) {
        try {
            if (DialectType.OB_ORACLE == connectionSession.getDialectType()) {
                String timeZoneStr = getSyncJdbcExecutor(connectionSession).execute(
                        (StatementCallback<String>) stmt -> ((OceanBaseConnection) stmt.getConnection())
                                .getSessionTimeZone());
                Verify.notNull(timeZoneStr, "TimeZone can not be null");
                connectionSession.setAttribute(ConnectionSessionConstants.SESSION_TIME_ZONE, timeZoneStr);
            } else if (DialectType.ORACLE == connectionSession.getDialectType()) {
                connectionSession.setAttribute(ConnectionSessionConstants.SESSION_TIME_ZONE,
                        TimeZone.getDefault().getID());
            }
        } catch (Exception exception) {
            log.warn("Failed to get time zone, session={}", connectionSession, exception);
            connectionSession.setAttribute(ConnectionSessionConstants.SESSION_TIME_ZONE, defaultTimeZone);
        }
    }

    public static void killQueryByDirectConnect(@NonNull String connectionId,
            @NonNull CloneableDataSourceFactory dataSourceFactory) throws Exception {
        List<OdcDBSession> sessionList;
        DataSource dataSource = dataSourceFactory.getDataSource();
        try {
            SyncJdbcExecutor jdbcExecutor = new GeneralSyncJdbcExecutor(dataSource);
            sessionList = jdbcExecutor.query("show full processlist", new OdcDBSessionRowMapper());
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
        Verify.notEmpty(sessionList, "DbSessions");

        OdcDBSession session = sessionList.stream().filter(s -> s.getSessionId().equals(connectionId))
                .findFirst().orElseThrow(() -> new UnexpectedException("connectionId is not found, " + connectionId));
        String svrIp = session.getSvrIp();
        Verify.notEmpty(svrIp, "ObserverIp");
        String[] ipPort = svrIp.split(":");
        Verify.verify(ipPort.length == 2, String.format("incorrect observer ip address %s", svrIp));

        CloneableDataSourceFactory factory = dataSourceFactory.deepCopy();
        factory.resetHost(origin -> ipPort[0]);
        factory.resetPort(origin -> Integer.parseInt(ipPort[1]));
        factory.resetUsername(origin -> {
            int index = origin.indexOf("#");
            if (index < 0) {
                return origin;
            }
            return origin.substring(0, index);
        });
        dataSource = factory.getDataSource();
        try {
            SyncJdbcExecutor jdbcExecutor = new GeneralSyncJdbcExecutor(dataSource);
            jdbcExecutor.execute("KILL QUERY " + connectionId);
        } catch (Exception e) {
            log.warn("Direct connection termination query attempt failed, connectionId={}", connectionId, e);
            throw e;
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
    }

    public static Long getRuleSetId(@NonNull ConnectionSession connectionSession) {
        return (Long) connectionSession.getAttribute(ConnectionSessionConstants.RULE_SET_ID_NAME);
    }

    public static void setRuleSetId(@NonNull ConnectionSession connectionSession, @NonNull Long ruleSetId) {
        connectionSession.setAttribute(ConnectionSessionConstants.RULE_SET_ID_NAME, ruleSetId);
    }

    public static String getConsoleConnectionId(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.CONNECTION_ID_KEY);
    }

    public static String getConsoleConnectionProxySessId(@NonNull ConnectionSession connectionSession) {
        Object proxySessId = connectionSession.getAttribute(ConnectionSessionConstants.OB_PROXY_SESSID_KEY);
        return proxySessId == null ? null : (String) proxySessId;
    }

    public static String getVersion(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.OB_VERSION);
    }

    public static String getArchitecture(@NonNull ConnectionSession connectionSession) {
        return (String) connectionSession.getAttribute(ConnectionSessionConstants.OB_ARCHITECTURE);
    }

    public static String getUniqueIdentifier(@NonNull ConnectionSession connectionSession) {
        return HashUtils.md5(connectionSession.getId()).replace("-", "");
    }

    private static String getOrCreateFullPathAppendingSuffixToDataPath(@NonNull String suffix) throws IOException {
        String dataDir = SystemUtils.getEnvOrProperty("file.storage.dir");
        if (dataDir == null) {
            dataDir = "./data/connection_session";
        } else {
            dataDir += dataDir.endsWith("/") ? "connection_session" : "/connection_session";
        }
        File absolutePath = new File(dataDir);
        File file = new File(
                absolutePath.getAbsolutePath() + "/" + (suffix.startsWith("/") ? suffix.substring(1) : suffix));
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IOException("Failed to create dir, " + file.getAbsolutePath());
            }
        }
        return file.getAbsolutePath();
    }

    private static File getSessionWorkingDir(@NonNull ConnectionSession connectionSession, @NonNull String suffix)
            throws IOException {
        String realSuffix = suffix.startsWith("/") ? suffix.substring(1) : suffix;
        String key = getUniqueIdentifier(connectionSession);
        return new File(getOrCreateFullPathAppendingSuffixToDataPath(key + "/" + realSuffix));
    }

    private static SyncJdbcExecutor getSyncJdbcExecutor(ConnectionSession session) {
        return session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
    }

}
