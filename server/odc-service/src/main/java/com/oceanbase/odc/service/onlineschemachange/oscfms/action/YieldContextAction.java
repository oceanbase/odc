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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action;

import java.util.Optional;
import java.util.function.Predicate;

import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.oscfms.ActionScheduler;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/9 15:10
 * @since 4.3.1
 */
@Slf4j
public class YieldContextAction implements Action<OscActionContext, OscActionResult> {

    private final ScheduleTaskRepository scheduleTaskRepository;

    private final ActionScheduler actionScheduler;

    public YieldContextAction(@NotNull ActionScheduler actionScheduler,
            @NotNull ScheduleTaskRepository scheduleTaskRepository) {
        this.scheduleTaskRepository = scheduleTaskRepository;
        this.actionScheduler = actionScheduler;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        ScheduleEntity scheduleEntity = context.getSchedule();
        if (tryDispatchNextSchedulerTask(scheduleEntity)) {
            return new OscActionResult(OscStates.YIELD_CONTEXT.getState(), null,
                    OscStates.CREATE_GHOST_TABLES.getState());
        } else {
            // delete scheduler job and complete
            return new OscActionResult(OscStates.YIELD_CONTEXT.getState(), null,
                    OscStates.COMPLETE.getState());
        }
    }

    // try to dispatch next task
    protected boolean tryDispatchNextSchedulerTask(ScheduleEntity schedule) {
        Long scheduleId = schedule.getId();
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                schedule.getJobParametersJson(), OnlineSchemaChangeParameters.class);
        Optional<ScheduleTaskEntity> nextTask = findFirstConditionScheduleTask(scheduleId,
                s -> s.getStatus() == TaskStatus.PREPARING);
        if (!nextTask.isPresent()) {
            log.info("No preparing status schedule task for next schedule, schedule id {}, delete quartz job",
                    scheduleId);
            return false;
        }
        Long nextTaskId = nextTask.get().getId();
        actionScheduler.submitFMSScheduler(schedule, nextTaskId, onlineSchemaChangeParameters.getFlowTaskID());
        return true;
    }


    private Optional<ScheduleTaskEntity> findFirstConditionScheduleTask(
            Long scheduleId, Predicate<ScheduleTaskEntity> predicate) {
        Specification<ScheduleTaskEntity> specification = Specification
                .where(ScheduleTaskSpecs.jobNameEquals(scheduleId + ""));
        return scheduleTaskRepository.findAll(specification, Sort.by("id"))
                .stream()
                .filter(s -> predicate == null || predicate.test(s))
                .findFirst();
    }
}
