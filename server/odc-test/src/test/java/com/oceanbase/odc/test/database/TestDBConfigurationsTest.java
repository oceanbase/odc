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
package com.oceanbase.odc.test.database;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author gaoda.xy
 * @date 2023/2/20 10:08
 */
public class TestDBConfigurationsTest {
    @Test
    public void test_GetTestOBMysqlConfiguration_Success() {
        TestDBConfiguration config = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getDataSource());
        Assert.assertNotNull(config.getHost());
        Assert.assertNotNull(config.getPort());
        Assert.assertNotNull(config.getTenant());
        Assert.assertNotNull(config.getUsername());
        Assert.assertNotNull(config.getPassword());
        Assert.assertNotNull(config.getSysPassword());
        Assert.assertNotNull(config.getSysUsername());
    }

    @Test
    public void test_GetTestOBOracleConfiguration_Success() {
        TestDBConfiguration config = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getDataSource());
        Assert.assertNotNull(config.getHost());
        Assert.assertNotNull(config.getPort());
        Assert.assertNotNull(config.getTenant());
        Assert.assertNotNull(config.getUsername());
        Assert.assertNotNull(config.getPassword());
        Assert.assertNotNull(config.getSysPassword());
        Assert.assertNotNull(config.getSysUsername());
    }

    @Test
    public void test_GetTestMySQLConfiguration_Success() {
        TestDBConfiguration config = TestDBConfigurations.getInstance().getTestMysqlConfiguration();
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getDataSource());
        Assert.assertNotNull(config.getHost());
        Assert.assertNotNull(config.getPort());
        Assert.assertNotNull(config.getUsername());
        Assert.assertNotNull(config.getPassword());
    }
}
