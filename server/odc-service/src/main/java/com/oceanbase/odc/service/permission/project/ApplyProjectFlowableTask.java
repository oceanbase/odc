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

package com.oceanbase.odc.service.permission.project;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.trace.TaskContextHolder;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.permission.project.ApplyProjectParameter.ApplyResourceRole;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/10/12 10:59
 */
@Slf4j
public class ApplyProjectFlowableTask extends BaseODCFlowTaskDelegate<ApplyProjectResult> {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRoleRepository resourceRoleRepository;

    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private volatile boolean success = false;
    private volatile boolean failure = false;
    private Long taskCreatorId;

    @Override
    protected ApplyProjectResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        this.taskCreatorId = FlowTaskUtil.getTaskCreator(execution).getId();
        TaskContextHolder.trace(this.taskCreatorId, taskId);
        ApplyProjectResult result = new ApplyProjectResult();
        log.info("Apply project task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    taskService.start(taskId);
                    ApplyProjectParameter parameter = FlowTaskUtil.getApplyProjectParameter(execution);
                    result.setParameter(parameter);
                    UserEntity userEntity = userRepository.findById(this.taskCreatorId).orElseThrow(
                            () -> {
                                log.warn("User not found, id={}", this.taskCreatorId);
                                return new NotFoundException(ResourceType.ODC_USER, "id", this.taskCreatorId);
                            });
                    Long projectId = parameter.getProject().getId();
                    ProjectEntity projectEntity = projectRepository.findById(projectId).orElseThrow(
                            () -> {
                                log.warn("Project not found, id={}", projectId);
                                return new NotFoundException(ResourceType.ODC_PROJECT, "id", projectId);
                            });
                    List<Long> resourceRoleIds = parameter.getResourceRoles().stream().map(ApplyResourceRole::getId)
                            .collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(resourceRoleIds)) {
                        log.info("No project role to apply, skip granting");
                        success = true;
                        return;
                    }
                    for (Long resourceRoleId : resourceRoleIds) {
                        ResourceRoleEntity resourceRoleEntity =
                                resourceRoleRepository.findById(resourceRoleId).orElseThrow(
                                        () -> {
                                            log.warn("Project role not found, id={}", resourceRoleId);
                                            return new NotFoundException(ResourceType.ODC_RESOURCE_ROLE, "id",
                                                    resourceRoleId);
                                        });
                        log.info("Start grant project role to user, userId={}, projectId={}, projectRoleId={}",
                                this.taskCreatorId, projectId, resourceRoleId);
                        UserResourceRoleEntity entity = new UserResourceRoleEntity();
                        entity.setUserId(userEntity.getId());
                        entity.setResourceId(projectEntity.getId());
                        entity.setResourceRoleId(resourceRoleEntity.getId());
                        entity.setOrganizationId(projectEntity.getOrganizationId());
                        if (userResourceRoleRepository.exists(Example.of(entity))) {
                            log.info("Project role already granted to user, skip granting");
                            continue;
                        }
                        userResourceRoleRepository.save(entity);
                        log.info("Grant project role to user successfully, userId={}, projectId={}, projectRoleId={}",
                                this.taskCreatorId, projectId, resourceRoleId);
                    }
                    success = true;
                } catch (Exception e) {
                    failure = true;
                    log.warn("Error occurs while apply project task executing", e);
                    status.setRollbackOnly();
                }
            });
            result.setSuccess(success);
            if (result.isSuccess()) {
                taskService.succeed(taskId, result);
                log.info("Apply project task success");
            } else {
                taskService.fail(taskId, 0, result);
                log.info("Apply project task failed");
            }
        } catch (Exception e) {
            failure = true;
            result.setSuccess(false);
            taskService.fail(taskId, 0, result);
            log.warn("Apply project task failed, error={}", e.getMessage());
        } finally {
            TaskContextHolder.clear();
        }
        return result;
    }

    @Override
    protected boolean isSuccessful() {
        return success;
    }

    @Override
    protected boolean isFailure() {
        return failure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.warn("Apply project task failed, taskId={}", taskId);
        TaskContextHolder.clear();
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.info("Apply project task succeed, taskId={}", taskId);
        TaskContextHolder.clear();
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.warn("Apply project permission task timeout, taskId={}", taskId);
        TaskContextHolder.clear();
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {}

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        throw new UnsupportedException(ErrorCodes.TaskNotTerminable, null,
                "The task is not terminable during execution");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    private long getTaskCreatorId(Long taskId, TaskService taskService) {
        if (this.taskCreatorId != null) {
            return this.taskCreatorId;
        }
        return taskService.detail(taskId).getCreatorId();
    }

}
