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
package com.oceanbase.odc.service.diagnose;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.diagnose.fulllinktrace.JaegerAdaptor;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

@Service
@SkipAuthorize("inside connect session")
public class SqlDiagnoseService {

    public SqlExplain explain(ConnectionSession session, ResourceSql odcSql) {
        return session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((StatementCallback<SqlExplain>) stmt -> ConnectionPluginUtil
                        .getDiagnoseExtension(session.getDialectType()).getExplain(stmt, odcSql.getSql()));
    }

    public SqlExplain getPhysicalPlan(ConnectionSession session, ResourceSql odcSql) {
        return StringUtils.isBlank(odcSql.getTag())
                ? session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<SqlExplain>) con -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getDialectType()).getPhysicalPlanBySql(con,
                                        odcSql.getSql()))
                : session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<SqlExplain>) con -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getDialectType()).getPhysicalPlanBySqlId(con,
                                        odcSql.getTag()));
    }

    public SqlExecDetail getExecutionDetail(ConnectionSession session, ResourceSql odcSql) {
        return StringUtils.isBlank(odcSql.getTag())
                ? session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<SqlExecDetail>) con -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getDialectType()).getExecutionDetailBySql(con,
                                        odcSql.getSql()))
                : session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<SqlExecDetail>) con -> ConnectionPluginUtil
                                .getDiagnoseExtension(session.getDialectType()).getExecutionDetailById(con,
                                        odcSql.getTag()));
    }

    public TraceSpan getFullLinkTrace(ConnectionSession session, ResourceSql odcSql) throws IOException {
        String traceId = odcSql.getTag();
        BinaryContentMetaData metaData = ConnectionSessionUtil.getBinaryContentMetadata(session, traceId);
        if (metaData == null) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"Trace info", "traceId", traceId},
                    "Trace info not found with traceId=" + traceId);
        }
        InputStream stream = ConnectionSessionUtil.getBinaryDataManager(session).read(metaData);
        return JsonUtils.fromJson(StreamUtils.copyToString(stream, StandardCharsets.UTF_8), TraceSpan.class);
    }

    public String getFullLinkTraceJson(ConnectionSession session, ResourceSql odcSql) throws IOException {
        TraceSpan traceSpan = getFullLinkTrace(session, odcSql);
        return new JaegerAdaptor().adapt(traceSpan);
    }

}
