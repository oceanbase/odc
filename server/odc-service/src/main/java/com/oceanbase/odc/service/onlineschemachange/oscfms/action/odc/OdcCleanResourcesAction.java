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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc;

import java.util.List;

import com.google.common.collect.Lists;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CleanResourcesActionBase;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.resource.ResourceManager;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/3/24 14:11
 */
@Slf4j
public class OdcCleanResourcesAction extends CleanResourcesActionBase {
    private final ResourceManager resourceManager;


    private final List<TaskStatus> expectedTaskStatus = Lists.newArrayList(TaskStatus.DONE, TaskStatus.FAILED,
            TaskStatus.CANCELED, TaskStatus.RUNNING, TaskStatus.ABNORMAL);

    public OdcCleanResourcesAction(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        OnlineSchemaChangeScheduleTaskParameters taskParameters = context.getTaskParameter();
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        PreConditions.validArgumentState(expectedTaskStatus.contains(scheduleTask.getStatus()), ErrorCodes.Unexpected,
                new Object[] {scheduleTask.getStatus()}, "schedule task is not excepted status");
        tryKillOSCMigrateJob(taskParameters);
        if (!tryDropNewTable(context)) {
            // try drop ghost table again
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.CLEAN_RESOURCE.getState());
        }
        releaseResource(taskParameters);
        return determinateNextState(scheduleTask, context.getSchedule());
    }

    protected void tryKillOSCMigrateJob(OnlineSchemaChangeScheduleTaskParameters parameters) {
        if (!OscCommandUtil.isOSCMigrateSupervisorAlive(parameters.getOdcCommandURl())) {
            log.info("ODCCleanResourcesAction: supervisor has quit, url={}", parameters.getOdcCommandURl());
            return;
        }
        SupervisorResponse clearResponse = OscCommandUtil.clearTask(parameters.getOdcCommandURl());
        log.info("ODCCleanResourcesAction: clear task response = {}", clearResponse);
    }

    protected void releaseResource(OnlineSchemaChangeScheduleTaskParameters parameters) throws Exception {
        if (null != parameters.getResourceID()) {
            resourceManager.destroy(parameters.getResourceID());
        }
    }
}
