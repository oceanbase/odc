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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.model.QueryRiskDetectRuleParams;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/5/14 22:17
 * @Description: []
 */

@RestController
@RequestMapping("/api/v2/regulation/riskDetectRules")
public class RiskDetectRuleController {
    @Autowired
    private RiskDetectService riskDetectService;

    @ApiOperation(value = "createRiskDetectRule", notes = "Create a risk detect rule")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SuccessResponse<RiskDetectRule> createRiskDetectRules(@RequestBody RiskDetectRule rule) {
        return Responses.success(riskDetectService.create(rule));
    }

    @ApiOperation(value = "listRiskDetectRules", notes = "List all risk detect rules")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ListResponse<RiskDetectRule> listRiskDetectRules(
            @RequestParam(name = "riskLevelId") Long riskLevelId,
            @RequestParam(required = false, name = "name") String name) {
        QueryRiskDetectRuleParams params = QueryRiskDetectRuleParams.builder().riskLevelId(riskLevelId).build();
        if (Objects.nonNull(name)) {
            params.setName(name);
        }
        return Responses.list(riskDetectService.list(params));
    }

    @ApiOperation(value = "detailRiskDetectRule", notes = "Detail a risk detect rule by id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<RiskDetectRule> detailRiskDetectRule(@PathVariable Long id) {
        return Responses.success(riskDetectService.detail(id));
    }

    @ApiOperation(value = "updateRiskDetectRule", notes = "Update a risk detect rule by id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<RiskDetectRule> updateRiskDetectRule(@PathVariable Long id,
            @RequestBody RiskDetectRule riskLevel) {
        return Responses.success(riskDetectService.update(id, riskLevel));
    }

    @ApiOperation(value = "deleteRiskDetectRule", notes = "Delete a risk detect rule by id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<RiskDetectRule> deleteRiskDetectRule(@PathVariable Long id) {
        return Responses.success(riskDetectService.delete(id));
    }
}
