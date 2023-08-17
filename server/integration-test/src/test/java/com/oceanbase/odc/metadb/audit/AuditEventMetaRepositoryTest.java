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
package com.oceanbase.odc.metadb.audit;

import java.util.Collections;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.service.audit.model.QueryAuditEventMetaParams;

public class AuditEventMetaRepositoryTest extends ServiceTestEnv {
    @Autowired
    private AuditEventMetaRepository auditEventMetaRepository;

    @Before
    public void setUp() throws Exception {
        auditEventMetaRepository.deleteAll();
        auditEventMetaRepository.saveAndFlush(getEnabledAuditEventMetaEntity());
        auditEventMetaRepository.saveAndFlush(getDisabledAuditEventMetaEntity());
    }

    @After
    public void tearDown() throws Exception {
        auditEventMetaRepository.deleteAll();
    }

    @Test
    public void test_FindByMethodSignature_Success() {
        Optional<AuditEventMetaEntity> opt =
                auditEventMetaRepository.findByMethodSignature("com.oceanbase.odc.controller"
                        + ".OdcUserConfigController.update");
        Assert.assertTrue(opt.isPresent());
    }

    @Test
    public void test_FindAll_Success() {
        Page<AuditEventMetaEntity> entities =
                auditEventMetaRepository.findAll(AuditSpecs.of(getQueryAuditEventMetaParams()),
                        Pageable.unpaged());
        Assert.assertTrue(entities.getSize() == 1);

    }

    private AuditEventMetaEntity getEnabledAuditEventMetaEntity() {
        AuditEventMetaEntity entity = new AuditEventMetaEntity();
        entity.setId(1L);
        entity.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        entity.setType(AuditEventType.PERSONAL_CONFIGURATION);
        entity.setEnabled(true);
        entity.setInConnection(true);
        entity.setSidExtractExpression("#{sid}");
        entity.setMethodSignature("com.oceanbase.odc.controller.OdcUserConfigController.update");
        return entity;
    }

    private AuditEventMetaEntity getDisabledAuditEventMetaEntity() {
        AuditEventMetaEntity entity = new AuditEventMetaEntity();
        entity.setId(2L);
        entity.setAction(AuditEventAction.ADD_USER);
        entity.setType(AuditEventType.MEMBER_MANAGEMENT);
        entity.setEnabled(false);
        entity.setInConnection(false);
        entity.setSidExtractExpression(null);
        entity.setMethodSignature("com.oceanbase.odc.web.controller.IamController.createUser");
        return entity;
    }

    private QueryAuditEventMetaParams getQueryAuditEventMetaParams() {
        return QueryAuditEventMetaParams.builder()
                .actions(Collections.singletonList(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION))
                .types(Collections.singletonList(AuditEventType.PERSONAL_CONFIGURATION))
                .inConnection(true)
                .enabled(true)
                .build();
    }
}
