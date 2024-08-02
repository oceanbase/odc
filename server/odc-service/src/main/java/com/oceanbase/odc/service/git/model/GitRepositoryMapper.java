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
package com.oceanbase.odc.service.git.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.git.GitRepositoryEntity;
import com.oceanbase.odc.service.encryption.EncryptionFacade;

import lombok.NonNull;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/29
 */
@Component
public class GitRepositoryMapper {

    @Autowired
    private EncryptionFacade encryptionFacade;

    public GitRepository entityToModel(GitRepositoryEntity entity) {
        GitRepository repo = new GitRepository();
        repo.setId(entity.getId());
        repo.setCreateTime(entity.getCreateTime());
        repo.setUpdateTime(entity.getUpdateTime());
        repo.setCreatorId(entity.getCreatorId());
        repo.setOrganizationId(entity.getOrganizationId());
        repo.setProjectId(entity.getProjectId());
        repo.setName(entity.getName());
        repo.setDescription(entity.getDescription());
        repo.setProviderType(entity.getProviderType());
        repo.setProviderUrl(entity.getProviderUrl());
        repo.setSshUrl(entity.getSshUrl());
        repo.setCloneUrl(entity.getCloneUrl());
        repo.setEmail(entity.getEmail());

        TextEncryptor encryptor = getEncryptor(entity.getOrganizationId(), entity.getSalt());
        PreConditions.notNull(encryptor, "encryptor");
        repo.setToken(encryptor.decrypt(entity.getToken()));

        return repo;
    }

    public GitRepositoryEntity modelToEntity(GitRepository repo) {
        GitRepositoryEntity entity = new GitRepositoryEntity();
        entity.setId(repo.getId());
        entity.setCreateTime(repo.getCreateTime());
        entity.setUpdateTime(repo.getUpdateTime());
        entity.setCreatorId(repo.getCreatorId());
        entity.setOrganizationId(repo.getOrganizationId());
        entity.setProjectId(repo.getProjectId());
        entity.setName(repo.getName());
        entity.setDescription(repo.getDescription());
        entity.setProviderType(repo.getProviderType());
        entity.setProviderUrl(repo.getProviderUrl());
        entity.setSshUrl(repo.getSshUrl());
        entity.setCloneUrl(repo.getCloneUrl());
        entity.setEmail(repo.getEmail());

        String salt = encryptionFacade.generateSalt();
        TextEncryptor encryptor = getEncryptor(repo.getOrganizationId(), salt);
        PreConditions.notNull(encryptor, "encryptor");
        entity.setToken(encryptor.encrypt(repo.getToken()));
        entity.setSalt(salt);

        return entity;
    }

    TextEncryptor getEncryptor(@NonNull Long organizationId, @NonNull String salt) {
        return encryptionFacade.organizationEncryptor(organizationId, salt);
    }

}
