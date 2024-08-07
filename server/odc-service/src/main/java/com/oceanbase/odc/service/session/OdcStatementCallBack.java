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

    /**
     * 在给定的Statement上执行SQL语句列表
     *
     * @param statement 给定的Statement
     * @return 执行结果列表
     * @throws SQLException
     * @throws DataAccessException
     */
    @Override
    public List<JdbcGeneralResult> doInStatement(Statement statement) throws SQLException, DataAccessException {
        if (Objects.nonNull(locale)) {
            LocaleContextHolder.setLocale(locale);
        }
        // 检查是否需要置控制台会话，如果需要，取消所有SQL执行并返回取消结果。
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
            // 以元组为单位执行SQL语句
            for (SqlTuple sqlTuple : this.sqls) {
                // handle.get()阻塞直到更新监听器的异步任务完成
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
                // 判断是否有失败的SQL语句或者不需要在出错时停止执行
                if (returnVal.stream().noneMatch(r -> r.getStatus() == SqlExecuteStatus.FAILED) || !stopWhenError) {
                    // 判断当前线程是否被中断或者控制台会话是否已经kill查询
                    if (Thread.currentThread().isInterrupted()
                        || ConnectionSessionUtil.isConsoleSessionKillQuery(connectionSession)) {
                        // 如果有失败的SQL语句且needStopWhenError为true，则将结果设置为已取消，并调用onExecutionCancelled方法
                        executeResults = Collections.singletonList(JdbcGeneralResult.canceledResult(sqlTuple));
                        onExecutionCancelled(sqlTuple, executeResults);
                    } else {
                        // 如果不是，则创建CountDownLatch对象，并提交任务到executor中执行
                        CountDownLatch latch = new CountDownLatch(1);
                        // 开启异步线程去完成监听器的更新
                        handle = executor.submit(() -> onExecutionStartAfterMillis(sqlTuple, latch));
                        executeResults = doExecuteSql(statement, sqlTuple, latch);
                        onExecutionEnd(sqlTuple, executeResults);
                    }
                } else {
                    // 如果有失败的SQL语句且needStopWhenError为true，则将结果设置为已取消，并调用onExecutionCancelled方法
                    executeResults = Collections.singletonList(JdbcGeneralResult.canceledResult(sqlTuple));
                    onExecutionCancelled(sqlTuple, executeResults);
                }
                // 将执行结果添加到returnVal中
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

    /**
     * 应用连接设置
     *
     * @param statement 语句对象
     * @throws SQLException SQL异常
     */
    private void applyConnectionSettings(Statement statement) throws SQLException {
        if (statement.getConnection() instanceof OceanBaseConnection) {
            // init jdbc statistic collect
            // 初始化JDBC统计收集器
            OceanBaseConnection connection = (OceanBaseConnection) statement.getConnection();
            // 清除网络统计信息
            connection.clearNetworkStatistics();
            // 开启网络统计信息收集
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

    /**
     * 执行SQL语句并返回结果
     *
     * @param statement   SQL语句
     * @param sqlTuple    SQL语句元数据
     * @param isResultSet 是否为结果集
     * @return 执行结果列表
     * @throws SQLException SQL异常
     * @throws IOException IO异常
     */
    private List<JdbcGeneralResult> consumeStatement(Statement statement, SqlTuple sqlTuple, boolean isResultSet)
        throws SQLException, IOException {
        boolean existWarnings = false;
        List<JdbcGeneralResult> executeResults = new ArrayList<>();
        if (connectionSession.getDialectType() == DialectType.OB_ORACLE && Objects.nonNull(statement.getWarnings())) {
            existWarnings = true;
        }
        TraceWatch traceWatch = sqlTuple.getSqlWatch();
        // 如果不是DQL语句就不执行
        if (isResultSet) {
            StopWatch stopWatch = StopWatch.createStarted();
            do {
                try (ResultSet resultSet = statement.getResultSet()) {
                    // 创建JdbcQueryResult对象
                    JdbcQueryResult jdbcQueryResult = new JdbcQueryResult(resultSet.getMetaData(), rowDataMapper);
                    // 创建SqlTuple对象的副本
                    SqlTuple copiedTuple = sqlTuple.softCopy();
                    // 创建JdbcGeneralResult对象
                    JdbcGeneralResult executeResult = JdbcGeneralResult.successResult(copiedTuple);
                    executeResult.setExistWarnings(existWarnings);
                    executeResult.setQueryResult(jdbcQueryResult);
                    // 创建ResultSetVirtualTable对象
                    ResultSetVirtualTable virtualTable = new ResultSetVirtualTable(copiedTuple.getSqlId(),
                        maxCachedLines, maxCachedSize, cachePredicate);
                    long line = 0;
                    while (resultSet.next()) {
                        // 将结果集中的数据添加到jdbcQueryResult中
                        jdbcQueryResult.addLine(resultSet);
                        // 将结果集中的数据添加到virtualTable中
                        virtualTable.addLine((line++), resultSet,
                            new ResultSetCachedElementFactory(resultSet, binaryDataManager));
                    }
                    if (virtualTable.count() != 0) {
                        // 将virtualTable设置到connectionSession中
                        ConnectionSessionUtil.setQueryCache(connectionSession, virtualTable);
                    }
                    executeResults.add(executeResult);
                }
            } while (statement.getMoreResults());
            stopWatch.stop();
            // 获取traceId并设置stage
            SqlExecTime execDetails = getTraceIdAndAndSetStage(statement, traceWatch);
            try (EditableTraceStage getResultSet = traceWatch.startEditableStage(SqlExecuteStages.GET_RESULT_SET)) {
                getResultSet.adapt(stopWatch);
            }
            if (execDetails != null) {
                // 将traceId、withFullLinkTrace和traceEmptyReason设置到executeResults中
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
            // 获取traceId并设置stage
            SqlExecTime execDetails = getTraceIdAndAndSetStage(statement, traceWatch);
            if (execDetails != null) {
                // 将traceId、withFullLinkTrace和traceEmptyReason设置到executeResult中
                executeResult.setTraceId(execDetails.getTraceId());
                executeResult.setWithFullLinkTrace(execDetails.isWithFullLinkTrace());
                executeResult.setTraceEmptyReason(execDetails.getTraceEmptyReason());
            }
            executeResults.add(executeResult);
        }
        // get pl log，not support for mysql mode
        // 获取pl log，不支持mysql模式
        if (this.dialectType.isOracle()) {
            try (TraceStage s = traceWatch.start(SqlExecuteStages.QUERY_DBMS_OUTPUT)) {
                // 将queryDBMSOutput的结果设置到executeResults中
                executeResults.forEach(jdbcResult -> jdbcResult.setDbmsOutput(queryDBMSOutput(statement)));
            }
        }
        return executeResults;
    }

    /**
     * 执行SQL语句
     *
     * @param statement SQL语句
     * @param sqlTuple  SQL语句元数据
     * @param latch     CountDownLatch计数器
     * @return JdbcGeneralResult列表
     */
    protected List<JdbcGeneralResult> doExecuteSql(Statement statement, SqlTuple sqlTuple, CountDownLatch latch) {
        try {
            String sql = sqlTuple.getExecutedSql();
            if (!ifFunctionCallExists(sql)) {
                // use text protocal
                // 此时才开始准备执行sql
                TraceStage stage = sqlTuple.getSqlWatch().start(SqlExecuteStages.EXECUTE);
                try {
                    boolean isResultSet;
                    try {
                        isResultSet = statement.execute(sql);
                    } catch (Exception e) {
                        return handleException(e, statement, sqlTuple);
                    }
                    latch.countDown();
                    return consumeStatement(statement, sqlTuple, isResultSet);
                } finally {
                    stage.close();
                }
            }
            // use ps protocal
            // 使用ps协议
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
                        // 加载clob数据
                        String fileName = retrieveFileNameFromParameters(definition);
                        File file = nullSafeFindFileByName(fileName);
                        preparedStatement.setBinaryStream(i + 1, new FileInputStream(file));
                    } else if ("load_clob_file".equalsIgnoreCase(definition.getFunctionName())) {
                        // load clob data
                        // 加载clob数据
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

    /**
     * 判断给定的SQL语句中是否存在函数调用
     *
     * @param sql 给定的SQL语句
     * @return 若存在函数调用则返回true，否则返回false
     */
    private boolean ifFunctionCallExists(String sql) {
        // 使用正则表达式匹配函数调用
        Matcher matcher = OBJECT_VALUE_PATTERN.matcher(sql);
        // 返回匹配结果
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
            // 创建并启动计时器
            StopWatch stopWatch = StopWatch.createStarted();
            // 获取连接会话的版本信息
            String version = ConnectionSessionUtil.getVersion(connectionSession);
            // 创建 SqlExecTime 对象
            SqlExecTime executeDetails = new SqlExecTime();
            // 判断是否使用全链路追踪，以及数据库版本和方言类型是否支持全链路追踪
            if (useFullLinkTrace && VersionUtils.isGreaterThanOrEqualsTo(version, "4.2") &&
                connectionSession.getDialectType().isOceanbase()) {
                try {
                    // 获取全链路追踪详情
                    executeDetails = FullLinkTraceUtil.getFullLinkTraceDetail(statement, fullLinkTraceTimeout);
                    executeDetails.setWithFullLinkTrace(true);
                } catch (Exception e) {
                    // 获取全链路追踪详情失败
                    executeDetails.setWithFullLinkTrace(false);
                    executeDetails.setTraceEmptyReason(ErrorCodes.ObGetFullLinkTraceFailed.getLocalizedMessage(null));
                    log.warn("Query full link trace info failed, reason={}", e.getMessage());
                }
            } else {
                // 获取执行详情
                executeDetails = ConnectionPluginUtil.getTraceExtension(connectionSession.getDialectType())
                    .getExecuteDetail(statement, version);
                // 获取全链路追踪详情失败
                executeDetails.setWithFullLinkTrace(false);
                executeDetails.setTraceEmptyReason(
                    useFullLinkTrace ? ErrorCodes.ObFullLinkTraceNotSupported.getLocalizedMessage(null)
                        : ErrorCodes.ObFullLinkTraceNotEnabled.getLocalizedMessage(null));
            }
            // 缓存跨度信息
            cacheTraceSpan(executeDetails.getTraceSpan());
            // 设置执行跟踪阶段
            setExecuteTraceStage(traceWatch, executeDetails, stopWatch);
            // 返回执行详情
            return executeDetails;
        } catch (Exception ex) {
            // 查询 SQL 执行详情失败
            log.warn("Query sql execute details failed, reason={}", ex.getMessage());
        }
        // 返回 null
        return null;    }

    private void setExecuteTraceStage(TraceWatch traceWatch, SqlExecTime executeDetails, StopWatch stopWatch) {
        // 判断执行微秒数是否为空
        if (executeDetails.getExecuteMicroseconds() == null) {
            return;
            // 判断最后一次数据包发送或者接收的时间戳是否为空
        } else if (executeDetails.getLastPacketSendTimestamp() == null
                   || executeDetails.getLastPacketResponseTimestamp() == null) {
            // 记录数据库服务器执行 SQL 的阶段
            try (EditableTraceStage dbServerExecute =
                traceWatch.startEditableStage(SqlExecuteStages.DB_SERVER_EXECUTE_SQL)) {
                dbServerExecute.setStartTime(traceWatch.getByTaskName(SqlExecuteStages.EXECUTE).get(0).getStartTime(),
                    TimeUnit.MICROSECONDS);
                dbServerExecute.setTime(executeDetails.getExecuteMicroseconds(), TimeUnit.MICROSECONDS);
            }
            // 计算持续时间的阶段
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

        // 计算网络消耗
        long networkConsumption =
            lastPacketResponseTimestamp - lastPacketSendTimestamp - executeDetails.getElapsedMicroseconds();

        // 记录 JDBC 准备的阶段
        try (EditableTraceStage jdbcPrepare = traceWatch.startEditableStage(SqlExecuteStages.JDBC_PREPARE)) {
            jdbcPrepare.setStartTime(beforeExecuteTimestamp, TimeUnit.MILLISECONDS);
            jdbcPrepare.setTime(lastPacketSendTimestamp / 1000 - beforeExecuteTimestamp, TimeUnit.MILLISECONDS);
        }
        // 记录网络消耗的阶段
        try (EditableTraceStage network = traceWatch.startEditableStage(SqlExecuteStages.NETWORK_CONSUMPTION)) {
            network.setStartTime(lastPacketSendTimestamp, TimeUnit.MICROSECONDS);
            network.setTime(networkConsumption, TimeUnit.MICROSECONDS);
        }
        // 记录ob服务器等待的阶段
        try (EditableTraceStage obServerWait = traceWatch.startEditableStage(SqlExecuteStages.OBSERVER_WAIT)) {
            // Assume the network takes the same time to send and receive
            // 假设网络发送和接收的时间相同
            obServerWait.setStartTime(lastPacketSendTimestamp + networkConsumption / 2, TimeUnit.MICROSECONDS);
            obServerWait.setTime(executeDetails.getElapsedMicroseconds() - executeDetails.getExecuteMicroseconds(),
                TimeUnit.MICROSECONDS);
        }
        // 记录ob服务器执行sql耗时
        try (EditableTraceStage obServerExecute =
            traceWatch.startEditableStage(SqlExecuteStages.DB_SERVER_EXECUTE_SQL)) {
            obServerExecute.setStartTime(
                lastPacketResponseTimestamp - networkConsumption / 2 - executeDetails.getExecuteMicroseconds(),
                TimeUnit.MICROSECONDS);
            obServerExecute.setTime(executeDetails.getExecuteMicroseconds(), TimeUnit.MICROSECONDS);
        }
        // 计算持续时间的阶段
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

    /**
     * 当执行SQL开始时调用此方法
     *
     * @param sqlTuple SQL元组
     */
    private void onExecutionStart(SqlTuple sqlTuple) {
        if (context != null) {
            // 设置当前执行的SQL语句
            context.setCurrentExecutingSql(sqlTuple.getExecutedSql());
            // 设置当前执行的SQL语句ID
            context.setCurrentExecutingSqlId(sqlTuple.getSqlId());
            // 增加总执行的SQL语句数量
            context.incrementTotalExecutedSqlCount();
            // 设置当前执行的SQL语句的跟踪ID为空
            context.setCurrentExecutingSqlTraceId(null);
        }
        // 遍历所有监听器并调用其onExecutionStart方法
        listeners.forEach(listener -> {
            try {
                listener.onExecutionStart(sqlTuple, context);
            } catch (Exception e) {
                // 如果监听器中出现异常，则记录日志
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

    /**
     * 当 SQL 执行结束时调用此方法
     *
     * @param sqlTuple SQL 执行上下文和参数
     * @param results  SQL 执行结果列表
     */
    private void onExecutionEnd(SqlTuple sqlTuple, List<JdbcGeneralResult> results) {
        // 如果上下文不为空，则将 SQL 执行结果添加到上下文中
        if (context != null) {
            context.addSqlExecutionResults(results);
        }
        // 遍历所有监听器，并调用其 onExecutionEnd 方法
        listeners.forEach(listener -> {
            try {
                listener.onExecutionEnd(sqlTuple, results, context);
            } catch (Exception e) {
                // 如果监听器调用出现异常，则记录日志
                log.warn("An error occurred in listener {}.", listener.getClass(), e);
            }
        });
    }

    /**
     * 在执行开始后指定的毫秒数后执行监听器
     *
     * @param sqlTuple SQL元组
     * @param latch    计数器闭锁
     * @return Void
     */
    private Void onExecutionStartAfterMillis(SqlTuple sqlTuple, CountDownLatch latch) {
        long startTs = System.currentTimeMillis();
        // 获取所有指定了执行开始后的毫秒数的监听器，并按照毫秒数排序
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
                // 阻塞等待sql元组执行完成
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
