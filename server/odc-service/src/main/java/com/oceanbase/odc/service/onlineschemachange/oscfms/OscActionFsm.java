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
package com.oceanbase.odc.service.onlineschemachange.oscfms;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CleanResourcesAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CreateDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CreateGhostTableAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ModifyDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.MonitorDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.SwapTableAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.YieldContextAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.extern.slf4j.Slf4j;

/**
 * Online schema change FSM impl It's actually only have OMS impl all task state change should impl
 * in this class
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 11:56
 * @since 4.3.1
 */
@Slf4j
@Component
public class OscActionFsm extends OscActionFsmBase {
    // register state change action
    @PostConstruct
    public void init() {
        OscStatesTransfer statesTransfer = new OscStatesTransfer();
        // YIELD_CONTEXT -> CREATE_GHOST_TABLES
        registerEvent(OscStates.YIELD_CONTEXT.getState(),
                new YieldContextAction(actionScheduler, scheduleTaskRepository), statesTransfer,
                ImmutableSet.of(OscStates.CREATE_GHOST_TABLES.getState(), OscStates.COMPLETE.getState()));

        // CREATE_GHOST_TABLES -> CREATE_GHOST_TABLES | CREATE_DATA_TASK | COMPLETE(cancel)
        registerEvent(OscStates.CREATE_GHOST_TABLES.getState(), new CreateGhostTableAction(), statesTransfer,
                ImmutableSet.of(OscStates.CREATE_GHOST_TABLES.getState(), OscStates.CREATE_DATA_TASK.getState(),
                        OscStates.COMPLETE.getState()));

        // CREATE_DATA_TASK -> CREATE_DATA_TASK | MONITOR_DATA_TASK| COMPLETE(cnacel)
        registerEvent(OscStates.CREATE_DATA_TASK.getState(),
                CreateDataTaskAction.createDataTaskAction(dataSourceOpenApiService, omsProjectOpenApiService,
                        onlineSchemaChangeProperties, systemConfigService, resourceManager),
                statesTransfer,
                ImmutableSet.of(OscStates.CREATE_DATA_TASK.getState(), OscStates.MONITOR_DATA_TASK.getState(),
                        OscStates.COMPLETE.getState()));

        // MONITOR_DATA_TASK -> MONITOR_DATA_TASK | MODIFY_DATA_TASK| SWAP_TABLE | COMPLETE(cancel)
        registerEvent(OscStates.MONITOR_DATA_TASK.getState(),
                MonitorDataTaskAction
                        .ofOMSMonitorDataTaskAction(omsProjectOpenApiService, onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.MONITOR_DATA_TASK.getState(), OscStates.MODIFY_DATA_TASK.getState(),
                        OscStates.SWAP_TABLE.getState(),
                        OscStates.COMPLETE.getState()));

        // MODIFY_DATA_TASK -> MODIFY_DATA_TASK | MONITOR_DATA_TASK | COMPLETE(cancel)
        registerEvent(OscStates.MODIFY_DATA_TASK.getState(),
                ModifyDataTaskAction.ofOMSModifyDataTaskAction(omsProjectOpenApiService, onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.MONITOR_DATA_TASK.getState(), OscStates.MODIFY_DATA_TASK.getState(),
                        OscStates.COMPLETE.getState()));

        // SWAP_TABLE -> SWAP_TABLE | CLEAN_RESOURCES | COMPLETE(cancel)
        registerEvent(OscStates.SWAP_TABLE.getState(),
                SwapTableAction.ofOMSSwapTableAction(dbSessionManageFacade, omsProjectOpenApiService,
                        onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.SWAP_TABLE.getState(), OscStates.CLEAN_RESOURCE.getState(),
                        OscStates.COMPLETE.getState()));

        // CLEAN_RESOURCE -> YIELD_CONTEXT | CLEAN_RESOURCES | COMPLETE(cancel)
        registerEvent(OscStates.CLEAN_RESOURCE.getState(),
                CleanResourcesAction.ofCleanResourcesAction(omsProjectOpenApiService, resourceManager), statesTransfer,
                ImmutableSet.of(OscStates.YIELD_CONTEXT.getState(), OscStates.CLEAN_RESOURCE.getState(),
                        OscStates.COMPLETE.getState()));
        // COMPLETE should not be scheduled
    }
}
