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
package com.oceanbase.odc.service.permission.table;

import java.util.ArrayList;
import java.util.HashSet;
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
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter.ApplyTable;
import com.oceanbase.odc.service.permission.table.model.ApplyTableResult;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.loaddump.utils.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/14 17:25
 * @Version 1.0
 */
@Slf4j
public class ApplyTableFlowableTask extends BaseODCFlowTaskDelegate<ApplyTableResult> {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ResourceRoleService resourceRoleService;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private TableMappingRepository tableMappingRepository;

    private volatile boolean success = false;
    private volatile boolean failure = false;
    private Long creatorId;

    @Override
    protected ApplyTableResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        this.creatorId = FlowTaskUtil.getTaskCreator(execution).getId();
        TaskContextHolder.trace(creatorId, taskId);
        ApplyTableResult result = new ApplyTableResult();
        log.info("Apply table task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    taskService.start(taskId);
                    ApplyTableParameter parameter = FlowTaskUtil.getApplyTableParameter(execution);
                    result.setParameter(parameter);
                    checkResourceAndPermission(parameter);
                    List<PermissionEntity> permissionEntities = new ArrayList<>();
                    Long organizationId = FlowTaskUtil.getOrganizationId(execution);
                    Set<Long> logicalTableIds = parameter.getTables().stream()
                            .filter(t -> t.getType() == DBObjectType.LOGICAL_TABLE).map(ApplyTable::getTableId)
                            .collect(Collectors.toSet());
                    Set<ApplyTable> mappingPhysicalTables = new HashSet<>();
                    if (CollectionUtils.isNotEmpty(logicalTableIds)) {
                        tableMappingRepository.findByLogicalTableIdIn(logicalTableIds);
                        mappingPhysicalTables.addAll(
                                tableMappingRepository.findByLogicalTableIdIn(logicalTableIds).stream().map(t -> {
                                    ApplyTable table = new ApplyTable();
                                    table.setTableId(t.getPhysicalTableId());
                                    table.setDatabaseId(t.getPhysicalDatabaseId());
                                    return table;
                                }).collect(Collectors.toSet()));

                    }
                    mappingPhysicalTables.addAll(parameter.getTables());
                    for (ApplyTable table : mappingPhysicalTables) {
                        for (DatabasePermissionType permissionType : parameter.getTypes()) {
                            PermissionEntity permissionEntity = new PermissionEntity();
                            permissionEntity.setAction(permissionType.getAction());
                            permissionEntity.setResourceIdentifier(
                                    ResourceType.ODC_DATABASE.name() + ":" + table.getDatabaseId() + "/"
                                            + ResourceType.ODC_TABLE.name() + ":" + table.getTableId());
                            permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
                            permissionEntity.setCreatorId(this.creatorId);
                            permissionEntity.setOrganizationId(organizationId);
                            permissionEntity.setBuiltIn(false);
                            permissionEntity.setExpireTime(parameter.getExpireTime());
                            permissionEntity.setAuthorizationType(AuthorizationType.TICKET_APPLICATION);
                            permissionEntity.setTicketId(FlowTaskUtil.getFlowInstanceId(execution));
                            permissionEntity.setResourceType(ResourceType.ODC_TABLE);
                            permissionEntity.setResourceId(table.getTableId());
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
                    log.warn("Error occurs while apply table task executing", e);
                    status.setRollbackOnly();
                }
            });
            result.setSuccess(success);
            if (result.isSuccess()) {
                taskService.succeed(taskId, result);
                log.info("Apply table task success");
            } else {
                taskService.fail(taskId, 0, result);
                log.info("Apply table task failed");
            }
        } catch (Exception e) {
            failure = true;
            result.setSuccess(false);
            taskService.fail(taskId, 0, result);
            log.warn("Apply table task failed, error={}", e.getMessage());
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
        log.warn("Apply table task failed, taskId={}", taskId);
        TaskContextHolder.clear();
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.info("Apply table task succeed, taskId={}", taskId);
        TaskContextHolder.clear();
        super.onSuccessful(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        TaskContextHolder.trace(getTaskCreatorId(taskId, taskService), taskId);
        log.warn("Apply table permission task timeout, taskId={}", taskId);
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

    private void checkResourceAndPermission(ApplyTableParameter parameter) {
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
        Set<Long> projectMemberIds =
                resourceRoleService.listUserIdsByResourceTypeAndResourceId(ResourceType.ODC_PROJECT, projectId);
        if (!projectMemberIds.contains(this.creatorId)) {
            log.warn("User not member of project, userId={}, projectId={}", this.creatorId, projectId);
            throw new IllegalStateException("User not member of project");
        }
        // Check databases still exists and belong to the project
        List<Long> tableIds = parameter.getTables().stream().map(ApplyTable::getTableId).collect(Collectors.toList());
        List<Long> databaseIds =
                parameter.getTables().stream().map(ApplyTable::getDatabaseId).collect(Collectors.toList());
        Map<Long, DBObjectEntity> id2tableEntity = dbObjectRepository.findByIdIn(tableIds).stream()
                .collect(Collectors.toMap(DBObjectEntity::getId, t -> t, (t1, t2) -> t1));
        Map<Long, DatabaseEntity> id2databaseEntity = databaseRepository.findByIdIn(databaseIds).stream()
                .collect(Collectors.toMap(DatabaseEntity::getId, d -> d, (d1, d2) -> d1));
        for (Long tableId : tableIds) {
            if (!id2tableEntity.containsKey(tableId)) {
                log.warn("Table not found, id={}", tableId);
                throw new NotFoundException(ResourceType.ODC_TABLE, "id", tableId);
            }
            Long databaseId = id2tableEntity.get(tableId).getDatabaseId();
            DatabaseEntity databaseEntity = id2databaseEntity.get(databaseId);
            if (databaseEntity == null) {
                log.warn("Database not found, id={}", databaseId);
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", databaseId);
            }
            if (databaseEntity.getProjectId() == null || !databaseEntity.getProjectId().equals(projectId)) {
                log.warn("Database not belong to project, databaseId={}, projectId={}", databaseId, projectId);
                throw new IllegalStateException("Database not belong to project");
            }
        }
    }

}
