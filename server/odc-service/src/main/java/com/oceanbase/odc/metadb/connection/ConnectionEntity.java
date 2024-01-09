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
package com.oceanbase.odc.metadb.connection;

import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.jpa.JsonMapConverter;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.UserRole;

import lombok.Data;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = {"passwordEncrypted", "sysTenantPasswordEncrypted", "readonlyPasswordEncrypted", "salt"})
@Table(name = "connect_connection")
public class ConnectionEntity {

    /**
     * Id for database connection
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Date createTime;

    /**
     * Record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date updateTime;

    /**
     * Visible scope, enum: PRIVATE, ORGANIZATION
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "visible_scope", updatable = false)
    private ConnectionVisibleScope visibleScope;

    /**
     * Owner id, user_id if visible_scope=PRIVATE, enterprise_id if visible_scope=ORGANIZATION
     */
    @Column(name = "owner_id")
    private Long ownerId;

    /**
     * Name
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Organization id
     */
    @Column(name = "organization_id", updatable = false, nullable = false)
    private Long organizationId;

    /**
     * Creator id, references odc_user_info(id)
     */
    @Column(name = "creator_id", updatable = false, nullable = false)
    private Long creatorId;

    /**
     * Last modifier id, references iam_user(id)
     */
    @Column(name = "last_modifier_id", nullable = false)
    private Long lastModifierId;

    /**
     * Connect type, <br>
     * enumeration values: OB_MYSQL, OB_ORACLE, CLOUD_OB_MYSQL, CLOUD_OB_ORACLE, ODP_SHARDING_OB_MYSQL
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConnectType type;

    /**
     * Dialect type, enumeration values: OB_MYSQL,OB_ORACLE,ORACLE,MYSQL
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "dialect_type", nullable = false)
    private DialectType dialectType;

    /**
     * Database connection address
     */
    @Column(name = "host", nullable = false)
    private String host;

    /**
     * Database connection port
     */
    @Column(name = "port")
    private Integer port;

    /**
     * Cluster name of OceanBase
     */
    @Column(name = "cluster_name")
    private String clusterName;

    /**
     * Tenant name of OceanBase
     */
    @Column(name = "tenant_name")
    private String tenantName;

    /**
     * Database username
     */
    @Column(name = "username", nullable = false)
    private String username;

    /**
     * Database password
     */
    @Column(name = "password")
    private String passwordEncrypted;

    /**
     * Schema name of the default connection
     */
    @Column(name = "default_schema")
    private String defaultSchema;

    /**
     * Username under the sys tenant
     */
    @Column(name = "sys_tenant_username")
    private String sysTenantUsername;

    /**
     * Password of the user under the sys tenant
     */
    @Column(name = "sys_tenant_password")
    private String sysTenantPasswordEncrypted;

    /**
     * Username of read only account for readonly db session
     */
    @Column(name = "readonly_username")
    private String readonlyUsername;

    /**
     * Password of read only account for readonly db session
     */
    @Column(name = "readonly_password")
    private String readonlyPasswordEncrypted;

    /**
     * [Deprecated], for OCJ connection type
     */
    @Column(name = "config_url")
    private String configUrl;

    /**
     * Query timeout
     */
    @Column(name = "query_timeout_seconds")
    private Integer queryTimeoutSeconds;

    /**
     * Extension field, no specific purpose
     */
    @Column(name = "properties_json")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, String> properties;

    /**
     * Enabled or not
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    /**
     * Password saved or not
     */
    @Column(name = "is_password_saved", nullable = false)
    private Boolean passwordSaved;

    /**
     * The algorithm used for encryption and decryption of the connection password field, optional value
     * RAW/AES256SALT
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "cipher", nullable = false)
    private Cipher cipher;

    /**
     * Used to connect the random value used by the encryption and decryption algorithm of the password
     * field
     */
    @Column(name = "salt", nullable = false)
    private String salt;

    /**
     * Temp or not
     */
    @Column(name = "is_temp", nullable = false)
    private Boolean temp;

    /**
     * 是否开启 SSL 连接
     */
    @Column(name = "is_ssl_enabled", nullable = false)
    private Boolean sslEnabled;

    /**
     * SSL 客户端证书 objectId
     */
    @Column(name = "ssl_client_cert_object_id")
    private String sslClientCertObjectId;

    /**
     * SSL 客户端密钥 objectId
     */
    @Column(name = "ssl_client_key_object_id")
    private String sslClientKeyObjectId;

    /**
     * SSL CA 证书 objectId
     */
    @Column(name = "ssl_ca_cert_object_id")
    private String sslCACertObjectId;

    @Column(name = "environment_id", nullable = false)
    private Long environmentId;

    @Column(name = "project_id")
    private Long projectId;

    /**
     * Oracle 连接方式特有的参数，该参数表示一个数据库
     */
    @Column(name = "sid")
    private String sid;

    /**
     * Oracle 连接方式特有的参数，该参数表示数据库的一个实例
     */
    @Column(name = "service_name")
    private String serviceName;

    /**
     * Oracle 连接方式特有的参数，该参数用户角色
     */
    @Column(name = "user_role")
    private UserRole userRole;
}
