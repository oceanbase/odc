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
 * The {@code VirtualColumn} object realized by using the linked list as the basic data structure
 * can be used to iterate the {@code VirtualColumn}
 *
 * @author yh263208
 * @date 2021-11-03 16:42
 * @since ODC_release_3.2.2
 * @see VirtualColumn
 */
class LinkedVirtualColumn implements VirtualColumn {

    private final ColumnNode head;

    public LinkedVirtualColumn(@NonNull ColumnNode head) {
        this.head = head;
    }

    public LinkedVirtualColumn(@NonNull LinkedVirtualColumn virtualColumn) {
        this.head = virtualColumn.head;
    }

    @Override
    public Integer columnId() {
        return head.getColumnId();
    }

    @Override
    public String columnName() {
        return head.getColumnName();
    }

    @Override
    public String tableId() {
        return head.getTableId();
    }

    @Override
    public String dataTypeName() {
        return head.getDataType();
    }

    @Override
    public VirtualElementNode put(@NonNull VirtualElementNode wrapper) {
        VirtualElement elt = wrapper.getElement();
        Long rowId = elt.rowId();
        Integer columnId = elt.columnId();
        if (rowId == null || columnId == null) {
            throw new NullPointerException("RowId or ColumnId can not be null");
        }
        if (!Objects.equals(head.getColumnId(), columnId)) {
            throw new IllegalArgumentException("Wrong columnId");
        }
        if (this.head.lastElement != null && rowId.compareTo(this.head.lastElement.getElement().rowId()) > 0) {
            this.head.lastElement.down = wrapper;
            wrapper.up = this.head.lastElement;
            this.head.lastElement = wrapper;
            return wrapper;
        }
        VirtualElementNode lineNode;
        for (lineNode = head.nextElement; lineNode != null; lineNode = lineNode.down) {
            VirtualElement element = lineNode.getElement();
            if (Objects.equals(element.rowId(), rowId)) {
                lineNode.setElement(elt);
                return wrapper;
            } else if (element.rowId().compareTo(rowId) > 0) {
                break;
            } else if (lineNode.down == null) {
                // last node
                lineNode.down = wrapper;
                wrapper.up = lineNode;
                this.head.lastElement = wrapper;
                return wrapper;
            }
        }
        if (head.nextElement == null) {
            // empty column
            head.nextElement = wrapper;
            this.head.lastElement = wrapper;
        } else {
            // not an empty column
            if (lineNode.up == null) {
                // first element
                head.nextElement = wrapper;
            } else {
                // not in first line
                VirtualElementNode upperNode = lineNode.up;
                upperNode.down = wrapper;
                wrapper.up = upperNode;
            }
            wrapper.down = lineNode;
            lineNode.up = wrapper;
        }
        return wrapper;
    }

    @Override
    public VirtualElementNode get(Long rowId) {
        VirtualElementNode node;
        for (node = head.nextElement; node != null; node = node.down) {
            VirtualElement element = node.getElement();
            if (Objects.equals(element.rowId(), rowId)) {
                return node;
            } else if (element.rowId().compareTo(rowId) > 0) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Iterator<VirtualElement> iterator() {
        return new ColumnNode(this.head);
    }

}
