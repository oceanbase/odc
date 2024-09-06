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
package com.oceanbase.odc.metadb.resourcehistory;

import java.util.Optional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import lombok.NonNull;

@Repository
public interface ResourceLastAccessRepository extends JpaRepository<ResourceLastAccessEntity, Long>,
        JpaSpecificationExecutor<ResourceLastAccessEntity> {
    Optional<ResourceLastAccessEntity> findByOrganizationIdAndProjectIdAndUserIdAndResourceTypeAndResourceId(
            @NonNull Long organizationId, @NonNull Long projectId,
            @NonNull Long userId, @NotBlank String resourceType, @NonNull Long resourceId);

    Page<ResourceLastAccessEntity> findByOrganizationIdAndProjectIdAndUserIdAndResourceType(
            @NonNull Long organizationId, @NonNull Long projectId,
            @NonNull Long userId, @NotEmpty String resourceType,
            @NonNull Pageable pageable);
}
