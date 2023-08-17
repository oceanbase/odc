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

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.DefaultConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.core.sql.execute.ConnectionExtensionExecutor;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.service.common.model.ResourceSql;

public class DiagnoseServiceTest {

    @Test
    public void test_GetFullLinkTrace_FromCache() throws Exception {
        ConnectionSession session = getConnectionSession();
        // set span
        TraceSpan span = new TraceSpan();
        span.setLogTraceId("Y2F170BA2D939-0005FB02C184A359-0-0");
        span.setSpanName("test span");
        BinaryContentMetaData metaData = ConnectionSessionUtil.getBinaryDataManager(session)
                .write(new ByteArrayInputStream(JsonUtils.toJson(span).getBytes()));
        ConnectionSessionUtil.setBinaryContentMetadata(session, span.getLogTraceId(), metaData);
        // get span
        ResourceSql resourceSql = new ResourceSql();
        resourceSql.setTag("Y2F170BA2D939-0005FB02C184A359-0-0");
        TraceSpan result = new SqlDiagnoseService().getFullLinkTrace(session, resourceSql);
        Assert.assertEquals("test span", result.getSpanName());
    }

    private ConnectionSession getConnectionSession() throws Exception {
        return new DefaultConnectionSession(() -> "", null, 10000, ConnectType.OB_MYSQL, true,
                Mockito.mock(ConnectionExtensionExecutor.class));
    }

}
