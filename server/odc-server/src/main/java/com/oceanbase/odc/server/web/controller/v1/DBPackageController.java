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
import com.oceanbase.odc.service.db.DBPackageService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBPackage;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/package")
public class DBPackageController {

    @Autowired
    private DBPackageService packageService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看pacakge列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<DBPackage>> list(@PathVariable String sid) {
        // sid:1-1:d:database
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(packageService.list(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase()));
    }

    @ApiOperation(value = "detail", notes = "查看package的详细信息，sid示例：sid:1000-1:d:db1:pkg:pkg_test")
    @RequestMapping(value = "/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<DBPackage> detail(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return OdcResult.ok(packageService.detail(
                sessionService.nullSafeGet(i.getSid(), true), i.getDatabase(), i.getPkg()));
    }


    @ApiOperation(value = "getCreateSql", notes = "获取创建package的sql，sid示例：sid:1000-1:d:db1:pkg:pkg_test")
    @RequestMapping(value = "/getCreateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getCreateSql(@PathVariable String sid, @RequestBody DBPackage resource) {
        return OdcResult.ok(packageService.getCreateSql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource));
    }

}
