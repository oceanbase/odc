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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.db.DBMaterializedViewLogService;
import com.oceanbase.odc.service.db.model.GenerateUpdateMViewLogDDLReq;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

import io.swagger.annotations.ApiOperation;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/3 11:17
 * @since: 4.3.4
 */
@RestController
@RequestMapping("/api/v2/connect/sessions")
public class DBMaterializedViewLogController {

    @Autowired
    private DBMaterializedViewLogService dbMaterializedViewLogService;

    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "obtain the log list of the materialized view logs.")
    @RequestMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<DBObjectIdentity> list(@PathVariable String sessionId, @PathVariable String databaseName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.list(dbMaterializedViewLogService.list(session, databaseName));
    }

    @ApiOperation(value = "detail", notes = "obtain detail about materialized view log.")
    @RequestMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/{mvLogName}",
            method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBMaterializedViewLog> detail(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvLogName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewLogService.detail(session, databaseName, mvLogName));
    }

    @ApiOperation(value = "getCreateSql", notes = "obtain the sql to create the materialized view log.")
    @RequestMapping(
            value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/{baseTableName}/generateCreateDDL",
            method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<String> getCreateSql(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String baseTableName,
            @RequestBody DBMaterializedViewLog resource) {
        resource.setBaseTableName(baseTableName);
        resource.setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewLogService.generateCreateTemplate(session, resource));
    }

    @ApiOperation(value = "generateUpdateDDL", notes = "obtain the sql to update the materialized view.")
    @PostMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/generateUpdateDDL")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<String> generateUpdateDDL(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName, @RequestBody GenerateUpdateMViewLogDDLReq req) {
        req.getPrevious().setSchemaName(databaseName);
        req.getCurrent().setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewLogService.generateUpdateDDL(session, req));
    }

    @ApiOperation(value = "purge", notes = "purge expired data of materialized view log")
    @RequestMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/{mvLogName}/purge",
            method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> purge(@PathVariable String sessionId, @PathVariable String databaseName,
            @PathVariable String mvLogName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbMaterializedViewLogService.purge(session, databaseName, mvLogName));
    }

}
