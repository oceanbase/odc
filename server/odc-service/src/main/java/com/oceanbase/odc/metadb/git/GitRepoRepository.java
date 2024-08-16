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
package com.oceanbase.odc.metadb.git;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.git.model.QueryGitRepositoryParams;

import lombok.NonNull;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/29
 */
public interface GitRepoRepository extends OdcJpaRepository<GitRepositoryEntity, Long>,
        JpaSpecificationExecutor<GitRepositoryEntity> {

    List<GitRepositoryEntity> findByOrganizationIdAndProjectId(Long organizationId, Long projectId);

    default Page<GitRepositoryEntity> find(@NonNull QueryGitRepositoryParams params, @NonNull Pageable pageable) {
        Specification<GitRepositoryEntity> specs = Specification
                .where(OdcJpaRepository.eq(GitRepositoryEntity_.organizationId, params.getOrganizationId()))
                .and(OdcJpaRepository.eq(GitRepositoryEntity_.projectId, params.getProjectId()))
                .and(OdcJpaRepository.like(GitRepositoryEntity_.name, params.getFuzzyName()))
                .and(OdcJpaRepository.in(GitRepositoryEntity_.providerType, params.getProviders()));
        return findAll(specs, pageable);
    }

    default List<GitRepositoryEntity> batchCreate(@NonNull List<GitRepositoryEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("integration_git_repository")
                .field(GitRepositoryEntity_.creatorId)
                .field(GitRepositoryEntity_.organizationId)
                .field(GitRepositoryEntity_.projectId)
                .field(GitRepositoryEntity_.name)
                .field(GitRepositoryEntity_.description)
                .field(GitRepositoryEntity_.providerType)
                .field(GitRepositoryEntity_.providerUrl)
                .field(GitRepositoryEntity_.sshAddress)
                .field(GitRepositoryEntity_.cloneAddress)
                .field(GitRepositoryEntity_.email)
                .field(GitRepositoryEntity_.personalAccessToken)
                .field(GitRepositoryEntity_.salt)
                .build();
        List<Function<GitRepositoryEntity, Object>> getter = valueGetterBuilder()
                .add(GitRepositoryEntity::getCreatorId)
                .add(GitRepositoryEntity::getOrganizationId)
                .add(GitRepositoryEntity::getProjectId)
                .add(GitRepositoryEntity::getName)
                .add(GitRepositoryEntity::getDescription)
                .add(e -> e.getProviderType().name())
                .add(GitRepositoryEntity::getProviderUrl)
                .add(GitRepositoryEntity::getSshAddress)
                .add(GitRepositoryEntity::getCloneAddress)
                .add(GitRepositoryEntity::getEmail)
                .add(GitRepositoryEntity::getPersonalAccessToken)
                .add(GitRepositoryEntity::getSalt)
                .build();
        return batchCreate(entities, sql, getter, GitRepositoryEntity::setId);
    }

}
