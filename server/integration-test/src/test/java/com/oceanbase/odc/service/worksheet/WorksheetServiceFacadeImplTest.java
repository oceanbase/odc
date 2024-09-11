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
package com.oceanbase.odc.service.worksheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.server.OdcServer;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.CloudObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq.UploadWorksheetTuple;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.ListWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.UpdateWorksheetReq;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
import com.oceanbase.odc.service.worksheet.service.DefaultWorksheetService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OdcServer.class)
@EnableTransactionManagement
@AutoConfigureMockMvc
@Ignore
public class WorksheetServiceFacadeImplTest {
    @Autowired
    WorksheetServiceFacadeImpl worksheetServiceFacade;
    @Autowired
    DefaultWorksheetService defaultWorksheetService;
    @MockBean
    AuthenticationFacade authenticationFacade;
    ObjectMapper objectMapper = new ObjectMapper();
    public static final String TEST_FILE_PATH = "src/test/resources/data/test0001.txt";
    public static final String TEST_FILE_PATH2 = "src/test/resources/data/中文名称.txt";
    public static final String TEST_DOWNLOAD_FILE = CloudObjectStorageConstants.TEMP_DIR + "/download/temp";
    Long projectId;
    CloudObjectStorageClient cloudObjectStorageClient;
    File tempFile;
    List<String> objectNames;

