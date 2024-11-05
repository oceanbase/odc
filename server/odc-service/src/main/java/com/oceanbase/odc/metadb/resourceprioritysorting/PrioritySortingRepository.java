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
package com.oceanbase.odc.metadb.resourceprioritysorting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.resourceprioritysorting.model.SortedResourceId;
import com.oceanbase.odc.service.resourceprioritysorting.model.SortedResourceType;

@Repository
public interface PrioritySortingRepository extends OdcJpaRepository<PrioritySortingEntity, Long>,
        JpaRepository<PrioritySortingEntity, Long>, JpaSpecificationExecutor<PrioritySortingEntity> {

    default Page<PrioritySortingEntity> pageBySortedResourceTypeAndSortByPriorityDesc(
            SortedResourceType sortedResourceType,
            Integer pageNum, Integer pageSize) {
        Specification<PrioritySortingEntity> specs = Specification
                .where(OdcJpaRepository.eq(PrioritySortingEntity_.resourceType, sortedResourceType.getResourceType()))
                .and(OdcJpaRepository.eq(PrioritySortingEntity_.resourceId, sortedResourceType.getResourceId()))
                .and(OdcJpaRepository.eq(PrioritySortingEntity_.sortedResourceType,
                        sortedResourceType.getSortedResourceType()));
        PageRequest pageRequest = PageRequest.of(pageNum, pageSize, Direction.DESC, PrioritySortingEntity_.PRIORITY);
        return findAll(specs, pageRequest);
    }

    @Query("SELECT c.* from PrioritySortingEntity c WHERE"
            + " c.resourceType = :#{#params.resourceType} AND c.resourceId = :#{#params.resourceId}"
            + " AND c.sortedResourceType = :#{#params.sortedResourceType}"
            + " AND c.sortedResourceId = :#{#params.sortedResourceId}")
    Optional<PrioritySortingEntity> findBySortedResourceId(@Param("params") SortedResourceId sortedResourceId);

    @Query("SELECT c.* from PrioritySortingEntity c WHERE"
            + " c.resourceType = :#{#params.resourceType} AND c.resourceId = :#{#params.resourceId}"
            + " AND c.sortedResourceType = :#{#params.sortedResourceType}")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PrioritySortingEntity> listAllInSortedResourceTypeWithWriteLock(
            @Param("params") SortedResourceType sortedResourceType);

    @Query("SELECT c.* from PrioritySortingEntity c WHERE"
            + " c.resourceType = :#{#params.resourceType} AND c.resourceId = :#{#params.resourceId}"
            + " AND c.sortedResourceType = :#{#params.sortedResourceType}"
            + " AND c.priority < :priority ORDER BY c.priority DESC limit 1")
    Optional<PrioritySortingEntity> maxPriorityInSortedResourceTypeLessThanPriority(
            @Param("params") SortedResourceType sortedResourceType,
            @Param("priority") Long priority);

    @Query("SELECT c.priority from PrioritySortingEntity c WHERE"
            + " c.resourceType = :#{#params.resourceType} AND c.resourceId = :#{#params.resourceId}"
            + " AND c.sortedResourceType = :#{#params.sortedResourceType}"
            + " ORDER BY c.priority desc limit 1")
    Optional<PrioritySortingEntity> maxPriorityInSortedResourceType(
            @Param("params") SortedResourceType sortedResourceType);


    @Query("SELECT c.* from PrioritySortingEntity c WHERE"
            + " c.resourceType = :#{#params.resourceType} AND c.resourceId = :#{#params.resourceId}"
            + " AND c.sortedResourceType = :#{#params.sortedResourceType}"
            + " AND c.sortedResourceId in :sortedResourceIds")
    List<PrioritySortingEntity> listBySortedResourceIds(
            @Param("params") SortedResourceType sortedResourceType,
            @Param("sortedResourceIds") Collection<Long> sortedResourceIds);

    @Modifying
    @Transactional
    @Query("DELETE PrioritySortingEntity c WHERE"
            + " c.resourceType = :#{#params.resourceType} AND c.resourceId = :#{#params.resourceId}"
            + " AND c.sortedResourceType = :#{#params.sortedResourceType}"
            + " AND c.sortedResourceId in :sortedResourceIds")
    int bathDeleteBySortedResources(
            @Param("params") SortedResourceType sortedResourceType,
            @Param("sortedResourceIds") Collection<Long> sortedResourceIds);

    @Transactional
    int deleteByResourceTypeAndResourceId(String resourceType, Long resourceId);
}
