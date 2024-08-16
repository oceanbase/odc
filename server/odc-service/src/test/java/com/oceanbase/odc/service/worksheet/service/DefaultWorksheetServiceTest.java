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
package com.oceanbase.odc.service.worksheet.service;

import static com.oceanbase.odc.service.worksheet.utils.WorksheetTestUtil.newDirWorksheet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.domain.WorksheetObjectStorageGateway;
import com.oceanbase.odc.service.worksheet.domain.WorksheetRepository;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq.UploadWorksheetTuple;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

@RunWith(MockitoJUnitRunner.class)
public class DefaultWorksheetServiceTest {
    Long projectId = 1L;
    Long id = 1L;
    Long defaultVersion = 1L;

    private Worksheet newWorksheet(String path) {
        return newWorksheet(new Path(path));
    }

    private Worksheet newWorksheet(Path path) {
        return newWorksheet(id++, projectId, path, id++ + "", 1L);
    }

    public Worksheet newWorksheet(Long id, Long projectId, Path path, String objectId, Long creatorId) {
        return new Worksheet(id, new Date(), new Date(), projectId, path, creatorId,
                defaultVersion, objectId, null, null);
    }

    @Mock
    private WorksheetRepository worksheetRepository;

    @Mock
    private WorksheetObjectStorageGateway worksheetObjectStorageGateway;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private AuthenticationFacade authenticationFacade;

