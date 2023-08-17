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
package com.oceanbase.odc.core.sql.execute.cache.table;

import lombok.NonNull;

/**
 * Virtual column, corresponding to a column in the {@code VirtualTable}, this is a virtual
 * structure that is only convenient for outside access, so it only provides iterative functions and
 * does not provide operations such as {@code remove}.
 *
 * @author yh263208
 * @date 2021-11-02 17:59
 * @since ODC_release_3.2.2
 */
public interface VirtualColumn extends Iterable<VirtualElement> {
    /**
     * Get column num
     *
     * @return column number
     */
    Integer columnId();

    /**
     * Column name
     *
     * @return column name
     */
    String columnName();

    /**
     * The table id corresponding to the column, usually the sqlid that generated the
     * {@code VirtualTable}
     *
     * @return table id
     */
    String tableId();

    /**
     * Data type of this column, value may be:
     *
     * <pre>
     *     1. varchar2(64)
     *     2. blob
     *     ...
     * </pre>
     *
     * @return data type value
     */
    String dataTypeName();

    /**
     * Put an element
     *
     * @param elt {@code VirtualElementWrapper}
     * @return {@code VirtualElementWrapper}
     */
    VirtualElementNode put(@NonNull VirtualElementNode elt);

    /**
     * Get an element
     *
     * @param rowId row id
     * @return element
     */
    VirtualElementNode get(Long rowId);

}

