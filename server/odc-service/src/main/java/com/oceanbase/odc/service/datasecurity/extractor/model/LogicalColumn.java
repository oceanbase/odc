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
package com.oceanbase.odc.service.datasecurity.extractor.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/6/5 19:22
 */
@Data
public class LogicalColumn {
    private String name;
    private String alias;
    private ColumnType type;

    /**
     * Only used when type is PHYSICAL
     */
    private String databaseName;
    private String tableName;

    /**
     * Only used when type is not PHYSICAL
     */
    private List<LogicalColumn> fromList;

    public static LogicalColumn empty() {
        LogicalColumn logicalColumn = new LogicalColumn();
        logicalColumn.setFromList(new ArrayList<>());
        return logicalColumn;
    }
}
