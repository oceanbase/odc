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
package com.oceanbase.odc.service.iam;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserOrganizationEntity;
import com.oceanbase.odc.metadb.iam.UserOrganizationRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : OrganizationService.java, v 0.1 2021-08-02 22:14
 */
@Slf4j
@Service
@Validated
@SkipAuthorize("odc internal usage")
public class OrganizationService {
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private VerticalPermissionValidator verticalPermissionValidator;

    private final OrganizationMapper organizationMapper = OrganizationMapper.INSTANCE;

    @SkipAuthorize("internal authentication")
    public List<Organization> listCurrentUserOrganizations() {
        List<Organization> organizations = listOrganizationsByUserId(authenticationFacade.currentUserId());
        if (SpringContextUtil.isActive("clientMode")) {
            return organizations;
        }
        Organization team = organizations.stream()
                .filter(organization -> organization.getType() == OrganizationType.TEAM)
                .findFirst().orElseThrow(() -> new RuntimeException("User doesn't belong to any TEAM organization"));
        return organizations.stream().filter(o -> {
            if (o.getType() == OrganizationType.TEAM) {
                return true;
            }
            return verticalPermissionValidator.implies(o, Arrays.asList("read", "update"), team.getId());
        }).collect(Collectors.toList());
    }

    @SkipAuthorize("internal authentication")
    public List<Organization> listOrganizationsByUserId(Long userId) {
        return organizationRepository.findByUserId(userId).stream()
                .map(organizationMapper::entityToModel).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Organization createIfNotExists(@NotBlank String identifier, @NotBlank String name) {
        return getByIdentifier(identifier).orElseGet(() -> {
            Organization organization = new Organization();
            organization.setName(name);
            organization.setUniqueIdentifier(identifier);
            organization.setDescription("Auto created by identifier " + identifier);
            organization.setBuiltin(false);
            organization.setType(OrganizationType.TEAM);
            return create(organization);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Organization create(@NotNull @Valid Organization organization) {
        organization.setId(null);
        organization.setCreateTime(null);
        organization.setUpdateTime(null);
        organization.setType(OrganizationType.TEAM);

        if (StringUtils.isEmpty(organization.getUniqueIdentifier())) {
            String uuid = StringUtils.uuid();
            organization.setUniqueIdentifier(uuid);
            log.info("uniqueIdentifier not given, generated uuid, uuid={}", uuid);
        }
        organization.setSecret(PasswordUtils.random(32));

        OrganizationEntity entity = organization.toEntity();
        OrganizationEntity saved = organizationRepository.saveAndFlush(entity);
        Organization created = Organization.ofEntity(saved);

        log.info("Organization created, organization={}", created);
        return created;
    }

    public boolean exists(OrganizationType type, Long userId) {
        Integer exists = organizationRepository.existsByTypeAndUserId(type, userId);
        return Objects.nonNull(exists);
    }

    public Optional<Organization> getByOrganizationTypeAndUserId(OrganizationType type, Long userId) {
        List<OrganizationEntity> organizationEntities = organizationRepository.findByTypeAndUserId(type, userId);
        if (organizationEntities.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(Organization.ofEntity(organizationEntities.get(0)));
    }

    @Transactional(rollbackFor = Exception.class)
    public OrganizationEntity createIndividualOrganization(User user) {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setCreatorId(user.getId());
        entity.setName("ODC_DEFAULT_INDIVIDUAL_ORGANIZATION_" + user.getId());
        entity.setDisplayName("${com.oceanbase.odc.builtin-resource.iam.organization.individual.display-name}");
        entity.setDescription("${com.oceanbase.odc.builtin-resource.iam.organization.individual.description}");
        entity.setBuiltIn(true);
        entity.setType(OrganizationType.INDIVIDUAL);
        entity.setUniqueIdentifier(StringUtils.uuid());
        entity.setSecret(user.getPassword());
        entity.setCreatorId(user.getId());
        OrganizationEntity saved = organizationRepository.saveAndFlush(entity);

        UserOrganizationEntity userOrganizationEntity = new UserOrganizationEntity();
        userOrganizationEntity.setOrganizationId(saved.getId());
        userOrganizationEntity.setUserId(user.getId());
        userOrganizationRepository.saveAndFlush(userOrganizationEntity);
        return saved;
    }

    public Optional<Organization> getByIdentifier(@NotNull String identifier) {
        return organizationRepository.findByUniqueIdentifier(identifier).map(Organization::ofEntity);
    }

    public Optional<Organization> get(@NotNull Long id) {
        return organizationRepository.findById(id).map(Organization::ofEntity);
    }
}
