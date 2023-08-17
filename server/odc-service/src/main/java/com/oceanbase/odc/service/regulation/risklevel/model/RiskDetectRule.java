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

import java.util.Date;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.common.model.InnerUser;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/5/11 14:24
 * @Description: []
 */
@Data
@Valid
public class RiskDetectRule {
    private Long id;

    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    /**
     * rule root node
     */
    @NotNull
    @Valid
    private BaseTreeNode rootNode;

    @NotNull
    @Valid
    private Long riskLevelId;

    /**
     * if the conditions match, then the ticket would map to the action which is a RiskLevel
     */
    @JsonProperty(access = Access.READ_ONLY)
    private RiskLevel riskLevel;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtIn;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser creator;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;
}
