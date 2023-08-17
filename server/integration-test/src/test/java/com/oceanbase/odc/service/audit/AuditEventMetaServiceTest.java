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

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.service.audit.model.AuditEventMeta;
import com.oceanbase.odc.service.audit.model.QueryAuditEventMetaParams;

public class AuditEventMetaServiceTest extends ServiceTestEnv {

    @Autowired
    private AuditEventMetaService auditEventMetaService;

    @Before
    public void setUp() throws Exception {
        auditEventMetaService.deleteAllAuditEventMeta();
        auditEventMetaService.saveAndFlush(createAuditEventMeta());
    }

    @After
    public void tearDown() throws Exception {
        auditEventMetaService.deleteAllAuditEventMeta();
    }

    @Test
    public void testListAllAuditEventMeta_Success() {
        QueryAuditEventMetaParams params = QueryAuditEventMetaParams.builder()
                .enabled(true)
                .build();
        List<AuditEventMeta> result = auditEventMetaService.listAllAuditEventMeta(params, Pageable.unpaged());
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testFindAuditEventMetaByMethodSignature_Success() {
        Optional<AuditEventMeta> opt =
                auditEventMetaService.findAuditEventMetaByMethodSignature("controller.personal.config");
        Assert.assertTrue(opt.isPresent());
    }

    private AuditEventMeta createAuditEventMeta() {
        return AuditEventMeta.builder()
                .id(1L)
                .action(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION)
                .type(AuditEventType.PERSONAL_CONFIGURATION)
                .enabled(true)
                .inConnection(false)
                .methodSignature("controller.personal.config")
                .build();
    }
}
