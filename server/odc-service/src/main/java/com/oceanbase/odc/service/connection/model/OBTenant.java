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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 多云/公有云 租户
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OBTenant {
    /**
     * 租户 ID
     */
    private String id;

    /**
     * 租户名称
     */
    private String name;

    /**
     * observer 上真实的租户名
     */
    private String obTenantName;

    /**
     * 租户兼容模式，可选值 mysql/oracle
     */
    private OBTenantMode tenantMode;

    /**
     * 集群 ID
     */
    @JsonAlias("instanceId")
    private String clusterInstanceId;

    private OBInstanceType instanceType;

    private OBInstanceRoleType instanceRole;

    public static OBTenant of(String clusterInstanceId, String tenantId) {
        OBTenant obTenant = new OBTenant();
        obTenant.setClusterInstanceId(clusterInstanceId);
        obTenant.setId(tenantId);
        return obTenant;
    }

}
