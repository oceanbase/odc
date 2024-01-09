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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.TriggerListenerSupport;

import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.DefaultJobConfiguration;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.dispatch.JobDispatcher;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.HostUrlProvider;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.StdJobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

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
        jc.setScheduler(sched);
        jc.setHostUrlProvider(Mockito.mock(HostUrlProvider.class));
        jc.setEventPublisher(new LocalEventPublisher());
        TaskFrameworkService taskFrameworkService = Mockito.mock(TaskFrameworkService.class);
        jc.setTaskFrameworkService(taskFrameworkService);
        Mockito.when(taskFrameworkService.save(Mockito.any())).thenReturn(Mockito.mock(JobEntity.class));
        Mockito.when(taskFrameworkService.find(Mockito.any())).thenReturn(Mockito.mock(JobEntity.class));

        DefaultJobDefinition jd = DefaultJobDefinition.builder().jobClass(SampleTask.class)
                .jobType("sampleTask").build();

        JobDispatcher jobDispatcher = Mockito.mock(JobDispatcher.class);
        Mockito.doNothing().when(jobDispatcher).start(Mockito.mock(JobContext.class));
        jc.setJobDispatcher(jobDispatcher);

        JobScheduler js = new StdJobScheduler(jc);
        Long id = js.scheduleJobNow(jd);

        sched.start();
        CountDownLatch cd = new CountDownLatch(1);
        sched.getListenerManager().addTriggerListener(new TriggerListenerSupport() {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public void triggerComplete(
                    Trigger trigger,
                    JobExecutionContext context,
                    CompletedExecutionInstruction triggerInstructionCode) {
                JobContext o = (JobContext) context.getMergedJobDataMap().get(JobConstants.QUARTZ_DATA_MAP_JOB_CONTEXT);
                Assert.equals(JobIdentity.of(id), o.getJobIdentity());
                cd.countDown();
            }
        });
        cd.await(3000, TimeUnit.MILLISECONDS);
        sched.shutdown();
    }
}