    private DefaultWorksheetService defaultWorksheetService;
    private File destinationDirectory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        defaultWorksheetService =
                new DefaultWorksheetService(transactionTemplate, worksheetObjectStorageGateway,
                        worksheetRepository, authenticationFacade);
        destinationDirectory = WorksheetPathUtil.createFileWithParent(
                WorksheetUtil.getWorksheetDownloadDirectory() + "project1", true).toFile();

    }

    @After
    public void clear() {
        OdcFileUtil.deleteFiles(new File(CloudObjectStorageConstants.TEMP_DIR));
    }



    @Test
    public void testGenerateUploadUrl() {
        Path path = new Path("/Worksheets/test");

        String uploadUrl = "uploadUrl";

        when(worksheetObjectStorageGateway.generateUploadUrl(anyString(), anyString())).thenReturn(uploadUrl);

        GenerateWorksheetUploadUrlResp response = defaultWorksheetService.generateUploadUrl(projectId, path);

        assertEquals(uploadUrl, response.getUploadUrl());
        assertNotNull(response.getObjectId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWorksheet_InvalidPath() {
        Path createPath = new Path("/Worksheets/");
        when(authenticationFacade.currentUserId()).thenReturn(1L);

        defaultWorksheetService.createWorksheet(projectId, createPath, "objectId");
    }

    @Test
    public void testCreateWorksheet_Success() {
        Path createPath = new Path("/Worksheets/test");
        Path parentPath = new Path("/Worksheets/");
        when(worksheetRepository.findByProjectIdAndPath(projectId, parentPath, null, true, true, true, false))
                .thenReturn(Optional.of(newWorksheet(parentPath)));

        Worksheet createdWorksheet = defaultWorksheetService.createWorksheet(projectId, createPath, id++ + "");

        assertNotNull(createdWorksheet);
        assertEquals(createdWorksheet.getPath(), createPath);
        verify(worksheetRepository).batchAdd(anySet());
    }

    @Test(expected = NotFoundException.class)
    public void testGetWorksheetDetails_NotFound() {
        Path path = new Path("/Worksheets/notfound");

        when(worksheetRepository.findByProjectIdAndPath(projectId, path, null, false, false, false, false))
                .thenReturn(Optional.empty());

        defaultWorksheetService.getWorksheetDetails(projectId, path);
    }

    @Test
    public void testGetWorksheetDetails_Success() {
        Path path = new Path("/Worksheets/test");
        Worksheet worksheet = newWorksheet(path);
        String objectId = "objectId";
        worksheet.setObjectId(objectId);
        String downloadUrl = "downloadUrl";

        when(worksheetRepository.findByProjectIdAndPath(projectId, path, null, false, false, false, false))
                .thenReturn(Optional.of(worksheet));
        when(worksheetObjectStorageGateway.generateDownloadUrl(objectId))
                .thenReturn(downloadUrl);
        Worksheet result = defaultWorksheetService.getWorksheetDetails(projectId, path);

        assertNotNull(result);
        assertEquals(objectId, result.getObjectId());
        assertEquals(downloadUrl, result.getContentDownloadUrl());
    }

    @Test
    public void testListWorksheets_Success() {
        Worksheet worksheet1 = newWorksheet("/Worksheets/folder1/");
        Worksheet worksheet2 = newWorksheet("/Worksheets/folder2/");
        Worksheet worksheet3 = newWorksheet("/Worksheets/test1");
        Worksheet worksheet4 = newWorksheet("/Worksheets/test2");
        Worksheet worksheet = newWorksheet(Path.worksheets());
        worksheet.setSubWorksheets(new HashSet<>(Arrays.asList(worksheet1, worksheet2, worksheet3, worksheet4)));
        when(worksheetRepository.findByProjectIdAndPath(projectId, Path.worksheets(), null, false, true, true, false))
                .thenReturn(Optional.of(worksheet));

        List<Worksheet> result = defaultWorksheetService.listWorksheets(projectId, Path.worksheets(), 1, null);
        assertNotNull(result);
        assertEquals(result, Arrays.asList(worksheet1, worksheet2, worksheet3, worksheet4));
    }

    @Test
    public void testBatchUploadWorksheets() {
        BatchUploadWorksheetsReq batchUploadWorksheetsReq = new BatchUploadWorksheetsReq();
        batchUploadWorksheetsReq.setWorksheets(new HashSet<>(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/test1", "object1"),
                new UploadWorksheetTuple("/Worksheets/test2", "object2"))));
        BatchCreateWorksheets batchCreateWorksheets = new BatchCreateWorksheets(batchUploadWorksheetsReq);

        when(worksheetRepository.findByProjectIdAndPath(anyLong(), any(Path.class), any(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(newWorksheet(new Path("/Worksheets/"))));

        BatchOperateWorksheetsResult result =
                defaultWorksheetService.batchUploadWorksheets(projectId, batchCreateWorksheets);

        assertNotNull(result);
        assertEquals(result.getSuccessful().size(), 2);
        verify(worksheetRepository).batchAdd(anySet());
    }

    @Test
    public void testBatchDeleteWorksheets() {
        Set<Path> paths = new HashSet<>();
        paths.add(new Path("/Worksheets/folder1/"));
        paths.add(new Path("/Worksheets/test2"));
        when(worksheetRepository.listWithSubsByProjectIdAndPath(projectId, new Path("/Worksheets/folder1/")))
                .thenReturn(Arrays.asList(
                        newWorksheet(new Path("/Worksheets/folder1/")),
                        newWorksheet(new Path("/Worksheets/folder1/file1")),
                        newWorksheet(new Path("/Worksheets/folder1/subfolder1/"))));
        when(worksheetRepository.listWithSubsByProjectIdAndPath(projectId, new Path("/Worksheets/test2")))
                .thenReturn(Arrays.asList(newWorksheet(new Path("/Worksheets/test2"))));
        BatchOperateWorksheetsResult result = defaultWorksheetService.batchDeleteWorksheets(projectId, paths);

        assertNotNull(result);
        assertEquals(result.getSuccessful().size(), 4);
        verify(worksheetRepository).batchDelete(anySet());
    }

    @Test
    public void testRenameWorksheet() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");

        when(worksheetRepository.findByProjectIdAndPath(anyLong(), any(Path.class), any(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(newWorksheet(oldPath)));

        List<Worksheet> result = defaultWorksheetService.renameWorksheet(projectId, oldPath, newPath);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getPath(), newPath);
        verify(worksheetRepository).batchUpdateById(anySet());
    }

    @Test
    public void testEditWorksheet() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");
        String objectId = "newObjectId";
        Long readVersion = 1L;

        when(worksheetRepository.findByProjectIdAndPath(anyLong(), any(Path.class), any(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(newWorksheet(oldPath)));

        List<Worksheet> result =
                defaultWorksheetService.editWorksheet(projectId, oldPath, newPath, objectId, readVersion);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getPath(), newPath);
        assertEquals(result.get(0).getObjectId(), objectId);
        verify(worksheetRepository).batchUpdateById(anySet());
    }

    @Test
    public void testGetDownloadUrl_Success() {
        Path path = new Path("/Worksheets/test");
        Worksheet worksheet = newWorksheet(path);
        worksheet.setObjectId("objectId");

        when(worksheetRepository.findByProjectIdAndPath(projectId, path, null, false, false, false, true))
                .thenReturn(Optional.of(worksheet));
        when(worksheetObjectStorageGateway.generateDownloadUrl(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class));
        String url = defaultWorksheetService.getDownloadUrl(projectId, path);
        assertNotNull(url);
        assertEquals(url, worksheet.getObjectId());
        verify(worksheetObjectStorageGateway).generateDownloadUrl(worksheet.getObjectId());
    }

    @Test
    public void testDownloadPathsToDirectory_NormalCase() {

        List<String> pathStrList = Arrays.asList(
                "/Worksheets/dir1/",
                "/Worksheets/dir2/",
                "/Worksheets/dir4/subdir1/",
                "/Worksheets/dir3/subdir1/file1",
                "/Worksheets/dir3/subdir1/file2",
                "/Worksheets/dir3/subdir1/file5");
        Set<Path> paths = pathStrList.stream().map(Path::new).collect(Collectors.toSet());
        Path commParentPath = Path.root();

        // Mock
        when(worksheetRepository.findByProjectIdAndPath(projectId, new Path("/Worksheets/dir1/"),
                null, false, true, true, false))
                .thenReturn(Optional.of(newDirWorksheet(projectId, "/Worksheets/dir1/", null,
                        Arrays.asList("/Worksheets/dir1/subdir1/", "/Worksheets/dir1/subdir2/file1",
                                "/Worksheets/dir1/subdir2/file2"))));
        when(worksheetRepository.findByProjectIdAndPath(projectId, new Path("/Worksheets/dir2/"),
                null, false, true, true, false))
                .thenReturn(Optional.of(newDirWorksheet(projectId, "/Worksheets/dir2/", null, null)));
        when(worksheetRepository.findByProjectIdAndPath(projectId, new Path("/Worksheets/dir4/subdir1/"),
                null, false, true, true, false))
                .thenReturn(Optional.of(newDirWorksheet(projectId, "/Worksheets/dir4/subdir1/", null, null)));
        when(worksheetRepository.findByProjectIdAndPath(projectId, new Path("/Worksheets/dir3/subdir1/file1"),
                null, false, true, false, false))
                .thenReturn(
                        Optional.of(newDirWorksheet(projectId, "/Worksheets/dir3/subdir1/file1", null, null)));
        when(worksheetRepository.findByProjectIdAndPath(projectId, new Path("/Worksheets/dir3/subdir1/file2"),
                null, false, true, false, false))
                .thenReturn(
                        Optional.of(newDirWorksheet(projectId, "/Worksheets/dir3/subdir1/file2", null, null)));
        when(worksheetRepository.findByProjectIdAndPath(projectId, new Path("/Worksheets/dir3/subdir1/file5"),
                null, false, true, false, false))
                .thenReturn(
                        Optional.of(newDirWorksheet(projectId, "/Worksheets/dir3/subdir1/file5", null, null)));


        // Test
        defaultWorksheetService.downloadPathsToDirectory(projectId, paths, commParentPath, destinationDirectory);

        // Verify
        verify(worksheetRepository, times(6)).findByProjectIdAndPath(anyLong(), any(Path.class), any(),
                anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean());
        verify(worksheetObjectStorageGateway, times(5)).downloadToFile(anyString(), any(File.class));

        List<String> expectedFileStrList = Arrays.asList(
                "/Worksheets/",
                "/Worksheets/dir1/",
                "/Worksheets/dir1/subdir1/",
                "/Worksheets/dir1/subdir2/",
                "/Worksheets/dir1/subdir2/file1",
                "/Worksheets/dir1/subdir2/file2",
                "/Worksheets/dir2/",
                "/Worksheets/dir4/",
                "/Worksheets/dir4/subdir1/",
                "/Worksheets/dir3/",
                "/Worksheets/dir3/subdir1/",
                "/Worksheets/dir3/subdir1/file1",
                "/Worksheets/dir3/subdir1/file2",
                "/Worksheets/dir3/subdir1/file5");
        List<String> files = getAllSubFiles(destinationDirectory);
        assert files.size() == expectedFileStrList.size();
        assertEquals(new HashSet<>(files),
                new HashSet<>(expectedFileStrList.stream()
                        .map(str -> destinationDirectory.getPath() + str)
                        .collect(Collectors.toList())));
    }

    private List<String> getAllSubFiles(File file) {
        List<String> fileStrList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File subFile : files) {
                fileStrList.add(subFile.getPath() + (subFile.isDirectory() ? "/" : ""));
                if (subFile.isDirectory()) {
                    fileStrList.addAll(getAllSubFiles(subFile));
                }
            }
        }
        return fileStrList;
    }

}
