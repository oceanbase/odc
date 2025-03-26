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
package com.oceanbase.odc.service.regulation.ruleset;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.util.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.service.regulation.ruleset.model.MetadataLabel;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;

/**
 * @Author: Lebie
 * @Date: 2023/5/22 20:17
 * @Description: []
 */
public class RuleMetadataServiceTest extends ServiceTestEnv {
    private static final String MIGRATE_CONFIG_FILE = "init-config/init/regulation-rule-metadata.yaml";
    private List<MetadataEntity> metadatas;
    @Autowired
    private RuleMetadataService ruleMetadataService;

    @Before
    public void setUp() {
        this.metadatas = YamlUtils.fromYaml(MIGRATE_CONFIG_FILE, new TypeReference<List<MetadataEntity>>() {});
    }

    @Test
    public void test_ListSqlConsoleRule_Success() {
        QueryRuleMetadataParams params = new QueryRuleMetadataParams();
        params.setRuleTypes(Arrays.asList(RuleType.SQL_CONSOLE));
        params.setLabels(
                Maps.newHashMap(MetadataLabel.SUPPORTED_DIALECT_TYPE, Arrays.asList("OB_MYSQL", "OB_ORACLE")));
        int actual = ruleMetadataService.list(params).size();
        long expected = this.metadatas.stream().filter(metadata -> metadata.getLabels().stream().anyMatch(
                label -> StringUtils.equals("OB_MYSQL", label.getValue())
                        || StringUtils.equals("OB_ORACLE", label.getValue())))
                .filter(m -> m.getType() == RuleType.SQL_CONSOLE)
                .count();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_ListAll_Success() {
        QueryRuleMetadataParams params = new QueryRuleMetadataParams();
        int actual = ruleMetadataService.list(params).size();
        long expected = metadatas.size();
        Assert.assertEquals(expected, actual);
    }
}
