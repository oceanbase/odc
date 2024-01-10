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
package com.oceanbase.odc.service.structurecompare.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
@Data
public class DBStructureComparisonConfig {
    @NotNull
    private String schemaName;
    @NotNull
    private ConnectType connectType;
    @NotNull
    private DataSource dataSource;
    @NotNull
    private Set<DBObjectType> toComparedObjectTypes;
    /**
     * Used to compare specified database objects [Optional] If null or empty, ignore
     */
    private Map<DBObjectType, Set<String>> blackListMap = new HashMap<>();
}
