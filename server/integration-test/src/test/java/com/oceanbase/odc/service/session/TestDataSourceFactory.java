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
package com.oceanbase.odc.service.session;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

import lombok.NonNull;
import lombok.Setter;

public class TestDataSourceFactory implements CloneableDataSourceFactory {

    private String username;
    private String password;
    private String host;
    private Integer port;
    private String defaultSchema;
    @Setter
    private boolean autoCommit = true;
    private final DialectType dialectType;

    public TestDataSourceFactory(DialectType dialectType) {
        this.username = getUsername(dialectType);
        this.password = getConfiguration(dialectType).getPassword();
        this.host = getConfiguration(dialectType).getHost();
        this.port = getConfiguration(dialectType).getPort();
        this.defaultSchema = getConfiguration(dialectType).getDefaultDBName();
        this.dialectType = dialectType;
    }

    @Override
    public DataSource getDataSource() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl(getUrl());
        dataSource.setUsername(this.username);
        dataSource.setPassword(this.password);
        dataSource.setAutoCommit(autoCommit);
        return dataSource;
    }

    @Override
    public CloneableDataSourceFactory deepCopy() {
        return new TestDataSourceFactory(dialectType);
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
        // ignore
    }

    private String getUrl() {
        return ConnectionPluginUtil.getConnectionExtension(dialectType)
                .generateJdbcUrl(this.host, this.port, this.defaultSchema, null);
    }

    private static String getUsername(DialectType dialectType) {
        TestDBConfiguration configuration = getConfiguration(dialectType);
        StringBuilder stringBuilder = new StringBuilder(configuration.getUsername());
        stringBuilder.append("@").append(configuration.getTenant());
        if (StringUtils.isNotBlank(configuration.getCluster())) {
            stringBuilder.append("#").append(configuration.getCluster());
        }
        return stringBuilder.toString();
    }

    private static TestDBConfiguration getConfiguration(DialectType dialectType) {
        TestDBConfigurations configurations = TestDBConfigurations.getInstance();
        if (dialectType.isMysql()) {
            return configurations.getTestOBMysqlConfiguration();
        }
        return configurations.getTestOBOracleConfiguration();
    }

}
