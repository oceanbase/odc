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
package com.oceanbase.odc.service.flow.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp.FlowNodeInstanceMapper;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.OdcMockTaskConfig;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.flow.util.FlowInstanceUtil;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter;
import com.oceanbase.odc.service.permission.project.ApplyProjectParameter;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author wenniu.ly
 * @date 2022/2/9
 */

@Data
public class FlowInstanceDetailResp {

    private Long id;
    private Long organizationId;
    private TaskType type;
    private List<FlowSubType> subTypes;
    private Integer maxRiskLevel;
    private RiskLevel riskLevel;
    private Database database;
    private Database relatedDatabase;
    private Long projectId;
    private Set<InnerUser> candidateApprovers;
    private InnerUser creator;
    private Date createTime;
    private TaskParameters parameters;
    private FlowTaskExecutionStrategy executionStrategy;
    private Date executionTime;
    @Internationalizable
    private String description;
    private FlowStatus status;
    private double progressPercentage;
    private boolean approvable;
    private boolean rollbackable;
    private Date completeTime;
    private List<FlowNodeInstanceDetailResp> nodeList;
    private Project project;

    @Getter
    @Builder
    public static class FlowInstanceMapper {
        private final Predicate<Long> ifApprovable;
        private final Predicate<Long> ifRollbackable;
        private final Function<Long, Database> getDatabaseById;
        private final Function<Long, UserEntity> getUserById;
        private final Function<Long, Set<TaskEntity>> getTasksByFlowInstanceId;
        private final Function<Long, List<RoleEntity>> getRolesByUserId;
        private final Function<Long, List<Date>> getExecutionTimeByFlowInstanceId;
        private final Function<Long, List<FlowTaskExecutionStrategy>> getExecutionStrategyByFlowInstanceId;
        private final Function<Long, RiskLevel> getRiskLevelByRiskLevelId;
        private final Function<Long, Set<UserEntity>> getCandidatesByFlowInstanceId;
        private final Function<Long, Project> getProjectById;

        public FlowInstanceDetailResp map(@NonNull FlowInstanceEntity entity) {
            FlowInstanceDetailResp resp = FlowInstanceDetailResp.withId(entity.getId());
            resp.setDescription(entity.getDescription());
            resp.setCreateTime(entity.getCreateTime());
            resp.setStatus(entity.getStatus());
            resp.setProjectId(entity.getProjectId());
            resp.setProject(getProjectById.apply(entity.getProjectId()));
            resp.setOrganizationId(entity.getOrganizationId());
            Set<UserEntity> candidates = getCandidatesByFlowInstanceId.apply(entity.getId());
            if (candidates != null) {
                resp.setCandidateApprovers(candidates.stream().map(InnerUser::new).collect(Collectors.toSet()));
            }
            if (this.ifApprovable != null) {
                resp.setApprovable(ifApprovable.test(entity.getId()));
            }
            if (this.ifRollbackable != null) {
                resp.setRollbackable(ifRollbackable.test(entity.getId()));
            }
            resp.setCompleteTime(entity.getUpdateTime());
            resp.setCreator(InnerUser.of(entity.getCreatorId(), getUserById, getRolesByUserId));
            List<Date> executionTimeList = getExecutionTimeByFlowInstanceId.apply(entity.getId());
            if (executionTimeList != null && !executionTimeList.isEmpty()) {
                resp.setExecutionTime(executionTimeList.get(0));
            }
            resp.setExecutionStrategy(getExecutionStrategyByFlowInstanceId.apply(entity.getId()).get(0));
            if (this.getTasksByFlowInstanceId == null) {
                return resp;
            }
            Set<TaskEntity> taskEntities = getTasksByFlowInstanceId.apply(entity.getId());
            Verify.notLessThan(taskEntities.size(), 1, "FlowInstanceRelatedTasks");
            TaskEntity taskEntity = taskEntities.stream().filter(t -> t.getTaskType() != TaskType.PRE_CHECK).findFirst()
                    .orElseThrow(() -> new UnexpectedException("not task found"));
            resp.setType(taskEntity.getTaskType());
            resp.setProgressPercentage(taskEntity.getProgressPercentage());
            if (taskEntity.getTaskType() == TaskType.ALTER_SCHEDULE) {
                resp.setParameters(JsonUtils.fromJson(taskEntity.getParametersJson(), AlterScheduleParameters.class));
            }
            if (this.getDatabaseById == null) {
                return resp;
            }
            resp.setDatabase(getDatabaseById.apply(taskEntity.getDatabaseId()));
            return resp;
        }

