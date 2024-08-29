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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface CollaborationWorksheetRepository extends JpaRepository<CollaborationWorksheetEntity, Long>,
        JpaSpecificationExecutor<CollaborationWorksheetEntity>, OdcJpaRepository<CollaborationWorksheetEntity, Long> {

    Optional<CollaborationWorksheetEntity> findByProjectIdAndPath(Long projectId, String path);

    @Query("SELECT c FROM CollaborationWorksheetEntity c WHERE c.projectId = :projectId AND c.path IN :paths")
    List<CollaborationWorksheetEntity> findByProjectIdAndInPaths(@Param("projectId") Long projectId,
            @Param("paths") List<String> paths);

    /**
     * Fuzzy query based on path and need to handle some filtering conditions
     *
     * @param projectId project
     * @param path find path
     * @param minLevelNumberFilter the minimum level number of the path，if the is null or less/equal to
     *        0,it will not be filtered
     * @param maxLevelNumberFilter the maximum level number of the path，if the is null or less/equal to
     *        0,it will not be filtered
     * @param nameLikeFilter path should contain the name, if the is null or empty,it will not be
     *        filtered
     * @return CollaborationWorksheetEntity list
     */
    default List<CollaborationWorksheetEntity> findByPathLikeWithFilter(Long projectId, String path,
            Integer minLevelNumberFilter, Integer maxLevelNumberFilter, String nameLikeFilter) {
        Specification<CollaborationWorksheetEntity> specs =
                getSpecs(projectId, path, minLevelNumberFilter, maxLevelNumberFilter,
                        nameLikeFilter);
        return findAll(specs);
    }

    default long countByPathLikeWithFilter(Long projectId, String path,
            Integer minLevelNumberFilter, Integer maxLevelNumberFilter, String nameLikeFilter) {
        Specification<CollaborationWorksheetEntity> specs =
                getSpecs(projectId, path, minLevelNumberFilter, maxLevelNumberFilter,
                        nameLikeFilter);
        return count(specs);
    }

    static Specification<CollaborationWorksheetEntity> getSpecs(Long projectId,
            String path, Integer minLevelNumberFilter, Integer maxLevelNumberFilter, String nameLikeFilter) {
        Specification<CollaborationWorksheetEntity> specs = Specification
                .where(OdcJpaRepository.eq(CollaborationWorksheetEntity_.projectId, projectId))
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
            + " c.projectId = :projectId AND c.path LIKE CONCAT(:path,'%')")
    int batchDeleteByPathLike(@Param("projectId") Long projectId, @Param("path") String path);

    /**
     * only for test
     * 
     * @param projectId
     * @return
     */
    @Transactional
    int deleteByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Transactional
    @Query("UPDATE CollaborationWorksheetEntity c SET "
            + " c.objectId = :#{#entity.objectId},c.size = :#{#entity.size},c.version = c.version + 1"
            + " WHERE c.id = :#{#entity.id} AND c.version = :#{#entity.version}")
    int updateContentByIdAndVersion(@Param("entity") CollaborationWorksheetEntity entity);

    /**
     * batch update path,not update value of other columns,especially columns such as version and
     * object_id.
     *
     * @param idToPathMap id to new path map
     * @return update rows count
     */
    @Transactional
    default int batchUpdatePath(Map<Long, String> idToPathMap) {
        if (CollectionUtils.isEmpty(idToPathMap)) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("UPDATE " + CollaborationWorksheetEntity.TABLE_NAME + " SET "
                + CollaborationWorksheetEntity_.PATH + " = CASE " + CollaborationWorksheetEntity_.ID + " ");
        List<Object> params = new ArrayList<>();

        idToPathMap.forEach((id, newValue) -> {
            sql.append("WHEN ").append(id).append(" THEN ? ");
            params.add(newValue);
        });

        sql.append(" END WHERE " + CollaborationWorksheetEntity_.ID + " IN (");
        for (int i = 0; i < idToPathMap.size(); i++) {
            sql.append("?");
            if (i < idToPathMap.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");

        params.addAll(idToPathMap.keySet());

        javax.persistence.Query nativeQuery = getEntityManager().createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            nativeQuery.setParameter(i + 1, params.get(i));
        }
        return nativeQuery.executeUpdate();
    }
}
