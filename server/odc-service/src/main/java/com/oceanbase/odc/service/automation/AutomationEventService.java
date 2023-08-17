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
package com.oceanbase.odc.service.automation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.CustomAttribute;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Service
public class AutomationEventService {

    @Autowired
    private IntegrationService integrationService;

    @PreAuthenticate(actions = "read", resourceType = "ODC_AUTOMATION_RULE", isForAll = true)
    public PromptVo promptExpression(String eventName) {
        if (TriggerEvent.isUserChangeEvent(eventName)) {
            PromptVo promptVo = new PromptVo();
            SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
            List<String> userInfo = Collections.singletonList("accountName");
            if (sSoClientRegistration == null) {
                promptVo.addExpression("User", userInfo);
                return promptVo;
            }
            List<CustomAttribute> extraInfo =
                    MoreObjects.firstNonNull(sSoClientRegistration.getMappingRule().getExtraInfo(), new ArrayList<>());
            List<String> extraInfoExpression =
                    extraInfo.stream().map(CustomAttribute::toAutomationExpression).collect(Collectors.toList());
            List<String> collect = Stream.concat(userInfo.stream(), extraInfoExpression.stream())
                    .collect(Collectors.toList());
            promptVo.addExpression("User", collect);
            return promptVo;
        }
        return new PromptVo();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class PromptVo {
        Map<String, List<String>> variableExpression;

        public PromptVo() {
            variableExpression = new HashMap<>();
        }

        public void addExpression(String variable, List<String> expressions) {
            variableExpression.put(variable, expressions);
        }
    }

}
