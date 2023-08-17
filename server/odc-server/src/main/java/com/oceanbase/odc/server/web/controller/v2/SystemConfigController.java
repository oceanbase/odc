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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2022/4/19 下午5:21
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/config/system")
public class SystemConfigController {
    @Autowired
    private SystemConfigService systemConfigService;

    @ApiOperation(value = "query", notes = "list odc system configurations")
    @RequestMapping(value = "/configurations", method = RequestMethod.GET)
    public ListResponse<Configuration> query() {
        return Responses.list(systemConfigService.query());
    }
}
