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

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.task.base.dataarchive.DataArchiveTask;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.TaskDescription;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/26 20:05
 * @Descripition:
 */
@Slf4j
public abstract class AbstractDlmJob implements OdcJob {

    public final ScheduleTaskRepository scheduleTaskRepository;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final DlmLimiterService limiterService;

    public JobScheduler jobScheduler = null;

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;

    public final TaskFrameworkService taskFrameworkService;


    public AbstractDlmJob() {
        scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        databaseService = SpringContextUtil.getBean(DatabaseService.class);
        scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        limiterService = SpringContextUtil.getBean(DlmLimiterService.class);
        taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        taskFrameworkService = SpringContextUtil.getBean(TaskFrameworkService.class);
        if (taskFrameworkProperties.isEnabled()) {
            jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        }
    }

    public DataSourceInfo getDataSourceInfo(Long databaseId) {
        Database db = databaseService.detail(databaseId);
        ConnectionConfig config = databaseService.findDataSourceForTaskById(databaseId);
        DataSourceInfo dataSourceInfo = DataSourceInfoMapper.toDataSourceInfo(config, db.getName());
        dataSourceInfo.setDatabaseName(db.getName());
        return dataSourceInfo;
    }

    public Map<String, Object> getDatasourceAttributesByDatabaseId(Long databaseId) {
        return databaseService.findDataSourceForTaskById(databaseId).getAttributes();
    }

    public Long publishJob(DLMJobReq params, Long timeoutMillis, Long srcDatabaseId) {
        Map<String, Object> attributes = getDatasourceAttributesByDatabaseId(srcDatabaseId);

        if (attributes != null && !attributes.isEmpty() && attributes.containsKey("cloudProvider")
                && attributes.containsKey("region")) {
            return publishJob(params, timeoutMillis,
                    CloudProvider.fromValue(attributes.get("cloudProvider").toString()),
                    attributes.get("region").toString());
        } else {
            return publishJob(params, timeoutMillis, null, null);
        }
    }

    public Long publishJob(DLMJobReq parameters, Long timeoutMillis, CloudProvider provider, String region) {
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON,
                JsonUtils.toJson(parameters));
        if (timeoutMillis != null) {
            jobData.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS, timeoutMillis.toString());
        }
        Map<String, String> jobProperties = new HashMap<>();
        if (provider != null && StringUtils.isNotEmpty(region)) {
            JobPropertiesUtils.setCloudProvider(jobProperties, provider);
            JobPropertiesUtils.setRegionName(jobProperties, region);
        } else {
            JobPropertiesUtils.setDefaultCloudProvider(jobProperties);
            JobPropertiesUtils.setDefaultRegionName(jobProperties);
        }
        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(1);
        jobProperties.putAll(singleJobProperties.toJobProperties());

        DefaultJobDefinition jobDefinition = DefaultJobDefinition.builder().jobClass(DataArchiveTask.class)
                .jobType(TaskDescription.DLM.getType())
                .jobParameters(jobData)
                .jobProperties(jobProperties)
                .build();
        return jobScheduler.scheduleJobNow(jobDefinition);
    }

    public DLMJobReq getDLMJobReq(Long jobId) {
        return JsonUtils.fromJson(JsonUtils.fromJson(
                taskFrameworkService.find(jobId).getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}).get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobReq.class);
    }


    @Override
    public void execute(JobExecutionContext context) {
        if (context.getResult() == null) {
            log.warn("Concurrent execute is not allowed,job will be existed.jobKey={}",
                    context.getJobDetail().getKey());
            return;
        }
        executeJob(context);
    }

    public abstract void executeJob(JobExecutionContext context);


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

}
