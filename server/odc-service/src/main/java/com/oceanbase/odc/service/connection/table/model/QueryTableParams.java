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
package com.oceanbase.odc.service.connection.table.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Builder;
import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/4/28 19:17
 */
@Data
@Builder
public class QueryTableParams {

    @NotNull
    private Long databaseId;
    @NotNull
    private Boolean includePermittedAction;
    /**
     * table belonging to type in this collection needs to be fetched. if null, only fetch collection of
     * basic table
     */
    private List<DBObjectType> types;

}
