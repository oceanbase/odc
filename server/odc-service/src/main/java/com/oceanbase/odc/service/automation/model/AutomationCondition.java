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
package com.oceanbase.odc.service.automation.model;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.metadb.automation.AutomationConditionEntity;
import com.oceanbase.odc.service.automation.util.EventParseUtil;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2022/11/30
 */

@Data
public class AutomationCondition {
    private Long id;
    private Long ruleId;
    private String object;
    private String expression;
    private String operation;
    private String value;
    private Boolean enabled;
    private Long creatorId;
    private Long organizationId;
    private Long lastModifierId;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;

    public AutomationCondition() {}

    public AutomationCondition(String object, String expression, String operation, String value) {
        this.object = object;
        this.expression = expression;
        this.operation = operation;
        this.value = value;
    }

    public static AutomationCondition of(AutomationConditionEntity entity) {
        AutomationCondition condition = new AutomationCondition();
        condition.setId(entity.getId());
        condition.setRuleId(entity.getRuleId());
        condition.setObject(entity.getObject());
        condition.setExpression(entity.getExpression());
        condition.setOperation(entity.getOperation());
        condition.setValue(entity.getValue());
        condition.setEnabled(entity.getEnabled());
        condition.setCreatorId(entity.getCreatorId());
        condition.setOrganizationId(entity.getOrganizationId());
        condition.setLastModifierId(entity.getLastModifierId());
        condition.setCreateTime(entity.getCreateTime());
        condition.setUpdateTime(entity.getUpdateTime());
        return condition;
    }

    public boolean validate(Object root) {
        return EventParseUtil.validate(root, operation, value);
    }
}
