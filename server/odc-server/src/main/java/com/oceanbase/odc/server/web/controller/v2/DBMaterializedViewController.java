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
import java.util.Base64;
import java.util.Collections;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.DBMaterializedViewService;
import com.oceanbase.odc.service.db.model.AllMVBaseTables;
import com.oceanbase.odc.service.db.model.GenerateTableDDLResp;
import com.oceanbase.odc.service.db.model.GenerateUpdateMViewDDLReq;
import com.oceanbase.odc.service.db.model.MViewRefreshReq;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshRecord;
import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshRecordParam;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBSchema;

import io.swagger.annotations.ApiOperation;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/3 11:02
 * @since: 4.3.4
 */

@RestController
@RequestMapping("/api/v2/connect/sessions")
@Validated
public class DBMaterializedViewController {

    @Autowired
    private DBMaterializedViewService dbMaterializedViewService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "obtain a list of all materialized views under the specified database.")
    @GetMapping(value = "/{sessionId}/databases/{databaseId}/materializedViews")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<Table> list(@PathVariable String sessionId,
            @PathVariable Long databaseId,
            @RequestParam(name = "includePermittedAction", required = false,
                    defaultValue = "false") boolean includePermittedAction)
            throws SQLException, InterruptedException {
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(databaseId)
                .types(Collections.singletonList(DBObjectType.MATERIALIZED_VIEW))
                .includePermittedAction(includePermittedAction)
                .build();
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.list(dbMaterializedViewService.list(session, params));
    }

    @ApiOperation(value = "detail", notes = "obtain details for the specified materialized view.")
    @GetMapping(value = "/{sessionId}/databases/{databaseName}/materializedViews/{mvName}")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBMaterializedView> detail(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvName) {
        Base64.Decoder decoder = Base64.getDecoder();
        mvName = new String(decoder.decode(mvName));
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewService.detail(session, databaseName, mvName));
    }

    @ApiOperation(value = "listAllBases",
            notes = "obtain list of all base tables under the current datasource that are used to create the materialized view.")
    @GetMapping(value = "/{sessionId}/listMaterializedViewBases")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<AllMVBaseTables> listAllBases(@PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "") String name) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewService.listAllBases(session, name));
    }

    @ApiOperation(value = "generateCreateSql", notes = "obtain the sql to create the materialized view.")
    @PostMapping(value = "/{sessionId}/databases/{databaseName}/materializedViews/{mvName}/generateCreateDDL")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<String> generateCreateSql(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvName,
            @RequestBody DBMaterializedView materializedView) {
        materializedView.setName(mvName);
        materializedView.setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewService.getCreateSql(
                session, materializedView));
    }

    @ApiOperation(value = "generateUpdateDDL", notes = "obtain the sql to update the materialized view.")
    @PostMapping(value = "/{sessionId}/databases/{databaseName}/materializedViews/generateUpdateMViewDDL")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<GenerateTableDDLResp> generateUpdateMViewDDL(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName, @RequestBody GenerateUpdateMViewDDLReq req) {
        DBSchema schema = DBSchema.of(databaseName);
        req.getPrevious().setSchema(schema);
        req.getPrevious().setSchemaName(databaseName);
        req.getCurrent().setSchema(schema);
        req.getCurrent().setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewService.generateUpdateDDL(session, req));
    }

    @ApiOperation(value = "refresh", notes = "refresh data of the materialized view.")
    @PostMapping(value = "/{sessionId}/databases/{databaseName}/materializedViews/{mvName}/refresh")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> refresh(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvName,
            @RequestBody @Valid MViewRefreshReq refreshReq) {
        refreshReq.setMvName(mvName);
        refreshReq.setDatabaseName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewService.refresh(session, refreshReq));
    }

    @ApiOperation(value = "getRefreshRecords", notes = "obtain refresh records for the specified materialized view.")
    @GetMapping(value = "/{sessionId}/databases/{databaseName}/materializedViews/{mvName}/refreshRecords")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<DBMViewRefreshRecord> getRefreshRecords(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvName,
            @RequestParam(required = false, defaultValue = "1000") @Min(value = 1,
                    message = "queryLimit must be greater than or equal to 1") @Max(value = 100000,
                            message = "queryLimit must be less than or equal to 100000") Integer queryLimit) {
        DBMViewRefreshRecordParam param = new DBMViewRefreshRecordParam(databaseName, mvName,
                queryLimit);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.list(dbMaterializedViewService.listRefreshRecords(session, param));
    }

}
