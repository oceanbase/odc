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
package com.oceanbase.odc.migrate.jdbc.web;

import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "4.1.3.1", description = "initial admin password")
public class V4131InitialPasswordMigrate implements JdbcMigratable {
    private static final String ENV_INITIAL_PASSWORD = "ODC_ADMIN_INITIAL_PASSWORD";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String sql = "SELECT is_active FROM iam_user "
                + " WHERE organization_id=1 AND account_name='admin'";
        List<Boolean> results = jdbcTemplate.queryForList(sql, Boolean.class);
        if (CollectionUtils.isEmpty(results)) {
            log.info("No 'admin' account found, skip initial password");
            return;
        }
        Verify.equals(1, results.size(), "admin account count");
        Boolean isAdminActive = results.get(0);
        if (Boolean.TRUE.equals(isAdminActive)) {
            log.info("Account 'admin' was active already, skip initial password");
            return;
        }
        String initialPassword = getInitialPassword();
        if (StringUtils.isBlank(initialPassword)) {
            log.info("Environment variable 'ODC_ADMIN_INITIAL_PASSWORD' not set, skip initial password");
            return;
        }
        PreConditions.validPassword(initialPassword);
        String encodedPassword = PASSWORD_ENCODER.encode(initialPassword);

        TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        txTemplate.execute(status -> {
            String sqlUpdate = "UPDATE iam_user SET `password`=?, is_active=1 "
                    + " WHERE organization_id=1 AND account_name='admin' AND is_active=0";
            int update = jdbcTemplate.update(sqlUpdate, encodedPassword);
            log.info("Initial admin password, update={}", update);

            if (0 == update) {
                log.info("Set password affect no account, may conflict with other operation, "
                        + "skip reset password for privation connection");
                return true;
            }
            String sqlResetConnectionPassword = "UPDATE connect_connection "
                    + "SET is_password_saved = 0 , `password` = NULL,  sys_tenant_password = NULL "
                    + "WHERE owner_id IN (SELECT `id` FROM iam_user WHERE organization_id=1 AND account_name = 'admin')  "
                    + "  AND visible_scope = 'PRIVATE' ";
            update = jdbcTemplate.update(sqlResetConnectionPassword);
            if (update > 0) {
                log.warn("Detect private connection exists for account 'admin', "
                        + "all related private connection password was reset for security, "
                        + "affectedConnectionCount={}", update);
            }
            return true;
        });
    }

    private String getInitialPassword() {
        return SystemUtils.getEnvOrProperty(ENV_INITIAL_PASSWORD);
    }

}


