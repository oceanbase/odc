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

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/7/13 19:47
 * @Descripition:
 */

@Slf4j
public class AbstractDlmJobPreprocessor implements Preprocessor {

    @Override
    public void process(CreateFlowInstanceReq req) {}

    public ScheduleEntity buildScheduleEntity(CreateFlowInstanceReq req, AuthenticationFacade authenticationFacade) {
        AlterScheduleParameters parameters = (AlterScheduleParameters) req.getParameters();
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setConnectionId(req.getConnectionId());
        scheduleEntity.setDatabaseName(req.getDatabaseName());
        scheduleEntity.setDatabaseId(req.getDatabaseId());
        scheduleEntity.setProjectId(req.getProjectId());
        scheduleEntity.setJobType(parameters.getType());
        scheduleEntity.setStatus(ScheduleStatus.APPROVING);
        scheduleEntity.setAllowConcurrent(parameters.getAllowConcurrent());
        scheduleEntity.setMisfireStrategy(parameters.getMisfireStrategy());
        scheduleEntity.setJobParametersJson(JsonUtils.toJson(parameters.getScheduleTaskParameters()));
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(parameters.getTriggerConfig()));
        scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
        scheduleEntity.setDescription(req.getDescription());
        scheduleEntity.setCreatorId(authenticationFacade.currentUser().id());
        scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
        scheduleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        return scheduleEntity;
    }


}
