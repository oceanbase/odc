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

package com.oceanbase.odc.service.datasecurity.model;

import java.util.List;
import java.util.Map;

import com.oceanbase.odc.service.feature.model.DataTypeUnit;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/9/14 15:18
 */
@Data
public class DatabaseWithAllColumns {

    private Long databaseId;
    private String databaseName;
    private Map<String, List<DBTableColumn>> table2Columns;
    private Map<String, List<DBTableColumn>> view2Columns;
    private Map<String, List<DBTableColumn>> externalTable2Columns;
    /**
     * Mapping from database type to show type, used for displaying column type icon
     */
    private List<DataTypeUnit> dataTypeUnits;

}
