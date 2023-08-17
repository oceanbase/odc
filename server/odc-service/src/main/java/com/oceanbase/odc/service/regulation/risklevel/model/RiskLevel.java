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
package com.oceanbase.odc.service.regulation.risklevel.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelStyle;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/5/11 14:11
 * @Description: []
 */
@Data
public class RiskLevel {
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    @Internationalizable
    private String name;

    @Internationalizable
    private String description;

    /**
     * 0 represents default risk level<br>
     * 1 represents low risk level<br>
     * 2 represents moderate risk level<br>
     * 3 represents high risk level<br>
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Integer level;

    @JsonProperty(access = Access.READ_ONLY)
    private RiskLevelStyle style;

    @NotNull
    @Valid
    private Long approvalFlowConfigId;

    @JsonProperty(access = Access.READ_ONLY)
    private ApprovalFlowConfig approvalFlowConfig;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;
}
