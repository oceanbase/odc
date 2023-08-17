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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.automation.AutomationEventService;
import com.oceanbase.odc.service.automation.AutomationEventService.PromptVo;
import com.oceanbase.odc.service.automation.AutomationService;
import com.oceanbase.odc.service.automation.model.AutomationEventMetadata;
import com.oceanbase.odc.service.automation.model.AutomationRule;
import com.oceanbase.odc.service.automation.model.CreateRuleReq;
import com.oceanbase.odc.service.automation.model.QueryAutomationRuleParams;
import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v2/management/auto")
public class AutomationController {
    @Autowired
    private AutomationService automationService;

    @Autowired
    private AutomationEventService automationEventService;

    @ApiOperation(value = "listEventMetadata", notes = "查询所有触发事件的元数据")
    @RequestMapping(value = "/eventMetadata", method = RequestMethod.GET)
    public ListResponse<AutomationEventMetadata> listEventMetadata() {
        return Responses.list(automationService.listEventMetadata());
    }

    @ApiOperation(value = "getAutomationRule", notes = "获取自动触发规则详情")
    @RequestMapping(value = "/rules/{id}", method = RequestMethod.GET)
    public SuccessResponse<AutomationRule> getAutomationRule(@PathVariable Long id) {
        return Responses.success(automationService.detail(id));
    }

    @ApiOperation(value = "listAutomationRules", notes = "查询规则列表")
    @RequestMapping(value = "/rules", method = RequestMethod.GET)
    public PaginatedResponse<AutomationRule> listAutomationRules(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "creatorName") String creatorName,
            @RequestParam(required = false, name = "enabled") Boolean enabled) {
        QueryAutomationRuleParams params =
                QueryAutomationRuleParams.builder()
                        .name(name).creatorName(creatorName).enabled(enabled)
                        .build();
        return Responses.paginated(automationService.listRules(pageable, params));
    }

    @ApiOperation(value = "createAutomationRule", notes = "创建规则")
    @RequestMapping(value = "/rules", method = RequestMethod.POST)
    public SuccessResponse<AutomationRule> createAutomationRule(@RequestBody CreateRuleReq createRuleReq) {
        return Responses.success(automationService.create(createRuleReq));
    }

    @ApiOperation(value = "enableAutomationRule", notes = "启用或停用规则")
    @RequestMapping(value = "/rules/{id}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<AutomationRule> enableAutomationRule(@PathVariable Long id, @RequestBody SetEnabledReq req) {
        return Responses.success(automationService.setRuleEnabled(id, req.getEnabled()));
    }

    @ApiOperation(value = "updateAutomationRule", notes = "更新规则详情")
    @RequestMapping(value = "/rules/{id}", method = RequestMethod.PUT)
    public SuccessResponse<AutomationRule> updateAutomationRule(@PathVariable Long id, @RequestBody CreateRuleReq req) {
        return Responses.success(automationService.update(id, req));
    }

    @ApiOperation(value = "deleteAutomationRule", notes = "删除规则")
    @RequestMapping(value = "/rules/{id}", method = RequestMethod.DELETE)
    public SuccessResponse<AutomationRule> deleteAutomationRule(@PathVariable Long id) {
        return Responses.success(automationService.delete(id));
    }

    @ApiOperation(value = "exists", notes = "查询规则是否存在")
    @RequestMapping(value = "/rules/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> exists(@RequestParam String name) {
        return Responses.success(automationService.exists(name));
    }

    @ApiOperation(value = "exists", notes = "查询某个事件的条件表达式")
    @RequestMapping(value = "/rules/prompt", method = RequestMethod.GET)
    public SuccessResponse<PromptVo> extraInfo(String eventName) {
        return Responses.success(automationEventService.promptExpression(eventName));
    }

}
