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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.onlineschemachange.OscService;
import com.oceanbase.odc.service.onlineschemachange.model.OscDatabaseInfo;

import io.swagger.annotations.ApiOperation;

/**
 * @author yaobin
 * @date 2023-11-06
 * @since 4.2.3
 */
@RestController
@RequestMapping("/api/v2/osc")
public class OscController {

    @Autowired
    private OscService oscService;

    @ApiOperation(value = "databaseInfo", notes = "show osc database info")
    @RequestMapping(value = "/databaseInfo/{databaseId:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<OscDatabaseInfo> getDatabase(@PathVariable Long databaseId) {
        return Responses.success(oscService.getOscDatabaseInfo(databaseId));
    }

}
