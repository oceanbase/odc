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

import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.service.iam.model.UserInfo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * fix empty password not encrypted
 * 
 * @author yizhou.xw
 * @version : V2422ConnectEmptyPasswordMigrate.java, v 0.1 2021-05-08 14:30
 */
@Slf4j
@Migratable(version = "2.4.2.2", description = "connect empty password migrate")
public class V2422ConnectEmptyPasswordMigrate implements JdbcMigratable {

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate txTemplate;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        List<UserInfo> users = listUsers();
        int total = 0;
        for (UserInfo user : users) {
            total += migrateForUser(user);
        }
        if (total > 0) {
            log.info("empty connect password migrated, userCount={}, total={}", users.size(), total);
        }
    }

    private int migrateForUser(UserInfo user) {
        List<SessionInfo> sessionInfoList = listRequireMigrateSessionsByUserId(user.getId());
        if (sessionInfoList.isEmpty()) {
            log.debug("skip migrate for user has no session require be migrated, userId={}", user.getId());
            return 0;
        }
        int rows = 0;
        for (SessionInfo sessionInfo : sessionInfoList) {
            String userPassword = user.getPassword();
            TextEncryptor encryptor = Encryptors.aesBase64(userPassword, sessionInfo.getSalt());
            sessionInfo.setPassword(encryptIfEmptyPassword(encryptor, sessionInfo.getPassword()));
            sessionInfo.setSysUserPassword(encryptIfEmptyPassword(encryptor, sessionInfo.getSysUserPassword()));
            rows += updateSession(sessionInfo);
        }
        log.info("migrate connect empty password for user, userId={}, requireMigrateSessionCount={}, rows={}",
                user.getId(), sessionInfoList.size(), rows);
        return rows;
    }

    private String encryptIfEmptyPassword(TextEncryptor encryptor, String password) {
        if (null == password) {
            return null;
        }
        if (password.length() > 0) {
            return password;
        }
        return encryptor.encrypt(password);
    }

    private int updateSession(SessionInfo sessionInfo) {
        String update1 = "UPDATE `odc_session_manager` SET `password`=? WHERE id=?";
        String update2 = "UPDATE `odc_session_extended` SET `sys_user_password`=? WHERE sid=?";
        return txTemplate.<Integer>execute(status -> {
            String password = sessionInfo.getPassword();
            String sysUserPassword = sessionInfo.getSysUserPassword();
            int row1 = (0 == StringUtils.length(password)) ? 0
                    : jdbcTemplate.update(update1, password, sessionInfo.getSid());
            int row2 = (0 == StringUtils.length(sysUserPassword)) ? 0
                    : jdbcTemplate.update(update2, sysUserPassword, sessionInfo.getSid());
            log.debug("update connection empty password, userId={}, sid={}, row1={}, row2={}",
                    sessionInfo.getUserId(), sessionInfo.getSid(), row1, row2);
            return row1 + row2;
        });
    }

    private List<SessionInfo> listRequireMigrateSessionsByUserId(long userId) {
        String sql = "SELECT t1.id AS sid, t1.user_id AS user_id, t1.salt as salt,"
                + " t1.password as password, t2.sys_user_password AS sys_user_password"
                + " FROM `odc_session_manager` t1"
                + " LEFT JOIN `odc_session_extended` t2 ON t1.id=t2.sid"
                + " WHERE t1.user_id=? AND t1.cipher='AES256SALT' AND (t1.password=''  OR t2.sys_user_password='')";
        return jdbcTemplate.query(sql, new Object[] {userId}, new BeanPropertyRowMapper<>(SessionInfo.class));
    }

    private List<UserInfo> listUsers() {
        String sql = "select `id`, `email`, `password`, cipher from odc_user_info where cipher='BCRYPT'";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserInfo.class));
    }

    @Data
    public static class SessionInfo {
        private long sid;
        private long userId;
        private String password;
        private String sysUserPassword;
        private String salt;
    }
}
