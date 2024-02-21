/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.config.model.Configuration;

import io.swagger.annotations.ApiOperation;


@RestController
@RequestMapping("/api/v2")
public class ConfigurationController {

    @ApiOperation(value = "listDefaultUserConfigurations")
    @RequestMapping(value = "/defaultUserConfigurations", method = RequestMethod.GET)
    public ListResponse<Configuration> listDefaultUserConfigurations() {
        return Responses.list(Collections.emptyList());
    }

    @ApiOperation(value = "listCurrentUserConfigurations")
    @RequestMapping(value = "/users/me/configurations", method = RequestMethod.GET)
    public ListResponse<Configuration> listCurrentUserConfigurations() {
        return Responses.list(Collections.emptyList());
    }

    @ApiOperation(value = "updateCurrentUserConfigurations")
    @RequestMapping(value = "/users/me/configurations", method = RequestMethod.PUT)
    public ListResponse<Configuration> updateCurrentUserConfigurations(
            @RequestBody List<Configuration> configurations) {
        return Responses.list(Collections.emptyList());
    }

    @ApiOperation(value = "updateCurrentUserConfiguration")
    @RequestMapping(value = "/users/me/configurations/{key}", method = RequestMethod.PUT)
    public SuccessResponse<Boolean> updateCurrentUserConfiguration(@PathVariable("key") String key,
            @RequestBody Configuration configuration) {
        return Responses.success(false);
    }

}
