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

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.logicaldatabase.model.CreateLogicalDatabaseReq;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;

/**
 * @Author: Lebie
 * @Date: 2024/5/7 15:07
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/connect/logicaldatabase")
public class LogicalDatabaseController {
    @RequestMapping(value = "/logicaldatabases", method = RequestMethod.POST)
    public SuccessResponse<Boolean> create(@RequestBody CreateLogicalDatabaseReq req) {
        throw new NotImplementedException();
    }

    @RequestMapping(value = "/logicaldatabases/{id:[\\d]+}", method = RequestMethod.POST)
    public SuccessResponse<DetailLogicalDatabaseResp> detail(@PathVariable Long id) {
        throw new NotImplementedException();
    }

    @RequestMapping(value = "/logicaldatabases/{id:[\\d]+}/logicaltables/extract", method = RequestMethod.POST)
    public SuccessResponse<Boolean> extractLogicalTables(@PathVariable Long id) {
        throw new NotImplementedException();
    }

    @RequestMapping(
            value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/logicaltables/{logicalTableId:[\\d]+}/checkStructureConsistency",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> checkLogicalTable(@PathVariable Long logicalDatabaseId,
            @PathVariable Long logicalTableId) {
        throw new NotImplementedException();
    }

    @RequestMapping(value = "/logicaldatabases/{logicalDatabaseId:[\\d]+}/logicaltables/{logicalTableId:[\\d]+}",
            method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteLogicalTable(@PathVariable Long logicalDatabaseId,
            @PathVariable Long logicalTableId) {
        throw new NotImplementedException();
    }

}
