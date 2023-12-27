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

package com.oceanbase.odc.metadb.flow;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.validation.constraints.NotNull;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

/**
 * {@link FlowInstanceViewEntity}
 *
 * @author jingtian
 * @date 2023/8/10
 * @since ODC_release_4.2.0
 */
public class FlowInstanceViewSpecs {
    private static final String FLOW_INSTANCE_VIEW_ID_NAME = "id";
    private static final String FLOW_INSTANCE_VIEW_CREATOR_ID_NAME = "creatorId";
    private static final String FLOW_INSTANCE_VIEW_ORGANIZATION_ID_NAME = "organizationId";
    private static final String FLOW_INSTANCE_VIEW_STATUS_NAME = "status";
    private static final String FLOW_INSTANCE_VIEW_CREATE_TIME_NAME = "createTime";
    private static final String FLOW_INSTANCE_VIEW_PROJECT_ID = "projectId";
    private static final String FLOW_INSTANCE_VIEW_TASK_TYPE = "taskType";
    private static final String FLOW_INSTANCE_VIEW_2_FLOW_INSTANCE_APPROVAL_VIEW = "approvals";
    private static final String FLOW_INSTANCE_APPROVAL_VIEW_ROLE_IDENTIFIER = "resourceRoleIdentifier";
    private static final String FLOW_INSTANCE_APPROVAL_VIEW_STATUS = "status";


    public static Specification<FlowInstanceViewEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_VIEW_ID_NAME, id);
    }

    public static Specification<FlowInstanceViewEntity> creatorIdIn(Collection<Long> userIds) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_VIEW_CREATOR_ID_NAME, userIds);
    }

    public static Specification<FlowInstanceViewEntity> creatorIdEquals(Long userId) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_VIEW_CREATOR_ID_NAME, userId);
    }

    public static Specification<FlowInstanceViewEntity> organizationIdEquals(Long organizationId) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_VIEW_ORGANIZATION_ID_NAME, organizationId);
    }

    public static Specification<FlowInstanceViewEntity> statusIn(Collection<FlowStatus> statuses) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_VIEW_STATUS_NAME, statuses);
    }

    public static Specification<FlowInstanceViewEntity> createTimeLate(Date time) {
        return SpecificationUtil.columnLate(FLOW_INSTANCE_VIEW_CREATE_TIME_NAME, time);
    }

    public static Specification<FlowInstanceViewEntity> createTimeBefore(Date time) {
        return SpecificationUtil.columnBefore(FLOW_INSTANCE_VIEW_CREATE_TIME_NAME, time);
    }

    public static Specification<FlowInstanceViewEntity> projectIdEquals(Long projectId) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_VIEW_PROJECT_ID, projectId);
    }

    public static Specification<FlowInstanceViewEntity> projectIdIn(Set<Long> projectIds) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_VIEW_PROJECT_ID, projectIds);
    }


    public static Specification<FlowInstanceViewEntity> taskTypeEquals(TaskType taskType) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_VIEW_TASK_TYPE, taskType);
    }

    public static Specification<FlowInstanceViewEntity> taskTypeIn(Collection<TaskType> taskTypes) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_VIEW_TASK_TYPE, taskTypes);
    }

    public static Specification<FlowInstanceViewEntity> leftJoinFlowInstanceApprovalView(
            @NotNull Set<String> resourceRoleIdentifiers, Long creatorId, Set<FlowNodeStatus> statusList) {
        return (root, query, builder) -> {
            Join<FlowInstanceViewEntity, FlowInstanceApprovalViewEntity> join =
                    root.join(FLOW_INSTANCE_VIEW_2_FLOW_INSTANCE_APPROVAL_VIEW, JoinType.LEFT);
            query.distinct(true);
            Predicate rolePredicate = join.get(FLOW_INSTANCE_APPROVAL_VIEW_ROLE_IDENTIFIER).in(resourceRoleIdentifiers);
            Predicate statusPredicate = join.get(FLOW_INSTANCE_APPROVAL_VIEW_STATUS).in(statusList);
            if (Objects.isNull(creatorId)) {
                return builder.and(rolePredicate, statusPredicate);
            }
            Predicate creatorPredicate = builder.equal(root.get(FLOW_INSTANCE_VIEW_CREATOR_ID_NAME), creatorId);
            return builder.or(builder.and(rolePredicate, statusPredicate), creatorPredicate);
        };
    }

}
