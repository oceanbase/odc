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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;
import com.oceanbase.odc.service.task.base.sqlplan.SqlPlanTask;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/11/15 16:58
 * @Descripition:
 */
@Slf4j
public class SqlPlanJob implements OdcJob {

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final ConnectProperties connectProperties;

    public SqlPlanJob() {
        this.taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.connectProperties = SpringContextUtil.getBean(ConnectProperties.class);
    }

    @Override
    public void execute(JobExecutionContext context) {

        if (taskFrameworkProperties.isEnabled()) {
            executeInTaskFramework(context);
            return;
        }

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        ScheduleEntity scheduleEntity = JSON.parseObject(JSON.toJSONString(jobDataMap), ScheduleEntity.class);

        DatabaseChangeParameters taskParameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DatabaseChangeParameters.class);
        taskParameters.setParentScheduleType(ScheduleType.SQL_PLAN);
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

    private void executeInTaskFramework(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        log.info("sql plan job execute in task framework, jobId={}", taskEntity.getJobName());
        SqlPlanParameters sqlPlanParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                SqlPlanParameters.class);
        PublishSqlPlanJobReq parameters = new PublishSqlPlanJobReq();
        parameters.setSqlContent(sqlPlanParameters.getSqlContent());
        parameters.setRetryTimes(sqlPlanParameters.getRetryTimes());
        parameters.setDelimiter(sqlPlanParameters.getDelimiter());
        parameters.setSqlObjectIds(sqlPlanParameters.getSqlObjectIds());
        parameters.setTimeoutMillis(sqlPlanParameters.getTimeoutMillis());
        parameters.setQueryLimit(sqlPlanParameters.getQueryLimit());
        parameters.setErrorStrategy(sqlPlanParameters.getErrorStrategy());
        parameters.setSessionTimeZone(connectProperties.getDefaultTimeZone());
        Map<String, String> jobData = new HashMap<>();
        ConnectionConfig connectionConfig = databaseService.findDataSourceForTaskById(
                sqlPlanParameters.getDatabaseId());
        jobData.put(JobParametersKeyConstants.CONNECTION_CONFIG, JsonUtils.toJson(connectionConfig));
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, JsonUtils.toJson(parameters));
        DefaultJobDefinition.builder().jobClass(SqlPlanTask.class)
                .jobType("SQL_PLAN")
                .jobParameters(jobData)
                .build();

    }

    @Override
    public void before(JobExecutionContext context) {
        scheduleService.refreshScheduleStatus(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void after(JobExecutionContext context) {
        scheduleService.refreshScheduleStatus(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void interrupt() {
        throw new UnsupportedException();
    }

}
