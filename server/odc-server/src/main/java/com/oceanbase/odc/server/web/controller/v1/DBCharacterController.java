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

import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBCharsetService;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController

@RequestMapping("/api/v1/character")
public class DBCharacterController {

    @Autowired
    private DBCharsetService charsetService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "listCharset", notes = "查看数据库支持的charset，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/charset/list/{sid}", method = RequestMethod.GET)
    public OdcResult<List<String>> listCharset(@PathVariable String sid) {
        return OdcResult.ok(charsetService.listCharset(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

    @ApiOperation(value = "listCollation", notes = "查看数据库支持的collation，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/collation/list/{sid}", method = RequestMethod.GET)
    public OdcResult<List<String>> listCollation(@PathVariable String sid) {
        return OdcResult.ok(charsetService.listCollation(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true)));
    }

}
