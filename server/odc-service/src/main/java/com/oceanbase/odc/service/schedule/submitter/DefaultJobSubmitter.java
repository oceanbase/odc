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
package com.oceanbase.odc.service.schedule.submitter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.base.dataarchive.DataArchiveTask;
import com.oceanbase.odc.service.task.base.logicdatabasechange.LogicalDatabaseChangeTask;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.TaskDescription;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.tools.migrator.common.exception.UnExpectedException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2025/1/22 14:07
 * @Descripition:
 */
@Slf4j
public class DefaultJobSubmitter implements JobSubmitter {

    @Autowired
    private JobScheduler jobScheduler;

    @Override
    public Long submit(String parametersJson, String type, Long timeoutMillis, CloudProvider provider, String region) {
        TaskDescription taskDescription = TaskDescription.valueOf(type);
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON, parametersJson);
        if (timeoutMillis != null) {
            jobData.put(JobParametersKeyConstants.TASK_EXECUTION_END_TIME_MILLIS,
                    String.valueOf(System.currentTimeMillis() + timeoutMillis));
        }
        Map<String, String> jobProperties = new HashMap<>();
        if (provider != null && StringUtils.isNotEmpty(region)) {
            JobPropertiesUtils.setCloudProvider(jobProperties, provider);
            JobPropertiesUtils.setRegionName(jobProperties, region);
        }
        DefaultJobDefinition jobDefinition =
                DefaultJobDefinition.builder().jobClass(getJobClassByType(taskDescription.getType()))
                        .jobType(taskDescription.getType())
                        .jobParameters(jobData)
                        .jobProperties(jobProperties)
                        .build();
        return getJobScheduler().scheduleJobNow(jobDefinition);
    }

    protected Class<? extends Task<?>> getJobClassByType(String type) {
        TaskDescription taskDescription = TaskDescription.valueOf(type);
        switch (taskDescription) {
            case DLM:
                return DataArchiveTask.class;
            case LOGICAL_DATABASE_CHANGE:
                return LogicalDatabaseChangeTask.class;
            default:
                log.warn("Task type {} is not supported.", type);
                throw new UnExpectedException("Task type is not supported.");
        }
    }

    private JobScheduler getJobScheduler() {
        if (this.jobScheduler == null) {
            jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        }
        return jobScheduler;
    }
}
