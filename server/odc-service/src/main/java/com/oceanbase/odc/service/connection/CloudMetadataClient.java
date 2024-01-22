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
package com.oceanbase.odc.service.connection;

import java.util.List;

import javax.validation.constraints.NotBlank;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.service.connection.model.OBDatabaseUser;
import com.oceanbase.odc.service.connection.model.OBInstance;
import com.oceanbase.odc.service.connection.model.OBInstanceType;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.connection.model.OceanBaseAccessMode;

public interface CloudMetadataClient {
    /**
     * if cloud metadata supports, ODC will use cluster/tenant metadata from cloud service
     */
    boolean supportsCloudMetadata();

    boolean supportsTenantInstance();

    boolean needsOBTenantName();

    boolean needsSysTenantUser();

    /**
     * Permission check hook. If you want to check some permission additionally, you can override this
     * method. The default implementation will always return true.
     * 
     * @param tenant, the instance to be checked
     * @param action, enum: "read", "write"
     * @return true if permission check passed, otherwise false
     */
    default void additionalAuthenticationChecks(OBTenant tenant, OBInstanceType type, String action)
            throws AccessDeniedException {}

    /**
     * if cluster name should be included in jdbc username
     */
    boolean includeClusterNameForJdbcUsername();

    OceanBaseAccessMode oceanbaseAccessMode();

    /**
     * 获取用户有权限的集群列表
     */
    List<OBInstance> listInstances();

    /**
     * 获取集群下租户列表
     */
    List<OBTenant> listTenants(@NotBlank String instanceId);

    /**
     * 获取集群下的租户详情
     */
    OBTenant getTenant(@NotBlank String instanceId, @NotBlank String tenantId);

    /**
     * 获取租户下数据库用户列表
     */
    List<OBDatabaseUser> listDatabaseUsers(@NotBlank String instanceId, @NotBlank String tenantId);

    /**
     * 获取租户的连接 Endpoint 配置，用于 IC 通道连接
     */
    OBTenantEndpoint getTenantEndpoint(@NotBlank String instanceId, @NotBlank String tenantId);

    OBDatabaseUser getSysTenantUser(@NotBlank String instanceId);
}
