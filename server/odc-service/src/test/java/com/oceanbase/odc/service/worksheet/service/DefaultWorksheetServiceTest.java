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
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.WorksheetObjectStorageGateway;
import com.oceanbase.odc.service.worksheet.domain.WorksheetRepository;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

@RunWith(MockitoJUnitRunner.class)
public class DefaultWorksheetServiceTest {

    @Mock
    private WorksheetRepository worksheetRepository;

    @Mock
    private WorksheetObjectStorageGateway worksheetObjectStorageGateway;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private AuthenticationFacade authenticationFacade;

    private DefaultWorksheetService defaultWorksheetService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        defaultWorksheetService =
                new DefaultWorksheetService(transactionTemplate, worksheetObjectStorageGateway,
                        worksheetRepository, authenticationFacade);
    }

    Long projectId = 1L;

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
        File destinationDirectory = WorksheetPathUtil.createFileWithParent(
                WorksheetUtil.getWorksheetDownloadDirectory() + "project1", true).toFile();

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
