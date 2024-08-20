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
package com.oceanbase.odc.service.worksheet.utils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;

/**
 * @author keyang
 * @date 2024/08/07
 * @since 4.3.2
 */
public class WorksheetTestUtil {

    static Long initId = System.currentTimeMillis();

    public static Worksheet newDirWorksheet(Long projectId, String path) {
        String objectId = null;
        if (new Path(path).isFile()) {
            objectId = "objectId_" + nextId();
        }
        return new Worksheet(nextId(), new Date(), new Date(), projectId,
                new Path(path), 1L, 0L, objectId, null, null);
    }

    public static Worksheet newDirWorksheet(Long projectId, String path, List<String> sameLevelPaths,
            List<String> subPaths) {
        Set<Worksheet> sameLevels =
                CollectionUtils.isEmpty(sameLevelPaths) ? null : sameLevelPaths.stream().map(pathStr -> {
                    Path temp = new Path(pathStr);
                    String objectId = null;
                    if (temp.isFile()) {
                        objectId = "objectId_" + nextId();
                    }
                    return new Worksheet(nextId(), null, null, projectId,
                            temp, 1L, 0L, objectId, null, null);
                }).collect(Collectors.toSet());
        Set<Worksheet> subLevels =
                CollectionUtils.isEmpty(subPaths) ? null : subPaths.stream().map(pathStr -> {
                    Path temp = new Path(pathStr);
                    String objectId = null;
                    if (temp.isFile()) {
                        objectId = "objectId_" + nextId();
                    }
                    return new Worksheet(nextId(), null, null, projectId,
                            temp, 1L, 0L, objectId, null, null);
                }).collect(Collectors.toSet());
        String objectId = null;
        if (new Path(path).isFile()) {
            objectId = "objectId_" + nextId();
        }
        return new Worksheet(nextId(), null, null, projectId,
                new Path(path), 1L, 0L, objectId, sameLevels, subLevels);
    }


    public static Long nextId() {
        return initId++;
    }
}
