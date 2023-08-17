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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.automation.AutomationActionEntity;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2022/11/30
 */

@Data
public class AutomationAction {
    private Long id;
    private Long ruleId;
    private String action;
    private Map<String, Object> arguments;
    private Boolean enabled;
    private Long creatorId;
    private Long organizationId;
    private Long lastModifierId;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;

    public AutomationAction() {}

    public AutomationAction(String action, Map<String, Object> arguments) {
        this.action = action;
        this.arguments = arguments;
    }

    public static AutomationAction of(AutomationActionEntity entity) {
        AutomationAction automationAction = new AutomationAction();
        automationAction.setId(entity.getId());
        automationAction.setRuleId(entity.getRuleId());
        automationAction.setAction(entity.getAction());
        automationAction.setArguments(JsonUtils.fromJsonMap(entity.getArgsJsonArray(), String.class, Object.class));
        automationAction.setEnabled(entity.getEnabled());
        automationAction.setCreatorId(entity.getCreatorId());
        automationAction.setOrganizationId(entity.getOrganizationId());
        automationAction.setLastModifierId(entity.getLastModifierId());
        automationAction.setCreateTime(entity.getCreateTime());
        automationAction.setUpdateTime(entity.getUpdateTime());
        return automationAction;
    }

}
