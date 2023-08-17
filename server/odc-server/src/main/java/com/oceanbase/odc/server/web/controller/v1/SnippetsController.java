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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.snippet.Snippet;
import com.oceanbase.odc.service.snippet.SnippetsService;

import io.swagger.annotations.ApiOperation;

/**
 * @author mogao.zj 用户创建语法帮助管理
 */
@RestController
@RequestMapping("/api/v1/snippets")
public class SnippetsController {

    @Autowired
    private SnippetsService snippetsService;

    @ApiOperation(value = "list", notes = "查询用户创建的snippets")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public OdcResult<List<Snippet>> list() {
        return OdcResult.ok(snippetsService.list());
    }

    @ApiOperation(value = "update", notes = "修改用户的snippets，id即记录id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.PUT)
    public OdcResult<Snippet> update(@PathVariable Long id, @RequestBody Snippet body) {
        body.setId(id);
        return OdcResult.ok(snippetsService.update(body));
    }

    @ApiOperation(value = "delete", notes = "删除snippets，id即记录id")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.DELETE)
    public OdcResult<Snippet> delete(@PathVariable Long id) {
        return OdcResult.ok(snippetsService.delete(id));
    }

    @ApiOperation(value = "create", notes = "创建snippets")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public OdcResult<Snippet> create(@RequestBody Snippet body) {
        return OdcResult.ok(snippetsService.create(body));
    }

}
