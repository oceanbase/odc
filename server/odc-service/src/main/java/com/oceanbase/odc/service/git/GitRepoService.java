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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.git.GitRepoRepository;
import com.oceanbase.odc.service.git.model.GitDiff;
import com.oceanbase.odc.service.git.model.GitRepository;
import com.oceanbase.odc.service.git.model.GitStatus;
import com.oceanbase.odc.service.git.repo.GitRepositoryManager;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/8
 */
@Service
@Slf4j
@Validated
@SkipAuthorize("internal authenticated")
public class GitRepoService {

    @Autowired
    private GitRepositoryManager repositoryManager;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private GitRepoRepository gitRepoRepository;
    @Autowired
    private GitIntegrationService gitIntegrationService;

    public Set<String> listBranches(Long projectId, Long repoId) throws GitAPIException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        return operator.listBranches();
    }

    public Set<String> createBranch(Long projectId, Long repoId, String branch) throws GitAPIException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        operator.createBranch(branch);
        return operator.listBranches();
    }

    public Set<String> deleteBranch(Long projectId, Long repoId, String branch)
            throws GitAPIException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        operator.deleteBranch(branch);
        return operator.listBranches();
    }

    public GitStatus getStatus(Long projectId, Long repoId) throws GitAPIException, IOException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        return operator.status();
    }

    public GitStatus pull(Long projectId, Long repoId) throws IOException, GitAPIException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        try {
            operator.pull(operator.currentBranch());
        } catch (CheckoutConflictException e) {
            throw new IllegalStateException(e.getMessage());
        }
        return operator.status();
    }

    public GitStatus checkout(Long projectId, Long repoId, String branch) throws GitAPIException, IOException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        try {
            operator.checkout(branch);
        } catch (RefNotFoundException e) {
            try {
                operator.checkout("origin/" + branch);
            } catch (RefNotFoundException ex) {
                throw new BadRequestException(String.format("Branch %s not found in local or remote", branch));
            }
        }
        return operator.status();
    }

    public GitDiff diff(Long projectId, Long repoId, GitDiff diff) throws IOException, GitAPIException {
        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        return operator.diffContent(diff);
    }

    public GitStatus commit(Long projectId, Long repoId, String message) throws GitAPIException, IOException {
        GitRepository repo = gitIntegrationService.detail(repoId);

        GitClientOperator operator = repositoryManager.getOperator(repoId, authenticationFacade.currentUserId());
        operator.add(".");
        operator.commit(message, repo.getEmail());
        List<Status> pushResults = operator.push(repo.getEmail(), repo.getPersonalAccessToken());
        if (pushResults.stream().anyMatch(status -> status != Status.OK && status != Status.UP_TO_DATE)) {
            if (pushResults.stream().anyMatch(status -> status == Status.REJECTED_NONFASTFORWARD)) {
                // push rejected, need merge
                PullResult pullResult = operator.pull(operator.currentBranch());
                if (!pullResult.isSuccessful()) {
                    log.warn("pull failed, detail:[fetchResult:{}, mergeResult:{}]",
                            pullResult.getFetchResult().getMessages(), pullResult.getMergeResult().toString());
                }
            } else {
                throw new IllegalStateException(
                        "there are some failed pushes, status: " + pushResults + ", please retry later");
            }
        }
        return operator.status();
    }

}
