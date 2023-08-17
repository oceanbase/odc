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
package com.oceanbase.odc.core.migrate.resource.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link TableMetaData}
 *
 * @author yh263208
 * @date 20222-04-20 15:02
 * @since ODC_release_3.3.1
 */
@Getter
@Setter
@ToString
public class TableMetaData {
    @JsonProperty("allow_duplicate")
    private boolean allowDuplicate = false;
    @NotNull
    @JsonProperty("table_name")
    private String table;
    @JsonProperty("unique_keys")
    private List<String> uniqueKeys;
}
