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

import java.util.Objects;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/3/24 17:53
 */
@Slf4j
public class OdcModifyDataTaskAction implements Action<OscActionContext, OscActionResult> {
    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        OnlineSchemaChangeScheduleTaskParameters taskParameters = context.getTaskParameter();
        OnlineSchemaChangeParameters inputParameters = context.getParameter();
        // if rate limiter parameters is changed, try to stop and restart project
        if (Objects.equals(inputParameters.getRateLimitConfig(), taskParameters.getRateLimitConfig())) {
            log.info("Rate limiter not changed,rateLimiterConfig = {}, update odc project not required",
                    inputParameters.getRateLimitConfig());
            // swap to monitor state
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MONITOR_DATA_TASK.getState());
        }
        SupervisorResponse modifyResponse = OscCommandUtil.updateTask(taskParameters.getOdcCommandURl(),
                inputParameters.getRateLimitConfig().getRowLimit());
        if (null == modifyResponse || !modifyResponse.isSuccess()) {
            log.info("OdcModifyDataTaskAction: update config failed, response is {}", modifyResponse);
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MODIFY_DATA_TASK.getState());
        } else {
            Long scheduleTaskId = context.getScheduleTask().getId();
            taskParameters.setRateLimitConfig(inputParameters.getRateLimitConfig());
            int rows = context.getScheduleTaskRepository().updateTaskParameters(scheduleTaskId,
                    JsonUtils.toJson(taskParameters));
            if (rows > 0) {
                log.info("Update throttle completed, scheduleTaskId={}", scheduleTaskId);
            }
            log.info("OdcModifyDataTaskAction: update config success, response is {}", modifyResponse);
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MONITOR_DATA_TASK.getState());
        }
    }
}
