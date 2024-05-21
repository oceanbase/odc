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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ConnectEnvironmentType;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.plugin.connect.model.oracle.UserRole;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.service.collaboration.environment.model.EnvironmentStyle;
import com.oceanbase.odc.service.connection.ConnectionStatusManager.CheckState;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 *
 * @author yh263208
 * @date 2021-06-29 14:24
 * @since ODC_release_3.2.0
 */
@Data
@ToString(exclude = {"salt", "password", "sysTenantPassword", "readonlyPassword",
        "passwordEncrypted", "sysTenantPasswordEncrypted", "readonlyPasswordEncrypted", "sslFileEntry"})
public class ConnectionConfig
        implements SecurityResource, OrganizationIsolated, CloudConnectionConfig, SSLConnectionConfig, Serializable {
    private static final long serialVersionUID = -7198204983655038981L;
    private static final String SESSION_INIT_SCRIPT_KEY = "SESSION_INIT_SCRIPT";
    private static final String JDBC_URL_PARAMETERS_KEY = "JDBC_URL_PARAMETERS";
    /**
     * 连接ID，对应 /api/v1 的 sid 字段，注意这里和使用连接时的 sid 概念是不一样的，之前版本未区分，另外之前是 String 类型，现在统一为 Long 类型
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    /**
     * ownerId，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    @Deprecated
    private Long ownerId;

    /**
     * organization ID，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    /**
     * Creator userId，对应 /api/v1 的 userId 字段，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long creatorId;

    /**
     * Creator username，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private String creatorName;

    /**
     * 连接名称，对应 /api/v1 的 cluster 字段 sessionName
     */
    @Size(min = 1, max = 128, message = "Connection name is out of range [1,128]")
    @Name(message = "Connection name cannot start or end with whitespaces")
    private String name;

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

    /**
     * 连接类型
     * 
     * @since 3.3.0
     */
    @NotNull
    private ConnectType type;
    /**
     * 可见范围，可选值 PRIVATE/ORGANIZATION，创建后不可修改
     */
    @Deprecated
    private ConnectionVisibleScope visibleScope = ConnectionVisibleScope.ORGANIZATION;
    /**
     * 主机，公有云连接格式为 {domain}:{port} <br>
     * 阿里云公有云和多云环境，host 字段值由服务端通过调用 OCP API 确定，传参无效
     */
    @Size(max = 256, message = "Host for connection is out of range [1,256]")
    private String host;
    /**
     * 端口，公有云连接不需要设置 port <br>
     * 阿里云公有云和多云环境，port 字段值由服务端通过调用 OCP API 确定，传参无效
     */
    @Min(value = 1, message = "Port can not be smaller than 1")
    @Max(value = 65535, message = "Port can not be bigger than 65535")
    private Integer port;
    /**
     * OceanBase 集群称，对应 /api/v1 的 cluster 字段，公有云连接不需要设置 clusterName
     */
    @Size(max = 256, message = "Cluster name is out of range [0, 256]")
    private String clusterName;
    /**
     * OceanBase 租户名称，对应 /api/v1 的 tenant 字段，公有云连接不需要设置 tenantName
     */
    @Size(max = 256, message = "Tenant name is out of range [0,256]")
    private String tenantName;
    /**
     * 数据库登录用户名，对应 /api/v1 的 dbUser 字段
     */
    @NotBlank
    @Size(min = 1, max = 128, message = "Username is out of range [1,128]")
    private String username;
    /**
     * 连接密码，null 表示不设置，空字符串表示空密码，当 passwordSaved=true 时，不能为 null，只写参数
     */
    @JsonProperty(value = "password", access = Access.WRITE_ONLY)
    @SensitiveInput
    private String password;
    /**
     * 是否置顶，true代表置顶，false反之
     */
    @Deprecated
    private Boolean setTop = false;
    /**
     * 连接设置的 id 列表
     */
    @Deprecated
    private Set<Long> labelIds;
    /**
     * 加密后的密码，调用端忽略此字段
     */
    @JsonIgnore
    private transient String passwordEncrypted;
    /**
     * 系统租户账号用户名称，对应 /api/v1 的 sysUser 字段
     */
    @Size(max = 128, message = "Sys tenant username is out of range [0,128]")
    private String sysTenantUsername;
    /**
     * 系统租户账号密码，对应 /api/v1 的 sysUserPassword 字段, sysTenantUsername 为 null 时无效，空字符串表示空密码，只写参数
     */
    @JsonProperty(value = "sysTenantPassword", access = Access.WRITE_ONLY)
    @SensitiveInput
    private String sysTenantPassword;
    /**
     * 加密后的密码，调用端忽略此字段
     */
    @JsonIgnore
    private transient String sysTenantPasswordEncrypted;
    /**
     * 只读账号用户名称
     */
    @Deprecated
    private String readonlyUsername;
    /**
     * 只读账号密码, readonlyUsername 为 null 时无效，空字符串表示空密码，只写参数
     */
    @JsonProperty(value = "readonlyPassword", access = Access.WRITE_ONLY)
    @SensitiveInput
    @Deprecated
    private String readonlyPassword;
    /**
     * 加密后的密码，调用端忽略此字段
     */
    @JsonIgnore
    @Deprecated
    private transient String readonlyPasswordEncrypted;
    /**
     * 默认 schema，对应 /api/v1 的 defaultDBName 字段
     */
    @Size(max = 128, message = "Schema name is out of range [0, 128]")
    private String defaultSchema;
    /**
     * 查询超时时间（单位：秒），对应 /api/v1 的 sessionTimeoutS 字段
     */
    @Min(value = 0, message = "Timeout for connection can not be negative")
    @Deprecated
    private Integer queryTimeoutSeconds = 10;
    /**
     * 创建时间，对应 /api/v1 的 gmtCreated 字段，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;
    /**
     * 修改时间，对应 /api/v1 的 gmtModified 字段，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;
    /**
     * 最近一次访问连接时间，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Date lastAccessTime;
    /**
     * 是否启用
     */
    @Deprecated
    private Boolean enabled = true;
    /**
     * 是否保存密码
     */
    private Boolean passwordSaved = true;
    /**
     * Extend properties
     */
    private Map<String, String> properties;
    @JsonIgnore
    private Cipher cipher;
    @JsonIgnore
    private String salt;
    /**
     * 从 copyFromSid 的配置拷贝密码字段的值，对应 /api/v1 的 copyFromSid 字段 <br>
     * - 值为 null：不拷贝 <br>
     * - 值不为 null： 则当密码字段为 null 时，从 copyFromSid 连接信息里拷贝密码字段
     */
    @JsonProperty(access = Access.WRITE_ONLY)
    @Deprecated
    private Long copyFromId;
    /**
     * 连接所拥有的操作权限
     */
    private Set<String> permittedActions;
    /**
     * 连接状态，可选值 ACTIVE/INACTIVE/TESTING，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private CheckState status;

    /**
     * 是否临时连接配置，临时连接配置未使用持续一段周期后会被自动清理
     */
    private Boolean temp = false;

    /**
     * 支持的操作列表，可选值 create/delete/update/read
     */
    private Set<String> supportedOperations;

    /**
     * 连接 Endpoint
     */
    @JsonIgnore
    private OBTenantEndpoint endpoint;

    @JsonIgnore
    private String OBTenantName;

    @JsonIgnore
    private OBInstanceType instanceType;

    @JsonIgnore
    private OBInstanceRoleType instanceRoleType;

    @JsonIgnore
    private transient Map<String, Object> attributes;

    /**
     * SSL 安全设置
     */
    // TODO remove new SSLConfig() default value
    @NotNull
    private SSLConfig sslConfig = new SSLConfig();

    @JsonIgnore
    private SSLFileEntry sslFileEntry;

    private Long environmentId;

    @JsonProperty(access = Access.READ_ONLY)
    @Internationalizable
    private String environmentName;

    @JsonProperty(access = Access.READ_ONLY)
    private EnvironmentStyle environmentStyle;

    private Long projectId;

    @JsonProperty(access = Access.READ_ONLY)
    private String projectName;

    @JsonProperty(access = Access.READ_ONLY)
    private Date dbObjectLastSyncTime;

    /**
     * 连接类型，可选值 CONNECT_TYPE_CLOUD/CONNECT_TYPE_OB 。只读参数
     *
     * @deprecated use ConnectType type instead`
     */
    @Deprecated
    @JsonProperty(access = Access.READ_ONLY)
    public ConnectEnvironmentType getConnectType() {
        return Objects.isNull(this.port) ? ConnectEnvironmentType.CONNECT_TYPE_CLOUD
                : ConnectEnvironmentType.CONNECT_TYPE_OB;
    }

    @JsonProperty(access = Access.READ_ONLY)
    public DialectType getDialectType() {
        return Objects.nonNull(this.type) ? this.type.getDialectType() : DialectType.UNKNOWN;
    }

    public String getDefaultSchema() {
        DialectType dialectType = getDialectType();
        if (dialectType == null) {
            return this.defaultSchema;
        }
        if (StringUtils.isNotBlank(this.defaultSchema)) {
            return ConnectionSessionUtil.getUserOrSchemaString(this.defaultSchema, dialectType);
        }
        switch (dialectType) {
            case ORACLE:
            case OB_ORACLE:
                return ConnectionSessionUtil.getUserOrSchemaString(this.username, dialectType);
            case MYSQL:
            case DORIS:
            case OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                return OdcConstants.MYSQL_DEFAULT_SCHEMA;
            default:
                return null;
        }
    }

    public void fillPasswordFromSavedIfNull(ConnectionConfig saved) {
        PreConditions.notNull(saved, "saved");
        if (Objects.isNull(this.password)) {
            setPassword(saved.getPassword());
        }
        if (Objects.isNull(this.sysTenantPassword)) {
            setSysTenantPassword(saved.getSysTenantPassword());
        }
        if (Objects.isNull(this.readonlyPassword)) {
            setReadonlyPassword(saved.getReadonlyPassword());
        }
    }

    public void fillEncryptedPasswordFromSavedIfNull(ConnectionConfig saved) {
        if (Boolean.FALSE.equals(this.getPasswordSaved())) {
            return;
        }
        PreConditions.notNull(saved, "saved");
        if (Objects.isNull(this.passwordEncrypted)) {
            setPasswordEncrypted(saved.getPasswordEncrypted());
        }
        if (Objects.isNull(this.sysTenantPasswordEncrypted)) {
            setSysTenantPasswordEncrypted(saved.getSysTenantPasswordEncrypted());
        }
        if (Objects.isNull(this.readonlyPasswordEncrypted)) {
            setReadonlyPasswordEncrypted(saved.getReadonlyPasswordEncrypted());
        }
    }

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        if (visibleScope == ConnectionVisibleScope.ORGANIZATION) {
            return ResourceType.ODC_CONNECTION.name();
        }
        return ResourceType.ODC_PRIVATE_CONNECTION.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    public int queryTimeoutSeconds() {
        if (this.queryTimeoutSeconds == null) {
            return OdcConstants.DEFAULT_QUERY_TIMEOUT_SECONDS;
        }
        return queryTimeoutSeconds;
    }

    @Size(max = 8192, message = "Session init script is out of range [0,8192]")
    public String getSessionInitScript() {
        if (this.attributes == null) {
            return null;
        }
        Object value = this.attributes.get(SESSION_INIT_SCRIPT_KEY);
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("all")
    public Map<String, Object> getJdbcUrlParameters() {
        if (this.attributes == null) {
            return new HashMap<>();
        }
        Object value = this.attributes.get(JDBC_URL_PARAMETERS_KEY);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    public void setSessionInitScript(
            @Size(max = 8192, message = "Session init script is out of range [0,8192]") String sessionInitScript) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(SESSION_INIT_SCRIPT_KEY, sessionInitScript);
    }

    public void setJdbcUrlParameters(Map<String, Object> jdbcUrlParameters) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(JDBC_URL_PARAMETERS_KEY, jdbcUrlParameters);
    }

    public ConnectionInfo toConnectionInfo() {
        ConnectionInfo target = new ConnectionInfo();
        target.setConnectType(type);
        target.setClusterName(clusterName);
        target.setHost(host);
        target.setPort(port);
        target.setUsername(username);
        target.setPassword(password);
        target.setOBTenant(OBTenantName);
        target.setTenantName(tenantName);
        if (Objects.nonNull(endpoint)) {
            target.setProxyHost(endpoint.getProxyHost());
            target.setProxyPort(endpoint.getProxyPort());
        }
        target.setSid(sid);
        target.setServiceName(serviceName);
        if (Objects.nonNull(userRole)) {
            target.setUserRole(userRole.name());
        }
        return target;
    }

    @Data
    public static class SSLConfig implements Serializable {
        /**
         * 是否开启 SSL 连接，为 false 时，下面的配置项无效，表示非 SSL 连接
         */
        private Boolean enabled = false;

        /**
         * 客户端证书 objectId
         */
        private String clientCertObjectId;

        /**
         * 客户端密钥 objectId
         */
        private String clientKeyObjectId;

        /**
         * CA 证书 objectId
         */
        @JsonProperty("CACertObjectId")
        private String CACertObjectId;
    }

    @Data
    @Builder
    public static class SSLFileEntry implements Serializable {
        private String keyStoreFilePath;

        private String keyStoreFilePassword;
    }
}