    @Before
    public void setUp() {
        projectId = System.currentTimeMillis();
        objectNames = new ArrayList<>();

        ObjectStorageConfiguration configuration = ITConfigurations.getOssConfiguration();
        CloudClient cloudClient = new CloudResourceConfigurations().publicEndpointCloudClient(() -> configuration);
        CloudClient internalCloudClient =
                new CloudResourceConfigurations().internalEndpointCloudClient(() -> configuration);
        cloudObjectStorageClient = new CloudObjectStorageClient(cloudClient, internalCloudClient, configuration);
        setFieldValue(worksheetServiceFacade, "objectStorageClient", cloudObjectStorageClient);
        setFieldValue(defaultWorksheetService, "objectStorageClient", cloudObjectStorageClient);

        when(authenticationFacade.currentUserId()).thenReturn(1L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        setFieldValue(defaultWorksheetService, "authenticationFacade", authenticationFacade);
    }

    @After
    public void clear() {
        OdcFileUtil.deleteFiles(new File(CloudObjectStorageConstants.TEMP_DIR));
        if (CollectionUtils.isNotEmpty(objectNames)) {
            cloudObjectStorageClient.deleteObjects(objectNames);
        }
    }

    @Test
    public void generateUploadUrl_Normal() throws Exception {
        GenerateWorksheetUploadUrlReq req = new GenerateWorksheetUploadUrlReq();
        req.setPath("/Worksheets/ods_user.sql");
        GenerateWorksheetUploadUrlResp resp = worksheetServiceFacade.generateUploadUrl(projectId, req);
        assertNotNull(resp.getUploadUrl());
    }

    @Test
    public void createWorksheet_Folder() throws Exception {
        String path = "/Worksheets/folder1/";
        WorksheetMetaResp resp = worksheetServiceFacade.createWorksheet(projectId, path, null, null);
        assertEquals(path, resp.getPath());
    }

    @Test
    public void createWorksheet_FileWithContentEmpty() throws Exception {
        String path = "/Worksheets/file.sql";
        WorksheetMetaResp resp = worksheetServiceFacade.createWorksheet(projectId, path, null, null);
        assertEquals(path, resp.getPath());
    }

    @Test
    public void createWorksheet_File() throws Exception {
        String path = "/Worksheets/file.sql";
        WorksheetMetaResp resp =
                worksheetServiceFacade.createWorksheet(projectId, path, UUID.randomUUID().toString(), 1L);
        assertEquals(path, resp.getPath());
    }

    @Test
    public void getWorksheetDetail() throws Exception {
        GenerateWorksheetUploadUrlReq req = new GenerateWorksheetUploadUrlReq();
        req.setPath("/Worksheets/ods_user.sql");
        GenerateWorksheetUploadUrlResp uploadUrlResp = worksheetServiceFacade.generateUploadUrl(projectId, req);
        objectNames.add(uploadUrlResp.getObjectId());
        File uploadFile = new File(TEST_FILE_PATH);
        uploadByPreSignedUrl(uploadUrlResp.getUploadUrl(), uploadFile);
        worksheetServiceFacade.createWorksheet(projectId, "/Worksheets/a.sql", uploadUrlResp.getObjectId(),
                uploadFile.length());
        WorksheetResp resp = worksheetServiceFacade.getWorksheetDetail(projectId, "/Worksheets/a.sql");
        tempFile = createFileWithParent(TEST_DOWNLOAD_FILE);
        downloadFromUrlToFile(new URL(resp.getContentDownloadUrl()), tempFile);
        assertEquals("test0001", readFirstLine(tempFile));
    }


    @Test
    public void listWorksheets() {
        prepareWorksheetList();

        ListWorksheetsReq req = new ListWorksheetsReq();
        req.setPath("/Worksheets/");
        req.setDepth(1);
        List<WorksheetMetaResp> worksheets = worksheetServiceFacade.listWorksheets(projectId, req);
        assertEquals(Arrays.asList(
                "/Worksheets/folder1/",
                "/Worksheets/folder2/",
                "/Worksheets/a_der.sql",
                "/Worksheets/b.sql"),
                worksheets.stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));

        req.setPath("/Worksheets/");
        req.setDepth(0);
        req.setNameLike("der");
        worksheets = worksheetServiceFacade.listWorksheets(projectId, req);
        assertEquals(Arrays.asList(
                "/Worksheets/folder1/",
                "/Worksheets/folder1/sub1/file_der2.sql",
                "/Worksheets/folder2/",
                "/Worksheets/a_der.sql"),
                worksheets.stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));
    }

    @Test
    public void flatListWorksheets() throws Exception {
        List<String> addPaths = prepareWorksheetList();

        Page<WorksheetMetaResp> worksheetMetaResponses =
                defaultWorksheetService.flatListWorksheets(projectId, PageRequest.of(0, 100));
        assertEquals(worksheetMetaResponses.toList().size(), addPaths.size());
    }

    @Test
    public void batchUploadWorksheets() {
        List<String> addPaths = Arrays.asList("/Worksheets/a.sql", "/Worksheets/b.sql", "/Worksheets/c/");
        BatchOperateWorksheetsResp batchOperateWorksheetsResp =
                worksheetServiceFacade.batchUploadWorksheets(projectId, new BatchUploadWorksheetsReq(
                        addPaths.stream().map(this::newUploadWorksheetTuple).collect(Collectors.toList())));
        assertTrue(batchOperateWorksheetsResp.getAllSuccessful());

        ListWorksheetsReq req = new ListWorksheetsReq();
        req.setPath("/Worksheets/");
        req.setDepth(1);
        List<WorksheetMetaResp> worksheets = worksheetServiceFacade.listWorksheets(projectId, req);
        assertEquals(Arrays.asList(
                "/Worksheets/c/",
                "/Worksheets/a.sql",
                "/Worksheets/b.sql"),
                worksheets.stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));
    }

    @Test
    public void batchDeleteWorksheets() {
        List<String> addPaths = prepareWorksheetList();

        BatchOperateWorksheetsResp response = worksheetServiceFacade.batchDeleteWorksheets(projectId, addPaths);
        assertTrue(response.getAllSuccessful());

        ListWorksheetsReq req = new ListWorksheetsReq();
        req.setPath("/Worksheets/");
        req.setDepth(0);
        List<WorksheetMetaResp> worksheets = worksheetServiceFacade.listWorksheets(projectId, req);
        assertEquals(Collections.emptyList(), worksheets);
    }

    @Test
    public void renameWorksheet() {
        prepareWorksheetList();
        List<String> expectPaths = Arrays.asList(
                "/Worksheets/folder3/",
                "/Worksheets/folder3/sub1/",
                "/Worksheets/folder3/sub1/file_der2.sql",
                "/Worksheets/folder3/sub2/",
                "/Worksheets/folder3/file1.sql");
        String pathStr = "/Worksheets/folder1/";
        String destinationPath = "/Worksheets/folder3/";
        List<WorksheetMetaResp> renamedWorksheets =
                worksheetServiceFacade.renameWorksheet(projectId, pathStr, destinationPath);
        assertEquals(expectPaths.stream().sorted().collect(Collectors.toList()),
                renamedWorksheets.stream().map(WorksheetMetaResp::getPath).sorted().collect(Collectors.toList()));

        ListWorksheetsReq listReq = new ListWorksheetsReq();
        listReq.setPath("/Worksheets/folder3/");
        listReq.setDepth(0);
        List<WorksheetMetaResp> worksheets = worksheetServiceFacade.listWorksheets(projectId, listReq);
        assertEquals(expectPaths,
                worksheets.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toList()));
    }

    @Test
    public void editWorksheet() {
        String path = "/Worksheets/a.sql";
        WorksheetMetaResp worksheet = worksheetServiceFacade.createWorksheet(projectId, path,
                UUID.randomUUID().toString(), 1L);
        assertEquals(worksheet.getPath(), path);

        UpdateWorksheetReq req = new UpdateWorksheetReq();
        req.setObjectId(UUID.randomUUID().toString());
        req.setSize(1L);
        req.setPreviousVersion(0L);
        List<WorksheetMetaResp> response = worksheetServiceFacade.editWorksheet(projectId, path, req);
        assertEquals(Collections.singletonList(path),
                response.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toList()));
    }

    @Test
    public void batchDownloadWorksheets() throws IOException {
        List<String> addDirectoryPaths = Arrays.asList(
                "/Worksheets/user_analysis/",
                "/Worksheets/user_analysis/c/");
        for (String path : addDirectoryPaths) {
            worksheetServiceFacade.createWorksheet(projectId, path, null, null);
        }
        Set<String> addFilePaths = new HashSet<>(Arrays.asList(
                "/Worksheets/user_analysis/ods_user.sql",
                "/Worksheets/user_analysis/c/dwd_user.sql"));
        for (String path : addFilePaths) {
            GenerateWorksheetUploadUrlReq req = new GenerateWorksheetUploadUrlReq();
            req.setPath(path);
            GenerateWorksheetUploadUrlResp resp = worksheetServiceFacade.generateUploadUrl(projectId, req);
            assertNotNull(resp.getUploadUrl());
            objectNames.add(resp.getObjectId());
            File uploadFile = new File(TEST_FILE_PATH2);
            uploadByPreSignedUrl(resp.getUploadUrl(), uploadFile);
            worksheetServiceFacade.createWorksheet(projectId, path, resp.getObjectId(), uploadFile.length());
        }

        String downloadUrl = worksheetServiceFacade.batchDownloadWorksheets(projectId, addFilePaths);
        assertNotNull(downloadUrl);
        System.out.println(downloadUrl);
    }

    private void downloadFromUrlToFile(URL url, File file) throws IOException {
        FileUtils.copyURLToFile(url, file, 3000, 5000);
    }

    private void uploadByPreSignedUrl(String presignedUrl, File file) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut putRequest = new HttpPut(presignedUrl);
        putRequest.setHeader("Content-Type", ContentType.APPLICATION_OCTET_STREAM.getMimeType());
        FileEntity fileEntity = new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM);
        putRequest.setEntity(fileEntity);
        HttpResponse response = httpClient.execute(putRequest);
        httpClient.close();
        assertEquals(response.getCode(), 200);
    }

    private File createFileWithParent(String filePath) throws IOException {
        File file = new File(filePath);
        File parentDirectories = FileUtils.createParentDirectories(file);
        while (!parentDirectories.exists()) {
            parentDirectories = FileUtils.createParentDirectories(parentDirectories);
        }
        return file;
    }

    private String readFirstLine(File file) {
        Verify.notNull(file, "file");
        try {
            List<String> strings = Files.readLines(file, Charsets.UTF_8);
            return strings.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MultiValueMap<String, String> convertToMultiValueMap(ListWorksheetsReq requestData) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();

        if (requestData.getPath() != null) {
            multiValueMap.add("path", requestData.getPath());
        }
        if (requestData.getDepth() != null) {
            multiValueMap.add("depth", requestData.getDepth() + "");
        }
        if (requestData.getNameLike() != null) {
            multiValueMap.add("nameLike", requestData.getNameLike());
        }
        return multiValueMap;
    }

    private void setFieldValue(Object target, String fieldName, Object newValue) {
        try {
            Class<?> clazz = target.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, newValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private UploadWorksheetTuple newUploadWorksheetTuple(String path) {
        UploadWorksheetTuple tuple = new UploadWorksheetTuple();
        tuple.setPath(path);
        if (new Path(path).isFile()) {
            tuple.setObjectId(UUID.randomUUID().toString());
            tuple.setSize(1L);
        }
        return tuple;
    }

    private @NotNull List<String> prepareWorksheetList() {
        List<String> addPaths = Arrays.asList(
                "/Worksheets/folder1/",
                "/Worksheets/folder1/sub1/",
                "/Worksheets/folder1/sub1/file_der2.sql",
                "/Worksheets/folder1/sub2/",
                "/Worksheets/folder1/file1.sql",
                "/Worksheets/folder2/",
                "/Worksheets/a_der.sql",
                "/Worksheets/b.sql");
        for (String path : addPaths) {
            String objectId = null;
            Long size = null;
            if (new Path(path).isFile()) {
                objectId = UUID.randomUUID().toString();
                size = 1L;
            }
            WorksheetMetaResp worksheet = worksheetServiceFacade.createWorksheet(projectId, path, objectId, size);
            assertNotNull(worksheet);
        }
        return addPaths;
    }
}
