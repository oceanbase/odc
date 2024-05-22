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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
        try {
            long minTimeoutSeconds = RuntimeTaskConstants.DEFAULT_TASK_CHECK_INTERVAL_SECONDS + 5;
            long timeoutSeconds = this.flowTaskProperties.getHeartbeatTimeoutSeconds();
            if (timeoutSeconds < minTimeoutSeconds) {
                timeoutSeconds = minTimeoutSeconds;
            }
            Date timeoutBound = new Date(System.currentTimeMillis() - timeoutSeconds * 1000);
            List<TaskEntity> taskEntities = this.taskRepository.findAllByLastHeartbeatTimeBefore(timeoutBound);
            if (CollectionUtils.isEmpty(taskEntities)) {
                return;
            }
            List<Long> taskIds = taskEntities.stream().map(TaskEntity::getId).collect(Collectors.toList());
            log.info("Find the task with heartbeat timeout, timeoutSeconds={}, earliestHeartbeatTime={}, taskIds={}",
                    timeoutSeconds, timeoutBound, taskIds);
            /**
             * we just find such flow task instances:
             * <p>
             * 1. heartbeat timeout 2. flow task instance is executing
             * </p>
             */
            List<ServiceTaskInstanceEntity> candidates = DBSchemaAccessorUtil.partitionFind(taskIds,
                    DBSchemaAccessorUtil.OB_MAX_IN_SIZE,
                    ids -> serviceTaskInstanceRepository.findByTargetTaskIdIn(new HashSet<>(ids))).stream()
                    .filter(entity -> entity.getStatus() == FlowNodeStatus.EXECUTING).collect(Collectors.toList());
            Set<Long> flowTaskInstIds = candidates.stream()
                    .map(ServiceTaskInstanceEntity::getId).collect(Collectors.toSet());
            List<Long> flowInstIds = candidates.stream().map(ServiceTaskInstanceEntity::getFlowInstanceId)
                    .distinct().collect(Collectors.toList());
            log.info("Find heartbeat timeout flow task instance, timeoutSeconds={}, earliestHeartbeatTime={}, "
                    + "flowTaskInstIds={}, flowInstIds={}", timeoutSeconds, timeoutBound, flowTaskInstIds, flowInstIds);
            List<Integer> executeResult =
                    DBSchemaAccessorUtil.partitionFind(flowInstIds, DBSchemaAccessorUtil.OB_MAX_IN_SIZE, ids -> {
                        int affectRows = flowInstanceRepository.updateStatusByIds(ids, FlowStatus.CANCELLED);
                        List<Integer> result = new ArrayList<>();
                        result.add(affectRows);
                        return result;
                    });
            log.info("Update flow instance's status succeed, affectRows={}", executeResult);
        } catch (Exception e) {
            log.warn("Failed to sync flow instance's status", e);
        }
    }

}
