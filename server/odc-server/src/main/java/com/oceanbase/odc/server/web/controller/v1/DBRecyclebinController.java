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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBRecyclebinService;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService.RecyclebinSettings;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService.UpdateRecyclebinSettingsReq;
import com.oceanbase.odc.service.db.model.DBRecycleObject;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController

@RequestMapping("/api/v1/recyclebin")
public class DBRecyclebinController {

    @Autowired
    private DBRecyclebinService recyclebinService;
    @Autowired
    private DBRecyclebinSettingsService recyclebinSettingsService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看回收站对象列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<DBRecycleObject>> list(@PathVariable String sid) {
        return OdcResult.ok(recyclebinService.list(sessionService.nullSafeGet(SidUtils.getSessionId(sid))));
    }

    @ApiOperation(value = "getDeleteSql", notes = "获取清除回收站指定对象的sql，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getDeleteSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getDeleteSql(@PathVariable String sid,
            @RequestBody List<DBRecycleObject> resource) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sid));
        return OdcResult.ok(ResourceSql.ofSql(recyclebinService.getPurgeSql(session, resource)));
    }

    @ApiOperation(value = "getPurgeAllSql", notes = "获取清除回收站所有对象的sql，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getPurgeAllSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getPurgeAllSql(@PathVariable String sid) {
        return OdcResult.ok(ResourceSql.ofSql(this.recyclebinService.getPurgeAllSql()));
    }

    @ApiOperation(value = "getUpdateSql", notes = "获取还原回收站对象的sql，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getUpdateSql/{sid:.*}", method = RequestMethod.PATCH)
    public OdcResult<ResourceSql> getUpdateSql(@PathVariable String sid,
            @RequestBody List<DBRecycleObject> resource) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sid));
        return OdcResult.ok(ResourceSql.ofSql(this.recyclebinService.getFlashbackSql(session, resource)));
    }

    @ApiOperation(value = "getExpireTime", notes = "查看回收站对象过期时间，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getExpireTime/{sid}", method = RequestMethod.GET)
    public OdcResult<String> getExpireTime(@PathVariable String sid) {
        return OdcResult
                .ok(recyclebinSettingsService.getExpireTime(sessionService.nullSafeGet(SidUtils.getSessionId(sid))));
    }

    @ApiOperation(value = "getRecyclebinSettings", notes = "查看回收站设置，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/settings/{sid}", method = RequestMethod.GET)
    public OdcResult<RecyclebinSettings> getRecyclebinSettings(@PathVariable String sid) {
        return OdcResult.ok(recyclebinSettingsService.get(sessionService.nullSafeGet(SidUtils.getSessionId(sid))));
    }

    @ApiOperation(value = "updateRecyclebinSettings", notes = "更新回收站设置，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/settings", method = RequestMethod.PATCH)
    public OdcResult<RecyclebinSettings> updateRecyclebinSettings(@RequestBody UpdateRecyclebinSettingsReq req) {
        List<ConnectionSession> sessions = req.getSessionIds().stream().map(s -> {
            ConnectionSession session = sessionService.nullSafeGet(s);
            return session;
        }).collect(Collectors.toList());
        return OdcResult.ok(recyclebinSettingsService.update(sessions, req.getSettings()));
    }

}
