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
package com.oceanbase.odc.service.git.repo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.git.GitRepoRepository;
import com.oceanbase.odc.metadb.git.GitRepoStageRepository;
import com.oceanbase.odc.metadb.git.GitRepositoryEntity;
import com.oceanbase.odc.metadb.git.GitRepositoryStageEntity;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.git.GitClientOperator;
import com.oceanbase.odc.service.git.GitIntegrationService;
import com.oceanbase.odc.service.git.model.GitIntegrationProperties;
import com.oceanbase.odc.service.git.model.RepoState;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/7
 */
@Slf4j
@Component
public class GitRepositoryManager {
    private static final String LOCK_FORMAT = "repo-lock-%s-%s";
    private static final String GIT_REPO_DIR =
            Paths.get(MoreObjects.firstNonNull(SystemUtils.getEnvOrProperty("file.storage.dir"), "./data"), "repos")
                    .toString();

    @Autowired
    private GitRepoRepository gitRepoRepository;
    @Autowired
    private GitRepoStageRepository stageRepository;
    @Autowired
    private EncryptionFacade encryptionFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private CloudObjectStorageService objectStorageService;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;
    @Autowired
    private GitIntegrationService integrationService;

    /**
     * Use {@code repoId}、{@code userId} to identify repository
     */
    private final LoadingCache<Pair<Long, Long>, GitClientOperator> operatorCache;

    public GitRepositoryManager(@Autowired GitIntegrationProperties gitIntegrationProperties) {
        operatorCache = Caffeine.newBuilder()
                .maximumSize(gitIntegrationProperties.getGitRepositoryMaxCachedSize())
                .expireAfterAccess(Duration.ofMinutes(gitIntegrationProperties.getGitRepositoryPreserveMinutes()))
                .removalListener((pair, operator, cause) -> {
                    if (operator != null) {
                        deleteRepo((GitClientOperator) operator);
                        ((GitClientOperator) operator).close();
                    }
                })
                .build(this::createGitOperator);
    }

    @PostConstruct
    public void init() {
        try {
            FileUtils.cleanDirectory(new File(GIT_REPO_DIR));
        } catch (Exception e) {
            log.warn("failed to clean git repo directory {}", GIT_REPO_DIR);
        }
    }

    public GitClientOperator getOperator(Long repoId, Long userId) {
        return operatorCache.get(new Pair<>(repoId, userId));
    }

    public void storeRepo(GitClientOperator operator, Long repoId, Long userId) throws GitAPIException, IOException {
        Lock lock = tryLock(repoId, userId);
        try {
            String objectKey = null;
            try (ByteArrayOutputStream diffStream = operator.getDiffForPatch()) {
                byte[] bytes = diffStream.toByteArray();
                if (bytes.length == 0) {
                    return;
                }
                if (Objects.nonNull(objectStorageService) && objectStorageService.supported()) {
                    objectKey = objectStorageService.uploadTemp("repo-" + repoId + "-" + userId,
                            new ByteArrayInputStream(bytes));
                }
            }
            GitRepositoryStageEntity stageInfo = new GitRepositoryStageEntity();
            stageInfo.setBranch(operator.currentBranch())
                    .setLastCommitId(operator.lastCommitId())
                    .setDiffPatchStorage(objectKey)
                    .setState(RepoState.UNCOMMITTED)
                    .setOrganizationId(authenticationFacade.currentOrganizationId())
                    .setRepoId(repoId)
                    .setUserId(userId);
            stageRepository.updateByOrganizationIdAndRepoIdAndUserId(stageInfo);
        } finally {
            lock.unlock();
        }
    }

    private GitClientOperator createGitOperator(Pair<Long, Long> pair) throws IOException, GitAPIException {
        File repo = getRepoDir(pair.left, pair.right);
        FileUtils.forceMkdir(repo);
        String[] children = repo.list();
        if (children.length > 0) {
            FileUtils.cleanDirectory(repo);
        }
        return initRepo(repo, pair.left, pair.right);
    }

    private GitClientOperator initRepo(File dir, Long repoId, Long userId) throws GitAPIException, IOException {
        Lock lock = tryLock(repoId, userId);
        RepoState originalState;
        GitRepositoryStageEntity stageInfo;
        try {
            stageInfo =
                    stageRepository.findByOrganizationIdAndRepoIdAndUserId(authenticationFacade.currentOrganizationId(),
                            repoId, userId);
            if (stageInfo == null) {
                originalState = RepoState.CLEAN;
                stageInfo = newStageInfo(repoId, userId);
                stageRepository.save(stageInfo);
            } else {
                Verify.verify(stageInfo.getState() != RepoState.INITIALIZING, "repo is initializing");
                originalState = stageInfo.getState();
                stageInfo.setState(RepoState.INITIALIZING);
                stageRepository.update(stageInfo);
            }
        } finally {
            lock.unlock();
        }

        try {
            GitRepositoryEntity entity = gitRepoRepository.findById(repoId)
                    .orElseThrow(() -> new NotFoundException(ResourceType.ODC_GIT_REPOSITORY, "repository.id", repoId));
            TextEncryptor encryptor = encryptionFacade.organizationEncryptor(entity.getOrganizationId(),
                    entity.getSalt());
            String token = encryptor.decrypt(entity.getPersonalAccessToken());

            GitClientOperator operator = GitClientOperator
                    .cloneRepo(entity.getCloneAddress(), dir, entity.getEmail(), token);
            if (originalState == RepoState.UNCOMMITTED) {
                restoreRepo(stageInfo, operator);
                stageInfo.setState(originalState);
            } else {
                stageInfo.setState(RepoState.CLEAN);
            }
            stageRepository.save(stageInfo);
            return operator;
        } catch (Exception e) {
            stageInfo.setState(originalState);
            stageRepository.update(stageInfo);
            throw e;
        }
    }

    private void restoreRepo(GitRepositoryStageEntity stageInfo, GitClientOperator operator)
            throws GitAPIException, IOException {
        Verify.notNull(stageInfo.getBranch(), "stage.branch");
        Verify.notNull(stageInfo.getLastCommitId(), "stage.lastCommitId");

        operator.checkout(stageInfo.getBranch());
        operator.resetHard(stageInfo.getLastCommitId());

        if (StringUtils.isEmpty(stageInfo.getDiffPatchStorage())) {
            byte[] diff = objectStorageService.readContent(stageInfo.getDiffPatchStorage());
            operator.applyDiff(new ByteArrayInputStream(diff));
            operator.add(".");
        }
    }

    private void deleteRepo(GitClientOperator operator) {
        FileUtils.deleteQuietly(operator.getRepoDir());
    }

    private File getRepoDir(Long repoId, Long userId) {
        return Paths.get(GIT_REPO_DIR, repoId + "", userId + "").toFile();
    }

    private Lock tryLock(Long repoId, Long userId) {
        Lock lock = jdbcLockRegistry.obtain(String.format(LOCK_FORMAT, repoId, userId));
        if (!lock.tryLock()) {
            throw new IllegalStateException("repo is initializing");
        }
        return lock;
    }

    private GitRepositoryStageEntity newStageInfo(Long repoId, Long userId) {
        GitRepositoryStageEntity stageInfo = new GitRepositoryStageEntity();
        stageInfo.setState(RepoState.INITIALIZING)
                .setOrganizationId(authenticationFacade.currentOrganizationId())
                .setRepoId(repoId)
                .setUserId(userId);
        return stageInfo;
    }

}
