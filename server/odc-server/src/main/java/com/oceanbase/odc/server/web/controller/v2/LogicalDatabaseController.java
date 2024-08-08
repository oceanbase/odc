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
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableService;
import com.oceanbase.odc.service.connection.logicaldatabase.model.CreateLogicalDatabaseReq;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTableTopologyResp;

/**
 * @Author: Lebie
 * @Date: 2024/5/7 15:07
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/connect/logicaldatabase")
public class LogicalDatabaseController {
    @Autowired
    private LogicalDatabaseService databaseService;

    @Autowired
    private LogicalTableService tableService;

    @RequestMapping(value = "/logicaldatabases", method = RequestMethod.POST)
    public SuccessResponse<Boolean> create(@RequestBody CreateLogicalDatabaseReq req) {
        return Responses.success(databaseService.create(req));
    }

    @RequestMapping(value = "/logicaldatabases/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<DetailLogicalDatabaseResp> detail(@PathVariable Long id) {
        return Responses.success(databaseService.detail(id));
    }

    @RequestMapping(
            value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/logicaltables/{logicalTableId:[\\d]+}",
            method = RequestMethod.GET)
    public SuccessResponse<DetailLogicalTableResp> detailLogicalTable(@PathVariable Long logicalDatabaseId,
            @PathVariable Long logicalTableId) {
        return Responses.success(tableService.detail(logicalDatabaseId, logicalTableId));
    }

    @RequestMapping(
        value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/previewLogicalTableTopologies", method = RequestMethod.POST)
    public ListResponse<LogicalTableTopologyResp> previewLogicalTableTopologies(@PathVariable Long logicalDatabaseId,
        @RequestParam(name = "expression") String expression) {
        return Responses.list(tableService.previewLogicalTableTopologies(logicalDatabaseId, expression));
    }

    @RequestMapping(
            value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/logicaltables/{logicalTableId:[\\d]+}/topologies",
            method = RequestMethod.GET)
    public ListResponse<LogicalTableTopologyResp> listLogicalTablesTopologies(@PathVariable Long logicalDatabaseId,
            @PathVariable Long logicalTableId) {
        return Responses.list(tableService.listLogicalTableTopologies(logicalDatabaseId, logicalTableId));
    }

    @RequestMapping(value = "/logicaldatabases/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> delete(@PathVariable Long id) {
        return Responses.success(databaseService.delete(id));
    }

    @RequestMapping(value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/logicaltables/{logicalTableId:[\\d]+}",
            method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteLogicalTable(@PathVariable Long logicalDatabaseId,
            @PathVariable Long logicalTableId) {
        return Responses.success(tableService.delete(logicalDatabaseId, logicalTableId));
    }

    @RequestMapping(value = "/logicaldatabases/{id:[\\d]+}/logicaltables/extract", method = RequestMethod.POST)
    public SuccessResponse<Boolean> extractLogicalTables(@PathVariable Long id) {
        return Responses.success(databaseService.extractLogicalTables(id));
    }

    @RequestMapping(
            value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/logicaltables/{logicalTableId:[\\d]+}/checkStructureConsistency",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> checkLogicalTable(@PathVariable Long logicalDatabaseId,
            @PathVariable Long logicalTableId) {
        return Responses.success(tableService.checkStructureConsistency(logicalDatabaseId, logicalTableId));
    }
}
