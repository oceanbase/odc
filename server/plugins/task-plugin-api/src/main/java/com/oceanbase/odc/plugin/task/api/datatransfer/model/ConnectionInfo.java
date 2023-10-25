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

package com.oceanbase.odc.plugin.task.api.datatransfer.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"password", "sysTenantPassword"})
public class ConnectionInfo {

    @Size(max = 256, message = "Host for connection is out of range [1,256]")
    private String host;

    @Min(value = 1, message = "Port can not be smaller than 1")
    @Max(value = 65535, message = "Port can not be bigger than 65535")
    private Integer port;

    @Size(max = 256, message = "Cluster name is out of range [0, 256]")
    private String clusterName;

    @Size(max = 256, message = "Tenant name is out of range [0,256]")
    private String tenantName;

    @NotBlank
    @Size(min = 1, max = 128, message = "Username is out of range [1,128]")
    private String username;

    private String password;

    @Size(max = 128, message = "Sys tenant username is out of range [0,128]")
    private String sysTenantUsername;

    @SensitiveInput
    private String sysTenantPassword;

    @Size(max = 128, message = "Schema name is out of range [0, 128]")
    private String defaultSchema;

    private String proxyHost;

    private Integer proxyPort;

    private String OBTenant;

    private String jdbcUrl;

    private ConnectType connectType;

    public String getUserNameForConnect() {
        String username = ConnectionSessionUtil.getUserOrSchemaString(this.username, connectType.getDialectType());
        if (DialectType.OB_ORACLE.equals(connectType.getDialectType())) {
            username = "\"" + username + "\"";
        }
        if (StringUtils.isNotBlank(OBTenant)) {
            username = username + "@" + OBTenant;
        } else if (StringUtils.isNotBlank(tenantName)) {
            username = username + "@" + tenantName;
        }
        if (StringUtils.isNotBlank(clusterName)) {
            username = username + "#" + clusterName;
        }
        return username;
    }

}
