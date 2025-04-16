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
package com.oceanbase.odc.service.onlineschemachange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CommonTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.DatabaseTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.request.FullTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.IncrTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsApiReturnResult;
import com.oceanbase.odc.service.onlineschemachange.oms.request.SpecificTransferMapping;
import com.oceanbase.odc.service.onlineschemachange.oms.request.TableTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.response.FullTransferStepInfoVO;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-27
 * @since 4.2.0
 */
@Slf4j
public class JsonTest {

    @Test
    public void test_json_error_detail() {
        String json = "{\n"
                + "    \"success\":false,\n"
                + "    \"errorDetail\":{\n"
                + "        \"code\":\"GHANA-OPERAT000001\",\n"
                + "        \"message\":\"Pay attention that the CM service may be not available.\",\n"
                + "        \"messageMcmsContext\":{\n"
                + "            \"service\":\"CM\"\n"
                + "        }\n"
                + "    },\n"
                + "    \"code\":\"GHANA-OPERAT000001\"\n"
                + "}";

        OmsApiReturnResult<List<OmsProjectStepVO>> apiReturnResults = JsonUtils.fromJsonIgnoreMissingProperty(json,
                new TypeReference<OmsApiReturnResult<List<OmsProjectStepVO>>>() {});
        Assert.assertNotNull(apiReturnResults);
        Assert.assertNotNull(apiReturnResults.getErrorDetail());
        Assert.assertTrue(apiReturnResults.getErrorDetail().containsKey("code"));
    }

    @Test
    public void test_json_sub_type_missing_property() {

        String json = "{\"Code\":200,\"Cost\":56,\"Data\":[{\"name\":\"TRANSFER_INCR_LOG_PULL\",\"progress\":0,"
                + "\"order\":3,\"status\":\"INIT\",\"extraInfo\":{}},{\"name\":\"FULL_TRANSFER\","
                + "\"progress\":0,\"stepInfo\":{\"processedRecords\":100},\"order\":4,\"status\":\"INIT\","
                + "\"extraInfo\":{}}],\"Deletable\":false,\"ErrorCode\":\"\",\"Message\":\"successful\","
                + "\"Success\":true,\"TotalCount\":0}";

        OmsApiReturnResult<List<OmsProjectStepVO>> apiReturnResults = JsonUtils.fromJsonIgnoreMissingProperty(json,
                new TypeReference<OmsApiReturnResult<List<OmsProjectStepVO>>>() {});
        Assert.assertNotNull(apiReturnResults);
        Assert.assertTrue(CollectionUtils.isNotEmpty(apiReturnResults.getData()));
        Optional<OmsProjectStepVO> first = apiReturnResults.getData().stream().filter(
                a -> a.getName() == OscStepName.FULL_TRANSFER).findFirst();
        Assert.assertTrue(first.isPresent());
        FullTransferStepInfoVO fullTransferStepInfoVO = (FullTransferStepInfoVO) first.get().getStepInfo();
        Assert.assertSame(fullTransferStepInfoVO.getProcessedRecords(), 100L);

    }

    @Test
    public void test_object_to_map_with_upper_camel_case() {
        CreateOmsProjectRequest createProjectRequest = getCreateProjectRequest();
        Map map = JsonUtils.fromJson(JsonUtils.toJsonUpperCamelCase(createProjectRequest), Map.class);
        Assert.assertTrue(map.containsKey("Name"));
        Assert.assertTrue(map.containsKey("TransferMapping"));

    }

    private CreateOmsProjectRequest getCreateProjectRequest() {
        String datasourceId = "ds1";
        String dbName = "db1";
        CreateOmsProjectRequest request = new CreateOmsProjectRequest();
        request.setSourceEndpointId(datasourceId);
        request.setSinkEndpointId(datasourceId);

        List<DatabaseTransferObject> databaseTransferObjects = new ArrayList<>();
        DatabaseTransferObject databaseTransferObject = new DatabaseTransferObject();
        databaseTransferObject.setName(dbName);
        databaseTransferObject.setMappedName(dbName);
        databaseTransferObjects.add(databaseTransferObject);

        List<TableTransferObject> tables = new ArrayList<>();
        TableTransferObject tableTransferObject = new TableTransferObject();
        tableTransferObject.setName("t1");
        tableTransferObject.setMappedName("t1_gho");
        tables.add(tableTransferObject);
        databaseTransferObject.setTables(tables);

        SpecificTransferMapping transferMapping = new SpecificTransferMapping();
        transferMapping.setDatabases(databaseTransferObjects);
        request.setTransferMapping(transferMapping);

        CommonTransferConfig commonTransferConfig = new CommonTransferConfig();
        request.setCommonTransferConfig(commonTransferConfig);

        FullTransferConfig fullTransferConfig = new FullTransferConfig();
        request.setFullTransferConfig(fullTransferConfig);

        IncrTransferConfig incrTransferConfig = new IncrTransferConfig();
        request.setIncrTransferConfig(incrTransferConfig);
        return request;
    }

}
