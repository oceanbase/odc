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
import java.util.Date;
import java.util.List;
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
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.service.audit.model.QueryAuditEventParams;

public class AuditEventRepositoryTest extends ServiceTestEnv {
    @Autowired
    private AuditEventRepository auditEventRepository;

    private static final Date startTime = new Date(1000L);
    private static final Date endTime = new Date(2000L);
    private static final Long userId = 1L;
    private static final Long organizationId = 1L;
    private static final String username = "lebie";
    private static final Long connectionId = 1L;
    private static final String connectionName = "fake_connection";
    private static final String serverIpAddress = "0.0.0.0";
    private static final String clientIpAddress = "127.0.0.1";
    private static final String taskId = "1";


    @Before
    public void setUp() throws Exception {
        auditEventRepository.deleteAll();
        auditEventRepository.saveAndFlush(getEntityWithoutTaskId());
        auditEventRepository.saveAndFlush(getEntityWithTaskId());
    }

    @After
    public void tearDown() throws Exception {
        auditEventRepository.deleteAll();
    }

    @Test
    public void testFindFirstByTaskIdAndResult_Success() {
        Optional<AuditEventEntity> opt =
                auditEventRepository.findFirstByTaskIdAndResult(taskId, AuditEventResult.FAILED);
        Assert.assertTrue(opt.isPresent());
    }

    @Test
    public void testFindAllOperatorsByOrganizationId_Success() {
        List<AuditEventOperator> operators =
                auditEventRepository.findAllOperatorsByOrganizationId(organizationId, startTime, endTime);
        Assert.assertTrue(operators.size() == 1 && operators.get(0).getUserId().equals(userId));
    }

    @Test
    public void testFindAll_Success() {
        Page<AuditEventEntity> entities =
                auditEventRepository.findAll(AuditSpecs.of(getQueryAuditEventParams()), Pageable.unpaged());
        Assert.assertTrue(entities.getSize() == 1);
    }

    private QueryAuditEventParams getQueryAuditEventParams() {
        return QueryAuditEventParams.builder()
                .actions(Collections.singletonList(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION))
                .types(Collections.singletonList(AuditEventType.PERSONAL_CONFIGURATION))
                .fuzzyClientIpAddress(".0")
                .fuzzyConnectionName("fake")
                .fuzzyUsername("le")
                .results(Collections.singletonList(AuditEventResult.FAILED))
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    private AuditEventEntity getEntityWithoutTaskId() {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setUsername(username);
        entity.setClientIpAddress(clientIpAddress);
        entity.setType(AuditEventType.PERSONAL_CONFIGURATION);
        entity.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        entity.setServerIpAddress(serverIpAddress);
        entity.setOrganizationId(organizationId);
        entity.setUserId(userId);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setDetail("details");
        entity.setResult(AuditEventResult.SUCCESS);
        return entity;
    }

    private AuditEventEntity getEntityWithTaskId() {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setUsername(username);
        entity.setClientIpAddress(clientIpAddress);
        entity.setType(AuditEventType.PERSONAL_CONFIGURATION);
        entity.setAction(AuditEventAction.UPDATE_PERSONAL_CONFIGURATION);
        entity.setServerIpAddress(serverIpAddress);
        entity.setOrganizationId(organizationId);
        entity.setUserId(userId);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setDetail("details");
        entity.setConnectionId(connectionId);
        entity.setConnectionName(connectionName);
        entity.setResult(AuditEventResult.FAILED);
        entity.setTaskId(taskId);
        return entity;
    }

}
