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
package com.oceanbase.odc.service.onlineschemachange.model;

import com.oceanbase.odc.common.json.NormalDialectTypeOutput;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * online schema change task result
 *
 * @author yaobin
 * @date 2023-05-24
 * @since 4.2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineSchemaChangeScheduleTaskResult {
    @NormalDialectTypeOutput
    private DialectType dialectType;

    /**
     * precheck result
     */
    private PrecheckResult precheckResult;

    /**
     * precheck result description
     */
    private String precheckResultDescription;

    /**
     * full transfer estimated rows count
     */
    private Long fullTransferEstimatedCount = 0L;

    /**
     * full transfer finished rows count
     */
    private Long fullTransferFinishedCount = 0L;

    /**
     * full transfer percentage
     */
    private Double fullTransferProgressPercentage = 0.0;

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
    private Double fullVerificationProgressPercentage = 0.0;

    /**
     * origin table name
     */
    private String originTableName;

    /**
     * origin table name is renamed to the old table name
     */
    private String oldTableName;

    /**
     * current step
     */
    private String currentStep;

    /**
     * current step
     */
    private String currentStepStatus;


    /**
     * origin table ddl
     */
    private String originTableDdl;

    /**
     * new table ddl
     */
    private String newTableDdl;

    /**
     * enable open manual swap table entry point
     */
    private boolean manualSwapTableEnabled;

    /**
     * manual start swap table
     */
    private boolean manualSwapTableStarted;


    public OnlineSchemaChangeScheduleTaskResult(OnlineSchemaChangeScheduleTaskParameters taskParam) {
        this.originTableName = taskParam.getOriginTableName();
        this.oldTableName = taskParam.getRenamedTableName();
        this.originTableDdl = taskParam.getOriginTableCreateDdl();
        this.newTableDdl = taskParam.getNewTableCreateDdlForDisplay();
        this.dialectType = taskParam.getDialectType();
    }

}
