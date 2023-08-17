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

import static com.oceanbase.odc.service.automation.model.TriggerEvent.OAUTH_2_FIRST_TIME_LOGIN;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.automation.model.AutomationConstants;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.automation.util.EventParseUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class OAuth2FirstLoginEventHandler extends AbstractAutomationEventHandler {

    @Override
    public boolean support(TriggerEvent triggerEvent) {
        return OAUTH_2_FIRST_TIME_LOGIN.equals(triggerEvent.getEventName());
    }

    @Override
    public boolean match(AutomationCondition condition, TriggerEvent event) {
        Object source = event.getSource();
        Object organizationId = ((Map) source).get("organizationId");
        if (!Objects.equals(organizationId, condition.getOrganizationId())) {
            return false;
        }
        String operation = condition.getOperation();
        if (!AutomationService.checkOperation(operation)) {
            log.warn("Not allowed operation:" + condition + " event name=" + event.getEventName());
        }
        Object root = EventParseUtil.parseObject(source, condition.getExpression());
        return EventParseUtil.validate(root, operation, condition.getValue());
    }

    @Override
    public List<Pair<String, ActionHandler<Object>>> registerActionHandlers() {
        Pair<String, ActionHandler<Object>> bindRoleActionHandler =
                new Pair<>(AutomationConstants.BIND_ROLE, (action, source) -> {
                    long userId = (long) ((Map) source).get("odcUserId");
                    bindRole(userId, action);
                });

        Pair<String, ActionHandler<Object>> permissionActionHandler =
                new Pair<>(AutomationConstants.BIND_PERMISSION, (action, source) -> {
                    long userId = (long) ((Map) source).get("odcUserId");
                    bindPermission(userId, action);
                });

        Pair<String, ActionHandler<Object>> bindProjectRoleHandler =
                new Pair<>(AutomationConstants.BIND_PROJECT_ROLE, (action, source) -> {
                    long userId = (long) ((Map) source).get("odcUserId");
                    bindProjectRole(userId, action);
                });

        return Arrays.asList(bindRoleActionHandler, permissionActionHandler, bindProjectRoleHandler);
    }
}
