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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBPLService;
import com.oceanbase.odc.service.db.model.CallFunctionReq;
import com.oceanbase.odc.service.db.model.CallFunctionResp;
import com.oceanbase.odc.service.db.model.CallProcedureReq;
import com.oceanbase.odc.service.db.model.CallProcedureResp;
import com.oceanbase.odc.service.db.model.CompileResult;
import com.oceanbase.odc.service.db.model.DBMSOutput;
import com.oceanbase.odc.service.db.model.PLIdentity;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v2/pl")
public class DBPLController {

    @Autowired
    private DBPLService plService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "compile", notes = "compile a pl object")
    @RequestMapping(value = "/compile/{sid}", method = RequestMethod.POST)
    public SuccessResponse<CompileResult> compile(@PathVariable String sid, @RequestBody PLIdentity params) {
        return Responses.ok(this.plService.compile(this.sessionService.nullSafeGet(
                SidUtils.getSessionId(sid), true), params));
    }

    @ApiOperation(value = "asyncCall", notes = "call a procedure")
    @RequestMapping(value = "/procedure/{sid}/asyncCall", method = RequestMethod.POST)
    public SuccessResponse<String> callProcedure(
            @PathVariable String sid, @RequestBody CallProcedureReq req) {
        return Responses.ok(this.plService.callProcedure(this.sessionService.nullSafeGet(
                SidUtils.getSessionId(sid), true), req));
    }

    @ApiOperation(value = "getResult", notes = "get the async result of a calling procedure")
    @RequestMapping(value = "/procedure/{sid}/getResult", method = RequestMethod.GET)
    public SuccessResponse<CallProcedureResp> getCallProcedureResult(@PathVariable String sid,
            @RequestParam String resultId, @RequestParam(required = false) Integer timeoutSeconds) {
        return Responses.ok(this.plService.getAsyncCallingResult(
                this.sessionService.nullSafeGet(SidUtils.getSessionId(sid), false), resultId, timeoutSeconds));
    }

    @ApiOperation(value = "asyncCall", notes = "call a function")
    @RequestMapping(value = "/function/{sid}/asyncCall", method = RequestMethod.POST)
    public SuccessResponse<String> callFunction(@PathVariable String sid, @RequestBody CallFunctionReq req) {
        return Responses.ok(this.plService.callFunction(this.sessionService.nullSafeGet(
                SidUtils.getSessionId(sid), true), req));
    }

    @ApiOperation(value = "getResult", notes = "get the async result of a calling function")
    @RequestMapping(value = "/function/{sid}/getResult", method = RequestMethod.GET)
    public SuccessResponse<CallFunctionResp> getCallFunctionResult(@PathVariable String sid,
            @RequestParam String resultId, @RequestParam(required = false) Integer timeoutSeconds) {
        return Responses.ok(this.plService.getAsyncCallingResult(
                this.sessionService.nullSafeGet(SidUtils.getSessionId(sid), false), resultId, timeoutSeconds));
    }

    @ApiOperation(value = "getLine", notes = "get dbms_output printing")
    @RequestMapping(value = "/getLine/{sid}", method = RequestMethod.GET)
    public SuccessResponse<DBMSOutput> getLine(@PathVariable String sid) {
        return Responses.ok(this.plService.getLine(sessionService.nullSafeGet(SidUtils.getSessionId(sid))));
    }

    @ApiOperation(value = "parsePLNameType", notes = "get a pl's name ant type by parsing")
    @RequestMapping(value = "/parsePLNameType/{sid}", method = RequestMethod.POST)
    public SuccessResponse<PLIdentity> getNameAndType(@PathVariable String sid, @RequestBody ResourceSql ddl) {
        DBPLObjectIdentity identity = this.plService.parsePLNameType(
                this.sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), ddl.getSql());
        return Responses.ok(PLIdentity.of(identity.getType(), identity.getName()));
    }

}
