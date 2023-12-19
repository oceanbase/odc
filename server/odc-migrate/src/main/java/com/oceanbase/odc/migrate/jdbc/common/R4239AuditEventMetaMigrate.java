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
import com.oceanbase.odc.metadb.audit.AuditEventMetaEntity;
import com.oceanbase.odc.metadb.audit.AuditEventMetaRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/2/21 下午4:14
 * @Description: []
 */
@Slf4j
@Migratable(version = "4.2.3.9", description = "Built-in audit event meta migrate",
        repeatable = true, ignoreChecksum = true)
public class R4239AuditEventMetaMigrate implements JdbcMigratable {
    private static final String MIGRATE_CONFIG_FILE = "init-config/default-audit-event-meta.yml";

    @Override
    public void migrate(DataSource dataSource) {
        AuditEventMetaRepository auditEventMetaRepository = SpringContextUtil.getBean(AuditEventMetaRepository.class);

        List<AuditEventMetaEntity> expected = YamlUtils.fromYaml(MIGRATE_CONFIG_FILE,
                new TypeReference<List<AuditEventMetaEntity>>() {});

        List<AuditEventMetaEntity> actual = auditEventMetaRepository.findAll();

        List<AuditEventMetaEntity> toAdd =
                expected.stream().filter(metadata -> !actual.contains(metadata)).collect(Collectors.toList());
        List<AuditEventMetaEntity> toRemove =
                actual.stream().filter(metadata -> !expected.contains(metadata)).collect(Collectors.toList());

        TransactionTemplate transactionTemplate = SpringContextUtil.getBean(TransactionTemplate.class);
        transactionTemplate.executeWithoutResult(tx -> {
            if (CollectionUtils.isNotEmpty(toRemove)) {
                auditEventMetaRepository.deleteInBatch(toRemove);
                log.info("remove audit event meta successfully, total removed audit meta count={}",
                        toRemove.size());
            }

            if (CollectionUtils.isNotEmpty(toAdd)) {
                auditEventMetaRepository.saveAll(toAdd);
                log.info("add new audit event meta successfully, total added audit event meta count={}",
                        toAdd.size());
            }

        });
    }

}
