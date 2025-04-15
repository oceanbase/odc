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

import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "4.3.4.9", description = "migrate the organization secret to this stored in confusion")
public class V4349OrganizationSecretMigrate implements JdbcMigratable {

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        List<OrganizationEntity> organizationList = listOrganizations();
        if (CollectionUtils.isEmpty(organizationList)) {
            log.info("organization secret migrate skipped, organization list is empty");
            return;
        }
        log.info("organization secret migrate started, organizationCount={}", organizationList.size());
        int total = 0;
        for (OrganizationEntity organization : organizationList) {
            organization.setSecret(Base64.getEncoder().encodeToString(organization.getSecret().getBytes()));
            total += migrateForOrganization(organization);
        }
        log.info("organization secret migrated, organizationCount={}, total={}", organizationList.size(), total);
    }

    private List<OrganizationEntity> listOrganizations() {
        String sql = "select `id`, `secret` from iam_organization";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(OrganizationEntity.class));
    }

    private int migrateForOrganization(OrganizationEntity organization) {
        String sql = "update iam_organization set secret=? where id=?";
        AtomicInteger row = new AtomicInteger();
        transactionTemplate.execute(status -> {
            try {
                row.set(jdbcTemplate.update(sql, organization.getSecret(),
                        organization.getId()));
            } catch (Exception e) {
                log.error("organization secret migrate failed, organizationId={}, error={}", organization.getId(),
                        e.getMessage());
                status.setRollbackOnly();
            }
            return row;
        });
        return row.get();
    }
}
