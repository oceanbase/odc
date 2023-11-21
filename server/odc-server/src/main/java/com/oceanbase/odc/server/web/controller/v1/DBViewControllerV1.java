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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBViewService;
import com.oceanbase.odc.service.db.model.AllTablesAndViews;
import com.oceanbase.odc.service.db.model.DBViewResponse;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBView;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController

@RequestMapping("/api/v1/view")
public class DBViewControllerV1 {

    @Autowired
    private DBViewService viewService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看视图列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<DBView>> list(@PathVariable String sid) {
        // sid:1-1:d:database
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(viewService.list(sessionService.nullSafeGet(i.getSid(), true), i.getDatabase()));
    }

    @ApiOperation(value = "detail", notes = "查看视图的详细信息，sid示例：sid:1000-1:d:db1:v:v1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<DBViewResponse> detail(@PathVariable String sid) {
        // parse sid and view name, sid:1-1:d:database:v:v1
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(viewService.detail(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(), i.getView()));
    }

    @ApiOperation(value = "listAll", notes = "查看视图和表列表，sid示例：sid:1000-1:d:db1:v:v1")
    @RequestMapping(value = "/listAll/{sid}", method = RequestMethod.GET)
    public OdcResult<AllTablesAndViews> listAll(@PathVariable String sid, @RequestParam String name) {
        return OdcResult.ok(viewService.listAllTableAndView(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), name));
    }

    @ApiOperation(value = "getCreateSql", notes = "获取创建视图的sql，sid示例：sid:1000-1:d:db1:v:v1")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody DBView resource) {
        return OdcResult.ok(ResourceSql.ofSql(viewService.getCreateSql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource)));
    }

}
