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
import java.util.Objects;

import lombok.NonNull;

/**
 * The {@code VirtualLine} object realized by using the linked list as the basic data structure can
 * be used to iterate the {@code VirtualLine}
 *
 * @author yh263208
 * @date 2021-11-03 16:48
 * @since ODC_release_3.2.2
 * @see VirtualLine
 */
class LinkedVirtualLine implements VirtualLine {

    private final LineNode head;

    public LinkedVirtualLine(@NonNull LineNode head) {
        this.head = head;
    }

    public LinkedVirtualLine(@NonNull LinkedVirtualLine virtualLine) {
        this.head = virtualLine.head;
    }

    @Override
    public Long rowId() {
        return this.head.getRowId();
    }

    @Override
    public String tableId() {
        return this.head.getTableId();
    }

    @Override
    public VirtualElementNode put(@NonNull VirtualElementNode wrapper) {
        VirtualElement elt = wrapper.getElement();
        Long rowId = elt.rowId();
        Integer columnId = elt.columnId();
        if (rowId == null || columnId == null) {
            throw new NullPointerException("RowId or ColumnId can not be null");
        }
        if (!Objects.equals(head.getRowId(), rowId)) {
            throw new IllegalArgumentException("Wrong rowId");
        }
        VirtualElementNode columnNode;
        for (columnNode = head.nextElement; columnNode != null; columnNode = columnNode.right) {
            VirtualElement element = columnNode.getElement();
            if (Objects.equals(element.columnId(), columnId)) {
                columnNode.setElement(elt);
                return wrapper;
            } else if (element.columnId().compareTo(columnId) > 0) {
                break;
            } else if (columnNode.right == null) {
                // last node
                columnNode.right = wrapper;
                wrapper.left = columnNode;
                return wrapper;
            }
        }
        if (head.nextElement == null) {
            // empty line
            head.nextElement = wrapper;
        } else {
            // not an empty line
            if (columnNode.left == null) {
                // first element
                head.nextElement = wrapper;
            } else {
                // not in first line
                VirtualElementNode leftNode = columnNode.left;
                leftNode.right = wrapper;
                wrapper.left = leftNode;
            }
            wrapper.right = columnNode;
            columnNode.left = wrapper;
        }
        return wrapper;
    }

    @Override
    public VirtualElementNode get(Integer columnId) {
        VirtualElementNode node;
        for (node = head.nextElement; node != null; node = node.right) {
            VirtualElement element = node.getElement();
            if (Objects.equals(element.columnId(), columnId)) {
                return node;
            } else if (element.columnId().compareTo(columnId) > 0) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Iterator<VirtualElement> iterator() {
        return new LineNode(this.head);
    }

}
