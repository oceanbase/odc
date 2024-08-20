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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataRepository;
import com.oceanbase.odc.metadb.worksheet.converter.WorksheetConverter;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.domain.WorksheetId;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author keyang
 * @date 2024/08/12
 * @since 4.3.2
 */
@Slf4j
@Component
public class WorksheetRepository {
    @Autowired
    private ObjectMetadataRepository metadataRepository;

    public Optional<Worksheet> findByProjectIdAndPath(Long projectId, Path path, String nameLike,
            boolean isAddWriteLock,
            boolean createDefaultIfNotExist, boolean loadSubFiles, boolean loadSameLevelFiles) {
        StringBuilder sql = new StringBuilder("SELECT * FROM objectstorage_object_metadata WHERE ");

        Map<String, Object> params = new HashMap<>();
        sql.append(" bucket_name = :bucketName");
        params.put("bucketName", WorksheetUtil.getBucketNameOfWorkSheets(projectId));

        Path queryPath = path;
        if (queryPath != null && loadSameLevelFiles) {
            queryPath = queryPath.getParentPath().orElseGet(() -> null);
        }
        if (queryPath != null) {
            sql.append(" AND object_name like :objectName");
            params.put("objectName", queryPath.getStandardPath() + "%");
        }
        if (StringUtils.isNotBlank(nameLike)) {
            sql.append(" AND object_name like :objectName");
            params.put("objectName", "%" + nameLike + "%");
        }
        if (isAddWriteLock) {
            sql.append(" FOR UPDATE");
        }
        Query query =
                metadataRepository.getEntityManager().createNativeQuery(sql.toString(), ObjectMetadataEntity.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<ObjectMetadataEntity> entities = query.getResultList();
        if (CollectionUtils.isEmpty(entities)) {
            entities = Collections.emptyList();
        }
        Worksheet worksheet = WorksheetConverter.toDomainFromEntities(entities, projectId, path,
                createDefaultIfNotExist, loadSubFiles, loadSameLevelFiles);
        return Optional.ofNullable(worksheet);
    }

    public List<Worksheet> listWithSubsByProjectIdAndPath(Long projectId, Path path) {
        PreConditions.notNull(path, "path");
        CriteriaBuilder criteriaBuilder = metadataRepository.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ObjectMetadataEntity> criteriaQuery = criteriaBuilder.createQuery(ObjectMetadataEntity.class);
        Root<ObjectMetadataEntity> root = criteriaQuery.from(ObjectMetadataEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates
                .add(criteriaBuilder.equal(root.get("bucketName"), WorksheetUtil.getBucketNameOfWorkSheets(projectId)));
        predicates.add(criteriaBuilder.like(root.get("objectName"), path.getStandardPath() + "%"));
        criteriaQuery.select(root).where(predicates.toArray(new Predicate[0]));
        TypedQuery<ObjectMetadataEntity> typedQuery = metadataRepository.getEntityManager().createQuery(criteriaQuery);
        List<ObjectMetadataEntity> resultList = typedQuery.getResultList();
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        return resultList.stream().map(WorksheetConverter::toDomain).collect(Collectors.toList());
    }

    public void batchAdd(Set<Worksheet> worksheets) {
        if (CollectionUtils.isEmpty(worksheets)) {
            return;
        }
        List<ObjectMetadataEntity> entities =
                worksheets.stream().map(WorksheetConverter::toEntity).collect(Collectors.toList());
        metadataRepository.saveAllAndFlush(entities);
        setEntityIdToDomain(worksheets, entities);
    }

    private static void setEntityIdToDomain(Set<Worksheet> worksheets, List<ObjectMetadataEntity> entities) {
        Map<WorksheetId, Long> worksheetIdToRowIdMap = entities.stream().collect(
                Collectors.toMap(o -> new WorksheetId(o.getBucketName(), o.getObjectName()),
                        ObjectMetadataEntity::getId, (o1, o2) -> o1));
        for (Worksheet worksheet : worksheets) {
            worksheet.setId(worksheetIdToRowIdMap.get(new WorksheetId(worksheet.getProjectId(), worksheet.getPath())));
        }
    }

    public void batchDelete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        metadataRepository.deleteAllById(ids);
    }

    public void batchUpdateById(Set<Worksheet> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        List<ObjectMetadataEntity> entities =
                files.stream().map(WorksheetConverter::toEntity).collect(Collectors.toList());
        metadataRepository.saveAllAndFlush(entities);
    }
}
