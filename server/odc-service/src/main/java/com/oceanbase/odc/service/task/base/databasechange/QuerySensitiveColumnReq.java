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
package com.oceanbase.odc.service.task.base.databasechange;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.datasecurity.extractor.model.DBColumn;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/1/31 16:03
 */
@Data
public class QuerySensitiveColumnReq {

    /**
     * The raw DB columns related to the column of SQL query result set. One column in result set may
     * have multiple related DB columns.
     */
    @NotEmpty
    private List<Set<DBColumn>> tableRelatedDBColumns;
    /**
     * The ID of data source which the SQL query is executed on.
     */
    @NotNull
    private Long dataSourceId;
    /**
     * The ID of organization which the SQL query is executed in.
     */
    @NotNull
    private Long organizationId;

}
