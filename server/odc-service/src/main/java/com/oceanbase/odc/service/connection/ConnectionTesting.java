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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.model.ConnectionPropertiesBuilder;
import com.oceanbase.odc.plugin.connect.model.JdbcUrlProperty;
import com.oceanbase.odc.service.connection.CloudMetadataClient.CloudPermissionAction;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.connection.model.OceanBaseAccessMode;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.connection.ssl.ConnectionSSLAdaptor;
import com.oceanbase.odc.service.connection.util.ConnectTypeUtil;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.odc.service.session.initializer.BackupInstanceInitializer;
import com.oceanbase.odc.service.session.initializer.DataSourceInitScriptInitializer;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Connection testing component, without metadb dependencies
 *
 * @author yizhou.xw
 * @version : ConnectionTesting.java, v 0.1 2021-07-30 18:06
 */
@Component
@Validated
@Slf4j
public class ConnectionTesting {

    @Autowired
    private ConnectProperties connectProperties;
    @Autowired
    private ConnectionAdapter environmentAdapter;
    @Autowired
    private ConnectionSSLAdaptor connectionSSLAdaptor;
    @Autowired
    private CloudMetadataClient cloudMetadataClient;
    @Value("${odc.sdk.test-connect.query-timeout-seconds:2}")
    private int queryTimeoutSeconds = 2;

    public ConnectionTestResult test(@NotNull @Valid TestConnectionReq req) {
        PreConditions.notNull(req, "req");
        environmentAdapter.adaptConfig(req);
        PreConditions.validArgumentState(Objects.nonNull(req.getPassword()),
                ErrorCodes.ConnectionPasswordMissed, null, "password required for connection without password saved");
        cloudMetadataClient.checkPermission(OBTenant.of(req.getClusterName(),
                req.getTenantName()), req.getInstanceType(), false, CloudPermissionAction.READONLY);
        connectionSSLAdaptor.adapt(req);

        PreConditions.validNotSqlInjection(req.getUsername(), "username");
        PreConditions.validNotSqlInjection(req.getClusterName(), "clusterName");
        PreConditions.validNotSqlInjection(req.getTenantName(), "tenantName");

        PreConditions.validInHostWhiteList(req.getHost(), connectProperties.getHostWhiteList());

        ConnectionConfig connectionConfig = reqToConnectionConfig(req);
        if (req.getAccountType() == ConnectionAccountType.SYS_READ) {
            connectionConfig.setDefaultSchema(null);
        }
        return test(connectionConfig);
    }

    public ConnectionTestResult test(@NonNull ConnectionConfig config) {
        ConnectType type = config.getType();
        try {
            /**
             * 进行连接测试时需要关注的值有一个 {@link ConnectType}， 容易产生问题信息主要是两个：{@code username}, {@code defaultSchema} 首先分析
             * {@link ConnectType} 可能的取值及相对应的场景：
             *
             * <pre>
             *     1. {@link ConnectType} 为 null 的情况。
             *        a. 用户将 obclient 串拷贝到输入框中，点击"解析"。
             *     2. {@link ConnectType} 不为空的情况：此时该信息可能是不准的。
             *        a. 用户在连接配置表单上输入信息，然后点击"测试连接"。
             *        b. 直接从数据库中查询到 {@link ConnectionConfig}。
             * </pre>
             *
             * 在以上两种情况下分别分析 {@code username} 和 {@code defaultSchema} 的设置：
             *
             * <pre>
             *     1. 用户的 username：
             *        a. 第一种场景下使用 {@link OBConsoleDataSourceFactory#getUsername(ConnectionConfig)}
             *           获取的用户名就是 obclient 串中 {@code -u} 中填入的内容，这是符合测试语义的。
             *        b. 第二种场景下由于 {@link ConnectType} 不准，如果信任该值可能导致 {@code username} 格式错误，因此需要将其设置为
             *           null，然后使用 {@link OBConsoleDataSourceFactory#getUsername(ConnectionConfig)}
             *           此时获取到的内容是 {@link ConnectionConfig} 中 {@code user@tenant#cluster}，这是符合测试语义的。
             *     2. 要连接到的目标 schema：
             *        a. 第一种场景下使用 {@link OBConsoleDataSourceFactory#getDefaultSchema(ConnectionConfig)}
             *           获取到的就是 obclient 串中 {@code -D} 中的内容，这同样是符合测试语义的。
             *        b. 第二种场景下需要分情况讨论：
             *           1). 如果 {@link ConnectType#getDialectType()} 为 {@link DialectType#OB_MYSQL}，此时 schema 应该使用
             *               {@link OBConsoleDataSourceFactory#getDefaultSchema(ConnectionConfig)}
             *               获取，这是因为无论连接是否真的是 mysql 模式都不会引发更多的问题。
             *           2). 如果 {@link ConnectType#getDialectType()} 为 {@link DialectType#OB_ORACLE}，此时 schema 应该设置为 null，
             *               这么做是因为，如果连接是 mysql 模式却被错误设置为 oracle 模式，此时 schema 信息的值将会被设置成 username，如果
             *               以这个schema 去尝试连接，那么将会收到一个 unknown database 而将真实的租户选择错误的信息隐藏掉。
             * </pre>
             *
             * 总的来说，对于 username 而言，我们调用 {@link OBConsoleDataSourceFactory#getUsername(ConnectionConfig)}
             * 前需要将{@link ConnectType} 设置为 {@code null}，对于 defaultSchema 而言需要分情况讨论。
             */
            String schema;
            if (type == null) {
                schema = OBConsoleDataSourceFactory.getDefaultSchema(config);
            } else if (type.getDialectType().isOracle()) {
                schema = null;
            } else if (type.getDialectType().isMysql()) {
                schema = OBConsoleDataSourceFactory.getDefaultSchema(config);
            } else if (type.getDialectType().isDoris()) {
                schema = OBConsoleDataSourceFactory.getDefaultSchema(config);
            } else {
                throw new UnsupportedOperationException("Unsupported type, " + type);
            }

            ConnectionExtensionPoint connectionExtensionPoint = ConnectionPluginUtil.getConnectionExtension(
                    (type != null) ? type.getDialectType() : DialectType.OB_MYSQL);

            JdbcUrlProperty jdbcUrlProperties = getJdbcUrlProperties(config, schema);
            Properties testConnectionProperties = getTestConnectionProperties(config);

            TestResult result = connectionExtensionPoint.test(
                    connectionExtensionPoint.generateJdbcUrl(jdbcUrlProperties),
                    testConnectionProperties, queryTimeoutSeconds);
            log.info("Test connection completed, result: {}", result);
            if (result.getErrorCode() != null) {
                if (type != null && !type.isCloud()

                        && StringUtils.endsWithAny(config.getHost(), ConnectTypeUtil.CLOUD_SUFFIX)) {
                    return ConnectionTestResult.connectTypeMismatch();
                }
                if (result.getErrorCode() == ErrorCodes.ObAccessDenied && type == ConnectType.OB_MYSQL) {
                    return ConnectionTestResult.fail(ErrorCodes.ObMysqlAccessDenied,
                            new String[] {schema, result.getArgs()[0]});
                }
                return new ConnectionTestResult(result, null);
            }
            ConnectType connectType = ConnectTypeUtil.getConnectType(
                    connectionExtensionPoint.generateJdbcUrl(jdbcUrlProperties),
                    testConnectionProperties, queryTimeoutSeconds);
            ConnectionTestResult testResult = new ConnectionTestResult(result, connectType);
            if (type != null && connectType != null && !Objects.equals(connectType, type)) {
                return ConnectionTestResult.connectTypeMismatch(connectType);
            }
            try {
                testInitScript(connectionExtensionPoint, schema, config);
            } catch (Exception e) {
                return ConnectionTestResult.initScriptFailed(e);
            }
            return testResult;
        } catch (Exception e) {
            return new ConnectionTestResult(TestResult.unknownError(e), null);
        } finally {
            config.setType(type);
        }
    }

