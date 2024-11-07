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
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableService;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.LogicalDatabaseChangeParameters;
import com.oceanbase.odc.service.schedule.model.PublishLogicalDatabaseChangeReq;
import com.oceanbase.odc.service.task.base.logicdatabasechange.LogicalDatabaseChangeTask;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.TaskDescription;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 16:08
 * @Description: []
 */
@Slf4j
public class LogicalDatabaseChangeJob implements OdcJob {
    public final ScheduleTaskRepository scheduleTaskRepository;
    public final ScheduleService scheduleService;
    private final DatabaseService databaseService;
    public final TaskFrameworkEnabledProperties taskFrameworkProperties;
    private final ConnectionService connectionService;
    private final LogicalDatabaseService logicalDatabaseService;
    private final LogicalTableService logicalTableService;
    private final ScheduleRepository scheduleRepository;
    private JobScheduler jobScheduler;

    public LogicalDatabaseChangeJob() {
        this.scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        this.scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        this.databaseService = SpringContextUtil.getBean(DatabaseService.class);
        this.taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        this.connectionService = SpringContextUtil.getBean(ConnectionService.class);
        this.logicalDatabaseService = SpringContextUtil.getBean(LogicalDatabaseService.class);
        this.logicalTableService = SpringContextUtil.getBean(LogicalTableService.class);
        this.scheduleRepository = SpringContextUtil.getBean(ScheduleRepository.class);

        if (taskFrameworkProperties.isEnabled()) {
            jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        }
    }

    @Override
    public void execute(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        String scheduleId = taskEntity.getJobName();
        ScheduleEntity scheduleEntity =
                scheduleRepository.findById(Long.parseLong(scheduleId)).orElseThrow(() -> new IllegalArgumentException(
                        "Schedule not found, scheduleId=" + scheduleId));
        LogicalDatabaseChangeParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                LogicalDatabaseChangeParameters.class);
        PublishLogicalDatabaseChangeReq req = new PublishLogicalDatabaseChangeReq();
        req.setSqlContent(parameters.getSqlContent());
        req.setCreatorId(scheduleEntity.getCreatorId());
        DetailLogicalDatabaseResp logicalDatabaseResp = logicalDatabaseService.detail(parameters.getDatabaseId());
        logicalDatabaseResp.getPhysicalDatabases().stream().forEach(
                database -> database.setDataSource(
                        connectionService.getForConnectionSkipPermissionCheck(database.getDataSource().getId())));
        req.setLogicalDatabaseResp(logicalDatabaseResp);
        req.setDelimiter(parameters.getDelimiter());
        req.setTimeoutMillis(parameters.getTimeoutMillis());
        req.setScheduleTaskId(taskEntity.getId());
        Long jobId = publishJob(req, parameters.getTimeoutMillis());
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
        log.info("Publish data-archive job to task framework succeed,scheduleTaskId={},jobIdentity={}",
                taskEntity.getId(), jobId);
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

    }

    public Long publishJob(PublishLogicalDatabaseChangeReq publishReq, Long timeoutMillis) {
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY,
                JobUtils.toJson(publishReq));
        if (timeoutMillis != null) {
            jobData.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS, timeoutMillis.toString());
        }
        Map<String, String> jobProperties = new HashMap<>();
        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(1);
        jobProperties.putAll(singleJobProperties.toJobProperties());

        DefaultJobDefinition jobDefinition = DefaultJobDefinition.builder().jobClass(LogicalDatabaseChangeTask.class)
                .jobType(TaskDescription.LOGICAL_DATABASE_CHANGE.getType())
                .jobParameters(jobData)
                .jobProperties(jobProperties)
                .build();
        return jobScheduler.scheduleJobNow(jobDefinition);
    }
}
