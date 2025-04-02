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
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.monitor.DBUserMonitorExecutor;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.onlineschemachange.rename.DefaultRenameTableInvoker;
import com.oceanbase.odc.service.onlineschemachange.rename.LockTableSupportDecider;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * swap table action
 *
 * @author longpeng.zlp
 * @date 2025/3/24 11:38
 */
@Slf4j
public abstract class SwapTableActionBase implements Action<OscActionContext, OscActionResult> {

    protected final DBSessionManageFacade dbSessionManageFacade;

    protected final OnlineSchemaChangeProperties onlineSchemaChangeProperties;

    public SwapTableActionBase(@NotNull DBSessionManageFacade dbSessionManageFacade,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        this.dbSessionManageFacade = dbSessionManageFacade;
        this.onlineSchemaChangeProperties = onlineSchemaChangeProperties;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        if (!checkOSCProjectReady(context)) {
            // OMS state abnormal, keep waiting
            log.info("OSC: swap table waiting for OSC task ready");
            return new OscActionResult(OscStates.SWAP_TABLE.getState(), null, OscStates.SWAP_TABLE.getState());
        }
        // begin swap table
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        log.info("Start execute {}, schedule task id={}", getClass().getSimpleName(), scheduleTask.getId());

        OnlineSchemaChangeScheduleTaskParameters taskParameters = context.getTaskParameter();
        PreConditions.notNull(taskParameters, "OnlineSchemaChangeScheduleTaskParameters is null");
        OnlineSchemaChangeParameters parameters = context.getParameter();

        ConnectionConfig config = context.getConnectionProvider().connectionConfig();
        DBUserMonitorExecutor userMonitorExecutor = new DBUserMonitorExecutor(config, parameters.getLockUsers());
        try {
            if (enableUserMonitor(parameters.getLockUsers())) {
                userMonitorExecutor.start();
            }
            DefaultRenameTableInvoker defaultRenameTableInvoker =
                    new DefaultRenameTableInvoker(context.getConnectionProvider(), dbSessionManageFacade,
                            () -> {
                                OnlineSchemaChangeScheduleTaskResult lastResult = JsonUtils.fromJson(
                                        context.getScheduleTask().getResultJson(),
                                        OnlineSchemaChangeScheduleTaskResult.class);
                                return isIncrementDataAppliedDone(onlineSchemaChangeProperties,
                                        taskParameters,
                                        lastResult.getCheckFailedTime(), 25000);
                            }, this::getLockTableSupportDecider);
            defaultRenameTableInvoker.invoke(taskParameters, parameters);
            // rename table success, jump to clean resource state
            return new OscActionResult(OscStates.SWAP_TABLE.getState(), null, OscStates.CLEAN_RESOURCE.getState());
        } finally {
            if (enableUserMonitor(parameters.getLockUsers())) {
                userMonitorExecutor.stop();
            }
        }
    }

    protected LockTableSupportDecider getLockTableSupportDecider() {
        String lockTableMatchers = onlineSchemaChangeProperties.getSupportLockTableObVersionJson();
        return LockTableSupportDecider.createWithJsonArrayWithDefaultValue(lockTableMatchers);
    }

    protected abstract boolean checkOSCProjectReady(OscActionContext context);

    /**
     * check if all increment data has applied to ghost table this check should called after source
     * table locked
     */
    @VisibleForTesting
    protected abstract boolean isIncrementDataAppliedDone(OnlineSchemaChangeProperties onlineSchemaChangeProperties,
            OnlineSchemaChangeScheduleTaskParameters parameters,
            Map<OscStepName, Long> checkFailedTimes, int timeOutMS);

    protected boolean enableUserMonitor(List<String> lockUsers) {
        return CollectionUtils.isNotEmpty(lockUsers);
    }
}
