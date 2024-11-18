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

import java.util.Base64;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.db.DBTableService;
import com.oceanbase.odc.service.db.model.GenerateTableDDLResp;
import com.oceanbase.odc.service.db.model.GenerateUpdateTableDDLReq;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanDBTable;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBSchema;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("api/v2/connect/sessions")
public class DBTableController {

    @Autowired
    private DBTableService tableService;
    @Autowired
    private ConnectSessionService sessionService;
    @Autowired
    private PartitionPlanService partitionPlanService;

    @GetMapping(value = {"/{sessionId}/databases/{databaseName}/tables", "/{sessionId}/currentDatabase/tables"})
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<String> listTables(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName,
            @RequestParam(required = false, name = "fuzzyTableName") String fuzzyTableName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        if (!StringUtils.isEmpty(fuzzyTableName)) {
            return Responses.list(tableService.showTablesLike(session, databaseName, fuzzyTableName));
        } else {
            return Responses.list(tableService.listTables(session, databaseName).stream().map(DBTable::getName).collect(
                    Collectors.toList()));
        }
    }

    @GetMapping(value = {"/{sessionId}/databases/{databaseName}/tables/{tableName}",
            "/{sessionId}/currentDatabase/tables/{tableName}"})
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBTable> getTable(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName,
            @PathVariable String tableName,
            @RequestParam(required = false, name = "type", defaultValue = "TABLE") DBObjectType type) {
        Base64.Decoder decoder = Base64.getDecoder();
        tableName = new String(decoder.decode(tableName));
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(tableService.getTable(session, databaseName, tableName, type));
    }

    @PostMapping(value = {"/{sessionId}/databases/{databaseName}/tables/generateCreateTableDDL",
            "/{sessionId}/currentDatabase/tables/generateCreateTableDDL"})
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<GenerateTableDDLResp> generateCreateTableDDL(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName, @RequestBody DBTable table) {
        table.setSchema(DBSchema.of(databaseName));
        table.setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(tableService.generateCreateDDL(session, table));
    }

    @PostMapping(value = {"/{sessionId}/databases/{databaseName}/tables/generateUpdateTableDDL",
            "/{sessionId}/currentDatabase/tables/generateUpdateTableDDL"})
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<GenerateTableDDLResp> generateUpdateTableDDL(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName, @RequestBody GenerateUpdateTableDDLReq req) {
        DBSchema schema = DBSchema.of(databaseName);
        req.getPrevious().setSchema(schema);
        req.getPrevious().setSchemaName(databaseName);
        req.getCurrent().setSchema(schema);
        req.getCurrent().setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(tableService.generateUpdateDDL(session, req));
    }

    @GetMapping(value = "/{sessionId}/databases/{databaseId}/candidatePartitionPlanTables")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<PartitionPlanDBTable> listTables(@PathVariable String sessionId,
            @PathVariable Long databaseId) {
        return Responses.list(this.partitionPlanService.listCandidateTables(sessionId, databaseId));
    }

    @GetMapping(value = "/{sessionId}/databases/{databaseId}/candidatePartitionPlanTables/"
            + "{tableName}/getPartitionKeyDataTypes")
    public ListResponse<DataType> getPartitionKeyDataTypes(@PathVariable String sessionId,
            @PathVariable Long databaseId, @PathVariable String tableName) {
        return Responses.list(this.partitionPlanService.getPartitionKeyDataTypes(sessionId, databaseId, tableName));
    }

    @ApiOperation(value = "syncExternalTableFiles", notes = "sync external table files")
    @PostMapping(value = "/{sessionId}/databases/{databaseName}/externalTables/"
            + "{externalTableName}/syncExternalTableFiles")
    public SuccessResponse<Boolean> syncExternalTableFiles(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName,
            @PathVariable(required = true, name = "externalTableName") String externalTableName) {
        Base64.Decoder decoder = Base64.getDecoder();
        externalTableName = new String(decoder.decode(externalTableName));
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(tableService.syncExternalTableFiles(session, databaseName, externalTableName));
    }
}
