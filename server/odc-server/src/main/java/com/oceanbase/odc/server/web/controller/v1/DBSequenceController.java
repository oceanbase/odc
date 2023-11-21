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
import com.oceanbase.odc.service.db.DBSequenceService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBSequence;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/sequence")
public class DBSequenceController {

    @Autowired
    private DBSequenceService sequenceService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看sequence列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<DBSequence>> list(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(sequenceService.list(sessionService.nullSafeGet(i.getSid(), true), i.getDatabase()));
    }

    @ApiOperation(value = "detail", notes = "查看sequence的详细信息，sid示例：sid:1000-1:d:db1:s:seq1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<DBSequence> detail(@PathVariable String sid) {
        // parse sid and sequence name, sid:1-1:d:database:s:s1
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(sequenceService.detail(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(), i.getSequence()));
    }

    @ApiOperation(value = "getCreateSql", notes = "获取创建sequence的sql，sid示例：sid:1000-1:d:db1:s:s1")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody DBSequence resource) {
        return OdcResult.ok(sequenceService.getCreateSql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource));
    }

    @ApiOperation(value = "getUpdateSql", notes = "获取修改sequence的sql，sid示例：sid:1000-1:d:db1:s:s1")
    @RequestMapping(value = "/getUpdateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getUpdateSql(@PathVariable String sid, @RequestBody DBSequence resource) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        resource.setName(i.getSequence());
        return OdcResult.ok(sequenceService.getUpdateSql(sessionService.nullSafeGet(i.getSid(), true), resource));
    }

}
