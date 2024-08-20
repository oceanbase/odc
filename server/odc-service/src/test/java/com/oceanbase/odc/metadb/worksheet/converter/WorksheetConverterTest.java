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
package com.oceanbase.odc.metadb.worksheet.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

/**
 * @author keyang
 * @date 2024/08/13
 * @since 4.3.2
 */
public class WorksheetConverterTest {
    final static Long projectId = 1L;

    int id = 0;

    private ObjectMetadataEntity newEntity(Long projectId, String objectName, Long updatedAt) {
        ObjectMetadataEntity entity = new ObjectMetadataEntity();
        entity.setId(id++);
        entity.setBucketName(WorksheetUtil.getBucketNameOfWorkSheets(projectId));
        entity.setObjectName(objectName);
        entity.setCreatorId(1L);
        entity.setVersion(0L);
        entity.setObjectId(id++ + "");
        Date date = new Date(updatedAt);
        entity.setCreateTime(date);
        entity.setUpdateTime(date);
        return entity;
    }

    @Test
    public void toDomainFromEntities_EmptyList() {
        Path path = new Path("/Worksheets/");
        Worksheet result =
                WorksheetConverter.toDomainFromEntities(new ArrayList<>(), projectId, path, true,
                        true, true);
        assertNull(result);

        result =
                WorksheetConverter.toDomainFromEntities(new ArrayList<>(), projectId, path, false,
                        true, true);
        assertNull(result);

        result =
                WorksheetConverter.toDomainFromEntities(new ArrayList<>(), projectId, null, true,
                        true, true);
        assertNull(result);

    }

    @Test
    public void toDomainFromEntities_SelfMatch() {
        List<ObjectMetadataEntity> entities = Arrays.asList(
                newEntity(projectId, "/Worksheets/file1", 1L));
        Path path = new Path("/Worksheets/file1");
        Worksheet result = WorksheetConverter.toDomainFromEntities(entities, projectId, path, false,
                true, true);

        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(path, result.getPath());
        assert CollectionUtils.isEmpty(result.getSubWorksheets());
        assert CollectionUtils.isEmpty(result.getSameParentAtPrevLevelWorksheets());
    }



    @Test
    public void toDomainFromEntities_SubAndSameLevel() {
        ObjectMetadataEntity selfEntity = newEntity(projectId, "/Worksheets/folder1/", 2L);

        ObjectMetadataEntity subEntity1 = newEntity(projectId, "/Worksheets/folder1/file1", 2L);
        ObjectMetadataEntity subEntity2 = newEntity(projectId, "/Worksheets/folder1/file2", 2L);
        ObjectMetadataEntity subEntity3 = newEntity(projectId, "/Worksheets/folder1/subfolder1/", 1L);
        ObjectMetadataEntity subEntity4 = newEntity(projectId, "/Worksheets/folder1/subfolder1/file2", 1L);

        ObjectMetadataEntity sameLevelEntity1 = newEntity(projectId, "/Worksheets/folder2/", 2L);
        ObjectMetadataEntity sameLevelEntity2 = newEntity(projectId, "/Worksheets/folder3/", 1L);
        List<ObjectMetadataEntity> entities = Arrays.asList(
                newEntity(projectId, "/Worksheets/", 1L),
                newEntity(projectId, "/Worksheets/folder1/", 1L),
                selfEntity,
                newEntity(projectId, "/Worksheets/folder1/file1", 1L),
                subEntity1,
                newEntity(projectId, "/Worksheets/folder1/file2", 1L),
                subEntity2,
                subEntity3,
                subEntity4,
                sameLevelEntity1,
                newEntity(projectId, "/Worksheets/folder2/", 1L),
                sameLevelEntity2);

        Path path = new Path("/Worksheets/folder1/");
        Worksheet result = WorksheetConverter.toDomainFromEntities(entities, projectId, path, false,
                true, true);

        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(path, result.getPath());
        assert CollectionUtils.isEqualCollection(
                Arrays.asList(subEntity1, subEntity2, subEntity3, subEntity4).stream()
                        .map(WorksheetConverter::toDomain).collect(Collectors.toSet()),
                result.getSubWorksheets());
        assert CollectionUtils.isEqualCollection(
                Arrays.asList(sameLevelEntity1, sameLevelEntity2).stream()
                        .map(WorksheetConverter::toDomain).collect(Collectors.toSet()),
                result.getSameParentAtPrevLevelWorksheets());

        result = WorksheetConverter.toDomainFromEntities(entities, projectId, path, false,
                false, true);
        assert CollectionUtils.isEmpty(result.getSubWorksheets());
        assert CollectionUtils.isEqualCollection(
                Arrays.asList(sameLevelEntity1, sameLevelEntity2).stream()
                        .map(WorksheetConverter::toDomain).collect(Collectors.toSet()),
                result.getSameParentAtPrevLevelWorksheets());

        result = WorksheetConverter.toDomainFromEntities(entities, projectId, path, false,
                false, false);
        assert CollectionUtils.isEmpty(result.getSubWorksheets());
        assert CollectionUtils.isEmpty(result.getSameParentAtPrevLevelWorksheets());
    }
}
