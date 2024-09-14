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
package com.oceanbase.odc.service.onlineschemachange.oms.client;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ApiReturnResult;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;

/**
 * @author longpeng.zlp
 * @date 2024/9/11 14:49
 */
public class OmsClientTest {
    private final String responseBody = "{\n"
            + "\t\"code\": \"GHANA-OPERIE000000\",\n"
            + "\t\"errorDetail\": {\n"
            + "\t\t\"code\": \"GHANA-OPERIE000000\",\n"
            + "\t\t\"message\": \"传输实例正在创建中，通常需要 5~10 分钟，请稍后重试。\",\n"
            + "\t\t\"messageMcmsContext\": {\n"
            + "\t\t\t\"message\": \"传输实例正在创建中，通常需要 5~10 分钟，请稍后重试。\"\n"
            + "\t\t},\n"
            + "\t\t\"messageMcmsKey\": \"GHANA-OPERIE000000\",\n"
            + "\t\t\"requestId\": \"xxxx\"\n"
            + "\t},\n"
            + "\t\"message\": \"[GHANA-OPERIE000000]: 服务内部错误。\",\n"
            + "\t\"requestId\": \"xxxx\",\n"
            + "\t\"success\": false\n"
            + "}";
    private DefaultPrivateCloudOmsClient client;
    private ClientRequestParams requestParams;
    private ResponseEntity<String> responseEntity;

    @Before
    public void init() {
        OnlineSchemaChangeProperties properties = new OnlineSchemaChangeProperties();
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1");
        properties.setOms(omsProperties);
        client = new DefaultPrivateCloudOmsClient(
                properties,
                Mockito.mock(RestTemplate.class));
        requestParams = new ClientRequestParams();
        requestParams.setAction("CreateDataSource");
        responseEntity = (ResponseEntity<String>) Mockito.mock(ResponseEntity.class);
    }

    @Test(expected = OmsException.class)
    public void testResolveErrorDetailFromOmsResponse() {
        Mockito.when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(responseEntity.getBody()).thenReturn(responseBody);
        ApiReturnResult<String> createDataSourceBadResponse = client.resolveResponseEntity(requestParams,
                responseEntity, new TypeReference<OmsApiReturnResult<String>>() {});
    }
}
