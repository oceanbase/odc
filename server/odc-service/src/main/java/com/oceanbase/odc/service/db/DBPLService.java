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
package com.oceanbase.odc.service.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.model.BatchCompileResp;
import com.oceanbase.odc.service.db.model.BatchCompileStatus;
import com.oceanbase.odc.service.db.model.CallFunctionReq;
import com.oceanbase.odc.service.db.model.CallFunctionResp;
import com.oceanbase.odc.service.db.model.CallProcedureReq;
import com.oceanbase.odc.service.db.model.CallProcedureResp;
import com.oceanbase.odc.service.db.model.CompileResult;
import com.oceanbase.odc.service.db.model.DBMSOutput;
import com.oceanbase.odc.service.db.model.PLIdentity;
import com.oceanbase.odc.service.db.model.StartBatchCompileReq;
import com.oceanbase.odc.service.db.util.OBMysqlCallFunctionCallBack;
import com.oceanbase.odc.service.db.util.OBMysqlCallProcedureCallBack;
import com.oceanbase.odc.service.db.util.OBOracleCallFunctionBlockCallBack;
import com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBack;
import com.oceanbase.odc.service.db.util.OBOracleCompilePLCallBack;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.odc.service.session.SessionProperties;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParsePLResult;
import com.oceanbase.tools.dbbrowser.util.ALLDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/6/10
 */

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBPLService {

    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private DBResourcePermissionHelper permissionHelper;

    private static final Integer DEFAULT_MAX_CONCURRENT_BATCH_COMPILE_TASK_COUNT = 10;
    private final DefaultSqlExecuteTaskManager taskManager;
    private final Map<String, Pair<BatchCompileTaskCallable, Future<BatchCompileResp>>> runningTaskMap;
    private final Cache<String, Pair<BatchCompileTaskCallable, Future<BatchCompileResp>>> endTaskCache;

    public DBPLService() {
        this.taskManager =
                new DefaultSqlExecuteTaskManager(DEFAULT_MAX_CONCURRENT_BATCH_COMPILE_TASK_COUNT,
                        "PL-batch-compile-service");
        this.runningTaskMap = new ConcurrentHashMap<>();
        this.endTaskCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();
    }

    @PreDestroy
    public void destroy() {
        log.info("PLDebug Service start to destroy...");
        try {
            taskManager.close();
        } catch (Exception e) {
            log.warn("Error occurs while destroying PLService", e);
        }
        log.info("PLDebug Service destroyed");
    }

    @SkipAuthorize("inside connect session")
    public String startBatchCompile(@NonNull ConnectionSession session,
            String databaseName, StartBatchCompileReq req) {
        if (Objects.nonNull(session.getDialectType()) && session.getDialectType().isMysql()) {
            throw new UnsupportedException("Batch compile is not supported in mysql mode");
        }
        if (Objects.nonNull(session.getDialectType()) && session.getDialectType().isDoris()) {
            throw new UnsupportedException("Batch compile is not supported in doris mode");
        }
        Validate.notNull(req.getScope(), "Parameter [scope] can not be null");
        List<DBPLObjectIdentity> identities = getPLList(session, req.getObjectType(), "INVALID".equals(req.getScope()));
        Set<String> plNamesSet = new HashSet<>();
        identities = identities.stream().filter(i -> {
            String name = i.getName();
            DBObjectType type = i.getType();
            if (OdcConstants.PL_DEBUG_PACKAGE.equals(name)) {
                return false;
            }
            if (DBObjectType.PACKAGE == type) {
                if (plNamesSet.contains(name)) {
                    return false;
                }
            } else if (DBObjectType.PACKAGE_BODY == type) {
                if (plNamesSet.contains(name)) {
                    return false;
                }
                i.setType(DBObjectType.PACKAGE);
            }
            plNamesSet.add(i.getName());
            return true;
        }).collect(Collectors.toList());

        return startBatchCompile(session, databaseName, identities);
    }

    @SkipAuthorize("inside connect session")
    public String startBatchCompile(@NonNull ConnectionSession session,
            String databaseName, @NonNull List<DBPLObjectIdentity> identities) {
        if (Objects.nonNull(session.getDialectType()) && session.getDialectType().isMysql()) {
            throw new UnsupportedException("Batch compile is not supported in mysql mode");
        }
        if (Objects.nonNull(session.getDialectType()) && session.getDialectType().isDoris()) {
            throw new UnsupportedException("Batch compile is not supported in doris mode");
        }
        BatchCompileTaskCallable taskCallable = new BatchCompileTaskCallable(session, identities);
        Future<BatchCompileResp> handle;
        String taskId;
        if (StringUtils.isBlank(databaseName)) {
            // it means batch compile refers to current database
            handle = taskManager.submit(taskCallable);
            taskId = UUID.randomUUID().toString();
        } else {
            throw new UnsupportedException("Batch compile PL in another database is not supported yet");
        }
        runningTaskMap.put(taskId, new Pair<>(taskCallable, handle));
        return taskId;
    }

    public CompileResult compile(@NonNull ConnectionSession session, @NonNull PLIdentity compile) {
        DBPLObjectIdentity identity = new DBPLObjectIdentity();
        identity.setType(compile.getObDbObjectType());
        identity.setName(compile.getPlName());
        identity.setSchemaName(ConnectionSessionUtil.getCurrentSchema(session));
        SyncJdbcExecutor jdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        OBOracleCompilePLCallBack callBack = new OBOracleCompilePLCallBack(identity, jdbcExecutor);
        CompileResult result = new CompileResult();
        try {
            String warning = jdbcExecutor.execute(callBack);
            if (com.oceanbase.tools.dbbrowser.util.StringUtils.isEmpty(warning)) {
                result.setSuccessful(true);
            } else {
                result.setSuccessful(false);
                result.setErrorMessage(warning);
            }
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setErrorMessage(e.getMessage());
            if (e.getCause() != null) {
                result.setErrorMessage(e.getCause().getMessage());
            }
        }
        return result;
    }

    public DBPLObjectIdentity parsePLNameType(@NonNull ConnectionSession session, @NonNull String ddl) {
        DBPLObjectIdentity plObject = new DBPLObjectIdentity();
        Validate.notNull(session.getDialectType(), "Dialect type can not be null");
        List<String> sqls = SqlCommentProcessor.removeSqlComments(ddl + "$$", "$$",
                session.getDialectType(), true).stream().map(OffsetString::getStr).collect(Collectors.toList());
        if (!sqls.isEmpty()) {
            ddl = sqls.get(0);
        }
        ParsePLResult result;
        if (session.getDialectType().isOracle()) {
            result = PLParser.parseOracle(ddl);
        } else if (session.getDialectType().isMysql()) {
            result = PLParser.parseObMysql(ddl);
        } else if (session.getDialectType().isDoris()) {
            result = PLParser.parseObMysql(ddl);
        } else {
            throw new UnsupportedException(
                    "Connection type=" + session.getDialectType() + " is not supported to parse pl");
        }
        String plName = result.getPlName();
        plObject.setName(plName);
        plObject.setType(DBObjectType.getEnumByName(result.getPlType()));
        return plObject;
    }

    public String callProcedure(@NonNull ConnectionSession session, @NonNull CallProcedureReq req) {
        Long databaseId = getDatabaseIdByConnectionSession(session);
        permissionHelper.checkDBPermissions(Collections.singleton(databaseId),
                Collections.singleton(DatabasePermissionType.CHANGE));
        ConnectionCallback<CallProcedureResp> callback;
        DialectType dialectType = session.getDialectType();
        if (dialectType.isOracle()) {
            callback = new OBOracleCallProcedureBlockCallBack(req, -1);
        } else if (dialectType.isMysql()) {
            callback = new OBMysqlCallProcedureCallBack(req, -1);
        } else {
            throw new IllegalArgumentException("Illegal dialect type, " + dialectType);
        }
        Future<CallProcedureResp> future = session.getAsyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY).execute(callback);
        return ConnectionSessionUtil.setFutureJdbc(session, future, new HashMap<>());
    }

    public <T> T getAsyncCallingResult(@NonNull ConnectionSession session,
            @NonNull String resultId, Integer timeoutSeconds) {
        int timeout = Objects.isNull(timeoutSeconds)
                ? ConnectConsoleService.DEFAULT_GET_RESULT_TIMEOUT_SECONDS
                : timeoutSeconds;
        Future<T> future = ConnectionSessionUtil.getFutureJdbcResult(session, resultId);
        try {
            T callingResult = future.get(timeout, TimeUnit.SECONDS);
            ConnectionSessionUtil.removeFutureJdbc(session, resultId);
            return callingResult;
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (TimeoutException timeoutException) {
            return null;
        }
    }

    public String callFunction(@NonNull ConnectionSession session, @NonNull CallFunctionReq req) {
        Long databaseId = getDatabaseIdByConnectionSession(session);
        permissionHelper.checkDBPermissions(Collections.singleton(databaseId),
                Collections.singleton(DatabasePermissionType.CHANGE));
        ConnectionCallback<CallFunctionResp> callback;
        DialectType dialectType = session.getDialectType();
        if (dialectType.isOracle()) {
            callback = new OBOracleCallFunctionBlockCallBack(req, -1);
        } else if (dialectType.isMysql()) {
            callback = new OBMysqlCallFunctionCallBack(req, -1);
        } else {
            throw new IllegalArgumentException("Illegal dialect type, " + dialectType);
        }
        Future<CallFunctionResp> future = session.getAsyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY).execute(callback);
        return ConnectionSessionUtil.setFutureJdbc(session, future, new HashMap<>());
    }

    public DBMSOutput getLine(ConnectionSession session) {
        DBMSOutput output = new DBMSOutput();
        try {
            int dbmsoutputMaxRows = sessionProperties.getDbmsOutputMaxRows();
            output.setLine(session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                    .execute((ConnectionCallback<String>) con -> OBUtils.queryDBMSOutput(con, dbmsoutputMaxRows)));
        } catch (Exception e) {
            output.setLine(e.getMessage());
            if (e.getCause() != null) {
                output.setLine(e.getCause().getMessage());
            }
        }
        return output;
    }

    // TODO：水平权限风险
    public Boolean endBatchCompile(String id) {
        Pair<BatchCompileTaskCallable, Future<BatchCompileResp>> taskIdentity = nullSafeGet(id);
        endTaskCache.put(id, taskIdentity);
        runningTaskMap.remove(id);
        return taskIdentity.right.cancel(true);
    }

    // TODO：水平权限风险
    public BatchCompileResp getBatchCompileResult(String id) {
        Pair<BatchCompileTaskCallable, Future<BatchCompileResp>> taskIdentity = endTaskCache.getIfPresent(id);
        if (Objects.isNull(taskIdentity)) {
            taskIdentity = nullSafeGet(id);
        }
        Future<BatchCompileResp> futureResult = taskIdentity.right;
        BatchCompileResp resp = new BatchCompileResp();
        if (futureResult.isDone()) {
            try {
                resp = futureResult.get();
                if (Objects.isNull(resp.getStatus())) {
                    resp.setStatus(BatchCompileStatus.COMPLETED);
                }
            } catch (Exception e) {
                log.warn("Error occurs while getting result for batch compile id={}", id, e);
                throw new RuntimeException(
                        String.format("Error occurs while getting result for batch compile id=%s", id), e);
            }
        } else {
            resp.setStatus(BatchCompileStatus.RUNNING);
        }
        resp.setTotalCount(taskIdentity.left.getIdentities().size());
        resp.setCompletedCount(taskIdentity.left.getCompletedCompileCounter());
        return resp;
    }

    private Pair<BatchCompileTaskCallable, Future<BatchCompileResp>> nullSafeGet(String id) {
        Pair<BatchCompileTaskCallable, Future<BatchCompileResp>> taskIdentity = runningTaskMap.get(id);
        if (Objects.isNull(taskIdentity)) {
            throw new RuntimeException(String.format("BatchCompile id=%s does not match any task", id));
        }
        return taskIdentity;
    }

    private List<DBPLObjectIdentity> getPLList(@NonNull ConnectionSession session,
            DBObjectType objectType, boolean isInvalid) {
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        OracleDataDictTableNames tableNames = new ALLDataDictTableNames();
        sqlBuilder.append("SELECT OBJECT_NAME,STATUS,OBJECT_TYPE,OWNER FROM").space()
                .append(tableNames.OBJECTS()).space()
                .append("WHERE").space();
        if (Objects.nonNull(objectType)) {
            if (DBObjectType.PACKAGE == objectType || DBObjectType.PACKAGE_BODY == objectType) {
                sqlBuilder.append("(OBJECT_TYPE='PACKAGE' OR OBJECT_TYPE='PACKAGE BODY')").space()
                        .append("AND").space();
            } else {
                sqlBuilder.append("OBJECT_TYPE=").space()
                        .value(objectType.name()).space().append("AND").space();
            }
        } else {
            sqlBuilder.append("OBJECT_TYPE in ('FUNCTION','PROCEDURE','PACKAGE','PACKAGE BODY','TRIGGER','TYPE')")
                    .space().append("AND").space();
        }
        if (isInvalid) {
            sqlBuilder.append("STATUS=").space().value("INVALID").space()
                    .append("AND").space();
        }
        sqlBuilder.append("OWNER=").space().value(ConnectionSessionUtil.getCurrentSchema(session)).space()
                .append("ORDER BY OBJECT_NAME ASC");
        return jdbcOperations.query(sqlBuilder.toString(), (rs, rowNum) -> {
            DBPLObjectIdentity identity = new DBPLObjectIdentity();
            identity.setName(rs.getString(1));
            identity.setType(DBObjectType.getEnumByName(rs.getString(3)));
            identity.setSchemaName(rs.getString(4));
            return identity;
        });
    }

    private Long getDatabaseIdByConnectionSession(@NonNull ConnectionSession session) {
        ConnectionConfig connConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        String schemaName = ConnectionSessionUtil.getCurrentSchema(session);
        DatabaseEntity databaseEntity = databaseRepository.findByConnectionIdAndName(connConfig.getId(), schemaName)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_DATABASE, "name", schemaName));
        return databaseEntity.getId();
    }

}
