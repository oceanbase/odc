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
 * Row dimension node, each node is used to represent a row in the virtual table
 *
 * @author yh263208
 * @date 2021-11-03 16:30
 * @since ODC_release_3.2.2
 */
class LineNode implements Iterator<VirtualElement> {
    @Getter
    private final Long rowId;
    @Getter
    private final String tableId;
    public LineNode nextLine = null;
    public LineNode priorLine = null;
    public VirtualElementNode nextElement = null;
    private VirtualElementNode iterPointer;
    private boolean iterated = false;

    public LineNode(@NonNull Long rowId, @NonNull String tableId) {
        this.rowId = rowId;
        this.tableId = tableId;
    }

    public LineNode(@NonNull LineNode lineNode) {
        this.rowId = lineNode.rowId;
        this.tableId = lineNode.tableId;
        this.nextLine = lineNode.nextLine;
        this.priorLine = lineNode.priorLine;
        this.nextElement = lineNode.nextElement;
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
        this.iterPointer = this.iterPointer.right;
        return returnVal;
    }

}
