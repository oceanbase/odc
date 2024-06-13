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

import static com.oceanbase.odc.service.automation.model.TriggerEvent.LOGIN_SUCCESS;
import static com.oceanbase.odc.service.automation.model.TriggerEvent.USER_CREATED;
import static com.oceanbase.odc.service.automation.model.TriggerEvent.USER_UPDATED;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.json.JacksonFactory;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.automation.model.AutomationConstants;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.iam.model.User;

@Service
@SkipAuthorize
public class UserChangeAutomationEventHandler extends AbstractAutomationEventHandler {

    private final String EXTRA_INFO_PREFIX = "extra#";

    private final ObjectMapper objectMapper = JacksonFactory.jsonMapper();

    @Override
    public boolean support(TriggerEvent triggerEvent) {
        return USER_CREATED.equals(triggerEvent.getEventName()) || USER_UPDATED.equals(triggerEvent.getEventName())
                || LOGIN_SUCCESS.equals(triggerEvent.getEventName());
    }

    /**
     * 重写match方法，用于判断自动化条件是否匹配触发事件
     *
     * @param condition    自动化条件
     * @param triggerEvent 触发事件
     * @return 匹配结果，true表示匹配，false表示不匹配
     */
    @Override
    protected boolean match(AutomationCondition condition, TriggerEvent triggerEvent) {
        // 获取触发事件源用户
        User user = (User) triggerEvent.getSource();
        try {
            // 获取条件表达式
            String filed = condition.getExpression();
            // 判断条件表达式是否以EXTRA_INFO_PREFIX开头
            if (condition.getExpression().startsWith(EXTRA_INFO_PREFIX)) {
                // 如果是，则去掉前缀后获取实际字段名
                filed = condition.getExpression().substring(EXTRA_INFO_PREFIX.length());
                // 从用户的额外属性中读取JSON节点
                JsonNode jsonNode = objectMapper.readTree(user.getExtraProperties());
                // 调用条件的validate方法进行校验
                return condition.validate(jsonNode.get(filed));
            } else {
                // 如果不是，则直接从用户对象中获取字段值
                String root = BeanUtils.getProperty(user, filed);
                return condition.validate(root);
            }
        } catch (Exception e) {
            // 如果出现异常，则返回false
            return false;
        }
    }

    @Override
    protected List<Pair<String, ActionHandler<Object>>> registerActionHandlers() {

        Pair<String, ActionHandler<Object>> bindRoleActionHandler =
                new Pair<>(AutomationConstants.BIND_ROLE, (action, source) -> {
                    User user = (User) source;
                    bindRole(user.getId(), action);
                });

        Pair<String, ActionHandler<Object>> permissionActionHandler =
                new Pair<>(AutomationConstants.BIND_PERMISSION, (action, source) -> {
                    User user = (User) source;
                    bindPermission(user.getId(), action);
                });

        Pair<String, ActionHandler<Object>> bindProjectRoleHandler =
                new Pair<>(AutomationConstants.BIND_PROJECT_ROLE, (action, source) -> {
                    User user = (User) source;
                    bindProjectRole(user.getId(), action);
                });

        return Arrays.asList(bindRoleActionHandler, permissionActionHandler, bindProjectRoleHandler);
    }

}
