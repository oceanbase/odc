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
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.databasechange.DatabaseChangeChangingOrderTemplateService;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderResp;

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
    private DatabaseChangeChangingOrderTemplateService databaseChangeChangingOrderTemplateService;

    @ApiOperation(value = "createDatabaseChangingOrderTemplate", notes = "create database changing order template")
    @PostMapping("/changingorder/templates")
    public SuccessResponse<Boolean> createDatabaseChangingOrderTemplate(
            @RequestBody CreateDatabaseChangeChangingOrderReq req) {
        return Responses.success(databaseChangeChangingOrderTemplateService.createDatabaseChangingOrderTemplate(req));
    }

    @ApiOperation(value = "modifyDatabaseChangingOrderTemplate/{id:[\\d]+}",
            notes = "modify database changing order template")
    @PutMapping("/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<Boolean> modifyDatabaseChangingOrderTemplate(@PathVariable Long id,
            @RequestBody CreateDatabaseChangeChangingOrderReq req) {
        return Responses
                .success(databaseChangeChangingOrderTemplateService.modifyDatabaseChangingOrderTemplate(id, req));
    }

    @ApiOperation(value = "queryDatabaseChangingOrderTemplateById",
            notes = "query database changing order template's detail by id")
    @GetMapping("/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<QueryDatabaseChangeChangingOrderResp> queryDatabaseChangingOrderTemplateById(
            @PathVariable Long id) {
        return Responses.success(databaseChangeChangingOrderTemplateService.queryDatabaseChangingOrderTemplateById(id));
    }

    @ApiOperation(value = "listDatabaseChangingOrderTemplates",
            notes = "get a list of database changing order templates")
    @GetMapping("/changingorder/templates")
    public PaginatedResponse<QueryDatabaseChangeChangingOrderResp> listDatabaseChangingOrderTemplates(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        return Responses
                .paginated(databaseChangeChangingOrderTemplateService.listDatabaseChangingOrderTemplates(pageable));
    }

    @ApiOperation(value = "deleteDatabaseChangingOrderTemplateById",
            notes = "delete database changing order template by id")
    @DeleteMapping("/changingorder/templates/{id:[\\d]+}")
    public SuccessResponse<Boolean> deleteDatabaseChangingOrderTemplateById(@PathVariable Long id) {
        return Responses
                .success(databaseChangeChangingOrderTemplateService.deleteDatabaseChangingOrderTemplateById(id));
    }
}


