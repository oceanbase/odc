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

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.script.ScriptService;
import com.oceanbase.odc.service.script.model.Script;
import com.oceanbase.odc.service.script.model.ScriptMeta;
import com.oceanbase.odc.service.script.model.UpdateScriptReq;

/**
 * @Author: Lebie
 * @Date: 2022/3/21 下午8:40
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/script")
public class ScriptController {
    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/scripts", method = RequestMethod.GET)
    public PaginatedResponse<ScriptMeta> list(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        return Responses.paginated(scriptService.list(pageable));
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.GET)
    public SuccessResponse<Script> get(@PathVariable Long id) throws IOException {
        return Responses.success(scriptService.detail(id));
    }

    @RequestMapping(value = "/scripts/batchDelete", method = RequestMethod.POST)
    public ListResponse<ScriptMeta> batchDelete(@RequestBody List<Long> id) {
        return Responses.list(scriptService.batchDeleteScript(id));
    }

    @RequestMapping(value = "/scripts/{id}", method = RequestMethod.PUT)
    public SuccessResponse<Script> update(@PathVariable Long id, @Valid @RequestBody UpdateScriptReq req) {
        return Responses.success(scriptService.updateScript(id, req));
    }

    @RequestMapping(value = "/scripts/batchUpload", method = RequestMethod.POST)
    public ListResponse<ScriptMeta> batchUpload(@RequestParam("file") List<MultipartFile> files) {
        return Responses.list(scriptService.batchPutScript(files));
    }

    @RequestMapping(value = "/scripts/batchGetDownloadUrl", method = RequestMethod.POST)
    public ListResponse<String> getDownloadUrl(@RequestBody List<Long> ids) {
        return Responses.list(scriptService.getDownloadUrl(ids));
    }

    @RequestMapping(value = "/scripts/{id}/sync", method = RequestMethod.POST)
    public SuccessResponse<ScriptMeta> synchronizeScript(@PathVariable Long id) throws IOException {
        return Responses.success(scriptService.synchronizeScript(id));
    }
}
