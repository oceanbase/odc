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
package com.oceanbase.odc.service.integration;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.service.flow.ApprovalPermissionService;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.integration.client.ApprovalClient;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.ApprovalStatus;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/4/13 11:40
 */
@Slf4j
@Configuration
public class ApprovalSynchronizer {
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private ApprovalClient approvalClient;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private ApprovalPermissionService approvalPermissionService;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;
    @Autowired
    private RuntimeService runtimeService;

    private static final String LOCK_NAME = "APPROVAL_INTEGRATION_SCHEDULE";
    private static final long LOCK_TIMEOUT_SECONDS = 3;
    private static final long SYNCHRONIZE_INTERVAL_SECONDS = 5;

    @Scheduled(initialDelay = 10000L, fixedDelay = SYNCHRONIZE_INTERVAL_SECONDS * 1000L)
    public void run() {
        try {
            syncApprovalStatus();
        } catch (Exception e) {
            log.error("Failed to synchronize external approval status, reason={}", e.getMessage(), e);
        }
    }

    private void syncApprovalStatus() throws InterruptedException {
        Lock lock = jdbcLockRegistry.obtain(LOCK_NAME);
        if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.info("Skip current synchronize due to trying lock failed, may other odc-server node is handling");
            return;
        }
        try {
            List<UserTaskInstanceEntity> entities = approvalPermissionService.listApprovableExternalInstances();
            if (CollectionUtils.isEmpty(entities)) {
                return;
            }
            Set<Long> flowInstanceIds =
                    entities.stream().map(UserTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toSet());
            Map<Long, ProcessInstance> flowInstanceId2ProcessInstance =
                    getFlowInstanceId2ProcessInstance(flowInstanceIds);
            entities.forEach(entity -> {
                ProcessInstance processInstance;
                TemplateVariables variables = new TemplateVariables();
                try {
                    processInstance = flowInstanceId2ProcessInstance.get(entity.getFlowInstanceId());
                    variables = FlowTaskUtil.getTemplateVariables(processInstance.getProcessVariables());
                    variables.setAttribute(Variable.PROCESS_INSTANCE_ID, entity.getExternalFlowInstanceId());
                    update(entity.getFlowInstanceId(), entity.getExternalApprovalId(), processInstance, variables);
                } catch (Exception e) {
                    log.warn(
                            "Failed to synchronize external approval status, flowInstanceId={}, integrationId={}, externalProcessInstanceId={}, variables={}",
                            entity.getFlowInstanceId(), entity.getExternalApprovalId(),
                            entity.getExternalFlowInstanceId(), variables, e);
                }
            });
        } finally {
            lock.unlock();
        }
    }

    private void update(Long flowInstanceId, Long externalApprovalId, ProcessInstance processInstance,
            TemplateVariables variables) throws IOException {
        ApprovalProperties properties =
                (ApprovalProperties) integrationService.getIntegrationProperties(externalApprovalId);
        ApprovalStatus status = approvalClient.status(properties, variables);
        switch (status) {
            case APPROVED:
                flowInstanceService.approve(flowInstanceId, "Approved by external approval service", true);
                return;
            case REJECTED:
                flowInstanceService.reject(flowInstanceId, "Rejected by external approval service", true);
                return;
            case TERMINATED:
                flowInstanceService.cancel(flowInstanceId, true);
                return;
            case PENDING:
            default:
                break;
        }
        Date startTime = processInstance.getStartTime();
        Date approvalExpireTime = new Date(startTime.getTime() + properties.getApprovalTimeoutSeconds() * 1000L);
        if (new Date().after(approvalExpireTime)) {
            approvalClient.cancel(properties, variables);
            flowInstanceService.cancel(flowInstanceId, true);
        }
    }

    private Map<Long, ProcessInstance> getFlowInstanceId2ProcessInstance(Collection<Long> flowInstanceIds) {
        List<FlowInstanceEntity> entities = flowInstanceService.listByIds(flowInstanceIds);
        Set<String> processInstanceIds =
                entities.stream().map(FlowInstanceEntity::getProcessInstanceId).collect(Collectors.toSet());
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .processInstanceIds(processInstanceIds)
                .includeProcessVariables();
        Map<String, ProcessInstance> processInstanceId2ProcessInstance = query.list().stream()
                .collect(Collectors.toMap(Execution::getProcessInstanceId, e -> e));
        Map<Long, ProcessInstance> map = new HashMap<>();
        entities.forEach(entity -> {
            ProcessInstance processInstance = processInstanceId2ProcessInstance.get(entity.getProcessInstanceId());
            map.put(entity.getId(), processInstance);
        });
        return map;
    }

}
