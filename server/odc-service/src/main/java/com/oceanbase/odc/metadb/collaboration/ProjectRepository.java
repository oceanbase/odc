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
package com.oceanbase.odc.metadb.collaboration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long>, JpaSpecificationExecutor<ProjectEntity> {
    Optional<ProjectEntity> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<ProjectEntity> findByNameAndOrganizationId(String name, Long organizationId);

    List<ProjectEntity> findByIdIn(Collection<Long> ids);

    List<ProjectEntity> findAllByOrganizationId(Long organizationId);

    Optional<ProjectEntity> findByUniqueIdentifier(String uniqueIdentifier);

    List<ProjectEntity> findByUniqueIdentifierIn(Collection<String> uniqueIdentifiers);
}
