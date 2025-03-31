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
package com.oceanbase.odc.service.task.base.precheck;

import java.io.Serializable;

import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;

import lombok.Data;
import lombok.NonNull;

/**
 * @Author: ysj
 * @Date: 2025/3/31 14:07
 * @Since: 4.3.4
 * @Description: For flow instance that applies database/table permission, it can confirm risklevel
 *               and to store on {@link TaskEntity#getParametersJson()}
 */
@Data
public class PreCheckRiskLevel implements Serializable {

    private static final long serialVersionUID = -612562837803359928L;
    /**
     * ref {@link RiskLevel#getId()} ()}
     */
    private Long riskLevelId;

    /**
     * ref {@link RiskLevel#getLevel()} ()}
     */
    private Integer riskLevel;

    /**
     * ref {@link ApprovalFlowConfig#getExecutionExpirationIntervalSeconds()}
     */
    private Integer executionExpirationIntervalSeconds;

    public static PreCheckRiskLevel from(@NonNull RiskLevel riskLevel) {
        ApprovalFlowConfig approvalFlowConfig = riskLevel.getApprovalFlowConfig();
        PreCheckRiskLevel preCheckRiskLevel = new PreCheckRiskLevel();
        preCheckRiskLevel.setRiskLevelId(riskLevel.getId());
        preCheckRiskLevel.setRiskLevel(riskLevel.getLevel());
        if (approvalFlowConfig != null) {
            preCheckRiskLevel
                    .setExecutionExpirationIntervalSeconds(approvalFlowConfig.getExecutionExpirationIntervalSeconds());
        }
        return preCheckRiskLevel;
    }

    public static RiskLevel toRiskLevel(@NonNull PreCheckRiskLevel preCheckRiskLevel) {
        RiskLevel riskLevel = new RiskLevel();
        riskLevel.setId(preCheckRiskLevel.getRiskLevelId());
        riskLevel.setLevel(preCheckRiskLevel.getRiskLevel());
        if (preCheckRiskLevel.getExecutionExpirationIntervalSeconds() != null) {
            ApprovalFlowConfig approvalFlowConfig = new ApprovalFlowConfig();
            approvalFlowConfig
                    .setExecutionExpirationIntervalSeconds(preCheckRiskLevel.getExecutionExpirationIntervalSeconds());
            riskLevel.setApprovalFlowConfig(approvalFlowConfig);
        }
        return riskLevel;
    }

}
