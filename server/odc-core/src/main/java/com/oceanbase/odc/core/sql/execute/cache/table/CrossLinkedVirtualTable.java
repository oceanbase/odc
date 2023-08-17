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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The realization of the {@code VirtualTable}, Used to abstract the execution result of sql the
 * bottom layer adopts thecross-linked list data structure to realize
 *
 * @author yh263208
 * @date 2021-11-02 19:48
 * @since ODC_release_3.2.2
 */
@Slf4j
public class CrossLinkedVirtualTable implements VirtualTable {

    private final String tableId;
    private final Date createTime;
    private final LineNode lineHeadNode;
    private LineNode lineLastNode;
    private final ColumnNode columnHeadNode;
    private final List<VirtualTableEventListener> listenerList = new LinkedList<>();
    private final Map<Long, LineNode> lineHeadNodeIndex = new HashMap<>();

    /**
     * This constructor constructs an empty {@code VirutalTable}
     *
     * @param tableId Id for a sql
     */
    public CrossLinkedVirtualTable(@NonNull String tableId) {
        this.tableId = tableId;
        this.createTime = new Date();
        this.lineHeadNode = generateLineNode(-1L, tableId);
        this.lineLastNode = lineHeadNode;
        this.columnHeadNode = generateColumnNode(-1, "N/A", tableId, "N/A");
    }

    public void addListener(@NonNull VirtualTableEventListener listener) {
        this.listenerList.add(listener);
    }

    @Override
    public VirtualTable project(@NonNull List<Integer> columnIds,
            @NonNull Function<VirtualColumn, VirtualColumn> columenMapper)
            throws NullPointerException {
        CrossLinkedVirtualTable virtualTable = new CrossLinkedVirtualTable("tmp_" + System.currentTimeMillis());
        for (Integer columnId : columnIds) {
            ColumnNode columnNode = findColumnNode(columnId);
            if (columnNode == null) {
                throw new NullPointerException("Column with Id " + columnId + " is not found");
            }
            VirtualColumn mappedColumn = columenMapper.apply(new LinkedVirtualColumn(columnNode));
            virtualTable.addColumn(mappedColumn);
        }
        return virtualTable;
    }

    @Override
    public VirtualTable select(@NonNull Predicate<VirtualLine> predicate) {
        CrossLinkedVirtualTable virtualTable = new CrossLinkedVirtualTable("tmp_" + System.currentTimeMillis());
        LineNode currentLine;
        for (currentLine = lineHeadNode.nextLine; currentLine != null; currentLine = currentLine.nextLine) {
            if (predicate.test(new LinkedVirtualLine(currentLine))) {
                virtualTable.addLine(new LinkedVirtualLine(currentLine));
            }
        }
        return virtualTable;
    }

    @Override
    public String tableId() {
        return this.tableId;
    }

    @Override
    public Long count() {
        long count = 0;
        for (LineNode item = lineHeadNode.nextLine; item != null; item = item.nextLine) {
            count++;
        }
        return count;
    }

    @Override
    public List<Integer> columnIds() {
        List<Integer> columnIds = new ArrayList<>();
        for (ColumnNode node = this.columnHeadNode.nextColumn; node != null; node = node.nextColumn) {
            columnIds.add(node.getColumnId());
        }
        return columnIds;
    }

    @Override
    public void forEach(Consumer<VirtualLine> lineConsumer) {
        for (LineNode currentLine = lineHeadNode.nextLine; currentLine != null; currentLine = currentLine.nextLine) {
            lineConsumer.accept(new LinkedVirtualLine(currentLine));
        }
    }

    protected ColumnNode generateColumnNode(Integer columnId, String columnName, String tableId,
            String dataType) {
        return new ColumnNode(columnId, columnName, tableId, dataType);
    }

    protected LineNode generateLineNode(Long rowId, String tableId) {
        return new LineNode(rowId, tableId);
    }

    protected synchronized VirtualColumn addColumn(@NonNull VirtualColumn virtualColumn) {
        Iterator<VirtualElement> iter = virtualColumn.iterator();
        Integer columnId = virtualColumn.columnId();
        while (iter.hasNext()) {
            VirtualElement elt = iter.next();
            Validate.isTrue(Objects.equals(elt.columnId(), columnId));
            put(elt);
        }
        return virtualColumn;
    }

    protected synchronized VirtualLine addLine(@NonNull VirtualLine virtualLine) {
        Iterator<VirtualElement> iter = virtualLine.iterator();
        Long rowId = virtualLine.rowId();
        while (iter.hasNext()) {
            VirtualElement elt = iter.next();
            Validate.isTrue(Objects.equals(elt.rowId(), rowId));
            put(elt);
        }
        return virtualLine;
    }

    protected synchronized ColumnNode addNode(@NonNull ColumnNode newNode) {
        Validate.notNull(newNode.getColumnId());
        ColumnNode lastNode;
        for (lastNode = this.columnHeadNode; lastNode.nextColumn != null
                && lastNode.nextColumn.getColumnId().compareTo(newNode.getColumnId()) < 0; lastNode =
                        lastNode.nextColumn) {
        }
        if (lastNode.nextColumn != null && lastNode.nextColumn.getColumnId().compareTo(newNode.getColumnId()) == 0) {
            throw new IllegalArgumentException("Column Id has to be unique, columnId=" + newNode.getColumnId());
        }
        ColumnNode nextNode = lastNode.nextColumn;
        lastNode.nextColumn = newNode;
        newNode.priorColumn = lastNode;
        if (nextNode != null) {
            newNode.nextColumn = nextNode;
            nextNode.priorColumn = newNode;
        }
        try {
            onColumnAddedEvent(newNode);
        } catch (Throwable e) {
            log.warn("Failed to add column event listener callback method", e);
        }
        return newNode;
    }

