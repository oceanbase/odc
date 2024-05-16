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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.common.util.TraceWatch;
import com.oceanbase.odc.common.util.TraceWatch.EditableTraceStage;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.CacheColumnPredicate;
import com.oceanbase.odc.core.sql.execute.cache.ResultSetCachedElementFactory;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.table.ResultSetVirtualTable;
import com.oceanbase.odc.core.sql.execute.mapper.DefaultJdbcRowMapper;
import com.oceanbase.odc.core.sql.execute.mapper.JdbcRowMapper;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.util.FullLinkTraceUtil;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * It is only applicable to the {@link StatementCallback} implementation of SQL executed by ODC
 * console
 *
 * @author yh263208
 * @date 2021-11-14 16:23
 * @since ODC_release_3.2.2
 */
@Getter
@Slf4j
public class OdcStatementCallBack implements StatementCallback<List<JdbcGeneralResult>> {
    /**
     * {@code ODC} refers to the {@code load_file()} function in the {@code Mysql} protocol when
     * processing large fields. The front end passes a special sql (eg.
     * {@code insert into "BLOB_TEST"("ID","C") values(3,load_file('eeode.txt'))}) containing the
     * {@code load_file()} function call. After the backend parses this function, the ps protocol is
     * changed by default, and the {@code #setBlob} method is used to set the value.
     */
    private static final Pattern OBJECT_VALUE_PATTERN =
            Pattern.compile("(?<!')(load_[a-zA-Z_]*file)\\('([a-zA-Z0-9_.\\-]+)'\\)(?!')", Pattern.CASE_INSENSITIVE);
    private final ConnectType connectType;
    private final DialectType dialectType;
    private final JdbcRowMapper rowDataMapper;
    private final boolean autoCommit;
    private final Integer queryLimit;
    private final List<SqlTuple> sqls;
    private final boolean stopWhenError;
    private final BinaryDataManager binaryDataManager;
    private final ConnectionSession connectionSession;
    private final AsyncExecuteContext context;
    private final List<SqlExecutionListener> listeners = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Setter
    private boolean useFullLinkTrace = false;
    @Setter
    private int fullLinkTraceTimeout = 60;
    @Setter
    private int maxCachedLines = 10000;
    @Setter
    private BiPredicate<Integer, ResultSetMetaData> cachePredicate = new CacheColumnPredicate();
    @Setter
    private long maxCachedSize = 1024 * 1024 * 1024; // 1 GB
    @Setter
    private Integer dbmsoutputMaxRows = null;
    @Setter
    private Locale locale;

    public OdcStatementCallBack(@NonNull List<SqlTuple> sqls, @NonNull ConnectionSession connectionSession,
            Boolean autoCommit, Integer queryLimit) {
        this(sqls, connectionSession, autoCommit, queryLimit, true);
    }

    public OdcStatementCallBack(@NonNull List<SqlTuple> sqls, @NonNull ConnectionSession connectionSession,
            Boolean autoCommit, Integer queryLimit, boolean stopWhenError) {
        this(sqls, connectionSession, autoCommit, queryLimit, stopWhenError, null);
    }

    public OdcStatementCallBack(@NonNull List<SqlTuple> sqls, @NonNull ConnectionSession connectionSession,
            Boolean autoCommit, Integer queryLimit, boolean stopWhenError, AsyncExecuteContext context) {
        this.sqls = sqls;
        this.autoCommit = autoCommit == null ? connectionSession.getDefaultAutoCommit() : autoCommit;
        this.connectType = connectionSession.getConnectType();
        this.dialectType = connectionSession.getDialectType();
        this.rowDataMapper = new DefaultJdbcRowMapper(connectionSession);
        if (queryLimit != null) {
            Validate.isTrue(queryLimit > 0, "QueryLimit can not be null");
        }
        this.queryLimit = queryLimit;
        this.connectionSession = connectionSession;
        this.stopWhenError = stopWhenError;
        this.binaryDataManager = ConnectionSessionUtil.getBinaryDataManager(connectionSession);
        ConnectionSessionUtil.setConsoleSessionKillQueryFlag(connectionSession, false);
        Validate.notNull(this.binaryDataManager, "BinaryDataManager can not be null");
        this.context = context;
    }

