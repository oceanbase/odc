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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * ClassName: TableController Package: com.oceanbase.odc.server.web.controller.v2 Description:
 *
 * @Author: fenghao
 * @Create 2024/3/19 11:13
 * @Version 1.0
 */
@RestController
@RequestMapping("/api/v2/table")
public class TableController {

    @Autowired
    private TableService tableService;

    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看表的列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<Table>> list(@PathVariable String sid) {
        // sid:1-1:d:database
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        List<Table> tables =
                tableService.listTablesWithoutPage(sessionService.nullSafeGet(i.getSid(), true), i.getDatabaseId());
        return OdcResult.ok(tables);
    }

    @ApiOperation(value = "listTables", notes = "不需要资源鉴权，获取表的列表，用于权限申请等场景")
    @RequestMapping(value = "/listTables/{databaseId}", method = RequestMethod.GET)
    public OdcResult<List<Table>> listTables(@PathVariable Long databaseId) {
        List<Table> tables =
                tableService.listTablesWithoutPageByDatabaseId(databaseId);
        return OdcResult.ok(tables);
    }
}
