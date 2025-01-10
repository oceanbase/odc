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
package com.oceanbase.odc.service.db.session;

import static com.oceanbase.odc.core.shared.constant.DialectType.OB_MYSQL;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.db.browser.DBStatsAccessors;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DefaultDBSessionManage implements DBSessionManageFacade {

    private static final String SERVER_REGEX = ".*/\\*(?<ip>([0-9]{1,3}.){1,3}([0-9]{1,3})):"
            + "(?<port>[0-9]{1,5})\\*/.*";
    private static final Pattern SERVER_PATTERN = Pattern.compile(SERVER_REGEX);
    private static final ConnectionMapper CONNECTION_MAPPER = ConnectionMapper.INSTANCE;
    private static final String GLOBAL_CLIENT_SESSION_OB_PROXY_VERSION_NUMBER = "4.2.3";
    private static final String GLOBAL_CLIENT_SESSION_OB_VERSION_NUMBER = "4.2.5";
    private static final String ORACLE_MODEL_KILL_SESSION_WITH_BLOCK_OB_VERSION_NUMBER = "4.2.1.0";
    private static final byte GLOBAL_CLIENT_SESSION_PROXY_ID_MIN = 0;
    private static final short GLOBAL_CLIENT_SESSION_PROXY_ID_MAX = 8191;
    private static final byte GLOBAL_CLIENT_SESSION_ID_VERSION = 2;

    @Autowired
    private DBSessionService dbSessionService;

    @Autowired
    private ConnectionService connectionService;

    @Override
    @SkipAuthorize
    public List<KillResult> killSessionOrQuery(KillSessionOrQueryReq request) {
        Verify.notNull(request.getSessionIds(), "session can not be null");
        Verify.notNull(request.getDatasourceId(), "connection session id can not be null");
        ConnectionConfig connectionConfig = connectionService.getForConnect(Long.valueOf(request.getDatasourceId()));
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connectionConfig);
        ConnectionSession connectionSession = null;
        try {
            connectionSession = factory.generateSession();
            Map<String, String> connectionId2KillSql;
            SessionExtensionPoint sessionExtension =
                    ConnectionPluginUtil.getSessionExtension(connectionConfig.getDialectType());
            if (KillSessionOrQueryReq.KILL_QUERY_TYPE.equals(request.getKillType())) {
                connectionId2KillSql = sessionExtension.getKillQuerySqls(new HashSet<>(request.getSessionIds()));
            } else {
                connectionId2KillSql = sessionExtension.getKillSessionSqls(new HashSet<>(request.getSessionIds()));
            }
            return doKill(connectionSession, connectionId2KillSql);
        } catch (Exception e) {
            log.info("kill session failed,datasourceId#{}", request.getDatasourceId(), e);
            throw e;
        } finally {
            if (connectionSession != null) {
                connectionSession.expire();
            }
        }
    }

    @Override
    public boolean supportKillConsoleQuery(ConnectionSession session) {
        if (Objects.nonNull(session) && ConnectionSessionUtil.isLogicalSession(session)) {
            return false;
        }
        return true;
    }

    @Override
    @SneakyThrows
    @SkipAuthorize
    public boolean killConsoleQuery(ConnectionSession session) {
        String connectionId = ConnectionSessionUtil.getConsoleConnectionId(session);
        Verify.notNull(connectionId, "ConnectionId");
        ConnectionConfig conn = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        Verify.notNull(conn, "ConnectionConfig");
        SessionExtensionPoint sessionExtension =
                ConnectionPluginUtil.getSessionExtension(conn.getDialectType());
        Map<String, String> connectionId2KillSql = sessionExtension.getKillQuerySqls(SetUtils.hashSet(connectionId));
        List<KillResult> results = doKill(session, connectionId2KillSql);
        Verify.singleton(results, "killResults");
        return results.get(0).isKilled();
    }

    @Override
    @SkipAuthorize("odc internal usage")
    public void killAllSessions(ConnectionSession connectionSession, Predicate<OdcDBSession> filter,
            Integer lockTableTimeOutSeconds) {
        log.info("Query sessions to killed");
        // Get backend connection session id
        List<OdcDBSession> list = getSessionList(connectionSession, filter);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        log.info("kill sessions = [{}]", list);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // max kill 1024 sessions by once and filter current session
            log.info("Begin kill all session");
            waitingForResult(() -> doKillAllSessions(list, connectionSession, executor), lockTableTimeOutSeconds);
        } finally {
            try {
                executor.shutdownNow();
            } catch (Exception ignored) {
                // do nothing
            }
        }

        List<OdcDBSession> checkedList = getSessionList(connectionSession, filter);
        if (CollectionUtils.isNotEmpty(checkedList)) {
            throw new IllegalStateException("kill session failed, has session reserved");
        }
    }

    private List<KillResult> doKill(ConnectionSession session, Map<String, String> connectionId2KillSqls) {
        List<SqlTupleSessionId> sqlTupleSessionIds = connectionId2KillSqls.entrySet().stream().map(entry -> {
            String connectionId = entry.getKey();
            String killSql = entry.getValue();
            return new SqlTupleSessionId(SqlTuple.newTuple(killSql), connectionId);
        }).collect(Collectors.toList());

        Map<String, String> sqlId2SessionId = sqlTupleSessionIds.stream().collect(
                Collectors.toMap(s -> s.getSqlTuple().getSqlId(), SqlTupleSessionId::getSessionId));

        List<JdbcGeneralResult> jdbcGeneralResults = executeSqls(session, sqlTupleSessionIds.stream()
                .map(SqlTupleSessionId::getSqlTuple)
                .collect(Collectors.toList()));
        if (session.getDialectType().isOceanbase()) {
            jdbcGeneralResults = additionalKillIfNecessary(session, jdbcGeneralResults, sqlTupleSessionIds);
        }
        return jdbcGeneralResults.stream()
                .map(res -> new KillResult(res, sqlId2SessionId.get(res.getSqlTuple().getSqlId())))
                .collect(Collectors.toList());
    }

    private List<OdcDBSession> getSessionList(ConnectionSession connectionSession, Predicate<OdcDBSession> filter) {
        return DBStatsAccessors.create(connectionSession)
                .listAllSessions()
                .stream()
                .map(OdcDBSession::from)
                .filter(filter == null ? a -> true : filter)
                .collect(Collectors.toList());
    }

    private List<JdbcGeneralResult> executeSqls(ConnectionSession connectionSession, List<SqlTuple> sqlTuples) {
        List<JdbcGeneralResult> results =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute(new OdcStatementCallBack(sqlTuples, connectionSession, true, null, false));
        if (results == null) {
            log.warn("Execution of the kill session command failed with unknown error, sql={}", sqlTuples);
            throw new InternalServerError("Unknown error");
        }
        return results;
    }

    /**
     * process the execution result after the first kill commands. if the result contains unknown thread
     * id exception, try to use other solutions to execute the kill commands.
     * 
     * @param connectionSession
     * @param results
     * @return
     */
    private List<JdbcGeneralResult> additionalKillIfNecessary(ConnectionSession connectionSession,
            List<JdbcGeneralResult> results, List<SqlTupleSessionId> sqlTupleSessionIds) {
        Map<String, ServerAddress> sessionId2SvrAddr =
                getSessionList(connectionSession, null).stream().collect(
                        Collectors.toMap(OdcDBSession::getSessionId,
                                s -> extractServerAddress(MoreObjects.firstNonNull(s.getSvrIp(), ""))));
        Map<String, String> sqlId2SessionId = sqlTupleSessionIds.stream().collect(
                Collectors.toMap(s -> s.getSqlTuple().getSqlId(), SqlTupleSessionId::getSessionId));

        Boolean isDirectedOBServer = isObServerDirected(connectionSession);
        String obProxyVersion = null;
        if (Boolean.FALSE.equals(isDirectedOBServer)) {
            obProxyVersion = ConnectionSessionUtil.getObProxyVersion(connectionSession);
        }
        String obVersion = ConnectionSessionUtil.getVersion(connectionSession);
        boolean isEnabledGlobalClientSession =
                isGlobalClientSessionEnabled(connectionSession, obProxyVersion, obVersion);
        boolean isSupportedOracleModeKillSession = isOracleModeKillSessionSupported(obVersion, connectionSession);
        List<JdbcGeneralResult> finalResults = new ArrayList<>();
        for (JdbcGeneralResult jdbcGeneralResult : results) {
            try {
                jdbcGeneralResult.getQueryResult();
            } catch (Exception e) {
                if (isUnknownThreadIdError(e)) {
                    jdbcGeneralResult = handleUnknownThreadIdError(connectionSession,
                            jdbcGeneralResult, isDirectedOBServer,
                            isEnabledGlobalClientSession, isSupportedOracleModeKillSession,
                            sessionId2SvrAddr.getOrDefault(
                                    sqlId2SessionId.get(jdbcGeneralResult.getSqlTuple().getSqlId()), null));
                } else {
                    log.warn("Failed to execute sql in kill session scenario, sqlTuple={}",
                            jdbcGeneralResult.getSqlTuple(), e);
                }
            }
            finalResults.add(jdbcGeneralResult);
        }
        return finalResults;
    }

    /**
     * Check whether client directed oceanbase database server. If an exception occurs or the version
     * does not support, return null.
     *
     * @param connectionSession
     * @return
     */
    private Boolean isObServerDirected(ConnectionSession connectionSession) {
        String sql = (connectionSession.getDialectType() == OB_MYSQL)
                ? "select PROXY_SESSID from oceanbase.gv$ob_processlist where ID =(select connection_id());"
                : "select PROXY_SESSID from gv$ob_processlist where ID =(select sys_context('userenv','sid') from dual);";
        try {
            List<String> proxySessids = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                    .query(sql, (rs, rowNum) -> rs.getString("PROXY_SESSID"));
            if (proxySessids != null && proxySessids.size() == 1) {
                return proxySessids.get(0) == null;
            }
        } catch (Exception e) {
            log.warn("Failed to obtain the PROXY_SESSID: {}", e.getMessage());
        }
        // Return null if the version is not supported or an exception occurs
        return null;
    }

    private boolean isGlobalClientSessionEnabled(ConnectionSession connectionSession, String obProxyVersion,
            String obVersion) {
        // verification version requirement
        if (StringUtils.isBlank(obProxyVersion)
                || StringUtils.isBlank(obVersion)
                || VersionUtils.isLessThan(obProxyVersion, GLOBAL_CLIENT_SESSION_OB_PROXY_VERSION_NUMBER)
                || VersionUtils.isLessThan(obVersion, GLOBAL_CLIENT_SESSION_OB_VERSION_NUMBER)) {
            return false;
        }
        try {
            Integer proxyId = getOBProxyConfig(connectionSession, "proxy_id");
            Integer clientSessionIdVersion = getOBProxyConfig(connectionSession, "client_session_id_version");

            return proxyId != null
                    && proxyId >= GLOBAL_CLIENT_SESSION_PROXY_ID_MIN
                    && proxyId <= GLOBAL_CLIENT_SESSION_PROXY_ID_MAX
                    && clientSessionIdVersion != null
                    && clientSessionIdVersion == GLOBAL_CLIENT_SESSION_ID_VERSION;
        } catch (Exception e) {
            log.warn("Failed to determine if global client session is enabled: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the value of OBProxy's configuration variable If an exception occurs or the version does not
     * support, return null.
     * 
     * @param connectionSession
     * @param configName
     * @return
     */
    private Integer getOBProxyConfig(ConnectionSession connectionSession, String configName) {
        try {
            return connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                    .query("show proxyconfig like '" + configName + "';",
                            rs -> rs.next() ? rs.getInt("value") : null);
        } catch (Exception e) {
            log.warn("Failed to obtain the value of OBProxy's configuration variable: {}", e.getMessage());
            return null;
        }
    }

    private boolean isUnknownThreadIdError(Exception e) {
        return StringUtils.containsIgnoreCase(e.getMessage(), "Unknown thread id");
    }

    private JdbcGeneralResult handleUnknownThreadIdError(ConnectionSession connectionSession,
            JdbcGeneralResult jdbcGeneralResult, Boolean isDirectedOBServer,
            boolean isEnabledGlobalClientSession, boolean isSupportedOracleModeKillSession,
            ServerAddress directServerAddress) {
        if (Boolean.TRUE.equals(isDirectedOBServer)) {
            log.info("The current connection mode is directing observer, return error result directly");
            return jdbcGeneralResult;
        }
        if (isEnabledGlobalClientSession) {
            log.info("The OBProxy has enabled the global client session, return error result directly");
            return jdbcGeneralResult;
        }
        if (isSupportedOracleModeKillSession) {
            return tryKillSessionByAnonymousBlock(connectionSession, jdbcGeneralResult,
                    jdbcGeneralResult.getSqlTuple());
        }
        return tryKillSessionViaDirectConnectObServer(connectionSession, jdbcGeneralResult,
                jdbcGeneralResult.getSqlTuple(), directServerAddress);
    }

    private boolean isOracleModeKillSessionSupported(String obVersion, ConnectionSession connectionSession) {
        return StringUtils.isNotBlank(obVersion) &&
                VersionUtils.isGreaterThanOrEqualsTo(obVersion, ORACLE_MODEL_KILL_SESSION_WITH_BLOCK_OB_VERSION_NUMBER)
                &&
                connectionSession.getDialectType() == DialectType.OB_ORACLE;
    }

    /**
     * Try to kill session by using anonymous code blocks. If successful, return a successful
     * jdbcGeneralResult, otherwise return the original jdbcGeneralResult.
     *
     * @param connectionSession
     * @param jdbcGeneralResult
     * @param sqlTuple
     * @return
     */
    private JdbcGeneralResult tryKillSessionByAnonymousBlock(ConnectionSession connectionSession,
            JdbcGeneralResult jdbcGeneralResult, SqlTuple sqlTuple) {
        log.info("Kill query/session Unknown thread id error, try anonymous code blocks");
        String executedSql = sqlTuple.getExecutedSql();
        if (executedSql != null && executedSql.endsWith(";")) {
            executedSql = executedSql.substring(0, executedSql.length() - 1);
        }
        String anonymousCodeBlock = "BEGIN\n"
                + "EXECUTE IMMEDIATE '"
                + executedSql
                + "';\n"
                + "END;";
        try {
            connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                    .execute(anonymousCodeBlock);
            return JdbcGeneralResult.successResult(sqlTuple);

        } catch (Exception e) {
            log.warn("Failed to kill session by using anonymous code blocks: {}", e.getMessage());
            return jdbcGeneralResult;
        }
    }

    /**
     * Try to kill session by direct connect observer. If successful, return a successful
     * jdbcGeneralResult, otherwise return the original jdbcGeneralResult.
     *
     * @param connectionSession
     * @param jdbcGeneralResult
     * @param sqlTuple
     * @return
     */
    private JdbcGeneralResult tryKillSessionViaDirectConnectObServer(ConnectionSession connectionSession,
            JdbcGeneralResult jdbcGeneralResult, SqlTuple sqlTuple, ServerAddress serverAddress) {
        try {
            log.info("Kill query/session Unknown thread id error, try direct connect observer");
            directLinkServerAndExecute(sqlTuple.getExecutedSql(), connectionSession, serverAddress);
            return JdbcGeneralResult.successResult(sqlTuple);
        } catch (Exception e) {
            log.warn("Failed to direct connect observer {}", e.getMessage());
            return jdbcGeneralResult;
        }
    }

    private void directLinkServerAndExecute(String sql, ConnectionSession session, ServerAddress serverAddress)
            throws Exception {
        if (Objects.isNull(serverAddress)) {
            throw new Exception("ServerAddress not found");
        }
        // create a connection
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        ConnectionConfig directConnectConfig = CONNECTION_MAPPER.clone(connectionConfig);
        directConnectConfig.setHost(serverAddress.getIpAddress());
        directConnectConfig.setPort(Integer.parseInt(serverAddress.getPort()));
        directConnectConfig.setClusterName(null);
        OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(directConnectConfig, null);

        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource();
                Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    // extract text(query from the dictionary) to server address(ip, port)
    // the text is expected be like 0.0.0.0:8888
    private ServerAddress extractServerAddress(String text) {
        String trimmed = StringUtils.trim(text);
        if (StringUtils.isBlank(trimmed)) {
            log.info("unable to extract server address, text is empty");
            return null;
        }
        Matcher matcher = SERVER_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            log.info("unable to extract server address, does not match pattern");
            return null;
        }
        String ipAddress = matcher.group("ip");
        String port = matcher.group("port");
        if (StringUtils.isEmpty(ipAddress) || StringUtils.isEmpty(port)) {
            log.info("unable to extract server address, ipAddress={}, port={}", ipAddress, port);
            return null;
        }
        return new ServerAddress(ipAddress, port);
    }

    private CompletableFuture<Void> doKillAllSessions(List<OdcDBSession> list, ConnectionSession connectionSession,
            Executor executor) {
        return CompletableFuture.supplyAsync((Supplier<Void>) () -> {
            Lists.partition(list, 1024)
                    .forEach(sessionList -> {
                        SessionExtensionPoint sessionExtension =
                                ConnectionPluginUtil.getSessionExtension(connectionSession.getDialectType());
                        Map<String, String> connectionId2KillSql = sessionExtension.getKillQuerySqls(
                                sessionList.stream().map(OdcDBSession::getSessionId).collect(Collectors.toSet()));
                        doKill(connectionSession, connectionId2KillSql);
                    });
            return null;
        }, executor).exceptionally(ex -> {
            throw new CompletionException(ex);
        });
    }

    private <T> void waitingForResult(Supplier<CompletableFuture<T>> completableFutureSupplier,
            Integer lockTableTimeOutSeconds) {
        try {
            int killSessionWaitSeconds = lockTableTimeOutSeconds == null ? 3 : lockTableTimeOutSeconds;

            completableFutureSupplier.get().get(killSessionWaitSeconds, TimeUnit.SECONDS);
            log.info("Successfully kill all sessions");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Kill all sessions occur error", e);
            throw new IllegalStateException("Kill all sessions occur error", e);
        }
    }

    @Data
    static class ServerAddress {
        String ipAddress;
        String port;

        public ServerAddress(String ipAddress, String port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
    }

    @AllArgsConstructor
    @Data
    @NoArgsConstructor
    class SqlTupleSessionId {
        private SqlTuple sqlTuple;
        private String sessionId;
    }
}
