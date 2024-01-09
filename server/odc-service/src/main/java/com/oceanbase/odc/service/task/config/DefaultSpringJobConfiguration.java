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

import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.info.InfoAdapter;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.dispatch.ImmediateJobDispatcher;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.schedule.DefaultJobImageNameProvider;
import com.oceanbase.odc.service.task.schedule.HostUrlProvider;
import com.oceanbase.odc.service.task.service.StdTaskFrameworkService;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

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
        setTaskFrameworkProperties(ctx.getBean(TaskFrameworkProperties.class));
        setConnectionService(ctx.getBean(ConnectionService.class));
        setTaskService(ctx.getBean(TaskService.class));
        setScheduleTaskService(ctx.getBean(ScheduleTaskService.class));
        setScheduler((Scheduler) ctx.getBean("taskFrameworkSchedulerFactoryBean"));
        setJobDispatcher(new ImmediateJobDispatcher());
        if (getTaskFrameworkProperties().getRunMode() == TaskRunModeEnum.K8S) {
            setK8sJobClient(ctx.getBean(K8sJobClient.class));
        }
        setHostUrlProvider(ctx.getBean(HostUrlProvider.class));
        LocalEventPublisher publisher = new LocalEventPublisher();
        TaskFrameworkService tfs = ctx.getBean(TaskFrameworkService.class);
        if (tfs instanceof StdTaskFrameworkService) {
            ((StdTaskFrameworkService) tfs).setPublisher(publisher);
        }
        setTaskFrameworkService(tfs);
        setEventPublisher(publisher);
        setJobImageNameProvider(new DefaultJobImageNameProvider(ctx.getBean(InfoAdapter.class),
                getTaskFrameworkProperties()));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

}