        public FlowInstanceDetailResp map(@NonNull FlowInstance flowInstance, @NonNull FlowNodeInstanceMapper mapper) {
            FlowInstanceDetailResp resp = FlowInstanceDetailResp.withId(flowInstance.getId());
            if (this.ifApprovable != null) {
                resp.setApprovable(ifApprovable.test(flowInstance.getId()));
            }
            if (this.ifRollbackable != null) {
                resp.setRollbackable(ifRollbackable.test(flowInstance.getId()));
            }
            resp.setOrganizationId(flowInstance.getOrganizationId());
            resp.setCreator(InnerUser.of(flowInstance.getCreatorId(), getUserById, getRolesByUserId));
            resp.setCompleteTime(flowInstance.getUpdateTime());
            resp.setCreateTime(flowInstance.getCreateTime());
            resp.setStatus(flowInstance.getStatus());
            resp.setDescription(flowInstance.getDescription());
            resp.setProjectId(flowInstance.getProjectId());
            resp.setProject(getProjectById.apply(flowInstance.getProjectId()));

            List<BaseFlowNodeInstance> instances =
                    flowInstance.filterInstanceNode(instance -> FlowNodeType.SERVICE_TASK == instance.getNodeType()
                            && ((FlowTaskInstance) instance).getTaskType().needForExecutionStrategy()
                            && Objects.nonNull(((FlowTaskInstance) instance).getTargetTaskId()));
            if (CollectionUtils.isNotEmpty(instances)) {
                FlowTaskInstance taskInstance = (FlowTaskInstance) instances.get(0);
                resp.setExecutionStrategy(taskInstance.getStrategyConfig().getStrategy());
                resp.setExecutionTime(taskInstance.getStrategyConfig().getExecutionTime());
            }
            final AtomicBoolean lastShownNodeDetected = new AtomicBoolean(false);
            List<FlowNodeInstanceDetailResp> route = FlowInstanceUtil.getExecutionRoute(flowInstance, nodeInstances -> {
                if (nodeInstances.size() == 1) {
                    return nodeInstances.get(0);
                } else if (nodeInstances.size() > 1) {
                    List<BaseFlowNodeInstance> instanceList = nodeInstances.stream().filter(
                            instance -> instance.getStatus() != FlowNodeStatus.CREATED).collect(Collectors.toList());
                    if (instanceList.size() == 1) {
                        return instanceList.get(0);
                    } else if (instanceList.size() == 0) {
                        return null;
                    }
                }
                // The process path analysis here is customized and can only handle the design on the current prd
                throw new InternalServerError("Structure of the flow instance is illegal");
            }).stream().filter(instance -> {
                if (lastShownNodeDetected.get()) {
                    return false;
                }
                if (instance.getNodeType() == FlowNodeType.GATEWAY) {
                    return false;
                } else if (instance.getNodeType() == FlowNodeType.SERVICE_TASK) {
                    return true;
                } else if (instance.getNodeType() == FlowNodeType.APPROVAL_TASK) {
                    if ((!((FlowApprovalInstance) instance).isApproved()
                            && instance.getStatus() == FlowNodeStatus.COMPLETED)
                            || instance.getStatus() == FlowNodeStatus.EXPIRED
                            || instance.getStatus() == FlowNodeStatus.CANCELLED) {
                        /**
                         * 审批不通过，审批过期 以及 审批取消 情况下后续节点不显示
                         */
                        lastShownNodeDetected.set(true);
                    }
                }
                return true;
            }).map(mapper::map).peek(detailResp -> {
                if (detailResp.getOperator() == null && !Objects.equals(detailResp.getAutoApprove(), true)) {
                    detailResp.setOperator(InnerUser.of(flowInstance.getCreatorId(), getUserById, getRolesByUserId));
                }
            }).collect(Collectors.toList());
            resp.setNodeList(route);

            if (this.getTasksByFlowInstanceId == null) {
                return resp;
            }
            Collection<TaskEntity> taskEntities = getTasksByFlowInstanceId.apply(flowInstance.getId());
            if (CollectionUtils.isEmpty(taskEntities)) {
                return resp;
            }
            Verify.notLessThan(taskEntities.size(), 1, "FlowInstanceRelatedTaskEntities");

            TaskEntity taskEntity = taskEntities.stream().filter(t -> t.getTaskType() != TaskType.PRE_CHECK).findFirst()
                    .orElseThrow(() -> new UnexpectedException("not task found"));
            resp.setRiskLevel(getRiskLevel(taskEntity));
            String parameterJson = taskEntity.getParametersJson();
            switch (taskEntity.getTaskType()) {
                case MULTIPLE_ASYNC:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, MultipleDatabaseChangeParameters.class));
                    break;
                case ASYNC:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, DatabaseChangeParameters.class));
                    break;
                case EXPORT:
                case IMPORT:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, DataTransferConfig.class));
                    break;
                case MOCKDATA:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, OdcMockTaskConfig.class));
                    break;
                case SHADOWTABLE_SYNC:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, ShadowTableSyncTaskParameter.class));
                    break;
                case PARTITION_PLAN:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, PartitionPlanConfig.class));
                    break;
                case ALTER_SCHEDULE:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, AlterScheduleParameters.class));
                    break;
                case ONLINE_SCHEMA_CHANGE:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, OnlineSchemaChangeParameters.class));
                    break;
                case EXPORT_RESULT_SET:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, ResultSetExportTaskParameter.class));
                    break;
                case APPLY_PROJECT_PERMISSION:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, ApplyProjectParameter.class));
                    break;
                case APPLY_DATABASE_PERMISSION:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, ApplyDatabaseParameter.class));
                    break;
                case APPLY_TABLE_PERMISSION:
                    resp.setParameters(JsonUtils.fromJson(parameterJson, ApplyTableParameter.class));
                    break;
                case STRUCTURE_COMPARISON:
                    DBStructureComparisonParameter dbStructureComparisonParameter = JsonUtils.fromJson(parameterJson,
                            DBStructureComparisonParameter.class);
                    resp.setParameters(dbStructureComparisonParameter);
                    if (getDatabaseById != null) {
                        resp.setRelatedDatabase(
                                this.getDatabaseById.apply(dbStructureComparisonParameter.getTargetDatabaseId()));
                    }
                    break;
                default:
                    throw new UnsupportedException("Unsupported task type " + taskEntity.getTaskType());
            }
            resp.setType(taskEntity.getTaskType());
            resp.setProgressPercentage(taskEntity.getProgressPercentage());
            if (this.getDatabaseById == null) {
                return resp;
            }
            resp.setDatabase(getDatabaseById.apply(taskEntity.getDatabaseId()));
            return resp;
        }

        private RiskLevel getRiskLevel(@NonNull TaskEntity taskEntity) {
            if (Objects.isNull(taskEntity.getRiskLevelId())) {
                return null;
            }
            return getRiskLevelByRiskLevelId.apply(taskEntity.getRiskLevelId());
        }
    }

    public static FlowInstanceDetailResp withId(@NonNull Long id) {
        FlowInstanceDetailResp resp = new FlowInstanceDetailResp();
        resp.setId(id);
        return resp;
    }

    public static FlowInstanceDetailResp withIdAndType(@NonNull Long id, @NonNull TaskType type) {
        FlowInstanceDetailResp resp = new FlowInstanceDetailResp();
        resp.setId(id);
        resp.setType(type);
        return resp;
    }

}
