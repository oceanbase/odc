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

    /**
     * 获取完整链路跟踪详情
     *
     * @param statement    SQL语句对象
     * @param queryTimeout 查询超时时间
     * @return SqlExecTime 执行时间对象
     * @throws SQLException SQL异常
     */
    public static SqlExecTime getFullLinkTraceDetail(Statement statement, int queryTimeout) throws SQLException {
        // 获取OceanBaseConnection对象
        OceanBaseConnection connection = (OceanBaseConnection) statement.getConnection();
        // 获取最后一个数据包的接收时间戳
        long lastPacketResponseTimestamp =
            TimeUnit.MICROSECONDS.convert(connection.getLastPacketResponseTimestamp(),
                TimeUnit.MILLISECONDS);
        // 获取最后一个数据包的发送时间戳
        long lastPacketSendTimestamp = TimeUnit.MICROSECONDS.convert(connection.getLastPacketSendTimestamp(),
            TimeUnit.MILLISECONDS);
        // 保存原始查询超时时间
        int originQueryTimeout = statement.getQueryTimeout();
        // 设置查询超时时间
        statement.setQueryTimeout(queryTimeout);
        try (ResultSet resultSet = statement.executeQuery("show trace format='json'")) {
            // 判断是否有跟踪信息
            if (!resultSet.next()) {
                throw new UnexpectedException("No trace info, maybe value of ob_enable_show_trace is 0.");
            }
            // 获取跟踪信息的JSON字符串
            String showTraceJson = resultSet.getString(1);
            // 解析跟踪信息的JSON字符串，获取SqlExecTime对象
            SqlExecTime execDetail = parseSpanList(JsonUtils.fromJsonList(showTraceJson, TraceSpan.class));

            // 设置最后一个数据包的接收时间戳
            execDetail.setLastPacketSendTimestamp(lastPacketSendTimestamp);
            // 设置最后一个数据包的发送时间戳
            execDetail.setLastPacketResponseTimestamp(lastPacketResponseTimestamp);
            return execDetail;
        } finally {
            // 恢复原始查询超时时间
            statement.setQueryTimeout(originQueryTimeout);
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

    private static String findLogTraceId(List<Map<String, Object>> tags) {
        if (tags == null) {
            return null;
        }
        for (Map<String, Object> tag : tags) {
            if (tag.containsKey(TRACE_ID_KEY)) {
                return (String) tag.get(TRACE_ID_KEY);
            }
        }
        return null;
    }

}
