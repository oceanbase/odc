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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.automation.AutomationRuleEntity;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2022/11/30
 */

@Data
public class AutomationRule implements OrganizationIsolated {
    @JsonProperty(access = Access.READ_ONLY)
    private Long id;
    private String name;
    private Long eventId;
    private String eventName;
    private Boolean enabled;
    private Boolean builtin;
    private String description;
    private Long creatorId;
    private String creatorName;
    private Long organizationId;
    private Long lastModifierId;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;
    private List<AutomationCondition> conditions;
    private List<AutomationAction> actions;

    public AutomationRule() {}

    public AutomationRule(AutomationRuleEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.eventId = entity.getEventId();
        this.enabled = entity.getEnabled();
        this.builtin = entity.getBuiltIn();
        this.description = entity.getDescription();
        this.creatorId = entity.getCreatorId();
        this.organizationId = entity.getOrganizationId();
        this.lastModifierId = entity.getLastModifierId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_AUTOMATION_RULE.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }
}
