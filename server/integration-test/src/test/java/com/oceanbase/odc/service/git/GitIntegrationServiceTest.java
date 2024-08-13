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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.git.GitRepoRepository;
import com.oceanbase.odc.metadb.git.GitRepositoryEntity;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.git.model.GitRepository;
import com.oceanbase.odc.service.git.model.GitRepositoryMapper;
import com.oceanbase.odc.service.git.model.QueryGitRepositoryParams;
import com.oceanbase.odc.service.git.model.VcsProvider;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.model.Organization;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/1
 */
public class GitIntegrationServiceTest extends AuthorityTestEnv {
    private GitRepositoryMapper gitRepositoryMapper = GitRepositoryMapper.INSTANCE;

    @Autowired
    private GitRepoRepository gitRepoRepository;
    @Autowired
    private GitIntegrationService integrationService;
    @Autowired
    private EncryptionFacade encryptionFacade;
    @MockBean
    private OrganizationService organizationService;

    private Long repoId;

    @Before
    public void setUp() {
        Organization organization = new Organization();
        organization.setSecret(PasswordUtils.random(32));
        Mockito.when(organizationService.get(Mockito.any())).thenReturn(Optional.of(organization));
        GitRepositoryEntity saved = gitRepoRepository.saveAndFlush(modelToEntity(getRepo()));
        repoId = saved.getId();
    }

    @After
    public void tearDown() {
        gitRepoRepository.deleteAll();
    }

    @Test
    public void test_list_success() {
        Page<GitRepository> repos = integrationService.list(1L, QueryGitRepositoryParams.builder().build(),
                Pageable.unpaged());
        Assert.assertEquals(1, repos.getSize());
    }

    @Test
    public void test_detail_success() {
        GitRepository repo = integrationService.detail(repoId);
        Assert.assertEquals("******", repo.getPersonalAccessToken());
    }

    @Test
    public void test_batchCreate_success() {
        List<GitRepository> repos = new ArrayList<>();
        GitRepository r2 = getRepo();
        r2.setSshAddress("git@github.com:test/git-repo-2.git");
        repos.add(r2);
        GitRepository r3 = getRepo();
        r3.setSshAddress("git@github.com:test/git-repo-3.git");
        repos.add(r3);
        integrationService.batchCreate(1L, repos);

        Assert.assertEquals(3, gitRepoRepository.findAll().size());
    }

    @Test
    public void test_update_success() {
        GitRepository repo = getRepo();
        repo.setEmail("test1@oceanbase.com");
        integrationService.update(1L, repoId, repo);

        GitRepositoryEntity entity = gitRepoRepository.findById(repoId).get();
        Assert.assertEquals("test1@oceanbase.com", entity.getEmail());
    }

    @Test
    public void test_delete_success() {
        integrationService.delete(1L, repoId);
        Assert.assertEquals(0, gitRepoRepository.findAll().size());
    }

    private GitRepository getRepo() {
        GitRepository repo = new GitRepository();
        repo.setCreatorId(1L);
        repo.setOrganizationId(1L);
        repo.setProjectId(1L);
        repo.setName("git-repo-1");
        repo.setProviderType(VcsProvider.GITHUB);
        repo.setProviderUrl("https://github.com");
        repo.setCloneAddress("https://github.com/test/git-repo-1.git");
        repo.setSshAddress("git@github.com:test/git-repo-1.git");
        repo.setEmail("test@oceanbase.com");
        repo.setPersonalAccessToken("******");
        return repo;
    }

    private GitRepositoryEntity modelToEntity(GitRepository repo) {
        GitRepositoryEntity entity = gitRepositoryMapper.modelToEntity(repo);
        String salt = encryptionFacade.generateSalt();
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(repo.getOrganizationId(), salt);
        PreConditions.notNull(encryptor, "encryptor");
        entity.setPersonalAccessToken(encryptor.encrypt(repo.getPersonalAccessToken()));
        entity.setSalt(salt);
        return entity;
    }

}
