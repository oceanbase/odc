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
package com.oceanbase.odc.service.db.schema.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Builder;
import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/3/29 13:55
 */
@Data
@Builder
public class QueryDBObjectParams {

    /**
     * Search DB objects inner a project. If not set, then datasourceId is required.
     */
    private Long projectId;

    /**
     * Search DB objects inner a datasource. If not set, then projectId is required.
     */
    private Long datasourceId;

    /**
     * Search DB objects inner specific databases. If not set, then search all databases.
     */
    private List<Long> databaseIds;

    /**
     * Search specific type of DB objects. If not set, then search all types.
     */
    private List<DBObjectType> types;

    /**
     * Default full text matching, use %searchKey% for fuzzy matching.
     */
    @NotNull
    private String searchKey;

}