    private JdbcUrlProperty getJdbcUrlProperties(ConnectionConfig config, String schema) {
        return new JdbcUrlProperty(config.getHost(), config.getPort(), schema,
                OBConsoleDataSourceFactory.getJdbcParams(config), config.getSid(),
                config.getServiceName());
    }

    private Properties getTestConnectionProperties(ConnectionConfig config) {
        return ConnectionPropertiesBuilder.getBuilder().user(OBConsoleDataSourceFactory.getUsername(config))
                .password(OBConsoleDataSourceFactory.getPassword(config)).userRole(config.getUserRole())
                .build();
    }

    private ConnectionConfig reqToConnectionConfig(TestConnectionReq req) {
        ConnectionConfig config = new ConnectionConfig();
        if (StringUtils.equals("sys", req.getTenantName())) {
            config.setType(ConnectType.OB_MYSQL);
        } else {
            config.setType(req.getType());
        }
        config.setHost(req.getHost());
        config.setPort(req.getPort());
        config.setClusterName(req.getClusterName());
        config.setTenantName(req.getTenantName());
        config.setUsername(req.getUsername());
        config.setPassword(req.getPassword());
        config.setDefaultSchema(req.getDefaultSchema());
        config.setSessionInitScript(req.getSessionInitScript());
        config.setJdbcUrlParameters(req.getJdbcUrlParameters());
        config.setSid(req.getSid());
        config.setServiceName(req.getServiceName());
        config.setUserRole(req.getUserRole());

        OBTenantEndpoint endpoint = req.getEndpoint();
        if (Objects.nonNull(endpoint) && OceanBaseAccessMode.IC_PROXY == endpoint.getAccessMode()) {
            config.setEndpoint(endpoint);
        }
        if (StringUtils.isNotBlank(req.getOBTenantName())) {
            config.setTenantName(req.getOBTenantName());
        }
        config.setSslFileEntry(req.getSslFileEntry());
        config.setSslConfig(req.getSslConfig());
        return config;
    }

    private void testInitScript(ConnectionExtensionPoint extensionPoint,
            String schema, ConnectionConfig config) throws SQLException {
        String jdbcUrl =
                extensionPoint.generateJdbcUrl(getJdbcUrlProperties(config, schema));

        Properties properties = getTestConnectionProperties(config);
        properties.setProperty("socketTimeout", ConnectTypeUtil.REACHABLE_TIMEOUT_MILLIS + "");
        properties.setProperty("connectTimeout", ConnectTypeUtil.REACHABLE_TIMEOUT_MILLIS + "");

        List<ConnectionInitializer> initializers = new ArrayList<>();
        initializers.addAll(
                Arrays.asList(new BackupInstanceInitializer(config),
                        new DataSourceInitScriptInitializer(config, false)));
        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties);
                Statement statement = connection.createStatement()) {
            if (queryTimeoutSeconds >= 0) {
                statement.setQueryTimeout(queryTimeoutSeconds);
            }
            for (ConnectionInitializer initializer : initializers) {
                initializer.init(connection);
            }
        }
    }

}
