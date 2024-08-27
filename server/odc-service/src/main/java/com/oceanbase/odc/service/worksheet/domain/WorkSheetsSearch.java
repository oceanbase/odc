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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;

import lombok.Getter;

/**
 * for file name search
 *
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Getter
public class WorkSheetsSearch {
    List<WorksheetMetaResp> worksheets;
    String nameLike;

    public WorkSheetsSearch(String nameLike) {
        worksheets = new ArrayList<>();
        this.nameLike = nameLike;
    }

    public void addAll(List<WorksheetMetaResp> worksheets) {
        if (CollectionUtils.isNotEmpty(worksheets)) {
            this.worksheets.addAll(worksheets);
        }
    }

    /**
     * fuzzy matching of names in files and sorting
     * 
     * @return
     */
    public List<WorksheetMetaResp> searchByNameLike(int limit) {
        if (CollectionUtils.isEmpty(worksheets) || StringUtils.isBlank(nameLike)) {
            return worksheets;
        }
        return worksheets.stream().filter(worksheet -> new Path(worksheet.getPath()).isNameContains(nameLike))
                .sorted((o1, o2) -> Path.getPathComparator().compare(new Path(o1.getPath()), new Path(o2.getPath())))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
