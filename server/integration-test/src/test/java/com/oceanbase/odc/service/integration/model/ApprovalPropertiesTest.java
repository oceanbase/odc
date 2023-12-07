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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.integration.IntegrationTestUtil;
import com.oceanbase.odc.service.integration.model.ApprovalProperties.AdvancedProperties;
import com.oceanbase.odc.service.integration.model.ApprovalProperties.Api;
import com.oceanbase.odc.service.integration.model.ApprovalProperties.StartProperties;
import com.oceanbase.odc.service.integration.model.ApprovalProperties.StatusProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.ApiProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.Body;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.BodyType;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.HttpProperties;
import com.oceanbase.odc.service.integration.model.IntegrationProperties.RequestMethod;

/**
 * @author gaoda.xy
 * @date 2023/3/27 16:16
 */
public class ApprovalPropertiesTest {

    @Test
    public void test_getApprovalProperties() {
        ApprovalProperties properties = ApprovalProperties.from(IntegrationTestUtil.createApprovalConfig());
        Assert.assertEquals(getExceptApprovalProperties(), properties);
    }

    private ApprovalProperties getExceptApprovalProperties() {
        StartProperties start = new StartProperties();
        start.setMethod(RequestMethod.POST);
        start.setUrl("http://localhost:18989/start");
        Body body = new Body();
        body.setType(BodyType.FORM_DATA);
        Map<String, String> content = new LinkedHashMap<>();
        content.put("processCode", "approval_integration_test");
        body.setContent(content);
        start.setBody(body);
        start.setRequestSuccessExpression("queryResult.success==true");
        start.setExtractInstanceIdExpression("queryResult.content.processInstanceId");
        StatusProperties status = new StatusProperties();
        status.setMethod(RequestMethod.POST);
        status.setUrl("http://localhost:18989/status");
        body = new Body();
        body.setType(BodyType.FORM_DATA);
        content = new LinkedHashMap<>();
        content.put("processInstanceId", "${process.instance.id}");
        body.setContent(content);
        status.setBody(body);
        status.setRequestSuccessExpression("success == true");
        status.setProcessPendingExpression("content.processInstanceStatus==\"RUNNING\"");
        status.setProcessApprovedExpression(
                "content.processInstanceStatus==\"COMPLETED\" && content.outResult==\"同意\"");
        status.setProcessRejectedExpression(
                "content.processInstanceStatus==\"COMPLETED\" && content.outResult==\"拒绝\"");
        status.setProcessTerminatedExpression("content.processInstanceStatus==\"TERMINATED\"");
        ApiProperties cancel = new ApiProperties();
        cancel.setMethod(RequestMethod.POST);
        cancel.setUrl("http://localhost:18989/cancel");
        body = new Body();
        body.setType(BodyType.FORM_DATA);
        content = new LinkedHashMap<>();
        content.put("processInstanceId", "${process.instance.id}");
        body.setContent(content);
        cancel.setBody(body);
        cancel.setRequestSuccessExpression("success == true");
        Api api = new Api();
        api.setStart(start);
        api.setStatus(status);
        api.setCancel(cancel);
        HttpProperties http = new HttpProperties();
        http.setConnectTimeoutSeconds(5);
        http.setSocketTimeoutSeconds(30);
        AdvancedProperties advanced = new AdvancedProperties();
        advanced.setHyperlinkExpression("http://localhost:8989/procInsId=${process.instance.id}");
        ApprovalProperties properties = new ApprovalProperties();
        properties.setApi(api);
        properties.setApprovalTimeoutSeconds(86400);
        properties.setEncryption(Encryption.empty());
        properties.setHttp(http);
        properties.setAdvanced(advanced);
        return properties;
    }

}
