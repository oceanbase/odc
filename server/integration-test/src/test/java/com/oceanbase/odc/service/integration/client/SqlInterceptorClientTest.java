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
package com.oceanbase.odc.service.integration.client;

import static com.github.dreamhead.moco.Moco.*;
import static com.github.dreamhead.moco.MocoJsonRunner.jsonHttpServer;
import static com.github.dreamhead.moco.Runner.*;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.dreamhead.moco.HttpServer;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.integration.IntegrationTestUtil;
import com.oceanbase.odc.service.integration.model.SqlCheckStatus;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;
import com.oceanbase.odc.service.integration.model.TemplateVariables;

/**
 * @author gaoda.xy
 * @date 2023/7/14 11:27
 */
public class SqlInterceptorClientTest extends ServiceTestEnv {

    @Autowired
    private SqlInterceptorClient client;

    private static final TemplateVariables variable;
    private static final String configFile;
    private static final SqlInterceptorProperties properties;

    static {
        variable = IntegrationTestUtil.createTemplateVariables();
        configFile = "src/test/resources/integration/mocked_sql_interceptor_server_config.json";
        properties = SqlInterceptorProperties.from(IntegrationTestUtil.createSqlInterceptorConfig());
    }

    @Test
    public void test_check_passed() throws Exception {
        final HttpServer server = jsonHttpServer(file(configFile));
        running(server, () -> {
            properties.getApi().getCheck().setUrl("http://localhost:" + server.port() + "/check_passed");
            SqlCheckStatus status = client.check(properties, variable);
            Assert.assertEquals(SqlCheckStatus.IN_WHITE_LIST, status);
        });
    }

    @Test
    public void test_check_need_review() throws Exception {
        final HttpServer server = jsonHttpServer(file(configFile));
        running(server, () -> {
            properties.getApi().getCheck().setUrl("http://localhost:" + server.port() + "/check_need_review");
            SqlCheckStatus status = client.check(properties, variable);
            Assert.assertEquals(SqlCheckStatus.NEED_REVIEW, status);
        });
    }

    @Test
    public void test_check_blocked() throws Exception {
        final HttpServer server = jsonHttpServer(file(configFile));
        running(server, () -> {
            properties.getApi().getCheck().setUrl("http://localhost:" + server.port() + "/check_blocked");
            SqlCheckStatus status = client.check(properties, variable);
            Assert.assertEquals(SqlCheckStatus.IN_BLACK_LIST, status);
        });
    }

}
