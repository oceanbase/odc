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
package com.oceanbase.odc.service.git;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.git.model.GitRepository;
import com.oceanbase.odc.service.git.model.GitStatus;
import com.oceanbase.odc.service.git.model.VcsProvider;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetType;
import com.oceanbase.odc.service.worksheet.service.RepoWorksheetService;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/12
 */
public class GitRepoServiceIT extends ServiceTestEnv {
    private static final String GIT_BRANCH = "test_branch_" + UUID.randomUUID();

    @Autowired
    private GitRepoService repoService;
    @Autowired
    private GitIntegrationService integrationService;
    @Autowired
    private RepoWorksheetService repoWorksheetService;
    @MockBean
    private ObjectStorageClient objectStorageClient;

    @Before
    public void setUp() {}

    @AfterClass
    public static void afterClass() {
        FileUtils.deleteQuietly(new File("./data/repos"));
    }

    @Test
    public void test_GitOperation() throws Exception {
        Long repoId = insertRepoRecord();

        Set<String> branches = repoService.listBranches(1L, repoId);
        System.out.println(branches);

        repoService.createBranch(1L, repoId, GIT_BRANCH);

        repoService.checkout(1L, repoId, GIT_BRANCH);

        deleteAWorksheet(repoId);
        GitStatus status = repoService.getStatus(1L, repoId);
        System.out.println(status);

        try {
            repoService.commit(1L, repoId, "test commit");

            GitStatus pulled = repoService.pull(1L, repoId);

            Assert.assertEquals(0, pulled.getCommitsBehindRemote());
        } finally {
            repoService.deleteBranch(1L, repoId, "origin/" + GIT_BRANCH);
        }

    }

    private Long insertRepoRecord() {
        GitRepository repoConfig = ITConfigurations.getGitRepositoryConfig();
        repoConfig.setName("test");
        repoConfig.setProviderType(VcsProvider.GITHUB);
        repoConfig.setProviderUrl("https://github.com");
        repoConfig.setSshAddress("git@github.com:xxx/xxx.git");
        List<GitRepository> repositories = integrationService.batchCreate(1L, Collections.singletonList(repoConfig));
        return repositories.get(0).getId();
    }

    private void deleteAWorksheet(Long repoId) throws FileNotFoundException {
        List<WorksheetMetaResp> worksheets =
                repoWorksheetService.listWorksheets(1L, new Path("/Repos/" + repoId + "/"), 1, "");
        WorksheetMetaResp worksheet = worksheets.stream()
                .filter(w -> w.getType() == WorksheetType.FILE)
                .findFirst().orElseThrow(FileNotFoundException::new);
        repoWorksheetService.batchDeleteWorksheets(1L, Collections.singletonList(new Path(worksheet.getPath())));
    }

}
