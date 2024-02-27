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

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.CreateEnvironmentReq;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.environment.model.QueryEnvironmentParam;
import com.oceanbase.odc.service.collaboration.environment.model.UpdateEnvironmentReq;
import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/4/12 16:34
 * @Description: []
 */

@RestController(value = "odcEnvironmentController")
@RequestMapping("/api/v2/collaboration")
public class EnvironmentController {
    @Autowired
    private EnvironmentService environmentService;

    @ApiOperation(value = "listEnvironments", notes = "List all environments")
    @RequestMapping(value = "/environments", method = RequestMethod.GET)
    public ListResponse<Environment> listEnvironments(
            @RequestParam(required = false, name = "enabled") Boolean enabled) {
        QueryEnvironmentParam param = QueryEnvironmentParam.builder().enabled(enabled).build();
        return Responses.list(environmentService.list(param));
    }

    @ApiOperation(value = "getEnvironments", notes = "Detail an environment")
    @RequestMapping(value = "/environments/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Environment> getEnvironment(@PathVariable Long id) {
        return Responses.success(environmentService.detail(id));
    }

    @ApiOperation(value = "createEnvironment", notes = "Create an environment")
    @RequestMapping(value = "/environments", method = RequestMethod.POST)
    public SuccessResponse<Environment> createEnvironment(@RequestBody @Valid CreateEnvironmentReq req) {
        return Responses.success(environmentService.create(req));
    }

    @ApiOperation(value = "updateEnvironment", notes = "Update an environment")
    @RequestMapping(value = "/environments/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<Environment> updateEnvironment(@PathVariable Long id,
            @RequestBody @Valid UpdateEnvironmentReq req) {
        return Responses.success(environmentService.update(id, req));
    }

    @ApiOperation(value = "deleteEnvironment", notes = "Delete an environment")
    @RequestMapping(value = "/environments/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<Environment> deleteEnvironment(@PathVariable Long id) {
        return Responses.success(environmentService.delete(id));
    }

    @ApiOperation(value = "setEnvironmentEnabled", notes = "Set an environment enabled/disabled")
    @RequestMapping(value = "/environments/{id:[\\d]+}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<Boolean> setEnabled(@PathVariable Long id, @RequestBody @Valid SetEnabledReq req) {
        return Responses.success(environmentService.setEnabled(id, req));
    }
}
