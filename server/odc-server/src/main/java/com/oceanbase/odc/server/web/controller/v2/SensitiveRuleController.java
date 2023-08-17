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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.datasecurity.SensitiveRuleService;
import com.oceanbase.odc.service.datasecurity.model.QuerySensitiveRuleParams;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2023/5/9 16:25
 */
@RestController
@RequestMapping("api/v2/collaboration/projects/{projectId:[\\d]+}/sensitiveRules")
public class SensitiveRuleController {

    @Autowired
    private SensitiveRuleService service;

    @ApiOperation(value = "sensitiveRuleExists", notes = "Check if sensitive rule exists")
    @RequestMapping(value = "/exists", method = RequestMethod.POST)
    public SuccessResponse<Boolean> exists(@PathVariable Long projectId, @RequestParam String name) {
        return Responses.success(service.exists(projectId, name));
    }

    @ApiOperation(value = "createSensitiveRule", notes = "Create a sensitive rule")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public SuccessResponse<SensitiveRule> createSensitiveRule(@PathVariable Long projectId,
            @RequestBody SensitiveRule rule) {
        rule.validate();
        return Responses.success(service.create(projectId, rule));
    }

    @ApiOperation(value = "detailSensitiveRule", notes = "View sensitive rule details")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<SensitiveRule> detailSensitiveRule(@PathVariable Long projectId, @PathVariable Long id) {
        return Responses.success(service.detail(projectId, id));
    }

    @ApiOperation(value = "updateSensitiveRule", notes = "Update a sensitive rule")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<SensitiveRule> updateSensitiveRule(@PathVariable Long projectId, @PathVariable Long id,
            @RequestBody SensitiveRule rule) {
        rule.validate();
        return Responses.success(service.update(projectId, id, rule));
    }

    @ApiOperation(value = "deleteSensitiveRule", notes = "Delete a sensitive rule")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<SensitiveRule> deleteSensitiveRule(@PathVariable Long projectId, @PathVariable Long id) {
        return Responses.success(service.delete(projectId, id));
    }

    @ApiOperation(value = "listSensitiveRules", notes = "List sensitive rules")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public PaginatedResponse<SensitiveRule> listSensitiveRules(@PathVariable Long projectId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) List<SensitiveRuleType> types,
            @RequestParam(name = "maskingAlgorithm", required = false) List<Long> maskingAlgorithmIds,
            @RequestParam(name = "enabled", required = false) List<Boolean> enabledList,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        Boolean enabled = CollectionUtils.size(enabledList) == 1 ? enabledList.get(0) : null;
        QuerySensitiveRuleParams params = QuerySensitiveRuleParams.builder()
                .projectId(projectId)
                .name(name)
                .types(types)
                .maskingAlgorithmIds(maskingAlgorithmIds)
                .enabled(enabled).build();
        return Responses.paginated(service.list(projectId, params, pageable));
    }

    @ApiOperation(value = "setEnabled", notes = "Enable or disable a sensitive rule")
    @RequestMapping(value = "/{id:[\\d]+}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<SensitiveRule> setEnabled(@PathVariable Long projectId,
            @PathVariable Long id, @RequestBody SetEnabledReq req) {
        return Responses.success(service.setEnabled(projectId, id, req.getEnabled()));
    }

}
