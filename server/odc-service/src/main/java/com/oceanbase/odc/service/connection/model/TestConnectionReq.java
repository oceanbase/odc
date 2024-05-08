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
import java.util.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.model.oracle.UserRole;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLConfig;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLFileEntry;

import lombok.Data;
import lombok.ToString;

/**
 * @author yizhou.xw
 * @version : TestConnectionReq.java, v 0.1 2021-07-21 9:36 PM
 */
@Data
@ToString(exclude = {"password"})
public class TestConnectionReq implements CloudConnectionConfig, SSLConnectionConfig {
    /**
     * Connection ID，用于编辑连接页面未传密码参数时从已保存的连接信息中获取对应密码字段
     */
    private Long id;
    /**
     * 连接类型
     */
    private ConnectType type;
    /**
     * 兼容模式
     */
    private DialectType dialectType;
    /**
     * 账号类型，用于编辑连接页面未传密码参数时从已保存的连接信息中获取对应密码字段
     */
    private ConnectionAccountType accountType = ConnectionAccountType.MAIN;

    /**
     * 连接 Host, 阿里云公有云、多云 环境此字段值基于 OCP API 获取，传参无效
     */
    @Size(max = 256, message = "Host for connection is out of range [1,256]")
    private String host;

    /**
     * 连接 Port, 阿里云公有云、多云 环境此字段值基于 OCP API 获取，传参无效
     */
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
     * 默认 schema，对应 /api/v1 的 defaultDBName 字段
     */
    @Size(max = 256, message = "default schema name out of range [0,256]")
    private String defaultSchema;

    @NotBlank
    @Size(min = 1, max = 128, message = "Username is out of range [1,128]")
    private String username;

    @JsonProperty(value = "password", access = Access.WRITE_ONLY)
    @SensitiveInput
    private String password;

    /**
     * Oracle 连接方式特有的参数，该参数表示一个数据库
     */
    private String serviceName;

    /**
     * Oracle 连接方式特有的参数，该参数表示数据库的一个实例
     */
    private String sid;

    /**
     * Oracle 连接方式特有的参数，该参数用户角色
     */
    private UserRole userRole;

    @JsonIgnore
    private transient OBTenantEndpoint endpoint;

    @JsonIgnore
    private String OBTenantName;

    @JsonIgnore
    private OBInstanceType instanceType;

    /**
     * SSL 安全设置
     */
    private SSLConfig sslConfig;

    @JsonIgnore
    private SSLFileEntry sslFileEntry;

    @Size(max = 8192, message = "Session init script is out of range [0,8192]")
    private String sessionInitScript;

    private Map<String, Object> jdbcUrlParameters;

    @JsonIgnore
    private OBInstanceRoleType instanceRoleType;

    public DialectType getDialectType() {
        if (Objects.nonNull(this.type)) {
            return this.type.getDialectType();
        }
        return this.dialectType;
    }

    public static TestConnectionReq fromConnection(ConnectionConfig connection,
            ConnectionAccountType accountType) {
        PreConditions.notNull(accountType, "AccountType");
        TestConnectionReq req = new TestConnectionReq();
        req.setId(connection.getId());
        req.setAccountType(accountType);
        req.setType(connection.getType());
        req.setDialectType(connection.getDialectType());
        req.setHost(connection.getHost());
        req.setPort(connection.getPort());
        req.setClusterName(connection.getClusterName());
        req.setDefaultSchema(connection.getDefaultSchema());
        if (accountType == ConnectionAccountType.MAIN) {
            req.setTenantName(connection.getTenantName());
            req.setUsername(connection.getUsername());
            req.setPassword(connection.getPassword());
        } else if (accountType == ConnectionAccountType.READONLY) {
            req.setTenantName(connection.getTenantName());
            req.setUsername(connection.getReadonlyUsername());
            req.setPassword(connection.getReadonlyPassword());
        } else {
            req.setTenantName("sys");
            req.setUsername(connection.getSysTenantUsername());
            req.setPassword(connection.getSysTenantPassword());
            req.setDefaultSchema(null);
        }
        req.setSslConfig(connection.getSslConfig());
        req.setSslFileEntry(connection.getSslFileEntry());
        req.setSid(connection.getSid());
        req.setServiceName(connection.getServiceName());
        req.setUserRole(connection.getUserRole());
        return req;
    }

    public void fillPasswordIfNull(ConnectionConfig connection) {
        PreConditions.notNull(connection, "connection");
        if (Objects.nonNull(this.password)) {
            return;
        }
        switch (accountType) {
            case MAIN:
                this.password = connection.getPassword();
                break;
            case SYS_READ:
                this.password = connection.getSysTenantPassword();
                break;
            case READONLY:
                this.password = connection.getReadonlyPassword();
            default:
        }
    }

    @Override
    public String getOBTenantName() {
        return this.OBTenantName;
    }

    @Override
    public void setSysTenantUsername(String userName) {
        // TODO
    }

    @Override
    public void setSysTenantPassword(String password) {
        // TODO
    }
}
