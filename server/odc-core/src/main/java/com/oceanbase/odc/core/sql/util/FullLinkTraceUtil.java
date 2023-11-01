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
package com.oceanbase.odc.core.sql.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.shared.model.TraceSpan.Node;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;

public class FullLinkTraceUtil {

    private final static String TRACE_ID_KEY = "log_trace_id";

    public static SqlExecTime getFullLinkTraceDetail(Statement statement) throws SQLException {
        OceanBaseConnection connection = (OceanBaseConnection) statement.getConnection();
        long lastPacketResponseTimestamp =
                TimeUnit.MICROSECONDS.convert(connection.getLastPacketResponseTimestamp(),
                        TimeUnit.MILLISECONDS);
        long lastPacketSendTimestamp = TimeUnit.MICROSECONDS.convert(connection.getLastPacketSendTimestamp(),
                TimeUnit.MILLISECONDS);

        try (ResultSet resultSet = statement.executeQuery("show trace format='json'")) {
            if (!resultSet.next()) {
                throw new UnexpectedException("No trace info, maybe value of ob_enable_show_trace is 0.");
            }
            String showTraceJson = resultSet.getString(1);
            SqlExecTime execDetail;
            try {
                execDetail = parseSpanList(JsonUtils.fromJsonList(showTraceJson, TraceSpan.class));
            } catch (Exception e) {
                throw new UnexpectedException("Parse trace failed, original text: " + showTraceJson, e);
            }

            execDetail.setLastPacketSendTimestamp(lastPacketSendTimestamp);
            execDetail.setLastPacketResponseTimestamp(lastPacketResponseTimestamp);
            return execDetail;
        }
    }

    private static SqlExecTime parseSpanList(List<TraceSpan> spanList) {
        SqlExecTime execDetail = new SqlExecTime();
        // space for efficiency
        Map<String, TraceSpan> spanMap = new LinkedHashMap<>();
        String traceId = null;
        long execTime = 0;
        for (TraceSpan span : spanList) {
            spanMap.put(span.getSpanId(), span);
            if (span.getParent() != null) {
                spanMap.computeIfPresent(span.getParent(), (k, v) -> {
                    v.getSubSpans().add(span);
                    return v;
                });
            }
            if (traceId == null) {
                traceId = findLogTraceId(span.getTags());
            }
            span.setNode(Node.from(span.getSpanName()));
            if ("com_query_process".equals(span.getSpanName())) {
                execDetail.setElapsedMicroseconds(span.getElapseMicroSeconds());
            } else if ("sql_execute".equals(span.getSpanName())) {
                execTime += span.getElapseMicroSeconds();
            }
        }
        if (spanMap.isEmpty()) {
            throw new UnexpectedException("Cannot get any trace info!");
        }
        // the first span is root
        TraceSpan root = spanMap.values().iterator().next();
        root.setLogTraceId(traceId);
        execDetail.setTraceSpan(root);
        execDetail.setTraceId(traceId);
        execDetail.setExecuteMicroseconds(execTime);
        return execDetail;
    }

    private static String findLogTraceId(Object tags) {
        if (tags instanceof Map) {
            if (((Map<?, ?>) tags).containsKey(TRACE_ID_KEY)) {
                return ((Map<?, ?>) tags).get(TRACE_ID_KEY).toString();
            }
            return null;
        } else if (tags instanceof Collection) {
            for (Object obj : (Collection<?>) tags) {
                String value = findLogTraceId(obj);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

}