    @Override
    public List<JdbcGeneralResult> doInStatement(Statement statement) throws SQLException, DataAccessException {
        if (Objects.nonNull(locale)) {
            LocaleContextHolder.setLocale(locale);
        }
        if (ConnectionSessionUtil.isConsoleSessionReset(connectionSession)) {
            ConnectionSessionUtil.setConsoleSessionResetFlag(connectionSession, false);
            return this.sqls.stream().map(sqlTuple -> {
                JdbcGeneralResult result = JdbcGeneralResult.canceledResult(sqlTuple);
                result.setConnectionReset(true);
                onExecutionCancelled(sqlTuple, Collections.singletonList(result));
                return result;
            }).collect(Collectors.toList());
        }
        boolean currentAutoCommit = statement.getConnection().getAutoCommit();
        List<JdbcGeneralResult> returnVal = new LinkedList<>();
        try {
            applyStatementSettings(statement);
            // 对于修改表数据DML，如果是自动提交，为了保证原子性，在执行过程设置为手动，执行完成后再进行reset
            if (this.autoCommit ^ currentAutoCommit) {
                statement.getConnection().setAutoCommit(this.autoCommit);
            }
            Future<Void> handle = null;
            for (SqlTuple sqlTuple : this.sqls) {
                if (handle != null) {
                    try {
                        handle.get();
                    } catch (Exception e) {
                        // eat exception
                    }
                }
                onExecutionStart(sqlTuple);
                try {
                    applyConnectionSettings(statement);
                } catch (Exception e) {
                    log.warn("Init driver statistic collect failed, reason={}", e.getMessage());
                }
                List<JdbcGeneralResult> executeResults;
                if (returnVal.stream().noneMatch(r -> r.getStatus() == SqlExecuteStatus.FAILED) || !stopWhenError) {
                    if (Thread.currentThread().isInterrupted()
                            || ConnectionSessionUtil.isConsoleSessionKillQuery(connectionSession)) {
                        executeResults = Collections.singletonList(JdbcGeneralResult.canceledResult(sqlTuple));
                        onExecutionCancelled(sqlTuple, executeResults);
                    } else {
                        CountDownLatch latch = new CountDownLatch(1);
                        handle = executor.submit(() -> onExecutionStartAfterMillis(sqlTuple, latch));
                        executeResults = doExecuteSql(statement, sqlTuple, latch);
                        onExecutionEnd(sqlTuple, executeResults);
                    }
                } else {
                    executeResults = Collections.singletonList(JdbcGeneralResult.canceledResult(sqlTuple));
                    onExecutionCancelled(sqlTuple, executeResults);
                }
                returnVal.addAll(executeResults);
            }
            Optional<JdbcGeneralResult> failed = returnVal
                    .stream().filter(r -> r.getStatus() == SqlExecuteStatus.FAILED).findFirst();
            if (failed.isPresent()) {
                throw failed.get().getThrown();
            }
        } catch (Exception e) {
            try {
                ConnectionSessionUtil.logSocketInfo(statement.getConnection(), "console error");
            } catch (Exception exception) {
                log.warn("Failed to execute abnormal replenishment logic", exception);
            }
        } finally {
            if (this.autoCommit ^ currentAutoCommit) {
                statement.getConnection().setAutoCommit(currentAutoCommit);
            }
            if (this.dialectType.isOracle()) {
                String dbmsInfo = queryDBMSOutput(statement);
                if (dbmsInfo != null) {
                    log.info("Clear dbms_output cache, dbmsInfo={}", dbmsInfo);
                }
            }
            executor.shutdownNow();
        }
        return returnVal;
    }

    private void applyConnectionSettings(Statement statement) throws SQLException {
        if (statement.getConnection() instanceof OceanBaseConnection) {
            // init jdbc statistic collect
            OceanBaseConnection connection = (OceanBaseConnection) statement.getConnection();
            connection.clearNetworkStatistics();
            connection.networkStatistics(true);
        }
    }

    private void applyStatementSettings(Statement statement) throws SQLException {
        if (this.queryLimit != null) {
            /**
             * 这里的{@link Statement#setFetchSize(int)} 调用会导致 driver 根据 {@code fetchSize} 的大小申请一块相同大小的内存，如果用户填入的
             * {@code queryLimit} 很大的话会导致 OOM 错误。
             *
             * 之所以会存在这个调用最初是因为 ODC 使用了 cursor，但是后来由于 ob-proxy 的缺陷导致弃用 cursor，因此这里的调用是不必要的。为了避免可能的
             * OOM，这里将调用注释掉，等将来driver 修复 了此问题再打开。
             */
            // statement.setFetchSize((int) this.queryLimit.longValue());
            statement.setMaxRows((int) this.queryLimit.longValue());
        }
    }

