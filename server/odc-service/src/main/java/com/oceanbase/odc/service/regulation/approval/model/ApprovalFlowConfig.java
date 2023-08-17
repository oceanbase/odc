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
package com.oceanbase.odc.service.regulation.approval.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/5/10 16:22
 * @Description: []
 */
@Data
public class ApprovalFlowConfig implements SecurityResource, OrganizationIsolated {
    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @Size(min = 1, max = 128, message = "ApprovalFlowConfig name is out of range [1,128]")
    @Name(message = "ApprovalFlowConfig name cannot start or end with whitespaces")
    @Internationalizable
    private String name;

    @Size(max = 1024, message = "ApprovalFlowConfig description is out of range [1,1024]")
    @Internationalizable
    private String description;

    @NotNull
    private Integer approvalExpirationIntervalSeconds;

    @NotNull
    private Integer executionExpirationIntervalSeconds;

    private Integer waitExecutionExpirationIntervalSeconds;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer referencedCount;

    @Valid
    @NotEmpty
    private List<ApprovalNodeConfig> nodes;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtIn;

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_APPROVAL_FLOW_CONFIG.name();
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
