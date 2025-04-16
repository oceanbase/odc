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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.MonitorDataTaskActionBase;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/8 20:04
 * @since 4.3.1
 */
@Slf4j
public class OmsMonitorDataTaskAction extends MonitorDataTaskActionBase {
    private final OmsProjectOpenApiService projectOpenApiService;

    public OmsMonitorDataTaskAction(@NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        super(onlineSchemaChangeProperties);
        this.projectOpenApiService = projectOpenApiService;
    }

    protected ProjectStepResult getProjectStepResult(OnlineSchemaChangeScheduleTaskParameters taskParameter,
            OnlineSchemaChangeScheduleTaskResult lastResult) {
        return OmsRequestUtil.buildProjectStepResult(projectOpenApiService, onlineSchemaChangeProperties,
                taskParameter.getUid(), taskParameter.getOmsProjectId(), taskParameter.getDatabaseName(),
                lastResult.getCheckFailedTime());
    }

    protected boolean isMigrateTaskReady(ProjectStepResult projectStepResult) {
        return OmsRequestUtil.isOmsTaskReady(projectStepResult);
    }

    @Override
    protected String getPrintLogName(OnlineSchemaChangeScheduleTaskParameters parameters) {
        return parameters.getOmsProjectId();
    }
}
