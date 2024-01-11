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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
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
    public ListResponse<Environment> listEnvironments() {
        return Responses.list(environmentService.list());
    }

    @ApiOperation(value = "getEnvironments", notes = "Detail an environment")
    @RequestMapping(value = "/environments/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Environment> getEnvironment(@PathVariable Long id) {
        return Responses.success(environmentService.detail(id));
    }

    @ApiOperation(value = "createEnvironment", notes = "Create an environment")
    @RequestMapping(value = "/environments", method = RequestMethod.POST)
    public SuccessResponse<Environment> createEnvironment(@RequestBody Environment environment) {
        throw new UnsupportedOperationException();
    }

    @ApiOperation(value = "deleteEnvironment", notes = "Delete an environment")
    @RequestMapping(value = "/environments", method = RequestMethod.DELETE)
    public SuccessResponse<Environment> deleteEnvironment() {
        throw new UnsupportedOperationException();
    }



}
