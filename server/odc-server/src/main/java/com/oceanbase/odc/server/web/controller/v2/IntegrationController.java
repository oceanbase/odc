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
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.QueryIntegrationParams;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2023/3/23 20:39
 */
@RestController
@RequestMapping("/api/v2/integration")
public class IntegrationController {
    @Autowired
    private IntegrationService integrationService;

    @ApiOperation(value = "exists", notes = "检查外部集成是否存在")
    @RequestMapping(value = "/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> exists(@RequestParam String name, @RequestParam IntegrationType type) {
        return Responses.success(integrationService.exists(name, type));
    }

    @ApiOperation(value = "createIntegration", notes = "创建外部集成配置")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public SuccessResponse<IntegrationConfig> createApprovalIntegration(
            @RequestBody IntegrationConfig config) {
        return Responses.success(integrationService.create(config));
    }

    @ApiOperation(value = "detailIntegration", notes = "查询外部集成配置")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<IntegrationConfig> detailIntegration(@PathVariable Long id) {
        return Responses.success(integrationService.detail(id));
    }

    @ApiOperation(value = "listIntegrations", notes = "查询外部集成配置列表")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public PaginatedResponse<IntegrationConfig> listIntegrations(
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "type") IntegrationType type,
            @RequestParam(required = false, name = "creatorName") String creatorName,
            @RequestParam(required = false, name = "enabled") List<Boolean> enabledList,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        Boolean enabled = CollectionUtils.size(enabledList) == 1 ? enabledList.get(0) : null;
        QueryIntegrationParams params = QueryIntegrationParams.builder()
                .name(name)
                .type(type)
                .creatorName(creatorName)
                .enabled(enabled)
                .build();
        return Responses.paginated(integrationService.list(params, pageable));
    }

    @ApiOperation(value = "deleteIntegration", notes = "删除外部集成配置")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<IntegrationConfig> deleteIntegration(@PathVariable Long id) {
        return Responses.success(integrationService.delete(id));
    }

    @ApiOperation(value = "updateIntegration", notes = "更新外部集成配置")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<IntegrationConfig> updateIntegration(@PathVariable Long id,
            @RequestBody IntegrationConfig config) {
        return Responses.success(integrationService.update(id, config));
    }

    @ApiOperation(value = "setIntegrationEnabled", notes = "启用/禁用外部集成配置")
    @RequestMapping(value = "/{id:[\\d]+}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<IntegrationConfig> setApprovalIntegrationEnabled(@PathVariable Long id,
            @RequestBody SetEnabledReq req) {
        return Responses.success(integrationService.setEnabled(id, req.getEnabled()));
    }
}
