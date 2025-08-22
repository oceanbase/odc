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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryScheduleStatParams;
import com.oceanbase.odc.service.schedule.model.QueryScheduleTaskParams;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2023/5/16 11:38
 * @Descripition:
 */
@Repository
public interface ScheduleTaskRepository extends OdcJpaRepository<ScheduleTaskEntity, Long>,
        JpaSpecificationExecutor<ScheduleTaskEntity> {

    List<ScheduleTaskEntity> findByIdIn(Set<Long> id);

    Optional<ScheduleTaskEntity> findByIdAndJobName(Long id, String scheduleId);

    @Query(value = "select * from schedule_task where job_name=:jobName and job_group = :jobGroup order by id desc limit 1",
            nativeQuery = true)
    Optional<ScheduleTaskEntity> getLatestScheduleTaskByJobNameAndJobGroup(@Param("jobName") String jobName,
            @Param("jobGroup") String jobGroup);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.status = ?2 where st.id = ?1")
    int updateStatusById(Long id, TaskStatus status);

    @Transactional
    @Modifying
    @Query(value = "update schedule_task set status = :#{#newStatus.name()} where id = :id and status in (:previousStatus)",
            nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("newStatus") TaskStatus newStatus,
            @Param("previousStatus") List<String> previousStatus);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.status = ?2, st.progressPercentage = ?3 where st.id = ?1")
    int updateStatusAndProcessPercentageById(Long id, TaskStatus status, double progressPercentage);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.resultJson = ?2 where st.id = ?1")
    int updateTaskResult(Long id, String resultJson);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.parametersJson = ?2 where st.id = ?1")
    int updateTaskParameters(Long id, String parametersJson);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.executor = ?2 where st.id = ?1")
    int updateExecutor(Long id, String executor);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.jobId = ?2 where st.id = ?1")
    int updateJobIdById(Long id, Long jobId);

    List<ScheduleTaskEntity> findByJobId(Long jobId);

    @Query(value = "select st.* from schedule_task st where st.job_name in (:jobNames)", nativeQuery = true)
    List<ScheduleTaskEntity> findByJobNames(@Param("jobNames") Set<String> jobNames);


    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.parametersJson=:#{#entity.parametersJson},"
            + "st.status=:#{#entity.status},st.progressPercentage=:#{#entity.progressPercentage},"
            + "st.resultJson=:#{#entity.resultJson} where st.id=:#{#entity.id}")
    int update(@Param("entity") ScheduleTaskEntity entity);

    default Page<ScheduleTaskEntity> find(@NotNull Pageable pageable, @NotNull QueryScheduleTaskParams params) {
        Specification<ScheduleTaskEntity> specification = Specification
                .where(OdcJpaRepository.between(ScheduleTaskEntity_.createTime, params.getStartTime(),
                        params.getEndTime()))
                .and(OdcJpaRepository.eq(ScheduleTaskEntity_.id, params.getId()))
                .and(OdcJpaRepository.in(ScheduleTaskEntity_.status, params.getStatuses()))
                .and(OdcJpaRepository.in(ScheduleTaskEntity_.jobName, params.getScheduleIds()))
                .and(OdcJpaRepository.in(ScheduleTaskEntity_.jobGroup, params.getJobGroups()));
        return findAll(specification, pageable);
    }

    /**
     * {@code
     *     select job_group, status, count(*)
     *     from schedule_task
     *          where job_name in (select CAST(id as CHAR) from schedule_schedule where condition...)
     *     where condition...
     *     group by job_group, status
     * }
     */
    default List<ScheduleTaskTypeAndStatusCount> findScheduleTaskStatusCountBySubQuery(
            @NonNull QueryScheduleStatParams params,
            @NonNull Specification<ScheduleEntity> scheduleIdSubQuerySpec) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        Specification<ScheduleTaskEntity> scheduleTaskSpec =
                OdcJpaRepository
                        .in(ScheduleTaskEntity_.jobGroup,
                                params.getScheduleTaskTypes().stream().map(Enum::name).collect(
                                        Collectors.toSet()))
                        .and(OdcJpaRepository.between(ScheduleTaskEntity_.createTime, params.getStartTime(),
                                params.getEndTime()))
                        .and(ScheduleTaskSpecs.groupByJobGroupAndStatus());

        CriteriaQuery<ScheduleTaskTypeAndStatusCount> query = cb.createQuery(ScheduleTaskTypeAndStatusCount.class);
        Root<ScheduleTaskEntity> root = query.from(ScheduleTaskEntity.class);

        Subquery<String> subQuery = query.subquery(String.class);
        Root<ScheduleEntity> subRoot = subQuery.from(ScheduleEntity.class);
        subQuery.select(subRoot.get(ScheduleTaskEntity_.ID).as(String.class))
                .where(scheduleIdSubQuerySpec.toPredicate(subRoot, query, cb));

        query.where(cb.and(scheduleTaskSpec.toPredicate(root, query, cb),
                root.get(ScheduleTaskEntity_.JOB_NAME)
                        .in(subQuery)));
        query.select(cb.construct(
                ScheduleTaskTypeAndStatusCount.class,
                root.get(ScheduleTaskEntity_.JOB_GROUP),
                root.get(ScheduleTaskEntity_.STATUS),
                cb.count(root)));
        return entityManager.createQuery(query).getResultList();
    }

    @Data
    class ScheduleTaskTypeAndStatusCount {
        private ScheduleTaskType scheduleTaskType;
        private TaskStatus scheduleStatus;
        private Long count;

        public ScheduleTaskTypeAndStatusCount(String jobGroup, TaskStatus scheduleStatus, Long count) {
            this.scheduleTaskType = ScheduleTaskType.valueOf(jobGroup);
            this.scheduleStatus = scheduleStatus;
            this.count = count;
        }
    }

    default List<ScheduleTaskProjection> findScheduleIdAndStatus(@NotNull QueryScheduleTaskParams params) {
        NamedParameterJdbcTemplate jdbcTemplate = getNamedParameterJdbcTemplate();
        MapSqlParameterSource parameterMap = new MapSqlParameterSource();
        String baseQuery = "SELECT job_name, job_group, status FROM schedule_task";

        List<String> conditions = new ArrayList<>();

        if (params.getStartTime() != null) {
            conditions.add("create_time >= :startTime");
            parameterMap.addValue("startTime", params.getStartTime());
        }
        if (params.getEndTime() != null) {
            conditions.add("create_time <= :endTime");
            parameterMap.addValue("endTime", params.getEndTime());
        }
        if (CollectionUtils.isNotEmpty(params.getJobGroups())) {
            conditions.add("job_group IN (:jobGroups)");
            parameterMap.addValue("jobGroups", params.getJobGroups());
        }

        // Avoid scanning all record of schedule_task table
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one query condition is required to avoid fetching all schedule tasks.");
        }

        String finalQuery = baseQuery + " WHERE " + String.join(" AND ", conditions);

        return jdbcTemplate.query(finalQuery, parameterMap,
                (rs, count) -> new ScheduleTaskProjection(Long.valueOf(rs.getString(1)), rs.getString(2),
                        TaskStatus.valueOf(rs.getString(3))));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ScheduleTaskProjection {
        private Long scheduleId;
        private String jobGroup;
        private TaskStatus status;
    }

}
