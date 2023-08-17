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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.metadb.audit.AuditEventEntity;
import com.oceanbase.odc.service.audit.model.AuditEvent;
import com.oceanbase.odc.service.audit.util.AuditEventMapper;

public class AuditEventMapperTest {
    private AuditEventMapper mapper = AuditEventMapper.INSTANCE;

    @Test
    public void test_EntityToModel_Success() {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setConnectionName("test");
        entity.setConnectionId(1L);
        entity.setTaskId("1");
        entity.setResult(AuditEventResult.SUCCESS);
        entity.setDetail("{detail:null}");
        entity.setUserId(1L);
        entity.setOrganizationId(1L);
        entity.setServerIpAddress("0.0.0.0");
        entity.setClientIpAddress("0.0.0.0");
        entity.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        entity.setType(AuditEventType.PERSONAL_CONFIGURATION);
        entity.setUsername("account");

        AuditEvent expected = AuditEvent.builder()
                .connectionName("test")
                .connectionId(1L)
                .clientIpAddress("0.0.0.0")
                .serverIpAddress("0.0.0.0")
                .organizationId(1L)
                .taskId("1")
                .action(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION)
                .type(AuditEventType.PERSONAL_CONFIGURATION)
                .userId(1L)
                .username("account")
                .result(AuditEventResult.SUCCESS)
                .detail("{detail:null}")
                .build();

        AuditEvent actual = mapper.entityToModel(entity);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_ModelToEntity_Success() {
        AuditEvent model = AuditEvent.builder()
                .connectionName("test")
                .connectionId(1L)
                .clientIpAddress("0.0.0.0")
                .serverIpAddress("0.0.0.0")
                .organizationId(1L)
                .taskId("1")
                .action(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION)
                .type(AuditEventType.PERSONAL_CONFIGURATION)
                .userId(1L)
                .username("account")
                .result(AuditEventResult.SUCCESS)
                .detail("{detail:null}")
                .build();

        AuditEventEntity expected = new AuditEventEntity();
        expected.setConnectionName("test");
        expected.setConnectionId(1L);
        expected.setTaskId("1");
        expected.setResult(AuditEventResult.SUCCESS);
        expected.setDetail("{detail:null}");
        expected.setUserId(1L);
        expected.setOrganizationId(1L);
        expected.setServerIpAddress("0.0.0.0");
        expected.setClientIpAddress("0.0.0.0");
        expected.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        expected.setType(AuditEventType.PERSONAL_CONFIGURATION);
        expected.setUsername("account");

        AuditEventEntity actual = mapper.modelToEntity(model);
        Assert.assertEquals(expected, actual);
    }

}
