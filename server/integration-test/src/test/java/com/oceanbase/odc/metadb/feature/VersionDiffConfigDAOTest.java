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
package com.oceanbase.odc.metadb.feature;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.feature.model.VersionDiffConfig;
import com.oceanbase.odc.test.tool.TestRandom;

public class VersionDiffConfigDAOTest extends ServiceTestEnv {

    @Autowired
    private VersionDiffConfigDAO diffConfigDAO;

    @Test
    public void insert_insertVersionDiffConfig_insertSucceed() {
        VersionDiffConfig expect = TestRandom.nextObject(VersionDiffConfig.class);
        Assert.assertEquals(1, diffConfigDAO.insert(expect));
    }

    @Test
    public void delete_deleteVersionDiffConfig_deleteSucceed() {
        VersionDiffConfig expect = TestRandom.nextObject(VersionDiffConfig.class);
        diffConfigDAO.insert(expect);
        List<VersionDiffConfig> expects = diffConfigDAO.query(expect);
        Assert.assertEquals(1, diffConfigDAO.delete(expects.get(0)));
    }

    @Test
    public void update_updateVersionDiffConfig_updateSucceed() {
        VersionDiffConfig expect = TestRandom.nextObject(VersionDiffConfig.class);
        diffConfigDAO.insert(expect);
        List<VersionDiffConfig> expects = diffConfigDAO.query(expect);
        expect = expects.get(0);
        expect.setConfigKey("new config key");
        expect.setConfigValue("new config value");
        expect.setDbMode("new db mode");
        expect.setMinVersion("new min version");
        diffConfigDAO.update(expect);
        List<VersionDiffConfig> actual = diffConfigDAO.query(expect);
        actual.forEach(v -> v.setGmtModify(null));
        Assert.assertEquals(Collections.singletonList(expect), actual);
    }

    @Test
    public void query_queryWithConfigKey_querySucceed() {
        VersionDiffConfig expect = TestRandom.nextObject(VersionDiffConfig.class);
        diffConfigDAO.insert(expect);
        List<VersionDiffConfig> actual = diffConfigDAO.query(expect);
        expect.setId(actual.get(0).getId());
        expect.setGmtModify(null);
        expect.setGmtCreate(null);
        Assert.assertEquals(Collections.singletonList(expect), actual);
    }

    @Test
    public void query_queryWithoutConfigKey_querySucceed() {
        VersionDiffConfig expect = TestRandom.nextObject(VersionDiffConfig.class);
        diffConfigDAO.insert(expect);
        String configKey = expect.getConfigKey();
        expect.setConfigKey(null);
        List<VersionDiffConfig> actual = diffConfigDAO.query(expect);
        expect.setId(actual.get(0).getId());
        expect.setGmtModify(null);
        expect.setGmtCreate(null);
        expect.setConfigKey(configKey);
        Assert.assertEquals(Collections.singletonList(expect), actual);
    }

}
