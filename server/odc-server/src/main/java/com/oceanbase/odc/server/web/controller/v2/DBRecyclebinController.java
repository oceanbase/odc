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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBRecyclebinService;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService.RecyclebinSettings;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService.UpdateRecyclebinSettingsReq;
import com.oceanbase.odc.service.db.model.DBRecycleObject;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v2/recyclebin")
public class DBRecyclebinController {

    @Autowired
    private DBRecyclebinService recyclebinService;
    @Autowired
    private DBRecyclebinSettingsService recyclebinSettingsService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看回收站对象列表，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<List<DBRecycleObject>> list(@PathVariable String sid) {
        return Responses.ok(recyclebinService.list(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

    @ApiOperation(value = "purgeObject", notes = "purge a specific db object")
    @RequestMapping(value = "/purge/{sid:.*}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<Boolean> purgeObject(@PathVariable String sid, @RequestBody List<DBRecycleObject> resource) {
        recyclebinService.purgeObject(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource);
        return Responses.ok(Boolean.TRUE);
    }

    @ApiOperation(value = "purgeAllObjects", notes = "purge all specific db objects")
    @RequestMapping(value = "/purgeAll/{sid:.*}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<Boolean> purgeAllObjects(@PathVariable String sid) {
        recyclebinService.purgeAllObjects(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true));
        return Responses.ok(Boolean.TRUE);
    }

    @ApiOperation(value = "flashback", notes = "flaskback db objects")
    @RequestMapping(value = "/flashback/{sid:.*}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<Boolean> flashback(@PathVariable String sid, @RequestBody List<DBRecycleObject> resource) {
        recyclebinService.flashback(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), resource);
        return Responses.ok(Boolean.TRUE);
    }

    @ApiOperation(value = "getExpireTime", notes = "查看回收站对象过期时间，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getExpireTime/{sid}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<String> getExpireTime(@PathVariable String sid) {
        return Responses.ok(recyclebinSettingsService.getExpireTime(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

    @ApiOperation(value = "getRecyclebinSettings", notes = "查看回收站设置，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/settings/{sid}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<RecyclebinSettings> getRecyclebinSettings(@PathVariable String sid) {
        return Responses.ok(recyclebinSettingsService.get(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

    @ApiOperation(value = "updateRecyclebinSettings", notes = "更新回收站设置，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/settings", method = RequestMethod.PATCH)
    @StatefulRoute(stateIdExpression = "#req.sessionIds",
            stateManager = "dbRecyclebinUpdateStateManager")
    public SuccessResponse<RecyclebinSettings> updateRecyclebinSettings(@RequestBody UpdateRecyclebinSettingsReq req) {
        List<ConnectionSession> sessions = req.getSessionIds().stream().map(s -> {
            return sessionService.nullSafeGet(s, true);
        }).collect(Collectors.toList());
        return Responses.ok(recyclebinSettingsService.update(sessions, req.getSettings()));
    }

}
