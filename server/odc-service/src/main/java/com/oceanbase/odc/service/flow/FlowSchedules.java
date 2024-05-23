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
package com.oceanbase.odc.service.flow;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link FlowSchedules}
 *
 * @author yh263208
 * @date 2024-05-22 15:33
 * @since ODC-release_4.3.0
 */
@Slf4j
@Component
public class FlowSchedules {

    private static final Integer OB_MAX_IN_SIZE = 2000;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private FlowTaskProperties flowTaskProperties;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;

    @Scheduled(fixedDelayString = "${odc.flow.task.heartbeat-timeout-check-interval-millis:15000}")
    public void cancelHeartbeatTimeoutFlow() {
        long timeoutSeconds = this.flowTaskProperties.getHeartbeatTimeoutSeconds();
        if (timeoutSeconds <= 0) {
            return;
        }
        long minTimeoutSeconds = RuntimeTaskConstants.DEFAULT_TASK_CHECK_INTERVAL_SECONDS * 3;
        if (timeoutSeconds < minTimeoutSeconds) {
            timeoutSeconds = minTimeoutSeconds;
        }
        try {
            List<ServiceTaskInstanceEntity> taskInstanceEntities = this.serviceTaskInstanceRepository
                    .findByStatus(FlowNodeStatus.EXECUTING).stream()
                    .filter(e -> e.getTargetTaskId() != null).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(taskInstanceEntities)) {
                return;
            }
            List<Long> taskIds = taskInstanceEntities.stream()
                    .map(ServiceTaskInstanceEntity::getTargetTaskId).distinct().collect(Collectors.toList());
            Date timeoutBound = new Date(System.currentTimeMillis() - timeoutSeconds * 1000);
            List<TaskEntity> heartbeatTimeoutTasks = DBSchemaAccessorUtil.partitionFind(taskIds,
                    OB_MAX_IN_SIZE, ids -> taskRepository.findAllByLastHeartbeatTimeBeforeAndIdIn(timeoutBound, ids));
            if (CollectionUtils.isEmpty(heartbeatTimeoutTasks)) {
                return;
            }
            Set<Long> heartbeatTimeoutTaskIds = heartbeatTimeoutTasks.stream()
                    .map(TaskEntity::getId).collect(Collectors.toSet());
            log.info("Find the task with heartbeat timeout, timeoutSeconds={}, earliestHeartbeatTime={}, "
                    + "heartbeatTimeoutTaskIds={}", timeoutSeconds, timeoutBound, heartbeatTimeoutTaskIds);
            /**
             * we just find such flow task instances:
             * <p>
             * 1. heartbeat timeout 2. flow task instance is executing
             * </p>
             */
            cancelFlowTaskInstanceAndFlowInstance(taskInstanceEntities.stream()
                    .filter(e -> heartbeatTimeoutTaskIds.contains(e.getTargetTaskId()))
                    .map(ServiceTaskInstanceEntity::getId).distinct().collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("Failed to sync flow instance's status", e);
        }
    }

    private void cancelFlowTaskInstanceAndFlowInstance(List<Long> candidateFlowTaskInstanceIds) {
        this.transactionTemplate.executeWithoutResult(tx -> {
            try {
                List<ServiceTaskInstanceEntity> candidates = serviceTaskInstanceRepository
                        .findByIdInAndStatus(candidateFlowTaskInstanceIds, FlowNodeStatus.EXECUTING);
                List<Long> candidateIds = candidates.stream()
                        .map(ServiceTaskInstanceEntity::getId).distinct().collect(Collectors.toList());
                List<Integer> result = DBSchemaAccessorUtil.partitionFind(candidateIds,
                        OB_MAX_IN_SIZE, ids -> Collections.singletonList(
                                serviceTaskInstanceRepository.updateStatusByIdIn(ids, FlowNodeStatus.FAILED)));
                log.info("Update flow task instance status succeed, affectRows={}, flowTaskInstIds={}",
                        result, candidateIds);
                List<Long> flowInstIds = candidates.stream().map(ServiceTaskInstanceEntity::getFlowInstanceId)
                        .distinct().collect(Collectors.toList());
                result = DBSchemaAccessorUtil.partitionFind(flowInstIds,
                        OB_MAX_IN_SIZE, ids -> Collections.singletonList(
                                flowInstanceRepository.updateStatusByIds(ids, FlowStatus.EXECUTION_FAILED)));
                log.info("Update flow instance status succeed, affectRows={}, flowInstIds={}", result, flowInstIds);
            } catch (Exception e) {
                log.warn("Failed to sync flow instance's status", e);
                tx.setRollbackOnly();
            }
        });
    }

}
