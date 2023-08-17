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
 * Virtual row, corresponding to a row in the {@code VirtualTable}. Since the virtual line is only a
 * virtual structure abstracted for easy access, it can only be iterated and cannot perform any
 * other operations.
 *
 * @author yh263208
 * @date 2021-11-02 17:56
 * @since ODC-release_3.2.2
 */
public interface VirtualLine extends Iterable<VirtualElement> {
    /**
     * The id of the {@code VirtualLine}, usually the index of the row
     *
     * @return Id of this line
     */
    Long rowId();

    /**
     * The table id corresponding to the column, usually the sqlid that generated the
     * {@code VirtualTable}
     *
     * @return table id
     */
    String tableId();

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
     * @param columnId column id
     * @return element
     */
    VirtualElementNode get(Integer columnId);

}
