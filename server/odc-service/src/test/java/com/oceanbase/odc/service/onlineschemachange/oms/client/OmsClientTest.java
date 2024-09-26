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

import java.util.List;

import org.junit.Assert;
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
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectResponse;

/**
 * @author longpeng.zlp
 * @date 2024/9/11 14:49
 */
public class OmsClientTest {
    private final String badResponseBody = "{\n"
            + "  \"code\": \"GHANA-OPERIE000000\",\n"
            + "  \"errorDetail\": {\n"
            + "    \"code\": \"GHANA-OPERIE000000\",\n"
            + "    \"message\": \"传输实例正在创建中，通常需要 5~10 分钟，请稍后重试。\",\n"
            + "    \"messageMcmsContext\": {\n"
            + "      \"message\": \"传输实例正在创建中，通常需要 5~10 分钟，请稍后重试。\"\n"
            + "    },\n"
            + "    \"messageMcmsKey\": \"GHANA-OPERIE000000\",\n"
            + "    \"requestId\": \"xxxx\"\n"
            + "  },\n"
            + "  \"message\": \"[GHANA-OPERIE000000]: 服务内部错误。\",\n"
            + "  \"requestId\": \"xxxx\",\n"
            + "  \"success\": false\n"
            + "}";
    private final String listProjectResult = "{\n"
            + "    \"success\": true,\n"
            + "    \"errorDetail\": null,\n"
            + "    \"code\": null,\n"
            + "    \"message\": null,\n"
            + "    \"advice\": null,\n"
            + "    \"requestId\": \"****\",\n"
            + "    \"pageNumber\": 1,\n"
            + "    \"pageSize\": 10,\n"
            + "    \"totalCount\": 1,\n"
            + "    \"cost\": \"13 ms\",\n"
            + "    \"data\": [\n"
            + "        {\n"
            + "            \"workerGradeId\": \"myworkerID\",\n"
            + "            \"id\": \"myID\",\n"
            + "            \"type\": \"MIGRATION\",\n"
            + "            \"name\": \"****\",\n"
            + "            \"labels\": null,\n"
            + "            \"owner\": \"xxxx\",\n"
            + "            \"importance\": \"LOW\",\n"
            + "            \"status\": \"RUNNING\",\n"
            + "            \"gmtCreate\": \"2024-09-13T09:08:34\",\n"
            + "            \"gmtModified\": \"2024-09-18T07:01:52\",\n"
            + "            \"gmtStart\": \"2024-09-13T09:08:35\",\n"
            + "            \"gmtFinish\": null,\n"
            + "            \"destConnId\": null,\n"
            + "            \"isMerging\": false,\n"
            + "            \"isModifying\": false,\n"
            + "            \"isSubProject\": false,\n"
            + "            \"sourceEndpointType\": \"MYSQL\",\n"
            + "            \"sinkEndpointType\": \"OB_MYSQL\",\n"
            + "            \"transferMapping\": null,\n"
            + "            \"commonTransferConfig\": null,\n"
            + "            \"enableStructTransfer\": false,\n"
            + "            \"structTransferConfig\": null,\n"
            + "            \"enableFullTransfer\": false,\n"
            + "            \"enableFullVerify\": false,\n"
            + "            \"fullTransferConfig\": null,\n"
            + "            \"enableIncrTransfer\": true,\n"
            + "            \"enableIncrVerify\": false,\n"
            + "            \"enableReverseIncrTransfer\": false,\n"
            + "            \"incrTransferConfig\": null,\n"
            + "            \"sourceConnectInfo\": {\n"
            + "                \"id\": \"*****\",\n"
            + "                \"endpointName\": \"****\",\n"
            + "                \"endpointId\": \"*****\",\n"
            + "                \"endpointSide\": null,\n"
            + "                \"dbEngine\": \"MYSQL_PUBLIC\",\n"
            + "                \"connectionInfo\": null,\n"
            + "                \"username\": \"root\",\n"
            + "                \"version\": \"5.7.27-log\",\n"
            + "                \"timezone\": \"UTC\",\n"
            + "                \"charset\": \"utf8mb4\",\n"
            + "                \"nlsLengthSemantics\": null,\n"
            + "                \"operatingSystem\": \"Linux\",\n"
            + "                \"region\": \"cn-shanghai\",\n"
            + "                \"ocpName\": \"\",\n"
            + "                \"connExtraAttributes\": null,\n"
            + "                \"owner\": \"admin\",\n"
            + "                \"resourceOwner\": \"admin\",\n"
            + "                \"host\": \"****\",\n"
            + "                \"port\": 3306\n"
            + "            },\n"
            + "            \"sinkConnectInfo\": {\n"
            + "                \"id\": \"****\",\n"
            + "                \"endpointName\": \"****\",\n"
            + "                \"endpointId\": \"*****\",\n"
            + "                \"endpointSide\": null,\n"
            + "                \"dbEngine\": \"OB_MYSQL_PUBLIC\",\n"
            + "                \"connectionInfo\": null,\n"
            + "                \"username\": \"*****\",\n"
            + "                \"version\": \"3.2.4.8\",\n"
            + "                \"timezone\": \"+08:00\",\n"
            + "                \"charset\": \"utf8mb4\",\n"
            + "                \"nlsLengthSemantics\": null,\n"
            + "                \"operatingSystem\": null,\n"
            + "                \"region\": \"*****\",\n"
            + "                \"ocpName\": \"\",\n"
            + "                \"connExtraAttributes\": {\n"
            + "                    \"cluster\": \"*****\",\n"
            + "                    \"tenant\": \"oms_mysql\",\n"
            + "                    \"isLogicSource\": false,\n"
            + "                    \"useLogProxy\": false,\n"
            + "                    \"drcUser\": \"root\",\n"
            + "                    \"configUrl\": \"******\",\n"
            + "                    \"logProxyIp\": null,\n"
            + "                    \"logProxyPort\": null,\n"
            + "                    \"noUserAuth\": false\n"
            + "                },\n"
            + "                \"owner\": \"****\",\n"
            + "                \"resourceOwner\": \"****\",\n"
            + "                \"host\": \"****\",\n"
            + "                \"port\": 56569\n"
            + "            },\n"
            + "            \"steps\": [\n"
            + "                {\n"
            + "                    \"order\": 1,\n"
            + "                    \"name\": \"TRANSFER_PRECHECK\",\n"
            + "                    \"description\": \"预检查\",\n"
            + "                    \"status\": \"FINISHED\",\n"
            + "                    \"extraInfo\": {\n"
            + "                        \"errorDetails\": null,\n"
            + "                        \"errorCode\": null,\n"
            + "                        \"errorMsg\": null,\n"
            + "                        \"errorParam\": null,\n"
            + "                        \"failedTime\": null\n"
            + "                    },\n"
            + "                    \"startTime\": \"2024-09-13T09:08:38\",\n"
            + "                    \"finishTime\": \"2024-09-13T09:08:38\",\n"
            + "                    \"progress\": 100,\n"
            + "                    \"stepInfo\": null\n"
            + "                },\n"
            + "                {\n"
            + "                    \"order\": 3,\n"
            + "                    \"name\": \"INCR_TRANSFER\",\n"
            + "                    \"description\": \"增量同步\",\n"
            + "                    \"status\": \"MONITORING\",\n"
            + "                    \"extraInfo\": {\n"
            + "                        \"errorDetails\": null,\n"
            + "                        \"errorCode\": null,\n"
            + "                        \"errorMsg\": null,\n"
            + "                        \"errorParam\": null,\n"
            + "                        \"failedTime\": null\n"
            + "                    },\n"
            + "                    \"startTime\": \"2024-09-13T09:09:17\",\n"
            + "                    \"finishTime\": null,\n"
            + "                    \"progress\": 100,\n"
            + "                    \"stepInfo\": {\n"
            + "                        \"incrTimestampCheckpoint\": 1726642901,\n"
            + "                        \"checkpointSampleTimestamp\": 1726642905,\n"
            + "                        \"enableIncrStatistics\": null\n"
            + "                    }\n"
            + "                },\n"
            + "                {\n"
            + "                    \"order\": 4,\n"
            + "                    \"name\": \"TRANSFER_APP_SWITCH\",\n"
            + "                    \"description\": \"正向切换\",\n"
            + "                    \"status\": \"RUNNING\",\n"
            + "                    \"extraInfo\": {\n"
            + "                        \"errorDetails\": null,\n"
            + "                        \"errorCode\": null,\n"
            + "                        \"errorMsg\": null,\n"
            + "                        \"errorParam\": null,\n"
            + "                        \"failedTime\": null\n"
            + "                    },\n"
            + "                    \"startTime\": \"2024-09-13T09:09:23\",\n"
            + "                    \"finishTime\": null,\n"
            + "                    \"progress\": 0,\n"
            + "                    \"stepInfo\": {\n"
            + "                        \"checkpointSampleTimestamp\": null\n"
            + "                    }\n"
            + "                }\n"
            + "            ],\n"
            + "            \"extraInfo\": null,\n"
            + "            \"alarmStats\": {\n"
            + "                \"target\": null,\n"
            + "                \"alarming\": false,\n"
            + "                \"recentlyTriggerCount\": null,\n"
            + "                \"ruleToRecentlyTriggerCount\": null,\n"
            + "                \"alarmContent\": null,\n"
            + "                \"openMonitor\": null\n"
            + "            }\n"
            + "        }\n"
            + "    ]\n"
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
        Mockito.when(responseEntity.getBody()).thenReturn(badResponseBody);
        ApiReturnResult<String> createDataSourceBadResponse = client.resolveResponseEntity(requestParams,
                responseEntity, new TypeReference<OmsApiReturnResult<String>>() {});
    }

    @Test
    public void testListOmsProject() {
        Mockito.when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(responseEntity.getBody()).thenReturn(listProjectResult);
        ApiReturnResult<List<OmsProjectResponse>> listObjectResponse = client.resolveResponseEntity(requestParams,
                responseEntity, new TypeReference<OmsApiReturnResult<List<OmsProjectResponse>>>() {});
        Assert.assertEquals(listObjectResponse.getData().size(), 1);
        OmsProjectResponse omsProjectResponse = listObjectResponse.getData().get(0);
        Assert.assertEquals(omsProjectResponse.getId(), "myID");
        Assert.assertEquals(omsProjectResponse.getWorkerGradeId(), "myworkerID");
    }
}
