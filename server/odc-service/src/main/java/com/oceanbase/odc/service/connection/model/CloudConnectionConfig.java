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

import java.util.Map;

public interface CloudConnectionConfig {
    String getClusterName();

    String getTenantName();

    String getOBTenantName();

    Map<String, Object> getAttributes();

    OBInstanceType getInstanceType();

    OBInstanceRoleType getInstanceRoleType();

    Long getOrganizationId();

    void setClusterName(String clusterName);

    void setTenantName(String tenantName);

    void setHost(String host);

    void setPort(Integer port);

    void setOBTenantName(String obTenantName);

    void setEndpoint(OBTenantEndpoint endpoint);

    void setSysTenantUsername(String userName);

    void setSysTenantPassword(String password);

    void setInstanceType(OBInstanceType instanceType);

    void setInstanceRoleType(OBInstanceRoleType roleType);
}
