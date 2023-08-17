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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.config.model.Configuration;

public class SystemConfigServiceTest extends ServiceTestEnv {
    @Autowired
    private SystemConfigService service;

    @Test
    public void testQueryByKeyPrefix() {
        List<Configuration> configsInDb = service.queryByKeyPrefix("sqlexecute");

        List<Configuration> configsExpected = new ArrayList<>(4);
        configsExpected.add(new Configuration("sqlexecute.defaultDelimiter", ";"));
        configsExpected.add(new Configuration("sqlexecute.mysqlAutoCommitMode", "ON"));
        configsExpected.add(new Configuration("sqlexecute.oracleAutoCommitMode", "ON"));
        configsExpected.add(new Configuration("sqlexecute.defaultQueryLimit", "1000"));
        configsExpected.add(new Configuration("sqlexecute.defaultObjectDraggingOption", "object_name"));
        configsExpected.add(new Configuration("sqlexecute.sqlCheckMode", "AUTO"));

        Assert.assertTrue(configsExpected.containsAll(configsInDb));
    }

    @Test
    public void testOscDisabledDefault() {
        List<Configuration> configsInDb = service.queryByKeyPrefix("odc.features.task.osc.enabled");
        Assert.assertFalse(CollectionUtils.isEmpty(configsInDb));
        Assert.assertEquals(Boolean.FALSE, Boolean.valueOf(configsInDb.get(0).getValue()));

    }
}
