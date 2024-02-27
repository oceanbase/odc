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
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.notification.PolicyMetadataEntity;
import com.oceanbase.odc.metadb.notification.PolicyMetadataRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/8
 */
@Slf4j
@Migratable(version = "4.2.4.3", description = "Built-in notification policy metadata migrate",
        repeatable = true, ignoreChecksum = true)
public class R4243NotificationPolicyMetaMigrate implements JdbcMigratable {
    private static final String MIGRATE_CONFIG_FILE = "init-config/init/notification-policy-metadata.yaml";

    @Override
    public void migrate(DataSource dataSource) {

        PolicyMetadataRepository repository = SpringContextUtil.getBean(PolicyMetadataRepository.class);

        List<PolicyMetadataEntity> expected = YamlUtils.fromYaml(MIGRATE_CONFIG_FILE,
                new TypeReference<List<PolicyMetadataEntity>>() {});

        List<PolicyMetadataEntity> actual = repository.findAll();

        List<PolicyMetadataEntity> toAdd =
                expected.stream().filter(metadata -> !actual.contains(metadata)).collect(Collectors.toList());
        List<PolicyMetadataEntity> toRemove =
                actual.stream().filter(metadata -> !expected.contains(metadata)).collect(Collectors.toList());

        TransactionTemplate transactionTemplate = SpringContextUtil.getBean(TransactionTemplate.class);
        transactionTemplate.executeWithoutResult(tx -> {
            if (CollectionUtils.isNotEmpty(toRemove)) {
                repository.deleteAllInBatch(toRemove);
                log.info("remove notification policy metadata successfully, total removed count={}",
                        toRemove.size());
            }

            if (CollectionUtils.isNotEmpty(toAdd)) {
                repository.saveAll(toAdd);
                log.info("add new notification policy metadata successfully, total added count={}",
                        toAdd.size());
            }

        });
    }

}
