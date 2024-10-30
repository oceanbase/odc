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
package com.oceanbase.odc.service.task.schedule.daemon;

import java.text.MessageFormat;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-22
 * @since 4.2.4
 */
@Slf4j
@DisallowConcurrentExecution
public class DestroyResourceJob implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        JobConfigurationValidator.validComponent();

        // scan terminate job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        Page<ResourceEntity> resources = taskFrameworkService.findAbandonedResource(0,
                taskFrameworkProperties.getSingleFetchDestroyExecutorJobRows());
        resources.forEach(a -> {
            try {
                destroyResource(a);
            } catch (Throwable e) {
                log.warn("Try to destroy failed, jobId={}.", a.getId(), e);
            }
        });
    }

    private void destroyResource(ResourceEntity resourceEntity) {
        getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
            ResourceID resourceID = new ResourceID(new ResourceLocation(resourceEntity.getRegion(),
                    resourceEntity.getGroupName()), resourceEntity.getResourceType(), resourceEntity.getNamespace(),
                    resourceEntity.getResourceName());
            try {
                configuration.getResourceManager().destroy(resourceID);
            } catch (Throwable e) {
                log.warn("DestroyResourceJob destroy resource = {} failed", resourceEntity, e);
                if (e.getMessage() != null &&
                        !e.getMessage().startsWith(JobConstants.ODC_EXECUTOR_CANNOT_BE_DESTROYED)) {
                    AlarmUtils.alarm(AlarmEventNames.TASK_EXECUTOR_DESTROY_FAILED,
                            MessageFormat.format("Job executor destroy failed, resource={0}, message={1}",
                                    resourceEntity, e.getMessage()));
                }
                throw new TaskRuntimeException(e);
            }
            log.info("Job destroy resource succeed, resource={}", resourceEntity);
        });
    }


    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
