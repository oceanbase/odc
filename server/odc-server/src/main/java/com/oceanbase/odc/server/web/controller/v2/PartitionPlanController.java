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
package com.oceanbase.odc.server.web.controller.v2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.model.ConnectionPartitionPlan;

/**
 * @Author：tianke
 * @Date: 2022/9/16 15:38
 * @Descripition:
 */
@RestController
@ConditionalOnProperty(name = "odc.feature.partitionplan.enabled", havingValue = "true")
public class PartitionPlanController {

    @Autowired
    private PartitionPlanService partitionPlanService;

    @RequestMapping(value = "/api/v2/patitionplan/ConnectionPartitionPlan", method = RequestMethod.GET)
    public SuccessResponse<ConnectionPartitionPlan> getConnectionPartitionPlan(@RequestParam Long connectionId,
            @RequestParam(required = false) Long flowInstanceId) {
        return Responses
                .success(partitionPlanService.findRangeTablePlan(connectionId, flowInstanceId));
    }


    @RequestMapping(value = "/api/v2/partitionplan/ConnectionPartitionPlan/batchUpdate", method = RequestMethod.PUT)
    public SuccessResponse<String> updateConnectionPartitionPlan(
            @RequestBody ConnectionPartitionPlan connectionPartitionPlan) throws IOException {
        partitionPlanService.updateTablePartitionPlan(connectionPartitionPlan);
        return Responses.success("ok");
    }

    @RequestMapping(value = "/api/v2/partitionplan/ConnectionPartitionPlan/exist", method = RequestMethod.GET)
    public SuccessResponse<Boolean> hasConnectionPartitionPlan(@RequestParam("connectionId") Long connectionId) {
        return Responses.success(partitionPlanService.hasConnectionPartitionPlan(connectionId));
    }

}
