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
package com.oceanbase.odc.service.session.factory;

import static com.oceanbase.odc.core.shared.constant.OdcConstants.DEFAULT_ZERO_DATE_TIME_BEHAVIOR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang.Validate;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.ConnectionConstants;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLConfig;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLFileEntry;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.connection.model.UserRole;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.initializer.SessionCreatedInitializer;

import lombok.NonNull;
import lombok.Setter;

/**
 * Long connection connection pool factory class, used to construct a long connection connection
 * pool according to the {@link com.oceanbase.odc.service.connection.model.ConnectionConfig}
 *
 * @author yh263208
 * @date 2021-11-16 17:29
 * @since ODC_release_3.2.2
 * @see DataSourceFactory
 */
public class OBConsoleDataSourceFactory implements CloneableDataSourceFactory {

    private String username;
    private String password;
    private String host;
    private Integer port;
    private String defaultSchema;
    private String sid;
    private String serviceName;
    private UserRole userRole;
    private Map<String, String> parameters;
    protected final ConnectionConfig connectionConfig;
    protected final ConnectionAccountType accountType;
    private final Boolean autoCommit;
    private final boolean initConnection;
    @Setter
    private EventPublisher eventPublisher;
    protected final ConnectionExtensionPoint connectionExtensionPoint;

    public OBConsoleDataSourceFactory(@NonNull ConnectionConfig connectionConfig,
            @NonNull ConnectionAccountType accountType, Boolean autoCommit) {
        this(connectionConfig, accountType, autoCommit, true);
    }

    public OBConsoleDataSourceFactory(@NonNull ConnectionConfig connectionConfig,
            @NonNull ConnectionAccountType accountType, Boolean autoCommit, boolean initConnection) {
        this.accountType = accountType;
        this.autoCommit = autoCommit;
        this.connectionConfig = connectionConfig;
        this.initConnection = initConnection;
        this.username = getUsername(connectionConfig, accountType);
        this.password = getPassword(connectionConfig, accountType);
        this.host = connectionConfig.getHost();
        this.port = connectionConfig.getPort();
        this.defaultSchema = getDefaultSchema(connectionConfig, accountType);
        this.sid = connectionConfig.getSid();
        this.serviceName = connectionConfig.getServiceName();
        this.userRole = connectionConfig.getUserRole();
        this.parameters = getJdbcParams(connectionConfig);
        this.connectionExtensionPoint = ConnectionPluginUtil.getConnectionExtension(connectionConfig.getDialectType());
    }

    protected String getJdbcUrl() {
        return connectionExtensionPoint.generateJdbcUrl(getJdbcUrlProperties(), this.parameters);
    }

    private Properties getJdbcUrlProperties() {
        Properties properties = new Properties();
        if (Objects.nonNull(this.host)) {
            properties.put(ConnectionConstants.HOST, this.host);
        }
        if (Objects.nonNull(this.port)) {
            properties.put(ConnectionConstants.PORT, this.port);
        }
        if (Objects.nonNull(this.defaultSchema)) {
            properties.put(ConnectionConstants.DEFAULT_SCHEMA, this.defaultSchema);
        }
        if (Objects.nonNull(this.sid)) {
            properties.put(ConnectionConstants.SID, this.sid);
        }
        if (Objects.nonNull(this.serviceName)) {
            properties.put(ConnectionConstants.SERVICE_NAME, this.serviceName);
        }
        return properties;
    }

    public static String getUsername(@NonNull ConnectionConfig connectionConfig,
            @NonNull ConnectionAccountType accountType) {
        String username = getDbUser(connectionConfig, accountType);
        if (DialectType.OB_ORACLE.equals(connectionConfig.getDialectType())) {
            username = "\"" + username + "\"";
        }
        if (StringUtils.isNotBlank(connectionConfig.getOBTenantName())) {
            username = username + "@" + connectionConfig.getOBTenantName();
        } else if (StringUtils.isNotBlank(connectionConfig.getTenantName())) {
            username = username + "@" + connectionConfig.getTenantName();
        }
        if (StringUtils.isNotBlank(connectionConfig.getClusterName())) {
            username = username + "#" + connectionConfig.getClusterName();
        }
        return username;
    }

    public static String getPassword(@NonNull ConnectionConfig connectionConfig,
            @NonNull ConnectionAccountType accountType) {
        String password;
        switch (accountType) {
            case MAIN:
                password = connectionConfig.getPassword();
                break;
            case READONLY:
                password = connectionConfig.getReadonlyPassword();
                break;
            default:
                throw new IllegalArgumentException("Invalid accountType, accountType=" + accountType);
        }
        return password;
    }

