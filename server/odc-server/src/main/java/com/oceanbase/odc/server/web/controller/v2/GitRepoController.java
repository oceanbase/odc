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

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.git.GitRepoService;
import com.oceanbase.odc.service.git.model.GitDiff;
import com.oceanbase.odc.service.git.model.GitReposOperationReq;
import com.oceanbase.odc.service.git.model.GitStatus;

import io.swagger.annotations.ApiOperation;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/12
 */
@RestController
@RequestMapping("/api/v2/project/{projectId}/repos")
public class GitRepoController {

    @Autowired
    private GitRepoService repoService;

    @ApiOperation(value = "listBranches", notes = "list git branches")
    @RequestMapping(value = "/{repoId}/branches", method = RequestMethod.GET)
    public ListResponse<String> listBranches(@PathVariable Long projectId, @PathVariable Long repoId)
            throws GitAPIException {
        return Responses.list(repoService.listBranches(projectId, repoId));
    }

    @ApiOperation(value = "createBranch", notes = "create branch")
    @RequestMapping(value = "/{repoId}/createBranch", method = RequestMethod.POST)
    public ListResponse<String> createBranch(@PathVariable Long projectId, @PathVariable Long repoId,
            @RequestBody GitReposOperationReq req) throws GitAPIException {
        return Responses.list(repoService.createBranch(projectId, repoId, req.getCheckoutBranch()));
    }

    @ApiOperation(value = "getStatus", notes = "get git status")
    @RequestMapping(value = "/{repoId}/status", method = RequestMethod.GET)
    public SuccessResponse<GitStatus> getStatus(@PathVariable Long projectId, @PathVariable Long repoId)
            throws GitAPIException, IOException {
        return Responses.success(repoService.getStatus(projectId, repoId));
    }

    @ApiOperation(value = "pull", notes = "pull from remote")
    @RequestMapping(value = "/{repoId}/pull", method = RequestMethod.POST)
    public SuccessResponse<GitStatus> pull(@PathVariable Long projectId, @PathVariable Long repoId)
            throws GitAPIException, IOException {
        return Responses.success(repoService.pull(projectId, repoId));
    }

    @ApiOperation(value = "checkout", notes = "checkout branch")
    @RequestMapping(value = "/{repoId}/checkout", method = RequestMethod.POST)
    public SuccessResponse<GitStatus> checkout(@PathVariable Long projectId, @PathVariable Long repoId,
            @RequestBody GitReposOperationReq req) throws GitAPIException, IOException {
        return Responses.success(repoService.checkout(projectId, repoId, req.getCheckoutBranch()));
    }

    @ApiOperation(value = "showDiff", notes = "show diff content")
    @RequestMapping(value = "/{repoId}/diff", method = RequestMethod.POST)
    public SuccessResponse<GitDiff> showDiff(@PathVariable Long projectId, @PathVariable Long repoId,
            @RequestBody GitDiff diff) throws GitAPIException, IOException {
        return Responses.success(repoService.diff(projectId, repoId, diff));
    }

    @ApiOperation(value = "commit", notes = "commit")
    @RequestMapping(value = "/{repoId}/commit", method = RequestMethod.POST)
    public SuccessResponse<GitStatus> commit(@PathVariable Long projectId, @PathVariable Long repoId,
            @RequestBody GitReposOperationReq req) throws GitAPIException, IOException {
        return Responses.success(repoService.commit(projectId, repoId, req.getCommitMessage()));
    }

}
