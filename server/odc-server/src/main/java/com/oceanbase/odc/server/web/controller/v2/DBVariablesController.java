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

import com.oceanbase.odc.service.common.model.ResourceIdentifier;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBVariablesService;
import com.oceanbase.odc.service.db.model.OdcDBVariable;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController
@RequestMapping("/api/v2/variables")
public class DBVariablesController {

    @Autowired
    private DBVariablesService variablesService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "list all variables")
    @RequestMapping(value = "/list/{sid:.*}", method = RequestMethod.GET)
    public ListResponse<OdcDBVariable> list(@PathVariable String sid) {
        ResourceIdentifier i = ResourceIDParser.parse(sid);
        return Responses.list(this.variablesService.list(
                this.sessionService.nullSafeGet(i.getSid(), true), i.getVariableScope()));
    }

    @ApiOperation(value = "update", notes = "update variable")
    @RequestMapping(value = "/update/{sid:.*}", method = RequestMethod.POST)
    public SuccessResponse<Boolean> getUpdateSql(@PathVariable String sid, @RequestBody OdcDBVariable var) {
        var.setVariableScope(ResourceIDParser.parse(sid).getVariableScope());
        return Responses.ok(this.variablesService.update(
                this.sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), var));
    }

}