    public static Map<String, String> getJdbcParams(@NonNull ConnectionConfig connectionConfig) {
        Map<String, String> jdbcUrlParams = new HashMap<>();
        jdbcUrlParams.put("maxAllowedPacket", "64000000");
        jdbcUrlParams.put("allowMultiQueries", "true");
        jdbcUrlParams.put("connectTimeout", "5000");
        jdbcUrlParams.put("zeroDateTimeBehavior", DEFAULT_ZERO_DATE_TIME_BEHAVIOR);
        jdbcUrlParams.put("noDatetimeStringSync", "true");
        jdbcUrlParams.put("allowLoadLocalInfile", "false");
        jdbcUrlParams.put("jdbcCompliantTruncation", "false");

        // TODO: set sendConnectionAttributes while upgrade oceanbase-client to v2.2.10
        jdbcUrlParams.put("sendConnectionAttributes", "false");

        OBTenantEndpoint endpoint = connectionConfig.getEndpoint();
        if (Objects.nonNull(endpoint)) {
            String proxyHost = endpoint.getProxyHost();
            Integer proxyPort = endpoint.getProxyPort();
            if (StringUtils.isNotBlank(proxyHost) && Objects.nonNull(proxyPort)) {
                jdbcUrlParams.put("socksProxyHost", proxyHost);
                jdbcUrlParams.put("socksProxyPort", proxyPort + "");
            }
        }
        SSLConfig sslConfig = connectionConfig.getSslConfig();
        if (sslConfig != null && sslConfig.getEnabled() != null && sslConfig.getEnabled()) {
            jdbcUrlParams.put("useSSL", "true");
            jdbcUrlParams.put("trustServerCertificate", "true");
            SSLFileEntry sslFileEntry = connectionConfig.getSslFileEntry();
            if (Objects.nonNull(sslFileEntry)) {
                if (StringUtils.isNotBlank(sslFileEntry.getKeyStoreFilePath())) {
                    jdbcUrlParams.put("disableSslHostnameVerification", "true");
                    jdbcUrlParams.put("trustStore", sslFileEntry.getKeyStoreFilePath());
                    jdbcUrlParams.put("trustStorePassword", sslFileEntry.getKeyStoreFilePassword());
                    jdbcUrlParams.put("keyStore", sslFileEntry.getKeyStoreFilePath());
                    jdbcUrlParams.put("keyStorePassword", sslFileEntry.getKeyStoreFilePassword());
                    jdbcUrlParams.put("trustServerCertificate", "false");
                }
            }
        } else {
            jdbcUrlParams.put("useSSL", "false");
        }
        connectionConfig.getJdbcUrlParameters().forEach((key, value) -> {
            if (value != null) {
                jdbcUrlParams.put(key, value.toString());
            }
        });
        return jdbcUrlParams;
    }

    @Override
    public DataSource getDataSource() {
        String jdbcUrl = getJdbcUrl();
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(true);
        dataSource.setEventPublisher(eventPublisher);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        if (Objects.nonNull(this.userRole)) {
            Properties properties = new Properties();
            properties.put(ConnectionConstants.USER_ROLE, this.userRole.name());
            dataSource.setConnectionProperties(properties);
        }
        // Set datasource driver class
        dataSource.setDriverClassName(connectionExtensionPoint.getDriverClassName());
        if (autoCommit != null) {
            dataSource.setAutoCommit(autoCommit);
        }
        if (this.initConnection) {
            dataSource.addInitializer(new SessionCreatedInitializer(connectionConfig));
            List<ConnectionInitializer> initializers = connectionExtensionPoint.getConnectionInitializers();
            if (!CollectionUtils.isEmpty(initializers)) {
                initializers.forEach(dataSource::addInitializer);
            }
        }
        return dataSource;
    }

    @Override
    public CloneableDataSourceFactory deepCopy() {
        ConnectionMapper mapper = ConnectionMapper.INSTANCE;
        return new OBConsoleDataSourceFactory(mapper.clone(connectionConfig), this.accountType, this.autoCommit);
    }

    @Override
    public void resetUsername(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        this.username = mapper.map(this.username);
    }

    @Override
    public void resetPassword(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        this.password = mapper.map(this.password);
    }

    @Override
    public void resetHost(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        this.host = mapper.map(this.host);
    }

    @Override
    public void resetPort(@NonNull CloneableDataSourceFactory.ValueMapper<Integer> mapper) {
        Integer newPort = mapper.map(this.port);
        Validate.isTrue(newPort > 0, "Port can not be negative");
        this.port = newPort;
    }

    @Override
    public void resetSchema(@NonNull CloneableDataSourceFactory.ValueMapper<String> mapper) {
        this.defaultSchema = mapper.map(this.defaultSchema);
    }

    @Override
    public void resetParameters(@NonNull CloneableDataSourceFactory.ValueMapper<Map<String, String>> mapper) {
        this.parameters = mapper.map(this.parameters);
    }

    protected String getUsername() {
        return this.username;
    }

    protected String getPassword() {
        return this.password;
    }

    protected ConnectType getConnectType() {
        return this.connectionConfig.getType();
    }

    private static String getDbUser(@NonNull ConnectionConfig connectionConfig,
            @NonNull ConnectionAccountType accountType) {
        String username;
        switch (accountType) {
            case MAIN:
                username = connectionConfig.getUsername();
                break;
            case READONLY:
                username = connectionConfig.getReadonlyUsername();
                break;
            default:
                throw new IllegalArgumentException("Invalid accountType, accountType=" + accountType);
        }
        if (DialectType.OB_ORACLE.equals(connectionConfig.getDialectType())) {
            username = ConnectionSessionUtil.getUserOrSchemaString(username, DialectType.OB_ORACLE);
        }
        return username;
    }

    public static String getDefaultSchema(@NonNull ConnectionConfig connectionConfig,
            @NonNull ConnectionAccountType accountType) {
        String dbUser = getDbUser(connectionConfig, accountType);
        String defaultSchema = connectionConfig.defaultSchema();
        if (DialectType.OB_ORACLE.equals(connectionConfig.getDialectType())) {
            defaultSchema = "\"" + dbUser + "\"";
        } else if (StringUtils.isBlank(defaultSchema)
                && connectionConfig.getDialectType().isMysql()) {
            defaultSchema = OdcConstants.MYSQL_DEFAULT_SCHEMA;
        }
        return defaultSchema;
    }

}