    private List<JdbcGeneralResult> consumeStatement(Statement statement, SqlTuple sqlTuple, boolean isResultSet)
            throws SQLException, IOException {
        boolean existWarnings = false;
        List<JdbcGeneralResult> executeResults = new ArrayList<>();
        if (connectionSession.getDialectType() == DialectType.OB_ORACLE && Objects.nonNull(statement.getWarnings())) {
            existWarnings = true;
        }
        TraceWatch traceWatch = sqlTuple.getSqlWatch();
        if (isResultSet) {
            StopWatch stopWatch = StopWatch.createStarted();
            do {
                try (ResultSet resultSet = statement.getResultSet()) {
                    JdbcQueryResult jdbcQueryResult = new JdbcQueryResult(resultSet.getMetaData(), rowDataMapper);
                    SqlTuple copiedTuple = sqlTuple.softCopy();
                    JdbcGeneralResult executeResult = JdbcGeneralResult.successResult(copiedTuple);
                    executeResult.setExistWarnings(existWarnings);
                    executeResult.setQueryResult(jdbcQueryResult);
                    ResultSetVirtualTable virtualTable = new ResultSetVirtualTable(copiedTuple.getSqlId(),
                            maxCachedLines, maxCachedSize, cachePredicate);
                    long line = 0;
                    while (resultSet.next()) {
                        jdbcQueryResult.addLine(resultSet);
                        virtualTable.addLine((line++), resultSet,
                                new ResultSetCachedElementFactory(resultSet, binaryDataManager));
                    }
                    if (virtualTable.count() != 0) {
                        ConnectionSessionUtil.setQueryCache(connectionSession, virtualTable);
                    }
                    executeResults.add(executeResult);
                }
            } while (statement.getMoreResults());
            stopWatch.stop();
            SqlExecTime execDetails = getTraceIdAndAndSetStage(statement, traceWatch);
            try (EditableTraceStage getResultSet = traceWatch.startEditableStage(SqlExecuteStages.GET_RESULT_SET)) {
                getResultSet.adapt(stopWatch);
            }
            if (execDetails != null) {
                executeResults.forEach(jdbcGeneralResult -> {
                    jdbcGeneralResult.setTraceId(execDetails.getTraceId());
                    jdbcGeneralResult.setWithFullLinkTrace(execDetails.isWithFullLinkTrace());
                    jdbcGeneralResult.setTraceEmptyReason(execDetails.getTraceEmptyReason());
                });
            }
        } else {
            // TODO: due to client will return -1 when call procedure
            JdbcGeneralResult executeResult = JdbcGeneralResult.successResult(sqlTuple);
            executeResult.setExistWarnings(existWarnings);
            executeResult.setAffectRows(Math.max(statement.getUpdateCount(), 0));
            SqlExecTime execDetails = getTraceIdAndAndSetStage(statement, traceWatch);
            if (execDetails != null) {
                executeResult.setTraceId(execDetails.getTraceId());
                executeResult.setWithFullLinkTrace(execDetails.isWithFullLinkTrace());
                executeResult.setTraceEmptyReason(execDetails.getTraceEmptyReason());
            }
            executeResults.add(executeResult);
        }
        // get pl log，not support for mysql mode
        if (this.dialectType.isOracle()) {
            try (TraceStage s = traceWatch.start(SqlExecuteStages.QUERY_DBMS_OUTPUT)) {
                executeResults.forEach(jdbcResult -> jdbcResult.setDbmsOutput(queryDBMSOutput(statement)));
            }
        }
        return executeResults;
    }

