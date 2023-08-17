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

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.service.iam.model.UserInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * use 2.4.1.2 so the data migrate will after schema migrate
 * 
 * @author yizhou.xw
 * @version : V2411UserPasswordMigrate.java, v 0.1 2021-04-02 12:59
 */
@Slf4j
@Migratable(version = "2.4.1.2", description = "user password migrate")
public class V2412UserPasswordMigrate implements JdbcMigratable {
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<UserInfo> users = listUsersNeedMigrate(jdbcTemplate);
        if (CollectionUtils.isEmpty(users)) {
            log.info("found no users need migrate password, skip it");
            return;
        }
        log.info("found users need migrate password, userCount={}", users.size());
        int migratedUserCount = 0;
        for (UserInfo user : users) {
            if (StringUtils.isEmpty(user.getPassword())) {
                log.warn("skip migrate for empty password user, username={}", user.getEmail());
                continue;
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            migratedUserCount += updateUserPassword(jdbcTemplate, user);
        }
        log.info("migrate users complete, migratedUserCount={}", migratedUserCount);
    }

    private int updateUserPassword(JdbcTemplate jdbcTemplate, UserInfo user) {
        String sql = "update odc_user_info set `password`=?, cipher=? where `id`=? AND cipher='RAW'";
        return jdbcTemplate.update(sql, user.getPassword(), Cipher.BCRYPT.name(), user.getId());
    }

    private List<UserInfo> listUsersNeedMigrate(JdbcTemplate jdbcTemplate) {
        String sql = "select `id`, `email`, `password`, cipher from odc_user_info where cipher = 'RAW'";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserInfo.class));
    }

}
