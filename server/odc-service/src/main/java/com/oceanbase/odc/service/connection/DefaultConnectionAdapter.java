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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.connection.model.CloudConnectionConfig;
import com.oceanbase.odc.service.connection.model.OBDatabaseUser;
import com.oceanbase.odc.service.connection.model.OBInstanceType;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;

@Component
public class DefaultConnectionAdapter implements ConnectionAdapter {

    @Autowired
    private CloudMetadataClient cloudMetadataClient;

    /**
     * 连接配置适配，主要是确定连接的 Endpoint{@link OBTenantEndpoint} 配置
     */
    public <T extends CloudConnectionConfig> T adaptConfig(T connectionConfig) {
        PreConditions.notNull(connectionConfig, "connectionConfig");
        OBTenantEndpoint endpoint = new OBTenantEndpoint();
        endpoint.setAccessMode(cloudMetadataClient.oceanbaseAccessMode());
        String clusterName = connectionConfig.getClusterName();
        String tenantName = connectionConfig.getTenantName();
        if (cloudMetadataClient.supportsCloudMetadata()) {
            endpoint = cloudMetadataClient.getTenantEndpoint(connectionConfig.getClusterName(),
                    connectionConfig.getTenantName());
            connectionConfig.setHost(endpoint.getVirtualHost());
            connectionConfig.setPort(endpoint.getVirtualPort());
            OBTenant tenant = cloudMetadataClient.getTenant(clusterName, tenantName);
            connectionConfig.setInstanceRoleType(tenant.getInstanceRole());
            if (cloudMetadataClient.supportsTenantInstance()) {
                // if supports tenant/serverless instance, then we need to get the clusterName
                connectionConfig.setClusterName(tenant.getClusterInstanceId());
                connectionConfig.setTenantName(tenant.getId());
                connectionConfig.setInstanceType(
                        tenant.getInstanceType() == null ? OBInstanceType.CLUSTER : tenant.getInstanceType());
            } else {
                connectionConfig.setInstanceType(OBInstanceType.CLUSTER);
            }
            if (cloudMetadataClient.needsOBTenantName()) {
                // 需要使用 observer 上真实的 tenantName 来建连，防止备库切主后无法连接
                connectionConfig.setOBTenantName(tenant.getObTenantName());
            }
            if (cloudMetadataClient.needsSysTenantUser()) {
                // 需要使用 sys 租户账密
                OBDatabaseUser sysTenantUser = cloudMetadataClient.getSysTenantUser(connectionConfig.getClusterName());
                connectionConfig.setSysTenantUsername(sysTenantUser.getUserName());
                connectionConfig.setSysTenantPassword(sysTenantUser.getPassword());
            }
        }
        connectionConfig.setEndpoint(endpoint);
        return connectionConfig;
    }
}
