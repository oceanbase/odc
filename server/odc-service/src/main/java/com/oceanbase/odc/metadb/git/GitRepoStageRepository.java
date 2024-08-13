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

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/7
 */
public interface GitRepoStageRepository extends OdcJpaRepository<GitRepositoryStageEntity, Long>,
        JpaSpecificationExecutor<GitRepositoryStageEntity> {

    GitRepositoryStageEntity findByOrganizationIdAndRepoIdAndUserId(Long organizationId, Long repoId, Long userId);

    @Modifying
    @Transactional
    @Query(value = "update GitRepositoryStageEntity as s set s.state=:#{#stage.state},s.branch=:#{#stage.branch},"
            + "s.lastCommitId=:#{#stage.lastCommitId},s.diffPatchStorage=:#{#stage.diffPatchStorage} "
            + "where s.id=:#{#stage.id}")
    int update(@Param("stage") GitRepositoryStageEntity stage);

    @Modifying
    @Transactional
    @Query(value = "update GitRepositoryStageEntity as s set s.state=:#{#stage.state},s.branch=:#{#stage.branch},"
            + "s.lastCommitId=:#{#stage.lastCommitId},s.diffPatchStorage=:#{#stage.diffPatchStorage} "
            + "where s.organizationId=:#{#stage.organizationId} and s.repoId=:#{#stage.repoId} and "
            + "s.userId=:#{#stage.userId}")
    int updateByOrganizationIdAndRepoIdAndUserId(@Param("stage") GitRepositoryStageEntity stage);

}
