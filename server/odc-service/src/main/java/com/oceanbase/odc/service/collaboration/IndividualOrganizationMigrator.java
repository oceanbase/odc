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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.migrate.resource.ResourceInitializer;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.IgnoreResourceIdHandle;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/30 12:07
 * @Description: []
 */
@Slf4j
@Service
@Validated
@SkipAuthorize
public class IndividualOrganizationMigrator {
    private final String DEFAULT_ENV_NAME =
            "${com.oceanbase.odc.builtin-resource.collaboration.environment.default.name}";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;


    @Transactional(rollbackFor = Exception.class)
    public OrganizationEntity migrate(@NonNull User user) {
        OrganizationEntity savedOrganization = organizationService.createIndividualOrganization(user);
        /**
         * init resources for this INDIVIDUAL organization
         */
        ResourceInitializer initializer = new ResourceInitializer(
                getResourceConfig(dataSource, user.getId(), savedOrganization.getId()));
        try {
            initializer.init();
            migrateDataSource(savedOrganization.getId(), user.getId());
        } catch (Exception e) {
            throw new RuntimeException("init organization resource failed, organizationId=" + savedOrganization.getId(),
                    e);
        }
        log.info("create INDIVIDUAL organization and resources for user succeed, userId={}, organizationId={}",
                user.getId(), savedOrganization.getId());


        return savedOrganization;
    }

    private ResourceConfig getResourceConfig(DataSource dataSource, Long userId, Long organizationId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, organizationId);
        parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, userId);
        List<String> resourceLocations = new LinkedList<>();
        resourceLocations.add("migrate/common/V_3_2_0_6__iam_permission.yaml");
        resourceLocations.add("migrate/common/V_3_3_0_4__iam_permission.yaml");
        resourceLocations.add("migrate/common/V_3_4_0_2__iam_permission.yaml");
        resourceLocations.add("migrate/common/V_3_4_0_13__data_masking_rule.yaml");
        resourceLocations.add("migrate/common/V_3_4_0_14__data_masking_rule_segment.yaml");
        resourceLocations.add("migrate/common/V_4_1_0_7__automation_event_metadata.yaml");
        resourceLocations.add("migrate/common/V_4_1_0_14__iam_permission.yaml");
        resourceLocations.add("migrate/common/V_4_2_0_24__resource_role.yaml");

        resourceLocations.add("migrate/rbac");
        resourceLocations.add("runtime");

        resourceLocations.add("init-config/runtime/iam_user_system_admin.yaml");

        return ResourceConfig.builder()
                .dataSource(dataSource)
                .valueEncoderFactory(new DefaultValueEncoderFactory())
                .valueGeneratorFactory(new DefaultValueGeneratorFactory())
                .variables(parameters)
                .handle(new IgnoreResourceIdHandle())
                .resourceLocations(resourceLocations).build();
    }

    private void migrateDataSource(Long organizationId, Long userId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Long defaultEnvId = jdbcTemplate.queryForObject(
                "select id from collaboration_environment where organization_id = ? and name = ?",
                new Object[] {organizationId, DEFAULT_ENV_NAME}, Long.class);
        int affectRows =
                jdbcTemplate.update(
                        "update connect_connection set environment_id = ?, organization_id = ?, visible_scope = 'ORGANIZATION' where visible_scope = 'PRIVATE' and creator_id = ? and environment_id = -1",
                        new Object[] {defaultEnvId, organizationId, userId});
        log.info(
                "migrate private connection to individual organization successfully, organizationId={}, userId={}, affectRows={}",
                organizationId, userId, affectRows);
    }
}
