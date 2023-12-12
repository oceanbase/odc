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

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBTriggerService;
import com.oceanbase.odc.service.db.model.CompileResult;
import com.oceanbase.odc.service.db.model.DBTriggerReq;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;

import io.swagger.annotations.ApiOperation;

/**
 * ODC中与触发器有关的接口，controller对象
 *
 * @author yh263208
 * @date 2020-12-04 19:26
 * @since ODC_release_2.4.0
 */
@RestController
@RequestMapping("/api/v1/trigger")
public class DBTriggerController {

    @Autowired
    private DBTriggerService triggerService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看触发器的列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<DBTrigger>> list(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(triggerService.list(sessionService.nullSafeGet(i.getSid(), true), i.getDatabase()));
    }

    @ApiOperation(value = "detail", notes = "查看某一个特定的触发器细节，sid示例：sid:1000-1:tr:tr1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<DBTrigger> detail(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(triggerService.detail(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(), i.getTrigger()));
    }

    @ApiOperation(value = "update", notes = "修改触发器状态，sid示例：sid:1000-1:tr:tr1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<DBTrigger> modify(@PathVariable String sid, @RequestBody DBTriggerReq body) {
        return OdcResult.ok(triggerService.alter(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), body));
    }

    @ApiOperation(value = "compile", notes = "编译一个特定的触发器，sid示例：sid:1000-1:tr:tr1")
    @RequestMapping(value = "/compile/{sid:.*}", method = RequestMethod.POST)
    public OdcResult<CompileResult> compile(@PathVariable String sid) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @ApiOperation(value = "getCreateSql", notes = "获取触发器的创建sql，sid示例：sid:1000-1:tr:tr1")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.POST)
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody DBTriggerReq resource) {
        return OdcResult.ok(ResourceSql.ofSql(triggerService.generateCreateSql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource)));
    }

}
