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
package com.oceanbase.odc.metadb.regulation;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;

/**
 * @Author: Lebie
 * @Date: 2024/2/5 10:07
 * @Description: []
 */
public class RuleApplyingRepositoryTest extends ServiceTestEnv {
    @Autowired
    private RuleApplyingRepository ruleApplyingRepository;

    @Test
    public void testBatchCreate_Success() {
        List<RuleApplyingEntity> saved = ruleApplyingRepository.batchCreate(listEntities());
        Assert.assertNotNull(saved.get(0).getId());

    }

    private List<RuleApplyingEntity> listEntities() {
        RuleApplyingEntity entity = new RuleApplyingEntity();
        entity.setOrganizationId(1L);
        entity.setPropertiesJson("{}");
        entity.setRulesetId(1L);
        entity.setEnabled(true);
        entity.setLevel(1);
        entity.setRuleMetadataId(1L);
        entity.setAppliedDialectTypes(Arrays.asList(DialectType.OB_MYSQL.name()));
        return Arrays.asList(entity);

    }
}
