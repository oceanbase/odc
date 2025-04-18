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
package com.oceanbase.odc.metadb.iam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.shared.constant.OrganizationType;

public interface OrganizationRepository
        extends JpaRepository<OrganizationEntity, Long>, JpaSpecificationExecutor<OrganizationEntity> {

    Optional<OrganizationEntity> findByUniqueIdentifier(String uniqueIdentifier);

    List<OrganizationEntity> findByUniqueIdentifierIn(Collection<String> uniqueIdentifiers);

    Optional<OrganizationEntity> findByName(String name);

    @Query(value = "select t1.* from iam_organization t1 inner join iam_user_organization t2 on t1.id=t2.organization_id where t2.user_id=:userId",
            nativeQuery = true)
    List<OrganizationEntity> findByUserId(@Param("userId") Long userId);

    @Query(value = "select 1 from iam_organization t1 inner join iam_user_organization t2 on t1.id=t2.organization_id where t1.type=:#{#type.name()} and t2.user_id=:userId LIMIT 1",
            nativeQuery = true)
    Integer existsByTypeAndUserId(@Param("type") OrganizationType type, @Param("userId") Long userId);

    @Query(value = "select t1.* from iam_organization t1 inner join iam_user_organization t2 on t1.id=t2.organization_id where t1.type=:#{#type.name()} and t2.user_id=:userId",
            nativeQuery = true)
    List<OrganizationEntity> findByTypeAndUserId(@Param("type") OrganizationType type, @Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("update OrganizationEntity as e set e.name=:name where e.id=:id")
    int updateOrganizationNameById(@Param("id") Long id, @Param("name") String name);

    @Query("select id from OrganizationEntity where type = ?1")
    List<Long> findIdByType(OrganizationType type);

}
