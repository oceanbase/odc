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
package com.oceanbase.odc.service.session.initializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.OBInstanceRoleType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/4/12 16:10
 * @Description: []
 */
@Slf4j
public class BackupInstanceInitializer implements ConnectionInitializer {
    private final ConnectionConfig connectionConfig;

    public BackupInstanceInitializer(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    @Override
    public void init(Connection connection) throws SQLException {
        // 判断连接配置的方言类型是否为OceanBase，且实例角色类型是否为物理备份
        if (this.connectionConfig.getDialectType().isOceanbase()
                && this.connectionConfig.getInstanceRoleType() == OBInstanceRoleType.PHYSICAL_STANDBY) {
            long start = System.currentTimeMillis();
            // 获取OceanBase版本号
            String version = OBUtils.getObVersion(connection);
            // 如果版本号小于4.0.0
            if (VersionUtils.isLessThan(version, "4.0.0")) {
                try (Statement statement = connection.createStatement()) {
                    // 设置查询超时时间为3秒
                    statement.setQueryTimeout(3);
                    // 执行设置ob_read_consistency='WEAK'的SQL语句
                    statement.execute("set ob_read_consistency='WEAK';");
                    // 输出初始化完成的日志信息
                    log.info("Backup instance initializer init finished, cost={}ms",
                            System.currentTimeMillis() - start);
                } catch (SQLException e) {
                    // 输出设置ob_read_consistency='WEAK'失败的日志信息
                    log.warn("Failed to set ob_read_consistency='WEAK' for backup instance, error: {}",
                            e.getMessage());
                }
            }
        }
    }
}
