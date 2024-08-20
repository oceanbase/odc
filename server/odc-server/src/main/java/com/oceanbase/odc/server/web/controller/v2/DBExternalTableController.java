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
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.DBExternalTableService;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/8/19 20:38
 * @since: 4.3.3
 */
@RestController
@RequestMapping("/api/v2/externalTable")
public class DBExternalTableController {

    @Autowired
    private DBExternalTableService dbExternalTableService;

    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "listExternalTables", notes = "List external tables with permitted actions")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ListResponse<Table> list(@RequestParam(name = "databaseId") Long databaseId,
            @RequestParam(name = "includePermittedAction", required = false,
                    defaultValue = "false") boolean includePermittedAction)
            throws SQLException, InterruptedException {
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(databaseId)
                .includePermittedAction(includePermittedAction)
                .build();
        return Responses.list(dbExternalTableService.list(params));
    }


}
