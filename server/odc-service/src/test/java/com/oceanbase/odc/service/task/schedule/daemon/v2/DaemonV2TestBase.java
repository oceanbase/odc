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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.resource.SupervisorAgentAllocator;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;
import com.oceanbase.odc.service.task.schedule.StartJobRateLimiter;
import com.oceanbase.odc.service.task.schedule.provider.HostUrlProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.service.TransactionManager;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisorJobCaller;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2025/1/6 16:12
 */
public abstract class DaemonV2TestBase {
    protected JobConfiguration configuration;
    protected TaskFrameworkService taskFrameworkService;
    protected TaskFrameworkProperties taskFrameworkProperties;
    protected JobEntity jobEntity;
    protected HostUrlProvider hostUrlProvider;
    protected Page<JobEntity> page;
    protected StartJobRateLimiter startJobRateLimiter;
    protected SupervisorEndpoint supervisorEndpoint;
    protected K8sProperties k8sProperties;
    protected JobCredentialProvider jobCredentialProvider;
    protected EventPublisher eventPublisher;
    protected TaskSupervisorJobCaller taskSupervisorJobCaller;
    protected ResourceManager resourceManager;
    protected SupervisorAgentAllocator supervisorAgentAllocator;



    public void init() throws JobException {
        taskFrameworkProperties = Mockito.mock(TaskFrameworkProperties.class);
        k8sProperties = new K8sProperties();
        Mockito.when(taskFrameworkProperties.isEnableTaskSupervisorAgent()).thenReturn(true);
        Mockito.when(taskFrameworkProperties.getSingleFetchPreparingJobRows()).thenReturn(5);
        Mockito.when(taskFrameworkProperties.getSinglePullResultJobRows()).thenReturn(5);
        Mockito.when(taskFrameworkProperties.getJobHeartTimeoutSeconds()).thenReturn(1000);
        Mockito.when(taskFrameworkProperties.getRunMode()).thenReturn(TaskRunMode.K8S);
        Mockito.when(taskFrameworkProperties.getK8sProperties()).thenReturn(k8sProperties);
        taskFrameworkService = Mockito.mock(TaskFrameworkService.class);
        Mockito.when(taskFrameworkService.updateStatusByIdOldStatus(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(1);
        Mockito.when(taskFrameworkService.updateTaskResult(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(1);
        hostUrlProvider = Mockito.mock(HostUrlProvider.class);
        configuration = Mockito.mock(JobConfiguration.class);
        startJobRateLimiter = Mockito.mock(StartJobRateLimiter.class);
        jobCredentialProvider = Mockito.mock(JobCredentialProvider.class);
        eventPublisher = Mockito.mock(EventPublisher.class);
        Mockito.when(startJobRateLimiter.tryAcquire()).thenReturn(true);
        taskSupervisorJobCaller = Mockito.mock(TaskSupervisorJobCaller.class);
        resourceManager = Mockito.mock(ResourceManager.class);
        supervisorAgentAllocator = Mockito.mock(SupervisorAgentAllocator.class);
        Mockito.when(configuration.getSupervisorAgentAllocator()).thenReturn(supervisorAgentAllocator);
        Mockito.when(configuration.getResourceManager()).thenReturn(resourceManager);
        Mockito.when(configuration.getTaskSupervisorJobCaller()).thenReturn(taskSupervisorJobCaller);
        Mockito.when(configuration.getTaskFrameworkProperties()).thenReturn(taskFrameworkProperties);
        Mockito.when(configuration.getTaskFrameworkService()).thenReturn(taskFrameworkService);
        Mockito.when(configuration.getHostUrlProvider()).thenReturn(hostUrlProvider);
        Mockito.when(configuration.getStartJobRateLimiter()).thenReturn(startJobRateLimiter);
        Mockito.when(configuration.getTransactionManager()).thenReturn(new SimpleTransactionManager());
        Mockito.when(configuration.getJobCredentialProvider()).thenReturn(jobCredentialProvider);
        Mockito.when(configuration.getEventPublisher()).thenReturn(eventPublisher);
        jobEntity = new JobEntity();
        jobEntity.setId(1024L);
        Map<String, String> jobProperties = new HashMap<>();
        jobProperties.put(ResourceIDUtil.REGION_PROP_NAME, "test");
        jobProperties.put(ResourceIDUtil.GROUP_PROP_NAME, "test");
        jobProperties.put("jobExpiredIfNotRunningAfterSeconds", "100");
        jobEntity.setRunMode(TaskRunMode.K8S);
        jobEntity.setJobProperties(jobProperties);
        page = Mockito.mock(Page.class);
        Mockito.when(page.iterator()).thenReturn(Arrays.asList(jobEntity).iterator());
        supervisorEndpoint = new SupervisorEndpoint(SystemUtils.getLocalIpAddress(), 9999);
    }

    private static final class SimpleTransactionManager implements TransactionManager {

        @Override
        public <T> T doInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void doInTransactionWithoutResult(Runnable r) {
            r.run();
        }
    }
}
