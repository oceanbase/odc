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
package com.oceanbase.odc.service.task.schedule.daemon.v2;

import java.text.MessageFormat;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.resource.manager.TaskResourceManager;
import com.oceanbase.odc.service.task.service.TransactionManager;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * manage resource for task. resource strategy should be implement here
 *
 * @author longpeng.zlp
 * @date 2024-12-17
 */
@Slf4j
@DisallowConcurrentExecution
public class ManagerResourceJobV2 implements Job {


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        // safe check
        if (!configuration.getTaskFrameworkProperties().isEnableTaskSupervisorAgent()) {
            return;
        }
        // scan terminate job
        processTaskResource(configuration);
        processRealResource(configuration);
    }

    private void processTaskResource(JobConfiguration configuration) {
        TaskResourceManager taskResourceManager = configuration.getTaskResourceManager();
        TransactionManager transactionManager = configuration.getTransactionManager();
        try {
            taskResourceManager.execute(transactionManager);
        } catch (Throwable e) {
            log.warn("process task resource failed cause", e);
        }
    }

    private void processRealResource(JobConfiguration configuration) {
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        if (!(taskFrameworkProperties.getRunMode() == TaskRunMode.K8S)) {
            return;
        }
        Page<ResourceEntity> resources = configuration.getTaskFrameworkService().findAbandonedResource(0,
                taskFrameworkProperties.getSingleFetchDestroyExecutorJobRows());
        resources.forEach(resource -> {
            try {
                destroyResource(configuration, resource);
            } catch (Throwable e) {
                log.warn("Try to destroy failed, jobId={}.", resource.getId(), e);
            }
        });
    }

    private void destroyResource(JobConfiguration configuration, ResourceEntity resourceEntity) {
        configuration.getTransactionManager().doInTransactionWithoutResult(() -> {
            ResourceID resourceID = new ResourceID(new ResourceLocation(resourceEntity.getRegion(),
                    resourceEntity.getGroupName()), resourceEntity.getResourceType(), resourceEntity.getNamespace(),
                    resourceEntity.getResourceName());
            try {
                configuration.getResourceManager().destroy(resourceID);
            } catch (Throwable e) {
                log.warn("DestroyResourceJob destroy resource = {} failed", resourceEntity, e);
                JobUtils.alarmResourceEvent(resourceEntity, AlarmEventNames.DESTROY_RESOURCE_FAILED,
                        MessageFormat.format("Job resource destroy failed, resourceID={0}, message={1}",
                                resourceEntity.getId(), e.getMessage()));
                throw new TaskRuntimeException(e);
            }
            log.info("Job destroy resource succeed, resource={}", resourceEntity);
        });
    }
}
