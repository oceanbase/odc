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
import java.util.concurrent.Future;
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
import com.oceanbase.odc.common.util.ExceptionUtils;
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
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.session.DefaultDBSessionManage;
import com.oceanbase.odc.service.db.session.KillSessionOrQueryReq;
import com.oceanbase.odc.service.db.session.KillSessionResult;
import com.oceanbase.odc.service.dml.ValueEncodeType;
import com.oceanbase.odc.service.feature.AllFeatures;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.queryprofile.OBQueryProfileManager;
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

    public static final int DEFAULT_GET_RESULT_TIMEOUT_SECONDS = 1;
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
    @Autowired
    private OBQueryProfileManager profileManager;

    /**
     * 查询表或视图数据
     *
     * @param sessionId 会话ID
     * @param req 查询表或视图数据请求
     * @return SqlExecuteResult SQL执行结果
     * @throws Exception 异常
     */
    public SqlExecuteResult queryTableOrViewData(@NotNull String sessionId,
            @NotNull @Valid QueryTableOrViewDataReq req) throws Exception {
        // 获取连接会话
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);
        SqlBuilder sqlBuilder;
        // 获取方言类型
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
        // 拼接SQL语句
        sqlBuilder.append("SELECT ");
        if (req.isAddROWID() && connectionSession.getDialectType().isOracle()) {
            sqlBuilder.append(" t.ROWID, ");
        }
        sqlBuilder.append(" t.* ").append(" FROM ")
                .schemaPrefixIfNotBlank(req.getSchemaName()).identifier(req.getTableOrViewName()).append(" t");

        // 检查查询限制
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

        // 异步执行SQL语句
        SqlAsyncExecuteReq asyncExecuteReq = new SqlAsyncExecuteReq();
        asyncExecuteReq.setSql(sqlBuilder.toString());
        // avoid rewrite while execute
        // 避免在执行时重写
        asyncExecuteReq.setAddROWID(false);
        asyncExecuteReq.setQueryLimit(queryLimit);
        asyncExecuteReq.setShowTableColumnInfo(true);
        asyncExecuteReq.setContinueExecutionOnError(true);
        asyncExecuteReq.setFullLinkTraceEnabled(false);
        // SqlAsyncExecuteResp resp = execute(sessionId, asyncExecuteReq, false);
        SqlAsyncExecuteResp resp = streamExecute(sessionId, asyncExecuteReq, false);

        // 处理未授权的数据库资源
        List<UnauthorizedDBResource> unauthorizedDBResources = resp.getUnauthorizedDBResources();
        if (CollectionUtils.isNotEmpty(unauthorizedDBResources)) {
            UnauthorizedDBResource unauthorizedDBResource = unauthorizedDBResources.get(0);
            throw new BadRequestException(ErrorCodes.DatabaseAccessDenied,
                    new Object[] {unauthorizedDBResource.getUnauthorizedPermissionTypes().stream()
                            .map(DatabasePermissionType::getLocalizedMessage).collect(Collectors.joining(","))},
                    "Lack permission for the database with id " + unauthorizedDBResource.getDatabaseId());
        }

        // 获取结果
        String requestId = resp.getRequestId();
        ConnectionConfig connConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);

        List<SqlExecuteResult> results =
                getMoreResults(sessionId, requestId, connConfig.queryTimeoutSeconds()).getResults();

        // 处理结果集超时
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

    public SqlAsyncExecuteResp streamExecute(@NotNull String sessionId, @NotNull @Valid SqlAsyncExecuteReq request)
            throws Exception {
        return streamExecute(sessionId, request, true);
    }

    /**
     * 流式执行SQL异步请求
     *
     * @param sessionId 会话ID
     * @param request SQL异步执行请求
     * @param needSqlRuleCheck 是否需要SQL规则检查
     * @return SQL异步执行响应
     * @throws Exception 异常
     */
    public SqlAsyncExecuteResp streamExecute(@NotNull String sessionId,
            @NotNull @Valid SqlAsyncExecuteReq request, boolean needSqlRuleCheck) throws Exception {
        // 获取连接会话
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);

        // 获取最大SQL长度
        long maxSqlLength = sessionProperties.getMaxSqlLength();
        if (maxSqlLength > 0) {
            // 判断SQL长度是否超过最大值
            PreConditions.lessThanOrEqualTo("sqlLength", LimitMetric.SQL_LENGTH,
                    StringUtils.length(request.getSql()), maxSqlLength);
        }
        // 过滤kill会话
        SqlAsyncExecuteResp result = filterKillSession(connectionSession, request);
        if (result != null) {
            return result;
        }
        // 将SQL分成一句
        List<OffsetString> sqls = request.ifSplitSqls()
                // 如果需要分片执行，则调用SqlUtils工具类的splitWithOffset方法进行分片
                ? SqlUtils.splitWithOffset(connectionSession, request.getSql(),
                        sessionProperties.isOracleRemoveCommentPrefix())
                // 如果不需要分片执行，则将原始SQL语句封装为一个List
                : Collections.singletonList(new OffsetString(0, request.getSql()));
        // 如果SQL只包含分隔符设置（例如delimiter $$），则执行以下代码
        if (sqls.size() == 0) {
            /**
             * if a sql only contains delimiter setting(eg. delimiter $$), code will do this
             */
            SqlTuple sqlTuple = SqlTuple.newTuple(request.getSql());
            AsyncExecuteContext executeContext =
                    new AsyncExecuteContext(Collections.singletonList(sqlTuple), new HashMap<>());
            Future<List<JdbcGeneralResult>> successFuture = FutureResult.successResultList(
                    JdbcGeneralResult.successResult(sqlTuple));
            executeContext.setFuture(successFuture);
            executeContext.addSqlExecutionResults(successFuture.get());
            String id = ConnectionSessionUtil.setExecuteContext(connectionSession, executeContext);
            return SqlAsyncExecuteResp.newSqlAsyncExecuteResp(id, Collections.singletonList(sqlTuple));
        }

        // 获取最大SQL语句数量，判断是否有最大语句数量限制
        long maxSqlStatementCount = sessionProperties.getMaxSqlStatementCount();
        if (maxSqlStatementCount > 0) {
            // 判断SQL语句数量是否超过最大值
            PreConditions.lessThanOrEqualTo("sqlStatementCount",
                    LimitMetric.SQL_STATEMENT_COUNT, sqls.size(), maxSqlStatementCount);
        }

        // 生成SQL元组
        List<SqlTuple> sqlTuples = generateSqlTuple(sqls, connectionSession, request);
        SqlAsyncExecuteResp response = SqlAsyncExecuteResp.newSqlAsyncExecuteResp(sqlTuples);
        // 设置上下文
        Map<String, Object> context = new HashMap<>();
        context.put(SHOW_TABLE_COLUMN_INFO, request.getShowTableColumnInfo());
        context.put(SqlCheckInterceptor.NEED_SQL_CHECK_KEY, needSqlRuleCheck);
        context.put(SqlConsoleInterceptor.NEED_SQL_CONSOLE_CHECK, needSqlRuleCheck);
        AsyncExecuteContext executeContext = new AsyncExecuteContext(sqlTuples, context);
        // 执行SQL前检查，更新sqlTuples中的stageList
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
        // 检查查询限制
        Integer queryLimit = checkQueryLimit(request.getQueryLimit());
        // 判断是否继续执行错误
        boolean continueExecutionOnError =
                Objects.nonNull(request.getContinueExecutionOnError()) ? request.getContinueExecutionOnError()
                        : userConfigFacade.isContinueExecutionOnError();
        boolean stopOnError = !continueExecutionOnError;
        // 创建OdcStatementCallBack对象
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
            statementCallBack.getListeners()
                    .add(new OBQueryProfileExecutionListener(connectionSession, profileManager));
        }

        // 创建一个Future对象，用于异步获取JdbcGeneralResult列表
        Future<List<JdbcGeneralResult>> futureResult = connectionSession.getAsyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY).execute(statementCallBack);
        // 将Future对象设置到executeContext中
        executeContext.setFuture(futureResult);
        // 将executeContext设置到connectionSession中，并获取其id
        String id = ConnectionSessionUtil.setExecuteContext(connectionSession, executeContext);
        // 将id设置到response中
        response.setRequestId(id);
        // 返回response
        return response;
    }

    public AsyncExecuteResultResp getMoreResults(@NotNull String sessionId, String requestId) {
        return getMoreResults(sessionId, requestId, null);
    }

    public AsyncExecuteResultResp getMoreResults(@NotNull String sessionId, String requestId, Integer timeoutSeconds) {
        // 校验请求ID是否为空
        PreConditions.validArgumentState(Objects.nonNull(requestId), ErrorCodes.SqlRegulationRuleBlocked, null, null);
        // 获取连接会话
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId);
        AsyncExecuteContext context =
                (AsyncExecuteContext) ConnectionSessionUtil.getExecuteContext(connectionSession, requestId);
        int gettingResultTimeoutSeconds =
                Objects.isNull(timeoutSeconds) ? DEFAULT_GET_RESULT_TIMEOUT_SECONDS : timeoutSeconds;
        // 判断是否需要移除上下文
        boolean shouldRemoveContext = context.isFinished();
        // 如果出现异常，前端将停止获取更多的结果。在这种情况下，未完成的查询应该被杀死。
        try {
            List<JdbcGeneralResult> resultList =
                    context.getMoreSqlExecutionResults(gettingResultTimeoutSeconds * 1000L);
            // 将JdbcGeneralResult转换为SqlExecuteResult
            List<SqlExecuteResult> results = resultList.stream().map(jdbcGeneralResult -> {
                SqlExecuteResult result = generateResult(connectionSession, jdbcGeneralResult, context.getContextMap());
                // 执行SQL后置拦截
                try (TraceStage stage = result.getSqlTuple().getSqlWatch().start(SqlExecuteStages.SQL_AFTER_CHECK)) {
                    sqlInterceptService.afterCompletion(result, connectionSession, context);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return result;
            }).collect(Collectors.toList());
            // 返回异步执行结果响应
            return new AsyncExecuteResultResp(shouldRemoveContext, context, results);
        } catch (Exception e) {
            shouldRemoveContext = true;
            // Front-end would stop getting more results if there is an exception. In this case the left queries
            // should be killed.
            // 如果出现异常，前端将停止获取更多的结果。在这种情况下，未完成的查询应该被杀死。
            try {
                killCurrentQuery(sessionId);
            } catch (Exception ex) {
                log.warn("Failed to kill query. Session id={}. Request id={}", sessionId, requestId);
            }
            throw e;
        } finally {
            // 如果需要移除上下文，则从连接会话中移除执行上下文
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

    /**
     * 读取二进制数据
     *
     * @param sessionId 会话ID
     * @param sqlId SQL ID
     * @param rowNum 行号
     * @param colNum 列号
     * @return InputStream 输入流
     * @throws IOException IO异常
     */
    public InputStream readBinaryData(String sessionId, String sqlId, Long rowNum, Integer colNum) throws IOException {
        // 获取连接会话
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId);
        // 从查询缓存中获取虚拟表
        VirtualTable virtualTable = ConnectionSessionUtil.getQueryCache(connectionSession, sqlId);
        // 如果虚拟表不存在，则抛出NotFoundException异常
        if (virtualTable == null) {
            log.warn("VirtualTable is not found, sqlId={}, session={}", sqlId, connectionSession);
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "SqlId", sqlId);
        }
        // 如果虚拟表是ResultSetVirtualTable类型，则进行判断
        if (virtualTable instanceof ResultSetVirtualTable) {
            ResultSetVirtualTable tmpTable = (ResultSetVirtualTable) virtualTable;
            long maxCachedRowId = tmpTable.getMaxCachedRowId();
            long totalCachedSize = tmpTable.getTotalCachedSize();
            // 如果行号大于最大缓存行号，则抛出NotFoundException异常
            if (rowNum > maxCachedRowId) {
                BinarySize size = BinarySizeUnit.B.of(totalCachedSize);
                log.warn(
                        "Failed to request binary data, rowNum={}, maxCachedRowId={}, totalCachedLines={}, "
                                + "totalCachedSize={}",
                        rowNum, maxCachedRowId, tmpTable.getTotalCachedLines(), size);
                size = BinarySizeUnit.B.of(sessionProperties.getResultSetMaxCachedSize());
                throw new NotFoundException(ErrorCodes.TooManyResultSetsToBeCached,
                        new Object[] {sessionProperties.getResultSetMaxCachedLines(), size},
                        "Too many resultsets to be cached");
            }
        }
        // 选择指定行并投影指定列
        virtualTable = virtualTable.select(virtualLine -> Objects.equals(virtualLine.rowId(), rowNum))
                .project(Collections.singletonList(colNum), virtualColumn -> virtualColumn);
        // 如果虚拟表大小不为1，则抛出InternalServerError异常
        if (virtualTable.count() != 1) {
            log.warn("Virtual table size error, virtualTableCount={}", virtualTable.count());
            throw new InternalServerError("Unknown error");
        }
        // 获取虚拟元素持有者
        Holder<VirtualElement> elementHolder = new Holder<>();
        virtualTable.forEach(virtualLine -> elementHolder.setValue(virtualLine.iterator().next()));
        // 如果虚拟元素为空，则抛出NotFoundException异常
        if (elementHolder.getValue() == null) {
            log.warn("Could not find indexed data in the virtual table, sqlId={}, rowNum={}, colNum={}", sqlId, rowNum,
                    colNum);
            throw new NotFoundException(ResourceType.ODC_ASYNC_SQL_RESULT, "SqlId", sqlId);
        }
        // 获取内容
        Object content = elementHolder.getValue().getContent();
        // 如果内容不是BinaryContentMetaData类型，则抛出BadRequestException异常
        if (!(content instanceof BinaryContentMetaData)) {
            log.warn("Wrong data type, content={}", content);
            throw new BadRequestException(ErrorCodes.BadRequest, new Object[] {"Only binary type cached"},
                    "Only binary type cached");
        }
        // 获取二进制数据管理器
        BinaryDataManager dataManager = ConnectionSessionUtil.getBinaryDataManager(connectionSession);
        // 如果二进制数据管理器为空，则抛出InternalServerError异常
        if (dataManager == null) {
            throw new InternalServerError("Data manager is null, Unknown error");
        }
        // 返回输入流
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
            // 开始跟踪解析 SQL 的时间
            try (TraceStage parseSql = traceWatch.start(SqlExecuteStages.PARSE_SQL)) {
                // 初始化 AST（抽象语法树）
                target.initAst(AbstractSyntaxTreeFactories.getAstFactory(session.getDialectType(), 0));
            } catch (IOException e) {
                // 捕获 IO 异常并抛出非法状态异常
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
    private SqlAsyncExecuteResp filterKillSession(ConnectionSession connectionSession, SqlAsyncExecuteReq request)
            throws Exception {
        // 获取 SQL 语句并转换为小写
        String sqlScript = request.getSql().trim().toLowerCase();
        // 如果 SQL 语句不是以 "kill " 开头或不包含 "/*"，则返回 null
        if (!sqlScript.startsWith("kill ") || !sqlScript.contains("/*")) {
            return null;
        }
        // 将 SQL 语句按 ";" 分割，并过滤掉空白项，然后转换为 SqlTuple 列表
        List<SqlTuple> sqlTuples = SqlTuple.newTuples(
                Arrays.stream(sqlScript.split(";")).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        // 执行 kill session 操作，并获取 JdbcGeneralResult 列表
        List<JdbcGeneralResult> results =
                defaultDbSessionManage.executeKillSession(connectionSession, sqlTuples, sqlScript);

        // 创建 AsyncExecuteContext 对象
        AsyncExecuteContext executeContext =
                new AsyncExecuteContext(sqlTuples, new HashMap<>());
        // 创建 SuccessFuture 对象，并设置执行结果
        Future<List<JdbcGeneralResult>> successFuture = FutureResult.successResult(results);
        executeContext.setFuture(successFuture);
        executeContext.addSqlExecutionResults(successFuture.get());
        // 将 AsyncExecuteContext 对象设置到 ConnectionSession 中，并返回 SqlAsyncExecuteResp 对象
        String id = ConnectionSessionUtil.setExecuteContext(connectionSession, executeContext);
        return SqlAsyncExecuteResp.newSqlAsyncExecuteResp(id, sqlTuples);
    }

    /**
     * 生成 SQL 执行结果
     *
     * @param connectionSession 数据库连接会话
     * @param generalResult 普通 SQL 执行结果
     * @param cxt 上下文参数
     * @return SQL 执行结果
     */
    private SqlExecuteResult generateResult(@NonNull ConnectionSession connectionSession,
            @NonNull JdbcGeneralResult generalResult, @NonNull Map<String, Object> cxt) {
        // 创建 SQL 执行结果对象
        SqlExecuteResult result = new SqlExecuteResult(generalResult);
        // 获取 SQL 执行时间计时器
        TraceWatch watch = generalResult.getSqlTuple().getSqlWatch();
        // 初始化可编辑表信息
        OdcTable resultTable = null;
        // 创建数据库模式访问器
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        try (TraceStage s = watch.start(SqlExecuteStages.INIT_SQL_TYPE)) {
            // 初始化 SQL 类型
            result.initSqlType(connectionSession.getDialectType());
        } catch (Exception e) {
            log.warn("Failed to init sql type", e);
        }
        try (TraceStage s = watch.start(SqlExecuteStages.INIT_EDITABLE_INFO)) {
            // 初始化可编辑表信息
            resultTable = result.initEditableInfo();
        } catch (Exception e) {
            log.warn("Failed to init editable info", e);
        }
        if (Boolean.TRUE.equals(cxt.get(SHOW_TABLE_COLUMN_INFO))) {
            try (TraceStage s = watch.start(SqlExecuteStages.INIT_COLUMN_INFO)) {
                // 初始化列信息
                result.initColumnInfo(connectionSession, resultTable, schemaAccessor);
            } catch (Exception e) {
                log.warn("Failed to init column comment, reason={}", ExceptionUtils.getSimpleReason(e));
            }
        }
        try (TraceStage s = watch.start(SqlExecuteStages.INIT_WARNING_MESSAGE)) {
            // 初始化警告信息
            result.initWarningMessage(connectionSession);
        } catch (Exception e) {
            log.warn("Failed to init warning message", e);
        }
        try {
            // 判断是否支持查询分析
            String version = ConnectionSessionUtil.getVersion(connectionSession);
            result.setWithQueryProfile(OBQueryProfileExecutionListener
                    .isSqlTypeSupportProfile(generalResult.getSqlTuple()) &&
                    OBQueryProfileExecutionListener.isObVersionSupportQueryProfile(version));
        } catch (Exception e) {
            result.setWithQueryProfile(false);
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
