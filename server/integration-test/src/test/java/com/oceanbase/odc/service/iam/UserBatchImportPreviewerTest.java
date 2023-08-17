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

import java.io.FileInputStream;
import java.io.IOException;
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
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.UserPreviewBatchImportResp;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

public class UserBatchImportPreviewerTest extends MockedAuthorityTestEnv {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserBatchImportPreviewer userBatchImportPreviewer;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    String fileName = "test.xlsx";
    String originalFilename = "test.xlsx";
    String filePath_Simplified_Chinese = "src/test/resources/batchImport/user/user_template_simplified_chinese.xlsx";
    MultipartFile multipartFile_Simplified_Chinese =
            new MockMultipartFile(fileName, originalFilename, null, new FileInputStream(filePath_Simplified_Chinese));
    String filePath_Traditional_Chinese = "src/test/resources/batchImport/user/user_template_traditional_chinese.xlsx";
    MultipartFile multipartFile_Traditional_Chinese =
            new MockMultipartFile(fileName, originalFilename, null, new FileInputStream(filePath_Traditional_Chinese));
    String filePath_US = "src/test/resources/batchImport/user/user_template_us.xlsx";
    MultipartFile multipartFile_US =
            new MockMultipartFile(fileName, originalFilename, null, new FileInputStream(filePath_US));

    public UserBatchImportPreviewerTest() throws IOException {}

    @Before
    public void setUp() {
        userRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
    }

    @After
    public void tearDown() {
        SecurityContextUtils.clear();
    }

    @Test
    public void success_Simplified_Chinese_PreviewBatchImportUser() throws IOException {
        UserPreviewBatchImportResp req;
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        req = userBatchImportPreviewer.preview(multipartFile_Simplified_Chinese);
        Assert.assertEquals(2, req.getBatchImportUserList().size());
    }

    @Test
    public void success_Traditional_Chinese_PreviewBatchImportUser() throws IOException {
        UserPreviewBatchImportResp req;
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        req = userBatchImportPreviewer.preview(multipartFile_Traditional_Chinese);
        Assert.assertEquals(2, req.getBatchImportUserList().size());
    }

    @Test
    public void success_US_PreviewBatchImportUser() throws IOException {
        UserPreviewBatchImportResp req;
        LocaleContextHolder.setLocale(Locale.US);
        req = userBatchImportPreviewer.preview(multipartFile_US);
        Assert.assertEquals(2, req.getBatchImportUserList().size());
    }
}
