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

import java.util.Map;

import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.PublishJobParams;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.DataArchiveTask;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/26 20:05
 * @Descripition:
 */
@Slf4j
public abstract class AbstractDlmJob extends AbstractJob {

    public final ScheduleTaskService scheduleTaskService;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final DlmLimiterService limiterService;

    public JobScheduler jobScheduler = null;

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;

    public final TaskFrameworkService taskFrameworkService;


    public AbstractDlmJob() {
        scheduleTaskService = SpringContextUtil.getBean(ScheduleTaskService.class);
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

    public void publishJob(DLMJobReq params, Long timeoutMillis, Long srcDatabaseId) {
        Map<String, Object> attributes = getDatasourceAttributesByDatabaseId(srcDatabaseId);
        publishJob(params, timeoutMillis, CloudProvider.fromValue(attributes.get("cloudProvider").toString()),
                attributes.get("region").toString());
    }

    public void publishJob(DLMJobReq parameters, Long timeoutMillis, CloudProvider provider, String region) {

        PublishJobParams publishJobParams = new PublishJobParams();
        publishJobParams.setTaskParametersJson(JobUtils.toJson(parameters));
        publishJobParams.setJobType("DLM");
        publishJobParams.setJobClass(DataArchiveTask.class);
        publishJobParams.setTimeoutMillis(timeoutMillis);
        publishJobParams.setCloudProvider(provider);
        publishJobParams.setRegion(region);
        publishJob(publishJobParams);

    }

    public DLMJobReq getDLMJobReq(Long jobId) {
        return JsonUtils.fromJson(JsonUtils.fromJson(
                taskFrameworkService.find(jobId).getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}).get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobReq.class);
    }


    @Override
    public void execute(JobExecutionContext context) {
        executeJob(context);
    }

    public abstract void executeJob(JobExecutionContext context);

    public void onFailure() {
        scheduleTaskService.updateStatusById(getScheduleTaskId(), TaskStatus.FAILED);
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

}
