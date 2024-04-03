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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.service.task.listener.DefaultJobProcessUpdateListener;
import com.oceanbase.odc.service.task.listener.DefaultJobTerminateListener;
import com.oceanbase.odc.service.task.listener.JobTerminateNotifyListener;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.StdJobScheduler;

import lombok.Setter;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobSchedulerFactoryBean implements FactoryBean<JobScheduler>, InitializingBean {

    private JobScheduler jobScheduler;

    @Autowired
    private JobTerminateNotifyListener jobTerminateNotifyListener;
    @Autowired
    private DefaultJobProcessUpdateListener defaultJobProcessUpdateListener;
    @Autowired
    private DefaultJobTerminateListener defaultJobTerminateListener;

    @Setter
    public JobConfiguration jobConfiguration;

    @Override
    public JobScheduler getObject() throws Exception {
        return jobScheduler;
    }

    @Override
    public Class<?> getObjectType() {
        return JobScheduler.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        jobScheduler = new StdJobScheduler(jobConfiguration);
        jobScheduler.getEventPublisher().addEventListener(jobTerminateNotifyListener);
        jobScheduler.getEventPublisher().addEventListener(defaultJobProcessUpdateListener);
        jobScheduler.getEventPublisher().addEventListener(defaultJobTerminateListener);
    }
}
