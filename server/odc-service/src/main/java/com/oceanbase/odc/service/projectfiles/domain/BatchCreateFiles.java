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
package com.oceanbase.odc.service.projectfiles.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.service.projectfiles.exceptions.NameDuplicatedException;
import com.oceanbase.odc.service.projectfiles.model.BatchUploadProjectFilesReq;
import com.oceanbase.odc.service.projectfiles.model.BatchUploadProjectFilesReq.UploadProjectFileTuple;

import lombok.Getter;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */

public class BatchCreateFiles {
    @Getter
    private Path parentPath;
    /**
     * 这个主要用以类中报错返回更详细的内容
     */
    private UploadProjectFileTuple tupleGetParentPath;
    @Getter
    private final Map<Path, String> createPathToObjectKeyMap;

    public BatchCreateFiles(BatchUploadProjectFilesReq req) {
        createPathToObjectKeyMap = new HashMap<>();
        for (UploadProjectFileTuple tuple : req.getFiles()) {
            String pathStr = tuple.getPath();
            String objectKey = tuple.getObjectKey();
            Path path = new Path(pathStr);
            if (path.isFile() && StringUtils.isBlank(objectKey)) {
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
            if (createPathToObjectKeyMap.containsKey(path)) {
                throw new NameDuplicatedException(
                        "duplicated path in request,create path: " + tuple.getPath());
            }
            createPathToObjectKeyMap.put(path, objectKey);
        }

    }
}
