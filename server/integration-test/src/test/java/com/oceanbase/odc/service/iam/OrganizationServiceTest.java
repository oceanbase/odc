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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.iam.model.Organization;

import cn.hutool.core.codec.Caesar;

public class OrganizationServiceTest extends ServiceTestEnv {
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationRepository organizationRepository;

    @Before
    public void setUp() throws Exception {
        organizationRepository.deleteAll();
        Organization created = organizationService.createIfNotExists("company-unique-id-1000", "CompanyOB");
        organizationRepository.saveAndFlush(created.toEntity());
    }

    @Test
    public void createIfNotExists_NotExists_ReturnNotNull() {
        Organization created = organizationService.createIfNotExists("123", "name123");
        Assert.assertNotNull(created);
    }

    @Test
    public void createIfNotExists_Exists_ReturnExists() {
        Organization created = organizationService.createIfNotExists("123", "name123");

        Organization created2rd = organizationService.createIfNotExists("123", "name123");

        Assert.assertEquals(created.getId(), created2rd.getId());
    }

    @Test
    public void create_NoUniqueIdentifier_GenerateUUID() {
        Organization organization = newOrganization();
        organization.setUniqueIdentifier(null);

        Organization created = organizationService.create(organization);

        String regex = "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
        Pattern uuidPattern = Pattern.compile(regex);

        Assert.assertTrue(uuidPattern.matcher(created.getUniqueIdentifier()).matches());
    }

    @Test
    public void get_NotExists_IsPresentFalse() {
        Optional<Organization> get = organizationService.get(-1L);

        Assert.assertFalse(get.isPresent());
    }

    @Test
    public void get_Exists_IsPresentTrue() {
        Organization organization = newOrganization();
        Organization created = organizationService.create(organization);

        Optional<Organization> get = organizationService.get(created.getId());

        Assert.assertTrue(get.isPresent());
    }

    @Test
    public void getByIdentifier_NotExists_IsPresentFalse() {
        Optional<Organization> get = organizationService.getByIdentifier("identifier-not-exists");

        Assert.assertFalse(get.isPresent());
    }

    @Test
    public void getByIdentifier_Exists_IsPresentTrue() {
        Organization organization = newOrganization();
        organizationService.create(organization);

        Optional<Organization> get = organizationService.getByIdentifier("company-unique-id-1");

        Assert.assertTrue(get.isPresent());
    }

    @Test
    public void isOrganizationSecretMigrated_IsMigratedTrue() {
        Organization organization = newOrganization();
        Organization org = organizationService.create(organization);
        Assert.assertTrue(Objects.nonNull(org.getSecret()));
        Assert.assertTrue(Objects.isNull(org.getCustomSecret()));
        Assert.assertTrue(organizationService.isOrganizationSecretMigrated(org.getId()));
    }

    @Test
    public void isOrganizationSecretMigrated_IsMigratedTrue2() {
        Organization organization = newOrganization();
        Organization org = organizationService.create(organization);
        org.setCustomSecret(Caesar.decode(org.getSecret(), 8));
        organizationRepository.saveAndFlush(org.toEntity());
        Assert.assertEquals(org.getSecret(), Caesar.encode(org.getCustomSecret(), 8));
        Assert.assertTrue(organizationService.isOrganizationSecretMigrated(org.getId()));
    }

    private Organization newOrganization() {
        Organization organization = new Organization();
        organization.setName("CompanyA");
        organization.setUniqueIdentifier("company-unique-id-1");
        organization.setBuiltin(false);
        return organization;
    }

}
