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

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;

import io.swagger.annotations.ApiOperation;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/19 11:13
 * @Version 1.0
 */
@RestController
@RequestMapping("/api/v2/databaseSchema")
public class DBSchemaController {

    @Autowired
    private TableService tableService;

    @ApiOperation(value = "listTables", notes = "List tables with permitted actions")
    @RequestMapping(value = "/tables", method = RequestMethod.GET)
    public ListResponse<Table> list(@RequestParam(name = "databaseId") Long databaseId,
            @RequestParam(name = "includePermittedAction", required = false,
                    defaultValue = "false") boolean includePermittedAction)
            throws SQLException, InterruptedException {
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(databaseId)
                .includePermittedAction(includePermittedAction)
                .build();
        return Responses.list(tableService.list(params));
    }

}
