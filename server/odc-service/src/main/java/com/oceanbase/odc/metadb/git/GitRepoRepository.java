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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

}
