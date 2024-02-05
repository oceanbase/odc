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

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBTableIndexService;
import com.oceanbase.odc.service.db.model.OdcDBTableIndex;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.StateName;
import com.oceanbase.odc.service.state.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/index")
public class DBTableIndexController {

    @Autowired
    private DBTableIndexService indexService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查询表的索引，sid示例：sid:1000-1:d:db1:t:tb1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<List<OdcDBTableIndex>> list(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(this.indexService.list(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(), i.getTable()));
    }

    @ApiOperation(value = "getDeleteSql", notes = "获取删除索引的sql，sid示例：sid:1000-1:d:db1:t:tb1")
    @RequestMapping(value = "/getDeleteSql/{sid:.*}", method = RequestMethod.PATCH)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<ResourceSql> getDeleteSql(@PathVariable String sid, @RequestBody OdcDBTableIndex index) {
        return OdcResult.ok(ResourceSql.ofSql(this.indexService.getDeleteSql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), index)));
    }

}
