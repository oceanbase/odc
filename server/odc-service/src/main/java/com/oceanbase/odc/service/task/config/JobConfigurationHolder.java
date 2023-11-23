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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.caller.JobCaller;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
public class JobConfigurationHolder implements ApplicationListener<ContextRefreshedEvent> {

    private static JobConfiguration configuration;

    public static JobConfiguration getJobConfiguration() {
        return configuration;
    }

    private static void setJobConfiguration(JobConfiguration config) {
        configuration = config;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        DefaultJobConfiguration configuration = new DefaultJobConfiguration();
        configuration.setConnectionService(ctx.getBean(ConnectionService.class));
        configuration.setTaskService(ctx.getBean(TaskService.class));
        configuration.setJobCaller(ctx.getBean(JobCaller.class));
        setJobConfiguration(configuration);
    }
}
