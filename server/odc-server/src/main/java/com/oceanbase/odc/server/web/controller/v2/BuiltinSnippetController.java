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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.snippet.BuiltinSnippet;
import com.oceanbase.odc.service.snippet.BuiltinSnippetService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v2/snippet/")
public class BuiltinSnippetController {

    @Autowired
    private BuiltinSnippetService builtinSnippetService;

    @ApiOperation(value = "listBuiltinSnippets", notes = "查询内建的 snippets")
    @RequestMapping(value = "builtinSnippets", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<BuiltinSnippet> listBuiltinSnippets(
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        if (Objects.isNull(sessionId)) {
            return Responses.list(builtinSnippetService.listAll());
        }
        return Responses.list(builtinSnippetService.listBySession(sessionId));
    }
}
