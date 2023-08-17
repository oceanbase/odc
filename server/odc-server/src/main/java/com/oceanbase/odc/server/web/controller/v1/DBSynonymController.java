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
import com.oceanbase.odc.service.db.DBSynonymService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;

import io.swagger.annotations.ApiOperation;

/**
 * ODC中与同义词有关的接口，controller对象
 *
 * @author yh263208
 * @date 2020-12-20 22:12
 * @since ODC_release_2.4.0
 */
@RestController
@RequestMapping("/api/v1/synonym")
public class DBSynonymController {

    @Autowired
    private DBSynonymService synonymService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看同义词的列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<DBSynonym>> list(@PathVariable String sid,
            @RequestParam(value = "synonymType", defaultValue = "COMMON") DBSynonymType synonymType) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(synonymService.list(sessionService.nullSafeGet(i.getSid()),
                i.getDatabase(), synonymType));
    }

    @ApiOperation(value = "detail", notes = "查看某一个特定的同义词细节，sid示例：sid:1000-1:syn:syn1")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<DBSynonym> detail(@PathVariable String sid,
            @RequestParam(value = "synonymType", defaultValue = "COMMON") DBSynonymType synonymType) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        DBSynonym param = new DBSynonym();
        param.setSynonymName(i.getSynonym());
        param.setOwner(i.getDatabase());
        param.setSynonymType(synonymType);
        return OdcResult.ok(synonymService.detail(sessionService.nullSafeGet(i.getSid()), param));
    }

    @ApiOperation(value = "getCreateSql", notes = "获取同义词的创建sql，sid示例：sid:1000-1:syn:syn1")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.POST)
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody DBSynonym resource) {
        return OdcResult.ok(ResourceSql.ofSql(synonymService.generateCreateSql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid)), resource)));
    }

}
