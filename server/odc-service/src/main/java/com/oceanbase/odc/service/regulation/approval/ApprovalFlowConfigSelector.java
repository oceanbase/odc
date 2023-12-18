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
package com.oceanbase.odc.service.regulation.approval;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;

/**
 * @Author: Lebie
 * @Date: 2023/6/19 13:45
 * @Description: []
 */
@Service
public class ApprovalFlowConfigSelector {
    @Autowired
    private ApprovalFlowConfigService approvalFlowConfigService;

    @Autowired
    private RiskLevelService riskLevelService;

    @Autowired
    private RiskDetectService riskDetectService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize("internal usage")
    public RiskLevel select(RiskLevelDescriber describer) {
        /**
         * if describer is over limit, return the highest risk level
         */
        if (describer.isOverLimit()) {
            return riskLevelService.findHighestRiskLevel();
        }
        /**
         * find all risk detect rules
         */
        List<RiskDetectRule> rules =
                riskDetectService.listAllByOrganizationId(authenticationFacade.currentOrganizationId());
        /**
         * get all hit risk level
         */
        Set<RiskLevel> matched = riskDetectService.detect(rules, describer);
        /**
         * determine the final selected risk level
         */
        return riskLevelService.findHighestRiskLevel(matched);
    }
}
