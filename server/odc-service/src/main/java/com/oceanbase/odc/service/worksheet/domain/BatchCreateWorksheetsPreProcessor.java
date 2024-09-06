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
package com.oceanbase.odc.service.worksheet.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq.UploadWorksheetTuple;

import lombok.Getter;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */

public class BatchCreateWorksheetsPreProcessor {
    @Getter
    private Path parentPath;
    @Getter
    private final Map<Path, UploadWorksheetTuple> createPathToObjectIdMap;

    public BatchCreateWorksheetsPreProcessor(Path createPath, String objectId, Long size) {
        createPathToObjectIdMap = new HashMap<>();
        this.parentPath = getParentPath(createPath, objectId);
        createPathToObjectIdMap.put(createPath,
                new UploadWorksheetTuple(createPath.getStandardPath(), objectId, size));
    }

    public BatchCreateWorksheetsPreProcessor(BatchUploadWorksheetsReq req) {
        createPathToObjectIdMap = new HashMap<>();
        UploadWorksheetTuple tupleGetParentPath = null;
        for (UploadWorksheetTuple tuple : req.getWorksheets()) {
            String pathStr = tuple.getPath();
            String objectId = tuple.getObjectId();
            Path path = new Path(pathStr);
            if (path.isFile()) {
                PreConditions.notBlank(objectId, "objectId");
                PreConditions.notNull(tuple.getSize(), "size");
            }
            Path parentPath = getParentPath(path, objectId);
            if (this.parentPath == null) {
                this.parentPath = parentPath;
                tupleGetParentPath = tuple;
            } else if (!this.parentPath.equals(parentPath)) {
                throw new IllegalArgumentException(
                        "different parent path, tuple1: " + tupleGetParentPath + ", "
                                + "tuple2: " + tuple);
            }
            if (createPathToObjectIdMap.containsKey(path)) {
                throw new BadRequestException(ErrorCodes.DuplicatedExists,
                        new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "name", path.getName()},
                        "duplicated path in request,create path: " + tuple.getPath());
            }
            createPathToObjectIdMap.put(path, tuple);
        }
    }

    public int size() {
        return createPathToObjectIdMap.size();
    }

    private static Path getParentPath(Path path, String objectId) {
        if (path.isSystemDefine()) {
            throw new IllegalArgumentException("invalid crate path : " + path);
        }
        Optional<Path> parentPathOptional = path.getParentPath();
        if (!parentPathOptional.isPresent()) {
            throw new IllegalArgumentException("invalid crate path : " + path);
        }
        return parentPathOptional.get();
    }
}
