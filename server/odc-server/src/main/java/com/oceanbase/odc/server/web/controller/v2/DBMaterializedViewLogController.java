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

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.db.model.DBViewResponse;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBView;

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

    @ApiOperation(value = "list", notes = "obtain the log list of the materialized view logs.")
    @RequestMapping(value = "/{sessionId}/databases/{databaseId}/materializedViewLogs", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<List<DBViewResponse>> list(@PathVariable String sessionId, @PathVariable Long databaseId) {
        throw new NotImplementedException("not implemented");
    }

    @ApiOperation(value = "detail", notes = "obtain detail about materialized view log.")
    @RequestMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/{mvLogName}",
            method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBViewResponse> detail(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvLogName) {
        throw new NotImplementedException("not implemented");
    }

    @ApiOperation(value = "getCreateSql", notes = "obtain the sql to create the materialized view log.")
    @RequestMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/{mvLogName}/generateCreateDDL",
            method = RequestMethod.PATCH)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<ResourceSql> getCreateSql(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String mvLogName,
            @RequestBody DBView resource) {
        throw new NotImplementedException("not implemented");
    }

    @ApiOperation(value = "purge", notes = "purge expired data of materialized view log")
    @RequestMapping(value = "/{sessionId}/databases/{databaseName}/materializedViewLogs/{mvLogName}/purge",
            method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> purge(@PathVariable String sessionId, @PathVariable String databaseName,
            @PathVariable String mvLogName) {
        throw new NotImplementedException("not implemented");
    }

}
