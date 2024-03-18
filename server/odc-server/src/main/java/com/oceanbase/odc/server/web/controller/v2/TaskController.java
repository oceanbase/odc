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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.task.executor.server.HeartRequest;
import com.oceanbase.odc.service.task.executor.task.DefaultTaskResult;
import com.oceanbase.odc.service.task.runtime.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.task.runtime.QueryDatabaseChangeChangineOrderResp;
import com.oceanbase.odc.service.task.runtime.QuerySensitiveColumnReq;
import com.oceanbase.odc.service.task.runtime.QuerySensitiveColumnResp;
import com.oceanbase.odc.service.task.service.DatabaseChangeChangingOrderTemplateService;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/task")
public class TaskController {

    @Autowired
    private TaskFrameworkService taskFrameworkService;

    @Autowired
    private DataMaskingService dataMaskingService;

    @Autowired
    private DatabaseChangeChangingOrderTemplateService databaseChangeChangingOrderTemplateService;

    @ApiOperation(value = "updateResult", notes = "update task result")
    @RequestMapping(value = "/result", method = RequestMethod.POST)
    public SuccessResponse<String> updateResult(@RequestBody DefaultTaskResult taskResult) {
        if (log.isDebugEnabled()) {
            log.debug("Accept task result {}.", JsonUtils.toJson(taskResult));
        }
        taskFrameworkService.handleResult(taskResult);
        return Responses.success("ok");
    }

    @ApiOperation(value = "heart", notes = "update heart request")
    @RequestMapping(value = "/heart", method = RequestMethod.POST)
    public SuccessResponse<String> heart(@RequestBody HeartRequest heart) {
        taskFrameworkService.handleHeart(heart);
        return Responses.success("ok");
    }

    @ApiOperation(value = "querySensitiveColumn", notes = "query sensitive columns")
    @RequestMapping(value = "/querySensitiveColumn", method = RequestMethod.POST)
    public SuccessResponse<QuerySensitiveColumnResp> querySensitiveColumn(@RequestBody QuerySensitiveColumnReq req) {
        return Responses.success(dataMaskingService.querySensitiveColumn(req));
    }

    @ApiOperation(value = "createOrModifyDatabaseTemplate", notes = "根据id是否有值来执行新增还是修改")
    @PostMapping("/databasechange/changingorder/templates")
    public SuccessResponse<Boolean> createOrModifyDatabaseTemplate(
            @RequestBody CreateDatabaseChangeChangingOrderReq req) {
        return Responses.success(databaseChangeChangingOrderTemplateService.createOrModifyDatabaseTemplate(req));
    }

    @ApiOperation(value = "queryDatabaseTemplateById", notes = "根据id查询模版详情")
    @GetMapping("/databasechange/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<QueryDatabaseChangeChangineOrderResp> queryDatabaseTemplateById(@PathVariable Long id) {
        return Responses.success(databaseChangeChangingOrderTemplateService.queryDatabaseTemplateById(id));
    }

    @ApiOperation(value = "listDatabaseTemplate", notes = "获取模版列表")
    @GetMapping("/databasechange/changingorder/templates")
    public PaginatedResponse<QueryDatabaseChangeChangineOrderResp> listDatabaseTemplate(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        return Responses.paginated(databaseChangeChangingOrderTemplateService.listDatabaseTemplate(pageable));
    }

    @ApiOperation(value = "deleteDatabaseTemplateById", notes = "删除单个模板")
    @DeleteMapping("/databasechange/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<Boolean> deleteDatabaseTemplateById(@PathVariable Long id) {
        return Responses.success(databaseChangeChangingOrderTemplateService.deleteDatabseTemplateById(id));
    }
}
