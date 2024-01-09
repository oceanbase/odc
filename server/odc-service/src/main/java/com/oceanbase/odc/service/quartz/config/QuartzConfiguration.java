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
package com.oceanbase.odc.service.quartz.config;

import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.oceanbase.odc.service.quartz.OdcJobListener;

/**
 * @Author：tinker
 * @Date: 2022/11/16 19:30
 * @Descripition:
 */

@Configuration
public class QuartzConfiguration {

    @Autowired
    private OdcJobListener odcJobListener;

    private final String defaultSchedulerName = "ODC-SCHEDULER";

    @Bean("defaultSchedulerFactoryBean")
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setSchedulerName(defaultSchedulerName);
        return schedulerFactoryBean;
    }

    @Bean("defaultScheduler")
    public Scheduler scheduler(
            @Autowired @Qualifier("defaultSchedulerFactoryBean") SchedulerFactoryBean schedulerFactoryBean)
            throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.getListenerManager().addJobListener(odcJobListener);
        return scheduler;
    }


    @Bean("taskFrameworkSchedulerFactoryBean")
    public SchedulerFactoryBean taskFrameworkSchedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setDataSource(dataSource);
        String taskFrameworkSchedulerName = "TASK-FRAMEWORK-SCHEDULER";
        schedulerFactoryBean.setSchedulerName(taskFrameworkSchedulerName);
        return schedulerFactoryBean;
    }


}
