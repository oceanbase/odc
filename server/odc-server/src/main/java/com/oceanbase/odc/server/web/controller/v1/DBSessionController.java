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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.session.DBSessionService;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author mogao.zj
 */
@RestController
@RequestMapping("/api/v1/dbsession")
public class DBSessionController {

    @Autowired
    private DBSessionService dbSessionService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "查看所有数据库会话，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public OdcResult<List<OdcDBSession>> list(@PathVariable String sid) {
        return OdcResult.ok(dbSessionService.listAllSessions(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

}
