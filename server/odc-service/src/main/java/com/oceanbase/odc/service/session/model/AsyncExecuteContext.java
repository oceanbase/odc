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
package com.oceanbase.odc.service.session.model;

import static com.oceanbase.odc.core.session.ConnectionSessionConstants.BACKEND_DS_KEY;
import static com.oceanbase.odc.core.session.ConnectionSessionConstants.CONSOLE_DS_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.common.util.TraceWatch;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.session.interceptor.SqlExecuteInterceptorService;
import com.oceanbase.odc.service.session.model.OdcResultSetMetaData.OdcTable;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/15
 */
@Getter
@Slf4j
public class AsyncExecuteContext {
    public static final String SHOW_TABLE_COLUMN_INFO = "SHOW_TABLE_COLUMN_INFO";

    private final ConnectionSession session;
    private final List<SqlTuple> sqlTuples;
    private final Queue<SqlExecuteResult> results = new ConcurrentLinkedQueue<>();
    private final Map<String, Object> contextMap = new HashMap<>();
    private final SqlExecuteInterceptorService sqlInterceptService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<BiConsumer<ConnectionSession, String>> traceIdHooks = new ArrayList<>();
    private final User user;
    private final List<String> sessionIds;
    private final boolean queryRuntimeTrace;

    @Setter
    private Future<List<JdbcGeneralResult>> future;
    @Setter
    private String currentTraceId;
    private Future<Void> handle;
    private int count = 0;

    public AsyncExecuteContext(ConnectionSession session, List<SqlTuple> sqlTuples,
            SqlExecuteInterceptorService sqlInterceptService, User user) {
        this.session = session;
        this.sqlTuples = sqlTuples;
        this.sqlInterceptService = sqlInterceptService;
        this.user = user;
        this.queryRuntimeTrace = VersionUtils.isGreaterThanOrEqualsTo(ConnectionSessionUtil.getVersion(session), "4.2")
                && sqlTuples.size() <= 10;
        this.sessionIds = initSessionIds(session);
    }

    public boolean await(long timeout, TimeUnit unit) {
        if (future == null) {
            return false;
        }
        try {
            future.get(timeout, unit);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public int getTotal() {
        return sqlTuples.size();
    }

    public List<SqlExecuteResult> getResults() {
        List<SqlExecuteResult> copiedResults = new ArrayList<>();
        while (!results.isEmpty()) {
            copiedResults.add(results.poll());
        }
        return copiedResults;
    }

    public void setCurrentTraceId(String traceId) {
        currentTraceId = traceId;
        for (BiConsumer<ConnectionSession, String> hook : traceIdHooks) {
            hook.accept(session, traceId);
        }
    }

    public void startQuery(CountDownLatch latch) {
        count++;
        if (handle != null && !handle.isDone()) {
            handle.cancel(true);
        }
        if (queryRuntimeTrace) {
            handle = executor.submit(() -> {
                try {
                    if (!latch.await(1100, TimeUnit.MILLISECONDS)) {
                        String traceId = session.getSyncJdbcExecutor(BACKEND_DS_KEY)
                                .execute((StatementCallback<String>) stmt -> OBUtils
                                        .queryTraceIdFromASH(stmt, sessionIds, session.getConnectType()));
                        if (traceId != null) {
                            setCurrentTraceId(traceId);
                        }
                    }
                } catch (InterruptedException e) {
                    log.warn("Failed to get trace id.", e);
                }
                return null;
            });
        }
    }

    public void finishQuery(List<JdbcGeneralResult> results) {
        if (handle != null && !handle.isDone()) {
            handle.cancel(true);
        }
        SecurityContextUtils.setCurrentUser(user);
        try {
            for (JdbcGeneralResult result : results) {
                this.results.add(mapResult(result));
                for (BiConsumer<ConnectionSession, String> hook : traceIdHooks) {
                    hook.accept(session, result.getTraceId());
                }
            }
        } finally {
            SecurityContextUtils.clear();
        }
    }

    private List<String> initSessionIds(ConnectionSession session) {
        if (!queryRuntimeTrace) {
            return null;
        }
        String proxySessId = ConnectionSessionUtil.getConsoleConnectionProxySessId(session);
        if (StringUtils.isEmpty(proxySessId)) {
            return Collections.singletonList(ConnectionSessionUtil.getConsoleConnectionId(session));
        }
        return session.getSyncJdbcExecutor(CONSOLE_DS_KEY).execute((StatementCallback<List<String>>) stmt -> OBUtils
                .querySessionIdsByProxySessId(stmt, proxySessId, session.getConnectType()));
    }

    private SqlExecuteResult mapResult(JdbcGeneralResult jdbcResult) {
        SqlExecuteResult res = generateResult(session, jdbcResult, contextMap);
        TraceWatch traceWatch = res.getSqlTuple().getSqlWatch();
        try (TraceStage stage = traceWatch.start(SqlExecuteStages.SQL_AFTER_CHECK)) {
            sqlInterceptService.afterCompletion(res, session, contextMap);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (!traceWatch.isClosed()) {
            traceWatch.close();
        }
        return res;
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

}
