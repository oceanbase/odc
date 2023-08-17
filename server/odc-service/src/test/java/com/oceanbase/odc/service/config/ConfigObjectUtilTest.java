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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.metadb.config.UserConfigDO;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.UserConfig;
import com.oceanbase.odc.service.config.util.ConfigObjectUtil;

/**
 * Test object for ConfigObjectUtil
 *
 * @author yh263208
 * @date 2021-05-29 01:04
 * @since ODC_release_2.4.2
 */
public class ConfigObjectUtilTest {

    @Test
    public void testConvertConfigObjectToDTO() {
        UserConfig userConfig = new UserConfig();
        List<Configuration> configDTOList = ConfigObjectUtil.convertToDTO(userConfig);
        Assert.assertEquals(6, configDTOList.size());
        Map<String, String> map =
                configDTOList.stream().collect(Collectors.toMap(Configuration::getKey, Configuration::getValue));
        Assert.assertEquals(6, map.size());
        Assert.assertEquals(userConfig.getDefaultDelimiter(), map.get("sqlexecute.defaultDelimiter"));
        Assert.assertEquals(userConfig.getDefaultQueryLimit().toString(), map.get("sqlexecute.defaultQueryLimit"));
        Assert.assertEquals(userConfig.getMysqlAutoCommitMode(), map.get("sqlexecute.mysqlAutoCommitMode"));
        Assert.assertEquals(userConfig.getOracleAutoCommitMode(), map.get("sqlexecute.oracleAutoCommitMode"));
        Assert.assertEquals(userConfig.getDefaultObjectDraggingOption(),
                map.get("sqlexecute.defaultObjectDraggingOption"));
        Assert.assertEquals(userConfig.getSessionMode(), map.get("connect.sessionMode"));
    }

    @Test
    public void testConvertConfigObjectToDO() {
        UserConfig userConfig = new UserConfig();
        List<UserConfigDO> configDTOList = ConfigObjectUtil.convertToDO(userConfig);
        Assert.assertEquals(6, configDTOList.size());
        Map<String, String> map =
                configDTOList.stream().collect(Collectors.toMap(UserConfigDO::getKey, UserConfigDO::getValue));
        Assert.assertEquals(6, map.size());
        Assert.assertEquals(userConfig.getDefaultDelimiter(), map.get("sqlexecute.defaultDelimiter"));
        Assert.assertEquals(userConfig.getDefaultQueryLimit().toString(), map.get("sqlexecute.defaultQueryLimit"));
        Assert.assertEquals(userConfig.getMysqlAutoCommitMode(), map.get("sqlexecute.mysqlAutoCommitMode"));
        Assert.assertEquals(userConfig.getOracleAutoCommitMode(), map.get("sqlexecute.oracleAutoCommitMode"));
        Assert.assertEquals(userConfig.getDefaultObjectDraggingOption(),
                map.get("sqlexecute.defaultObjectDraggingOption"));
        Assert.assertEquals(userConfig.getSessionMode(), map.get("connect.sessionMode"));
    }

    @Test
    public void testSetConfigObjectFromDTO() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setDefaultQueryLimit(1500);
        List<Configuration> configDTOList = ConfigObjectUtil.convertToDTO(config);
        config = ConfigObjectUtil.setConfigObjectFromDTO(configDTOList, new UserConfig());
        Assert.assertEquals("$$", config.getDefaultDelimiter());
        Assert.assertEquals(Integer.valueOf(1500), config.getDefaultQueryLimit());
    }

    @Test
    public void testSetConfigObjectFromDO() {
        UserConfig config = new UserConfig();
        config.setDefaultDelimiter("$$");
        config.setDefaultQueryLimit(1500);
        List<UserConfigDO> configDOList = ConfigObjectUtil.convertToDO(config);
        config = ConfigObjectUtil.setConfigObjectFromDO(configDOList, new UserConfig());
        Assert.assertEquals("$$", config.getDefaultDelimiter());
        Assert.assertEquals(Integer.valueOf(1500), config.getDefaultQueryLimit());
    }

}
