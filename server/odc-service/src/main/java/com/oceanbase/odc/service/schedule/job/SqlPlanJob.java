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
import java.util.Map;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.config.OrganizationConfigUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;
import com.oceanbase.odc.service.task.base.sqlplan.SqlExecuteTask;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
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
    public final SystemConfigService systemConfigService;
    public final ConnectionService datasourceService;
    private final ScheduleTaskService scheduleTaskService;
    public final OrganizationConfigUtils organizationConfigUtils;


    public SqlPlanJob() {
        this.taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        this.scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.connectProperties = SpringContextUtil.getBean(ConnectProperties.class);
        this.jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        this.systemConfigService = SpringContextUtil.getBean(SystemConfigService.class);
        this.datasourceService = SpringContextUtil.getBean(ConnectionService.class);
        this.scheduleTaskService = SpringContextUtil.getBean(ScheduleTaskService.class);
        this.organizationConfigUtils = SpringContextUtil.getBean(OrganizationConfigUtils.class);
    }

    @Override
    public void execute(JobExecutionContext context) {
        if (!taskFrameworkProperties.isEnabled()) {
            throw new UnsupportedException("task framework is not enabled");
        }
        executeInTaskFramework(context);
    }

    private void executeInTaskFramework(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        SqlPlanParameters sqlPlanParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                SqlPlanParameters.class);
        PublishSqlExecuteJobReq parameters = new PublishSqlExecuteJobReq();
        parameters.setSqlContent(sqlPlanParameters.getSqlContent());
        parameters.setRetryTimes(sqlPlanParameters.getRetryTimes());
        parameters.setDelimiter(sqlPlanParameters.getDelimiter());
        parameters.setSqlObjectIds(sqlPlanParameters.getSqlObjectIds());
        parameters.setTimeoutMillis(sqlPlanParameters.getTimeoutMillis());
        OrganizationConfigUtils configUtils = SpringContextUtil.getBean(OrganizationConfigUtils.class);
        configUtils.checkQueryLimitValidity(sqlPlanParameters);
        parameters.setQueryLimit(sqlPlanParameters.getQueryLimit());
        parameters.setErrorStrategy(sqlPlanParameters.getErrorStrategy());
        parameters.setSessionTimeZone(connectProperties.getDefaultTimeZone());
        Map<String, String> jobData = new HashMap<>();
        Database database = databaseService.getBasicSkipPermissionCheck(sqlPlanParameters.getDatabaseId());
        ConnectionConfig dataSource = datasourceService.getDecryptedConfig(database.getDataSource().getId());
        dataSource.setDefaultSchema(database.getName());
        jobData.put(JobParametersKeyConstants.CONNECTION_CONFIG, JobUtils.toJson(dataSource));
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, JobUtils.toJson(parameters));
        jobData.put(JobParametersKeyConstants.DEFAULT_MAX_QUERY_LIMIT,
                organizationConfigUtils.getDefaultMaxQueryLimit().toString());

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
        } else {
            JobPropertiesUtils.setDefaultCloudProvider(jobProperties);
            JobPropertiesUtils.setDefaultRegionName(jobProperties);
        }
        DefaultJobDefinition jd = DefaultJobDefinition.builder().jobClass(SqlExecuteTask.class)
                .jobType("SQL_PLAN")
                .jobParameters(jobData)
                .jobProperties(jobProperties)
                .build();

        Long jobId = jobScheduler.scheduleJobNow(jd);
        scheduleTaskService.updateJobIdByTaskIdWithCheckScheduleTaskCancelingStatus(taskEntity.getId(), jobId);
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
