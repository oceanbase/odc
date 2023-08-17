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
package com.oceanbase.odc.service.audit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.audit.model.AuditEvent;
import com.oceanbase.odc.service.audit.model.AuditEventExportReq;
import com.oceanbase.odc.service.audit.model.QueryAuditEventParams;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

public class AuditEventServiceTest extends ServiceTestEnv {

    private static final Date searchStartTime = new Date(1000L);
    private static final Date searchEndTime = new Date(2000L);
    private static final Date auditEventStartTime = new Date(1500L);
    private static final Long systemAdminUserId = 1L;
    private static final Long nonSystemAdminUserId = 2L;
    private static final String nonSystemAdminUsername = "lebie";
    private static final String systemAdminUsername = "lebie_admin";
    private static final Long connectionId = 1L;
    private static final Long organizationId = 1L;
    private static final String connectionName = "fake_connection";
    private static final String serverIpAddress = "0.0.0.0";
    private static final String clientIpAddress = "127.0.0.1";

    @Autowired
    private AuditEventService auditEventService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private SecurityManager securityManager;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        auditEventService.deleteAllAuditEvent();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(organizationId);
        Mockito.when(securityManager.isPermitted(Mockito.anyCollection())).thenReturn(false);
        initAuditEvent();
    }

    @After
    public void tearDown() throws Exception {
        auditEventService.deleteAllAuditEvent();
        FileUtils.deleteQuietly(new File("data/AUDIT"));
    }

    @Test
    public void testListAllAuditEvent_NonSystemAdminUser_Success() {
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(nonSystemAdminUserId);
        Mockito.when(authenticationFacade.currentUsername()).thenReturn(nonSystemAdminUsername);
        Page<AuditEvent> result =
                auditEventService.listPersonalAuditEvent(createQueryParametersWithoutUserIds(), Pageable.unpaged());
        Assert.assertTrue(result.getSize() == 1);
    }

    @Test
    public void testListAllAuditEvent_SystemAdminUser_Success() {
        Page<AuditEvent> result =
                auditEventService.listOrganizationAuditEvents(createQueryParametersWithUserIds(), Pageable.unpaged());
        Assert.assertTrue(result.getSize() == 2);
    }

    @Test
    public void testStats_Success() {
        Stats stats = auditEventService.stats(createQueryParametersWithoutUserIds());
        Assert.assertEquals(2, stats.get("userIds").getDistinct().size());
    }

    @Test
    public void testFindById_Success() {
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(nonSystemAdminUserId);
        Page<AuditEvent> result =
                auditEventService.listPersonalAuditEvent(createQueryParametersWithoutUserIds(), Pageable.unpaged());
        Optional<AuditEvent> opt = result.stream().findFirst();
        AuditEvent auditEventInDb = auditEventService.findById(opt.get().getId());
        Assert.assertEquals(nonSystemAdminUserId, auditEventInDb.getUserId());
    }

    @Test
    public void testFindById_OrganizationNotMatch_Failed() {
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(nonSystemAdminUserId);
        Page<AuditEvent> result =
                auditEventService.listPersonalAuditEvent(createQueryParametersWithoutUserIds(), Pageable.unpaged());
        Optional<AuditEvent> opt = result.stream().findFirst();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1000L);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1000L);
        thrown.expect(NotFoundException.class);
        auditEventService.findById(opt.get().getId());
    }

    @Test
    public void testExportCSV_Success() throws IOException {
        AuditEventExportReq req = new AuditEventExportReq();
        req.setFormat(DataTransferFormat.CSV);
        String downloadUrl = auditEventService.export(req);
        Assert.assertTrue(downloadUrl.startsWith("/api/v2/objectstorage/AUDIT/files/") && downloadUrl.endsWith(".csv"));
    }

    @Test
    public void testExportExcel_Success() throws IOException {
        AuditEventExportReq req = new AuditEventExportReq();
        req.setFormat(DataTransferFormat.EXCEL);
        String downloadUrl = auditEventService.export(req);
        Assert.assertTrue(
                downloadUrl.startsWith("/api/v2/objectstorage/AUDIT/files/") && downloadUrl.endsWith(".xlsx"));
    }

    private QueryAuditEventParams createQueryParametersWithoutUserIds() {
        return QueryAuditEventParams.builder()
                .actions(Arrays.asList(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION))
                .types(Arrays.asList(AuditEventType.PERSONAL_CONFIGURATION))
                .fuzzyClientIpAddress(".0")
                .fuzzyConnectionName("fake")
                .organizationId(organizationId)
                .results(Arrays.asList(AuditEventResult.FAILED))
                .startTime(searchStartTime)
                .endTime(searchEndTime)
                .build();
    }

    private QueryAuditEventParams createQueryParametersWithUserIds() {
        QueryAuditEventParams params = createQueryParametersWithoutUserIds();
        params.setFuzzyUsername("le");
        return params;
    }

    private void initAuditEvent() {
        AuditEvent auditEvent1 = createAuditEvent();
        auditEvent1.setUserId(nonSystemAdminUserId);
        auditEvent1.setUsername(nonSystemAdminUsername);
        auditEventService.record(auditEvent1);

        AuditEvent auditEvent2 = createAuditEvent();
        auditEvent2.setUserId(systemAdminUserId);
        auditEvent2.setUsername(systemAdminUsername);
        auditEventService.record(auditEvent2);
    }

    private AuditEvent createAuditEvent() {
        return AuditEvent.builder()
                .action(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION)
                .clientIpAddress(clientIpAddress)
                .connectionId(connectionId)
                .connectionName(connectionName)
                .detail("detail")
                .organizationId(organizationId)
                .result(AuditEventResult.FAILED)
                .serverIpAddress(serverIpAddress)
                .type(AuditEventType.PERSONAL_CONFIGURATION)
                .startTime(auditEventStartTime)
                .build();
    }
}
