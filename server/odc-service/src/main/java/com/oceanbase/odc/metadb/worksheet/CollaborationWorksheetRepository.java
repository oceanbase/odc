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
package com.oceanbase.odc.metadb.worksheet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.common.jdbc.JdbcTemplateUtils;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.ResourceType;

public interface CollaborationWorksheetRepository extends JpaRepository<CollaborationWorksheetEntity, Long>,
        JpaSpecificationExecutor<CollaborationWorksheetEntity>, OdcJpaRepository<CollaborationWorksheetEntity, Long> {
    Optional<CollaborationWorksheetEntity> findByOrganizationIdAndProjectIdAndWorkspaceIdAndPath(Long organizationId,
            Long projectId, Long workspaceId, String path);

    @Query("SELECT c FROM CollaborationWorksheetEntity c WHERE c.organizationId = :organizationId AND c.projectId = :projectId "
            + "AND c.workspaceId = :workspaceId AND c.path IN :paths")
    List<CollaborationWorksheetEntity> findByOrganizationIdAndProjectIdAndWorkspaceIdAndInPaths(
            @Param("organizationId") Long organizationId,
            @Param("projectId") Long projectId, @Param("workspaceId") Long workspaceId,
            @Param("paths") List<String> paths);

    /**
     * Fuzzy query based on path and need to handle some filtering conditions
     *
     * @param organizationId organization id
     * @param projectId project id
     * @param workspaceId workspaceId value
     * @param path find path
     * @param minLevelNumberFilter the minimum level number of the path，if the is null or less/equal to
     *        0,it will not be filtered
     * @param maxLevelNumberFilter the maximum level number of the path，if the is null or less/equal to
     *        0,it will not be filtered
     * @param nameLikeFilter path should contain the name, if the is null or empty,it will not be
     *        filtered
     * @return CollaborationWorksheetEntity list
     */
    default List<CollaborationWorksheetEntity> findByPathLikeWithFilter(Long organizationId, Long projectId,
            Long workspaceId, String path,
            Integer minLevelNumberFilter, Integer maxLevelNumberFilter, String nameLikeFilter) {
        Specification<CollaborationWorksheetEntity> specs =
                getSpecs(organizationId, projectId, workspaceId, path, minLevelNumberFilter, maxLevelNumberFilter,
                        nameLikeFilter);
        return findAll(specs);
    }

    default Page<CollaborationWorksheetEntity> leftJoinResourceLastAccess(Long organizationId, Long projectId,
            Long workspaceId, Long userId, Pageable pageable) {
        String orderColumn = "rl.last_access_time";
        String direction = "DESC";
        // Set sorting parameters based on pageable
        if (!pageable.getSort().isEmpty()) {
            Order order = pageable.getSort().iterator().next();

            if (order.getProperty().equals("updateTime")) {
                orderColumn = "ws.update_time";
            }
            direction = order.getDirection().name();
        }

        String sql = String.format("SELECT ws.*, rl.last_access_time "
                + "FROM collaboration_worksheet ws "
                + "LEFT OUTER JOIN history_resource_last_access rl "
                + "ON rl.organization_id=ws.organization_id AND rl.project_id=ws.project_id AND rl.resource_id = ws.id "
                + "AND rl.user_id = ?4 AND rl.resource_type = ?5 "
                + "WHERE ws.organization_id = ?1 AND ws.project_id = ?2 AND ws.workspace_id = ?3 "
                + "ORDER BY %s %s, ws.id %s", orderColumn, direction, direction);

        javax.persistence.Query nativeQuery =
                getEntityManager().createNativeQuery(sql, CollaborationWorksheetEntity.class);

        // Set parameters
        nativeQuery.setParameter(1, organizationId);
        nativeQuery.setParameter(2, projectId);
        nativeQuery.setParameter(3, workspaceId);
        nativeQuery.setParameter(4, userId);
        nativeQuery.setParameter(5, ResourceType.ODC_WORKSHEET.name());

        // Set pagination parameters
        nativeQuery.setFirstResult((int) pageable.getOffset());
        nativeQuery.setMaxResults(pageable.getPageSize());

        // Execute query and process results
        List<CollaborationWorksheetEntity> resultList = nativeQuery.getResultList();

        // Count total items
        long total = countByOrganizationIdAndProjectIdAndWorkspaceId(organizationId, projectId, workspaceId);

        return new PageImpl<>(resultList, pageable, total);
    }

    long countByOrganizationIdAndProjectIdAndWorkspaceId(Long organizationId, Long projectId, Long workspaceId);

    long countByOrganizationIdAndProjectId(Long organizationId, Long projectId);

    default long countByPathLikeWithFilter(Long organizationId, Long projectId,
            Long workspaceId, String path,
            Integer minLevelNumberFilter, Integer maxLevelNumberFilter, String nameLikeFilter) {
        Specification<CollaborationWorksheetEntity> specs =
                getSpecs(organizationId, projectId, workspaceId, path, minLevelNumberFilter, maxLevelNumberFilter,
                        nameLikeFilter);
        return count(specs);
    }

    static Specification<CollaborationWorksheetEntity> getSpecs(Long organizationId, Long projectId,
            Long workspaceId, String path,
            Integer minLevelNumberFilter, Integer maxLevelNumberFilter, String nameLikeFilter) {
        Specification<CollaborationWorksheetEntity> specs = Specification
                .where(OdcJpaRepository.eq(CollaborationWorksheetEntity_.organizationId, organizationId))
                .and(OdcJpaRepository.eq(CollaborationWorksheetEntity_.projectId, projectId))
                .and(OdcJpaRepository.eq(CollaborationWorksheetEntity_.workspaceId, workspaceId))
                .and(OdcJpaRepository.startsWith(CollaborationWorksheetEntity_.path, path));
        boolean min = minLevelNumberFilter != null && minLevelNumberFilter > 0;
        boolean max = maxLevelNumberFilter != null && maxLevelNumberFilter > 0;
        if (min && max) {
            if (minLevelNumberFilter.equals(maxLevelNumberFilter)) {
                // This line of code does not use 'between' in order to use index:idx_project_id_level_num_path
                // and improve query performance
                specs = specs.and(OdcJpaRepository.eq(CollaborationWorksheetEntity_.pathLevel, maxLevelNumberFilter));
            } else if (minLevelNumberFilter < maxLevelNumberFilter) {
                specs = specs
                        .and(OdcJpaRepository.between(CollaborationWorksheetEntity_.pathLevel, minLevelNumberFilter,
                                maxLevelNumberFilter));
            }
        } else if (min) {
            specs = specs.and(OdcJpaRepository.gte(CollaborationWorksheetEntity_.pathLevel, minLevelNumberFilter));
        } else if (max) {
            specs = specs.and(OdcJpaRepository.lte(CollaborationWorksheetEntity_.pathLevel, maxLevelNumberFilter));
        }
        if (nameLikeFilter != null && !nameLikeFilter.isEmpty()) {
            specs = specs.and(OdcJpaRepository.like(CollaborationWorksheetEntity_.path, nameLikeFilter));
        }
        return specs;
    }

    @Modifying
    @Transactional
    @Query("DELETE CollaborationWorksheetEntity c WHERE"
            + " c.organizationId = :organizationId AND c.projectId = :projectId AND c.workspaceId = :workspaceId"
            + " AND c.path LIKE CONCAT(:path,'%')")
    int batchDeleteByPathLike(@Param("organizationId") Long organizationId,
            @Param("projectId") Long projectId, @Param("workspaceId") Long workspaceId, @Param("path") String path);

    /**
     * only for test
     * 
     * @param organizationId
     * @param projectId
     * @return
     */
    @Transactional
    int deleteByOrganizationIdAndProjectId(@Param("organizationId") Long organizationId,
            @Param("projectId") Long projectId);

    @Modifying
    @Transactional
    @Query("UPDATE CollaborationWorksheetEntity c SET "
            + " c.objectId = :#{#entity.objectId},c.size = :#{#entity.size},c.version = c.version + 1,c.updateTime=:#{#entity.updateTime}"
            + " WHERE c.id = :#{#entity.id} AND c.version = :#{#entity.version}")
    int updateContentByIdAndVersion(@Param("entity") CollaborationWorksheetEntity entity);

    /**
     * batch update path,not update value of other columns,especially columns such as version and
     * object_id.
     *
     * @param idToPathMap id to new path map
     * @return update rows count
     */
    default int batchUpdatePath(Map<Long, String> idToPathMap) {
        String sql = "UPDATE " + CollaborationWorksheetEntity.TABLE_NAME + " SET "
                + CollaborationWorksheetEntity_.PATH + "=? WHERE " + CollaborationWorksheetEntity_.ID + "=?";
        List<Object[]> params = idToPathMap.entrySet().stream()
                .map(entry -> new Object[] {entry.getValue(), entry.getKey()})
                .collect(Collectors.toList());

        int[] updateCounts = getJdbcTemplate().batchUpdate(sql, params);
        return JdbcTemplateUtils.batchInsertAffectRows(updateCounts);
    }
}
