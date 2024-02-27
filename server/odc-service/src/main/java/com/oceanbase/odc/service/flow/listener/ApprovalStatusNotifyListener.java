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
package com.oceanbase.odc.service.flow.listener;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.flow.util.EmptyExecutionListener;
import com.oceanbase.odc.metadb.flow.FlowInstanceApprovalViewEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceApprovalViewRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.notification.Broker;
import com.oceanbase.odc.service.notification.NotificationProperties;
import com.oceanbase.odc.service.notification.helper.EventBuilder;
import com.oceanbase.odc.service.notification.model.Event;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/26
 */
@Slf4j
public class ApprovalStatusNotifyListener extends EmptyExecutionListener {

    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private EventBuilder eventBuilder;
    @Autowired
    private Broker broker;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    FlowInstanceApprovalViewRepository flowInstanceApprovalViewRepository;
    @Autowired
    UserResourceRoleRepository userResourceRoleRepository;

    @Override
    protected void onExecutiuonStart(DelegateExecution execution) {
        if (!notificationProperties.isEnabled()) {
            return;
        }
        try {
            FlowApprovalInstance target = getTarget(execution);
            TaskEntity taskEntity = flowInstanceService.getTaskByFlowInstanceId(target.getFlowInstanceId());
            if (target.isAutoApprove()) {
                return;
            }

            Set<Long> approverIds = null;
            Optional<FlowInstanceApprovalViewEntity> optional =
                    flowInstanceApprovalViewRepository.findById(target.getId());
            if (optional.isPresent()) {
                List<UserResourceRoleEntity> userResourceRoles =
                        userResourceRoleRepository.findByResourceIdsAndResourceRoleIdsIn(
                                Collections.singleton(optional.get().getResourceRoleIdentifier()));
                approverIds = CollectionUtils.isEmpty(userResourceRoles) ? null
                        : userResourceRoles.stream().map(UserResourceRoleEntity::getUserId).collect(Collectors.toSet());
            }
            Event event = eventBuilder.ofPendingApprovalTask(taskEntity, approverIds);
            broker.enqueueEvent(event);
        } catch (Exception e) {
            log.warn("Failed to enqueue event.", e);
        }
    }

    @Override
    protected void onExecutionEnd(DelegateExecution execution) {
        if (!notificationProperties.isEnabled()) {
            return;
        }
        try {
            FlowApprovalInstance approvalInstance = getTarget(execution);
            if (approvalInstance.isAutoApprove()) {
                Event event = eventBuilder.ofApprovedTask(
                        flowInstanceService.getTaskByFlowInstanceId(approvalInstance.getFlowInstanceId()), null);
                broker.enqueueEvent(event);
            }
        } catch (Exception e) {
            log.warn("Failed to enqueue event.", e);
        }
    }

    private FlowApprovalInstance getTarget(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        String processDefinitionId = execution.getProcessDefinitionId();
        Optional<Long> optional = flowableAdaptor.getFlowInstanceIdByProcessDefinitionId(processDefinitionId);
        if (!optional.isPresent()) {
            log.warn("Flow instance id does not exist, activityId={}, processDefinitionId={}", activityId,
                    processDefinitionId);
            throw new IllegalStateException(
                    "Can not find flow instance id by process definition id " + processDefinitionId);
        }
        Long flowInstanceId = optional.get();
        Optional<FlowApprovalInstance> targetOptional =
                flowableAdaptor.getApprovalInstanceByActivityId(activityId, flowInstanceId);
        if (!targetOptional.isPresent()) {
            log.warn("Flow node instance does not exist, activityId={}, flowInstanceId={}", activityId, flowInstanceId);
            throw new IllegalStateException("Can not find instance by activityId " + activityId);
        }
        return targetOptional.get();
    }

}
