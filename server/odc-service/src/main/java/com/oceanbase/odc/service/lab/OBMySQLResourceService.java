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
package com.oceanbase.odc.service.lab;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.util.ConnectTypeUtil;
import com.oceanbase.odc.service.connection.util.DefaultJdbcUrlParser;
import com.oceanbase.odc.service.connection.util.JdbcUrlParser;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.lab.LabDataSourceFactory.DataSourceContext;
import com.oceanbase.odc.service.lab.model.LabProperties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/12/16 下午3:39
 * @Description: [This class is responsible for creating and revoking trial lab ob resources, like
 *               db user, schema, etc.]
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class OBMySQLResourceService {

    // self define variable prefix/suffix for avoid conflict with spring @Value annotation value inject
    private final String VARIABLE_PREFIX = "{{";
    private final String VARIABLE_SUFFIX = "}}";
    private final String VARIABLE_DB_NAME = "dbName";
    private final String VARIABLE_DB_USERNAME = "dbUsername";
    private final String VARIABLE_PASSWORD = "password";

    @Autowired
    private LabDataSourceFactory dataSourceFactory;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private LabProperties labProperties;

    @Value("${odc.sdk.test-connect.query-timeout-seconds:2}")
    private int queryTimeoutSeconds = 2;

    public DbResourceInfo createObResource(String dbUsername, String password, String dbName, Long userId)
            throws SQLException {
        DataSourceContext dataSourceContext =
                dataSourceFactory.getDataSourceContext(userId);
        try (Connection connection = dataSourceContext.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                log.info("already get connection, dbUsername={}, dbName={}", dbUsername, dbName);
                DbResourceInfo resourceInfo = new DbResourceInfo(dbUsername, password, dbName,
                        dataSourceContext.getConnectionProperty().getHost());
                resourceInfo.setClusterName(dataSourceContext.getClusterName());
                resourceInfo.setTenantName(dataSourceContext.getTenantName());

                String template = labProperties.getInitMysqlResourceInitScriptTemplate();
                String script = generateScript(template, resourceInfo);
                statement.execute(script);

                connection.commit();
                resourceInfo.setConnectType(getConnectType(
                        new DefaultJdbcUrlParser(dataSourceContext.getConnectionProperty().getJdbcUrl()), statement));
                log.info("Created Ob resource, username={}, dbName={}", dbUsername, dbName);
                return resourceInfo;
            } catch (SQLException ex) {
                log.warn("createObResourceSQLException, errorCode={}, sqlState={}, message={}", ex.getErrorCode(),
                        ex.getSQLState(), ex.getMessage());
                connection.rollback();
                log.info("rollback succeed, connection={}", connection);
                throw new UnexpectedException("Create OB resource failed, but rollback succeed");
            }
        }
    }

    public void revokeObResource(String dbUsername, String dbName, Long userId) {
        DataSourceContext dataSourceContext =
                dataSourceFactory.getDataSourceContext(userId);
        try (Connection connection = dataSourceContext.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                log.info("start revoke ob resource, username={}, dbName={}", dbUsername, dbName);
                String template = labProperties.getInitMysqlResourceRevokeScriptTemplate();
                DbResourceInfo resourceInfo = new DbResourceInfo(dbUsername, "dummy_user", dbName, null);
                String script = generateScript(template, resourceInfo);
                statement.execute(script);
                connection.commit();
                log.info("revoke ob resource successfully, username={}, dbName={}", dbUsername, dbName);
            } catch (SQLException ex) {
                log.warn("revokeObResourceSQLException, errorCode={}, sqlState={}, message={}", ex.getErrorCode(),
                        ex.getSQLState(), ex.getMessage());
                connection.rollback();
                log.info("rollback succeed, connection={}", connection);
            }
        } catch (Exception ex) {
            log.warn("revoke ob resource failed, dbUsername={}, dbName={}", dbUsername, dbName, ex);
        }
    }

    String generateScript(String template, DbResourceInfo resourceInfo) {
        PreConditions.notBlank(template, "template");
        Map<String, String> variables = new HashMap<>();
        variables.put(VARIABLE_DB_NAME, StringUtils.quoteMysqlIdentifier(resourceInfo.getDbName()));
        variables.put(VARIABLE_DB_USERNAME, StringUtils.quoteMysqlIdentifier(resourceInfo.getDbUsername()));
        variables.put(VARIABLE_PASSWORD, StringUtils.quoteMysqlValue(resourceInfo.getPassword()));
        StringSubstitutor sub = new StringSubstitutor(variables, VARIABLE_PREFIX, VARIABLE_SUFFIX)
                .setDisableSubstitutionInValues(true);
        return sub.replace(template);
    }

    private static ConnectType getConnectType(JdbcUrlParser urlParser, Statement statement) throws SQLException {
        DialectType dialectType = ConnectionSessionUtil.getDialectType(statement);
        if (dialectType == null) {
            // {@code dialectType} 为 null，可能是原生的 mysql 或 oracle，默认返回 MYSQL
            return ConnectType.OB_MYSQL;
        }
        if (ConnectTypeUtil.isCloud(urlParser)) {
            // 公有云模式
            switch (dialectType) {
                case OB_ORACLE:
                    return ConnectType.CLOUD_OB_ORACLE;
                case OB_MYSQL:
                    return ConnectType.CLOUD_OB_MYSQL;
                default:
                    throw new UnsupportedOperationException("Unsupport dialect type, " + dialectType);
            }
        }
        return ConnectType.from(dialectType);
    }

    @Data
    class DbResourceInfo {
        String dbUsername;
        String password;
        String dbName;
        String host;
        /**
         * valid while use local tenant, ignore while use cloud tenant
         */
        String tenantName;
        /**
         * valid while use local tenant, ignore while use cloud tenant
         */
        String clusterName;
        ConnectType connectType;

        public DbResourceInfo(String dbUsername, String password, String dbName, String host) {
            this.dbUsername = dbUsername;
            this.password = password;
            this.dbName = dbName;
            this.host = host;
        }
    }
}
