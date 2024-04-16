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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.db.schema.DBSchemaIndexService;
import com.oceanbase.odc.service.db.schema.model.QueryDBObjectParams;
import com.oceanbase.odc.service.db.schema.model.QueryDBObjectResp;
import com.oceanbase.odc.service.db.schema.model.SyncDBObjectReq;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2024/3/28 10:52
 */
@RestController
@RequestMapping("api/v2/database/object")
public class DBObjectIndexController {

    @Autowired
    private DBSchemaIndexService dbSchemaIndexService;

    @ApiOperation(value = "listDatabaseObjects", notes = "List database objects")
    @RequestMapping(value = "/objects", method = RequestMethod.GET)
    public SuccessResponse<QueryDBObjectResp> listDatabaseObjects(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "datasourceId", required = false) Long datasourceId,
            @RequestParam(value = "databaseIds", required = false) List<Long> databaseIds,
            @RequestParam(value = "types", required = false) List<DBObjectType> types,
            @RequestParam(value = "searchKey") String searchKey) {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(projectId)
                .datasourceId(datasourceId)
                .databaseIds(databaseIds)
                .types(types)
                .searchKey(searchKey).build();
        return Responses.success(dbSchemaIndexService.listDatabaseObjects(params));
    }

    @ApiOperation(value = "syncDatabaseObjects", notes = "Sync database objects")
    @RequestMapping(value = "/sync", method = RequestMethod.POST)
    public SuccessResponse<Boolean> syncDatabaseObjects(@RequestBody SyncDBObjectReq req) {
        return Responses.success(dbSchemaIndexService.syncDatabaseObjects(req));
    }


}
