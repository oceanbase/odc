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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionGroupContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionHandler;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionStatus;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionInfoUtil;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 21:04
 * @Description: []
 */
@Slf4j
public class SqlExecutionHandler implements ExecutionHandler<SqlExecuteReq, SqlExecutionResultWrapper> {
    private ConnectionSession connectionSession;
    private SqlExecuteReq req;

    public SqlExecutionHandler(@NonNull SqlExecuteReq req) {
        this.req = req;
    }

    @Override
    public ExecutionResult<SqlExecutionResultWrapper> beforeExecute(
            ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> context) {
        SqlExecutionResultWrapper resultWrapper =
                new SqlExecutionResultWrapper(req.getLogicalDatabaseId(), req.getPhysicalDatabaseId(),
                        req.getScheduleTaskId(), null);
        resultWrapper.setExecuteSql(req.getSql());
        resultWrapper.setStatus(SqlExecuteStatus.CREATED);
        return new ExecutionResult<>(resultWrapper, ExecutionStatus.PENDING, req.getOrder());
    }

    @Override
    public ExecutionResult<SqlExecutionResultWrapper> execute(
            ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> context)
            throws SQLException {
        this.connectionSession = generateSession(req.getConnectionConfig());
        try {
            OdcStatementCallBack statementCallBack = new OdcStatementCallBack(
                    Arrays.asList(SqlTuple.newTuple(req.getSql())), connectionSession, true, 1000);
            List<JdbcGeneralResult> results = connectionSession
                    .getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                    .execute((StatementCallback<List<JdbcGeneralResult>>) stmt -> {
                        stmt.setQueryTimeout(
                                (int) TimeUnit.MILLISECONDS.toSeconds(req.getTimeoutMillis()));
                        return statementCallBack.doInStatement(stmt);
                    });
            JdbcGeneralResult result = results.get(0);
            log.info("SqlExecutionCallback execute result, connectionReset={}", result.isConnectionReset());
            return new ExecutionResult<>(
                    new SqlExecutionResultWrapper(req.getLogicalDatabaseId(), req.getPhysicalDatabaseId(),
                            req.getScheduleTaskId(), new SqlExecuteResult(result)),
                    getExecutionStatus(result.getStatus()), req.getOrder());
        } finally {
            tryExpireConnectionSession(this.connectionSession);
        }

    }

    @Override
    public void terminate(ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> context)
            throws Exception {
        if (this.connectionSession == null || this.connectionSession.isExpired()) {
            log.warn("ConnectionSession is null or expired, skip terminate");
            return;
        }
        String connectionId = ConnectionSessionUtil.getConsoleConnectionId(connectionSession);
        Verify.notNull(connectionId, "ConnectionId");
        ConnectionConfig conn = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        Verify.notNull(conn, "ConnectionConfig");
        DruidDataSourceFactory factory = new DruidDataSourceFactory(conn);
        try {
            ConnectionInfoUtil.killQuery(connectionId, factory, connectionSession.getDialectType());
        } catch (Exception e) {
            if (connectionSession.getDialectType().isOceanbase()) {
                ConnectionSessionUtil.killQueryByDirectConnect(connectionId, factory);
                log.info("Kill query by direct connect succeed, connectionId={}", connectionId);
            } else {
                log.warn("Kill query occur error, connectionId={}", connectionId, e);
            }
        }
    }

    private ConnectionSession generateSession(@NonNull ConnectionConfig connectionConfig) {
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        sessionFactory.setSessionTimeoutMillis(this.req.getTimeoutMillis());
        ConnectionSession connectionSession = sessionFactory.generateSession();
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true,
                true);
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);
        ConnectionSessionUtil.setColumnAccessor(connectionSession, new DatasourceColumnAccessor(connectionSession));
        return connectionSession;
    }

    private void tryExpireConnectionSession(ConnectionSession connectionSession) {
        if (connectionSession != null && !connectionSession.isExpired()) {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }
    }

    private ExecutionStatus getExecutionStatus(@NonNull SqlExecuteStatus sqlStatus) {
        ExecutionStatus executionStatus = null;
        switch (sqlStatus) {
            case SUCCESS:
                executionStatus = ExecutionStatus.SUCCESS;
                break;
            case FAILED:
            case CANCELED:
                executionStatus = ExecutionStatus.FAILED;
                break;
            case RUNNING:
                executionStatus = ExecutionStatus.RUNNING;
                break;
            case CREATED:
                executionStatus = ExecutionStatus.PENDING;
                break;
        }
        return executionStatus;
    }
}
