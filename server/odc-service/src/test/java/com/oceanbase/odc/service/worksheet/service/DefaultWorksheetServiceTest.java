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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.worksheet.CollaborationWorksheetEntity;
import com.oceanbase.odc.metadb.worksheet.CollaborationWorksheetRepository;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.resourcehistory.ResourceLastAccessService;
import com.oceanbase.odc.service.worksheet.constants.WorksheetConstants;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheetsPreProcessor;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq.UploadWorksheetTuple;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

@RunWith(MockitoJUnitRunner.class)
public class DefaultWorksheetServiceTest {
    Long organizationId = 1L;
    Long projectId = 1L;
    Long id = 1L;
    Long defaultVersion = 1L;
    String groupId = WorksheetConstants.DEFAULT_WORKSHEET_GROUP_ID;

    @Mock
    private CollaborationWorksheetRepository worksheetRepository;

    @Mock
    private ObjectStorageClient objectStorageClient;
    @Mock
    private AuthenticationFacade authenticationFacade;
    @Mock
    private ResourceLastAccessService resourceLastAccessService;

    private DefaultWorksheetService defaultWorksheetService;
    private File destinationDirectory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        defaultWorksheetService =
                new DefaultWorksheetService(objectStorageClient,
                        worksheetRepository, authenticationFacade, resourceLastAccessService);
        destinationDirectory = WorksheetPathUtil.createFileWithParent(
                WorksheetUtil.getWorksheetDownloadDirectory() + "project1", true);

    }

    @After
    public void clear() {
        OdcFileUtil.deleteFiles(new File(CloudObjectStorageConstants.TEMP_DIR));
    }


    @Test
    public void generateUploadUrl() throws MalformedURLException {
        Path path = new Path("/Worksheets/test");

        String uploadUrl = "http://uploadUrl";
        when(objectStorageClient.generateUploadUrl(anyString())).thenReturn(new URL(uploadUrl));

        GenerateWorksheetUploadUrlResp response = defaultWorksheetService.generateUploadUrl(projectId, groupId, path);

        assertEquals(uploadUrl, response.getUploadUrl());
        assertNotNull(response.getObjectId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWorksheet_InvalidPath() {
        Path createPath = new Path("/Worksheets/");
        defaultWorksheetService.createWorksheet(projectId, groupId, createPath, "objectId", 0L);
    }

    @Test
    public void createWorksheet_Success() {
        Path createPath = new Path("/Worksheets/test");
        Path parentPath = new Path("/Worksheets/");
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndInPaths(organizationId, projectId,
                groupId,
                Arrays.asList(createPath.getStandardPath(), parentPath.getStandardPath())))
                        .thenReturn(new ArrayList<>());
        when(worksheetRepository.countByPathLikeWithFilter(organizationId, projectId, groupId,
                parentPath.getStandardPath(), 2, 2, null))
                        .thenReturn(0L);
        when(worksheetRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    CollaborationWorksheetEntity entity = invocation.getArgument(0);
                    entity.setId(id++);
                    return entity;
                });

        WorksheetMetaResp createdWorksheet =
                defaultWorksheetService.createWorksheet(projectId, groupId, createPath, id++ + "", 0L);

        assertNotNull(createdWorksheet);
        assertEquals(createdWorksheet.getPath(), createPath.getStandardPath());
        verify(worksheetRepository).saveAndFlush(any());
    }

    @Test(expected = NotFoundException.class)
    public void getWorksheetDetails_NotFound() {
        Path path = new Path("/Worksheets/notfound");

        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                path.getStandardPath()))
                        .thenReturn(Optional.empty());

        defaultWorksheetService.getWorksheetDetails(projectId, groupId, path);
    }

    @Test
    public void getWorksheetDetails_Success() throws MalformedURLException {
        Path path = new Path("/Worksheets/test");
        CollaborationWorksheetEntity worksheet = newWorksheet(path);
        String objectId = "objectId";
        worksheet.setObjectId(objectId);
        String downloadUrl = "http://downloadUrl";

        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                path.getStandardPath()))
                        .thenReturn(Optional.of(worksheet));
        when(objectStorageClient.generateDownloadUrl(objectId, WorksheetConstants.MAX_DURATION_DOWNLOAD_SECONDS))
                .thenReturn(new URL(downloadUrl));
        WorksheetResp result = defaultWorksheetService.getWorksheetDetails(projectId, groupId, path);

        assertNotNull(result);
        assertEquals(downloadUrl, result.getContentDownloadUrl());
    }

    @Test
    public void listWorksheets_Success() {
        CollaborationWorksheetEntity worksheet1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity worksheet2 = newWorksheet("/Worksheets/folder2/");
        CollaborationWorksheetEntity worksheet3 = newWorksheet("/Worksheets/test1");
        CollaborationWorksheetEntity worksheet4 = newWorksheet("/Worksheets/test2");
        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                Path.worksheets().getStandardPath(), 2, 2,
                null))
                        .thenReturn(Arrays.asList(worksheet1, worksheet2, worksheet3, worksheet4));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.listWorksheets(projectId, groupId, Path.worksheets(), 1, null);
        assertNotNull(result);
        assertEquals(result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()),
                Stream.of(worksheet1, worksheet2, worksheet3, worksheet4)
                        .map(CollaborationWorksheetEntity::getPath).collect(Collectors.toSet()));
    }

    @Test
    public void batchUploadWorksheets() {
        BatchUploadWorksheetsReq batchUploadWorksheetsReq = new BatchUploadWorksheetsReq();
        batchUploadWorksheetsReq.setWorksheets(Arrays.asList(
                new UploadWorksheetTuple("/Worksheets/test1", "object1", 1L),
                new UploadWorksheetTuple("/Worksheets/test2", "object2", 1L)));
        Path parentPath = Path.worksheets();
        BatchCreateWorksheetsPreProcessor batchCreateWorksheetsPreProcessor =
                new BatchCreateWorksheetsPreProcessor(batchUploadWorksheetsReq);
        when(worksheetRepository.countByPathLikeWithFilter(organizationId, projectId, groupId,
                parentPath.getStandardPath(), 2, 2, null))
                        .thenReturn(new Long(WorksheetConstants.SAME_LEVEL_WORKSHEET_NUM_LIMIT - 2));
        when(worksheetRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> {
                    List<CollaborationWorksheetEntity> entities = invocation.getArgument(0);
                    entities.forEach(entity -> entity.setId(id++));
                    return entities;
                });

        BatchOperateWorksheetsResp result =
                defaultWorksheetService.batchUploadWorksheets(projectId, groupId, batchCreateWorksheetsPreProcessor);

        assertNotNull(result);
        assertEquals(result.getSuccessfulFiles().size(), 2);
        verify(worksheetRepository).saveAllAndFlush(anyList());
    }

    @Test
    public void batchDeleteWorksheets() {
        List<Path> paths = new ArrayList<>();
        paths.add(new Path("/Worksheets/folder2/"));
        paths.add(new Path("/Worksheets/folder1/"));
        paths.add(new Path("/Worksheets/test2"));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndInPaths(organizationId, projectId,
                groupId,
                Collections.singletonList("/Worksheets/test2")))
                        .thenReturn(Arrays.asList(newWorksheet(new Path("/Worksheets/test2"))));
        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                Path.worksheets().getStandardPath(), null,
                null,
                null))
                        .thenReturn(Arrays.asList(
                                newWorksheet(new Path("/Worksheets/folder3/")),
                                newWorksheet(new Path("/Worksheets/folder2/file1")),
                                newWorksheet(new Path("/Worksheets/folder1/")),
                                newWorksheet(new Path("/Worksheets/folder1/file1")),
                                newWorksheet(new Path("/Worksheets/folder1/subfolder1/"))));
        BatchOperateWorksheetsResp result = defaultWorksheetService.batchDeleteWorksheets(projectId, groupId, paths);
        assertNotNull(result);
        assertEquals(result.getSuccessfulFiles().size(), 5);
        verify(worksheetRepository).deleteAllByIdInBatch(anySet());
    }

    @Test
    public void renameWorksheet() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Collections.singletonList(newWorksheet(oldPath)));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.renameWorksheet(projectId, groupId, oldPath, newPath);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getPath(), newPath.getStandardPath());
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void moveWorksheet_renameSingle() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Collections.singletonList(newWorksheet(oldPath)));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, true);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getPath(), newPath.getStandardPath());
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void moveWorksheet_rename_WithSub() {
        Path oldPath = new Path("/Worksheets/old/");
        Path oldSub1Path = new Path("/Worksheets/old/file1");
        Path oldSub2Path = new Path("/Worksheets/old/sub1/");
        Path oldSub3Path = new Path("/Worksheets/old/sub1/file1");
        Path newPath = new Path("/Worksheets/new/");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath), newWorksheet(oldSub1Path),
                                newWorksheet(oldSub2Path),
                                newWorksheet(oldSub3Path)));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, true);

        assertNotNull(result);
        assertEquals(result.size(), 4);
        assertEquals(
                new HashSet<>(Arrays.asList("/Worksheets/new/", "/Worksheets/new/file1", "/Worksheets/new/sub1/",
                        "/Worksheets/new/sub1/file1")),
                result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void moveWorksheet_rename_DuplicatedNameException() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");

        // when(worksheetRepository.findByPathLikeWithFilter(organizationId,projectId,groupId,
        // oldPath.getStandardPath(), null, null, null))
        // .thenReturn(Collections.singletonList(newWorksheet(oldPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.of(newWorksheet(newPath)));
        try {
            defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, true);
            fail();
        } catch (BadRequestException e) {
            assert e.getErrorCode() == ErrorCodes.DuplicatedExists;
        }
    }

    @Test
    public void moveWorksheet_ExistDestinationPath_Single() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new/");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.of(newWorksheet(newPath)));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, false);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(
                new HashSet<>(
                        Arrays.asList("/Worksheets/new/old")),
                result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void moveWorksheet_ExistDestinationPath_Sub() {
        Path oldPath = new Path("/Worksheets/old/");
        Path oldSub1Path = new Path("/Worksheets/old/file1");
        Path oldSub2Path = new Path("/Worksheets/old/sub1/");
        Path oldSub3Path = new Path("/Worksheets/old/sub1/file1");
        Path newPath = new Path("/Worksheets/new/");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath), newWorksheet(oldSub1Path),
                                newWorksheet(oldSub2Path),
                                newWorksheet(oldSub3Path)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.of(newWorksheet(newPath)));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, false);

        assertNotNull(result);
        assertEquals(result.size(), 4);
        assertEquals(
                new HashSet<>(
                        Arrays.asList("/Worksheets/new/old/", "/Worksheets/new/old/file1", "/Worksheets/new/old/sub1/",
                                "/Worksheets/new/old/sub1/file1")),
                result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void moveWorksheet_ExistDestinationPath_DuplicatedNameException() {
        Path oldPath = new Path("/Worksheets/old/");
        Path newPath = new Path("/Worksheets/new/");
        Path movedPath = new Path("/Worksheets/new/old/");

        // when(worksheetRepository.findByPathLikeWithFilter(organizationId,projectId,groupId,
        // oldPath.getStandardPath(), null, null, null))
        // .thenReturn(Collections.singletonList(newWorksheet(oldPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.of(newWorksheet(newPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                movedPath.getStandardPath()))
                        .thenReturn(Optional.of(newWorksheet(movedPath)));
        try {
            defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, false);
            fail();
        } catch (BadRequestException e) {
            assert e.getErrorCode() == ErrorCodes.DuplicatedExists;
        }
    }

    @Test
    public void moveWorksheet_ExistDestinationPath_CannotMoveFileWhenDestinationExist() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.of(newWorksheet(newPath)));
        assertThrows(BadArgumentException.class,
                () -> defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath,
                        false));
    }

    @Test
    public void moveWorksheet_NotExistDestinationPath_Single() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.empty());

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, false);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(
                new HashSet<>(
                        Arrays.asList("/Worksheets/new")),
                result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }


    @Test
    public void moveWorksheet_NotExistDestinationPath_Single2() {
        Path oldPath = new Path("/Worksheets/old/");
        Path newPath = new Path("/Worksheets/new/");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.empty());

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, false);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(
                new HashSet<>(
                        Arrays.asList("/Worksheets/new/")),
                result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void moveWorksheet_NotExistDestinationPath_Sub() {
        Path oldPath = new Path("/Worksheets/old/");
        Path oldSub1Path = new Path("/Worksheets/old/file1");
        Path oldSub2Path = new Path("/Worksheets/old/sub1/");
        Path oldSub3Path = new Path("/Worksheets/old/sub1/file1");
        Path newPath = new Path("/Worksheets/new/");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                oldPath.getStandardPath(), null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath), newWorksheet(oldSub1Path),
                                newWorksheet(oldSub2Path),
                                newWorksheet(oldSub3Path)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.empty());

        List<WorksheetMetaResp> result =
                defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath, false);

        assertNotNull(result);
        assertEquals(result.size(), 4);
        assertEquals(
                new HashSet<>(
                        Arrays.asList("/Worksheets/new/", "/Worksheets/new/file1", "/Worksheets/new/sub1/",
                                "/Worksheets/new/sub1/file1")),
                result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }


    @Test
    public void moveWorksheet_NotExistDestinationPath_BadArgument() {
        Path oldPath = new Path("/Worksheets/old");
        Path newPath = new Path("/Worksheets/new/");

        // when(worksheetRepository.findByPathLikeWithFilter(organizationId,projectId,groupId,
        // oldPath.getStandardPath(), null, null, null))
        // .thenReturn(Arrays.asList(newWorksheet(oldPath)));
        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                newPath.getStandardPath()))
                        .thenReturn(Optional.empty());

        assertThrows(BadArgumentException.class,
                () -> defaultWorksheetService.moveWorksheet(organizationId, projectId, groupId, oldPath, newPath,
                        false));
    }

    @Test
    public void renameWorksheet_Sub() {
        Path oldPath = new Path("/Worksheets/old/");
        Path oldSubPath1 = new Path("/Worksheets/old/sub1");
        Path oldSubPath2 = new Path("/Worksheets/old/sub2/");
        Path newPath = new Path("/Worksheets/new/");

        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                "/Worksheets/old/", null, null, null))
                        .thenReturn(Arrays.asList(newWorksheet(oldPath), newWorksheet(oldSubPath1),
                                newWorksheet(oldSubPath2)));

        List<WorksheetMetaResp> result =
                defaultWorksheetService.renameWorksheet(projectId, groupId, oldPath, newPath);

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.stream().map(WorksheetMetaResp::getPath).collect(Collectors.toSet()),
                new HashSet<>(Arrays.asList(
                        "/Worksheets/new/",
                        "/Worksheets/new/sub1",
                        "/Worksheets/new/sub2/")));
        verify(worksheetRepository).batchUpdatePath(anyMap());
    }

    @Test
    public void editWorksheet() {
        Path editPath = new Path("/Worksheets/old");
        String objectId = "newObjectId";
        CollaborationWorksheetEntity worksheetEntity = newWorksheet(editPath.getStandardPath());

        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                editPath.getStandardPath()))
                        .thenReturn(Optional.of(worksheetEntity));
        when(worksheetRepository.updateContentByIdAndVersion(any()))
                .thenReturn(1);

        List<WorksheetMetaResp> result =
                defaultWorksheetService.editWorksheet(projectId, groupId, editPath, objectId,
                        0L, worksheetEntity.getVersion());

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getPath(), editPath.getStandardPath());
        assertEquals(result.get(0).getObjectId(), objectId);
        verify(worksheetRepository).updateContentByIdAndVersion(any());
    }

    @Test
    public void generateDownloadUrl_Success() {
        Path path = new Path("/Worksheets/test");
        CollaborationWorksheetEntity worksheet = newWorksheet(path);
        worksheet.setObjectId("objectId");

        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                path.getStandardPath()))
                        .thenReturn(Optional.of(worksheet));
        when(objectStorageClient.generateDownloadUrl(any(), anyLong()))
                .thenAnswer(invocation -> new URL("http://" + invocation.getArgument(0, String.class)));
        String url = defaultWorksheetService.generateDownloadUrl(projectId, groupId, path);
        assertNotNull(url);
        assertEquals(url, "http://" + worksheet.getObjectId());
        verify(objectStorageClient).generateDownloadUrl(worksheet.getObjectId(),
                WorksheetConstants.MAX_DURATION_DOWNLOAD_SECONDS);
    }

    @Test
    public void downloadPathsToDirectory_NormalCase() throws IOException {
        List<String> pathStrList = Arrays.asList(
                "/Worksheets/dir1/",
                "/Worksheets/dir2/",
                "/Worksheets/dir4/subdir1/",
                "/Worksheets/dir3/subdir1/file1",
                "/Worksheets/dir3/subdir1/file2",
                "/Worksheets/dir3/subdir1/file5");
        List<Path> paths = pathStrList.stream().map(Path::new).collect(Collectors.toList());
        Path commParentPath = Path.root();

        when(worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndInPaths(organizationId, projectId,
                groupId,
                Arrays.asList("/Worksheets/dir3/subdir1/file1",
                        "/Worksheets/dir3/subdir1/file2",
                        "/Worksheets/dir3/subdir1/file5")))
                                .thenReturn(Arrays.asList(newWorksheet("/Worksheets/dir3/subdir1/file1"),
                                        newWorksheet("/Worksheets/dir3/subdir1/file2"),
                                        newWorksheet("/Worksheets/dir3/subdir1/file5")));
        when(worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                Path.worksheets().getStandardPath(), null,
                null,
                null))
                        .thenReturn(Arrays.asList(
                                newWorksheet("/Worksheets/dir1/"),
                                newWorksheet("/Worksheets/dir1/subdir1/"),
                                newWorksheet("/Worksheets/dir1/subdir2/file1"),
                                newWorksheet("/Worksheets/dir1/subdir2/file2"),
                                newWorksheet("/Worksheets/dir2/"),
                                newWorksheet("/Worksheets/dir4/subdir1/")));

        // Test
        defaultWorksheetService.downloadPathsToDirectory(projectId, groupId, paths, commParentPath,
                destinationDirectory);

        // Verify
        verify(worksheetRepository, times(1)).findByOrganizationIdAndProjectIdAndGroupIdAndInPaths(anyLong(), anyLong(),
                anyString(), any());
        verify(objectStorageClient, times(5)).downloadToFile(anyString(), any(File.class));

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

    private CollaborationWorksheetEntity newWorksheet(String path) {
        return newWorksheet(new Path(path));
    }

    private CollaborationWorksheetEntity newWorksheet(Path path) {
        return newWorksheet(id++, projectId, path, id++ + "", 1L);
    }

    private CollaborationWorksheetEntity newWorksheet(Long id, Long projectId, Path path, String objectId,
            Long creatorId) {
        return CollaborationWorksheetEntity.builder()
                .id(id)
                .organizationId(organizationId)
                .projectId(projectId)
                .groupId(groupId)
                .path(path.toString())
                .pathLevel(path.getLevelNum())
                .size(0L)
                .extension(path.getExtension())
                .objectId(objectId)
                .creatorId(creatorId)
                .version(defaultVersion)
                .createTime(new Date())
                .updateTime(new Date())
                .build();
    }
}
