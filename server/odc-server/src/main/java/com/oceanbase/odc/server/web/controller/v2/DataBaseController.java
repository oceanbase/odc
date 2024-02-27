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

import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.DatabaseSyncManager;
import com.oceanbase.odc.service.connection.database.model.CreateDatabaseReq;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DeleteDatabasesReq;
import com.oceanbase.odc.service.connection.database.model.QueryDatabaseParams;
import com.oceanbase.odc.service.connection.database.model.TransferDatabasesReq;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/4/12 20:32
 * @Description: []
 */

@RestController
@RequestMapping("/api/v2/database")
public class DataBaseController {
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DatabaseSyncManager databaseSyncManager;

    @ApiOperation(value = "getDatabase", notes = "Detail a database")
    @RequestMapping(value = "/databases/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Database> getDatabase(@PathVariable Long id) {
        return Responses.success(databaseService.detail(id));
    }

    @ApiOperation(value = "listDatabases", notes = "List all databases")
    @RequestMapping(value = "/databases", method = RequestMethod.GET)
    public PaginatedResponse<Database> listDatabases(
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "existed") Boolean existed,
            @RequestParam(required = false, name = "dataSourceName") String dataSourceName,
            @RequestParam(required = false, name = "dataSourceId") Long dataSourceId,
            @RequestParam(required = false, name = "environmentId") Long environmentId,
            @RequestParam(required = false, name = "projectName") String projectName,
            @RequestParam(required = false, name = "projectId") Long projectId,
            @RequestParam(required = false, defaultValue = "false",
                    name = "containsUnassigned") Boolean containsUnassigned,
            @RequestParam(required = false, defaultValue = "false",
                    name = "includesPermittedAction") Boolean includesPermittedAction,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        QueryDatabaseParams params = QueryDatabaseParams.builder()
                .dataSourceId(dataSourceId)
                .existed(existed)
                .environmentId(environmentId)
                .schemaName(name)
                .containsUnassigned(containsUnassigned)
                .includesPermittedAction(includesPermittedAction)
                .projectId(projectId).build();
        return Responses.paginated(databaseService.list(params, pageable));
    }


    @RequestMapping(value = "/databases", method = RequestMethod.POST)
    public SuccessResponse<Database> create(@RequestBody CreateDatabaseReq req) {
        return Responses.success(databaseService.create(req));
    }

    @ApiOperation(value = "transferDatabases", notes = "Transfer databases to a project")
    @RequestMapping(value = "/databases/transfer", method = RequestMethod.POST)
    public SuccessResponse<Boolean> transferDatabase(@RequestBody TransferDatabasesReq req) {
        return Responses.success(databaseService.transfer(req));
    }

    @ApiOperation(value = "deleteDatabases", notes = "Delete non-existed databases")
    @RequestMapping(value = "/databases", method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteDatabases(@RequestBody DeleteDatabasesReq req) {
        return Responses.success(databaseService.deleteDatabases(req));
    }
}
