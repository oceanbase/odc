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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigService;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/5/11 11:37
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/regulation/approvalFlows")
public class ApprovalFlowController {
    @Autowired
    private ApprovalFlowConfigService approvalFlowConfigService;

    @ApiOperation(value = "listApprovalFlows", notes = "List all approval flows")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ListResponse<ApprovalFlowConfig> listApprovalFlows() {
        return Responses.list(approvalFlowConfigService.list());
    }

    @ApiOperation(value = "detailApprovalFlow", notes = "Detail an approval process by id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ApprovalFlowConfig> detailApprovalFlow(@PathVariable Long id) {
        return Responses.success(approvalFlowConfigService.detail(id));
    }

    @ApiOperation(value = "existApprovalFlow", notes = "Check an approval process exists or not")
    @RequestMapping(value = "/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> existsApprovalFlow(@RequestParam String name) {
        return Responses.success(approvalFlowConfigService.exists(name));
    }

    @ApiOperation(value = "createApprovalFlow", notes = "Create an approval process")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SuccessResponse<ApprovalFlowConfig> createApprovalFlow(@RequestBody ApprovalFlowConfig approvalFlow) {
        return Responses.success(approvalFlowConfigService.create(approvalFlow));
    }

    @ApiOperation(value = "updateApprovalFlow", notes = "Update an approval process")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<ApprovalFlowConfig> updateApprovalFlow(@PathVariable Long id,
            @RequestBody ApprovalFlowConfig approvalFlow) {
        return Responses.success(approvalFlowConfigService.update(id, approvalFlow));
    }

    @ApiOperation(value = "deleteApprovalFlow", notes = "Delete an approval process by id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteApprovalFlow(@PathVariable Long id) {
        return Responses.success(approvalFlowConfigService.delete(id));
    }
}
