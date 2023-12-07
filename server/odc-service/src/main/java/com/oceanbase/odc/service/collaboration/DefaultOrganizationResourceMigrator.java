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
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.UserOrganizationRepository;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.VerticalPermissionValidator;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/30 10:53
 * @Description: []
 */
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValues = {"local", "alipay"})
@Service("organizationResourceMigrator")
@Slf4j
@SkipAuthorize
public class DefaultOrganizationResourceMigrator implements OrganizationResourceMigrator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Autowired
    private IndividualOrganizationMigrator individualOrganizationMigrator;

    @Autowired
    private TeamOrganizationMigrator teamOrganizationMigrator;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private VerticalPermissionValidator verticalPermissionValidator;

    @Autowired
    private UserOrganizationMigrator userOrganizationMigrator;

    @Override
    public void migrate(@NonNull User user) {
        if (Objects.isNull(user)) {
            return;
        }
        this.transactionTemplate.execute((transactionStatus -> {
            try {
                boolean exists = userOrganizationRepository.existsByOrganizationId(user.getOrganizationId());
                if (!exists) {
                    log.info(
                            "First user login to team organization, start to initialize resource, organizationId={}, userId={}",
                            user.getOrganizationId(), user.getId());
                    teamOrganizationMigrator.migrate(user);
                    log.info("Initialized team organization resource successfully, organizationId={}, userId={}",
                            user.getOrganizationId(), user.getId());
                }
                userOrganizationMigrator.migrate(user);
                return null;
            } catch (Exception ex) {
                transactionStatus.setRollbackOnly();
                throw new IllegalStateException(ex);
            }
        }));
        this.transactionTemplate.execute((transactionStatus -> {
            try {
                Organization team = organizationService
                        .getByOrganizationTypeAndUserId(OrganizationType.TEAM, user.getId())
                        .orElseThrow(() -> new RuntimeException("User doesn't belong to any TEAM organization"));
                Organization individual = new Organization();
                individual.setId(-1L);
                individual.setType(OrganizationType.INDIVIDUAL);
                boolean hasIndividualPermission =
                        verticalPermissionValidator.implies(individual, Arrays.asList("read", "update"), team.getId());
                Optional<Organization> organization = organizationService.getByOrganizationTypeAndUserId(
                        OrganizationType.INDIVIDUAL, user.getId());
                if (!organization.isPresent() && hasIndividualPermission) {
                    log.info("Individual organization not found, start to initialize for userId={}", user.getId());
                    OrganizationEntity saved = individualOrganizationMigrator.migrate(user);
                    log.info("Initialized individual organization successfully for userId={}", user.getId());
                }
                return null;
            } catch (Exception ex) {
                transactionStatus.setRollbackOnly();
                throw new IllegalStateException(ex);
            }
        }));
    }
}
