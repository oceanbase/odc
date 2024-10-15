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
import java.util.Map;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.PublishJobParams;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.executor.task.SqlPlanTask;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/11/15 16:58
 * @Descripition:
 */
@Slf4j
public class SqlPlanJob extends AbstractJob {

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;
    public final ScheduleTaskRepository scheduleTaskRepository;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final ConnectProperties connectProperties;
    public final JobScheduler jobScheduler;
    public final ConnectionService datasourceService;


    public SqlPlanJob() {
        this.taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        this.scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.connectProperties = SpringContextUtil.getBean(ConnectProperties.class);
        this.jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        this.datasourceService = SpringContextUtil.getBean(ConnectionService.class);
    }

    @Override
    public void execute(JobExecutionContext context) {
        ScheduleTaskType scheduleTaskType = ScheduleTaskUtils.getScheduleTaskType(context);
        if (scheduleTaskType.isExecuteInTaskFramework()) {
            executeInTaskFramework(context);
            return;
        }
        DatabaseChangeParameters taskParameters = ScheduleTaskUtils.getDatabaseChangeTaskParameters(context);
        Schedule schedule = scheduleService.nullSafeGetModelById(ScheduleTaskUtils.getScheduleId(context));
        log.info("Execute sql plan job, scheduleId={}, taskParameters={}", schedule.getId(), taskParameters);
        taskParameters.setParentScheduleType(ScheduleType.SQL_PLAN);
        CreateFlowInstanceReq flowInstanceReq = new CreateFlowInstanceReq();
        flowInstanceReq.setParameters(taskParameters);
        flowInstanceReq.setTaskType(TaskType.ASYNC);
        flowInstanceReq.setParentFlowInstanceId(Long.parseLong(context.getJobDetail().getKey().getName()));
        flowInstanceReq.setDatabaseId(schedule.getDatabaseId());

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
        SqlPlanParameters sqlPlanParameters = ScheduleTaskUtils.getSqlPlanParameters(context);
        PublishSqlPlanJobReq parameters = new PublishSqlPlanJobReq();
        parameters.setSqlContent(sqlPlanParameters.getSqlContent());
        parameters.setRetryTimes(sqlPlanParameters.getRetryTimes());
        parameters.setDelimiter(sqlPlanParameters.getDelimiter());
        parameters.setSqlObjectIds(sqlPlanParameters.getSqlObjectIds());
        parameters.setTimeoutMillis(sqlPlanParameters.getTimeoutMillis());
        parameters.setQueryLimit(sqlPlanParameters.getQueryLimit());
        parameters.setErrorStrategy(sqlPlanParameters.getErrorStrategy());
        parameters.setSessionTimeZone(connectProperties.getDefaultTimeZone());
        Database database = databaseService.getBasicSkipPermissionCheck(sqlPlanParameters.getDatabaseId());
        ConnectionConfig dataSource = datasourceService.getDecryptedConfig(database.getDataSource().getId());
        dataSource.setDefaultSchema(database.getName());
        parameters.setDataSource(dataSource);

        PublishJobParams publishJobParams = new PublishJobParams();
        publishJobParams.setTaskParametersJson(JobUtils.toJson(parameters));
        publishJobParams.setJobType("SQL_PLAN");
        publishJobParams.setJobClass(SqlPlanTask.class);
        publishJobParams.setTimeoutMillis(parameters.getTimeoutMillis());
        Map<String, Object> attributes = getDatasourceAttributesByDatabaseId(sqlPlanParameters.getDatabaseId());
        if (attributes != null && attributes.containsKey("cloudProvider") && attributes.containsKey("region")) {
            publishJobParams.setCloudProvider(CloudProvider.fromValue(attributes.get("cloudProvider").toString()));
            publishJobParams.setRegion(attributes.get("region").toString());
        }
        publishJob(publishJobParams);
    }


    public Map<String, Object> getDatasourceAttributesByDatabaseId(Long databaseId) {
        return databaseService.findDataSourceForTaskById(databaseId).getAttributes();
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
