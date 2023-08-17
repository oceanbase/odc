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
package com.oceanbase.odc.service.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;

import javax.sql.DataSource;
import javax.validation.ConstraintViolationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.config.UserConfigDO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserConfigServiceTest extends ServiceTestEnv {

    private final Long userId = (long) new Random().nextInt(100000);
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private UserConfigService configService;
    @Autowired
    private DataSource metadbDatasource;

    @Before
    public void initMetadb() throws SQLException {
        try (Connection connection = metadbDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String sql = "truncate table `odc_user_configuration`";
                statement.executeUpdate(sql);
            }
        }
    }

    @Test
    public void testInsertNullUserConfig() {
        thrown.expect(ConstraintViolationException.class);
        thrown.expectMessage("insert.arg0: must not be null");
        configService.insert(null);
    }

    @Test
    public void testInsertUserConfigWithoutKey() {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        thrown.expect(ConstraintViolationException.class);
        configService.insert(config);
    }

    @Test
    public void testInsertUserConfig() {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        config.setKey("ORACLE_AUTO_COMMIT");
        config.setValue("ON");
        config.setDescription("ORACLE_AUTO_COMMIT.getDescription()");
        UserConfigDO config1 = configService.insert(config);
        Assert.assertEquals(config, config1);
    }

    @Test
    public void testQueryNullConfigList() {
        UserConfigDO row1 = new UserConfigDO();
        row1.setUserId(userId);
        row1.setKey("ORACLE_AUTO_COMMIT");
        row1.setValue("ON");
        row1.setDescription("ORACLE_AUTO_COMMIT.getDescription()");
        UserConfigDO config1 = configService.insert(row1);
        Assert.assertEquals(config1, row1);

        UserConfigDO row2 = new UserConfigDO();
        row2.setUserId(userId);
        row2.setKey("MYSQL_AUTO_COMMIT");
        row2.setValue("OFF");
        row2.setDescription("MYSQL_AUTO_COMMIT.getDescription()");
        config1 = configService.insert(row2);
        Assert.assertEquals(config1, row2);

        List<UserConfigDO> configMap = configService.query(userId);
        Assert.assertEquals(configMap.size(), 2);

        for (UserConfigDO configDO : configMap) {
            if (configDO.getKey().equals(row1.getKey())) {
                Assert.assertEquals(configDO.getValue(), row1.getValue());
            } else {
                Assert.assertEquals(configDO.getValue(), row2.getValue());
            }
        }
    }

    @Test
    public void testQueryNullUserConfigItem() {
        Object value = configService.query(userId, "MYSQL_AUTO_COMMIT");
        Assert.assertNull(value);
    }

    @Test
    public void testQueryUserConfigItem() {
        UserConfigDO row1 = new UserConfigDO();
        row1.setUserId(userId);
        row1.setKey("ORACLE_AUTO_COMMIT");
        row1.setValue("ON");
        row1.setDescription("ORACLE_AUTO_COMMIT.getDescription()");
        UserConfigDO config1 = configService.insert(row1);
        Assert.assertEquals(config1, row1);

        Object value = configService.query(userId, "ORACLE_AUTO_COMMIT");
        Assert.assertEquals(value, row1.getValue());
    }

    @Test
    public void testUpdateNoExistUserConfig() {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        config.setKey("ORACLE_AUTO_COMMIT");
        config.setValue("ON");
        config.setDescription("ORACLE_AUTO_COMMIT.getDescription()");
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("UserConfig does not exist");
        configService.update(config);
    }

    @Test
    public void testUpdateUserConfigItem() {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        config.setKey("ORACLE_AUTO_COMMIT");
        config.setValue("ON");
        config.setDescription("ORACLE_AUTO_COMMIT.getDescription()");
        UserConfigDO config1 = configService.insert(config);
        Assert.assertEquals(config, config1);

        config.setValue("OFF");
        config1 = configService.update(config);
        Assert.assertEquals(config, config1);

        Object value = configService.query(userId, "ORACLE_AUTO_COMMIT");
        Assert.assertEquals(value, config.getValue());
    }

    @Test
    public void testDeleteNoExistUserConfig() {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        config.setKey("ORACLE_AUTO_COMMIT");
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("UserConfig does not exist");
        configService.delete(config);
    }

    @Test
    public void testDeleteUserConfigItem() {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        config.setKey("ORACLE_AUTO_COMMIT");
        config.setValue("ON");
        config.setDescription("ORACLE_AUTO_COMMIT.getDescription()");
        UserConfigDO config1 = configService.insert(config);
        Assert.assertEquals(config, config1);

        config1 = configService.delete(config);
        Assert.assertEquals(config, config1);

        Object value = configService.query(userId, "ORACLE_AUTO_COMMIT");
        Assert.assertEquals(value, null);
    }

}
