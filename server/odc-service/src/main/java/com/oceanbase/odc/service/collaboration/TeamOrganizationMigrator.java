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
package com.oceanbase.odc.service.collaboration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.migrate.resource.ResourceInitializer;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserOrganizationRepository;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.IgnoreResourceIdHandle;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/30 13:56
 * @Description: []
 */
@Slf4j
@Service
@Validated
@SkipAuthorize
public class TeamOrganizationMigrator {
    private final String DEFAULT_ENV_NAME =
            "${com.oceanbase.odc.builtin-resource.collaboration.environment.default.name}";

    private final String PRIVATE_CONNECTION_ROLE = "private_connection";

    private final String APPLY_CONNECTION_ROLE = "apply_connection";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Transactional(rollbackFor = Exception.class)
    public void migrate(@NonNull User user) {
        /**
         * init resources for this TEAM organization
         */
        ResourceInitializer initializer = new ResourceInitializer(
                getResourceConfig(dataSource, user.getId(), user.getOrganizationId()));
        try {
            initializer.init();
            migrateDataSource(user.getOrganizationId());
            migrateRole(user.getOrganizationId());
        } catch (Exception e) {
            throw new RuntimeException("init organization resource failed, organizationId=" + user.getOrganizationId(),
                    e);
        }
        log.info("create TEAM organization and resources for user succeed, userId={}, organizationId={}",
                user.getId(), user.getOrganizationId());
    }

    private ResourceConfig getResourceConfig(DataSource dataSource, Long userId, Long organizationId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, organizationId);
        parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, userId);
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

    private void migrateDataSource(Long organizationId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Long defaultEnvId = jdbcTemplate.queryForObject(
                "select id from collaboration_environment where organization_id = ? and name = ?",
                new Object[] {organizationId, DEFAULT_ENV_NAME}, Long.class);
        int affectRows =
                jdbcTemplate.update(
                        "update connect_connection set environment_id = ? where visible_scope = 'ORGANIZATION' and organization_id = ? and environment_id = -1",
                        new Object[] {defaultEnvId, organizationId});
        log.info("migrate public connection to team organization successfully, organizationId={}, affectRows={}",
                organizationId, affectRows);
    }

    private void migrateRole(Long organizationId) {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("names", Arrays.asList(PRIVATE_CONNECTION_ROLE, APPLY_CONNECTION_ROLE));
        parameters.addValue("organizationId", organizationId);
        int affectRows = jdbcTemplate.update(
                "update iam_role set is_enabled = 0, description = 'This role was deprecated since V4.2.0' where organization_id=:organizationId and name in(:names)",
                parameters);
        log.info("migrate role successfully, organizationId={}, affectRows={}", organizationId, affectRows);
    }
}
