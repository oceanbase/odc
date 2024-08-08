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

import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.service.worksheet.exceptions.NameDuplicatedException;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq.UploadWorksheetTuple;

import lombok.Getter;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */

public class BatchCreateWorksheets {
    @Getter
    private Path parentPath;
    /**
     *
     * This is mainly used to report more details when an error occurs
     */
    private UploadWorksheetTuple tupleGetParentPath;
    @Getter
    private final Map<Path, String> createPathToObjectIdMap;

    public BatchCreateWorksheets(BatchUploadWorksheetsReq req) {
        createPathToObjectIdMap = new HashMap<>();
        for (UploadWorksheetTuple tuple : req.getWorksheets()) {
            String pathStr = tuple.getPath();
            String objectId = tuple.getObjectId();
            Path path = new Path(pathStr);
            if (path.isFile() && StringUtils.isBlank(objectId)) {
                throw new IllegalArgumentException("invalid UploadProjectFileTuple : " + tuple);
            }
            Optional<Path> parentPathOptional = path.getParentPath();
            if (!parentPathOptional.isPresent()) {
                throw new IllegalArgumentException("invalid UploadProjectFileTuple : " + tuple);
            }
            if (this.parentPath == null) {
                this.parentPath = parentPathOptional.get();
                this.tupleGetParentPath = tuple;
            } else if (!this.parentPath.equals(parentPathOptional.get())) {
                throw new IllegalArgumentException(
                        "different parent path, tuple1: " + tupleGetParentPath + ", "
                                + "tuple2: " + tuple);
            }
            if (createPathToObjectIdMap.containsKey(path)) {
                throw new NameDuplicatedException(
                        "duplicated path in request,create path: " + tuple.getPath());
            }
            createPathToObjectIdMap.put(path, objectId);
        }
    }
}
