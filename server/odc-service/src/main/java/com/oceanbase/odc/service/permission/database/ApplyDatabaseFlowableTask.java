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
package com.oceanbase.odc.service.permission.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.trace.TaskContextHolder;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter.ApplyDatabase;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseResult;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/1/3 15:00
 */
@Slf4j
public class ApplyDatabaseFlowableTask extends BaseODCFlowTaskDelegate<ApplyDatabaseResult> {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    private volatile boolean success = false;
    private volatile boolean failure = false;
    private Long creatorId;

    @Override
    protected ApplyDatabaseResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        this.creatorId = FlowTaskUtil.getTaskCreator(execution).getId();
        TaskContextHolder.trace(creatorId, taskId);
        ApplyDatabaseResult result = new ApplyDatabaseResult();
        log.info("Apply database task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    taskService.start(taskId);
                    ApplyDatabaseParameter parameter = FlowTaskUtil.getApplyDatabaseParameter(execution);
                    result.setParameter(parameter);
                    checkResourceAndPermission(parameter);
                    List<PermissionEntity> permissionEntities = new ArrayList<>();
                    Long organizationId = FlowTaskUtil.getOrganizationId(execution);
                    for (ApplyDatabase database : parameter.getDatabases()) {
                        for (DatabasePermissionType permissionType : parameter.getTypes()) {
                            PermissionEntity permissionEntity = new PermissionEntity();
                            permissionEntity.setAction(permissionType.getAction());
                            permissionEntity
                                    .setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + database.getId());
                            permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
                            permissionEntity.setCreatorId(this.creatorId);
                            permissionEntity.setOrganizationId(organizationId);
                            permissionEntity.setBuiltIn(false);
                            permissionEntity.setExpireTime(parameter.getExpireTime());
                            permissionEntity.setAuthorizationType(AuthorizationType.TICKET_APPLICATION);
                            permissionEntity.setTicketId(FlowTaskUtil.getFlowInstanceId(execution));
                            permissionEntity.setResourceType(ResourceType.ODC_DATABASE);
                            permissionEntity.setResourceId(database.getId());
                            permissionEntities.add(permissionEntity);
                        }
                    }
                    List<PermissionEntity> saved = permissionRepository.batchCreate(permissionEntities);
                    List<UserPermissionEntity> userPermissionEntities = new ArrayList<>();
                    for (PermissionEntity permissionEntity : saved) {
                        UserPermissionEntity userPermissionEntity = new UserPermissionEntity();
                        userPermissionEntity.setUserId(this.creatorId);
                        userPermissionEntity.setPermissionId(permissionEntity.getId());
                        userPermissionEntity.setCreatorId(this.creatorId);
                        userPermissionEntity.setOrganizationId(organizationId);
                        userPermissionEntities.add(userPermissionEntity);
                    }
                    userPermissionRepository.batchCreate(userPermissionEntities);
                    success = true;
                } catch (Exception e) {
                    failure = true;
                    log.warn("Error occurs while apply database task executing", e);
                    status.setRollbackOnly();
                }
            });
            result.setSuccess(success);
            if (result.isSuccess()) {
                taskService.succeed(taskId, result);
                log.info("Apply database task success");
            } else {
                taskService.fail(taskId, 0, result);
                log.info("Apply database task failed");
            }
        } catch (Exception e) {
            failure = true;
            result.setSuccess(false);
            taskService.fail(taskId, 0, result);
            log.warn("Apply database task failed, error={}", e.getMessage());
        } finally {
            TaskContextHolder.clear();
        }
        return result;
    }

    @Override
    protected boolean isSuccessful() {
        return this.success;
    }

    @Override
    protected boolean isFailure() {
        return this.failure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.warn("Apply database task failed, taskId={}", taskId);
        TaskContextHolder.clear();
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.info("Apply database task succeed, taskId={}", taskId);
        TaskContextHolder.clear();
        super.onSuccessful(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.warn("Apply database permission task timeout, taskId={}", taskId);
        TaskContextHolder.clear();
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {

    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        throw new UnsupportedException(ErrorCodes.RunningTaskNotTerminable, null,
                "The task is not terminable during execution");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    private long getTaskCreatorId(Long taskId, TaskService taskService) {
        if (this.creatorId != null) {
            return this.creatorId;
        }
        return taskService.detail(taskId).getCreatorId();
    }

    private void checkResourceAndPermission(ApplyDatabaseParameter parameter) {
        // Check project still exists
        Long projectId = parameter.getProject().getId();
        projectRepository.findById(projectId).orElseThrow(
                () -> {
                    log.warn("Project not found, id={}", projectId);
                    return new NotFoundException(ResourceType.ODC_PROJECT, "id", projectId);
                });
        // Check user still exists and is member of the project
        userRepository.findById(this.creatorId).orElseThrow(
                () -> {
                    log.warn("User not found, id={}", this.creatorId);
                    return new NotFoundException(ResourceType.ODC_USER, "id", this.creatorId);
                });
        Set<Long> projectMemberIds = userResourceRoleRepository.findByResourceId(projectId).stream()
                .map(UserResourceRoleEntity::getUserId).collect(Collectors.toSet());
        if (!projectMemberIds.contains(this.creatorId)) {
            log.warn("User not member of project, userId={}, projectId={}", this.creatorId, projectId);
            throw new IllegalStateException("User not member of project");
        }
        // Check databases still exists and belong to the project
        List<Long> databaseIds =
                parameter.getDatabases().stream().map(ApplyDatabase::getId).collect(Collectors.toList());
        Map<Long, DatabaseEntity> id2databaseEntities = databaseRepository.findByIdIn(databaseIds).stream()
                .collect(Collectors.toMap(DatabaseEntity::getId, d -> d, (d1, d2) -> d1));
        for (Long id : databaseIds) {
            if (!id2databaseEntities.containsKey(id)) {
                log.warn("Database not found, id={}", id);
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", id);
            }
            DatabaseEntity databaseEntity = id2databaseEntities.get(id);
            if (databaseEntity.getProjectId() == null || !databaseEntity.getProjectId().equals(projectId)) {
                log.warn("Database not belong to project, databaseId={}, projectId={}", id, projectId);
                throw new IllegalStateException("Database not belong to project");
            }
        }
    }

}