    protected synchronized LineNode addNode(@NonNull LineNode newNode) {
        Validate.notNull(newNode.getRowId());
        if (this.lineLastNode.getRowId().compareTo(newNode.getRowId()) < 0) {
            this.lineLastNode.nextLine = newNode;
            newNode.priorLine = this.lineLastNode;
            this.lineLastNode = newNode;
        } else {
            LineNode lastNode;
            for (lastNode = this.lineHeadNode; lastNode.nextLine != null
                    && lastNode.nextLine.getRowId().compareTo(newNode.getRowId()) < 0; lastNode = lastNode.nextLine) {
            }
            if (lastNode.nextLine != null && lastNode.nextLine.getRowId().compareTo(newNode.getRowId()) == 0) {
                throw new IllegalArgumentException("Row Id has to be unique, rowId=" + newNode.getRowId());
            }
            LineNode nextNode = lastNode.nextLine;
            lastNode.nextLine = newNode;
            newNode.priorLine = lastNode;
            if (nextNode != null) {
                newNode.nextLine = nextNode;
                nextNode.priorLine = newNode;
            } else {
                this.lineLastNode = newNode;
            }
        }
        try {
            onLineAddedEvent(newNode);
        } catch (Throwable e) {
            log.warn("Failed to add line event listener callback method", e);
        }
        return newNode;
    }

    public synchronized VirtualElement put(@NonNull VirtualElement elt) {
        Long rowId = elt.rowId();
        Integer columnId = elt.columnId();
        if (rowId == null || columnId == null) {
            throw new NullPointerException("RowId or ColumnId can not be null");
        }
        ColumnNode columnNode = findColumnNode(columnId);
        if (columnNode == null) {
            columnNode = addNode(generateColumnNode(columnId, elt.columnName(), elt.tableId(), elt.dataTypeName()));
        }
        LineNode lineNode = findLineNode(rowId);
        if (lineNode == null) {
            lineNode = addNode(generateLineNode(rowId, elt.tableId()));
        }
        return put(lineNode, columnNode, elt);
    }

    private VirtualElement put(@NonNull LineNode lineNode, @NonNull ColumnNode columnNode,
            @NonNull VirtualElement elt) {
        VirtualElementNode newNode = new VirtualElementNode(elt);

        LinkedVirtualColumn virtualColumn = new LinkedVirtualColumn(columnNode);
        virtualColumn.put(newNode);

        LinkedVirtualLine virtualLine = new LinkedVirtualLine(lineNode);
        virtualLine.put(newNode);

        try {
            onElementPutEvent(newNode);
        } catch (Throwable e) {
            log.warn("The element placement event listener callback method failed", e);
        }
        return elt;
    }

    public VirtualElement get(@NonNull Long rowId, @NonNull Integer columnId) {
        LineNode lineNode = findLineNode(rowId);
        if (lineNode == null) {
            return null;
        }
        LinkedVirtualLine virtualLine = new LinkedVirtualLine(lineNode);
        VirtualElementNode elementNode = virtualLine.get(columnId);
        if (elementNode == null) {
            return null;
        }
        return elementNode.getElement();
    }

    protected LineNode findLineNode(@NonNull Long rowId) {
        if (this.lineHeadNode == null) {
            throw new NullPointerException("Line head node can not be null");
        }
        if (!lineHeadNodeIndex.isEmpty()) {
            return lineHeadNodeIndex.get(rowId);
        }
        LineNode nodePointer;
        for (nodePointer = this.lineHeadNode.nextLine; nodePointer != null; nodePointer =
                nodePointer.nextLine) {
            if (Objects.equals(nodePointer.getRowId(), rowId)) {
                return nodePointer;
            }
        }
        return null;
    }

    protected ColumnNode findColumnNode(@NonNull Integer columnId) {
        if (this.columnHeadNode == null) {
            throw new NullPointerException("Column head node can not be null");
        }
        ColumnNode nodePointer;
        for (nodePointer = this.columnHeadNode.nextColumn; nodePointer != null; nodePointer =
                nodePointer.nextColumn) {
            if (Objects.equals(nodePointer.getColumnId(), columnId)) {
                return nodePointer;
            }
        }
        return null;
    }

    private void onElementPutEvent(@NonNull VirtualElementNode elt) {
        for (VirtualTableEventListener listener : listenerList) {
            try {
                listener.onElementPut(this, elt.getElement());
            } catch (Throwable throwable) {
                log.warn("The callback method of the element placement event listener failed to execute", throwable);
            }
        }
    }

    private void onColumnAddedEvent(@NonNull ColumnNode columnNode) {
        for (VirtualTableEventListener listener : listenerList) {
            try {
                listener.onColumnAdded(this, new LinkedVirtualColumn(columnNode));
            } catch (Throwable throwable) {
                log.warn("The callback method of the new column event listener failed to execute", throwable);
            }
        }
    }

    private void onLineAddedEvent(@NonNull LineNode lineNode) {
        lineHeadNodeIndex.put(lineNode.getRowId(), lineNode);
        for (VirtualTableEventListener listener : listenerList) {
            try {
                listener.onLineAdded(this, new LinkedVirtualLine(lineNode));
            } catch (Throwable throwable) {
                log.warn("The callback method of the new line event listener failed to execute", throwable);
            }
        }
    }

}
