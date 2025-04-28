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

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.MapUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.MonitorDataTaskActionBase;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/03/24 20:04
 */
@Slf4j
public class OdcMonitorDataTaskAction extends MonitorDataTaskActionBase {

    public OdcMonitorDataTaskAction(@NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        super(onlineSchemaChangeProperties);
    }

    /**
     * rebuild ProjectStepResult from supervisor response
     * 
     * @param taskParameter
     * @return
     */
    protected ProjectStepResult getProjectStepResult(OnlineSchemaChangeScheduleTaskParameters taskParameter,
            OnlineSchemaChangeScheduleTaskResult lastResult) {
        return getProjectStepResultInner(taskParameter.getOdcCommandURl());
    }

    protected ProjectStepResult getProjectStepResultInner(String commandURL) {
        ProjectStepResult projectStepResult = new ProjectStepResult();
        SupervisorResponse supervisorResponse = OscCommandUtil.monitorTask(commandURL);
        if (null == supervisorResponse || !supervisorResponse.isSuccess()) {
            log.info("OdcMonitorDataTaskAction: supervisor response failed, response = {}", supervisorResponse);
            return null;
        }
        // response like {
        // "errorMessage":null,
        // "responseData":
        // {"data":"\"
        // {
        // \\\"checkpoint\\\":\\\"1742804267\\\",
        // \\\"enableIncrementMigrator\\\":\\\"true\\\",
        // \\\"estimateMigrateRows\\\":\\\"0\\\",
        // \\\"enableFullMigrator\\\":\\\"true\\\",
        // \\\"fullMigratorProgress\\\":\\\"0\\\",
        // \\\"fullMigratorDone\\\":\\\"false\\\",
        // \\\"tableTotalRows\\\":\\\"0\\\"
        // }\""
        // },
        // "success":true}
        Map<String, String> payload = supervisorResponse.getResponseData();
        if (MapUtils.isEmpty(payload) || StringUtils.isEmpty(payload.get("data"))) {
            log.info("OdcMonitorDataTaskAction: supervisor response invalid, response = {}", supervisorResponse);
            return null;
        }
        Map<String, String> dataMap = JsonUtils.fromJson(payload.get("data"), Map.class);
        // fill common fields
        projectStepResult.setPreCheckResult(PrecheckResult.FINISHED);
        projectStepResult.setTaskStatus(TaskStatus.RUNNING);
        projectStepResult.setFullVerificationResult(FullVerificationResult.UNCHECK);
        projectStepResult.setFullVerificationResultDescription("unchecked");
        projectStepResult.setFullVerificationProgressPercentage(0.0);
        // adapt steps
        try {
            projectStepResult.setFullTransferFinishedCount(getAndCheckValue(dataMap, "tableTotalRows", Long::valueOf));
            projectStepResult
                    .setFullTransferEstimatedCount(getAndCheckValue(dataMap, "estimateMigrateRows", Long::valueOf));
            projectStepResult.setFullTransferProgressPercentage(
                    getAndCheckValue(dataMap, "fullMigratorProgress", Double::valueOf));
            boolean fullMigrateDone = getAndCheckValue(dataMap, "fullMigratorDone", Boolean::valueOf);
            if (fullMigrateDone) {
                projectStepResult.setCurrentStep(OscStepName.TRANSFER_APP_SWITCH.name());
            } else {
                projectStepResult.setCurrentStep(OscStepName.FULL_TRANSFER.name());
            }
            projectStepResult.setCurrentStepStatus(OmsStepStatus.RUNNING.name());
            projectStepResult.setCheckFailedTime(Collections.emptyMap());
            projectStepResult.setIncrementCheckpoint(getAndCheckValue(dataMap, "checkpoint", Long::valueOf));
            projectStepResult.setTaskPercentage(
                    Math.min(95.0, getAndCheckValue(dataMap, "fullMigratorProgress", Double::valueOf)));
            return projectStepResult;
        } catch (Exception e) {
            log.info("OdcMonitorDataTaskAction: supervisor response not contains all fields, response = {}",
                    supervisorResponse);
            return null;
        }
    }


    protected static <T> T getAndCheckValue(Map<String, String> data, String key, Function<String, T> valCaster) {
        String val = data.get(key);
        if (null == val) {
            throw new RuntimeException(key + " not provided");
        }
        return valCaster.apply(val);
    }

    protected boolean isMigrateTaskReady(ProjectStepResult projectStepResult) {
        // step into increment transfer and checkpoint delay less than 5 seconds
        return OscStepName.valueOf(projectStepResult.getCurrentStep()) == OscStepName.TRANSFER_APP_SWITCH &&
                (System.currentTimeMillis() / 1000 - projectStepResult.getIncrementCheckpoint() <= 5);
    }

    @Override
    protected String getPrintLogName(OnlineSchemaChangeScheduleTaskParameters parameters) {
        return parameters.getOdcCommandURl();
    }
}
