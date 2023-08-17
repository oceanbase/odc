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
package com.oceanbase.odc.service.integration;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import javax.validation.ConstraintViolationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.metadb.integration.IntegrationRepository;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.Encryption.EncryptionAlgorithm;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.QueryIntegrationParams;

/**
 * @author gaoda.xy
 * @date 2023/3/29 19:42
 */
public class IntegrationServiceTest extends MockedAuthorityTestEnv {
    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, "id");

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private UserService userService;

    @MockBean
    private EncryptionFacade encryptionFacade;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        integrationRepository.deleteAll();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(ADMIN_USER_ID);
        Mockito.when(userService.nullSafeGet(Mockito.anyLong())).thenReturn(new UserEntity());
        Mockito.when(userService.getUsersByFuzzyNameWithoutPermissionCheck(Mockito.anyString()))
                .thenReturn(Collections.singletonList(User.of(ADMIN_USER_ID)));
        grantAllPermissions(ResourceType.ODC_INTEGRATION);

        TextEncryptor mockEncryptor = Mockito.mock(TextEncryptor.class);
        Mockito.when(mockEncryptor.decrypt(Mockito.anyString())).thenReturn("decrypt");
        Mockito.when(mockEncryptor.encrypt(Mockito.anyString())).thenReturn("encrypted-password");
        Mockito.when(encryptionFacade.generateSalt()).thenReturn("salt");
        Mockito.when(encryptionFacade.organizationEncryptor(Mockito.eq(ORGANIZATION_ID), Mockito.eq("salt")))
                .thenReturn(mockEncryptor);
    }

    @After
    public void tearDown() {
        integrationRepository.deleteAll();
    }

    @Test
    public void test_exists_notExist() {
        Assert.assertFalse(integrationService.exists("notExist", IntegrationType.APPROVAL));
        Assert.assertFalse(integrationService.exists("notExist", IntegrationType.SQL_INTERCEPTOR));
    }

    @Test
    public void test_exists_exist() {
        IntegrationConfig config = createApprovalConfig("test_exists", IntegrationType.APPROVAL);
        integrationService.create(config);
        Assert.assertTrue(integrationService.exists("test_exists", IntegrationType.APPROVAL));
        config = createApprovalConfig("test_exists", IntegrationType.SQL_INTERCEPTOR);
        integrationService.create(config);
        Assert.assertTrue(integrationService.exists("test_exists", IntegrationType.SQL_INTERCEPTOR));
    }

    @Test
    public void test_create_success() {
        IntegrationConfig config = createApprovalConfig("test_create", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        Assert.assertNotNull(created);
        config = createApprovalConfig("test_create", IntegrationType.SQL_INTERCEPTOR);
        created = integrationService.create(config);
        Assert.assertNotNull(created);
    }

    @Test
    public void test_create_checkConfigurationYamlFailed() {
        IntegrationConfig config = createApprovalConfig("test_create", IntegrationType.APPROVAL);
        config.setConfiguration("approvalTimeoutSeconds: 3600");
        thrown.expect(ConstraintViolationException.class);
        integrationService.create(config);
        config = createApprovalConfig("test_create", IntegrationType.SQL_INTERCEPTOR);
        config.setConfiguration("approvalTimeoutSeconds: 3600");
        thrown.expect(ConstraintViolationException.class);
        integrationService.create(config);
    }

    @Test
    public void test_create_duplicateName() {
        IntegrationConfig config1 = createApprovalConfig("test_create", IntegrationType.APPROVAL);
        integrationService.create(config1);
        IntegrationConfig config2 = createApprovalConfig("test_create", IntegrationType.SQL_INTERCEPTOR);
        integrationService.create(config2);
        thrown.expect(BadRequestException.class);
        integrationService.create(config1);
        thrown.expect(BadRequestException.class);
        integrationService.create(config2);
    }

    @Test
    public void test_detail_success() {
        IntegrationConfig config = createApprovalConfig("test_detail", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        IntegrationConfig details = integrationService.detail(created.getId());
        Assert.assertEquals("test_detail", details.getName());
        Assert.assertEquals(IntegrationType.APPROVAL, details.getType());
        config = createApprovalConfig("test_detail", IntegrationType.SQL_INTERCEPTOR);
        created = integrationService.create(config);
        details = integrationService.detail(created.getId());
        Assert.assertEquals("test_detail", details.getName());
        Assert.assertEquals(IntegrationType.SQL_INTERCEPTOR, details.getType());
    }

    @Test
    public void test_detail_failed() {
        thrown.expect(NotFoundException.class);
        integrationService.detail(1L);
    }

    @Test
    public void test_list_listAllNoPage() {
        integrationService.create(createApprovalConfig("test_list_1", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_2", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_3", IntegrationType.SQL_INTERCEPTOR));
        Page<IntegrationConfig> lists =
                integrationService.list(QueryIntegrationParams.builder().build(), Pageable.unpaged());
        Assert.assertEquals(3, lists.getContent().size());
    }

    @Test
    public void test_list_listAllWithPage() {
        integrationService.create(createApprovalConfig("test_list_1", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_2", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_3", IntegrationType.SQL_INTERCEPTOR));
        Page<IntegrationConfig> lists =
                integrationService.list(QueryIntegrationParams.builder().build(), DEFAULT_PAGEABLE);
        Assert.assertEquals(3, lists.getContent().size());
        Assert.assertEquals(3, lists.getTotalElements());
        Assert.assertEquals(1, lists.getTotalPages());
    }

    @Test
    public void test_list_listWithNameAndEnabledAndPage() {
        integrationService.create(createApprovalConfig("test_list_1", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_2", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_3", IntegrationType.SQL_INTERCEPTOR));
        Page<IntegrationConfig> lists = integrationService.list(
                QueryIntegrationParams.builder().name("test_list").enabled(true).build(),
                PageRequest.of(0, 2, Direction.DESC, "id"));
        Assert.assertEquals(2, lists.getContent().size());
        Assert.assertEquals(3, lists.getTotalElements());
        Assert.assertEquals(2, lists.getTotalPages());
    }

    @Test
    public void test_list_listWithCreatorName() {
        integrationService.create(createApprovalConfig("test_list_1", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_2", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_3", IntegrationType.SQL_INTERCEPTOR));
        Page<IntegrationConfig> lists = integrationService.list(
                QueryIntegrationParams.builder().creatorName("admin").enabled(true).build(),
                DEFAULT_PAGEABLE);
        Assert.assertEquals(3, lists.getContent().size());
    }

    @Test
    public void test_list_listWithType() {
        integrationService.create(createApprovalConfig("test_list_1", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_2", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_3", IntegrationType.SQL_INTERCEPTOR));
        Page<IntegrationConfig> lists = integrationService.list(
                QueryIntegrationParams.builder().type(IntegrationType.APPROVAL).enabled(true).build(),
                DEFAULT_PAGEABLE);
        Assert.assertEquals(2, lists.getContent().size());
    }

    @Test
    public void test_delete_success() {
        IntegrationConfig created =
                integrationService.create(createApprovalConfig("test_delete", IntegrationType.APPROVAL));
        Assert.assertEquals(1, integrationService
                .list(QueryIntegrationParams.builder().build(), DEFAULT_PAGEABLE).getContent().size());
        integrationService.delete(created.getId());
        Assert.assertEquals(0, integrationService
                .list(QueryIntegrationParams.builder().build(), DEFAULT_PAGEABLE).getContent().size());
    }

    @Test
    public void test_delete_throwUnsupportedException() {
        IntegrationConfig config = createApprovalConfig("test_delete", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        IntegrationEntity entity = integrationRepository.findById(created.getId()).get();
        entity.setBuiltin(true);
        integrationRepository.saveAndFlush(entity);
        thrown.expect(UnsupportedException.class);
        integrationService.delete(created.getId());
    }

    @Test
    public void test_update_success() {
        IntegrationConfig config = createApprovalConfig("test_update_before", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        Assert.assertEquals(config.getName(), created.getName());
        config.setName("test_update_after");
        IntegrationConfig updated = integrationService.update(created.getId(), config);
        Assert.assertEquals(config.getName(), updated.getName());
        config = createApprovalConfig("test_update_before", IntegrationType.SQL_INTERCEPTOR);
        created = integrationService.create(config);
        Assert.assertEquals(config.getName(), created.getName());
        config.setName("test_update_after");
        updated = integrationService.update(created.getId(), config);
        Assert.assertEquals(config.getName(), updated.getName());
    }

    @Test
    public void test_update_throwUnsupportedException() {
        IntegrationConfig config = createApprovalConfig("test_update_before", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        IntegrationEntity entity = integrationRepository.findById(created.getId()).get();
        entity.setBuiltin(true);
        integrationRepository.saveAndFlush(entity);
        config.setName("test_update_after");
        thrown.expect(UnsupportedException.class);
        integrationService.update(created.getId(), config);
    }

    @Test
    public void test_setEnabled_success() {
        IntegrationConfig config = createApprovalConfig("test_enabled", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        integrationService.setEnabled(created.getId(), false);
        Assert.assertEquals(false, integrationService.detail(created.getId()).getEnabled());
    }

    @Test
    public void test_setEnabled_throwUnsupportedException() {
        IntegrationConfig config = createApprovalConfig("test_enabled", IntegrationType.APPROVAL);
        IntegrationConfig created = integrationService.create(config);
        IntegrationEntity entity = integrationRepository.findById(created.getId()).get();
        entity.setBuiltin(true);
        integrationRepository.saveAndFlush(entity);
        thrown.expect(UnsupportedException.class);
        integrationService.setEnabled(created.getId(), false);
    }

    @Test
    public void test_listByTypeAndEnabled_success() {
        integrationService.create(createApprovalConfig("test_list_1", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_2", IntegrationType.APPROVAL));
        integrationService.create(createApprovalConfig("test_list_3", IntegrationType.SQL_INTERCEPTOR));
        Assert.assertEquals(2, integrationService.listByTypeAndEnabled(IntegrationType.APPROVAL, true).size());
        Assert.assertEquals(1, integrationService.listByTypeAndEnabled(IntegrationType.SQL_INTERCEPTOR, true).size());
    }

    private IntegrationConfig createApprovalConfig(String name, IntegrationType type) {
        IntegrationConfig returnValue = new IntegrationConfig();
        returnValue.setName(name);
        returnValue.setType(type);
        returnValue.setEnabled(true);
        String testFilepath = "";
        if (type == IntegrationType.APPROVAL) {
            testFilepath = "src/test/resources/integration/approval_integration_template.yaml";
        } else if (type == IntegrationType.SQL_INTERCEPTOR) {
            testFilepath = "src/test/resources/integration/sql_interceptor_integration_template.yaml";
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(testFilepath)))) {
            returnValue.setConfiguration(IOUtils.toString(in, String.valueOf(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Encryption encryption = Encryption.builder()
                .enabled(true)
                .algorithm(EncryptionAlgorithm.AES256_BASE64)
                .secret(RandomStringUtils.randomAlphanumeric(256)).build();
        returnValue.setEncryption(encryption);
        return returnValue;
    }
}
