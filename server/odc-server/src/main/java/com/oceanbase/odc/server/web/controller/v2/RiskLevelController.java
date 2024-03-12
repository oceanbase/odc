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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/5/11 14:16
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/regulation/risklevels")
public class RiskLevelController {

    @Autowired
    private RiskLevelService riskLevelService;

    @ApiOperation(value = "listRiskLevels", notes = "List all risk levels")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ListResponse<RiskLevel> listRiskLevels() {
        return Responses.list(riskLevelService.list());
    }

    @ApiOperation(value = "detailRiskLevel", notes = "Detail a risk level by id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<RiskLevel> detailRiskLevel(@PathVariable Long id) {
        return Responses.success(riskLevelService.detail(id));
    }

    @ApiOperation(value = "updateRiskLevel", notes = "Update a risk level")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<RiskLevel> updateRiskLevel(@PathVariable Long id, @RequestBody RiskLevel riskLevel) {
        return Responses.success(riskLevelService.update(id, riskLevel));
    }
}