    protected List<JdbcGeneralResult> doExecuteSql(Statement statement, SqlTuple sqlTuple, CountDownLatch latch) {
        try {
            String sql = sqlTuple.getExecutedSql();
            if (!ifFunctionCallExists(sql)) {
                // use text protocal
                try (TraceStage stage = sqlTuple.getSqlWatch().start(SqlExecuteStages.EXECUTE)) {
                    boolean isResultSet;
                    try {
                        isResultSet = statement.execute(sql);
                    } catch (Exception e) {
                        return handleException(e, statement, sqlTuple);
                    }
                    latch.countDown();
                    return consumeStatement(statement, sqlTuple, isResultSet);
                }
            }
            // use ps protocal
            String preparedSql = OBJECT_VALUE_PATTERN.matcher(sql).replaceAll("?");
            log.info(
                    "Load_file call is detected in sql, use ps protocol to rewrite "
                            + "the original sql, originalSql={}, modifiedSql={}",
                    sql, preparedSql);
            List<FunctionDefinition> definitions = retrieveFunctionCalls(sql);
            log.info("There is a function call in sql, functions={}", definitions);
            try (PreparedStatement preparedStatement = statement.getConnection().prepareStatement(preparedSql);
                    TraceStage stage = sqlTuple.getSqlWatch().start(SqlExecuteStages.EXECUTE)) {
                for (int i = 0; i < definitions.size(); i++) {
                    FunctionDefinition definition = definitions.get(i);
                    if ("load_file".equalsIgnoreCase(definition.getFunctionName())) {
                        // load binary data
                        String fileName = retrieveFileNameFromParameters(definition);
                        File file = nullSafeFindFileByName(fileName);
                        preparedStatement.setBinaryStream(i + 1, new FileInputStream(file));
                    } else if ("load_clob_file".equalsIgnoreCase(definition.getFunctionName())) {
                        // load clob data
                        String fileName = retrieveFileNameFromParameters(definition);
                        File file = nullSafeFindFileByName(fileName);
                        preparedStatement.setClob(i + 1, new FileReader(file));
                    } else {
                        throw new NotImplementedException("Unsupport function call " + definition.getFunctionName());
                    }
                }
                boolean isResultSet;
                try {
                    isResultSet = preparedStatement.execute();
                } catch (Exception e) {
                    return handleException(e, statement, sqlTuple);
                }
                latch.countDown();
                return consumeStatement(statement, sqlTuple, isResultSet);
            }
        } catch (Exception e) {
            return Collections.singletonList(JdbcGeneralResult.failedResult(sqlTuple, e));
        } finally {
            latch.countDown();
        }
    }

    protected List<JdbcGeneralResult> handleException(Exception exception, Statement statement, SqlTuple sqlTuple) {
        if (exception instanceof SQLTransientConnectionException
                && ((SQLTransientConnectionException) exception).getErrorCode() == 1094) {
            // ERROR 1094 (HY000) : Unknown thread id: %lu when kill a not exists session
            log.warn("Error executing SQL statement, sql={}, message ={}",
                    sqlTuple.getExecutedSql(), exception.getMessage());
        } else {
            log.warn("Error executing SQL statement, sql={}", sqlTuple.getExecutedSql(), exception);
        }
        JdbcGeneralResult failedResult = JdbcGeneralResult.failedResult(sqlTuple, exception);
        SqlExecTime execDetails = getTraceIdAndAndSetStage(statement, sqlTuple.getSqlWatch());
        if (execDetails != null) {
            failedResult.setTraceId(execDetails.getTraceId());
            failedResult.setWithFullLinkTrace(execDetails.isWithFullLinkTrace());
            failedResult.setTraceEmptyReason(execDetails.getTraceEmptyReason());
        }
        return Collections.singletonList(failedResult);
    }

    private String retrieveFileNameFromParameters(@NonNull FunctionDefinition definition) {
        if (definition.getParameterList().size() != 1) {
            throw new BadRequestException(
                    "Wrong parameter number, expect 1, actual " + definition.getParameterList().size());
        }
        return definition.getParameterList().get(0).toString();
    }

    private List<FunctionDefinition> retrieveFunctionCalls(String sql) {
        Matcher matcher = OBJECT_VALUE_PATTERN.matcher(sql);
        List<FunctionDefinition> definitions = new LinkedList<>();
        while (matcher.find()) {
            String functionName = matcher.group(1);
            List<Object> parameterList = new LinkedList<>();
            if (matcher.group(2) != null) {
                parameterList = Collections.singletonList(matcher.group(2));
            }
            definitions.add(new FunctionDefinition(functionName, parameterList));
        }
        return definitions;
    }

    private boolean ifFunctionCallExists(String sql) {
        Matcher matcher = OBJECT_VALUE_PATTERN.matcher(sql);
        return matcher.find();
    }

    private File nullSafeFindFileByName(String fileName) throws IOException {
        File parentDir = ConnectionSessionUtil.getSessionUploadDir(connectionSession);
        for (File itemFile : parentDir.listFiles()) {
            if (itemFile.isFile() && Objects.equals(itemFile.getName(), fileName)) {
                return itemFile;
            }
        }
        log.warn("The file does not exist or the target directory does not point to a file, fileName={}", fileName);
        throw new FileNotFoundException("File not found " + fileName);
    }

