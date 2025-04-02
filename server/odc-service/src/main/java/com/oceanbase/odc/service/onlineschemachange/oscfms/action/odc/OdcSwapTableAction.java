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

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.SwapTableActionBase;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsRequestUtil;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * swap table action
 * 
 * @author longpeng.zlp
 * @date 2024/3/24 11:38
 * @since 4.3.1
 */
@Slf4j
public class OdcSwapTableAction extends SwapTableActionBase {

    private final OdcMonitorDataTaskAction odcMonitorDataTaskAction;

    public OdcSwapTableAction(@NotNull DBSessionManageFacade dbSessionManageFacade,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        super(dbSessionManageFacade, onlineSchemaChangeProperties);
        this.odcMonitorDataTaskAction = new OdcMonitorDataTaskAction(onlineSchemaChangeProperties);
    }

    protected boolean checkOSCProjectReady(OscActionContext context) {
        OnlineSchemaChangeScheduleTaskParameters taskParameter = context.getTaskParameter();
        // get oms step result
        ProjectStepResult projectStepResult = odcMonitorDataTaskAction.getProjectStepResultInner(taskParameter);
        if (null == projectStepResult) {
            return false;
        }
        return odcMonitorDataTaskAction.isMigrateTaskReady(projectStepResult);
    }

    /**
     * check if all increment data has applied to ghost table this check should called after source
     * table locked
     */
    @VisibleForTesting
    protected boolean isIncrementDataAppliedDone(OnlineSchemaChangeProperties onlineSchemaChangeProperties,
            OnlineSchemaChangeScheduleTaskParameters parameters,
            Map<OscStepName, Long> checkFailedTimes, int timeOutMS) {
        long safeDataCheckpoint = System.currentTimeMillis() / 1000;
        // max check 25s
        long checkTimeoutMs = System.currentTimeMillis() + timeOutMS;
        while (true) {
            ProjectStepResult projectStepResult = odcMonitorDataTaskAction.getProjectStepResultInner(parameters);
            log.info("Osc check increment checkpoint, expect greater than {}, current = {}",
                    safeDataCheckpoint, projectStepResult.getIncrementCheckpoint());
            if (odcMonitorDataTaskAction.isMigrateTaskReady(projectStepResult)
                    && projectStepResult.getIncrementCheckpoint() > safeDataCheckpoint) {
                return true;
            }
            if (System.currentTimeMillis() < checkTimeoutMs) {
                OmsRequestUtil.sleep(1000);
            } else {
                return false;
            }
        }
    }
}
