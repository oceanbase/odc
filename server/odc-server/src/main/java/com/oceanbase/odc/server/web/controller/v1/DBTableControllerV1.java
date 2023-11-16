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
package com.oceanbase.odc.server.web.controller.v1;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.db.DBTableService;
import com.oceanbase.odc.service.db.model.OdcDBTable;
import com.oceanbase.odc.service.db.model.OdcGenerateUpdateTableDDLReq;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController

@RequestMapping("/api/v1/table")
public class DBTableControllerV1 {

    @Autowired
    private DBTableService tableService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看表的列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<OdcDBTable>> list(@PathVariable String sid) {
        // sid:1-1:d:database
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        String dbName = i.getDatabase();
        List<DBTable> tables = tableService.listTables(sessionService.nullSafeGet(i.getSid(), true), dbName);
        return OdcResult.ok(tables.stream().map(OdcDBTable::new).collect(Collectors.toList()));
    }

    @ApiOperation(value = "detail", notes = "查看表的详细信息(字符编码、ddl等)，sid示例：sid:1000-1:d:db1:t:tb1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<OdcDBTable> detail(@PathVariable String sid) {
        // parse sid and database name, sid:1-1:d:database:t:tb1
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(new OdcDBTable(tableService.getTable(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(), i.getTable())));
    }

    @ApiOperation(value = "getUpdateSql", notes = "获取修改表名的sql")
    @RequestMapping(value = "/getUpdateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getUpdateSql(@PathVariable String sid,
            @RequestBody OdcGenerateUpdateTableDDLReq req) {
        // parse sid and database name, sid:1-1:d:database:t:tb1
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        if (req.getPrevious() == null) {
            Validate.notNull(i.getTable(), "Old table name can not be null");
            DBTable previous = new DBTable();
            BeanUtils.copyProperties(req.getCurrent(), previous);
            previous.setName(i.getTable());
            req.setPrevious(previous);
        }
        ConnectionSession session = sessionService.nullSafeGet(i.getSid(), true);
        String schema = ConnectionSessionUtil.getCurrentSchema(session);
        if (req.getCurrent().getSchemaName() == null) {
            req.getCurrent().setSchemaName(schema);
        }
        if (req.getPrevious().getSchemaName() == null) {
            req.getPrevious().setSchemaName(schema);
        }
        return OdcResult.ok(ResourceSql.ofSql(tableService.generateUpdateDDL(session, req).getSql()));
    }

}
