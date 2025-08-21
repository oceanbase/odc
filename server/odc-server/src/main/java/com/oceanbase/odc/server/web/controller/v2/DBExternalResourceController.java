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

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;
import com.oceanbase.tools.dbbrowser.model.DBExternalResource;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

import io.swagger.annotations.ApiOperation;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/8/21 10:08
 * @since: 4.4.1
 */
@RestController
@RequestMapping("/api/v2/connect/sessions")
@Validated
public class DBExternalResourceController {

    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "list", notes = "obtain a list of all external resources under the specified database.")
    @GetMapping(value = "/{sessionId}/databases/{databaseName}/externalResources")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<DBObjectIdentity> list(@PathVariable String sessionId,
            @PathVariable Long databaseName)
            throws SQLException, InterruptedException {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        throw new NotImplementedException();
    }

    @ApiOperation(value = "detail", notes = "obtain details for the specified external resource.")
    @GetMapping(value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBExternalResource> detail(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        throw new NotImplementedException();
    }

    @ApiOperation(value = "uploadExternalResource", notes = "upload External Resource to the business database.")
    @PostMapping(
            value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}/uploadExternalResource")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> uploadExternalResource(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName,
            @RequestParam("file") MultipartFile file) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        throw new NotImplementedException();
    }

    @ApiOperation(value = "downloadExternalResource", notes = "download ExternalResource from the business database.")
    @PostMapping(
            value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}/downloadExternalResource")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ResponseEntity<InputStreamResource> downloadExternalResource(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        throw new NotImplementedException();
    }

    @ApiOperation(value = "deleteExternalResource", notes = "delete External Resource in the business database.")
    @DeleteMapping(
            value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}/deleteExternalResource")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> deleteExternalResource(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName) {
        ConnectionSession session = sessionService.nullSafeGet(sessionId, true);
        throw new NotImplementedException();
    }

}
