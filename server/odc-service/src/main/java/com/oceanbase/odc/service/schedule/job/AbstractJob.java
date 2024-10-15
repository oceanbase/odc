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
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.PublishJobParams;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/10/15 10:23
 * @Descripition:
 */


@Slf4j
public class AbstractJob implements OdcJob {

    protected JobExecutionContext context;

    @Override
    public void execute(JobExecutionContext context) {

    }

    @Override
    public void before(JobExecutionContext context) {
        this.context = context;
        log.info("Job will be executed,scheduleTaskId={},scheduleId={}", getScheduleTaskId(), getScheduleId());
    }

    @Override
    public void after(JobExecutionContext context) {
        log.info("Job executed finished,scheduleTaskId={},scheduleId={}", getScheduleTaskId(), getScheduleId());
        this.context = null;
    }

    @Override
    public void interrupt() {

    }

    public Long getScheduleId() {
        return context == null ? null : ScheduleTaskUtils.getScheduleId(context);
    }

    public Long getScheduleTaskId() {
        return context == null ? null : ScheduleTaskUtils.getScheduleTaskId(context);
    }


    public void publishJob(PublishJobParams jobParams) {
        log.info("Start to publish job,cloudProvider={},region={}", jobParams.getCloudProvider(),
                jobParams.getRegion());
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON,
                JsonUtils.toJson(jobParams.getTaskParametersJson()));
        if (jobParams.getTimeoutMillis() != null) {
            jobData.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS,
                    jobParams.getTimeoutMillis().toString());
        }
        Map<String, String> jobProperties = new HashMap<>();
        if (jobParams.getCloudProvider() != null) {
            JobPropertiesUtils.setCloudProvider(jobProperties, jobParams.getCloudProvider());
            JobPropertiesUtils.setRegionName(jobProperties, jobParams.getRegion());
        }
        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(1);
        jobProperties.putAll(singleJobProperties.toJobProperties());
        DefaultJobDefinition jobDefinition = DefaultJobDefinition.builder().jobClass(jobParams.getJobClass())
                .jobType(jobParams.getJobType())
                .jobParameters(jobData)
                .jobProperties(jobProperties)
                .build();
        Long jobId = SpringContextUtil.getBean(JobScheduler.class).scheduleJobNow(jobDefinition);
        SpringContextUtil.getBean(ScheduleTaskService.class).updateJobId(getScheduleTaskId(), jobId);
        log.info("Publish DLM job to task framework succeed,scheduleTaskId={},jobIdentity={}", getScheduleTaskId(),
                jobId);
    }

}
