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
package com.oceanbase.odc.service.schedule.flowtask;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;

/**
 * @Author：tinker
 * @Date: 2024/9/10 17:10
 * @Descripition:
 */

public class DefaultApprovalFlowClient implements ApprovalFlowClient {

    @Override
    public Long create(ScheduleChangeParams params) {
        CreateFlowInstanceReq req = new CreateFlowInstanceReq();
        req.setParentFlowInstanceId(params.getScheduleId());
        req.setTaskType(TaskType.ALTER_SCHEDULE);
        AlterScheduleParameters alterScheduleParameters = new AlterScheduleParameters();
        alterScheduleParameters.setTaskId(params.getScheduleId());
        alterScheduleParameters.setScheduleChangeParams(params);
        alterScheduleParameters.setOperationType(params.getOperationType());
        if (params.getOperationType() == OperationType.CREATE || params.getOperationType() == OperationType.UPDATE) {
            alterScheduleParameters.setTriggerConfig(
                    params.getOperationType() == OperationType.CREATE ? params.getCreateScheduleReq().getTriggerConfig()
                            : params.getUpdateScheduleReq().getTriggerConfig());
        }
        req.setParameters(alterScheduleParameters);
        return SpringContextUtil.getBean(FlowInstanceService.class).createAlterSchedule(req);
    }
}
