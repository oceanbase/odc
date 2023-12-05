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
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/12/5 11:06
 * @Description: []
 */
@Slf4j
@Migratable(version = "4.2.3.7", description = "init default regulation rule applying", repeatable = true,
        ignoreChecksum = true)
public class R4237DefaultRuleApplyingMigrate implements JdbcMigratable {
    private static final String MIGRATE_CONFIG_FILE = "init-config/init/regulation-rule-applying.yaml";

    private DefaultRuleApplyingRepository defaultRuleApplyingRepository;
    private RuleMetadataRepository ruleMetadataRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrate(DataSource dataSource) {
        this.defaultRuleApplyingRepository = SpringContextUtil.getBean(DefaultRuleApplyingRepository.class);
        this.ruleMetadataRepository = SpringContextUtil.getBean(RuleMetadataRepository.class);

        Map<String, MetadataEntity> metadataName2Metadata =
                ruleMetadataRepository.findAll().stream().collect(Collectors.toMap(MetadataEntity::getName, e -> e));
        List<DefaultRuleApplyingEntity> expected =
                YamlUtils.fromYaml(MIGRATE_CONFIG_FILE, new TypeReference<List<DefaultRuleApplyingEntity>>() {});
        expected.stream().forEach(e -> {
            MetadataEntity metadataEntity = metadataName2Metadata.get(e.getRulesetName());
            if (metadataEntity == null) {
                throw new RuntimeException("metadata name " + e.getRulesetName() + " not found");
            }
            e.setRuleMetadataId(metadataEntity.getId());
        });

        List<DefaultRuleApplyingEntity> actual = defaultRuleApplyingRepository.findAll();

        List<DefaultRuleApplyingEntity> toAdd =
                expected.stream().filter(metadata -> !actual.contains(metadata)).collect(Collectors.toList());

        List<DefaultRuleApplyingEntity> toRemove =
                actual.stream().filter(metadata -> !expected.contains(metadata)).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(toAdd)) {
            log.info("new default rule applying detected, start to add");
            defaultRuleApplyingRepository.saveAll(toAdd);
            log.info("add new default rule applying success, size={}", toAdd.size());
        }
        if (CollectionUtils.isNotEmpty(toRemove)) {
            log.info("deprecated default rule applying detected, start to remove");
            defaultRuleApplyingRepository.deleteAll(toRemove);
            log.info("remove deprecated default rule applying success, size={}", toRemove.size());
        }
    }
}
