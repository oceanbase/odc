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

package com.oceanbase.odc.service.diagnose.fulllinktrace;

import org.junit.Test;

import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.shared.model.TraceSpan.Node;

public class JaegerAdaptorTest {

    @Test
    public void testAdapt_Success() {
        TraceSpan root = getTraceSpan();
        String trace = new JaegerConverter().convert(root);
        System.out.println(trace);
    }

    private TraceSpan getTraceSpan() {
        TraceSpan root = new TraceSpan();
        root.setLogTraceId("Y2F170BA2D939-0005FB02C184A359-0-0");
        root.setSpanId("1");
        root.setTraceId("1");
        root.setSpanName("oceanbase_jdbc");
        root.setHost("0.0.0.0");
        root.setPort(9999);
        root.setStartTimestamp("2023-07-11 14:09:49.650929");
        root.setNode(Node.JDBC);
        root.setTenantId(1);

        TraceSpan child1 = new TraceSpan();
        child1.setSpanId("2");
        child1.setTraceId("2");
        child1.setSpanName("com_query_process");
        child1.setHost("1.1.1.1");
        child1.setPort(9999);
        child1.setStartTimestamp("2023-07-11 14:09:49.750929");
        child1.setNode(Node.OBServer);
        child1.setTenantId(1);

        TraceSpan child2 = new TraceSpan();
        child2.setSpanId("3");
        child2.setTraceId("3");
        child2.setSpanName("sql_execute");
        child2.setHost("1.1.1.1");
        child2.setPort(9999);
        child2.setStartTimestamp("2023-07-11 14:09:50.213724");
        child2.setNode(Node.OBServer);
        child2.setTenantId(1);

        root.getSubSpans().add(child1);
        root.getSubSpans().add(child2);
        return root;
    }

}
