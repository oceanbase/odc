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
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

/**
 * {@link org.springframework.data.jpa.domain.Specification} for {@link ServiceTaskInstanceEntity}
 *
 * @author yh263208
 * @date 2022-02-15 11:59
 * @since ODC_release_3.3.0
 */
public class ServiceTaskInstanceSpecs {

    private static final String SERVICE_TASK_ID_NAME = "id";
    private static final String SERVICE_TASK_STATUS_NAME = "status";
    private static final String SERVICE_TASK_TARGET_TASK_ID_NAME = "targetTaskId";
    private static final String SERVICE_TASK_FLOW_INSTANCE_NAME = "flowInstanceId";
    private static final String SERVICE_TASK_TASK_TYPE = "taskType";

    public static Specification<ServiceTaskInstanceEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(SERVICE_TASK_ID_NAME, id);
    }

    public static Specification<ServiceTaskInstanceEntity> taskTypeEquals(TaskType taskType) {
        return SpecificationUtil.columnEqual(SERVICE_TASK_TASK_TYPE, taskType);
    }

    public static Specification<ServiceTaskInstanceEntity> targetTaskIdIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn(SERVICE_TASK_TARGET_TASK_ID_NAME, ids);
    }

    public static Specification<ServiceTaskInstanceEntity> flowInstanceIdEquals(Long id) {
        return SpecificationUtil.columnEqual(SERVICE_TASK_FLOW_INSTANCE_NAME, id);
    }

    public static Specification<ServiceTaskInstanceEntity> flowInstanceIdIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn(SERVICE_TASK_FLOW_INSTANCE_NAME, ids);
    }

    public static Specification<ServiceTaskInstanceEntity> statusIn(Collection<FlowNodeStatus> statusList) {
        return SpecificationUtil.columnIn(SERVICE_TASK_STATUS_NAME, statusList);
    }

}
