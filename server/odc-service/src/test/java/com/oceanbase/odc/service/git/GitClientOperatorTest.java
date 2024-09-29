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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.service.git.model.FileChangeType;
import com.oceanbase.odc.service.git.model.GitDiff;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/31
 */
public class GitClientOperatorTest {
    private static final String PROJECT_WORKDIR = "data/git/dummy-project";

    @Before
    public void setUp() throws IOException, GitAPIException {
        File workingDir = new File(PROJECT_WORKDIR);
        if (!workingDir.exists()) {
            FileUtils.forceMkdir(workingDir);
        }
        FileUtils.write(new File(workingDir, "test.sql"), "select 1 from dual", StandardCharsets.UTF_8);
        Git git = Git.init().setDirectory(workingDir).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("init").call();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(PROJECT_WORKDIR));
    }

    @Test
    public void test_listBranches_success() throws IOException, GitAPIException {
        Set<String> branches = getOperator().listBranches();
        Assert.assertEquals(1, branches.size());
    }

    @Test
    public void test_currentBranch_success() throws IOException {
        String currentBranch = getOperator().currentBranch();
        Assert.assertTrue("main".equals(currentBranch) || "master".equals(currentBranch));
    }

    @Test
    public void test_createBranch_success() throws IOException, GitAPIException {
        getOperator().createBranch("test-branch-1");
        List<Ref> refs = getGit().branchList().call();
        Assert.assertEquals(2, refs.size());
    }

    @Test
    public void test_checkout_success() throws IOException, GitAPIException {
        String branch = "test-branch-1";
        Git git = getGit();
        git.branchCreate().setName(branch).call();

        getOperator().checkout(branch);
        Assert.assertEquals(branch, git.getRepository().getBranch());
    }

    @Test
    public void test_add_success() throws IOException, GitAPIException {
        Git git = getGit();
        FileUtils.write(new File(PROJECT_WORKDIR, "test.sql"), "this is updated", StandardCharsets.UTF_8);
        Status status = git.status().call();
        Assert.assertTrue(status.getModified().contains("test.sql"));

        getOperator().add("test.sql");
        status = git.status().call();
        Assert.assertTrue(status.getChanged().contains("test.sql"));
    }

    @Test
    public void test_commit_success() throws IOException, GitAPIException {
        Git git = getGit();
        FileUtils.write(new File(PROJECT_WORKDIR, "test.sql"), "this is updated", StandardCharsets.UTF_8);
        git.add().addFilepattern("test.sql").call();

        getOperator().commit("my commit", "xxx@xxx.com");

        Assert.assertEquals("my commit", git.log().call().iterator().next().getFullMessage());
    }

    @Test
    public void test_diffContent_success() throws IOException, GitAPIException {
        Git git = getGit();
        FileUtils.write(new File(PROJECT_WORKDIR, "test.sql"), "this is updated", StandardCharsets.UTF_8);
        git.add().addFilepattern("test.sql").call();

        GitDiff req = new GitDiff();
        req.setOldPath("test.sql");
        req.setNewPath("test.sql");
        req.setState(FileChangeType.M);
        GitDiff diff = getOperator().diffContent(req);

        Assert.assertEquals(
                "@@ -1 +1 @@\n"
                        + "-select 1 from dual\n"
                        + "\\ No newline at end of file\n"
                        + "+this is updated\n"
                        + "\\ No newline at end of file\n",
                diff.getDiff());
    }

    @Test
    public void test_resetHard_success() throws IOException, GitAPIException {
        Git git = getGit();
        String lastCommitId = git.log().call().iterator().next().getId().getName();

        FileUtils.write(new File(PROJECT_WORKDIR, "test.sql"), "this is updated", StandardCharsets.UTF_8);
        git.add().addFilepattern("test.sql").call();
        git.commit().setMessage("my commit").call();

        getOperator().resetHard(lastCommitId);

        Assert.assertEquals("select 1 from dual",
                FileUtils.readFileToString(new File(PROJECT_WORKDIR, "test.sql"), StandardCharsets.UTF_8));
    }

    @Test
    public void test_diffAndPatch_success() throws IOException, GitAPIException {
        Git git = getGit();
        GitClientOperator operator = getOperator();

        FileUtils.write(new File(PROJECT_WORKDIR, "test.sql"), "this is updated", StandardCharsets.UTF_8);
        git.add().addFilepattern("test.sql").call();

        String diff = operator.getDiffForPatch().toString();

        git.reset()
                .setMode(ResetType.HARD)
                .setRef("HEAD")
                .call();
        Assert.assertTrue(git.status().call().isClean());

        operator.applyDiff(new ByteArrayInputStream(diff.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("this is updated",
                FileUtils.readFileToString(new File(PROJECT_WORKDIR, "test.sql"), StandardCharsets.UTF_8));
    }

    private Git getGit() throws IOException {
        return Git.open(new File(PROJECT_WORKDIR));
    }

    private GitClientOperator getOperator() throws IOException {
        return new GitClientOperator(getGit());
    }

}
