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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.git.GitClientOperator;
import com.oceanbase.odc.service.git.repo.GitRepositoryManager;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
import com.oceanbase.odc.service.worksheet.service.RepoWorksheetService;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/26
 */
public class RepoWorksheetServiceTest extends ServiceTestEnv {

    private static final String GIT_REPO_DIR = "./data/repos";
    private static final String WORKSHEET_PREFIX = "/Repos/1/";

    @Autowired
    private RepoWorksheetService repoWorksheetService;
    @MockBean
    private ObjectStorageClient objectStorageClient;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private GitRepositoryManager repositoryManager;

    @Before
    public void setUp() throws IOException, GitAPIException {
        File workingDir = Paths.get(GIT_REPO_DIR, "1/1/dummy-project").toFile();
        if (!workingDir.exists()) {
            FileUtils.forceMkdir(workingDir);
        }
        FileUtils.write(new File(workingDir, "test.sql"), "select 1 from dual", StandardCharsets.UTF_8);
        Git git = Git.init().setDirectory(workingDir).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("init").call();

        Mockito.doAnswer(o -> {
            File file = getRealFile("dir1/create.sql");
            FileUtils.touch(file);
            return null;
        }).when(objectStorageClient).downloadToFile(Mockito.any(), Mockito.any());

        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1L);
        Mockito
                .when(repositoryManager.getOperator(Mockito.any(), Mockito.any()))
                .thenReturn(new GitClientOperator(git));
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(GIT_REPO_DIR, "1/1/dummy-project"));
    }

    @Test
    public void test_CreateWorksheet_success() {
        WorksheetMetaResp result =
                repoWorksheetService.createWorksheet(1L, new Path(WORKSHEET_PREFIX + "dir1/create.sql"), "", 0L);
        File realFile = getRealFile(result.getPath().replace(WORKSHEET_PREFIX, ""));
        Assert.assertTrue(realFile.exists());
    }

    @Test
    public void test_GetWorksheetDetails_success() {
        WorksheetResp result = repoWorksheetService.getWorksheetDetails(1L,
                new Path(WORKSHEET_PREFIX + "test.sql"));
        Assert.assertEquals("select 1 from dual", result.getContent());
    }

    @Test
    public void test_ListWorksheets_WithoutPath_success() {
        List<WorksheetMetaResp> result = repoWorksheetService.listWorksheets(1L, new Path(WORKSHEET_PREFIX), 1, "");
        // contains `.git` directory
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void test_ListWorksheets_WithPath_success() throws IOException {
        File file = getRealFile("dir1/create.sql");
        FileUtils.touch(file);

        List<WorksheetMetaResp> result =
                repoWorksheetService.listWorksheets(1L, new Path(WORKSHEET_PREFIX + "dir1/"), 1, "");
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void test_ListWorksheets_WithNameLike_success() {
        List<WorksheetMetaResp> result = repoWorksheetService.listWorksheets(1L, new Path(WORKSHEET_PREFIX), 1, "test");
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void test_ListWorksheets_WithNameLike_Empty() {
        List<WorksheetMetaResp> result =
                repoWorksheetService.listWorksheets(1L, new Path(WORKSHEET_PREFIX), 1, "not_exist");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void test_BatchDeleteWorksheets_success() {
        BatchOperateWorksheetsResp result = repoWorksheetService.batchDeleteWorksheets(1L,
                Collections.singletonList(new Path(WORKSHEET_PREFIX + "test.sql")));
        WorksheetMetaResp deleteResult = result.getSuccessfulFiles().get(0);
        File deleted = getRealFile(deleteResult.getPath().replace(WORKSHEET_PREFIX, ""));
        Assert.assertFalse(deleted.exists());
    }

    @Test
    public void test_RenameWorksheet_RenameFile_success() {
        List<WorksheetMetaResp> result = repoWorksheetService.renameWorksheet(1L,
                new Path(WORKSHEET_PREFIX + "test.sql"),
                new Path(WORKSHEET_PREFIX + "renamed.sql"));
        File renamed = getRealFile(result.get(0).getPath().replace(WORKSHEET_PREFIX, ""));
        Assert.assertTrue(renamed.exists());
    }

    @Test
    public void test_RenameWorksheet_RenameDirectory_success() {
        getRealFile("dir1").mkdir();
        List<WorksheetMetaResp> result = repoWorksheetService.renameWorksheet(1L,
                new Path(WORKSHEET_PREFIX + "dir1"),
                new Path(WORKSHEET_PREFIX + "dir2"));
        File renamed = getRealFile(result.get(0).getPath().replace(WORKSHEET_PREFIX, ""));
        Assert.assertTrue(renamed.exists());
    }

    private File getRealFile(String relativePath) {
        return Paths.get(GIT_REPO_DIR, "1/1/dummy-project", relativePath).toFile();
    }

    private File getRealFile(Path path) {
        return getRealFile(path.getStandardPath().replace(WORKSHEET_PREFIX, ""));
    }

}
