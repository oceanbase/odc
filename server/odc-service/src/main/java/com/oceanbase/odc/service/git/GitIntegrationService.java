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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.git.GitRepoRepository;
import com.oceanbase.odc.metadb.git.GitRepositoryEntity;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.git.model.GitProvider;
import com.oceanbase.odc.service.git.model.GitRepository;
import com.oceanbase.odc.service.git.model.GitRepositoryMapper;
import com.oceanbase.odc.service.git.model.QueryGitRepositoryParams;
import com.oceanbase.odc.service.git.vcs.VcsFacadeMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/29
 */
@Service
@Slf4j
@Validated
@SkipAuthorize("internal authenticated")
public class GitIntegrationService {

    @Autowired
    private GitRepoRepository gitRepoRepository;
    @Autowired
    private VcsFacadeMapper vcsFacadeMapper;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private EncryptionFacade encryptionFacade;
    private GitRepositoryMapper gitRepositoryMapper = GitRepositoryMapper.INSTANCE;

    public Page<GitRepository> list(@NotNull Long projectId, @NotNull QueryGitRepositoryParams params,
            @NotNull Pageable pageable) {
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        return gitRepoRepository.find(params, pageable).map(this::entityToModel);
    }

    public List<GitRepository> listCandidates(@NotNull GitProvider provider) {
        return vcsFacadeMapper.getVcsFacade(provider).listRepositories(provider.getPersonalAccessToken());
    }

    public GitRepository detail(@NotNull Long id) {
        return entityToModel(nullSafeGet(id));
    }

    public List<GitRepository> batchCreate(@NotNull Long projectId, @NotNull List<GitRepository> repositories) {
        long organizationId = authenticationFacade.currentOrganizationId();
        long creatorId = authenticationFacade.currentUserId();

        Set<String> reposInMeta = gitRepoRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
                .stream().map(GitRepositoryEntity::getSshAddress).collect(Collectors.toSet());
        repositories.forEach(r -> {
            if (reposInMeta.contains(r.getSshAddress())) {
                throw new BadRequestException(String.format("repository %s already exists", r.getSshAddress()));
            }
        });

        List<GitRepositoryEntity> entities = repositories.stream()
                .map(r -> {
                    GitRepositoryEntity entity = modelToEntity(r);
                    entity.setProjectId(projectId);
                    entity.setCreatorId(creatorId);
                    entity.setOrganizationId(organizationId);
                    return entity;
                })
                .collect(Collectors.toList());
        List<GitRepositoryEntity> saved = gitRepoRepository.batchCreate(entities);

        log.info("batch created git repositories, projectId={}, size={}", projectId, saved.size());
        return saved.stream().map(this::entityToModel).collect(Collectors.toList());
    }

    public GitRepository update(@NotNull Long projectId, @NotNull Long repoId, @NotNull GitProvider repository) {
        PreConditions.notNull(repository.getEmail(), "repository.email");
        PreConditions.notNull(repository.getPersonalAccessToken(), "repository.token");
        GitRepositoryEntity entity = nullSafeGet(repoId);
        entity.setEmail(repository.getEmail());
        TextEncryptor encryptor = getEncryptor(entity.getOrganizationId(), entity.getSalt());
        entity.setPersonalAccessToken(encryptor.encrypt(repository.getPersonalAccessToken()));
        GitRepositoryEntity updated = gitRepoRepository.saveAndFlush(entity);

        log.info("updated git repository, id={}", updated.getId());
        return entityToModel(updated);
    }

    /**
     * Adaptive multi-cloud is required.
     * 
     * @param organizationId
     * @param oldSecret
     * @param newSecret
     */
    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public void attachedUpdateGitPersonalToken(@NotNull Long organizationId, String oldSecret, String newSecret) {
        List<GitRepositoryEntity> entities = gitRepoRepository.findByOrganizationId(organizationId);
        if (entities.isEmpty()) {
            return;
        }
        List<GitRepositoryEntity> saved = entities.stream()
                .map(entity -> migrateTokenFromOld2New(entity, oldSecret, newSecret))
                .collect(Collectors.toList());
        gitRepoRepository.saveAllAndFlush(saved);
        int affectedRows = saved.size();

        log.info("attached update git repository from organization config completed, total={}", affectedRows);
    }

    public GitRepository delete(@NotNull Long projectId, @NotNull Long id) {
        GitRepositoryEntity entity = nullSafeGet(id);
        gitRepoRepository.delete(entity);

        log.info("deleted git repository, id={}", id);
        return entityToModel(entity);
    }

    private GitRepositoryEntity nullSafeGet(Long id) {
        return gitRepoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_GIT_REPOSITORY, "repository.id", id));
    }

    private GitRepository entityToModel(GitRepositoryEntity entity) {
        GitRepository repo = gitRepositoryMapper.entityToModel(entity);
        TextEncryptor encryptor = getEncryptor(entity.getOrganizationId(), entity.getSalt());
        PreConditions.notNull(encryptor, "encryptor");
        repo.setPersonalAccessToken(encryptor.decrypt(entity.getPersonalAccessToken()));
        return repo;
    }

    private GitRepositoryEntity modelToEntity(GitRepository repo) {
        GitRepositoryEntity entity = gitRepositoryMapper.modelToEntity(repo);
        String salt = encryptionFacade.generateSalt();
        Long organizationId = Objects.isNull(repo.getOrganizationId()) ? authenticationFacade.currentOrganizationId()
                : repo.getOrganizationId();
        TextEncryptor encryptor = getEncryptor(organizationId, salt);
        PreConditions.notNull(encryptor, "encryptor");
        entity.setPersonalAccessToken(encryptor.encrypt(repo.getPersonalAccessToken()));
        entity.setSalt(salt);
        return entity;
    }

    private GitRepositoryEntity migrateTokenFromOld2New(GitRepositoryEntity entity, String oldSecret,
            String newSecret) {
        TextEncryptor encryptor = encryptionFacade.passwordEncryptor(oldSecret, entity.getSalt());
        String rawToken = encryptor.decrypt(entity.getPersonalAccessToken());
        String reEncodeToken = attachedEncodeToken(rawToken, entity.getSalt(), newSecret);
        entity.setPersonalAccessToken(reEncodeToken);
        return entity;
    }

    private String attachedEncodeToken(String rawToken, String salt, String organizationSecret) {
        if (rawToken == null) {
            return null;
        }
        TextEncryptor encryptor = encryptionFacade.passwordEncryptor(organizationSecret, salt);
        return encryptor.encrypt(rawToken);
    }

    TextEncryptor getEncryptor(@NonNull Long organizationId, @NonNull String salt) {
        return encryptionFacade.organizationEncryptor(organizationId, salt);
    }

}
