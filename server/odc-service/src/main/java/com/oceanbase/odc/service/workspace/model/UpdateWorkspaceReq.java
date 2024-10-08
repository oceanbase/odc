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

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author keyang
 * @date 2024/09/13
 * @since 4.3.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class UpdateWorkspaceReq {
    @Size(min = 1, max = 64, message = "Workspace name is out of range [1,64]")
    private String name;

    @Size(max = 1024, message = "Workspace description is out of range [0,1024]")
    private String description;

    /**
     * when boundInstanceId,boundInstanceUsername,boundInstancePassword all are not blank, will update
     * instance account
     */
    private String boundInstanceId;
    /**
     * when it is null,it will not be updated
     */
    private String boundInstanceTenantId;
    @Size(max = 128, message = "boundInstanceUsername is out of range [0,128]")
    private String boundInstanceUsername;
    /**
     * The encrypted database account password passed in by the caller
     */
    @SensitiveInput
    @JsonProperty(value = "boundInstancePassword", access = Access.WRITE_ONLY)
    private String boundInstancePassword;

    public UpdateWorkspaceInstanceAccountReq toUpdateWorkspaceInstanceAccountReq(Long workspaceId) {
        return UpdateWorkspaceInstanceAccountReq.builder()
                .workspaceId(workspaceId)
                .boundInstanceId(boundInstanceId)
                .boundInstanceTenantId(boundInstanceTenantId)
                .boundInstanceUsername(boundInstanceUsername)
                .boundInstancePassword(boundInstancePassword)
                .build();
    }
}
