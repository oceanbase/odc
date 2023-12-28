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

import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.integration.model.OdcIntegrationResponse;

/**
 * @author gaoda.xy
 * @date 2023/12/6 17:25
 */
public class HttpOperationServiceTest extends ServiceTestEnv {

    @Autowired
    private HttpOperationService httpOperationService;

    @Test
    public void test_extractHttpResponse_json_returnString() {
        String actual =
                httpOperationService.extractHttpResponse(getJsonResponse(), "company.employee[0].name", String.class);
        Assert.assertEquals("John Doe", actual);
    }

    @Test
    public void test_extractHttpResponse_json_returnBoolean_TRUE() {
        Boolean actual =
                httpOperationService.extractHttpResponse(getJsonResponse(),
                        "company.employee[0].skills.skill[1]==\"Python\"", Boolean.class);
        Assert.assertEquals(Boolean.TRUE, actual);
    }

    @Test
    public void test_extractHttpResponse_json_returnBoolean_FALSE() {
        Boolean actual =
                httpOperationService.extractHttpResponse(getJsonResponse(),
                        "company.employee[0].skills.skill[1]==\"Python\"", Boolean.class);
        Assert.assertEquals(Boolean.TRUE, actual);
    }

    @Test
    public void test_extractHttpResponse_xml_returnString() {
        String actual =
                httpOperationService.extractHttpResponse(getXmlResponse(), "company.employee[0].name", String.class);
        Assert.assertEquals("John Doe", actual);
    }

    @Test
    public void test_extractHttpResponse_xml_returnBoolean_TRUE() {
        Boolean actual =
                httpOperationService.extractHttpResponse(getXmlResponse(),
                        "company.employee[0].skills.skill[1]==\"Python\"", Boolean.class);
        Assert.assertEquals(Boolean.TRUE, actual);
    }

    @Test
    public void test_extractHttpResponse_xml_returnBoolean_FALSE() {
        Boolean actual =
                httpOperationService.extractHttpResponse(getXmlResponse(),
                        "company.employee[0].skills.skill[1]==\"Python\"", Boolean.class);
        Assert.assertEquals(Boolean.TRUE, actual);
    }

    private OdcIntegrationResponse getJsonResponse() {
        return OdcIntegrationResponse.builder()
                .content(getResponse())
                .contentType(ContentType.APPLICATION_JSON)
                .build();
    }

    private OdcIntegrationResponse getXmlResponse() {
        return OdcIntegrationResponse.builder()
                .content(JsonUtils.jsonToXml(getResponse()))
                .contentType(ContentType.APPLICATION_XML)
                .build();
    }

    private String getResponse() {
        return "{\n"
                + "    \"company\": {\n"
                + "        \"employee\": [\n"
                + "            {\n"
                + "                \"id\": \"1\",\n"
                + "                \"name\": \"John Doe\",\n"
                + "                \"department\": \"Engineering\",\n"
                + "                \"skills\": {\n"
                + "                    \"skill\": [\n"
                + "                        \"Java\",\n"
                + "                        \"Python\",\n"
                + "                        \"SQL\"\n"
                + "                    ]\n"
                + "                }\n"
                + "            },\n"
                + "            {\n"
                + "                \"id\": \"2\",\n"
                + "                \"name\": \"Jane Smith\",\n"
                + "                \"department\": \"Marketing\",\n"
                + "                \"skills\": {\n"
                + "                    \"skill\": [\n"
                + "                        \"Marketing Strategy\",\n"
                + "                        \"Communication\",\n"
                + "                        \"Analytics\"\n"
                + "                    ]\n"
                + "                }\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}";
    }

}
