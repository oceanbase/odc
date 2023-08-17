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

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;

/**
 * {@link org.springframework.data.jpa.domain.Specification} for {@link UserTaskInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-07 15:54
 * @since ODC_release_3.3.0
 */
public class UserTaskInstanceSpecs {

    private static final String USER_TASK_ID_NAME = "id";
    private static final String USER_TASK_APPROVAL_STATUS_NAME = "approved";
    private static final String USER_TASK_OPERATOR_NAME = "operatorId";
    private static final String USER_TASK_FLOW_INSTANCE_NAME = "flowInstanceId";

    public static Specification<UserTaskInstanceEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(USER_TASK_ID_NAME, id);
    }

    public static Specification<UserTaskInstanceEntity> flowInstanceIdEquals(Long id) {
        return SpecificationUtil.columnEqual(USER_TASK_FLOW_INSTANCE_NAME, id);
    }

    public static Specification<UserTaskInstanceEntity> flowInstanceIdIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn(USER_TASK_FLOW_INSTANCE_NAME, ids);
    }

    public static Specification<UserTaskInstanceEntity> operatorIdEquals(Long id) {
        return SpecificationUtil.columnEqual(USER_TASK_OPERATOR_NAME, id);
    }

    public static Specification<UserTaskInstanceEntity> approvaled() {
        return SpecificationUtil.columnEqual(USER_TASK_APPROVAL_STATUS_NAME, true);
    }

    public static Specification<UserTaskInstanceEntity> disApprovaled() {
        return SpecificationUtil.columnEqual(USER_TASK_APPROVAL_STATUS_NAME, false);
    }

}
