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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;

import lombok.Getter;

/**
 * divide paths by {@link WorksheetLocation}
 * 
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Getter
public class BatchOperateWorksheetsDivider {
    List<Path> normalPaths = new ArrayList<>();
    List<Path> reposPaths = new ArrayList<>();

    public BatchOperateWorksheetsDivider(Collection<String> pathStrList) {
        List<Path> sortedPaths =
                pathStrList.stream().map(Path::new)
                        .sorted(Path.getLevelNumComparator())
                        .distinct().collect(Collectors.toList());
        for (Path path : sortedPaths) {
            switch (path.getLocation()) {
                case WORKSHEETS:
                    if (!path.isChildOfAny(this.normalPaths.toArray(new Path[0]))) {
                        this.normalPaths.add(path);
                    }
                    break;
                case REPOS:
                    if (!path.isChildOfAny(this.reposPaths.toArray(new Path[0]))) {
                        this.reposPaths.add(path);
                    }
                    break;
            }
        }
    }

    public int size() {
        return normalPaths.size() + reposPaths.size();
    }

    public Optional<Path> findFirst() {
        if (size() == 0) {
            return Optional.empty();
        }
        return Optional.of(
                normalPaths.isEmpty() ? reposPaths.iterator().next() : normalPaths.iterator().next());
    }

    public Set<Path> all() {
        Set<Path> all = new HashSet<>();
        all.addAll(normalPaths);
        all.addAll(reposPaths);
        return all;
    }



}
