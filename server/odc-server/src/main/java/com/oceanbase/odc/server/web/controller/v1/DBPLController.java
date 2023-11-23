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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.model.OdcSqlExecuteResult;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBPLService;
import com.oceanbase.odc.service.db.model.CallFunctionReq;
import com.oceanbase.odc.service.db.model.CallFunctionResp;
import com.oceanbase.odc.service.db.model.CallProcedureReq;
import com.oceanbase.odc.service.db.model.CallProcedureResp;
import com.oceanbase.odc.service.db.model.DBMSOutput;
import com.oceanbase.odc.service.db.model.PLIdentity;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;

import io.swagger.annotations.ApiOperation;

/**
 * @author mogao.zj
 */
@RestController
@RequestMapping("/api/v1/pl")
public class DBPLController {
    // TODO: refactor and merge into `PLController`

    @Autowired
    private DBPLService plService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "compile", notes = "编译PL对象，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/compile/{sid}", method = RequestMethod.POST)
    public OdcResult<OdcSqlExecuteResult> compile(@PathVariable String sid, @RequestBody PLIdentity params) {
        return OdcResult.ok(this.plService.compile(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), params));
    }

    @ApiOperation(value = "parsePLNameType", notes = "解析pl的名称和类型，支持compile等操作，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/parsePLNameType/{sid}", method = RequestMethod.PUT)
    public OdcResult<PLIdentity> parseNameType(@PathVariable String sid, @RequestBody ResourceSql ddl) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sid), true);
        DBPLObjectIdentity identity = this.plService.parsePLNameType(session, ddl.getSql());
        PLIdentity plIdentity = new PLIdentity();
        plIdentity.setPlName(identity.getName());
        plIdentity.setObDbObjectType(identity.getType());
        return OdcResult.ok(plIdentity);
    }

    @ApiOperation(value = "callProcedure", notes = "调用存储过程，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/callProcedure/{sid}", method = RequestMethod.PUT)
    public OdcResult<CallProcedureResp> callProcedure(@PathVariable String sid,
            @RequestBody CallProcedureReq callProcedureReq) {
        return OdcResult.ok(this.plService.callProcedure(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), callProcedureReq));
    }

    @ApiOperation(value = "callFunction", notes = "调用函数，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/callFunction/{sid}", method = RequestMethod.PUT)
    public OdcResult<CallFunctionResp> callFunction(@PathVariable String sid,
            @RequestBody CallFunctionReq callFunctionReq) {
        return OdcResult.ok(this.plService.callFunction(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), callFunctionReq));
    }

    @ApiOperation(value = "getLine", notes = "获取pl中打印的日志信息，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getLine/{sid}", method = RequestMethod.GET)
    public OdcResult<DBMSOutput> getLine(@PathVariable String sid) {
        return OdcResult.ok(this.plService.getLine(sessionService.nullSafeGet(SidUtils.getSessionId(sid))));
    }

}
