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

import static com.github.dreamhead.moco.Moco.file;
import static com.github.dreamhead.moco.MocoJsonRunner.jsonHttpServer;
import static com.github.dreamhead.moco.Runner.running;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.dreamhead.moco.HttpServer;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.integration.IntegrationTestUtil;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.ApprovalStatus;
import com.oceanbase.odc.service.integration.model.TemplateVariables;

/**
 * @author gaoda.xy
 * @date 2023/7/17 10:25
 */
public class ApprovalClientTest extends ServiceTestEnv {

    @Autowired
    private ApprovalClient client;

    private static final TemplateVariables variable;
    private static final String configFile;
    private static final ApprovalProperties properties;

    static {
        variable = IntegrationTestUtil.createTemplateVariables();
        configFile = "src/test/resources/integration/mocked_approval_server_config.json";
        properties = ApprovalProperties.from(IntegrationTestUtil.createApprovalConfig());
    }

    @Test
    public void test_check_start() throws Exception {
        final HttpServer server = jsonHttpServer(file(configFile));
        running(server, () -> {
            properties.getApi().getStart().setUrl("http://localhost:" + server.port() + "/start");
            String processInstanceId = client.start(properties, variable);
            Assert.assertEquals("test_process_instance_id", processInstanceId);
        });
    }

    @Test
    public void test_check_status() throws Exception {
        final HttpServer server = jsonHttpServer(file(configFile));
        running(server, () -> {
            properties.getApi().getStatus().setUrl("http://localhost:" + server.port() + "/status");
            ApprovalStatus processInstanceStatus = client.status(properties, variable);
            Assert.assertEquals(ApprovalStatus.APPROVED, processInstanceStatus);
        });
    }

    @Test
    public void test_check_cancel() throws Exception {
        final HttpServer server = jsonHttpServer(file(configFile));
        running(server, () -> {
            properties.getApi().getCancel().setUrl("http://localhost:" + server.port() + "/cancel");
            client.cancel(properties, variable);
        });
    }

}
