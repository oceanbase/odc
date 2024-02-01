/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.plugin.task.oracle.datatransfer;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.MySQLDataTransferJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.factory.BaseTransferJobFactory;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.factory.MySQLTransferJobFactory;
import com.zaxxer.hikari.HikariDataSource;

import lombok.NonNull;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/1
 */
public class OracleDataTransferJob extends MySQLDataTransferJob {

    public OracleDataTransferJob(@NonNull DataTransferConfig config, @NonNull File workingDir, @NonNull File logDir,
            @NonNull List<URL> inputs) {
        super(config, workingDir, logDir, inputs);
    }

    @Override
    protected BaseTransferJobFactory getJobFactory() {
        return new MySQLTransferJobFactory(baseConfig, workingDir, logDir, inputs);
    }

    @Override
    protected HikariDataSource getDataSource() {
        ConnectionInfo connectionInfo = baseConfig.getConnectionInfo();
        HikariDataSource dataSource = new HikariDataSource();
        // TODO: use OracleConnectionExtension
        // dataSource.setJdbcUrl(new OBOracleConnectionExtension().generateJdbcUrl(connectionInfo.getHost(),
        // connectionInfo.getPort(), connectionInfo.getSchema(), jdbcUrlParams));
        // dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUsername(connectionInfo.getUserNameForConnect());
        dataSource.setPassword(connectionInfo.getPassword());
        dataSource.setMaximumPoolSize(3);
        return dataSource;
    }

}
