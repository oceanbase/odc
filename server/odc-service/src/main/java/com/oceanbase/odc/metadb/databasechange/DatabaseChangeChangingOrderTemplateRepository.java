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
package com.oceanbase.odc.metadb.databasechange;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseChangeChangingOrderTemplateRepository
        extends JpaRepository<DatabaseChangeChangingOrderTemplateEntity, Long>,
        JpaSpecificationExecutor<DatabaseChangeChangingOrderTemplateEntity> {

    Boolean existsByNameAndProjectId(String name, Long projectId);

    Optional<DatabaseChangeChangingOrderTemplateEntity> findByIdAndProjectId(Long id, Long projectId);

    List<DatabaseChangeChangingOrderTemplateEntity> findByNameAndProjectId(String name, Long projectId);

    void deleteByIdAndProjectId(Long id, Long projectId);


    Page<DatabaseChangeChangingOrderTemplateEntity> findByProjectId(Long projectId, Pageable pageable);
}
