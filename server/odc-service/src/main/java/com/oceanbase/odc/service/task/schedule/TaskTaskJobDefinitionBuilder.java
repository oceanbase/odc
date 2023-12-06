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

package com.oceanbase.odc.service.task.schedule;

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-11-30
 * @since 4.2.4
 */
public class TaskTaskJobDefinitionBuilder implements JobDefinitionBuilder {

    @Override
    public JobDefinition build(@NonNull JobIdentity jobIdentity, Map<String, String> jobData,
            MisfireStrategy misfireStrategy, TriggerConfig triggerConfig) {

        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        TaskService taskService = configuration.getTaskService();
        TaskEntity taskEntity = taskService.detail(jobIdentity.getSourceId());
        if (taskEntity.getConnectionId() != null) {
            ConnectionConfig config = configuration.getConnectionService()
                    .getForConnectionSkipPermissionCheck(taskEntity.getConnectionId());
            config.setDefaultSchema(taskEntity.getDatabaseName());
            if (jobData == null) {
                jobData = new HashMap<>();
            }
            jobData.put(JobDataMapConstants.META_DB_TASK_PARAMETER, taskEntity.getParametersJson());
            jobData.put(JobDataMapConstants.CONNECTION_CONFIG, JobUtils.toJson(config));
        }
        return DefaultJobDefinition.builder()
                .jobIdentity(jobIdentity)
                .jobData(jobData)
                .misfireStrategy(misfireStrategy)
                .triggerConfig(triggerConfig)
                .build();
    }
}
