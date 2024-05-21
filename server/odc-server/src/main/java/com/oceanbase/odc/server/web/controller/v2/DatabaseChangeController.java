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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.databasechange.DatabaseChangeChangingOrderTemplateService;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderTemplateReq;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeChangingOrderTemplateResp;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingOrderTemplateExists;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderParams;
import com.oceanbase.odc.service.databasechange.model.UpdateDatabaseChangeChangingOrderReq;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/4/18
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/databasechange")
public class DatabaseChangeController {
    @Autowired
    private DatabaseChangeChangingOrderTemplateService templateService;

    @ApiOperation(value = "createDatabaseChangingOrderTemplate", notes = "create database changing order template")
    @PostMapping("/changingorder/templates")
    public SuccessResponse<DatabaseChangeChangingOrderTemplateResp> create(
            @RequestBody CreateDatabaseChangeChangingOrderTemplateReq req) {
        return Responses.success(templateService.create(req));
    }

    @ApiOperation(value = "modifyDatabaseChangingOrderTemplate",
            notes = "modify database changing order template")
    @PutMapping("/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<DatabaseChangeChangingOrderTemplateResp> update(@PathVariable Long id,
            @RequestBody UpdateDatabaseChangeChangingOrderReq req) {
        return Responses
                .success(templateService.update(id, req));
    }

    @ApiOperation(value = "queryDatabaseChangingOrderTemplateById",
            notes = "query database changing order template's detail by id")
    @GetMapping("/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<DatabaseChangeChangingOrderTemplateResp> detail(
            @PathVariable Long id) {
        return Responses.success(templateService.detail(id));
    }

    @ApiOperation(value = "listDatabaseChangingOrderTemplates",
            notes = "get a list of database changing order templates")
    @GetMapping("/changingorder/templates")
    public PaginatedResponse<DatabaseChangeChangingOrderTemplateResp> list(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "creatorId") Long creatorId,
            @RequestParam(required = true, name = "projectId") Long projectId) {
        QueryDatabaseChangeChangingOrderParams queryDatabaseChangeChangingOrderParams =
                QueryDatabaseChangeChangingOrderParams.builder()
                        .name(name)
                        .creatorId(creatorId)
                        .projectId(projectId)
                        .build();
        return Responses
                .paginated(templateService.listTemplates(pageable,
                        queryDatabaseChangeChangingOrderParams));
    }

    @ApiOperation(value = "deleteDatabaseChangingOrderTemplateById",
            notes = "delete database changing order template by id")
    @DeleteMapping("/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<DatabaseChangeChangingOrderTemplateResp> delete(
            @PathVariable Long id) {
        return Responses
                .success(templateService.delete(id));
    }

    @ApiOperation(value = "exists", notes = "Returns whether an database changing order template exists")
    @RequestMapping(value = "/changingorder/templates/exists", method = RequestMethod.GET)
    public SuccessResponse<DatabaseChangingOrderTemplateExists> exists(@RequestParam String name,
            @RequestParam Long projectId) {
        return Responses.success(templateService.exists(name, projectId));
    }
}


