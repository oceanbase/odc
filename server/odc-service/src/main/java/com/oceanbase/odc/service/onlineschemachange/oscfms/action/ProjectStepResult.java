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

import java.util.Map;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;

import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2025/3/24 15:17
 */
@Data
public class ProjectStepResult {
    private PrecheckResult preCheckResult;

    private double precheckProgressPercentage;

    private String errorMsg;

    private String currentStep;

    private String currentStepStatus;
    private TaskStatus taskStatus;

    /**
     * full transfer estimated rows count
     */
    private Long fullTransferEstimatedCount;

    /**
     * full transfer finished rows count
     */
    private Long fullTransferFinishedCount;

    /**
     * full transfer percentage
     */
    private double fullTransferProgressPercentage;

    /**
     * full verification result
     */
    private FullVerificationResult fullVerificationResult;

    /**
     * full verification result description
     */
    private String fullVerificationResultDescription;

    /**
     * full verification percentage
     */
    private double fullVerificationProgressPercentage;

    /**
     * task percentage
     */
    private double taskPercentage;


    private Map<OscStepName, Long> checkFailedTime;

    private Long incrementCheckpoint;
}
