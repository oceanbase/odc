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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataLabelRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.PropertyMetadataRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/22 11:06
 * @Description: []
 */
@Slf4j
@Migratable(version = "4.2.0.17", description = "init regulation rule metadata", repeatable = true,
        ignoreChecksum = true)
public class R42017RuleMetadataMigrate implements JdbcMigratable {
    private static final String MIGRATE_CONFIG_FILE = "init-config/init/regulation-rule-metadata.yaml";

    private RuleMetadataRepository ruleMetadataRepository;

    private PropertyMetadataRepository propertyMetadataRepository;

    private MetadataLabelRepository metadataLabelRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrate(DataSource dataSource) {
        this.ruleMetadataRepository = SpringContextUtil.getBean(RuleMetadataRepository.class);
        this.propertyMetadataRepository = SpringContextUtil.getBean(PropertyMetadataRepository.class);
        this.metadataLabelRepository = SpringContextUtil.getBean(MetadataLabelRepository.class);


        List<MetadataEntity> expected =
                YamlUtils.fromYaml(MIGRATE_CONFIG_FILE, new TypeReference<List<MetadataEntity>>() {});
        List<MetadataEntity> actual = ruleMetadataRepository.findAll();
        List<MetadataEntity> toAdd =
                expected.stream().filter(metadata -> !actual.contains(metadata)).collect(Collectors.toList());
        List<MetadataEntity> toRemove =
                actual.stream().filter(metadata -> !expected.contains(metadata)).collect(Collectors.toList());

        for (MetadataEntity metadata : toAdd) {
            if (CollectionUtils.isNotEmpty(metadata.getPropertyMetadatas())) {
                metadata.getPropertyMetadatas().stream()
                        .forEach(propertyMetadata -> propertyMetadata.setRuleMetadata(metadata));
            }
            if (CollectionUtils.isNotEmpty(metadata.getLabels())) {
                metadata.getLabels().stream().forEach(label -> label.setMetadata(metadata));
            }
        }
        for (MetadataEntity metadata : toRemove) {
            if (CollectionUtils.isNotEmpty(metadata.getPropertyMetadatas())) {
                propertyMetadataRepository.deleteInBatch(metadata.getPropertyMetadatas());
            }
            if (CollectionUtils.isNotEmpty(metadata.getLabels())) {
                metadataLabelRepository.deleteInBatch(metadata.getLabels());
            }
        }
        if (CollectionUtils.isNotEmpty(toRemove)) {
            ruleMetadataRepository.deleteInBatch(toRemove);
            log.info("remove regulation rules successfully, total removed rule count={}", toRemove.size());
        }

        if (CollectionUtils.isNotEmpty(toAdd)) {
            ruleMetadataRepository.saveAll(toAdd);
            log.info("add new regulation rules successfully, total added rule count={}", toAdd.size());
        }
    }
}
