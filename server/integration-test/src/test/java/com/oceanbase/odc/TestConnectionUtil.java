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
package com.oceanbase.odc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/3/2 10:32
 */
@Slf4j
public class TestConnectionUtil extends PluginTestEnv {
    private static final Long CONNECTION_ID_OB_MYSQL = 1001L;
    private static final Long CONNECTION_ID_OB_ORACLE = 1002L;
    private static final Long CONNECTION_ID_MYSQL = 1003L;
    private static final Long CONNECTION_ID_ORACLE = 1004L;
    private static final int QUERY_TIMEOUT_SECONDS = 60;
    private static final Map<ConnectType, ConnectionSession> connectionSessionMap = new HashMap<>();

    static {
        Thread shutdownHookThread = new Thread(TestConnectionUtil::expireConnectionSession);
        shutdownHookThread.setDaemon(true);
        shutdownHookThread.setName("thread-odc-unit-test-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }

    public static ConnectionSession getTestConnectionSession(ConnectType type) {
        if (!connectionSessionMap.containsKey(type)) {
            ConnectionConfig config = getTestConnectionConfig(type);
            ConnectionSession session = new DefaultConnectSessionFactory(config).generateSession();
            connectionSessionMap.put(type, session);
        }
        return connectionSessionMap.get(type);
    }

    public static ConnectionConfig getTestConnectionConfig(ConnectType type) {
        ConnectionConfig connection = new ConnectionConfig();
        TestDBConfiguration configuration;
        if (type == ConnectType.OB_MYSQL) {
            connection.setId(CONNECTION_ID_OB_MYSQL);
            configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        } else if (type == ConnectType.OB_ORACLE) {
            connection.setId(CONNECTION_ID_OB_ORACLE);
            configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        } else if (type == ConnectType.MYSQL) {
            connection.setId(CONNECTION_ID_MYSQL);
            configuration = TestDBConfigurations.getInstance().getTestMysqlConfiguration();
        } else if (type == ConnectType.ORACLE) {
            connection.setId(CONNECTION_ID_ORACLE);
            configuration = TestDBConfigurations.getInstance().getTestOracleConfiguration();
        } else {
            throw new UnsupportedOperationException(String.format("Test DB for %s mode is not supported yet", type));
        }
        connection.setName("test");
        connection.setEnabled(true);
        connection.setCreateTime(null);
        connection.setUpdateTime(null);
        connection.setPasswordSaved(true);
        connection.setType(type);
        connection.setHost(configuration.getHost());
        connection.setPort(configuration.getPort());
        connection.setClusterName(configuration.getCluster());
        connection.setTenantName(configuration.getTenant());
        connection.setUsername(configuration.getUsername());
        connection.setPassword(configuration.getPassword());
        connection.setReadonlyUsername(configuration.getUsername());
        connection.setReadonlyPassword(configuration.getPassword());
        connection.setSysTenantUsername(configuration.getSysUsername());
        connection.setSysTenantPassword(configuration.getSysPassword());
        connection.setDefaultSchema(configuration.getDefaultDBName());
        connection.setVisibleScope(ConnectionVisibleScope.PRIVATE);
        connection.setQueryTimeoutSeconds(QUERY_TIMEOUT_SECONDS);
        connection.setSid(configuration.getSID());
        connection.setServiceName(configuration.getServiceName());
        return connection;
    }

    private static void expireConnectionSession() {
        connectionSessionMap.forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                v.expire();
            }
        });
    }
}
