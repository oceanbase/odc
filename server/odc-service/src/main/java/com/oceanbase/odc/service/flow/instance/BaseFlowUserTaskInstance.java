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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.flowable.engine.FormService;
import org.flowable.engine.TaskService;
import org.flowable.engine.form.FormData;
import org.flowable.engine.form.FormProperty;
import org.flowable.task.service.delegate.DelegateTask;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.event.UserTaskCreatedListener;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Refer to {@link org.flowable.bpmn.model.UserTask} in {@code Flowable}
 *
 * @author yh263208
 * @date 2022-02-17 11:53
 * @since ODC_release_3.3.0
 * @see BaseFlowNodeInstance
 */
@Slf4j
public abstract class BaseFlowUserTaskInstance extends BaseFlowNodeInstance {
    @Getter
    @Setter
    private volatile String userTaskId;

    private final TaskService taskService;
    private final FormService formService;
    private final EventPublisher eventPublisher;
    private UserTaskCreatedListener listener;

    public BaseFlowUserTaskInstance(@NonNull FlowNodeType nodeType, Long id,
            @NonNull Long organizationId, @NonNull FlowNodeStatus status, String userTaskId,
            @NonNull Long flowInstanceId, Date createTime, Date updateTime, boolean startEndpoint, boolean endEndPoint,
            @NonNull TaskService taskService, @NonNull FormService formService,
            @NonNull EventPublisher eventPublisher,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository) {
        super(nodeType, id, organizationId, status, flowInstanceId, createTime, updateTime,
                startEndpoint, endEndPoint, flowableAdaptor, nodeRepository, sequenceRepository);
        this.userTaskId = userTaskId;
        this.taskService = taskService;
        this.formService = formService;
        this.eventPublisher = eventPublisher;
    }

    public BaseFlowUserTaskInstance(@NonNull FlowNodeType nodeType, @NonNull Long organizationId,
            @NonNull Long flowInstanceId, boolean startEndpoint, boolean endEndPoint,
            @NonNull TaskService taskService, @NonNull FormService formService,
            @NonNull EventPublisher eventPublisher,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository) {
        super(nodeType, organizationId, flowInstanceId, startEndpoint, endEndPoint,
                flowableAdaptor, nodeRepository, sequenceRepository);
        this.taskService = taskService;
        this.formService = formService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public boolean isPresentOnThisMachine() {
        Optional<String> optionalTask = getUserTask();
        return optionalTask.isPresent();
    }

    public List<FormProperty> getFormProperties() {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("User task isn't on this machine");
        }
        Optional<String> optionalTask = getUserTask();
        if (!optionalTask.isPresent()) {
            throw new IllegalStateException("Can not get Task by user task id " + this.userTaskId);
        }
        FormData formData = formService.getTaskFormData(optionalTask.get());
        if (formData == null) {
            return Collections.emptyList();
        }
        return formData.getFormProperties();
    }

    public void bindToUserTask(Long relatedInstanceId, @NonNull DelegateTask task) {
        if (Objects.equals(relatedInstanceId, getId())) {
            setUserTaskId(task.getId());
            setStatus(FlowNodeStatus.EXECUTING);
        }
    }

    @Override
    public void dealloc() {
        if (this.listener == null) {
            return;
        }
        this.eventPublisher.removeEventListener(this.listener);
        this.listener = null;
    }

    @Override
    public void alloc() {
        if (this.listener != null) {
            return;
        }
        this.listener = new UserTaskCreatedListener(this);
        this.eventPublisher.addEventListener(this.listener);
    }

    protected void complete(Map<String, Object> variables) {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("User task isn't on this machine");
        }
        Optional<String> optionalTask = getUserTask();
        if (!optionalTask.isPresent()) {
            throw new IllegalStateException("Can not get Task by user task id " + this.userTaskId);
        }
        if (variables == null) {
            taskService.complete(optionalTask.get());
        } else {
            taskService.complete(optionalTask.get(), variables);
        }
        setStatus(FlowNodeStatus.COMPLETED);
        update();
    }

    protected Optional<String> getUserTask() {
        if (this.userTaskId == null) {
            log.warn("userTaskId is empty, flowInstanceId={}, instanceId={}",
                    this.getFlowInstanceId(), getId());
            return Optional.empty();
        }
        return Optional.of(this.userTaskId);
    }

}
