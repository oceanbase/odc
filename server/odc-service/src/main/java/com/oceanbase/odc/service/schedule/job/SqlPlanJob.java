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
package com.oceanbase.odc.service.schedule.job;

import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/11/15 16:58
 * @Descripition:
 */
@Slf4j
public class SqlPlanJob implements OdcJob {

    @Override
    public void execute(JobExecutionContext context) {

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        ScheduleEntity scheduleEntity = JSON.parseObject(JSON.toJSONString(jobDataMap), ScheduleEntity.class);
        if (!scheduleEntity.getAllowConcurrent()) {
            ScheduleService scheduleService = SpringContextUtil.getBean(ScheduleService.class);
            if (scheduleService.hasExecutingAsyncTask(scheduleEntity)) {
                log.info("Concurrent execution is not allowed and wait for next time,job key={},fire time={}",
                        context.getJobDetail().getKey(), context.getFireTime());
                return;
            }
        }

        DatabaseChangeParameters taskParameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DatabaseChangeParameters.class);
        taskParameters.setParentJobType(JobType.SQL_PLAN);
        CreateFlowInstanceReq flowInstanceReq = new CreateFlowInstanceReq();
        flowInstanceReq.setParameters(taskParameters);
        flowInstanceReq.setTaskType(TaskType.ASYNC);
        flowInstanceReq.setParentFlowInstanceId(Long.parseLong(context.getJobDetail().getKey().getName()));
        flowInstanceReq.setDatabaseId(scheduleEntity.getDatabaseId());

        FlowInstanceService flowInstanceService = SpringContextUtil.getBean(FlowInstanceService.class);
        List<FlowInstanceDetailResp> flowInstance = flowInstanceService.createWithoutApprovalNode(
                flowInstanceReq);
        if (flowInstance.isEmpty()) {
            log.warn("Create sql plan subtask failed.");
        } else {
            log.info("Create sql plan subtask success,flowInstanceId={}", flowInstance.get(0).getId());
        }

    }

    @Override
    public void before(JobExecutionContext context) {

    }

    @Override
    public void after(JobExecutionContext context) {

    }

    @Override
    public void interrupt() {
        throw new UnsupportedException();
    }

}
