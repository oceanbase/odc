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
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
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
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.SqlPlanTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/11/15 16:58
 * @Descripition:
 */
@Slf4j
public class SqlPlanJob implements OdcJob {

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;
    public final ScheduleTaskRepository scheduleTaskRepository;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final ConnectProperties connectProperties;
    public final JobScheduler jobScheduler;


    public SqlPlanJob() {
        this.taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        this.scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.connectProperties = SpringContextUtil.getBean(ConnectProperties.class);
        this.jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
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
        Database database = databaseService.detail(sqlPlanParameters.getDatabaseId());
        ConnectionConfig dataSource = database.getDataSource();
        dataSource.setDefaultSchema(database.getName());
        jobData.put(JobParametersKeyConstants.CONNECTION_CONFIG, JobUtils.toJson(dataSource));
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, JobUtils.toJson(parameters));

        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(1);
        Map<String, String> jobProperties = new HashMap<>(singleJobProperties.toJobProperties());

        Map<String, Object> attributes = getDatasourceAttributesByDatabaseId(sqlPlanParameters.getDatabaseId());
        if (attributes != null && !attributes.isEmpty() && attributes.containsKey("cloudProvider")
                && attributes.containsKey("region")) {
            JobPropertiesUtils.setCloudProvider(jobProperties,
                    CloudProvider.fromValue(attributes.get("cloudProvider").toString()));
            JobPropertiesUtils.setRegionName(jobProperties, attributes.get("region").toString());
        }
        DefaultJobDefinition jd = DefaultJobDefinition.builder().jobClass(SqlPlanTask.class)
                .jobType("SQL_PLAN")
                .jobParameters(jobData)
                .jobProperties(jobProperties)
                .build();

        Long jobId = jobScheduler.scheduleJobNow(jd);
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        log.info("Publish sql plan job to task framework success, scheduleTaskId={}, jobId={}",
                taskEntity.getId(),
                jobId);
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
