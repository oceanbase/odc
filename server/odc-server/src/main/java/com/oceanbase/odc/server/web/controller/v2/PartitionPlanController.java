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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.model.DatabasePartitionPlan;

/**
 * @Author：tianke
 * @Date: 2022/9/16 15:38
 * @Descripition:
 */
@RestController
@RequestMapping("/api/v2/partitionPlan")
public class PartitionPlanController {

    @Autowired
    private PartitionPlanService partitionPlanService;

    @RequestMapping(value = "/partitionPlans", method = RequestMethod.GET)
    public SuccessResponse<DatabasePartitionPlan> getPartitionPlans(@RequestParam Long databaseId,
            @RequestParam(required = false) Long flowInstanceId) {
        return Responses
                .success(partitionPlanService.findRangeTablePlan(databaseId, flowInstanceId));
    }

    @RequestMapping(value = "/partitionPlans/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<String> update(@RequestParam Long id,
            @RequestBody DatabasePartitionPlan databasePartitionPlan) throws IOException {
        databasePartitionPlan.setId(id);
        partitionPlanService.updateTablePartitionPlan(databasePartitionPlan);
        return Responses.success("ok");
    }

    @RequestMapping(value = "/partitionPlans/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> exist(@RequestParam("databaseId") Long databaseId) {
        return Responses.success(partitionPlanService.hasConnectionPartitionPlan(databaseId));
    }

}
