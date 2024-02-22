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

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.config.model.Configuration;

import io.swagger.annotations.ApiOperation;

/**
 * Personal config controller, which is used to config delimiter, auto_commit, etc.
 *
 * @author yh263208
 * @date 2021-05-19 14:27
 * @since ODC_release_2.4.2
 */
@RestController
@RequestMapping("/api/v1/users")
@Deprecated
public class UserConfigController {
    /**
     * Get all User configs
     */
    @ApiOperation(value = "query", notes = "Get all user Configs")
    @RequestMapping(value = "/me/configurations", method = RequestMethod.GET)
    public OdcResult<List<Configuration>> query() {
        throw new UnsupportedException("please use /api/v2/users/me/configurations instead");
    }

    /**
     * Update user config
     */
    @ApiOperation(value = "update", notes = "Update user Config")
    @RequestMapping(value = "/me/configurations", method = RequestMethod.PATCH)
    public OdcResult<List<Configuration>> update(@RequestBody List<Configuration> configDTOList) {
        throw new UnsupportedException("please use /api/v2/users/me/configurations instead");
    }

}
