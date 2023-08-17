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
package com.oceanbase.odc.service.partitionplan;

import java.util.List;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author：tianke
 * @Date: 2022/9/22 11:52
 * @Descripition: flowable 线程中无法创建 ProcessDefinition
 */
public class PartitionPlanSubFlowThread extends Thread {

    private CreateFlowInstanceReq createFlowInstanceReq;
    private FlowInstanceService flowInstanceService;

    private User user;
    @Setter
    @Getter
    protected long userId;

    @Getter
    @Setter
    protected long taskId;

    public PartitionPlanSubFlowThread(String schemaName, Long parentFlowInstanceId,
            Long connectionId, List<String> sqls, FlowInstanceService flowInstanceService, User user) {
        DatabaseChangeParameters taskParameters = new DatabaseChangeParameters();
        taskParameters.setErrorStrategy("ABORT");
        StringBuilder sqlContent = new StringBuilder();
        for (String sql : sqls) {
            sqlContent.append(sql).append("\n");
        }
        taskParameters.setSqlContent(sqlContent.toString());
        CreateFlowInstanceReq flowInstanceReq = new CreateFlowInstanceReq();
        flowInstanceReq.setParameters(taskParameters);
        flowInstanceReq.setTaskType(TaskType.ASYNC);
        flowInstanceReq.setConnectionId(connectionId);
        flowInstanceReq.setParentFlowInstanceId(parentFlowInstanceId);
        flowInstanceReq.setExecutionStrategy(FlowTaskExecutionStrategy.AUTO);
        flowInstanceReq.setDatabaseName(schemaName);
        this.createFlowInstanceReq = flowInstanceReq;
        this.flowInstanceService = flowInstanceService;
        this.user = user;
    }

    @Override
    public void run() {
        SecurityContextUtils.setCurrentUser(user);
        flowInstanceService.create(this.createFlowInstanceReq);
    }

    public Object getResult() {
        return null;
    }

}
