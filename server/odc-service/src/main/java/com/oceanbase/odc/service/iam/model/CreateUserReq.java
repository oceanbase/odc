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

import java.util.List;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.common.validate.Name;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wenniu.ly
 * @date 2021/6/28
 */

@Getter
@Setter
public class CreateUserReq {
    @Size(min = 1, max = 64, message = "User name is out of range [1,64]")
    @Name(message = "User name cannot start or end with whitespaces")
    private String name;
    @Size(min = 1, max = 64, message = "User account name is out of range [1,64]")
    @Name(message = "User account name cannot start or end with whitespaces")
    private String accountName;
    @SensitiveInput
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private boolean enabled = true;
    private List<Long> roleIds;
    private String description;
    private String errorMessage;
}
