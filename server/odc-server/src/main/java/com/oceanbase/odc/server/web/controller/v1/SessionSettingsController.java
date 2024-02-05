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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.SessionSettingsService;
import com.oceanbase.odc.service.session.model.SessionSettings;
import com.oceanbase.odc.service.state.StateName;
import com.oceanbase.odc.service.state.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v1/transaction")
public class SessionSettingsController {

    @Autowired
    private SessionSettingsService settingsService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "getTransactionInfo", notes = "查询事务信息，目前是会话的提交模式，后续可能扩展，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/getTransactionInfo/{sid}", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<SessionSettings> getSessionSettings(@PathVariable String sid) {
        return OdcResult.ok(settingsService.getSessionSettings(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

    @ApiOperation(value = "setTransactionInfo", notes = "设置事务信息，目前是会话的提交模式，后续可能扩展，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/setTransactionInfo/{sid}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<SessionSettings> setSessionSettings(@PathVariable String sid,
            @RequestBody SessionSettings settings) {
        return OdcResult.ok(settingsService.setSessionSettings(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), settings));
    }

}
