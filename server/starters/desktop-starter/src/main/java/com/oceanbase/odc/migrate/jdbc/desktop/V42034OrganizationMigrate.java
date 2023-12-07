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
package com.oceanbase.odc.migrate.jdbc.desktop;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.migrate.resource.ResourceInitializer;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.IgnoreResourceIdHandle;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/7/19 22:36
 * @Description: []
 */
@Slf4j
@Migratable(version = "4.2.0.34", description = "client mode data migration")
public class V42034OrganizationMigrate implements JdbcMigratable {
    private final Long DEFAULT_USER_ID = 0L;
    private final Long SYSTEM_ADMIN_ROLE_ID = 1L;
    private final String DEFAULT_ENV_NAME =
            "${com.oceanbase.odc.builtin-resource.collaboration.environment.default.name}";


    @Override
    public void migrate(DataSource dataSource) {
        ResourceInitializer initializer = new ResourceInitializer(
                getResourceConfig(dataSource));
        try {
            migrateOrganization(dataSource);
            initializer.init();
            migrateUserRole(dataSource);
            migrateDataSource(dataSource);
        } catch (Exception e) {
            throw new RuntimeException("init organization resource failed, ", e);
        }
    }

    private ResourceConfig getResourceConfig(DataSource dataSource) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, 1L);
        parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, 0L);
        List<String> resourceLocations = new LinkedList<>();
        resourceLocations.add("migrate/rbac/V_3_2_0_5__iam_role.yaml");
        resourceLocations.add("migrate/common/V_4_2_0_24__resource_role.yaml");
        resourceLocations.add("runtime");

        return ResourceConfig.builder()
                .dataSource(dataSource)
                .valueEncoderFactory(new DefaultValueEncoderFactory())
                .valueGeneratorFactory(new DefaultValueGeneratorFactory())
                .variables(parameters)
                .handle(new IgnoreResourceIdHandle())
                .resourceLocations(resourceLocations).build();
    }

    private void migrateOrganization(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String selectUserPassword = "select `password` from iam_user where id = 0";
        String userPassword = jdbcTemplate.queryForObject(selectUserPassword, String.class);

        String updateOrganization =
                "update `iam_organization` set `secret` = ?, `type`='INDIVIDUAL', `display_name` = '${com.oceanbase.odc.builtin-resource.iam.organization.individual.display-name}', `description` = '${com.oceanbase.odc.builtin-resource.iam.organization.individual.description}';";
        int affectRows = jdbcTemplate.update(updateOrganization, new Object[] {userPassword});
        log.info("update organization successfully, affectRows={}", affectRows);

        String insertIamUserOrganization =
                "insert into `iam_user_organization`(`id`, `user_id`, `organization_id`) values(1, 0, 1) on duplicate key update `id`=`id`;"
                        + "insert into `iam_user_organization`(`id`, `user_id`, `organization_id`) values(2, 1, 1) on duplicate key update `id`=`id`;";
        affectRows = jdbcTemplate.update(insertIamUserOrganization);
        log.info("update iam_user_organization successfully, affectRows={}", affectRows);
    }

    private void migrateDataSource(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String updateConnection =
                "update connect_connection set owner_id = ?, organization_id = ?, visible_scope = 'ORGANIZATION', environment_id = (select id from collaboration_environment where organization_id = ? and name = ?)";
        int affectRows =
                jdbcTemplate.update(updateConnection,
                        new Object[] {1L, 1L, 1L, DEFAULT_ENV_NAME});
        log.info(
                "migrate private connection to individual organization successfully, affectRows={} ", affectRows);
    }

    private void migrateUserRole(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String updateUserRole =
                "insert into iam_user_role(user_id,role_id,creator_id,organization_id) values(?,?,?,?) on duplicate key update `id`=`id`;";
        int affectRows =
                jdbcTemplate.update(updateUserRole, new Object[] {DEFAULT_USER_ID, SYSTEM_ADMIN_ROLE_ID, 1L, 1L});
        log.info("bind role to user successfully, userId={}, roleId={}, affectRows={}", DEFAULT_USER_ID,
                SYSTEM_ADMIN_ROLE_ID, affectRows);
    }
}
