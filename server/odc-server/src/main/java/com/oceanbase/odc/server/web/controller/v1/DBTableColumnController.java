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
import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBTableColumnService;
import com.oceanbase.odc.service.db.model.OdcDBTableColumn;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author mogao.zj
 */
@RestController
@RequestMapping("/api/v1/column")
public class DBTableColumnController {

    @Autowired
    private DBTableColumnService columnService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查询表的所有列，sid示例：sid:1000-1:d:db1:t:tb1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<OdcDBTableColumn>> list(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        ConnectionSession session = sessionService.nullSafeGet(i.getSid());
        return OdcResult.ok(this.columnService.list(session, i.getDatabase(), i.getTable()));
    }

    @ApiOperation(value = "getCreateSql", notes = "获取新增列的sql，sid示例：sid:1000-1:d:db1:t:tb1")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody OdcDBTableColumn column) {
        String sql = this.columnService.getCreateSql(sessionService.nullSafeGet(SidUtils.getSessionId(sid)), column);
        return OdcResult.ok(ResourceSql.ofSql(sql));
    }

    @ApiOperation(value = "getDeleteSql", notes = "获取删除列的sql，sid示例：sid:1000-1:d:db1:t:tb1")
    @RequestMapping(value = "/getDeleteSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getDeleteSql(@PathVariable String sid, @RequestBody OdcDBTableColumn column) {
        String sql = this.columnService.getDeleteSql(sessionService.nullSafeGet(SidUtils.getSessionId(sid)), column);
        return OdcResult.ok(ResourceSql.ofSql(sql));
    }

}
