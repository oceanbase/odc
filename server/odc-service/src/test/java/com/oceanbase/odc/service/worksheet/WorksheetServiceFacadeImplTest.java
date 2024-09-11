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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.worksheet.constants.WorksheetConstants;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.factory.WorksheetServiceFactory;
import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.service.DefaultWorksheetService;
import com.oceanbase.odc.service.worksheet.service.RepoWorksheetService;

@RunWith(MockitoJUnitRunner.class)
public class WorksheetServiceFacadeImplTest {
    Long projectId = 1L;
    String groupId = WorksheetConstants.DEFAULT_WORKSHEET_GROUP_ID;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ObjectStorageClient objectStorageClient;
    @Mock
    private WorksheetServiceFactory worksheetServiceFactory;
    @Mock
    private DefaultWorksheetService defaultWorksheetService;
    @Mock
    private RepoWorksheetService repoWorksheetService;

    @InjectMocks
    private WorksheetServiceFacadeImpl worksheetServiceFacade;

    @After
    public void clear() {
        OdcFileUtil.deleteFiles(new File(CloudObjectStorageConstants.TEMP_DIR));
    }

    /**
     * single file download
     */
    @Test
    public void batchDownloadWorksheets_singleFile() {
        Path path = new Path("/Worksheets/dir3/subdir1/file1");
        Set<String> paths = new HashSet<>();
        paths.add(path.getStandardPath());

        when(worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS))
                .thenReturn(defaultWorksheetService);
        when(defaultWorksheetService.generateDownloadUrl(any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2, Path.class).getStandardPath());

        String result = worksheetServiceFacade.batchDownloadWorksheets(projectId, groupId, paths);

        assertEquals(path.getStandardPath(), result);
    }

    /**
     * multi file download
     */
    @Test
    public void batchDownloadWorksheets_multipleFiles() {
        Long projectId = 1L;
        Path path1 = new Path("/Worksheets/dir3/subdir1/file1");
        Path path2 = new Path("/Worksheets/dir3/subdir1/path2");
        Set<String> paths = new HashSet<>();
        paths.add(path1.getStandardPath());
        paths.add(path2.getStandardPath());

        lenient().when(projectRepository.getReferenceById(anyLong()))
                .thenAnswer(invocation -> "projectName" + invocation.getArgument(0, Long.class));
        when(worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS))
                .thenReturn(defaultWorksheetService);
        when(objectStorageClient.generateDownloadUrl(any(), anyLong()))
                .thenAnswer(invocation -> new URL("http://" + invocation.getArgument(0, String.class)));

        String result = worksheetServiceFacade.batchDownloadWorksheets(projectId, groupId, paths);

        assert StringUtils.isNotBlank(result) && result.endsWith(".zip");
        // Verify
        verify(projectRepository, times(0)).getReferenceById(anyLong());
    }
}
