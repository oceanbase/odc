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
package com.oceanbase.odc.server.web.controller.v2;

import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.worksheet.CollaborationWorksheetRepository;
import com.oceanbase.odc.server.OdcServer;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.CloudObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.worksheet.WorksheetServiceFacadeImpl;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.factory.WorksheetServiceFactory;
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
// @Ignore
public class WorksheetControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    WorksheetServiceFacadeImpl worksheetServiceFacade;
    @Autowired
    WorksheetServiceFactory worksheetServiceFactory;
    @Autowired
    CollaborationWorksheetRepository worksheetRepository;
    @Autowired
    TransactionTemplate transactionTemplate;
    @MockBean
    AuthenticationFacade authenticationFacade;


    ObjectMapper objectMapper = new ObjectMapper();
    public static final String TEST_FILE_PATH = "src/test/resources/data/test0001.txt";
    public static final String TEST_FILE_PATH2 = "src/test/resources/data/中文名称.txt";
    public static final String TEST_DOWNLOAD_FILE =
            CloudObjectStorageConstants.TEMP_DIR + "/download/temp";
    Long projectId;
    CloudObjectStorageClient cloudObjectStorageClient;
    File tempFile;
    String objectName;
    List<String> objectNames;

    @Before
    public void setUp() {
        projectId = System.currentTimeMillis();
        MockitoAnnotations.openMocks(this);
        when(authenticationFacade.currentUserId())
                .thenReturn(1L);
        setFieldValue();
        objectNames = new ArrayList<>();
    }

    @After
    public void clear() {
        OdcFileUtil.deleteFiles(new File(CloudObjectStorageConstants.TEMP_DIR));
        if (objectName != null) {
            cloudObjectStorageClient.deleteObjects(Collections.singletonList(objectName));
        }
        if (CollectionUtils.isNotEmpty(objectNames)) {
            cloudObjectStorageClient.deleteObjects(objectNames);
        }
    }

    private void setFieldValue() {
        ObjectStorageConfiguration configuration = ITConfigurations.getOssConfiguration();
        CloudClient cloudClient = new CloudResourceConfigurations().publicEndpointCloudClient(() -> configuration);
        CloudClient internalCloudClient =
                new CloudResourceConfigurations().internalEndpointCloudClient(() -> configuration);
        cloudObjectStorageClient = new CloudObjectStorageClient(cloudClient,
                internalCloudClient, configuration);
        try {
            Class<?> clazz = worksheetServiceFacade.getClass();
            Field field = clazz.getDeclaredField("objectStorageClient");
            field.setAccessible(true);
            field.set(worksheetServiceFacade, cloudObjectStorageClient);
            DefaultWorksheetService defaultWorksheetService = new DefaultWorksheetService(transactionTemplate,
                    cloudObjectStorageClient, worksheetRepository,
                    authenticationFacade);
            Class<?> clazz2 = worksheetServiceFactory.getClass();
            Field field2 = clazz2.getDeclaredField("defaultWorksheetService");
            field2.setAccessible(true);
            field2.set(worksheetServiceFactory, defaultWorksheetService);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void generateUploadUrl_Normal() throws Exception {
        GenerateWorksheetUploadUrlReq req = new GenerateWorksheetUploadUrlReq();
        req.setPath("/Worksheets/ods_user.sql");
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/generateUploadUrl", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void createWorksheet_Folder() throws Exception {
        String path = "/Worksheets/folder1/";
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void createWorksheet_FileWithContentEmpty() throws Exception {
        String path = "/Worksheets/file.sql";
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void createWorksheet_File() throws Exception {
        String path = "/Worksheets/a.sql";
        String objectId = UUID.randomUUID().toString();
        Long totalLength = 1000L;

        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .param("objectId", objectId)
                        .param("totalLength", String.valueOf(totalLength))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/folder1/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/folder1/file.sql")
                .param("objectId", System.currentTimeMillis() + 1 + "")
                .param("totalLength", 100L + "")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    public void createWorksheet_FileNotFound() throws Exception {
        String path = "/Worksheets/folder1/a.sql";
        String objectId = UUID.randomUUID().toString();
        Long totalLength = 1000L;

        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .param("objectId", objectId)
                        .param("totalLength", String.valueOf(totalLength))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404)).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void getWorksheetDetail() throws Exception {
        GenerateWorksheetUploadUrlReq req = new GenerateWorksheetUploadUrlReq();
        req.setPath("/Worksheets/ods_user.sql");
        MvcResult uploadUrlResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/generateUploadUrl", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        SuccessResponse<GenerateWorksheetUploadUrlResp> uploadUrl =
                JsonUtils.fromJson(uploadUrlResult.getResponse().getContentAsString(),
                        new TypeReference<SuccessResponse<GenerateWorksheetUploadUrlResp>>() {});
        objectName = uploadUrl.getData().getObjectId();
        File uploadFile = new File(TEST_FILE_PATH);
        uploadByPreSignedUrl(uploadUrl.getData().getUploadUrl(), uploadFile);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/a.sql")
                .param("objectId", objectName)
                .param("totalLength", uploadFile.length() + "")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        MvcResult detailResult = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v2/project/{projectId}/worksheets/{path}",
                        projectId, "%2FWorksheets%2Fa.sql"))
                .andExpect(status().isOk()).andReturn();
        SuccessResponse<WorksheetResp> detail =
                JsonUtils.fromJson(detailResult.getResponse().getContentAsString(),
                        new TypeReference<SuccessResponse<WorksheetResp>>() {});
        tempFile = createFileWithParent(TEST_DOWNLOAD_FILE);
        downloadFromUrlToFile(new URL(detail.getData().getContentDownloadUrl()), tempFile);
        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }

    @Test
    public void listWorksheets() throws Exception {
        List<String> addPaths = Arrays.asList(
                "/Worksheets/folder1/",
                "/Worksheets/folder1/sub1/",
                "/Worksheets/folder1/sub1/file_der2.sql",
                "/Worksheets/folder1/sub2/",
                "/Worksheets/folder1/file1.sql",
                "/Worksheets/folder2/",
                "/Worksheets/a_der.sql",
                "/Worksheets/b.sql");
        int i = 0;
        for (String path : addPaths) {
            if (new Path(path).isFile()) {
                mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .param("objectId", UUID.randomUUID().toString())
                        .param("totalLength", i++ + "")
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk()).andReturn();
            } else {
                mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk()).andReturn();
            }
        }
        ListWorksheetsReq req = new ListWorksheetsReq();
        req.setPath("/Worksheets/");
        req.setDepth(1);
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v2/project/{projectId}/worksheets", projectId)
                        .params(convertToMultiValueMap(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        ListResponse<WorksheetMetaResp> listResponse =
                JsonUtils.fromJson(mvcResult.getResponse().getContentAsString(),
                        new TypeReference<ListResponse<WorksheetMetaResp>>() {});
        assert CollectionUtils.isEqualCollection(Arrays.asList(
                "/Worksheets/folder1/",
                "/Worksheets/folder2/",
                "/Worksheets/a_der.sql",
                "/Worksheets/b.sql"),
                listResponse.getData().getContents().stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));

        req.setPath("/Worksheets/");
        req.setDepth(0);
        req.setNameLike("der");
        mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v2/project/{projectId}/worksheets", projectId)
                        .params(convertToMultiValueMap(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        listResponse =
                JsonUtils.fromJson(mvcResult.getResponse().getContentAsString(),
                        new TypeReference<ListResponse<WorksheetMetaResp>>() {});
        assert CollectionUtils.isEqualCollection(Arrays.asList(
                "/Worksheets/folder1/",
                "/Worksheets/folder1/sub1/file_der2.sql",
                "/Worksheets/folder2/",
                "/Worksheets/a_der.sql"),
                listResponse.getData().getContents().stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));
    }

    @Test
    public void batchUploadWorksheets() throws Exception {

        BatchUploadWorksheetsReq req = new BatchUploadWorksheetsReq();
        req.setWorksheets(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/a.sql", UUID.randomUUID().toString(), 1L),
                new UploadWorksheetTuple("/Worksheets/b.sql", UUID.randomUUID().toString(), 2L),
                new UploadWorksheetTuple("/Worksheets/c/", null, null)));
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchUpload", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());

        ListWorksheetsReq listReq = new ListWorksheetsReq();
        listReq.setPath("/Worksheets/");
        listReq.setDepth(1);
        MvcResult listMvcResult = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v2/project/{projectId}/worksheets", projectId)
                        .params(convertToMultiValueMap(listReq))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        ListResponse<WorksheetMetaResp> listResponse =
                JsonUtils.fromJson(listMvcResult.getResponse().getContentAsString(),
                        new TypeReference<ListResponse<WorksheetMetaResp>>() {});
        assert CollectionUtils.isEqualCollection(Arrays.asList(
                "/Worksheets/c/",
                "/Worksheets/a.sql",
                "/Worksheets/b.sql"),
                listResponse.getData().getContents().stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));
    }

    @Test
    public void batchDeleteWorksheets() throws Exception {
        BatchUploadWorksheetsReq req = new BatchUploadWorksheetsReq();
        req.setWorksheets(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/a.sql", UUID.randomUUID().toString(), 1L),
                new UploadWorksheetTuple("/Worksheets/b.sql", UUID.randomUUID().toString(), 2L),
                new UploadWorksheetTuple("/Worksheets/c/", null, null)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchUpload", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        req.setWorksheets(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/c/a.sql", UUID.randomUUID().toString(), 1L),
                new UploadWorksheetTuple("/Worksheets/c/b.sql", UUID.randomUUID()
                        .toString(), 2L),
                new UploadWorksheetTuple("/Worksheets/c/d/", null, null)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchUpload", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();


        List<String> paths = Arrays.asList("/Worksheets/a.sql", "/Worksheets/b.sql", "/Worksheets/c/");
        mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchDelete", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paths)))
                .andExpect(status().isOk()).andReturn();

        ListWorksheetsReq listReq = new ListWorksheetsReq();
        listReq.setPath("/Worksheets/");
        listReq.setDepth(1);
        MvcResult listMvcResult = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v2/project/{projectId}/worksheets", projectId)
                        .params(convertToMultiValueMap(listReq))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        ListResponse<WorksheetMetaResp> listResponse =
                JsonUtils.fromJson(listMvcResult.getResponse().getContentAsString(),
                        new TypeReference<ListResponse<WorksheetMetaResp>>() {});
        assert listResponse.getData().getContents().isEmpty();
    }

    @Test
    public void renameWorksheet() throws Exception {
        BatchUploadWorksheetsReq req = new BatchUploadWorksheetsReq();
        req.setWorksheets(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/a.sql", UUID.randomUUID().toString(), 1L),
                new UploadWorksheetTuple("/Worksheets/b.sql", UUID.randomUUID().toString(), 2L),
                new UploadWorksheetTuple("/Worksheets/c/", null, null)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchUpload", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        req.setWorksheets(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/c/a.sql", UUID.randomUUID().toString(), 1L),
                new UploadWorksheetTuple("/Worksheets/c/b.sql", UUID.randomUUID()
                        .toString(), 2L),
                new UploadWorksheetTuple("/Worksheets/c/d/", null, null)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchUpload", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/rename", projectId)
                        .param("path", "/Worksheets/c/")
                        .param("destinationPath", "/Worksheets/e/")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        ListWorksheetsReq listReq = new ListWorksheetsReq();
        listReq.setPath("/Worksheets/e/");
        listReq.setDepth(1);
        MvcResult listMvcResult = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v2/project/{projectId}/worksheets", projectId)
                        .params(convertToMultiValueMap(listReq))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        ListResponse<WorksheetMetaResp> listResponse =
                JsonUtils.fromJson(listMvcResult.getResponse().getContentAsString(),
                        new TypeReference<ListResponse<WorksheetMetaResp>>() {});
        assert CollectionUtils.isEqualCollection(Arrays.asList(
                "/Worksheets/e/d/",
                "/Worksheets/e/a.sql",
                "/Worksheets/e/b.sql"),
                listResponse.getData().getContents().stream().map(WorksheetMetaResp::getPath).collect(
                        Collectors.toList()));
    }

    @Test
    public void editWorksheet() throws Exception {
        String path = "/Worksheets/a.sql";
        mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                        .param("path", path)
                        .param("objectId", UUID.randomUUID().toString())
                        .param("totalLength", 10L + "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        UpdateWorksheetReq req = new UpdateWorksheetReq();
        req.setObjectId(UUID.randomUUID().toString());
        req.setTotalLength(1L);
        req.setPrevVersion(0L);
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders
                        .put("/api/v2/project/{projectId}/worksheets/{path}", projectId,
                                URLEncoder.encode(path, String.valueOf(
                                        StandardCharsets.UTF_8)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void shouldBatchDownloadWorksheetsSuccessfully() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/user_analysis/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/user_analysis/c/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        GenerateWorksheetUploadUrlReq req = new GenerateWorksheetUploadUrlReq();
        req.setPath("/Worksheets/user_analysis/ods_user.sql");
        MvcResult uploadUrlResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/generateUploadUrl", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        SuccessResponse<GenerateWorksheetUploadUrlResp> uploadUrl =
                JsonUtils.fromJson(uploadUrlResult.getResponse().getContentAsString(),
                        new TypeReference<SuccessResponse<GenerateWorksheetUploadUrlResp>>() {});
        objectNames.add(uploadUrl.getData().getObjectId());
        File uploadFile = new File(TEST_FILE_PATH);
        uploadByPreSignedUrl(uploadUrl.getData().getUploadUrl(), uploadFile);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/user_analysis/ods_user.sql")
                .param("objectId", uploadUrl.getData().getObjectId())
                .param("totalLength", uploadFile.length() + "")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        req.setPath("/Worksheets/user_analysis/c/dwd_user.sql");
        uploadUrlResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/generateUploadUrl", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn();
        uploadUrl =
                JsonUtils.fromJson(uploadUrlResult.getResponse().getContentAsString(),
                        new TypeReference<SuccessResponse<GenerateWorksheetUploadUrlResp>>() {});
        objectNames.add(uploadUrl.getData().getObjectId());
        uploadFile = new File(TEST_FILE_PATH2);
        uploadByPreSignedUrl(uploadUrl.getData().getUploadUrl(), uploadFile);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets", projectId)
                .param("path", "/Worksheets/user_analysis/c/dwd_user.sql")
                .param("objectId", uploadUrl.getData().getObjectId())
                .param("totalLength", uploadFile.length() + "")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();


        Set<String> paths = new HashSet<>(
                Arrays.asList("/Worksheets/user_analysis/ods_user.sql",
                        "/Worksheets/user_analysis/c/dwd_user.sql"));
        MvcResult mvcResult = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v2/project/{projectId}/worksheets/batchDownload", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paths)))
                .andExpect(status().isOk()).andReturn();
        System.out.println(mvcResult.getResponse().getContentAsString());
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
        assert response.getCode() == 200;
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

    public static MultiValueMap<String, String> convertToMultiValueMap(ListWorksheetsReq requestData) {
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

}
