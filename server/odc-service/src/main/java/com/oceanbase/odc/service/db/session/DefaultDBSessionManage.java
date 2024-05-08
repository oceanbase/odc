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

import java.sql.Connection;
import java.sql.Statement;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionInfoUtil;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.db.browser.DBStatsAccessors;
import com.oceanbase.odc.service.db.session.DBSessionService.SessionIdKillSql;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DefaultDBSessionManage implements DBSessionManageFacade {

    private static final String SERVER_REGEX = ".*/\\*(?<ip>([0-9]{1,3}.){1,3}([0-9]{1,3})):"
            + "(?<port>[0-9]{1,5})\\*/.*";
    private static final Pattern SERVER_PATTERN = Pattern.compile(SERVER_REGEX);
    private static final ConnectionMapper CONNECTION_MAPPER = ConnectionMapper.INSTANCE;

    @Autowired
    private ConnectSessionService sessionService;

    @Autowired
    private DBSessionService dbSessionService;

    @Autowired
    private ConnectionService connectionService;

    @Override
    @SkipAuthorize
    public List<KillSessionResult> killSessionOrQuery(KillSessionOrQueryReq request) {
        Verify.notNull(request.getSessionIds(), "session can not be null");
        Verify.notNull(request.getDatasourceId(), "connection session id can not be null");
        ConnectionConfig connectionConfig = connectionService.getForConnect(Long.valueOf(request.getDatasourceId()));
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connectionConfig);
        ConnectionSession connectionSession = null;
        try {
            connectionSession = factory.generateSession();
            List<SessionIdKillSql> session =
                    dbSessionService.getKillSql(connectionSession, request.getSessionIds(), request.getKillType());
            return doKillSessionOrQuery(connectionSession, session);
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
    @SneakyThrows
    @SkipAuthorize
    public boolean killCurrentQuery(ConnectionSession session) {
        String connectionId = ConnectionSessionUtil.getConsoleConnectionId(session);
        Verify.notNull(connectionId, "ConnectionId");
        ConnectionConfig conn = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        Verify.notNull(conn, "ConnectionConfig");
        DruidDataSourceFactory factory = new DruidDataSourceFactory(conn);
        try {
            ConnectionInfoUtil.killQuery(connectionId, factory, session.getDialectType());
        } catch (Exception e) {
            if (session.getDialectType().isOceanbase()) {
                ConnectionSessionUtil.killQueryByDirectConnect(connectionId, factory);
                log.info("Kill query by direct connect succeed, connectionId={}", connectionId);
            } else {
                log.warn("Kill query occur error, connectionId={}", connectionId, e);
                return false;
            }
        }
        return true;
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

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // max kill 1024 sessions by once and filter current session
            log.info("Begin kill all session");
            waitingForResult(
                    () -> doKillAllSessions(
                            list,
                            connectionSession,
                            executor),
                    lockTableTimeOutSeconds);
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

    private List<OdcDBSession> getSessionList(ConnectionSession connectionSession, Predicate<OdcDBSession> filter) {
        return DBStatsAccessors.create(connectionSession)
                .listAllSessions()
                .stream()
                .map(OdcDBSession::from)
                .filter(filter == null ? a -> true : filter)
                .collect(Collectors.toList());
    }

    private List<KillSessionResult> doKillSessionOrQuery(
            ConnectionSession connectionSession, List<SessionIdKillSql> killSessionSqls) {
        List<SqlTupleSessionId> sqlTupleSessionIds = killSessionSqls.stream().map(
                s -> new SqlTupleSessionId(SqlTuple.newTuple(s.getKillSql()), s.getSessionId()))
                .collect(Collectors.toList());
        Map<String, String> sqlId2SessionId = sqlTupleSessionIds.stream().collect(
                Collectors.toMap(s -> s.getSqlTuple().getSqlId(), SqlTupleSessionId::getSessionId));

        List<SqlTuple> sqlTuples =
                sqlTupleSessionIds.stream().map(SqlTupleSessionId::getSqlTuple).collect(Collectors.toList());
        List<JdbcGeneralResult> jdbcGeneralResults =
                executeKillSession(connectionSession, sqlTuples, sqlTuples.toString());
        return jdbcGeneralResults.stream()
                .map(res -> new KillSessionResult(res, sqlId2SessionId.get(res.getSqlTuple().getSqlId())))
                .collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public List<JdbcGeneralResult> executeKillSession(ConnectionSession connectionSession, List<SqlTuple> sqlTuples,
            String sqlScript) {
        List<JdbcGeneralResult> results =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute(new OdcStatementCallBack(sqlTuples, connectionSession, true, null, false));
        if (results == null) {
            log.warn("Execution of the kill session command failed with unknown error, sql={}", sqlScript);
            throw new InternalServerError("Unknown error");
        }
        return results.stream().map(jdbcGeneralResult -> {
            SqlTuple sqlTuple = jdbcGeneralResult.getSqlTuple();
            try {
                jdbcGeneralResult.getQueryResult();
            } catch (Exception e) {
                if (StringUtils.contains(e.getMessage(), "Unknown thread id")) {
                    try {
                        log.info("Kill query/session Unknown thread id error, try direct connect observer");
                        directLinkServerAndExecute(sqlTuple.getExecutedSql(),
                                connectionSession);
                        return JdbcGeneralResult.successResult(sqlTuple);
                    } catch (Exception e1) {
                        log.warn("Failed to direct connect observer {}", e1.getMessage());
                    }
                } else {
                    log.warn("Failed to execute sql in kill session scenario, sqlTuple={}", sqlTuple, e);
                }
            }
            return jdbcGeneralResult;
        }).collect(Collectors.toList());
    }

    private void directLinkServerAndExecute(String sql, ConnectionSession session)
            throws Exception {
        ServerAddress serverAddress = extractServerAddress(sql);
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

    ServerAddress extractServerAddress(String sql) {
        String removeBlank = org.apache.commons.lang3.StringUtils.replace(sql, "\\s+", "");
        if (org.apache.commons.lang3.StringUtils.isBlank(removeBlank)) {
            log.debug("unable to extract server address, sql was empty");
            return null;
        }
        int startPos = org.apache.commons.lang3.StringUtils.indexOf(removeBlank, "/*");
        if (-1 == startPos) {
            log.debug("unable to extract server address, no comment found");
            return null;
        }
        String subStr = org.apache.commons.lang3.StringUtils.substring(removeBlank, startPos);
        Matcher matcher = SERVER_PATTERN.matcher(subStr);
        if (!matcher.matches()) {
            log.info("unable to extract server address, does not match pattern");
            return null;
        }
        String ipAddress = matcher.group("ip");
        String port = matcher.group("port");
        if (org.apache.commons.lang3.StringUtils.isEmpty(ipAddress)
                || org.apache.commons.lang3.StringUtils.isEmpty(port)) {
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
                        doKillSessionOrQuery(connectionSession, getKillSql(sessionList));
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

    private List<SessionIdKillSql> getKillSql(@NonNull List<OdcDBSession> allSession) {
        return allSession.stream()
                .map(dbSession -> {
                    StringBuilder sqlBuilder = new StringBuilder();
                    sqlBuilder.append("kill ");
                    sqlBuilder.append(dbSession.getSessionId());
                    if (dbSession.getSvrIp() != null) {
                        sqlBuilder.append(" /*").append(dbSession.getSvrIp()).append("*/");
                    }
                    return new SessionIdKillSql(dbSession.getSessionId(), sqlBuilder.append(";").toString());
                }).collect(Collectors.toList());
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
