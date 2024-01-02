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

package com.oceanbase.odc.plugin.task.api.structurecompare;

import java.util.List;

import com.oceanbase.odc.plugin.task.api.structurecompare.model.DBObjectComparisonResult;

/**
 * @author jingtian
 * @date 2023/12/29
 * @since ODC_release_4.2.4
 */
public interface StructureComparator {
    /**
     * Compare all the TABLE definition between source database and target database.
     *
     * @return {@link DBObjectComparisonResult}
     */
    List<DBObjectComparisonResult> compareTables();

    /**
     * Compare specified TABLE definition between source database and target database.
     *
     * @param tableNames table names to be compared
     * @return {@link DBObjectComparisonResult}
     */
    List<DBObjectComparisonResult> compareTables(List<String> tableNames);
}
