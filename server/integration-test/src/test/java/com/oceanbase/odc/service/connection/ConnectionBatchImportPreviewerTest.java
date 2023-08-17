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
package com.oceanbase.odc.service.connection;

import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.shared.constant.FieldName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.model.ConnectionPreviewBatchImportResp;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;

public class ConnectionBatchImportPreviewerTest extends MockedAuthorityTestEnv {

    @Autowired
    private ConnectionBatchImportPreviewer connectionBatchImportPreviewer;
    @Autowired
    public ConnectionConfigRepository connectionRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private EnvironmentService environmentService;

    private static final Long CREATOR_ID = 1L;
    private static final Long ORGANIZATION_ID = 1L;
    private static final String fileName = "test.xlsx";
    private static final String filePath_Simplified_Chinese =
            "src/test/resources/batchImport/connection/connection_template_simplified_chinese.xlsx";
    private static final String filePath_Traditional_Chinese =
            "src/test/resources/batchImport/connection/connection_template_traditional_chinese.xlsx";
    private static final String filePath_US = "src/test/resources/batchImport/connection/connection_template_us.xlsx";
    private static final MultipartFile multipartFile_Simplified_Chinese;
    private static final MultipartFile multipartFile_Traditional_Chinese;
    private static final MultipartFile multipartFile_US;

    static {
        try {
            multipartFile_Simplified_Chinese =
                    new MockMultipartFile(fileName, fileName, null, new FileInputStream(filePath_Simplified_Chinese));
            multipartFile_Traditional_Chinese =
                    new MockMultipartFile(fileName, fileName, null, new FileInputStream(filePath_Traditional_Chinese));
            multipartFile_US = new MockMultipartFile(fileName, fileName, null, new FileInputStream(filePath_US));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        when(authenticationFacade.currentUser()).thenReturn(User.of(CREATOR_ID));
        when(authenticationFacade.currentUserId()).thenReturn(CREATOR_ID);
        when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(environmentService.list(Mockito.anyLong())).thenReturn(initEnvironments());
        connectionRepository.deleteAll();
        grantAllPermissions(ResourceType.ODC_CONNECTION, ResourceType.ODC_RESOURCE_GROUP, ResourceType.ODC_USER,
                ResourceType.ODC_PRIVATE_CONNECTION);
    }

    @After
    public void tearDown() {
        connectionRepository.deleteAll();
        DefaultLoginSecurityManager.removeSecurityContext();
        DefaultLoginSecurityManager.removeContext();
    }

    @Test
    public void success_Simplified_Chinese_PreviewBatchImportConnection() throws IOException {
        ConnectionPreviewBatchImportResp req;
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        req = connectionBatchImportPreviewer.preview(multipartFile_Simplified_Chinese);
        Assert.assertEquals(8, req.getBatchImportConnectionList().size());
    }

    @Test
    public void success_Traditional_Chinese_PreviewBatchImportConnection() throws IOException {
        ConnectionPreviewBatchImportResp req;
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        req = connectionBatchImportPreviewer.preview(multipartFile_Traditional_Chinese);
        Assert.assertEquals(4, req.getBatchImportConnectionList().size());
    }

    @Test
    public void success_US_PreviewBatchImportConnection() throws IOException {
        ConnectionPreviewBatchImportResp req;
        LocaleContextHolder.setLocale(Locale.US);
        req = connectionBatchImportPreviewer.preview(multipartFile_US);
        Assert.assertEquals(4, req.getBatchImportConnectionList().size());
    }

    private ProjectEntity getProjectEntity() {
        ProjectEntity project = new ProjectEntity();
        project.setId(1L);
        project.setBuiltin(false);
        project.setArchived(false);
        project.setOrganizationId(1L);
        project.setLastModifierId(1L);
        project.setCreatorId(1L);
        return project;
    }

    private List<Environment> initEnvironments() {
        return Arrays.asList(
                createEnvironment(FieldName.DATASOURCE_ENVIRONMENT_DEFAULT.getLocalizedMessage()),
                createEnvironment(FieldName.DATASOURCE_ENVIRONMENT_DEV.getLocalizedMessage()),
                createEnvironment(FieldName.DATASOURCE_ENVIRONMENT_PROD.getLocalizedMessage()),
                createEnvironment(FieldName.DATASOURCE_ENVIRONMENT_SIT.getLocalizedMessage()));
    }

    private Environment createEnvironment(String name) {
        Environment environment = new Environment();
        environment.setName(name);
        return environment;
    }

}
