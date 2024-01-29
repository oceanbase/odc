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
package com.oceanbase.odc.service.collaboration.environment.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.oceanbase.odc.common.validate.Name;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/1/11 10:17
 * @Description: []
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnvironmentReq {
    @Name
    @Size(min = 1, max = 8, message = "The length of the environment name must be between 1 and 8")
    private String name;

    @Size(max = 256, message = "The length of the environment description must be between 0 and 256")
    private String description;

    @NotNull
    private EnvironmentStyle style;

    @NotNull
    private Long copiedRulesetId;

    @NotNull
    private Boolean enabled;
}
