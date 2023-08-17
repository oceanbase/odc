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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.model.OdcSqlExecuteResult;
import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBVariablesService;
import com.oceanbase.odc.service.db.model.OdcDBVariable;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/variables")
public class DBVariablesController {

    @Autowired
    private DBVariablesService variablesService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看所有数据库变量，session是会话变量，global是全局变量，"
            + "sid示例：sid:1000-1:d:db1:var:session 或 sid:1000-1:d:db1:var:global")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<OdcDBVariable>> list(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        String scope = i.getVariableScope();
        return OdcResult.ok(variablesService.list(sessionService.nullSafeGet(i.getSid()), scope));
    }

    @ApiOperation(value = "getUpdateSql", notes = "拼接修改session变量的sql，sid示例：sid:1000-1:d:db1:var:session")
    @RequestMapping(value = "/getUpdateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getUpdateSql(@PathVariable String sid, @RequestBody OdcDBVariable resource) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        String sql = this.variablesService.getUpdateDml(i.getVariableScope(), resource);
        return OdcResult.ok(ResourceSql.ofSql(sql));
    }

    @ApiOperation(value = "doExecute", notes = "执行sql，sid示例：sid:1000-1:d:db1:var:session")
    @RequestMapping(value = "/execute/{sid}", method = RequestMethod.PATCH)
    public OdcResult<OdcSqlExecuteResult> doExecute(@PathVariable String sid, @RequestBody ResourceSql resource) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sid));
        return OdcResult.ok(variablesService.doExecute(session, resource.getSql()));
    }

}
