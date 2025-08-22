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
package com.oceanbase.odc.metadb.schedule;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryScheduleStatParams;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2022/11/16 14:51
 * @Descripition:
 */
public interface ScheduleRepository extends OdcJpaRepository<ScheduleEntity, Long> {

    @Query(value = "select * from schedule_schedule where connection_id in (:connectionIds) and status = 'ENABLED'",
            nativeQuery = true)
    List<ScheduleEntity> getEnabledScheduleByConnectionIds(@Param("connectionIds") Set<Long> connectionIds);

    List<ScheduleEntity> findByIdIn(Collection<Long> ids);

    @Transactional
    @Modifying
    @Query(value = "update schedule_schedule set status = :#{#status.name()} "
            + "where id=:id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") ScheduleStatus status);

    Long countByStatus(ScheduleStatus status);

    @Transactional
    @Modifying
    @Query(value = "update schedule_schedule set job_parameters_json = :jobParametersJson "
            + "where id=:id", nativeQuery = true)
    int updateJobParametersById(@Param("id") Long id, @Param("jobParametersJson") String jobParametersJson);

    @Transactional
    @Modifying
    @Query(value = "update schedule_schedule set latest_schedule_changelog_id = :latestScheduleChangeLogId "
            + "where id=:id", nativeQuery = true)
    int updateLatestScheduleChangeLogIdById(@Param("id") Long id,
            @Param("latestScheduleChangeLogId") Long latestScheduleChangeLogId);

    @Query(value = "select count(1) from schedule_schedule where project_id=:projectId and status in ('ENABLED', 'CREATING', 'APPROVING', 'PAUSE')",
            nativeQuery = true)
    int getEnabledScheduleCountByProjectId(@Param("projectId") Long projectId);

    default List<ScheduleTypeCount> findScheduleTypeCount(@NonNull QueryScheduleStatParams params) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ScheduleTypeCount> query = cb.createQuery(ScheduleTypeCount.class);
        Root<ScheduleEntity> root = query.from(ScheduleEntity.class);

        query.where(buildWithScheduleTypeAndStatusSpec(params).toPredicate(root, query, cb));
        query.select(cb.construct(
                ScheduleTypeCount.class,
                root.get(ScheduleEntity_.TYPE),
                cb.count(root)));
        return entityManager.createQuery(query).getResultList();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class ScheduleTypeCount {
        private ScheduleType scheduleType;
        private Long count;
    }

    default Specification<ScheduleEntity> buildWithScheduleTypeAndStatusSpec(@NonNull QueryScheduleStatParams params) {
        return OdcJpaRepository.in(ScheduleEntity_.type, params.getScheduleTypes())
                .and(OdcJpaRepository.in(ScheduleEntity_.status, params.getStatuses()))
                .and(ScheduleSpecs.groupByScheduleType());
    }

    default Page<ScheduleEntity> find(@NotNull Pageable pageable, @NotNull QueryScheduleParams params) {
        Specification<ScheduleEntity> specification = Specification
                .where(OdcJpaRepository.between(ScheduleEntity_.createTime, params.getStartTime(), params.getEndTime()))
                .and(OdcJpaRepository.in(ScheduleEntity_.dataSourceId, params.getDataSourceIds()))
                .and(OdcJpaRepository.eq(ScheduleEntity_.databaseName, params.getDatabaseName()))
                .and(OdcJpaRepository.eq(ScheduleEntity_.type, params.getType()))
                .and(OdcJpaRepository.in(ScheduleEntity_.projectId, params.getProjectIds()))
                .and(OdcJpaRepository.eq(ScheduleEntity_.id, params.getId()))
                .and(OdcJpaRepository.in(ScheduleEntity_.status, params.getStatuses()))
                .and(OdcJpaRepository.notEq(ScheduleEntity_.status, ScheduleStatus.DELETED))
                .and(OdcJpaRepository.in(ScheduleEntity_.creatorId, params.getCreatorIds()))
                .and(OdcJpaRepository.like(ScheduleEntity_.name, params.getName()))
                .and(OdcJpaRepository.eq(ScheduleEntity_.organizationId, params.getOrganizationId()))
                .and(OdcJpaRepository.eq(ScheduleEntity_.isInner, Boolean.FALSE));
        return findAll(specification, pageable);
    }

    default Page<ScheduleEntity> findWithJoinScheduleChangeLog(@NotNull Pageable pageable,
            @NotNull QueryScheduleParams params) {
        return findAll(ScheduleSpecs.joinScheduleChangeLog(params), pageable);
    }

    default Long countDistinctId(@NonNull Specification<ScheduleEntity> spec) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ScheduleEntity> root = query.from(ScheduleEntity.class);

        query.select(cb.countDistinct(root.get(ScheduleEntity_.ID))).where(spec.toPredicate(root, query, cb));

        return entityManager.createQuery(query).getSingleResult();
    }

    List<ScheduleEntity> findByOrganizationIdAndIdInAndProjectIdIn(Long organizationId, Collection<Long> ids,
            Collection<Long> projectIds);

    List<ScheduleEntity> findByIdInAndProjectIdIn(Collection<Long> ids, Collection<Long> projectIds);

    List<ScheduleEntity> findByOrganizationIdAndProjectIdInAndTypeIn(Long organizationId, Collection<Long> projectIds,
            Collection<ScheduleType> types);
}
