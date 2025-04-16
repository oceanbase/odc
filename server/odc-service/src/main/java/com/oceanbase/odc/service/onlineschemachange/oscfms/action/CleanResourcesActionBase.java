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

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.OscTableUtil;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/3/24 14:11
 */
@Slf4j
public abstract class CleanResourcesActionBase implements Action<OscActionContext, OscActionResult> {

    protected final List<TaskStatus> expectedTaskStatus = Lists.newArrayList(TaskStatus.DONE, TaskStatus.FAILED,
            TaskStatus.CANCELED, TaskStatus.RUNNING, TaskStatus.ABNORMAL);

    protected boolean tryDropNewTable(OscActionContext context) {
        ConnectionSession connectionSession = null;
        OnlineSchemaChangeScheduleTaskParameters taskParam = context.getTaskParameter();
        String databaseName = taskParam.getDatabaseName();
        String tableName = taskParam.getNewTableNameUnwrapped();
        boolean succeed;
        try {
            connectionSession = context.getConnectionProvider().createConnectionSession();
            OscTableUtil.dropNewTableIfExits(databaseName, tableName, connectionSession);
            succeed = true;
        } catch (Throwable e) {
            log.warn("osc: drop table = {}.{} failed", databaseName, tableName, e);
            succeed = false;
        } finally {
            if (connectionSession != null) {
                connectionSession.expire();
            }
        }
        return succeed;
    }

    @VisibleForTesting
    protected OscActionResult determinateNextState(ScheduleTaskEntity scheduleTask, ScheduleEntity schedule) {
        Long scheduleId = schedule.getId();
        // try to dispatch to next state for done status
        if (scheduleTask.getStatus() == TaskStatus.DONE) {
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.YIELD_CONTEXT.getState());
        }
        // if task state is in cancel state, stop and transfer to complete state
        if (scheduleTask.getStatus() == TaskStatus.CANCELED) {
            log.info("Because task is canceled, so delete quartz job={}", scheduleId);
            // cancel as complete
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.COMPLETE.getState());
        }
        // remain failed and prepare state
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                schedule.getJobParametersJson(), OnlineSchemaChangeParameters.class);
        if (onlineSchemaChangeParameters.getErrorStrategy() == TaskErrorStrategy.CONTINUE) {
            log.info("Because error strategy is continue, so schedule next task");
            // try schedule next task
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.YIELD_CONTEXT.getState());
        } else {
            log.info("Because error strategy is abort, so delete quartz job={}", scheduleId);
            // not continue for remain state, transfer to complete state
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.COMPLETE.getState());
        }
    }
}
