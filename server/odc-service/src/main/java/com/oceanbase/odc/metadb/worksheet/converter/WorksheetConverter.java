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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.service.objectstorage.model.ObjectUploadStatus;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author keyang
 * @date 2024/08/13
 * @since 4.3.2
 */
@Slf4j
public class WorksheetConverter {
    public static Worksheet toDomain(ObjectMetadataEntity entity) {
        return new Worksheet(entity.getId(), entity.getCreateTime(), entity.getUpdateTime(),
                WorksheetUtil.getProjectIdOfWorkSheets(entity.getBucketName()),
                new Path(entity.getObjectName()), entity.getCreatorId(),
                entity.getVersion(), entity.getObjectId(), null, null);
    }

    public static Worksheet toDomainFromEntities(List<ObjectMetadataEntity> entities, Long projectId,
            Path path, boolean createTempIfSelfNotExist,
            boolean loadSub, boolean loadSameDirectParent) {
        Map<Path, Worksheet> sameParentAtPrevLevelMap = new HashMap<>();
        Map<Path, Worksheet> subLevelMap = new HashMap<>();
        Worksheet self = null;
        for (ObjectMetadataEntity entity : entities) {
            try {
                Path entityPath = new Path(entity.getObjectName());
                boolean isSubLevel = path == null || entityPath.isChildOfAny(path);
                boolean isSamePrevParent = path != null && entityPath.isSameParentAtPrevLevel(path);
                boolean isSelf = entityPath.equals(path);
                if (isSubLevel && loadSub) {
                    Worksheet worksheet = toDomain(entity);
                    Worksheet existWorksheet = subLevelMap.get(entityPath);
                    if (existWorksheet == null ||
                            existWorksheet.getUpdateTime().before(worksheet.getUpdateTime())) {
                        subLevelMap.put(entityPath, worksheet);
                    }
                }
                if (!isSelf && !isSubLevel && isSamePrevParent && loadSameDirectParent) {
                    Worksheet worksheet = toDomain(entity);
                    Worksheet existWorksheet = sameParentAtPrevLevelMap.get(entityPath);
                    if (existWorksheet == null ||
                            existWorksheet.getUpdateTime().before(worksheet.getUpdateTime())) {
                        sameParentAtPrevLevelMap.put(entityPath, worksheet);
                    }
                }
                if (isSelf) {
                    Worksheet selfCandidate = toDomain(entity);
                    if (self == null || self.getUpdateTime().before(selfCandidate.getUpdateTime())) {
                        self = selfCandidate;
                    }
                }
            } catch (Throwable e) {
                log.warn("Illegal ObjectMetadataEntity format,convert to worksheet error,{}",
                        JsonUtils.toJson(entity), e);
            }
        }
        if (self == null && createTempIfSelfNotExist) {
            self = Worksheet.of(projectId, path == null ? Path.root() : path, null, null);
        }
        if (self == null) {
            return null;
        }
        if (loadSub) {
            self.setSubWorksheets(new HashSet<>(subLevelMap.values()));
        }
        if (loadSameDirectParent) {
            self.setSameDirectParentWorksheets(new HashSet<>(sameParentAtPrevLevelMap.values()));
        }
        return self;
    }

    public static ObjectMetadataEntity toEntity(Worksheet worksheet) {
        ObjectMetadataEntity entity = new ObjectMetadataEntity();
        if (worksheet.getId() != null) {
            entity.setId(worksheet.getId());
        }
        entity.setBucketName(WorksheetUtil.getBucketNameOfWorkSheets(worksheet.getProjectId()));
        entity.setObjectName(worksheet.getPath().toString());
        entity.setCreatorId(worksheet.getCreatorId());
        entity.setVersion(worksheet.getVersion());
        entity.setObjectId(worksheet.getObjectId());
        entity.setStatus(ObjectUploadStatus.FINISHED);
        return entity;
    }
}
