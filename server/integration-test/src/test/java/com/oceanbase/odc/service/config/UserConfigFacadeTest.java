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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.config.UserConfigDO;
import com.oceanbase.odc.service.config.model.UserConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserConfigFacadeTest extends MockedAuthorityTestEnv {

    private final Long userId = (long) new Random().nextInt(100000);
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private UserConfigFacade configFacade;
    @Autowired
    private DataSource metadbDatasource;
    @Autowired
    private UserConfigService configService;

    @Before
    public void initMetadb() throws SQLException {
        try (Connection connection = metadbDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String sql = "truncate table `odc_user_configuration`";
                statement.executeUpdate(sql);
            }
        }
        grantAllPermissions(ResourceType.ODC_SYSTEM_CONFIG);
    }

    @After
    public void clear() {
        DefaultLoginSecurityManager.removeSecurityContext();
        DefaultLoginSecurityManager.removeContext();
    }

    @Test
    public void testInsertUserConfig() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setMysqlAutoCommitMode("OFF");
        config.setOracleAutoCommitMode("ON");
        config.setDefaultQueryLimit(123456);

        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);
    }

    @Test
    public void testQueryAllUserConfig() {
        UserConfig config = configFacade.query(userId);
        UserConfig config1 = new UserConfig();
        Assert.assertEquals(config.getDefaultDelimiter(), config1.getDefaultDelimiter());
        Assert.assertEquals(config.getOracleAutoCommitMode(), config1.getOracleAutoCommitMode());
        Assert.assertEquals(config.getMysqlAutoCommitMode(), config1.getMysqlAutoCommitMode());
        Assert.assertEquals(config.getDefaultQueryLimit(), config1.getDefaultQueryLimit());
    }

    @Test
    public void testQueryAllUserConfigWithSettingDelimiter() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");

        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);

        UserConfig queryConfig = configFacade.query(userId);
        Assert.assertEquals(queryConfig, config);
    }

    @Test
    public void testQueryAllUserConfigWithSettingOracleAutoCommit() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setOracleAutoCommitMode("OFF");

        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);

        UserConfig config2 = configFacade.query(userId);
        Assert.assertEquals(config2.getDefaultDelimiter(), "$$");
        Assert.assertEquals(config2.getOracleAutoCommitMode(), "OFF");
        Assert.assertEquals(config2.getMysqlAutoCommitMode(), config.getMysqlAutoCommitMode());
        Assert.assertEquals(config2.getDefaultQueryLimit(), config.getDefaultQueryLimit());
    }

    @Test
    public void testSetUserConfigWithIllegalOracleAutoCommit() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setOracleAutoCommitMode("123");

        thrown.expectMessage("put.arg1.oracleAutoCommitMode: Oracle auto commit mode can only accept the value ON/OFF");
        thrown.expect(ConstraintViolationException.class);
        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);
    }

    @Test
    public void testQueryAllUserConfigWithSettingMysqlAutoCommit() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setOracleAutoCommitMode("OFF");
        config.setMysqlAutoCommitMode("OFF");

        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);

        UserConfig config2 = configFacade.query(userId);
        Assert.assertEquals(config2.getDefaultDelimiter(), "$$");
        Assert.assertEquals(config2.getOracleAutoCommitMode(), "OFF");
        Assert.assertEquals(config2.getMysqlAutoCommitMode(), "OFF");
        Assert.assertEquals(config2.getDefaultQueryLimit(), config.getDefaultQueryLimit());
    }

    @Test
    public void testQueryAllUserConfigWithSettingLimit() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setOracleAutoCommitMode("OFF");
        config.setMysqlAutoCommitMode("OFF");
        config.setDefaultQueryLimit(123456);

        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);

        UserConfig config2 = configFacade.query(userId);
        Assert.assertEquals(config2.getDefaultDelimiter(), "$$");
        Assert.assertEquals(config2.getOracleAutoCommitMode(), "OFF");
        Assert.assertEquals(config2.getMysqlAutoCommitMode(), "OFF");
        Assert.assertEquals(123456L, (long) config2.getDefaultQueryLimit());
    }

    @Test
    public void testUpdateUserConfigItemWithSettingDelimiterBefore() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        UserConfig config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);

        config = new UserConfig();
        config.setDefaultDelimiter("%%");
        config.setMysqlAutoCommitMode(config.getMysqlAutoCommitMode());
        config.setOracleAutoCommitMode(config.getOracleAutoCommitMode());
        config.setDefaultQueryLimit(config.getDefaultQueryLimit());
        config.setDefaultObjectDraggingOption(config.getDefaultObjectDraggingOption());

        config1 = configFacade.put(userId, config);
        Assert.assertEquals(config, config1);

        List<UserConfigDO> configMap = configService.query(userId);
        Assert.assertEquals(6, configMap.size());
    }
}
