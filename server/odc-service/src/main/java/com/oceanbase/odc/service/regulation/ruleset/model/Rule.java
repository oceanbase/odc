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
package com.oceanbase.odc.service.regulation.ruleset.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.NormalDialectTypeOutput;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/4/13 15:23
 * @Description: []
 */

@Data
public class Rule implements SecurityResource, OrganizationIsolated, Serializable {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    @Internationalizable
    private RuleMetadata metadata;

    @JsonProperty(access = Access.READ_ONLY)
    private Long rulesetId;

    /**
     * represents the rule level<br>
     * if type = SQL_CHECK, then level is 1, 2, and 3<br>
     * if type = SQL_CONSOLE, then level is always 0, which means this is a config, no level<br>
     */
    private Integer level;

    /**
     * represents which dialectTypes that the rule applied to<br>
     * if type = SQL_CONSOLE, then appliedDialectTypes is always null which means applied to all
     * dialectTypes<br>
     */
    @NormalDialectTypeOutput
    private List<DialectType> appliedDialectTypes;

    /**
     * rule properties map<br>
     * the key references to PropertyMetadata#getName<br>
     * the concrete type of value depends on PropertyMetadata#getType<br>
     */
    private Map<String, Object> properties;

    private Boolean enabled;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    @JsonProperty(access = Access.READ_ONLY)
    private RuleViolation violation;

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_RULE.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Data
    public static class RuleViolation {
        private String text;
        private int offset;
        private int start;
        private int stop;
        private int level;
        @Internationalizable
        private String localizedMessage;

        public static RuleViolation fromCheckViolation(CheckViolation checkViolation) {
            RuleViolation ruleViolation = new RuleViolation();
            ruleViolation.setOffset(checkViolation.getOffset());
            ruleViolation.setStart(checkViolation.getStart());
            ruleViolation.setStop(checkViolation.getStop());
            ruleViolation.setLevel(checkViolation.getLevel());
            ruleViolation.setLocalizedMessage(checkViolation.getLocalizedMessage());
            ruleViolation.setText(checkViolation.getText());
            return ruleViolation;
        }
    }
}
