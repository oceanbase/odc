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
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.automation.EventMetadataEntity;

import lombok.Data;

@Data
public class AutomationEventMetadata {
    private Long id;
    private String name;
    private List<String> variables;
    private Boolean builtin;
    private Boolean hidden;
    @Internationalizable
    private String description;
    private Long creatorId;
    private Long organizationId;
    private Long lastModifierId;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;

    public AutomationEventMetadata() {}

    public AutomationEventMetadata(EventMetadataEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.variables = JsonUtils.fromJsonList(entity.getVariableNames(), String.class);
        this.builtin = entity.getBuiltin();
        this.hidden = entity.getHidden();
        this.description = entity.getDescription();
        this.creatorId = entity.getCreatorId();
        this.organizationId = entity.getOrganizationId();
        this.lastModifierId = entity.getLastModifierId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
    }
}
