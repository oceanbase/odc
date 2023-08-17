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
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.service.audit.AuditEventMetaService;
import com.oceanbase.odc.service.audit.AuditEventService;
import com.oceanbase.odc.service.audit.model.AuditEvent;
import com.oceanbase.odc.service.audit.model.AuditEventExportReq;
import com.oceanbase.odc.service.audit.model.AuditEventMeta;
import com.oceanbase.odc.service.audit.model.QueryAuditEventMetaParams;
import com.oceanbase.odc.service.audit.model.QueryAuditEventParams;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;

/**
 * @Author: Lebie
 * @Date: 2022/1/24 下午3:36
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/audit")
public class AuditController {
    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventMetaService auditEventMetaService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @RequestMapping(value = "/events", method = RequestMethod.GET)
    public PaginatedResponse<AuditEvent> listAllAuditEvents(
            @RequestParam(required = false, name = "type") List<AuditEventType> eventTypes,
            @RequestParam(required = false, name = "action") List<AuditEventAction> eventActions,
            @RequestParam(required = false, name = "fuzzyClientIPAddress") String fuzzyClientIPAddress,
            @RequestParam(required = false, name = "fuzzyConnectionName") String fuzzyConnectionName,
            @RequestParam(required = false, name = "result") List<AuditEventResult> results,
            @RequestParam(required = false, name = "fuzzyUsername") String fuzzyUsername,
            @RequestParam(required = false,
                    name = "startTime") Date startTime,
            @RequestParam(required = false,
                    name = "endTime") Date endTime,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"organizationId", "startTime"},
                    direction = Direction.DESC) Pageable pageable) {
        QueryAuditEventParams params = QueryAuditEventParams.builder()
                .types(eventTypes)
                .actions(eventActions)
                .fuzzyClientIpAddress(fuzzyClientIPAddress)
                .fuzzyConnectionName(fuzzyConnectionName)
                .results(results)
                .fuzzyUsername(fuzzyUsername)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        if (Objects.isNull(params.getFuzzyUsername())) {
            return Responses.paginated(auditEventService.listPersonalAuditEvent(params, pageable));
        } else {
            return Responses.paginated(auditEventService.listOrganizationAuditEvents(params, pageable));
        }
    }

    @RequestMapping(value = "/events/export", method = RequestMethod.POST)
    public SuccessResponse<String> exportAuditEvents(@RequestBody AuditEventExportReq req) throws IOException {
        return Responses.single(auditEventService.export(req));
    }

    @RequestMapping(value = "/events/{id}", method = RequestMethod.GET)
    public SuccessResponse<AuditEvent> detail(@PathVariable Long id) {
        return Responses.success(auditEventService.findById(id));
    }

    @RequestMapping(value = "/eventMeta", method = RequestMethod.GET)
    public ListResponse<AuditEventMeta> listAuditEventMeta() {
        QueryAuditEventMetaParams params = QueryAuditEventMetaParams.builder()
                .enabled(true)
                .build();
        return Responses.list(auditEventMetaService.listAllAuditEventMeta(params, Pageable.unpaged()));
    }

    @RequestMapping(value = "/events/stats", method = RequestMethod.GET)
    public SuccessResponse<Stats> stats(@RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime) {
        QueryAuditEventParams params = QueryAuditEventParams.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return Responses
                .success(auditEventService.stats(params));
    }

    @RequestMapping(value = "/events/connections", method = RequestMethod.GET)
    public ListResponse<ConnectionConfig> listRelatedConnections() {
        return Responses.list(auditEventService.relatedConnections());
    }

    @RequestMapping(value = "/events/users", method = RequestMethod.GET)
    public ListResponse<User> listRelatedUsers() {
        return Responses.list(auditEventService.relatedUsers());
    }

}
