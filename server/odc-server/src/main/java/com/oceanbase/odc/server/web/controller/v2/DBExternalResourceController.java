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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.db.DBExternalResourceService;
import com.oceanbase.odc.service.db.model.DBExternalResourceReq;
import com.oceanbase.odc.service.db.model.DBExternalResourceUploadReq;
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
public class DBExternalResourceController {

    @Autowired
    private ConnectSessionService sessionService;

    @Autowired
    private DBExternalResourceService externalResourceService;

    @ApiOperation(value = "list", notes = "obtain a list of all external resources under the specified database.")
    @GetMapping(value = "/{sessionId}/databases/{databaseName}/externalResources")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<DBObjectIdentity> list(@PathVariable String sessionId,
            @PathVariable String databaseName) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sessionId), true);
        return Responses.list(externalResourceService.list(session, databaseName));
    }

    @ApiOperation(value = "detail", notes = "obtain details for the specified external resource.")
    @GetMapping(value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBExternalResource> detail(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName,
            @RequestBody DBExternalResourceReq req) throws IOException {
        req.setName(resourceName);
        req.setSchemaName(databaseName);
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sessionId), true);
        return Responses.success(externalResourceService.detail(session, req));
    }

    @ApiOperation(value = "uploadExternalResource", notes = "upload External Resource to the business database.")
    @PostMapping(
            value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}/upload")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> uploadExternalResource(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName,
            @RequestPart("req") DBExternalResourceUploadReq req,
            @RequestPart("file") MultipartFile file) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sessionId), true);
        req.setSchemaName(databaseName);
        req.setName(resourceName);
        return Responses.success(externalResourceService.upload(session, req, file));
    }

    @ApiOperation(value = "downloadExternalResource", notes = "download ExternalResource from the business database.")
    @PostMapping(
            value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}/download")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ResponseEntity<InputStreamResource> downloadExternalResource(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName) throws IOException {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sessionId), true);
        return externalResourceService.download(session, databaseName, resourceName);
    }

    @ApiOperation(value = "deleteExternalResource", notes = "delete External Resource in the business database.")
    @DeleteMapping(
            value = "/{sessionId}/databases/{databaseName}/externalResources/{resourceName}")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> deleteExternalResource(@PathVariable String sessionId,
            @PathVariable String databaseName,
            @PathVariable String resourceName) {
        ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sessionId), true);
        return Responses.success(externalResourceService.drop(session, databaseName, resourceName));
    }

}
