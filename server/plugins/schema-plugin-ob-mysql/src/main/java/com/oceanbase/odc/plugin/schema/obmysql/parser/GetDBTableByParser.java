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
package com.oceanbase.odc.plugin.schema.obmysql.parser;

import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;

/**
 * @author jingtian
 * @date 2023/6/30
 * @since 4.2.0
 */
public interface GetDBTableByParser {
    /**
     * Get table columns by parse table ddl
     */
    List<DBTableColumn> listColumns();

    /**
     * Get table constraints by parse table ddl
     */
    List<DBTableConstraint> listConstraints();

    /**
     * Get table indexes by parse table ddl
     */
    List<DBTableIndex> listIndexes();

    /**
     * Get table partition info by parse table ddl
     */
    DBTablePartition getPartition();

}
