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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.common.util.OdcFileUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrate for user, from <code>odc_user_info</code> to <code>iam_user</code> init
 * <code>iam_role</code>, <code>iam_permission</code>, <code>iam_user_role</code>
 * <code>iam_role_permission</code> and <code>iam_organization</code>
 *
 * @author yh263208
 * @date 2021-08-26 10:35
 * @since ODC_release_3.2.0
 */
@Slf4j
@Migratable(version = "3.2.0.2", description = "user migrate")
public class V3202BuiltInResourceMigrate implements JdbcMigratable {

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private static final String PROPERTIES_FILE_NAME = "rename.properties";

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = getTransactionTemplate(dataSource);
        // scan odc_user_info
        List<String> reservedAccountNames = new LinkedList<>(OdcConstants.RESERVED_ACCOUNT_NAMES);
        List<String> conflictAccountNames = checkIfAccountNamePresented(reservedAccountNames);
        if (conflictAccountNames.isEmpty()) {
            // if you are here, which means that there is not any account name in
            // <code>ODCConstants.RESERVED_ACCOUNT_NAMES</code>. Just start migrate
            migrateUsers();
            return;
        }
        String renameConfigFile = OdcFileUtil.getStaticPath() + PROPERTIES_FILE_NAME;
        File file = new File(renameConfigFile);
        if (!file.exists()) {
            createPropertiesFile(file, conflictAccountNames);
            log.error("Failed to migrate users, conflict detected, reservedAccountNames={}, renameFile={}",
                    String.join(",", conflictAccountNames), file.getAbsolutePath());
            throw new RuntimeException("Failed to migrate odc users");
        } else {
            Map<String, String> conflictName2NewName =
                    readRenameConfig(file, conflictAccountNames, reservedAccountNames);
            List<String> duplicatedAccountNames = checkIfAccountNamePresented(conflictName2NewName.values());
            if (!duplicatedAccountNames.isEmpty()) {
                log.error("Renamed account name conflicts with the existing account name, conflictNames={}",
                        String.join(",", duplicatedAccountNames));
                throw new RuntimeException("Duplicated account name");
            }
            long effectRows = updateAccountNameForOdcUserInfoTable(conflictName2NewName);
            log.info("Successfully renamed the conflicting account name, effectRows={}, renameInfo={}",
                    effectRows, conflictName2NewName);
            migrateUsers();
        }
    }

    private void migrateUsers() {
        String querySql =
                "SELECT `id`,`name`,`email`,`password`,`gmt_create`,`gmt_modify`,`cipher` FROM `odc_user_info` WHERE 1=1";
        AtomicReference<Exception> thrown = new AtomicReference<>(null);
        Long totalEffectRows = this.transactionTemplate.execute(status -> {
            try {
                AtomicLong effectRows = new AtomicLong(0);
                jdbcTemplate.query(querySql, resultSet -> {
                    UserResource userResource = getFromOdcUserInfoResultSet(resultSet);
                    int effectRow = insertBuiltInUser(userResource);
                    effectRows.addAndGet(effectRow);
                });
                return effectRows.get();
            } catch (Exception e) {
                log.error("Failed to migrate from old user table to new user table", e);
                thrown.set(e);
                status.setRollbackOnly();
            }
            return 0L;
        });
        if (thrown.get() != null) {
            throw new RuntimeException(thrown.get());
        }
        long effectRows = totalEffectRows == null ? 0 : totalEffectRows;
        log.info("Successfully migrated users from old table to new table, effectRows={}", effectRows);
    }

    private int insertBuiltInUser(UserResource user) {
        String insertSql =
                "INSERT INTO `iam_user` (`id`,`name`,`account_name`,`organization_id`,`email_address`,`password`,`cipher`,"
                        + "`is_active`,`is_enabled`,`creator_id`,`is_builtin`,`user_create_time`,`user_update_time`,`create_time`,"
                        + "`update_time`,`description`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `id`=`id`";
        return jdbcTemplate.update(insertSql, user.getId(), user.getName(), user.getAccountName(),
                user.getOrganizationId(), user.getEmail(), user.getPassword(), user.getCipher(), user.isActive(),
                user.isEnabled(), user.getCreatorId(), 0, user.getCreateTime(), user.getUpdateTime(),
                user.getCreateTime(), user.getUpdateTime(), user.getDescription());
    }

    private UserResource getFromOdcUserInfoResultSet(ResultSet resultSet) throws SQLException {
        UserResource user = new UserResource();
        user.setId(resultSet.getLong("id"));
        user.setName(resultSet.getString("name") == null ? user.getId() + "" : resultSet.getString("name"));
        user.setAccountName("account_name_" + user.getId());
        if (resultSet.getString("email") != null) {
            user.setAccountName(resultSet.getString("email"));
        }
        user.setPassword(resultSet.getString("password"));
        user.setOrganizationId(1L);
        user.setActive(true);
        user.setEnabled(true);
        user.setCreatorId(user.getId());
        user.setEmail(user.getAccountName());
        user.setCipher(resultSet.getString("cipher") == null ? "RAW" : resultSet.getString("cipher"));

        long timestamp = System.currentTimeMillis();
        user.setCreateTime(new Timestamp(timestamp));
        if (resultSet.getTimestamp("gmt_create") != null) {
            user.setCreateTime(resultSet.getTimestamp("gmt_create"));
        }
        user.setUpdateTime(new Timestamp(timestamp));
        if (resultSet.getTimestamp("gmt_modify") != null) {
            user.setCreateTime(resultSet.getTimestamp("gmt_modify"));
        }
        return user;
    }

    private long updateAccountNameForOdcUserInfoTable(Map<String, String> conflictName2NewName) {
        String updateSql = "UPDATE `odc_user_info` SET `email`=? WHERE `email`=?";
        List<Object[]> parameters = conflictName2NewName.entrySet().stream()
                .map(entry -> new Object[] {entry.getValue(), entry.getKey()}).collect(Collectors.toList());
        AtomicReference<Exception> thrown = new AtomicReference<>(null);
        Long totalEffectRows = this.transactionTemplate.execute(status -> {
            try {
                long total = 0;
                int[] effectRows = jdbcTemplate.batchUpdate(updateSql, parameters);
                for (int effectRow : effectRows) {
                    total += effectRow;
                }
                return total;
            } catch (Exception e) {
                log.error("Failed to update duplicated account name", e);
                thrown.set(e);
                status.setRollbackOnly();
            }
            return 0L;
        });
        if (thrown.get() != null) {
            throw new RuntimeException(thrown.get());
        }
        return totalEffectRows == null ? 0 : totalEffectRows;
    }

    private Map<String, String> readRenameConfig(File propertiesFile, List<String> conflictAccountNames,
            Collection<String> reservedNames) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (IOException e) {
            log.error("Failed to load properties file, renameFile={}", propertiesFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
        Map<String, String> conflictName2NewName = new HashMap<>();
        Set<String> reservedAccountNames = new HashSet<>(reservedNames);
        for (String conflictName : conflictAccountNames) {
            String newName = properties.getProperty(conflictName);
            if (StringUtils.isBlank(newName)) {
                log.error("Wrong account rename, conflictName={}, newName={}, renameFile={}", conflictName, newName,
                        propertiesFile.getAbsolutePath());
                throw new RuntimeException("Wrong account rename");
            } else if (reservedAccountNames.contains(newName)) {
                log.error("New name is in reserved account name, newName={}, reservedAccountName={}, renameFile={}",
                        newName, String.join(",", reservedAccountNames), propertiesFile.getAbsolutePath());
                throw new RuntimeException("New name is in reserved names");
            }
            conflictName2NewName.putIfAbsent(conflictName, newName);
        }
        return conflictName2NewName;
    }

    private void createPropertiesFile(File propertiesFile, List<String> conflictAccountNames) {
        File parent = new File(propertiesFile.getParent());
        if (!parent.exists() && !parent.mkdirs()) {
            log.error("Failed to mkdirs for rename file, filePath={}", propertiesFile.getParent());
            throw new RuntimeException("Fail to mkdirs " + PROPERTIES_FILE_NAME);
        }
        try {
            if (!propertiesFile.createNewFile()) {
                log.error("Failed to create rename file, filePath={}", propertiesFile.getAbsolutePath());
                throw new RuntimeException("Fail to create " + PROPERTIES_FILE_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Properties properties = new Properties();
        for (String conflictName : conflictAccountNames) {
            properties.setProperty(conflictName, "");
        }
        try {
            properties.store(new FileOutputStream(propertiesFile),
                    "Please enter the rename of the conflicting account name here");
        } catch (IOException e) {
            log.error("Failed to store property file, filePath={}", propertiesFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    private List<String> checkIfAccountNamePresented(Collection<String> reservedNames) {
        String querySql = "SELECT `email` FROM `odc_user_info`";
        List<String> returnVal = new LinkedList<>();
        Set<String> reservedAccountNames = new HashSet<>(reservedNames);
        jdbcTemplate.query(querySql, resultSet -> {
            String accountName = resultSet.getString(1);
            if (reservedAccountNames.contains(accountName)) {
                returnVal.add(accountName);
            }
        });
        return returnVal;
    }

    private TransactionTemplate getTransactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);

        TransactionTemplate template = new TransactionTemplate();
        template.setTransactionManager(transactionManager);
        template.setIsolationLevel(TransactionTemplate.ISOLATION_DEFAULT);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return template;
    }

    @Getter
    @Setter
    public static class UserResource {
        private Long id;
        private String name;
        private String accountName;
        private String password;
        private Long organizationId;
        private boolean active;
        private boolean enabled;
        private Long creatorId;
        private String description;
        private String email;
        private String cipher = "BCRYPT";
        private Timestamp createTime;
        private Timestamp updateTime;
    }

}
