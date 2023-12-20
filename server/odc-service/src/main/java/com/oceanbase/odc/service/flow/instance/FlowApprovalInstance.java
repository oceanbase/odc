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
package com.oceanbase.odc.service.flow.instance;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.FormService;
import org.flowable.engine.TaskService;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.impl.form.BooleanFormType;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Approval nodes in process instances. Corresponding to the database table structure in
 * {@code flow_instance_node_approval}
 *
 * @author yh263208
 * @date 2022-02-16 17:00
 * @since ODC_release_3.3.0
 * @see BaseFlowNodeInstance
 */
@Slf4j
@Getter
public class FlowApprovalInstance extends BaseFlowUserTaskInstance {

    public static final String APPROVAL_VARIABLE_NAME = "approved";
    @Setter
    private String comment;
    private Long operatorId;
    private boolean approved;
    private final Integer expireIntervalSeconds;
    private final boolean autoApprove;
    @Setter
    private String externalFlowInstanceId;
    @Setter
    private String candidate;
    private final Long externalApprovalId;
    private boolean waitForConfirm;

    @Getter(AccessLevel.NONE)
    private final AuthenticationFacade authenticationFacade;
    @Getter(AccessLevel.NONE)
    private final UserTaskInstanceRepository userTaskInstanceRepository;
    @Getter(AccessLevel.NONE)
    private final UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;

    public FlowApprovalInstance(@NonNull UserTaskInstanceEntity entity, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull TaskService taskService, @NonNull FormService formService,
            @NonNull EventPublisher eventPublisher,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository) {
        super(FlowNodeType.APPROVAL_TASK, entity.getId(), entity.getOrganizationId(), entity.getStatus(),
                entity.getUserTaskId(), entity.getFlowInstanceId(), entity.getCreateTime(), entity.getUpdateTime(),
                entity.isStartEndpoint(), entity.isEndEndpoint(), taskService, formService, eventPublisher,
                flowableAdaptor, nodeRepository, sequenceRepository);
        this.userTaskInstanceRepository = userTaskInstanceRepository;
        this.comment = entity.getComment();
        this.operatorId = entity.getOperatorId();
        this.approved = entity.isApproved();
        this.authenticationFacade = authenticationFacade;
        this.expireIntervalSeconds = entity.getExpireIntervalSeconds();
        this.autoApprove = entity.isAutoApprove();
        this.externalFlowInstanceId = entity.getExternalFlowInstanceId();
        this.externalApprovalId = entity.getExternalApprovalId();
        this.userTaskInstanceCandidateRepository = userTaskInstanceCandidateRepository;
        alloc();
    }

    public FlowApprovalInstance(@NonNull Long organizationId, @NonNull Long flowInstanceId, Long externalApprovalId,
            @NonNull Integer expireIntervalSeconds, boolean startEndpoint, boolean endEndPoint, boolean autoApprove,
            @NonNull FlowableAdaptor flowableAdaptor, @NonNull TaskService taskService,
            @NonNull FormService formService,
            @NonNull EventPublisher eventPublisher,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository) {
        super(FlowNodeType.APPROVAL_TASK, organizationId, flowInstanceId, startEndpoint, endEndPoint,
                taskService, formService, eventPublisher, flowableAdaptor, nodeRepository, sequenceRepository);
        this.userTaskInstanceRepository = userTaskInstanceRepository;
        this.authenticationFacade = authenticationFacade;
        this.expireIntervalSeconds = expireIntervalSeconds;
        this.autoApprove = autoApprove;
        this.externalApprovalId = externalApprovalId;
        this.userTaskInstanceCandidateRepository = userTaskInstanceCandidateRepository;
        alloc();
    }

    public FlowApprovalInstance(@NonNull Long organizationId, @NonNull Long flowInstanceId,
            @NonNull Integer expireIntervalSeconds, boolean startEndpoint, boolean endEndPoint, boolean autoApprove,
            @NonNull FlowableAdaptor flowableAdaptor, @NonNull TaskService taskService,
            @NonNull FormService formService,
            @NonNull EventPublisher eventPublisher,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository, @NonNull Boolean waitForConfirm,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository) {
        super(FlowNodeType.APPROVAL_TASK, organizationId, flowInstanceId, startEndpoint, endEndPoint,
                taskService, formService, eventPublisher, flowableAdaptor, nodeRepository, sequenceRepository);
        this.userTaskInstanceRepository = userTaskInstanceRepository;
        this.authenticationFacade = authenticationFacade;
        this.expireIntervalSeconds = expireIntervalSeconds;
        this.autoApprove = autoApprove;
        this.waitForConfirm = waitForConfirm;
        this.externalApprovalId = null;
        this.userTaskInstanceCandidateRepository = userTaskInstanceCandidateRepository;
        alloc();
    }

    @Override
    public FlowableElementType getCoreFlowableElementType() {
        return FlowableElementType.USER_TASK;
    }

