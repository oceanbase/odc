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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBPLService;
import com.oceanbase.odc.service.db.model.BatchCompileResp;
import com.oceanbase.odc.service.db.model.StartBatchCompileReq;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * @author wenniu.ly
 * @date 2022/6/10
 */

@RestController
@RequestMapping("/api/v2/connect/sessions")
public class PLController {

    @Autowired
    private DBPLService plService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "start batchCompile", notes = "发起一次批量编译")
    @RequestMapping(value = {"/{sessionId}/databases/{databaseName}/batchCompilations",
            "/{sessionId}/currentDatabase/batchCompilations"}, method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<String> startBatchCompile(@PathVariable String sessionId,
            @PathVariable(required = false) String databaseName, @RequestBody StartBatchCompileReq req) {
        String sid = SidUtils.getSessionId(sessionId);
        return Responses.success(plService.startBatchCompile(sessionService.nullSafeGet(sid, true), databaseName, req));
    }

    @ApiOperation(value = "end batchCompile", notes = "终止批量编译")
    @RequestMapping(value = {"/{sessionId}/databases/{databaseName}/batchCompilations/{id}",
            "/{sessionId}/currentDatabase/batchCompilations/{id}"}, method = RequestMethod.DELETE)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#id")
    public SuccessResponse<Boolean> endBatchCompile(@PathVariable String id) {
        return Responses.success(plService.endBatchCompile(id));
    }

    @ApiOperation(value = "get result for batchCompile", notes = "获取批量编译PL对象的结果")
    @RequestMapping(value = {"/{sessionId}/databases/{databaseName}/batchCompilations/{id}",
            "/{sessionId}/currentDatabase/batchCompilations/{id}"}, method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#id")
    public SuccessResponse<BatchCompileResp> getBatchCompileResult(@PathVariable String id) {
        return Responses.success(plService.getBatchCompileResult(id));
    }
}
