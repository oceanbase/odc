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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;

import cn.hutool.core.util.ObjectUtil;

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

    @SkipAuthorize("internal usage")
    public Map<RiskLevelDescriber, RiskLevel> batchSelect(Collection<RiskLevelDescriber> describers) {
        if (CollectionUtils.isEmpty(describers)) {
            return Collections.emptyMap();
        }
        Set<RiskLevelDescriber> copiedDescribers = new HashSet<>(describers);
        Map<RiskLevelDescriber, RiskLevel> describer2RiskLevel = new HashMap<>();
        Iterator<RiskLevelDescriber> describerIterator = copiedDescribers.iterator();
        RiskLevel heighestRiskLevel = null;
        while (describerIterator.hasNext()) {
            RiskLevelDescriber describer = describerIterator.next();
            if (describer.isOverLimit()) {
                heighestRiskLevel =
                        ObjectUtil.defaultIfNull(heighestRiskLevel, riskLevelService.findHighestRiskLevel());
                describer2RiskLevel.put(describer, heighestRiskLevel);
                describerIterator.remove();
            }
        }
        if (CollectionUtils.isEmpty(copiedDescribers)) {
            return describer2RiskLevel;
        }
        List<RiskDetectRule> rules =
                riskDetectService.listAllByOrganizationId(authenticationFacade.currentOrganizationId());

        Map<RiskLevelDescriber, Set<RiskLevel>> describer2Matched =
                riskDetectService.batchDetect(rules, copiedDescribers);
        RiskLevel defaultRiskLevel = null;
        for (Map.Entry<RiskLevelDescriber, Set<RiskLevel>> entry : describer2Matched.entrySet()) {
            RiskLevelDescriber describer = entry.getKey();
            Set<RiskLevel> riskLevels = entry.getValue();
            defaultRiskLevel = ObjectUtil.defaultIfNull(defaultRiskLevel, riskLevelService.findHighestRiskLevel());
            RiskLevel matched = riskLevels.stream()
                    .filter(r -> Objects.nonNull(r) && Objects.nonNull(r.getLevel()))
                    .max(Comparator.comparingInt(RiskLevel::getLevel))
                    .orElse(defaultRiskLevel);
            describer2RiskLevel.put(describer, matched);
        }
        return describer2RiskLevel;
    }

    @SkipAuthorize("internal usage")
    public RiskLevel selectForMultipleDatabase() {
        return riskLevelService.findHighestRiskLevel();
    }

}
