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

package com.oceanbase.odc.service.permissionapply.project;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/10/12 10:59
 */
@Slf4j
public class ApplyProjectFlowableTask extends BaseODCFlowTaskDelegate<Void> {

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

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                log.info("Apply project permission task starts, taskId={}, activityId={}", taskId,
                        execution.getCurrentActivityId());
                taskService.start(taskId);
                ApplyProjectParameter parameter = FlowTaskUtil.getApplyProjectParameter(execution);
                UserEntity userEntity = userRepository.findById(parameter.getUserId()).orElseThrow(
                        () -> {
                            log.error("User not found, id={}", parameter.getUserId());
                            return new NotFoundException(ResourceType.ODC_USER, "id", parameter.getUserId());
                        });
                ProjectEntity projectEntity = projectRepository.findById(parameter.getProjectId()).orElseThrow(
                        () -> {
                            log.error("Project not found, id={}", parameter.getProjectId());
                            return new NotFoundException(ResourceType.ODC_PROJECT, "id", parameter.getProjectId());
                        });
                if (CollectionUtils.isEmpty(parameter.getResourceRoleIds())) {
                    log.info("No project role to apply, skip grant");
                    return;
                }
                for (Long resourceRoleId : parameter.getResourceRoleIds()) {
                    log.info("Grant project role to user, userId={}, projectId={}, projectRoleId={}",
                            parameter.getUserId(), parameter.getProjectId(), resourceRoleId);
                    ResourceRoleEntity resourceRoleEntity = resourceRoleRepository.findById(resourceRoleId).orElseThrow(
                            () -> {
                                log.error("Project role not found, id={}", resourceRoleId);
                                return new NotFoundException(ResourceType.ODC_RESOURCE_ROLE, "id", resourceRoleId);
                            });
                    UserResourceRoleEntity entity = new UserResourceRoleEntity();
                    entity.setUserId(userEntity.getId());
                    entity.setResourceId(projectEntity.getId());
                    entity.setResourceRoleId(resourceRoleEntity.getId());
                    entity.setOrganizationId(projectEntity.getOrganizationId());
                    userResourceRoleRepository.save(entity);
                    log.info("Grant project role to user successfully, userId={}, projectId={}, projectRoleId={}",
                            parameter.getUserId(), parameter.getProjectId(), resourceRoleId);
                    success = true;
                    taskService.succeed(taskId, null);
                }
            } catch (Exception e) {
                log.error("Error occurs while apply project permission task executing", e);
                failure = true;
                taskService.fail(taskId, 0, null);
                status.setRollbackOnly();
                if (e instanceof BaseFlowException) {
                    throw e;
                } else {
                    throw new ServiceTaskError(e);
                }
            }
        });
        return null;
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
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Apply project permission task timeout, taskId={}", taskId);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {}

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
