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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.jpa.JsonListConverter;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
        List<InnerDefaultRuleApplying> expected =
                YamlUtils.fromYaml(MIGRATE_CONFIG_FILE, new TypeReference<List<InnerDefaultRuleApplying>>() {});
        Map<String, List<DefaultRuleApplyingEntity>> actualRulesetName2RuleApplyings = defaultRuleApplyingRepository
                .findAll().stream().collect(Collectors.groupingBy(DefaultRuleApplyingEntity::getRulesetName));


    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class InnerDefaultRuleApplying {
        private Boolean enabled;

        private Integer level;

        private Long rulesetName;

        private String ruleName;

        private List<String> appliedDialectTypes;

        private String propertiesJson;
    }
}
