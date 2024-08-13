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

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.logicaldatabase.model.PreviewSqlReq;
import com.oceanbase.odc.service.connection.logicaldatabase.model.PreviewSqlResp;
import com.oceanbase.odc.service.connection.logicaldatabase.task.model.SchemaChangeRecord;

/**
 * @Author: Lebie
 * @Date: 2024/8/13 15:49
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/logicaldatabase")
public class LogicalDatabaseChangeController {

    @RequestMapping(value = "/schemaChanges/{schemaChangeId:[\\d]+}/records", method = RequestMethod.GET)
    public PaginatedResponse<SchemaChangeRecord> listSchemaChangeRecords(@PathVariable Long schemaChangeId) {
        throw new NotImplementedException();
    }

    @RequestMapping(value = "/schemaChanges/{schemaChangeId:[\\d]+}/records/{recordId:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<SchemaChangeRecord> detailSchemaChangeRecord(@PathVariable Long schemaChangeId, @PathVariable Long recordId) {
        throw new NotImplementedException();
    }

    @RequestMapping(value = "/schemaChanges/{schemaChangeId:[\\d]+}/records/{recordId:[\\d]+}/skip", method = RequestMethod.POST)
    public SuccessResponse<Boolean> skipSchemaChangeRecord(@PathVariable Long schemaChangeId, @PathVariable Long recordId) {
        throw new NotImplementedException();
    }
}
