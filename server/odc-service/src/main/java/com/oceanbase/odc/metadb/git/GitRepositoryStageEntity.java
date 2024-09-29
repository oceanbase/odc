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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.service.git.model.RepoState;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/8/7
 */
@Data
@Entity
@Table(name = "integration_git_repository_stage")
@NoArgsConstructor
@Accessors(chain = true)
public class GitRepositoryStageEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "description")
    private String description;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "repo_id", nullable = false)
    private Long repoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RepoState state;

    @Column(name = "branch")
    private String branch;

    @Column(name = "last_commit_id")
    private String lastCommitId;

    @Column(name = "diff_patch_storage")
    private String diffPatchStorage;

    @Column(name = "user_id", nullable = false)
    private Long userId;

}
