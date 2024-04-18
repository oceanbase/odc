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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/15
 */
@Getter
@Slf4j
public class AsyncExecuteContext<T> {
    public static final String SHOW_TABLE_COLUMN_INFO = "SHOW_TABLE_COLUMN_INFO";

    private final Function<JdbcGeneralResult, T> mapper;
    private final ConnectionSession session;
    private final List<SqlTuple> sqlTuples;
    private final Queue<T> results = new ConcurrentLinkedQueue<>();
    private final List<BiConsumer<ConnectionSession, String>> traceIdHooks = new ArrayList<>();
    private final User user;
    private final Long waitTimeMillis;
    private final List<String> sessionIds;

    @Setter
    private Future<List<JdbcGeneralResult>> future;
    private String currentTraceId;
    private int count = 0;

    public AsyncExecuteContext(ConnectionSession session, List<SqlTuple> sqlTuples,
            Function<JdbcGeneralResult, T> mapper, User user, Long waitTimeMillis) {
        this.session = session;
        this.sqlTuples = sqlTuples;
        this.mapper = mapper;
        this.user = user;
        this.waitTimeMillis = waitTimeMillis;
        this.sessionIds = initSessionIds(session);
    }

    public int getTotal() {
        return sqlTuples.size();
    }

    public List<T> getResults() {
        List<T> copiedResults = new ArrayList<>();
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

    public void onQueryStart() {
        count++;
    }

    public void onQueryExecuting() {
        String traceId = session.getSyncJdbcExecutor(BACKEND_DS_KEY)
                .execute((StatementCallback<String>) stmt -> OBUtils
                        .queryTraceIdFromASH(stmt, sessionIds, session.getConnectType()));
        if (traceId != null) {
            setCurrentTraceId(traceId);
        }
    }

    public void onQueryFinish(List<JdbcGeneralResult> results) {
        SecurityContextUtils.setCurrentUser(user);
        try {
            for (JdbcGeneralResult result : results) {
                this.results.add(mapper.apply(result));
                for (BiConsumer<ConnectionSession, String> hook : traceIdHooks) {
                    hook.accept(session, result.getTraceId());
                }
            }
        } finally {
            SecurityContextUtils.clear();
        }
    }

    private List<String> initSessionIds(ConnectionSession session) {
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(session), "4.2")
                || sqlTuples.size() > 10) {
            return null;
        }
        String proxySessId = ConnectionSessionUtil.getConsoleConnectionProxySessId(session);
        if (StringUtils.isEmpty(proxySessId)) {
            return Collections.singletonList(ConnectionSessionUtil.getConsoleConnectionId(session));
        }
        return session.getSyncJdbcExecutor(CONSOLE_DS_KEY).execute((StatementCallback<List<String>>) stmt -> OBUtils
                .querySessionIdsByProxySessId(stmt, proxySessId, session.getConnectType()));
    }
}
