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

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.core.shared.constant.FlowStatus;

/**
 * {@link org.springframework.data.jpa.domain.Specification} object for JPA query, used to construct
 * predicate logic
 *
 * @author yh263208
 * @date 2022-02-07 14:14
 * @since ODC_release_3.3.0
 */
public class FlowInstanceSpecs {

    private static final String FLOW_INSTANCE_ID_NAME = "id";
    private static final String FLOW_INSTANCE_NAME_NAME = "name";
    private static final String FLOW_INSTANCE_CREATOR_ID_NAME = "creatorId";
    private static final String FLOW_INSTANCE_ORGANIZATION_ID_NAME = "organizationId";
    private static final String FLOW_INSTANCE_STATUS_NAME = "status";
    private static final String FLOW_INSTANCE_PROCESS_INSTANCE_ID_NAME = "processInstanceId";
    private static final String FLOW_INSTANCE_PROCESS_DEFINITION_ID_NAME = "processDefinitionId";
    private static final String FLOW_INSTANCE_CREATE_TIME_NAME = "createTime";

    private static final String FLOW_INSTANCE_PARENT_INSTANCE_ID = "parentInstanceId";

    private static final String FLOW_INSTANCE_PROJECT_ID = "projectId";

    public static Specification<FlowInstanceEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_ID_NAME, id);
    }

    public static Specification<FlowInstanceEntity> idIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_ID_NAME, ids);
    }

    public static Specification<FlowInstanceEntity> processInstanceIdEquals(String id) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_PROCESS_INSTANCE_ID_NAME, id);
    }

    public static Specification<FlowInstanceEntity> processDefinitionIdEquals(String id) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_PROCESS_DEFINITION_ID_NAME, id);
    }

    public static Specification<FlowInstanceEntity> nameEquals(String name) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_NAME_NAME, name);
    }

    public static Specification<FlowInstanceEntity> nameLike(String nameLike) {
        return SpecificationUtil.columnLike(FLOW_INSTANCE_NAME_NAME, nameLike);
    }

    public static Specification<FlowInstanceEntity> creatorIdEqual(Long userId) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_CREATOR_ID_NAME, userId);
    }

    public static Specification<FlowInstanceEntity> creatorIdIn(Collection<Long> userIds) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_CREATOR_ID_NAME, userIds);
    }

    public static Specification<FlowInstanceEntity> organizationIdEquals(Long organizationId) {
        return SpecificationUtil.columnEqual(FLOW_INSTANCE_ORGANIZATION_ID_NAME, organizationId);
    }

    public static Specification<FlowInstanceEntity> createTimeBefore(Date time) {
        return SpecificationUtil.columnBefore(FLOW_INSTANCE_CREATE_TIME_NAME, time);
    }

    public static Specification<FlowInstanceEntity> createTimeLate(Date time) {
        return SpecificationUtil.columnLate(FLOW_INSTANCE_CREATE_TIME_NAME, time);
    }

    public static Specification<FlowInstanceEntity> statusIn(Collection<FlowStatus> statuses) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_STATUS_NAME, statuses);
    }

    public static Specification<FlowInstanceEntity> parentInstanceIdIn(Collection<Long> parentInstanceId) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_PARENT_INSTANCE_ID, parentInstanceId);
    }

    public static Specification<FlowInstanceEntity> projectIdIn(Collection<Long> projectIds) {
        return SpecificationUtil.columnIn(FLOW_INSTANCE_PROJECT_ID, projectIds);
    }
}
