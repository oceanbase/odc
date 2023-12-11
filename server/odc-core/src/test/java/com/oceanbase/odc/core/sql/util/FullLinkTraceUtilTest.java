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

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.StreamUtils;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;

public class FullLinkTraceUtilTest {
    @Test
    public void test_GetFullLinkTrace() throws Exception {
        SqlExecTime execDetail = FullLinkTraceUtil.getFullLinkTraceDetail(mockStmt(), 60);
        Assert.assertNotNull(execDetail.getTraceSpan());
    }

    @Test
    public void test_DeserializeTimestamp() {
        String expected = "2023-07-11 14:09:49.650929";
        String input1 = "{\"start_ts\":\"11-JUL-23 02.09.49.650929 PM\"}";
        String input2 = "{\"start_ts\":\"2023-07-11 14:09:49.650929\"}";

        TraceSpan span1 = JsonUtils.fromJson(input1, TraceSpan.class);
        TraceSpan span2 = JsonUtils.fromJson(input2, TraceSpan.class);

        Assert.assertEquals(expected, span1.getStartTimestamp());
        Assert.assertEquals(expected, span2.getStartTimestamp());
    }

    @Test
    public void test_DeserializeTimestamp_preserve_accuracy() {
        String expected = "2023-07-11 14:09:49.650000";
        String input = "{\"start_ts\":\"2023-07-11 14:09:49.650000\"}";

        TraceSpan span = JsonUtils.fromJson(input, TraceSpan.class);

        Assert.assertEquals(expected, span.getStartTimestamp());
    }

    private Statement mockStmt() throws Exception {
        URL resource = this.getClass().getClassLoader().getResource("trace.json");
        String jsonStr;
        try (InputStream stream = resource.openStream()) {
            jsonStr = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        }

        Statement stmt = Mockito.mock(Statement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.when(stmt.executeQuery(Mockito.anyString())).thenReturn(rs);
        Mockito.when(rs.next()).thenReturn(true);
        Mockito.when(rs.getString(Mockito.anyInt())).thenReturn(jsonStr);

        OceanBaseConnection conn = Mockito.mock(OceanBaseConnection.class);
        Mockito.when(conn.getLastPacketSendTimestamp()).thenReturn(1L);
        Mockito.when(conn.getLastPacketResponseTimestamp()).thenReturn(1L);
        Mockito.when(stmt.getConnection()).thenReturn(conn);
        return stmt;
    }

}