    private SqlExecTime getTraceIdAndAndSetStage(Statement statement, TraceWatch traceWatch) {
        try {
            StopWatch stopWatch = StopWatch.createStarted();
            String version = ConnectionSessionUtil.getVersion(connectionSession);
            SqlExecTime executeDetails = new SqlExecTime();
            if (useFullLinkTrace && VersionUtils.isGreaterThanOrEqualsTo(version, "4.2") &&
                    connectionSession.getDialectType().isOceanbase()) {
                try {
                    executeDetails = FullLinkTraceUtil.getFullLinkTraceDetail(statement, fullLinkTraceTimeout);
                    executeDetails.setWithFullLinkTrace(true);
                } catch (Exception e) {
                    executeDetails.setWithFullLinkTrace(false);
                    executeDetails.setTraceEmptyReason(ErrorCodes.ObGetFullLinkTraceFailed.getLocalizedMessage(null));
                    log.warn("Query full link trace info failed, reason={}", e.getMessage());
                }
            } else {
                executeDetails = ConnectionPluginUtil.getTraceExtension(connectionSession.getDialectType())
                        .getExecuteDetail(statement, version);
                executeDetails.setWithFullLinkTrace(false);
                executeDetails.setTraceEmptyReason(
                        useFullLinkTrace ? ErrorCodes.ObFullLinkTraceNotSupported.getLocalizedMessage(null)
                                : ErrorCodes.ObFullLinkTraceNotEnabled.getLocalizedMessage(null));
            }
            cacheTraceSpan(executeDetails.getTraceSpan());
            setExecuteTraceStage(traceWatch, executeDetails, stopWatch);
            return executeDetails;
        } catch (Exception ex) {
            log.warn("Query sql execute details failed, reason={}", ex.getMessage());
        }
        return null;
    }

    private void setExecuteTraceStage(TraceWatch traceWatch, SqlExecTime executeDetails, StopWatch stopWatch) {
        if (executeDetails.getExecuteMicroseconds() == null) {
            return;
        } else if (executeDetails.getLastPacketSendTimestamp() == null
                || executeDetails.getLastPacketResponseTimestamp() == null) {
            try (EditableTraceStage dbServerExecute =
                    traceWatch.startEditableStage(SqlExecuteStages.DB_SERVER_EXECUTE_SQL)) {
                dbServerExecute.setStartTime(traceWatch.getByTaskName(SqlExecuteStages.EXECUTE).get(0).getStartTime(),
                        TimeUnit.MICROSECONDS);
                dbServerExecute.setTime(executeDetails.getExecuteMicroseconds(), TimeUnit.MICROSECONDS);
            }
            try (EditableTraceStage calculateDuration =
                    traceWatch.startEditableStage(SqlExecuteStages.CALCULATE_DURATION)) {
                calculateDuration.adapt(stopWatch);
            }
            return;
        }
        long lastPacketSendTimestamp = executeDetails.getLastPacketSendTimestamp();
        long lastPacketResponseTimestamp = executeDetails.getLastPacketResponseTimestamp();
        List<TraceStage> executeStages = traceWatch.getByTaskName(SqlExecuteStages.EXECUTE);
        Verify.singleton(executeStages, "execute stages");
        long beforeExecuteTimestamp = executeStages.get(0).getStartTime();

        long networkConsumption =
                lastPacketResponseTimestamp - lastPacketSendTimestamp - executeDetails.getElapsedMicroseconds();

        try (EditableTraceStage jdbcPrepare = traceWatch.startEditableStage(SqlExecuteStages.JDBC_PREPARE)) {
            jdbcPrepare.setStartTime(beforeExecuteTimestamp, TimeUnit.MILLISECONDS);
            jdbcPrepare.setTime(lastPacketSendTimestamp / 1000 - beforeExecuteTimestamp, TimeUnit.MILLISECONDS);
        }
        try (EditableTraceStage network = traceWatch.startEditableStage(SqlExecuteStages.NETWORK_CONSUMPTION)) {
            network.setStartTime(lastPacketSendTimestamp, TimeUnit.MICROSECONDS);
            network.setTime(networkConsumption, TimeUnit.MICROSECONDS);
        }
        try (EditableTraceStage obServerWait = traceWatch.startEditableStage(SqlExecuteStages.OBSERVER_WAIT)) {
            // Assume the network takes the same time to send and receive
            obServerWait.setStartTime(lastPacketSendTimestamp + networkConsumption / 2, TimeUnit.MICROSECONDS);
            obServerWait.setTime(executeDetails.getElapsedMicroseconds() - executeDetails.getExecuteMicroseconds(),
                    TimeUnit.MICROSECONDS);
        }
        try (EditableTraceStage obServerExecute =
                traceWatch.startEditableStage(SqlExecuteStages.DB_SERVER_EXECUTE_SQL)) {
            obServerExecute.setStartTime(
                    lastPacketResponseTimestamp - networkConsumption / 2 - executeDetails.getExecuteMicroseconds(),
                    TimeUnit.MICROSECONDS);
            obServerExecute.setTime(executeDetails.getExecuteMicroseconds(), TimeUnit.MICROSECONDS);
        }
        try (EditableTraceStage calculateDuration =
                traceWatch.startEditableStage(SqlExecuteStages.CALCULATE_DURATION)) {
            calculateDuration.adapt(stopWatch);
        }
    }

