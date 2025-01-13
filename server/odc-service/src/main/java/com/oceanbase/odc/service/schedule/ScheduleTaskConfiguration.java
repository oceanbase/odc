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
package com.oceanbase.odc.service.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.service.schedule.alarm.DefaultScheduleAlarmClient;
import com.oceanbase.odc.service.schedule.alarm.ScheduleAlarmClient;
import com.oceanbase.odc.service.schedule.flowtask.ApprovalFlowClient;
import com.oceanbase.odc.service.schedule.flowtask.NoApprovalFlowClient;
import com.oceanbase.odc.service.schedule.util.DefaultScheduleDescriptionGenerator;
import com.oceanbase.odc.service.schedule.util.ScheduleDescriptionGenerator;

/**
 * @Author：tinker
 * @Date: 2024/9/13 11:11
 * @Descripition:
 */

@Configuration
public class ScheduleTaskConfiguration {


    @Bean
    @ConditionalOnMissingBean(ApprovalFlowClient.class)
    public ApprovalFlowClient approvalFlowService() {
        return new NoApprovalFlowClient();
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleAlarmClient.class)
    public ScheduleAlarmClient scheduleAlarmClient() {
        return new DefaultScheduleAlarmClient();
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleDescriptionGenerator.class)
    public ScheduleDescriptionGenerator scheduleDescriptionGenerator() {
        return new DefaultScheduleDescriptionGenerator();
    }
}
