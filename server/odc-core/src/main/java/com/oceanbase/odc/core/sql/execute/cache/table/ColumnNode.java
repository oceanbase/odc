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

import java.util.Iterator;

import lombok.Getter;
import lombok.NonNull;

/**
 * Column dimension node, each node is used to represent a column in the virtual table
 *
 * @author yh263208
 * @date 2021-11-03 16:22
 * @since ODC_release_3.2.2
 */
class ColumnNode implements Iterator<VirtualElement> {
    @Getter
    private final Integer columnId;
    @Getter
    private final String columnName;
    @Getter
    private final String tableId;
    @Getter
    private final String dataType;
    public ColumnNode nextColumn = null;
    public ColumnNode priorColumn = null;
    public VirtualElementNode nextElement = null;
    public VirtualElementNode lastElement = null;
    private VirtualElementNode iterPointer;
    private boolean iterated = false;

    public ColumnNode(@NonNull Integer columnId, @NonNull String columnName, @NonNull String tableId,
            @NonNull String dataType) {
        this.columnId = columnId;
        this.columnName = columnName;
        this.tableId = tableId;
        this.dataType = dataType;
    }

    public ColumnNode(@NonNull ColumnNode columnNode) {
        this.columnId = columnNode.columnId;
        this.columnName = columnNode.columnName;
        this.tableId = columnNode.tableId;
        this.dataType = columnNode.dataType;
        this.nextColumn = columnNode.nextColumn;
        this.priorColumn = columnNode.priorColumn;
        this.nextElement = columnNode.nextElement;
    }

    @Override
    public boolean hasNext() {
        if (this.iterPointer == null && !iterated) {
            this.iterPointer = this.nextElement;
        }
        return this.iterPointer != null;
    }

    @Override
    public VirtualElement next() {
        if (this.iterPointer == null && !hasNext()) {
            return null;
        }
        iterated = true;
        VirtualElement returnVal = this.iterPointer.getElement();
        this.iterPointer = this.iterPointer.down;
        return returnVal;
    }

}
