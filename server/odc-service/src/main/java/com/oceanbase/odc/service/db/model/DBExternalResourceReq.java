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
package com.oceanbase.odc.service.db.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/8/27 20:32
 * @since: 4.4.1
 */
@Data
public class DBExternalResourceReq {

    @NotBlank(message = "schemaName cannot be blank")
    private String schemaName;

    @NotBlank(message = "name cannot be blank")
    private String name;

    @Max(value = Integer.MAX_VALUE, message = "supportViewBytes is too large, max is " + Integer.MAX_VALUE + " bytes")
    private Integer supportViewBytes = 1 * 1024 * 1024;

    private Charset charset = StandardCharsets.UTF_8;

}
