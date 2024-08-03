/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.projectfiles.model;

import java.util.Optional;

import lombok.Getter;

/**
 * 项目文件位置类型
 * 
 * @author keyangs
 * @date 2024/8/1
 * @since 4.3.2
 */
@Getter
public enum ProjectFileLocation {
    WORKSHEETS("Worksheets"),
    REPOS("Repos");

    final String name;

    ProjectFileLocation(String value) {
        this.name = value;
    }

    public static Optional<ProjectFileLocation> getByName(String name) {
        for (ProjectFileLocation value : values()) {
            if (value.getName().equals(name)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
