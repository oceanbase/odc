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
package com.oceanbase.odc.service.structurecompare.comparedbobject;

import java.util.List;

import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.model.DBObject;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public interface DBObjectStructureComparator<T extends DBObject> {
    /**
     * Compare specified database object types between two schema.
     *
     * @param sourceObjects source database objects
     * @param targetObjects target database objects
     * @return {@link DBObjectComparisonResult}
     */
    List<DBObjectComparisonResult> compare(List<T> sourceObjects, List<T> targetObjects);


    /**
     * Compare single specified database object type between two schema.
     *
     * @param sourceObject source database object
     * @param targetObject target database object
     * @return {@link DBObjectComparisonResult}
     */
    DBObjectComparisonResult compare(T sourceObject, T targetObject);
}
