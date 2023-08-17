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
package com.oceanbase.odc.service.connection.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;

import lombok.Data;

@Data
public class GenerateConnectionStringReq {
    @NotBlank
    @Size(min = 1, max = 64, message = "Host for connection is out of range [1,64]")
    private String host;

    @NotNull
    @Min(value = 1, message = "Port can not be smaller than 1")
    @Max(value = 65535, message = "Port can not be bigger than 65535")
    private Integer port;

    /**
     * OceanBase 集群称，对应 /api/v1 的 cluster 字段
     */
    @Size(max = 256, message = "Cluster name is out of range [0, 256]")
    private String clusterName;

    /**
     * OceanBase 租户名称，对应 /api/v1 的 tenant 字段
     */
    @Size(max = 256, message = "Tenant name is out of range [0,256]")
    private String tenantName;

    /**
     * 数据库登录用户名，对应 /api/v1 的 dbUser 字段
     */
    @NotBlank
    @Size(min = 1, max = 128, message = "Username is out of range [1,128]")
    private String username;

    @JsonProperty(value = "password", access = Access.WRITE_ONLY)
    @SensitiveInput
    private String password;

    /**
     * 默认 schema，对应 /api/v1 的 defaultDBName 字段
     */
    @Size(max = 256, message = "default schema name out of range [0,256]")
    private String defaultSchema;
}
