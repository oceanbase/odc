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
package com.oceanbase.odc.service.worksheet.model;

import java.util.Optional;

import lombok.Getter;

/**
 * the location type of worksheet
 * 
 * @author keyangs
 * @date 2024/8/1
 * @since 4.3.2
 */
@Getter
public enum WorksheetLocation {
    /**
     * the worksheet is in directory:/Worksheets/
     */
    WORKSHEETS("Worksheets"),
    /**
     * the worksheet is in directory:/Repos/RepoName/
     */
    REPOS("Repos");

    final String value;

    WorksheetLocation(String value) {
        this.value = value;
    }

    public static Optional<WorksheetLocation> getByName(String name) {
        for (WorksheetLocation value : values()) {
            if (value.getValue().equals(name)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
