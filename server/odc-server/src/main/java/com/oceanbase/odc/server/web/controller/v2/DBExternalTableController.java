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

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.db.DBExternalTableService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/8/19 20:38
 * @since: 4.3.3
 */
@RestController
@RequestMapping("api/v2/connect/sessions")
public class DBExternalTableController {

    @Autowired
    private DBExternalTableService dbExternalTableService;

    @Autowired
    private ConnectSessionService sessionService;

    @GetMapping(value = "/{sessionId}/databases/{databaseName}/externalTables/{tableName}")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBTable> getTable(@PathVariable String sessionId,
        @PathVariable(required = false) String databaseName,
        @PathVariable String tableName) {
        Base64.Decoder decoder = Base64.getDecoder();
        tableName = new String(decoder.decode(tableName));
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        return Responses.success(dbExternalTableService.getTable(session, databaseName, tableName));
    }


}
