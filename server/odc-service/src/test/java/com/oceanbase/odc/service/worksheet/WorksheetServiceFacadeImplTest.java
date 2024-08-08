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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.WorksheetObjectStorageGateway;
import com.oceanbase.odc.service.worksheet.domain.WorksheetProjectRepository;
import com.oceanbase.odc.service.worksheet.factory.WorksheetServiceFactory;
import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.service.DefaultWorksheetService;
import com.oceanbase.odc.service.worksheet.service.RepoWorksheetService;

@RunWith(MockitoJUnitRunner.class)
public class WorksheetServiceFacadeImplTest {

    @Mock
    private WorksheetProjectRepository    worksheetProjectRepository;
    @Mock
    private WorksheetObjectStorageGateway projectFileOssGateway;
    @Mock
    private WorksheetServiceFactory       worksheetServiceFactory;
    @Mock
    private DefaultWorksheetService defaultWorksheetService;
    @Mock
    private RepoWorksheetService repoWorksheetService;

    @InjectMocks
    private WorksheetServiceFacadeImpl worksheetServiceFacade;
    Long projectId = 1L;

    /**
     * single file download
     */
    @Test
    public void testBatchDownloadWorksheets_singleFile() {
        Path path = new Path("/Worksheets/dir3/subdir1/file1");
        Set<String> paths = new HashSet<>();
        paths.add(path.getStandardPath());

        when(worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS))
                .thenReturn(defaultWorksheetService);
        when(defaultWorksheetService.getDownloadUrl(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1, Path.class).getStandardPath());

        String result = worksheetServiceFacade.batchDownloadWorksheets(projectId, paths);

        assertEquals(path.getStandardPath(), result);
    }

    /**
     * multi file download
     */
    @Test
    public void testBatchDownloadWorksheets_multipleFiles() {
        Long projectId = 1L;
        Path path1 = new Path("/Worksheets/dir3/subdir1/file1");
        Path path2 = new Path("/Worksheets/dir3/subdir1/path2");
        Set<String> paths = new HashSet<>();
        paths.add(path1.getStandardPath());
        paths.add(path2.getStandardPath());

        lenient().when(worksheetProjectRepository.getProjectName(anyLong()))
                .thenAnswer(invocation -> "projectName" + invocation.getArgument(0, Long.class));
        when(worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS))
                .thenReturn(defaultWorksheetService);
        // when(worksheetServiceFactory.getProjectFileService(WorksheetLocation.REPOS))
        // .thenReturn(repoWorksheetService);
        when(projectFileOssGateway.uploadFile(any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0, File.class).getPath());
        when(projectFileOssGateway.generateDownloadUrl(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        String result = worksheetServiceFacade.batchDownloadWorksheets(projectId, paths);

        assert StringUtils.isNotBlank(result) && result.endsWith(".zip");
        // Verify
        verify(worksheetProjectRepository, times(0)).getProjectName(anyLong());
    }
}
