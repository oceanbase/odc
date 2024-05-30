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

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * @author wenniu.ly
 * @date 2022/2/9
 */

@Data
public class FlowNodeInstanceDetailResp {

    private Long id;
    private InnerUser operator;
    private FlowNodeStatus status;
    private TaskType taskType;
    private FlowNodeType nodeType;
    private List<InnerUser> candidates;
    private String comment;
    private Boolean autoApprove;
    private Date createTime;
    private Date completeTime;
    private Date deadlineTime;
    private String externalApprovalName;
    private String externalFlowInstanceUrl;
    private Integer issueCount;
    private List<UnauthorizedDBResource> unauthorizedDBResources;
    private Boolean preCheckOverLimit;

    public static FlowNodeInstanceMapper mapper() {
        return new FlowNodeInstanceMapper();
    }

    public static class FlowNodeInstanceMapper {
        private Function<Long, UserEntity> getUserById = null;
        private Function<Long, TaskEntity> getTaskById = null;
        private Function<Long, List<RoleEntity>> getRolesByUserId = null;
        private Function<Long, List<UserEntity>> getCandidatesByApprovalId = null;
        private Function<ExternalApproval, String> getExternalUrlByExternalId = null;
        private Function<Long, String> getExternalApprovalNameById = null;

        public FlowNodeInstanceMapper withGetExternalApprovalNameById(
                @NonNull Function<Long, String> getExternalApprovalNameById) {
            this.getExternalApprovalNameById = getExternalApprovalNameById;
            return this;
        }

        public FlowNodeInstanceMapper withGetExternalUrlByExternalId(
                @NonNull Function<ExternalApproval, String> getExternalUrlByExternalId) {
            this.getExternalUrlByExternalId = getExternalUrlByExternalId;
            return this;
        }

        public FlowNodeInstanceMapper withGetUserById(@NonNull Function<Long, UserEntity> getUserById) {
            this.getUserById = getUserById;
            return this;
        }

        public FlowNodeInstanceMapper withGetRolesByUserId(@NonNull Function<Long, List<RoleEntity>> getRolesByUserId) {
            this.getRolesByUserId = getRolesByUserId;
            return this;
        }

        public FlowNodeInstanceMapper withGetCandidatesByApprovalId(
                @NonNull Function<Long, List<UserEntity>> getCandidatesByApprovalId) {
            this.getCandidatesByApprovalId = getCandidatesByApprovalId;
            return this;
        }

        public FlowNodeInstanceMapper withGetTaskById(@NonNull Function<Long, TaskEntity> getTaskById) {
            this.getTaskById = getTaskById;
            return this;
        }

        public FlowNodeInstanceDetailResp map(@NonNull BaseFlowNodeInstance instance) {
            if (instance instanceof FlowApprovalInstance) {
                return map((FlowApprovalInstance) instance);
            } else if (instance instanceof FlowTaskInstance) {
                return map((FlowTaskInstance) instance);
            } else if (instance instanceof FlowGatewayInstance) {
                return map((FlowGatewayInstance) instance);
            }
            return commonMap(instance);
        }

        public FlowNodeInstanceDetailResp map(@NonNull FlowGatewayInstance instance) {
            return commonMap(instance);
        }

