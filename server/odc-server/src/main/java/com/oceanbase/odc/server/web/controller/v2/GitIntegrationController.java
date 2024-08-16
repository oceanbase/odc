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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.git.GitIntegrationService;
import com.oceanbase.odc.service.git.model.GitProvider;
import com.oceanbase.odc.service.git.model.GitRepository;
import com.oceanbase.odc.service.git.model.QueryGitRepositoryParams;
import com.oceanbase.odc.service.git.model.VcsProvider;

import io.swagger.annotations.ApiOperation;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/9
 */
@RestController
@RequestMapping("/api/v2/collaboration/projects/{projectId}/gitRepos")
public class GitIntegrationController {

    @Autowired
    private GitIntegrationService integrationService;

    @ApiOperation(value = "listRepositories", notes = "List all repositories in the project")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public PaginatedResponse<GitRepository> listRepositories(@PathVariable Long projectId,
            @PageableDefault(size = Integer.MAX_VALUE, sort = "id") Pageable pageable,
            @RequestParam(required = false, name = "fuzzyName") String fuzzyName,
            @RequestParam(required = false, name = "provider") List<VcsProvider> providers) {
        QueryGitRepositoryParams params = QueryGitRepositoryParams.builder()
                .fuzzyName(fuzzyName)
                .providers(providers)
                .projectId(projectId)
                .build();
        return Responses.paginated(integrationService.list(projectId, params, pageable));
    }

    @ApiOperation(value = "listCandidateRepositories", notes = "List all candidate repos from vsc provider")
    @RequestMapping(value = "/listCandidateRepositories", method = RequestMethod.POST)
    public List<GitRepository> listCandidateRepositories(@PathVariable Long projectId,
            @RequestBody GitProvider provider) {
        return integrationService.listCandidates(provider);
    }

    @ApiOperation(value = "batchCreate", notes = "Batch create repositories")
    @RequestMapping(value = "/batchCreate", method = RequestMethod.POST)
    public List<GitRepository> batchCreate(@PathVariable Long projectId,
            @RequestBody List<GitRepository> repositories) {
        return integrationService.batchCreate(projectId, repositories);
    }

    @ApiOperation(value = "updateRepository", notes = "Update repository")
    @RequestMapping(value = "/{repoId}", method = RequestMethod.PUT)
    public GitRepository updateRepository(@PathVariable Long projectId, @PathVariable Long repoId,
            @RequestBody GitProvider provider) {
        return integrationService.update(projectId, repoId, provider);
    }

    @ApiOperation(value = "deleteRepository", notes = "Delete repository")
    @RequestMapping(value = "/{repoId}", method = RequestMethod.DELETE)
    public GitRepository deleteReposiory(@PathVariable Long projectId, @PathVariable Long repoId) {
        return integrationService.delete(projectId, repoId);
    }

}
