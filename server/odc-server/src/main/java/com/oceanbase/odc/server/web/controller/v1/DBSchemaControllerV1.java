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
import org.springframework.web.bind.annotation.*;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBSchemaService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/database")
public class DBSchemaControllerV1 {

    @Autowired
    private DBSchemaService databaseService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "列出数据库实例上所有的数据库，sid示例：sid:1000-1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<List<DBDatabase>> list(@PathVariable String sid) {
        return OdcResult.ok(databaseService.listDatabases(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

    @ApiOperation(value = "detail", notes = "查询数据库详情，包括大小、编码等，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<DBDatabase> detail(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        ConnectionSession session = sessionService.nullSafeGet(i.getSid(), true);
        return OdcResult.ok(databaseService.detail(session, i.getDatabase()));
    }

}
