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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.ConnectionTestService;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;

import io.swagger.annotations.ApiOperation;

/**
 * 连接测试
 */
@RestController
@RequestMapping("/api/v2/connect")
public class ConnectTestController {
    @Autowired
    private ConnectionTestService testService;

    /**
     * 测试连接（注意方法改为 POST）
     * 
     * @param req
     * @return true if test success
     */
    @ApiOperation(value = "testConnection", notes = "测试连接是否能够连通")
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public SuccessResponse<ConnectionTestResult> test(@RequestBody TestConnectionReq req) {
        return Responses.single(testService.test(req));
    }

}
