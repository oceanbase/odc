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
package com.oceanbase.odc.metadb.automation;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.automation.model.AutomationAction;

import lombok.Data;

@Data
@Entity
@Table(name = "automation_action")
public class AutomationActionEntity {

    /**
     * Id for trigger action
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Id of the rule
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "args_json_array")
    private String argsJsonArray;

    /**
     * Enabled or not
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    /**
     * Creator id, references iam_user(id)
     */
    @Column(name = "creator_id", updatable = false, nullable = false)
    private Long creatorId;

    /**
     * Organization id, references iam_organization(id)
     */
    @Column(name = "organization_id", updatable = false, nullable = false)
    private Long organizationId;

    /**
     * Last modifier id, references iam_user(id)
     */
    @Column(name = "last_modifier_id")
    private Long lastModifierId;

    /**
     * Record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;

    /**
     * Record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updateTime;

    public AutomationActionEntity() {}

    public AutomationActionEntity(AutomationAction action) {
        this.ruleId = action.getRuleId();
        this.action = action.getAction();
        this.argsJsonArray = JsonUtils.toJson(action.getArguments());
        this.enabled = action.getEnabled();
        this.creatorId = action.getCreatorId();
        this.organizationId = action.getOrganizationId();
        this.lastModifierId = action.getLastModifierId();
    }

}
