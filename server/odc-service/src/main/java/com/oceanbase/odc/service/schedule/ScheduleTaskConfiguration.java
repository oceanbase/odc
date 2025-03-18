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

import java.util.List;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.schedule.alarm.DefaultScheduleAlarmClient;
import com.oceanbase.odc.service.schedule.alarm.ScheduleAlarmClient;
import com.oceanbase.odc.service.schedule.export.exception.DatabaseNonExistException;
import com.oceanbase.odc.service.schedule.export.model.ExportedDataSource;
import com.oceanbase.odc.service.schedule.export.model.ExportedDatabase;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.ScheduleRowPreviewDto;
import com.oceanbase.odc.service.schedule.flowtask.ApprovalFlowClient;
import com.oceanbase.odc.service.schedule.flowtask.NoApprovalFlowClient;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.submitter.DefaultJobSubmitter;
import com.oceanbase.odc.service.schedule.submitter.JobSubmitter;
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
    @ConditionalOnMissingBean(JobSubmitter.class)
    public JobSubmitter taskSubmitter() {
        return new DefaultJobSubmitter();
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleDescriptionGenerator.class)
    public ScheduleDescriptionGenerator scheduleDescriptionGenerator() {
        return new DefaultScheduleDescriptionGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleExportImportFacade.class)
    public ScheduleExportImportFacade defaultScheduleArchiveFacade() {
        return new ScheduleExportImportFacade() {

            @Override
            public Set<ScheduleType> supportedScheduleTypes() {
                throw new UnsupportedOperationException("Community Edition is not supported yet");
            }

            @Override
            public void adaptProperties(ExportProperties exportProperties) {
                throw new UnsupportedOperationException("Community Edition is not supported yet");
            }

            @Override
            public void adaptExportDatasource(ExportedDataSource exportedDataSource) {
                throw new UnsupportedOperationException("Community Edition is not supported yet");
            }

            @Override
            public List<ImportScheduleTaskView> preview(ScheduleType scheduleType, Long projectId,
                    ExportProperties exportProperties, List<ScheduleRowPreviewDto> dtos) {
                throw new UnsupportedOperationException("Community Edition is not supported yet");
            }

            @Override
            public Long getOrCreateDatabaseId(Long projectId, ExportedDatabase exportedDatabase)
                    throws DatabaseNonExistException {
                throw new UnsupportedOperationException("Community Edition is not supported yet");
            }

        };
    }
}
