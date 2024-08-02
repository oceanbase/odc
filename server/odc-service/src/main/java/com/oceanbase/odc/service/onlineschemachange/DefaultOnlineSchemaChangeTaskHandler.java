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
package com.oceanbase.odc.service.onlineschemachange;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionFsm;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultOnlineSchemaChangeTaskHandler
 *
 * @author yaobin
 * @date 2023-06-08
 * @since 4.2.0
 */
@Slf4j
@Component
public class DefaultOnlineSchemaChangeTaskHandler implements OnlineSchemaChangeTaskHandler {
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private OscActionFsm oscActionFSM;

    @Override
    public void start(@NonNull Long scheduleId, @NonNull Long scheduleTaskId) {
        oscActionFSM.start(scheduleId, scheduleTaskId);
    }

    @Override
    public void complete(Long scheduleId, Long scheduleTaskId) {
        oscActionFSM.schedule(scheduleId, scheduleTaskId);
    }

    @Override
    public void terminate(@NonNull Long scheduleId, @NonNull Long scheduleTaskId) {
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        oscActionFSM.transferTaskStatesWithStates(null, OscStates.CLEAN_RESOURCE.getState(), null, scheduleTaskEntity,
                TaskStatus.CANCELED);
    }

}
