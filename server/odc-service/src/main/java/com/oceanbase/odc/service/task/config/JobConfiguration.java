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

package com.oceanbase.odc.service.task.config;

import org.quartz.Scheduler;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.dispatch.JobDispatcher;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.resource.SupervisorAgentAllocator;
import com.oceanbase.odc.service.task.resource.manager.TaskResourceManager;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;
import com.oceanbase.odc.service.task.schedule.StartJobRateLimiter;
import com.oceanbase.odc.service.task.schedule.TaskFrameworkDisabledHandler;
import com.oceanbase.odc.service.task.schedule.provider.HostUrlProvider;
import com.oceanbase.odc.service.task.schedule.provider.JobImageNameProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.service.TransactionManager;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisorJobCaller;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
public interface JobConfiguration {
    TaskFrameworkEnabledProperties getTaskFrameworkEnabledProperties();

    TaskFrameworkProperties getTaskFrameworkProperties();

    CloudEnvConfigurations getCloudEnvConfigurations();

    ResourceManager getResourceManager();

    TaskService getTaskService();

    ConnectionService getConnectionService();

    Scheduler getDaemonScheduler();

    /**
     * scheduler for task supervisor scheduler
     * 
     * @return
     */
    Scheduler getTaskSupervisorScheduler();

    /**
     * old job dispatcher
     * 
     * @return
     */
    JobDispatcher getJobDispatcher();

    /**
     * task job caller based on task supervisor
     * 
     * @return
     */
    TaskSupervisorJobCaller getTaskSupervisorJobCaller();

    HostUrlProvider getHostUrlProvider();

    TaskFrameworkService getTaskFrameworkService();

    TaskExecutorClient getTaskExecutorClient();

    EventPublisher getEventPublisher();

    JobImageNameProvider getJobImageNameProvider();

    TransactionManager getTransactionManager();

    StartJobRateLimiter getStartJobRateLimiter();

    TaskFrameworkDisabledHandler getTaskFrameworkDisabledHandler();

    JasyptEncryptorConfigProperties getJasyptEncryptorConfigProperties();

    HostProperties getHostProperties();

    JobCredentialProvider getJobCredentialProvider();

    /**
     * get task resource manage
     * 
     * @return
     */
    TaskResourceManager getTaskResourceManager();

    /**
     * resource allocator for task supervisor
     * 
     * @return
     */
    SupervisorAgentAllocator getSupervisorAgentAllocator();
}
