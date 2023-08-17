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
package com.oceanbase.tools.dbbrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString.Exclude;

/**
 * Index column metadata, mapping to <br>
 * - Oracle: ALL_IND_COLUMNS dict <br>
 * - MySQL: KEY_COLUMN_USAGE dict
 */
@Data
public class DBTableIndexColumn {
    /**
     * 所属 index，API 层面不可见
     */
    @JsonIgnore
    @Exclude
    private DBTableIndex index;

    private String columnName;
    private Boolean ascending;
    private Boolean nullable;
    private String subPart;
    private Integer ordinalPosition;
}
