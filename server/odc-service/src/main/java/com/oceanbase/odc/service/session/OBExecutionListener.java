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

import static com.oceanbase.odc.core.session.ConnectionSessionConstants.BACKEND_DS_KEY;
import static com.oceanbase.odc.core.session.ConnectionSessionConstants.CONSOLE_DS_KEY;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/23
 */
public class OBExecutionListener implements SqlExecutionListener {
    private static final Long DEFAULT_QUERY_TRACE_ID_WAIT_MILLIS = 1100L;
    private static final String ENABLE_QUERY_PROFILE_VERSION = "4.2";

    private final ConnectionSession session;
    private final List<String> sessionIds;

    public OBExecutionListener(ConnectionSession session) {
        this.session = session;
        sessionIds = getSessionIds();
    }

    @Override
    public void onExecutionStart(SqlTuple sqlTuple, AsyncExecuteContext context) {}

    @Override
    public void onExecutionEnd(SqlTuple sqlTuple, List<JdbcGeneralResult> results, AsyncExecuteContext context) {}

    @Override
    public void onExecutionCancelled(SqlTuple sqlTuple, List<JdbcGeneralResult> results, AsyncExecuteContext context) {}

    public void onExecutionStartAfter(SqlTuple sqlTuple, AsyncExecuteContext context) {
        if (CollectionUtils.isEmpty(sessionIds)) {
            return;
        }
        String traceId = session.getSyncJdbcExecutor(BACKEND_DS_KEY).execute((StatementCallback<String>) stmt -> OBUtils
                .queryTraceIdFromASH(stmt, sessionIds, session.getConnectType()));
        if (traceId != null) {
            context.setCurrentExecutingSqlTraceId(traceId);
        }
    }

    @Override
    public Long getOnExecutionStartAfterMillis() {
        return DEFAULT_QUERY_TRACE_ID_WAIT_MILLIS;
    }

    private List<String> getSessionIds() {
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(session), ENABLE_QUERY_PROFILE_VERSION)) {
            return Collections.emptyList();
        }
        String proxySessId = ConnectionSessionUtil.getConsoleConnectionProxySessId(session);
        if (StringUtils.isEmpty(proxySessId)) {
            return Collections.singletonList(ConnectionSessionUtil.getConsoleConnectionId(session));
        }
        return session.getSyncJdbcExecutor(CONSOLE_DS_KEY).execute((StatementCallback<List<String>>) stmt -> OBUtils
                .querySessionIdsByProxySessId(stmt, proxySessId, session.getConnectType()));
    }
}
