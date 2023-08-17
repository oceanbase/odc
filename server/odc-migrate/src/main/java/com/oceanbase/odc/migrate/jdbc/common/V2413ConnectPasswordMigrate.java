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

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.crypto.CryptoUtils;
import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.service.iam.model.UserInfo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * use 2.4.1.3 so the connect password migrate will after user password migrate
 * 
 * @author yizhou.xw
 * @version : V2412ConnectPasswordMigrate.java, v 0.1 2021-04-02 13:01
 */
@Slf4j
@Migratable(version = "2.4.1.3", description = "connect password migrate")
public class V2413ConnectPasswordMigrate implements JdbcMigratable {

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
            log.info("connect password migrated, userCount={}, total={}", users.size(), total);
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
            String salt = CryptoUtils.generateSalt();
            String userPassword = user.getPassword();
            String sysUserPassword = sessionInfo.getSysUserPassword();
            if (StringUtils.isNotEmpty(sysUserPassword)) {
                sysUserPassword = OdcEncrypteUtil.decode(sysUserPassword);
            }
            sessionInfo.setSalt(salt);
            sessionInfo.setPassword(encryptPassword(userPassword, salt, sessionInfo.getPassword()));
            sessionInfo.setSysUserPassword(encryptPassword(userPassword, salt, sysUserPassword));
            sessionInfo.setCipher(Cipher.AES256SALT);
            rows += updateSession(sessionInfo);
        }
        log.info("migrate connect password for user, userId={}, requireMigrateSessionCount={}, rows={}",
                user.getId(), sessionInfoList.size(), rows);
        return rows;
    }

    private String encryptPassword(String userPassword, String salt, String password) {
        if (StringUtils.isEmpty(password)) {
            return password;
        }
        TextEncryptor encryptor = Encryptors.aesBase64(userPassword, salt);
        return encryptor.encrypt(password);
    }

    private int updateSession(SessionInfo sessionInfo) {
        String update1 = "UPDATE `odc_session_manager` SET `password`=?, `salt`=?, `cipher`=? WHERE id=?";
        String update2 = "UPDATE `odc_session_extended` SET `sys_user_password`=? WHERE sid=?";
        return txTemplate.<Integer>execute(status -> {
            int row1 = jdbcTemplate.update(update1, sessionInfo.getPassword(), sessionInfo.getSalt(),
                    sessionInfo.getCipher().name(), sessionInfo.getSid());
            int row2 = jdbcTemplate.update(update2, sessionInfo.getSysUserPassword(), sessionInfo.getSid());
            log.info("update connection password, userId={}, sid={}, row1={}, row2={}",
                    sessionInfo.getUserId(), sessionInfo.getSid(), row1, row2);
            return row1 + row2;
        });
    }

    private List<SessionInfo> listRequireMigrateSessionsByUserId(long userId) {
        String sql = "SELECT t1.id AS sid, t1.user_id AS user_id,"
                + " t1.`password` AS `password`, t2.sys_user_password AS sys_user_password\n"
                + " FROM `odc_session_manager` t1"
                + " LEFT JOIN `odc_session_extended` t2 ON t1.id=t2.sid"
                + " WHERE t1.user_id=? AND t1.cipher='RAW'";
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
        private Cipher cipher;
    }

    public static class OdcEncrypteUtil {
        private static final Base64 base64 = new Base64();

        public static String decode(String pwd) {
            return new String(base64.decode(pwd), StandardCharsets.UTF_8);
        }

        public static String encode(String pwd) {
            return base64.encodeToString(pwd.getBytes());
        }
    }

}
