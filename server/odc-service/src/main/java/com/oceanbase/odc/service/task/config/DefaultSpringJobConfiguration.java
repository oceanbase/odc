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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoRepository;
import com.oceanbase.odc.metadb.task.SupervisorEndpointRepository;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.dispatch.ImmediateJobDispatcher;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.resource.LocalProcessResource;
import com.oceanbase.odc.service.task.resource.SupervisorAgentAllocator;
import com.oceanbase.odc.service.task.resource.manager.TaskResourceManager;
import com.oceanbase.odc.service.task.resource.manager.strategy.k8s.K8SResourceManageStrategy;
import com.oceanbase.odc.service.task.resource.manager.strategy.process.ProcessResourceManageStrategy;
import com.oceanbase.odc.service.task.schedule.DefaultTaskFrameworkDisabledHandler;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;
import com.oceanbase.odc.service.task.schedule.StartJobRateLimiter;
import com.oceanbase.odc.service.task.schedule.StartJobRateLimiterSupport;
import com.oceanbase.odc.service.task.schedule.provider.DefaultHostUrlProvider;
import com.oceanbase.odc.service.task.schedule.provider.DefaultJobImageNameProvider;
import com.oceanbase.odc.service.task.service.SpringTransactionManager;
import com.oceanbase.odc.service.task.service.StdTaskFrameworkService;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.DefaultJobEventListener;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisorJobCaller;
import com.oceanbase.odc.service.task.supervisor.proxy.LocalTaskSupervisorProxy;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;
import com.oceanbase.odc.service.task.util.TaskSupervisorUtil;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
public class DefaultSpringJobConfiguration extends DefaultJobConfiguration
        implements InitializingBean, ApplicationContextAware {

    private ApplicationContext ctx;

    @Override
    public void afterPropertiesSet() {
        setTaskFrameworkEnabledProperties(ctx.getBean(TaskFrameworkEnabledProperties.class));
        setCloudEnvConfigurations(ctx.getBean(CloudEnvConfigurations.class));
        setHostUrlProvider(new DefaultHostUrlProvider(this::getTaskFrameworkProperties,
                ctx.getBean(HostProperties.class)));
        setJobImageNameProvider(new DefaultJobImageNameProvider(this::getTaskFrameworkProperties));
        setConnectionService(ctx.getBean(ConnectionService.class));
        setTaskService(ctx.getBean(TaskService.class));
        TaskFrameworkProperties taskFrameworkProperties = getTaskFrameworkProperties();
        if (!taskFrameworkProperties.isEnableTaskSupervisorAgent()) {
            setDaemonScheduler((Scheduler) ctx.getBean("taskFrameworkSchedulerFactoryBean"));
        } else {
            setTaskSupervisorScheduler((Scheduler) ctx.getBean("defaultTaskSchedulerFactoryBean"));
        }

        setJobDispatcher(new ImmediateJobDispatcher(ctx.getBean(ResourceManager.class)));
        setResourceManager(ctx.getBean(ResourceManager.class));
        LocalEventPublisher publisher = new LocalEventPublisher();
        TaskFrameworkService tfs = ctx.getBean(TaskFrameworkService.class);
        if (tfs instanceof StdTaskFrameworkService) {
            ((StdTaskFrameworkService) tfs).setPublisher(publisher);
        }
        setTaskFrameworkService(tfs);
        setEventPublisher(publisher);
        TaskExecutorClient executorClient = new TaskExecutorClient();
        setTaskExecutorClient(executorClient);
        setTaskSupervisorJobCaller(
                new TaskSupervisorJobCaller(new DefaultJobEventListener(), new LocalTaskSupervisorProxy(
                        TaskSupervisorUtil.getDefaultSupervisorEndpoint(), JobConstants.ODC_AGENT_CLASS_NAME),
                        executorClient));
        setTransactionManager(new SpringTransactionManager(ctx.getBean(TransactionTemplate.class)));
        initJobRateLimiter();
        setTaskFrameworkDisabledHandler(new DefaultTaskFrameworkDisabledHandler());
        setJasyptEncryptorConfigProperties(ctx.getBean(JasyptEncryptorConfigProperties.class));
        setHostProperties(ctx.getBean(HostProperties.class));
        setJobCredentialProvider(ctx.getBean(JobCredentialProvider.class));
        if (TaskSupervisorUtil.isTaskSupervisorEnabled(taskFrameworkProperties)) {
            initTaskSupervisor(taskFrameworkProperties);
        }
    }

    protected void initTaskSupervisor(TaskFrameworkProperties taskFrameworkProperties) {
        // init resource allocator and resource manager
        TaskResourceManager taskResourceManager = null;
        if (taskFrameworkProperties.getRunMode().isProcess()) {
            // prepare local process resource
            LocalProcessResource localProcessResource =
                    new LocalProcessResource(ctx.getBean(SupervisorEndpointRepository.class));
            localProcessResource.prepareLocalProcessResource();
            taskResourceManager =
                    new TaskResourceManager(ctx.getBean(SupervisorEndpointRepository.class), ctx.getBean(
                            ResourceAllocateInfoRepository.class), new ProcessResourceManageStrategy());
        } else {
            // k8s mode
            taskResourceManager =
                    new TaskResourceManager(ctx.getBean(SupervisorEndpointRepository.class), ctx.getBean(
                            ResourceAllocateInfoRepository.class),
                            new K8SResourceManageStrategy(taskFrameworkProperties.getK8sProperties(),
                                    ctx.getBean(ResourceManager.class),
                                    ctx.getBean(SupervisorEndpointRepository.class)));
        }

        setTaskResourceManager(taskResourceManager);
        setSupervisorAgentAllocator(
                new SupervisorAgentAllocator(ctx.getBean(ResourceAllocateInfoRepository.class)));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Override
    public TaskFrameworkProperties getTaskFrameworkProperties() {
        return ctx.getBean(TaskFrameworkProperties.class);
    }


    private void initJobRateLimiter() {
        StartJobRateLimiterSupport limiterSupport = new StartJobRateLimiterSupport();
        ctx.getBeansOfType(StartJobRateLimiter.class).forEach((k, v) -> limiterSupport.addJobRateLimiter(v));
        setStartJobRateLimiter(limiterSupport);
    }
}
