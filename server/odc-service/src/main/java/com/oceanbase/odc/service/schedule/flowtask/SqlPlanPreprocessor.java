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
package com.oceanbase.odc.service.schedule.flowtask;

import java.util.Set;

import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.flow.processor.ScheduleTaskPreprocessor;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

/**
 * @Author：tinker
 * @Date: 2023/5/25 11:40
 * @Descripition:
 */

@ScheduleTaskPreprocessor(type = JobType.SQL_PLAN)
public class SqlPlanPreprocessor implements Preprocessor {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowTaskProperties flowTaskProperties;

    @Override
    public void process(CreateFlowInstanceReq req) {

        AlterScheduleParameters parameters = (AlterScheduleParameters) req.getParameters();
        TriggerConfig triggerConfig = parameters.getTriggerConfig();

        // find approving instance before create new flow instance.
        if (parameters.getTaskId() != null) {
            checkAlterSchedule(parameters.getTaskId());
        }

        switch (parameters.getOperationType()) {
            case CREATE: {
                // pre-create schedule(order)
                checkSqlContent(parameters);
                checkCronExpression(triggerConfig.getCronExpression());
                ScheduleEntity scheduleEntity = new ScheduleEntity();
                scheduleEntity.setConnectionId(req.getConnectionId());
                scheduleEntity.setDatabaseName(req.getDatabaseName());
                scheduleEntity.setProjectId(req.getProjectId());
                scheduleEntity.setDatabaseId(req.getDatabaseId());
                scheduleEntity.setJobType(parameters.getType());
                scheduleEntity.setStatus(ScheduleStatus.APPROVING);
                scheduleEntity.setAllowConcurrent(parameters.getAllowConcurrent());
                scheduleEntity.setMisfireStrategy(parameters.getMisfireStrategy());
                scheduleEntity.setJobParametersJson(JsonUtils.toJson(parameters.getScheduleTaskParameters()));
                scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));
                scheduleEntity.setCreatorId(authenticationFacade.currentUser().id());
                scheduleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
                scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
                scheduleEntity.setDescription(req.getDescription());
                scheduleEntity = scheduleService.create(scheduleEntity);
                parameters.setTaskId(scheduleEntity.getId());
                break;
            }
            case UPDATE: {
                checkSqlContent(parameters);
                checkCronExpression(triggerConfig.getCronExpression());
                parameters.setDescription(req.getDescription());
                break;
            }
            default: {
                ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(parameters.getTaskId());
                if (scheduleEntity.getJobType() == JobType.SQL_PLAN) {
                    parameters.setScheduleTaskParameters(JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                            DatabaseChangeParameters.class));
                    parameters.setType(JobType.SQL_PLAN);
                    parameters.setTriggerConfig(
                            JsonUtils.fromJson(scheduleEntity.getTriggerConfigJson(), TriggerConfig.class));
                    parameters.setDescription(scheduleEntity.getDescription());
                }
                break;
            }
        }
        req.setParentFlowInstanceId(parameters.getTaskId());
    }

    private void checkSqlContent(AlterScheduleParameters parameters) {
        DatabaseChangeParameters taskParameters = (DatabaseChangeParameters) parameters.getScheduleTaskParameters();
        PreConditions.maxLength(taskParameters.getSqlContent(), "sql content",
                flowTaskProperties.getSqlContentMaxLength());
    }

    private void checkAlterSchedule(Long scheduleId) {
        Set<Long> approvingAlterScheduleIds = flowInstanceService.listAlterScheduleIdsByScheduleIdAndStatus(
                scheduleId, FlowStatus.APPROVING);
        PreConditions.validNoDuplicatedAlterSchedule("ScheduleId", scheduleId,
                () -> approvingAlterScheduleIds != null && approvingAlterScheduleIds.size() > 0);
    }

    private void checkCronExpression(String cron) {
        try {
            cron = QuartzCronExpressionUtils.adaptCronExpression(cron);
            new CronExpression(cron);
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.InvalidCronExpression,
                    new Object[] {e.getMessage()},
                    e.getMessage());
        }
    }


}
