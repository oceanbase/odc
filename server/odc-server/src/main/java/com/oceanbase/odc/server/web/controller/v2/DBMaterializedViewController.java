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
import java.util.Collections;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.DBMaterializedViewService;
import com.oceanbase.odc.service.db.model.AllMVBaseTables;
import com.oceanbase.odc.service.db.model.DBViewResponse;
import com.oceanbase.odc.service.db.model.MVSyncDataReq;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import io.swagger.annotations.ApiOperation;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/3 11:02
 * @since: 4.3.4
 */

@RestController
@RequestMapping("/api/v2/materializedView")
public class DBMaterializedViewController {

    @Autowired
    private DBMaterializedViewService dbMaterializedViewService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "obtain the list of materialized views. Sid example: sid:1000-1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public ListResponse<Table> list(@PathVariable String sid,
            @RequestParam(name = "databaseId") Long databaseId,
            @RequestParam(name = "includePermittedAction", required = false,
                    defaultValue = "false") boolean includePermittedAction)
            throws SQLException, InterruptedException {
        // sid:1000-1
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(databaseId)
                .types(Collections.singletonList(DBObjectType.MATERIALIZED_VIEW))
                .includePermittedAction(includePermittedAction)
                .build();
        return Responses.list(dbMaterializedViewService.list(sessionService.nullSafeGet(i.getSid(), true), params));
    }

    @ApiOperation(value = "detail",
            notes = "obtain detail about the materialized view. Sid example: sid:1000-1:d:db1:v:v1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<DBViewResponse> detail(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult
                .ok(dbMaterializedViewService.detail(sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(),
                        i.getView()));
    }

    @ApiOperation(value = "listBases",
            notes = "obtain list of base tables under the current data source that are used to create the materialized view. Sid example: sid:1000-1")
    @RequestMapping(value = "/listAllBases/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<AllMVBaseTables> listAllBases(@PathVariable String sid, @RequestParam String name) {
        // sid:1000-1
        return OdcResult.ok(dbMaterializedViewService.listAllBases(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), name));
    }

    @ApiOperation(value = "getCreateSql",
            notes = "obtain the sql to create the materialized view, Sid example: sid:1000-1:d:db1:v:v1")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.PATCH)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody DBMView resource) {
        return OdcResult.ok(ResourceSql.ofSql(dbMaterializedViewService.getCreateSql(
            sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource)));
    }

    @ApiOperation(value = "syncData",
            notes = "obtain the sql to create the materialized view, Sid example: sid:1000-1")
    @RequestMapping(value = "/syncData/{sid:.*}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<Boolean> syncData(@PathVariable String sid,
            @RequestBody @Valid MVSyncDataReq mvSyncDataReq) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return Responses.success(dbMaterializedViewService.syncData(sessionService.nullSafeGet(i.getSid(), true),
                mvSyncDataReq));
    }

}
