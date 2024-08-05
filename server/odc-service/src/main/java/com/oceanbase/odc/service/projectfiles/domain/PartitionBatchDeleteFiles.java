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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import lombok.Getter;

/**
 * 划分要删除的paths
 * 
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Getter
public class PartitionBatchDeleteFiles {
    Set<Path> normalPaths = new HashSet<>();
    Set<Path> reposPaths = new HashSet<>();

    public PartitionBatchDeleteFiles(List<String> pathStrList) {
        List<Path> sortedPaths =
                pathStrList.stream().map(Path::new)
                        .sorted(Path.getLevelNulComparator())
                        .distinct().collect(Collectors.toList());
        for (Path path : sortedPaths) {
            switch (path.getLocation()) {
                case WORKSHEETS:
                    if (!isPathDeleted(this.normalPaths, path)) {
                        this.normalPaths.add(path);
                    }
                    break;
                case REPOS:
                    if (!isPathDeleted(this.reposPaths, path)) {
                        this.reposPaths.add(path);
                    }
                    break;
            }
        }
    }

    private boolean isPathDeleted(Set<Path> deletePaths, Path path) {
        if (CollectionUtils.isEmpty(deletePaths)) {
            return false;
        }
        Optional<Path> parentPath = path.getParentPath();
        return parentPath.filter(deletePaths::contains).isPresent();
    }
}
