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
package com.oceanbase.odc.service.iam.model;

import java.time.OffsetDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.metadb.iam.LoginHistoryEntity;
import com.oceanbase.odc.metadb.iam.LoginHistoryEntity.FailedReason;

import lombok.Data;

@Data
public class LoginHistory {
    private Long id;
    private Long userId;
    private Long organizationId;
    @NotBlank
    private String accountName;
    @NotNull
    private OffsetDateTime loginTime;
    private boolean success;
    private FailedReason failedReason;

    public LoginHistoryEntity toEntity() {
        LoginHistoryEntity entity = new LoginHistoryEntity();
        entity.setUserId(this.getUserId());
        entity.setOrganizationId(this.getOrganizationId());
        entity.setAccountName(this.getAccountName());
        entity.setLoginTime(this.getLoginTime());
        entity.setFailedReason(this.getFailedReason());
        entity.setSuccess(this.isSuccess());
        return entity;
    }

    public static LoginHistory fromEntity(LoginHistoryEntity entity) {
        LoginHistory model = new LoginHistory();
        model.setId(entity.getId());
        model.setUserId(entity.getUserId());
        model.setOrganizationId(entity.getOrganizationId());
        model.setAccountName(entity.getAccountName());
        model.setLoginTime(entity.getLoginTime());
        model.setFailedReason(entity.getFailedReason());
        model.setSuccess(entity.isSuccess());
        return model;
    }
}