    private void cacheTraceSpan(TraceSpan span) {
        if (Objects.isNull(span)) {
            return;
        }
        try {
            BinaryContentMetaData metaData = binaryDataManager.write(
                    new ByteArrayInputStream(JsonUtils.toJson(span).getBytes()));
            ConnectionSessionUtil.setBinaryContentMetadata(connectionSession, span.getLogTraceId(), metaData);
        } catch (IOException e) {
            log.warn("Failed to write trace span with traceId {}", span.getLogTraceId(), e);
        }
    }

    private String queryDBMSOutput(Statement statement) {
        try {
            return OBUtils.queryDBMSOutput(statement.getConnection(), dbmsoutputMaxRows);
        } catch (Exception e) {
            log.warn("Failed to query dbms output", e);
        }
        return null;
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            log.warn("Rollback failed", exception);
        }
    }

    private void onExecutionStart(SqlTuple sqlTuple) {
        if (context != null) {
            context.setCurrentExecutingSql(sqlTuple.getExecutedSql());
            context.incrementTotalExecutedSqlCount();
            context.setCurrentExecutingSqlTraceId(null);
        }
        listeners.forEach(listener -> {
            try {
                listener.onExecutionStart(sqlTuple, context);
            } catch (Exception e) {
                log.warn("An error occurred in listener {}.", listener.getClass(), e);
            }
        });
    }

    private void onExecutionCancelled(SqlTuple sqlTuple, List<JdbcGeneralResult> results) {
        if (context != null) {
            context.addSqlExecutionResults(results);
        }
        listeners.forEach(listener -> {
            try {
                listener.onExecutionCancelled(sqlTuple, results, context);
            } catch (Exception e) {
                log.warn("An error occurred in listener {}.", listener.getClass(), e);
            }
        });
    }

    private void onExecutionEnd(SqlTuple sqlTuple, List<JdbcGeneralResult> results) {
        if (context != null) {
            context.addSqlExecutionResults(results);
        }
        listeners.forEach(listener -> {
            try {
                listener.onExecutionEnd(sqlTuple, results, context);
            } catch (Exception e) {
                log.warn("An error occurred in listener {}.", listener.getClass(), e);
            }
        });
    }

    private Void onExecutionStartAfterMillis(SqlTuple sqlTuple, CountDownLatch latch) {
        long startTs = System.currentTimeMillis();
        List<SqlExecutionListener> sortedListeners = listeners.stream()
                .filter(listener -> listener.getOnExecutionStartAfterMillis() != null
                        && listener.getOnExecutionStartAfterMillis() > 0)
                .sorted(Comparator
                        .comparingLong(SqlExecutionListener::getOnExecutionStartAfterMillis))
                .collect(Collectors.toList());
        for (SqlExecutionListener listener : sortedListeners) {
            long waitTs = System.currentTimeMillis() - startTs;
            Long expectedTs = listener.getOnExecutionStartAfterMillis();
            try {
                if (!latch.await(expectedTs - waitTs, TimeUnit.MILLISECONDS)) {
                    listener.onExecutionStartAfter(sqlTuple, context);
                } else {
                    break;
                }
            } catch (InterruptedException e) {
                return null;
            } catch (Exception e) {
                log.warn("An error occurred in listener {}.", listener.getClass(), e);
            }
        }
        return null;
    }

    @Getter
    @ToString
    static class FunctionDefinition {
        private final String functionName;
        private final List<Object> parameterList;

        public FunctionDefinition(@NonNull String functionName, @NonNull List<Object> parameterList) {
            this.functionName = functionName;
            this.parameterList = parameterList;
        }
    }

}
