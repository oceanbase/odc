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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.unit.BinarySize;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.LogUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.common.util.TraceWatch;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.RequestTimeoutException;
import com.oceanbase.odc.core.sql.execute.FutureResult;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.table.ResultSetVirtualTable;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualTable;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.EmptyAstFactory;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.config.UserConfigFacade;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.session.DefaultDBSessionManage;
import com.oceanbase.odc.service.db.session.KillSessionOrQueryReq;
import com.oceanbase.odc.service.db.session.KillSessionResult;
import com.oceanbase.odc.service.dml.ValueEncodeType;
import com.oceanbase.odc.service.feature.AllFeatures;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.UnauthorizedDatabase;
import com.oceanbase.odc.service.session.interceptor.SqlCheckInterceptor;
import com.oceanbase.odc.service.session.interceptor.SqlConsoleInterceptor;
import com.oceanbase.odc.service.session.interceptor.SqlExecuteInterceptorService;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.AsyncExecuteResultResp;
import com.oceanbase.odc.service.session.model.BinaryContent;
import com.oceanbase.odc.service.session.model.OdcResultSetMetaData.OdcTable;
import com.oceanbase.odc.service.session.model.QueryTableOrViewDataReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.util.SqlRewriteUtil;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Sql Console, used to execute sql
 *
 * @author yh263208
 * @date 2021-11-18 15:18
 * @since ODC_release_3.2.2
 */
@Service
@Slf4j
@Validated
@SkipAuthorize("inside connect session")
public class ConnectConsoleService {

    public static final int DEFAULT_GET_RESULT_TIMEOUT_SECONDS = 3;
    public static final String SHOW_TABLE_COLUMN_INFO = "SHOW_TABLE_COLUMN_INFO";

    @Autowired
    private ConnectSessionService sessionService;
    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private SqlExecuteInterceptorService sqlInterceptService;
    @Autowired
    private DBSessionManageFacade dbSessionManageFacade;
    @Autowired
    private DefaultDBSessionManage defaultDbSessionManage;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private UserConfigFacade userConfigFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    public SqlExecuteResult queryTableOrViewData(@NotNull String sessionId,
            @NotNull @Valid QueryTableOrViewDataReq req) throws Exception {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);
        SqlBuilder sqlBuilder;
        DialectType dialectType = connectionSession.getConnectType().getDialectType();
        if (dialectType.isMysql()) {
            sqlBuilder = new MySQLSqlBuilder();
        } else if (dialectType.isOracle()) {
            sqlBuilder = new OracleSqlBuilder();
        } else if (dialectType.isDoris()) {
            sqlBuilder = new MySQLSqlBuilder();
        } else {
            throw new IllegalArgumentException("Unsupported dialect type, " + dialectType);
        }
        sqlBuilder.append("SELECT ");
        if (req.isAddROWID() && connectionSession.getDialectType().isOracle()) {
            sqlBuilder.append(" t.ROWID, ");
        }
        sqlBuilder.append(" t.* ").append(" FROM ")
                .schemaPrefixIfNotBlank(req.getSchemaName()).identifier(req.getTableOrViewName()).append(" t");

        Integer queryLimit = checkQueryLimit(req.getQueryLimit());
        if (DialectType.OB_ORACLE == connectionSession.getDialectType()) {
            String version = ConnectionSessionUtil.getVersion(connectionSession);
            if (VersionUtils.isGreaterThanOrEqualsTo(version, "2.2.50")) {
                sqlBuilder.append(" FETCH FIRST ").append(queryLimit.toString()).append(" ROWS ONLY");
            } else {
                sqlBuilder.append(" WHERE ROWNUM <= ").append(queryLimit.toString());
            }
        } else if (DialectType.ORACLE == connectionSession.getDialectType()) {
            sqlBuilder.append(" WHERE ROWNUM <= ").append(queryLimit.toString());
        } else {
            sqlBuilder.append(" LIMIT ").append(queryLimit.toString());
        }

        SqlAsyncExecuteReq asyncExecuteReq = new SqlAsyncExecuteReq();
        asyncExecuteReq.setSql(sqlBuilder.toString());
        // avoid rewrite while execute
        asyncExecuteReq.setAddROWID(false);
        asyncExecuteReq.setQueryLimit(queryLimit);
        asyncExecuteReq.setShowTableColumnInfo(true);
        asyncExecuteReq.setContinueExecutionOnError(true);
        asyncExecuteReq.setFullLinkTraceEnabled(false);
        SqlAsyncExecuteResp resp = execute(sessionId, asyncExecuteReq, false);

