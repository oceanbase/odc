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

package com.oceanbase.odc.service.task;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.base.databasechange.DatabaseChangeTask;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.DefaultJobConfiguration;
import com.oceanbase.odc.service.task.config.DefaultTaskFrameworkProperties;
import com.oceanbase.odc.service.task.dispatch.JobDispatcher;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.StdJobScheduler;
import com.oceanbase.odc.service.task.schedule.provider.HostUrlProvider;
import com.oceanbase.odc.service.task.schedule.provider.JobImageNameProvider;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.service.TransactionManager;

import cn.hutool.core.lang.Assert;

/**
 * @author yaobin
 * @date 2023-11-24
 * @since 4.2.4
 */
public class JobSchedulerTest {

    @Test
    public void test_scheduleJob() throws JobException, SchedulerException, InterruptedException {

        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();
        DefaultJobConfiguration jc = new DefaultJobConfiguration() {};
        jc.setDaemonScheduler(sched);
        jc.setHostUrlProvider(Mockito.mock(HostUrlProvider.class));
        jc.setEventPublisher(new LocalEventPublisher());
        jc.setJobImageNameProvider(Mockito.mock(JobImageNameProvider.class));
        TaskFrameworkService taskFrameworkService = Mockito.mock(TaskFrameworkService.class);
        jc.setTaskFrameworkService(taskFrameworkService);
        jc.setTransactionManager(Mockito.mock(TransactionManager.class));
        DefaultTaskFrameworkProperties properties = new DefaultTaskFrameworkProperties();
        properties.setCheckRunningJobCronExpression("0/3 * * * * ?");
        properties.setStartPreparingJobCronExpression("0/3 * * * * ?");
        properties.setDoCancelingJobCronExpression("0/3 * * * * ?");
        properties.setDestroyExecutorJobCronExpression("0/3 * * * * ?");
        jc.setTaskFrameworkProperties(properties);
        Mockito.when(taskFrameworkService.save(Mockito.any())).thenReturn(Mockito.mock(JobEntity.class));
        Mockito.when(taskFrameworkService.find(Mockito.any())).thenReturn(Mockito.mock(JobEntity.class));

        DefaultJobDefinition jd = DefaultJobDefinition.builder().jobClass(DatabaseChangeTask.class)
                .jobType("ASYNC").build();

        JobDispatcher jobDispatcher = Mockito.mock(JobDispatcher.class);
        Mockito.doNothing().when(jobDispatcher).start(Mockito.mock(JobContext.class));
        jc.setJobDispatcher(jobDispatcher);

        JobScheduler js = new StdJobScheduler(jc);
        Long id = js.scheduleJobNow(jd);
        Assert.notNull(id);
    }
}
