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
package com.oceanbase.odc.migrate.jdbc.common;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "4.4.0.9", description = "upgrade distributed lock client id for ipv6 support")
public class V4409UpgradeDistributedLockClientIdForIpv6Migrate implements JdbcMigratable {

    private static final String TABLE_NAME = "DISTRIBUTED_LOCK";
    private static final String COLUMN_NAME = "CLIENT_ID";
    private static final int TARGET_LENGTH = 128;

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate txTemplate;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));


        try {
            if (!isTableExists()) {
                log.info("DISTRIBUTED_LOCK not exit ,skip migrate");
                return;
            }

            upgradeClientIdColumn();
        } catch (Exception e) {
            log.error("failed to update DISTRIBUTED_LOCK", e);
            throw new RuntimeException("IPv6升级失败", e);
        }
    }


    private boolean isTableExists() {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM " + TABLE_NAME + " LIMIT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.info("DISTRIBUTED_LOCK not exit ,skip migrate", e);
            return false;
        }
    }


    private void upgradeClientIdColumn() {
        txTemplate.execute((TransactionCallback<Void>) status -> {
            try {
                String alterSql =
                        "ALTER TABLE " + TABLE_NAME + " MODIFY COLUMN " + COLUMN_NAME + " CHAR(" + TARGET_LENGTH + ")";
                jdbcTemplate.execute(alterSql);
                return null;

            } catch (Exception e) {
                throw new RuntimeException("字段升级失败", e);
            }
        });
    }

}
