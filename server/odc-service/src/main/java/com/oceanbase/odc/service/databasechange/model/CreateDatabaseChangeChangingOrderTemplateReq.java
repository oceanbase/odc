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
package com.oceanbase.odc.service.databasechange.model;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.oceanbase.odc.common.validate.Name;

import lombok.Data;

@Data
public class CreateDatabaseChangeChangingOrderTemplateReq {
    @NotBlank
    @Size(max = 256, message = "name is out of range [0, 256]")
    @Name(message = "Template name cannot start or end with whitespaces")
    private String name;

    @NotNull
    private Long projectId;

    @NotEmpty(message = "The number of databases must be greater than 1 and not more than 100")
    private List<@NotEmpty(message = "Each execution node must have at least one database") List<@NotNull(
            message = "Database cannot not be empty") Long>> orders;
}