        List<UnauthorizedDatabase> unauthorizedDatabases = resp.getUnauthorizedDatabases();
        if (CollectionUtils.isNotEmpty(unauthorizedDatabases)) {
            UnauthorizedDatabase unauthorizedDatabase = unauthorizedDatabases.get(0);
            throw new BadRequestException(ErrorCodes.DatabaseAccessDenied,
                    new Object[] {unauthorizedDatabase.getUnauthorizedPermissionTypes().stream()
                            .map(DatabasePermissionType::getLocalizedMessage).collect(Collectors.joining(","))},
                    "Lack permission for the database with id " + unauthorizedDatabase.getId());
        }

        String requestId = resp.getRequestId();
        ConnectionConfig connConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        List<SqlExecuteResult> results = getAsyncResult(sessionId, requestId, connConfig.queryTimeoutSeconds());
        if (CollectionUtils.isEmpty(results)) {
            String sqlId = resp.getSqls().get(0).getSqlTuple().getSqlId();
            throw new RequestTimeoutException(String
                    .format("Query data failed, get result-set timeout, requestId=%s, sqlId=%s", requestId, sqlId));
        }
        Verify.verify(results.size() == 1, "Expect results.size=1, but " + results.size());
        SqlExecuteResult result = results.get(0);
        /**
         * editable will always be false because ResultSetMetaData#getTableName will return blank in oracle
         * JDBC, but the resultSet can be edited in this single-table query scenario, so we just set it to
         * true.
         */
        if (DialectType.ORACLE == connectionSession.getDialectType()) {
            if (result.getResultSetMetaData() != null) {
                result.getResultSetMetaData().setEditable(true);
                result.getResultSetMetaData().getFieldMetaDataList()
                        .forEach(jdbcColumnMetaData -> jdbcColumnMetaData.setEditable(true));
            }
        }
        return result;
    }

    public SqlAsyncExecuteResp execute(@NotNull String sessionId, @NotNull @Valid SqlAsyncExecuteReq request)
            throws Exception {
        return execute(sessionId, request, true);
    }

    public SqlAsyncExecuteResp execute(@NotNull String sessionId,
            @NotNull @Valid SqlAsyncExecuteReq request, boolean needSqlRuleCheck) throws Exception {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);

        long maxSqlLength = sessionProperties.getMaxSqlLength();
        if (maxSqlLength > 0) {
            PreConditions.lessThanOrEqualTo("sqlLength", LimitMetric.SQL_LENGTH,
                    StringUtils.length(request.getSql()), maxSqlLength);
        }
        SqlAsyncExecuteResp result = filterKillSession(connectionSession, request);
        if (result != null) {
            return result;
        }
        List<OffsetString> sqls = request.ifSplitSqls()
                ? SqlUtils.splitWithOffset(connectionSession, request.getSql(),
                        sessionProperties.isOracleRemoveCommentPrefix())
                : Collections.singletonList(new OffsetString(0, request.getSql()));
        if (sqls.size() == 0) {
            /**
             * if a sql only contains delimiter setting(eg. delimiter $$), code will do this
             */
            SqlTuple sqlTuple = SqlTuple.newTuple(request.getSql());
            String id = ConnectionSessionUtil.setFutureJdbc(connectionSession,
                    FutureResult.successResultList(JdbcGeneralResult.successResult(sqlTuple)), null);
            return SqlAsyncExecuteResp.newSqlAsyncExecuteResp(id, Collections.singletonList(sqlTuple));
        }

        long maxSqlStatementCount = sessionProperties.getMaxSqlStatementCount();
        if (maxSqlStatementCount > 0) {
            PreConditions.lessThanOrEqualTo("sqlStatementCount",
                    LimitMetric.SQL_STATEMENT_COUNT, sqls.size(), maxSqlStatementCount);
        }

        List<SqlTuple> sqlTuples = generateSqlTuple(sqls, connectionSession, request);
        SqlAsyncExecuteResp response = SqlAsyncExecuteResp.newSqlAsyncExecuteResp(sqlTuples);
        Map<String, Object> context = new HashMap<>();
        context.put(SHOW_TABLE_COLUMN_INFO, request.getShowTableColumnInfo());
        context.put(SqlCheckInterceptor.NEED_SQL_CHECK_KEY, needSqlRuleCheck);
        context.put(SqlConsoleInterceptor.NEED_SQL_CONSOLE_CHECK, needSqlRuleCheck);
        AsyncExecuteContext executeContext = new AsyncExecuteContext(sqlTuples, context);
        List<TraceStage> stages = sqlTuples.stream()
                .map(s -> s.getSqlWatch().start(SqlExecuteStages.SQL_PRE_CHECK))
                .collect(Collectors.toList());
        try {
            if (!sqlInterceptService.preHandle(request, response, connectionSession, executeContext)) {
                return response;
            }
        } finally {
            for (TraceStage stage : stages) {
                try {
                    stage.close();
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
        Integer queryLimit = checkQueryLimit(request.getQueryLimit());
        boolean continueExecutionOnError =
                Objects.nonNull(request.getContinueExecutionOnError()) ? request.getContinueExecutionOnError()
                        : userConfigFacade.isContinueExecutionOnError();
        boolean stopOnError = !continueExecutionOnError;
        OdcStatementCallBack statementCallBack = new OdcStatementCallBack(sqlTuples, connectionSession,
                request.getAutoCommit(), queryLimit, stopOnError);

        statementCallBack.setDbmsoutputMaxRows(sessionProperties.getDbmsOutputMaxRows());

        boolean fullLinkTraceEnabled =
                Objects.nonNull(request.getFullLinkTraceEnabled()) ? request.getFullLinkTraceEnabled()
                        : userConfigFacade.isFullLinkTraceEnabled();
        statementCallBack.setUseFullLinkTrace(fullLinkTraceEnabled);

        statementCallBack.setFullLinkTraceTimeout(sessionProperties.getFullLinkTraceTimeoutSeconds());
        statementCallBack.setMaxCachedSize(sessionProperties.getResultSetMaxCachedSize());
        statementCallBack.setMaxCachedLines(sessionProperties.getResultSetMaxCachedLines());
        statementCallBack.setLocale(LocaleContextHolder.getLocale());

        Future<List<JdbcGeneralResult>> futureResult = connectionSession.getAsyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY).execute(statementCallBack);
        String id = ConnectionSessionUtil.setFutureJdbc(connectionSession, futureResult, context);
        response.setRequestId(id);
        return response;
    }

    public SqlAsyncExecuteResp streamExecute(@NotNull String sessionId,
            @NotNull @Valid SqlAsyncExecuteReq request, boolean needSqlRuleCheck) throws Exception {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);

        long maxSqlLength = sessionProperties.getMaxSqlLength();
        if (maxSqlLength > 0) {
            PreConditions.lessThanOrEqualTo("sqlLength", LimitMetric.SQL_LENGTH,
                    StringUtils.length(request.getSql()), maxSqlLength);
        }
        SqlAsyncExecuteResp result = filterKillSession(connectionSession, request);
        if (result != null) {
            return result;
        }
        List<OffsetString> sqls = request.ifSplitSqls()
                ? SqlUtils.splitWithOffset(connectionSession, request.getSql(),
                        sessionProperties.isOracleRemoveCommentPrefix())
                : Collections.singletonList(new OffsetString(0, request.getSql()));
        if (sqls.size() == 0) {
            /**
             * if a sql only contains delimiter setting(eg. delimiter $$), code will do this
             */
            SqlTuple sqlTuple = SqlTuple.newTuple(request.getSql());
            String id = ConnectionSessionUtil.setFutureJdbc(connectionSession,
                    FutureResult.successResultList(JdbcGeneralResult.successResult(sqlTuple)), null);
            return SqlAsyncExecuteResp.newSqlAsyncExecuteResp(id, Collections.singletonList(sqlTuple));
        }

        long maxSqlStatementCount = sessionProperties.getMaxSqlStatementCount();
        if (maxSqlStatementCount > 0) {
            PreConditions.lessThanOrEqualTo("sqlStatementCount",
                    LimitMetric.SQL_STATEMENT_COUNT, sqls.size(), maxSqlStatementCount);
        }

        List<SqlTuple> sqlTuples = generateSqlTuple(sqls, connectionSession, request);
        SqlAsyncExecuteResp response = SqlAsyncExecuteResp.newSqlAsyncExecuteResp(sqlTuples);
        Map<String, Object> context = new HashMap<>();
        context.put(SHOW_TABLE_COLUMN_INFO, request.getShowTableColumnInfo());
        context.put(SqlCheckInterceptor.NEED_SQL_CHECK_KEY, needSqlRuleCheck);
        context.put(SqlConsoleInterceptor.NEED_SQL_CONSOLE_CHECK, needSqlRuleCheck);
        AsyncExecuteContext executeContext = new AsyncExecuteContext(sqlTuples, context);
        List<TraceStage> stages = sqlTuples.stream()
                .map(s -> s.getSqlWatch().start(SqlExecuteStages.SQL_PRE_CHECK))
                .collect(Collectors.toList());
        try {
            if (!sqlInterceptService.preHandle(request, response, connectionSession, executeContext)) {
                return response;
            }
        } finally {
            for (TraceStage stage : stages) {
                try {
                    stage.close();
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
        Integer queryLimit = checkQueryLimit(request.getQueryLimit());
        boolean continueExecutionOnError =
                Objects.nonNull(request.getContinueExecutionOnError()) ? request.getContinueExecutionOnError()
                        : userConfigFacade.isContinueExecutionOnError();
        boolean stopOnError = !continueExecutionOnError;
        OdcStatementCallBack statementCallBack = new OdcStatementCallBack(sqlTuples, connectionSession,
                request.getAutoCommit(), queryLimit, stopOnError, executeContext);

        statementCallBack.setDbmsoutputMaxRows(sessionProperties.getDbmsOutputMaxRows());

        boolean fullLinkTraceEnabled =
                Objects.nonNull(request.getFullLinkTraceEnabled()) ? request.getFullLinkTraceEnabled()
                        : userConfigFacade.isFullLinkTraceEnabled();
        statementCallBack.setUseFullLinkTrace(fullLinkTraceEnabled);

        statementCallBack.setFullLinkTraceTimeout(sessionProperties.getFullLinkTraceTimeoutSeconds());
        statementCallBack.setMaxCachedSize(sessionProperties.getResultSetMaxCachedSize());
        statementCallBack.setMaxCachedLines(sessionProperties.getResultSetMaxCachedLines());
        statementCallBack.setLocale(LocaleContextHolder.getLocale());
        if (connectionSession.getDialectType().isOceanbase() && sqlTuples.size() <= 10) {
            statementCallBack.getListeners().add(new OBExecutionListener(connectionSession));
        }

        Future<List<JdbcGeneralResult>> futureResult = connectionSession.getAsyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY).execute(statementCallBack);
        executeContext.setFuture(futureResult);
        String id = ConnectionSessionUtil.setExecuteContext(connectionSession, executeContext);
        response.setRequestId(id);
        return response;
    }

    public List<SqlExecuteResult> getAsyncResult(@NotNull String sessionId, @NotNull String requestId) {
        return getAsyncResult(sessionId, requestId, DEFAULT_GET_RESULT_TIMEOUT_SECONDS);
    }

    public List<SqlExecuteResult> getAsyncResult(@NotNull String sessionId, String requestId, Integer timeoutSeconds) {
        PreConditions.validArgumentState(Objects.nonNull(requestId), ErrorCodes.SqlRegulationRuleBlocked, null, null);
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId);
        Future<List<JdbcGeneralResult>> listFuture =
                ConnectionSessionUtil.getFutureJdbcResult(connectionSession, requestId);
        int timeout = Objects.isNull(timeoutSeconds) ? DEFAULT_GET_RESULT_TIMEOUT_SECONDS : timeoutSeconds;
        try {
            List<JdbcGeneralResult> resultList = listFuture.get(timeout, TimeUnit.SECONDS);
            Map<String, Object> context = ConnectionSessionUtil.getFutureJdbcContext(connectionSession, requestId);
            ConnectionSessionUtil.removeFutureJdbc(connectionSession, requestId);
            return resultList.stream().map(jdbcGeneralResult -> {
                Map<String, Object> ctx = context == null ? new HashMap<>() : context;
                SqlExecuteResult result = generateResult(connectionSession, jdbcGeneralResult, ctx);
                try (TraceStage stage = result.getSqlTuple().getSqlWatch().start(SqlExecuteStages.SQL_AFTER_CHECK)) {
                    sqlInterceptService.afterCompletion(result, connectionSession, new AsyncExecuteContext(null, ctx));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return result;
            }).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (TimeoutException timeoutException) {
            if (log.isDebugEnabled()) {
                log.debug("Get sql execution result timed out, sessionId={}, requestId={}", sessionId, requestId,
                        timeoutException);
            }
            return Collections.emptyList();
        }
    }

    public AsyncExecuteResultResp getMoreResults(@NotNull String sessionId, String requestId) {
        PreConditions.validArgumentState(Objects.nonNull(requestId), ErrorCodes.SqlRegulationRuleBlocked, null, null);
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId);
        AsyncExecuteContext context =
                (AsyncExecuteContext) ConnectionSessionUtil.getExecuteContext(connectionSession, requestId);
        boolean shouldRemoveContext = context.isFinished();
        try {
            List<JdbcGeneralResult> resultList = context.getMoreSqlExecutionResults();
            List<SqlExecuteResult> results = resultList.stream().map(jdbcGeneralResult -> {
                SqlExecuteResult result = generateResult(connectionSession, jdbcGeneralResult, context.getContextMap());
                try (TraceStage stage = result.getSqlTuple().getSqlWatch().start(SqlExecuteStages.SQL_AFTER_CHECK)) {
                    sqlInterceptService.afterCompletion(result, connectionSession, context);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return result;
            }).collect(Collectors.toList());
            return new AsyncExecuteResultResp(shouldRemoveContext, context, results);
        } catch (Exception e) {
            shouldRemoveContext = true;
            throw e;
        } finally {
            if (shouldRemoveContext) {
                ConnectionSessionUtil.removeExecuteContext(connectionSession, requestId);
            }
        }
    }

    public BinaryContent getBinaryContent(@NotNull String sessionId, @NotNull String sqlId,
            @NotNull Long rowNum, @NotNull Integer colNum, @NotNull Long skip,
            @NotNull Integer len, @NotNull ValueEncodeType format) throws IOException {
        InputStream inputStream;
        try {
            inputStream = readBinaryData(sessionId, sqlId, rowNum, colNum);
        } catch (IOException exception) {
            log.warn("Failed to read binary data", exception);
            throw new InternalServerError("Failed to read binary data");
        } catch (NullPointerException npe) {
            log.warn("Failed to read binary data", npe);
            return BinaryContent.ofNull(format);
        }
        int size = inputStream.available();
        Validate.isTrue(len >= 0, "Length can not be negative");
        Validate.inclusiveBetween(0, size, skip, "Skip index is out of range");
        byte[] buffer = new byte[len];
        try {
            inputStream.skip(skip);
            int length = Math.max(inputStream.read(buffer), 0);
            buffer = Arrays.copyOfRange(buffer, 0, length);
            log.info("Successfully read data from input stream, length={}", BinarySizeUnit.B.of(length));
        } catch (IOException e) {
            log.warn("Failed to read data from input stream, length={}", BinarySizeUnit.B.of(len), e);
        }
        return new BinaryContent(buffer, size, format);
    }

    public ResponseEntity<InputStreamResource> downloadBinaryContent(@NotNull String sessionId, @NotNull String sqlId,
            @NotNull Long rowNum, @NotNull Integer colNum) {
        try {
            InputStream inputStream = readBinaryData(sessionId, sqlId, rowNum, colNum);
            return WebResponseUtils.getFileAttachmentResponseEntity(new InputStreamResource(inputStream),
                    String.format("%s_%s-%d_%d"
                            + ".data", sessionId, sqlId, rowNum, colNum));
        } catch (IOException exception) {
            log.warn("Failed to download binary data, sessionId={}, sqlId={}, rowNum={}, colNum={}", sessionId, sqlId,
                    rowNum, colNum, exception);
            throw new InternalServerError("Unknown Error");
        }
    }

    public InputStream readBinaryData(String sessionId, String sqlId, Long rowNum, Integer colNum) throws IOException {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId);
        VirtualTable virtualTable = ConnectionSessionUtil.getQueryCache(connectionSession, sqlId);
        if (virtualTable == null) {
            log.warn("VirtualTable is not found, sqlId={}, session={}", sqlId, connectionSession);
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "SqlId", sqlId);
        }
        if (virtualTable instanceof ResultSetVirtualTable) {
            ResultSetVirtualTable tmpTable = (ResultSetVirtualTable) virtualTable;
            long maxCachedRowId = tmpTable.getMaxCachedRowId();
            long totalCachedSize = tmpTable.getTotalCachedSize();
            if (rowNum > maxCachedRowId) {
                BinarySize size = BinarySizeUnit.B.of(totalCachedSize);
                log.warn(
                        "Failed to request binary data, rowNum={}, maxCachedRowId={}, totalCachedLines={}, totalCachedSize={}",
                        rowNum, maxCachedRowId, tmpTable.getTotalCachedLines(), size);
                size = BinarySizeUnit.B.of(sessionProperties.getResultSetMaxCachedSize());
                throw new NotFoundException(ErrorCodes.TooManyResultSetsToBeCached,
                        new Object[] {sessionProperties.getResultSetMaxCachedLines(), size},
                        "Too many resultsets to be cached");
            }
        }
        virtualTable = virtualTable.select(virtualLine -> Objects.equals(virtualLine.rowId(), rowNum))
                .project(Collections.singletonList(colNum), virtualColumn -> virtualColumn);
        if (virtualTable.count() != 1) {
            log.warn("Virtual table size error, virtualTableCount={}", virtualTable.count());
            throw new InternalServerError("Unknown error");
        }
        Holder<VirtualElement> elementHolder = new Holder<>();
        virtualTable.forEach(virtualLine -> elementHolder.setValue(virtualLine.iterator().next()));
        if (elementHolder.getValue() == null) {
            log.warn("Could not find indexed data in the virtual table, sqlId={}, rowNum={}, colNum={}", sqlId, rowNum,
                    colNum);
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "SqlId", sqlId);
        }
        Object content = elementHolder.getValue().getContent();
        if (!(content instanceof BinaryContentMetaData)) {
            log.warn("Wrong data type, content={}", content);
            throw new BadRequestException(ErrorCodes.BadRequest, new Object[] {"Only binary type cached"},
                    "Only binary type cached");
        }
        BinaryDataManager dataManager = ConnectionSessionUtil.getBinaryDataManager(connectionSession);
        if (dataManager == null) {
            throw new InternalServerError("Data manager is null, Unknown error");
        }
        return dataManager.read((BinaryContentMetaData) content);
    }

    /**
     * Rewrite sqls, will do <br>
     * 1. add ODC_INTERNAL_ROWID query column
     */
    private List<SqlTuple> generateSqlTuple(List<OffsetString> sqls, ConnectionSession session,
            SqlAsyncExecuteReq request) {
        return sqls.stream().filter(s -> StringUtils.isNotBlank(s.getStr())).map(sql -> {
            TraceWatch traceWatch = new TraceWatch("SQL-EXEC");
            SqlTuple target = SqlTuple.newTuple(sql.getStr(), sql.getStr(), traceWatch, sql.getOffset());
            try (TraceStage parseSql = traceWatch.start(SqlExecuteStages.PARSE_SQL)) {
                target.initAst(AbstractSyntaxTreeFactories.getAstFactory(session.getDialectType(), 0));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            if (Objects.isNull(request.getQueryLimit())
                    || !request.ifAddROWID()
                    || session.getDialectType() != DialectType.OB_ORACLE) {
                return target;
            }
            try {
                AbstractSyntaxTree ast = target.getAst();
                BasicResult result = ast.getParseResult();
                if (result instanceof ParseSqlResult && ((ParseSqlResult) result).isSupportAddROWID()) {
                    target = SqlTuple.newTuple(sql.getStr(), rewriteSql(sql.getStr(), session, traceWatch, ast),
                            traceWatch, sql.getOffset());
                    target.initAst(new EmptyAstFactory(ast));
                }
            } catch (Exception e) {
                // eat exception
            }
            return target;
        }).collect(Collectors.toList());
    }

    @SkipAuthorize
    public boolean killCurrentQuery(@NotNull String sessionId) {
        ConnectionSession session = this.sessionService.nullSafeGet(sessionId);
        ConnectionSessionUtil.setConsoleSessionKillQueryFlag(session, true);
        return this.dbSessionManageFacade.killCurrentQuery(session);
    }

    @SkipAuthorize
    public List<KillSessionResult> killSessionOrQuery(KillSessionOrQueryReq request) {
        if (!connectionService.checkPermission(
                Long.valueOf(request.getDatasourceId()), Collections.singletonList("update"))) {
            throw new AccessDeniedException();
        }
        return dbSessionManageFacade.killSessionOrQuery(request);
    }

    private String rewriteSql(String sql, ConnectionSession session, TraceWatch watch, AbstractSyntaxTree ast) {
        try (TraceStage rewriteSql = watch.start(SqlExecuteStages.REWRITE_SQL)) {
            String newSql;
            try (TraceStage applyNewSql = watch.start(SqlExecuteStages.DO_REWRITE_SQL)) {
                newSql = SqlRewriteUtil.addInternalRowIdColumn(sql, ast);
            }
            if (StringUtils.equals(sql, newSql)) {
                return sql;
            }
            try (TraceStage stage = watch.start(SqlExecuteStages.VALIDATE_SEMANTICS)) {
                return validateSqlSemantics(newSql, session) ? newSql : sql;
            }
        } catch (Exception e) {
            log.warn("Failed to rewrite sql, errMessage={}", e.getMessage());
        }
        return sql;
    }

    private boolean validateSqlSemantics(String sql, ConnectionSession session) {
        if (!AllFeatures.getByConnectType(session.getConnectType()).supportsExplain()) {
            return false;
        }
        try {
            session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute("explain " + sql);
            return true;
        } catch (Exception e) {
            log.warn("Failed to validate sql semantics, sql={}, errorMessage={}", sql, LogUtils.prefix(e.getMessage()));
        }
        return false;
    }

    /**
     * for some special sql execution(eg. kill session). This will be required to connect to specific
     * observer
     *
     * @param connectionSession connection engine
     * @param request odc sql object
     * @return result of sql execution
     */
    private SqlAsyncExecuteResp filterKillSession(ConnectionSession connectionSession, SqlAsyncExecuteReq request) {
        String sqlScript = request.getSql().trim().toLowerCase();
        if (!sqlScript.startsWith("kill ") || !sqlScript.contains("/*")) {
            return null;
        }
        List<SqlTuple> sqlTuples = SqlTuple.newTuples(
                Arrays.stream(sqlScript.split(";")).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        List<JdbcGeneralResult> results =
                defaultDbSessionManage.executeKillSession(connectionSession, sqlTuples, sqlScript);
        String id = ConnectionSessionUtil.setFutureJdbc(connectionSession, FutureResult.successResult(results), null);
        return SqlAsyncExecuteResp.newSqlAsyncExecuteResp(id, sqlTuples);
    }

    private SqlExecuteResult generateResult(@NonNull ConnectionSession connectionSession,
            @NonNull JdbcGeneralResult generalResult, @NonNull Map<String, Object> cxt) {
        SqlExecuteResult result = new SqlExecuteResult(generalResult);
        TraceWatch watch = generalResult.getSqlTuple().getSqlWatch();
        OdcTable resultTable = null;
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        try (TraceStage s = watch.start(SqlExecuteStages.INIT_SQL_TYPE)) {
            result.initSqlType(connectionSession.getDialectType());
        } catch (Exception e) {
            log.warn("Failed to init sql type", e);
        }
        try (TraceStage s = watch.start(SqlExecuteStages.INIT_EDITABLE_INFO)) {
            resultTable = result.initEditableInfo();
        } catch (Exception e) {
            log.warn("Failed to init editable info", e);
        }
        if (Boolean.TRUE.equals(cxt.get(SHOW_TABLE_COLUMN_INFO))) {
            try (TraceStage s = watch.start(SqlExecuteStages.INIT_COLUMN_INFO)) {
                result.initColumnInfo(connectionSession, resultTable, schemaAccessor);
            } catch (Exception e) {
                log.warn("Failed to init column comment", e);
            }
        }
        try (TraceStage s = watch.start(SqlExecuteStages.INIT_WARNING_MESSAGE)) {
            result.initWarningMessage(connectionSession);
        } catch (Exception e) {
            log.warn("Failed to init warning message", e);
        }
        return result;
    }

    private Integer checkQueryLimit(Integer queryLimit) {
        if (Objects.isNull(queryLimit)) {
            queryLimit = (int) sessionProperties.getResultSetDefaultRows();
        }
        // if default rows limit is exceeded than max rows limit, still use max rows limit
        if (sessionProperties.getResultSetMaxRows() > 0) {
            return Math.min(queryLimit, (int) sessionProperties.getResultSetMaxRows());
        }
        return queryLimit;
    }

}
