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
package com.oceanbase.odc.service.integration;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;

/**
 * @author gaoda.xy
 * @date 2023/7/14 14:28
 */
public class IntegrationTestUtil {

    public static IntegrationConfig createSqlInterceptorConfig() {
        String configStr;
        String testFilepath = "src/test/resources/integration/sql_interceptor_integration_template.yaml";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(testFilepath)))) {
            configStr = IOUtils.toString(in, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        IntegrationConfig returnValue = new IntegrationConfig();
        returnValue.setConfiguration(configStr);
        returnValue.setEncryption(Encryption.empty());
        return returnValue;
    }

    public static IntegrationConfig createApprovalConfig() {
        String configStr;
        String testFilepath = "src/test/resources/integration/approval_integration_template.yaml";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(testFilepath)))) {
            configStr = IOUtils.toString(in, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        IntegrationConfig returnValue = new IntegrationConfig();
        returnValue.setConfiguration(configStr);
        returnValue.setEncryption(Encryption.empty());
        return returnValue;
    }

    public static TemplateVariables createTemplateVariables() {
        Map<String, Serializable> variables = new HashMap<>();
        variables.put(Variable.USER_NAME.key(), "Jack");
        variables.put(Variable.USER_ACCOUNT.key(), "Jack");
        variables.put(Variable.USER_ID.key(), 10001L);
        variables.put(Variable.SQL_CONTENT.key(), "select 1 from dual");
        List<String> statements = new ArrayList<>();
        statements.add("select 1 from dual");
        statements.add("select 2 from dual");
        variables.put(Variable.SQL_CONTENT_JSON_ARRAY.key(), JsonUtils.toJson(statements));
        variables.put(Variable.PROCESS_INSTANCE_ID.key(), "a1b2c3d4e5");
        variables.put(Variable.TASK_TYPE.key(), TaskType.ASYNC);
        variables.put(Variable.TASK_DETAILS.key(), "");
        variables.put(Variable.CONNECTION_NAME.key(), "public_connection_001");
        variables.put(Variable.CONNECTION_TENANT.key(), "tenant");
        TemplateVariables template = new TemplateVariables(variables);
        Map<String, String> properties = new HashMap<>();
        properties.put("key", "value");
        template.setAttribute(Variable.CONNECTION_PROPERTIES, JsonUtils.toJson(properties));
        template.setAttribute(Variable.ODC_SITE_URL, "http://localhost:18989");
        return template;
    }

}
