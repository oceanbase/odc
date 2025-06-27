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
package com.oceanbase.odc.service.workspace.model;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * when boundInstanceId,boundInstanceUsername,boundInstancePassword all are not blank, will update
 * instance account
 * 
 * @author keyang
 * @date 2024/09/13
 * @since 4.3.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class UpdateWorkspaceInstanceAccountReq {
    /**
     * use inner，not set value
     */
    private Long workspaceId;

    private String boundInstanceId;

    private String boundInstanceTenantId;

    @Size(max = 128, message = "boundInstanceUsername is out of range [0,128]")
    private String boundInstanceUsername;
    /**
     * The encrypted database account password passed in by the caller
     */
    @SensitiveInput
    @JsonProperty(value = "boundInstancePassword", access = Access.WRITE_ONLY)
    private String boundInstancePassword;

    public boolean isUpdateAccount() {
        return StringUtils.isNotBlank(boundInstanceId) && StringUtils.isNotBlank(boundInstanceUsername)
                && StringUtils.isNotBlank(boundInstancePassword);
    }
}