        public FlowNodeInstanceDetailResp map(@NonNull FlowTaskInstance instance) {
            FlowNodeInstanceDetailResp resp = commonMap(instance);
            resp.setTaskType(instance.getTaskType());
            TaskEntity taskEntity = null;
            if (this.getTaskById != null && instance.getTargetTaskId() != null) {
                taskEntity = getTaskById.apply(instance.getTargetTaskId());
            }
            if (instance.getStatus() == FlowNodeStatus.PENDING || instance.getStatus() == FlowNodeStatus.EXPIRED) {
                ExecutionStrategyConfig strategyConfig = instance.getStrategyConfig();
                if (strategyConfig.getStrategy() == FlowTaskExecutionStrategy.TIMER) {
                    resp.setDeadlineTime(strategyConfig.getExecutionTime());
                } else {
                    resp.setDeadlineTime(new Date(instance.getUpdateTime().getTime() + TimeUnit.MILLISECONDS
                            .convert(strategyConfig.getPendingExpireIntervalSeconds(), TimeUnit.SECONDS)));
                }
            } else if (instance.getStatus() == FlowNodeStatus.EXECUTING && taskEntity != null) {
                long expireInterval = TimeUnit.MILLISECONDS.convert(
                        taskEntity.getExecutionExpirationIntervalSeconds(), TimeUnit.SECONDS);
                resp.setDeadlineTime(new Date(instance.getUpdateTime().getTime() + expireInterval));
            }
            if (taskEntity != null && taskEntity.getTaskType() == TaskType.PRE_CHECK) {
                PreCheckTaskResult result =
                        JsonUtils.fromJson(taskEntity.getResultJson(), PreCheckTaskResult.class);
                if (result != null) {
                    resp.setPreCheckOverLimit(result.isOverLimit());
                    if (Objects.nonNull(result.getSqlCheckResult())) {
                        resp.setIssueCount(result.getSqlCheckResult().getIssueCount());
                    }
                    if (Objects.nonNull(result.getPermissionCheckResult())) {
                        resp.setUnauthorizedDBResources(result.getPermissionCheckResult().getUnauthorizedDBResources());
                    }
                }
            }
            return resp;
        }

        public FlowNodeInstanceDetailResp map(@NonNull FlowApprovalInstance instance) {
            FlowNodeInstanceDetailResp resp = commonMap(instance);
            resp.setAutoApprove(instance.isAutoApprove());
            if (Objects.nonNull(instance.getExternalApprovalId())) {
                resp.setExternalApprovalName(getExternalApprovalNameById.apply(instance.getExternalApprovalId()));
                if (StringUtils.isNotBlank(instance.getExternalFlowInstanceId())) {
                    resp.setExternalFlowInstanceUrl(getExternalUrlByExternalId.apply(ExternalApproval.builder()
                            .approvalId(instance.getExternalApprovalId())
                            .instanceId(instance.getExternalFlowInstanceId()).build()));
                }
            }
            if (instance.getStatus().isFinalStatus()) {
                if (!instance.isApproved() && instance.getStatus() == FlowNodeStatus.COMPLETED) {
                    resp.setStatus(FlowNodeStatus.FAILED);
                }
                resp.setComment(instance.getComment());
                if (instance.getOperatorId() != null) {
                    resp.setOperator(InnerUser.of(instance.getOperatorId(), getUserById, getRolesByUserId));
                }
            }
            if (this.getCandidatesByApprovalId != null) {
                List<UserEntity> candidates = this.getCandidatesByApprovalId.apply(instance.getId());
                if (CollectionUtils.isNotEmpty(candidates)) {
                    resp.setCandidates(candidates.stream()
                            .map(userEntity -> InnerUser.of(userEntity.getId(), getUserById, getRolesByUserId))
                            .collect(Collectors.toList()));
                }
            }
            if (instance.getStatus() == FlowNodeStatus.EXECUTING) {
                long expireInterval =
                        TimeUnit.MILLISECONDS.convert(instance.getExpireIntervalSeconds(), TimeUnit.SECONDS);
                resp.setDeadlineTime(new Date(instance.getUpdateTime().getTime() + expireInterval));
            }
            return resp;
        }

        private FlowNodeInstanceDetailResp commonMap(@NonNull BaseFlowNodeInstance instance) {
            FlowNodeInstanceDetailResp resp = new FlowNodeInstanceDetailResp();
            resp.setStatus(instance.getStatus());
            resp.setId(instance.getId());
            resp.setNodeType(instance.getNodeType());
            resp.setCreateTime(instance.getCreateTime());
            if (instance.getStatus().isFinalStatus()) {
                resp.setCompleteTime(instance.getUpdateTime());
            }
            return resp;
        }

        @Data
        @Builder
        public static class ExternalApproval {
            private String instanceId;
            private Long approvalId;
        }
    }

}