    public static List<FlowApprovalInstance> batchCreate(List<FlowApprovalInstance> instances,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }
        List<UserTaskInstanceEntity> entities = instances.stream()
                .map(FlowApprovalInstance::mapToUserEntity).collect(Collectors.toList());
        entities = userTaskInstanceRepository.batchCreate(entities);
        for (int i = 0; i < instances.size(); i++) {
            instances.get(i).setId(entities.get(i).getId());
            instances.get(i).createTime = entities.get(i).getCreateTime();
            instances.get(i).updateTime = entities.get(i).getUpdateTime();
        }
        List<UserTaskInstanceCandidateEntity> entities1 = instances.stream()
                .filter(instance -> StringUtils.isNotBlank(instance.getCandidate()))
                .map(FlowApprovalInstance::mapToCandidateEntity).collect(Collectors.toList());
        userTaskInstanceCandidateRepository.batchCreate(entities1);
        return instances;
    }

    @Override
    public void create() {
        validNotExists();
        UserTaskInstanceEntity entity = mapToUserEntity(this);
        entity = userTaskInstanceRepository.save(entity);
        Verify.notNull(entity.getId(), "id");
        Verify.notNull(entity.getCreateTime(), "CreateTime");
        Verify.notNull(entity.getUpdateTime(), "UpdateTime");
        this.id = entity.getId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
        if (this.candidate != null) {
            UserTaskInstanceCandidateEntity candidateEntity = mapToCandidateEntity(this);
            userTaskInstanceCandidateRepository.save(candidateEntity);
        }
        log.info("Create approval task instance successfully, approvalTask={}", entity);
    }

    @Override
    public void update() {
        validExists();
        UserTaskInstanceEntity entity = new UserTaskInstanceEntity();
        entity.setId(getId());
        entity.setUserTaskId(getUserTaskId());
        entity.setStatus(getStatus());
        entity.setApproved(isApproved());
        entity.setOperatorId(getOperatorId());
        entity.setComment(getComment());
        entity.setExpireIntervalSeconds(getExpireIntervalSeconds());
        entity.setExternalFlowInstanceId(getExternalFlowInstanceId());
        int affectRows = userTaskInstanceRepository.update(entity);
        log.info("Update approval task instance successfully, affectRows={}, approvalTask={}", affectRows, entity);
    }

    public void approve(String comment, boolean requireOperator) {
        internalOperate(true, comment, requireOperator);
    }

    public void disApprove(String comment, boolean requireOperator) {
        internalOperate(false, comment, requireOperator);
    }

    public void approve() {
        approve(null, true);
    }

    public void disApprove() {
        disApprove(null, true);
    }

    @Override
    protected void doDelete(@NonNull Long instanceId) {
        userTaskInstanceRepository.deleteById(instanceId);
        log.info("Delete approval task instance successfully, approvalTaskId={}", instanceId);
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_FLOW_APPROVAL_INSTANCE.name();
    }

    private void internalOperate(boolean approved, String comment, boolean requireOperator) {
        boolean originApproved = isApproved();
        String originComment = getComment();
        Long originOperatorId = getOperatorId();
        try {
            this.approved = approved;
            this.comment = comment;
            if (!this.autoApprove && requireOperator) {
                this.operatorId = authenticationFacade.currentUserId();
            }
            List<FormProperty> formProperties = getFormProperties();
            formProperties = formProperties.stream()
                    .filter(f -> APPROVAL_VARIABLE_NAME.equals(f.getName()) && f.getType() instanceof BooleanFormType)
                    .collect(Collectors.toList());
            if (formProperties.isEmpty()) {
                log.warn("Form data in the approval node is not detected, propertyName={}", APPROVAL_VARIABLE_NAME);
            }
            Map<String, Object> variables = new HashMap<>();
            variables.putIfAbsent(APPROVAL_VARIABLE_NAME, approved);
            complete(variables);
        } catch (Exception e) {
            Long operatorId = null;
            if (!this.autoApprove) {
                operatorId = authenticationFacade.currentUserId();
            }
            log.warn("Failed to perform approval or rejection, approved={}, comment={}, operatorId={}",
                    approved, comment, operatorId, e);
            this.approved = originApproved;
            this.comment = originComment;
            this.operatorId = originOperatorId;
            throw e;
        }
    }

    private static UserTaskInstanceEntity mapToUserEntity(FlowApprovalInstance instance) {
        UserTaskInstanceEntity entity = new UserTaskInstanceEntity();
        entity.setOrganizationId(instance.getOrganizationId());
        entity.setUserTaskId(instance.getUserTaskId());
        entity.setStatus(instance.getStatus());
        entity.setOperatorId(instance.getOperatorId());
        entity.setComment(instance.getComment());
        entity.setApproved(instance.isApproved());
        entity.setStartEndpoint(instance.isStartEndpoint());
        entity.setEndEndpoint(instance.isEndEndPoint());
        entity.setFlowInstanceId(instance.getFlowInstanceId());
        entity.setAutoApprove(instance.isAutoApprove());
        entity.setExpireIntervalSeconds(instance.getExpireIntervalSeconds());
        entity.setExternalFlowInstanceId(instance.getExternalFlowInstanceId());
        entity.setExternalApprovalId(instance.getExternalApprovalId());
        entity.setWaitForConfirm(instance.isWaitForConfirm());
        return entity;
    }

    private static UserTaskInstanceCandidateEntity mapToCandidateEntity(FlowApprovalInstance instance) {
        Verify.notNull(instance.getId(), "FlowApprovalInstanceId");
        Verify.notNull(instance.getCandidate(), "FlowApprovalInstanceCandidate");
        UserTaskInstanceCandidateEntity entity = new UserTaskInstanceCandidateEntity();
        entity.setApprovalInstanceId(instance.getId());
        entity.setResourceRoleIdentifier(instance.getCandidate());
        return entity;
    }

}
