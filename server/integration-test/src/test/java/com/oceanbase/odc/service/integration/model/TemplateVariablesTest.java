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
package com.oceanbase.odc.service.integration.model;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.integration.IntegrationTestUtil;

/**
 * @author gaoda.xy
 * @date 2023/4/11 16:43
 */
public class TemplateVariablesTest {

    @Test
    public void test_process() {
        String origin = "{\n"
                + "    \"params\": \"user=${user.name}, account=${user.account}, id=${user.id}\",\n"
                + "    \"body\": {\n"
                + "        \"url\": \"${odc.site.url}\",\n"
                + "        \"instance\": \"${process.instance.id}\",\n"
                + "        \"task\": {\n"
                + "            \"type\": \"${task.type}\",\n"
                + "            \"details\": \"${task.details}\"\n"
                + "        },\n"
                + "        \"connection\": {\n"
                + "            \"name\": \"${connection.name}\",\n"
                + "            \"tenant\": \"${connection.tenant}\",\n"
                + "            \"properties\": \"${connection.properties}\"\n"
                + "        },\n"
                + "        \"sqls\": \"${sql.content}\",\n"
                + "        \"statements\": ${sql.content.json.array}\n"
                + "    }\n"
                + "}";
        String excepted = "{\n"
                + "    \"params\": \"user=Jack, account=Jack, id=10001\",\n"
                + "    \"body\": {\n"
                + "        \"url\": \"http://localhost:18989\",\n"
                + "        \"instance\": \"a1b2c3d4e5\",\n"
                + "        \"task\": {\n"
                + "            \"type\": \"ASYNC\",\n"
                + "            \"details\": \"\"\n"
                + "        },\n"
                + "        \"connection\": {\n"
                + "            \"name\": \"public_connection_001\",\n"
                + "            \"tenant\": \"tenant\",\n"
                + "            \"properties\": \"{\"key\":\"value\"}\"\n"
                + "        },\n"
                + "        \"sqls\": \"select 1 from dual\",\n"
                + "        \"statements\": [\"select 1 from dual\",\"select 2 from dual\"]\n"
                + "    }\n"
                + "}";
        TemplateVariables templateVariables = IntegrationTestUtil.createTemplateVariables();
        String processed = templateVariables.process(origin);
        Assert.assertEquals(excepted, processed);
    }

}
