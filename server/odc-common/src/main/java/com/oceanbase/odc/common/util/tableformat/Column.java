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
package com.oceanbase.odc.common.util.tableformat;

import java.util.ArrayList;
import java.util.List;

public class Column {

    private int colIndex;

    private List<Cell> cells;

    private int minWidth;

    private int maxWidth;

    private int width;

    /**
     * @param colIndex
     * @param minWidth
     * @param maxWidth
     */
    public Column(final int colIndex, final int minWidth, final int maxWidth) {
        this.colIndex = colIndex;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.cells = new ArrayList<Cell>();
        this.width = 0;
    }

    /**
     * @param colIndex
     * @param width
     */
    public Column(final int colIndex, final int width) {
        this.colIndex = colIndex;
        this.minWidth = width;
        this.maxWidth = width;
        this.cells = new ArrayList<Cell>();
        this.width = width;
    }

    /**
     * @param columns
     * @param separatorWidth
     */
    public void calculateWidth(final List<Column> columns, final int separatorWidth) {
        this.width = this.minWidth;
        for (Cell cell : this.cells) {
            int previousWidth = 0;
            if (cell.getColSpan() > 1) {
                for (int pos = this.colIndex - cell.getColSpan() + 1; pos < this.colIndex; pos++) {
                    previousWidth = previousWidth + columns.get(pos).getColumnWidth() + separatorWidth;
                }
            }
            int cellTightWidth = cell != null ? cell.getTightWidth(this.maxWidth) : 0;
            int tw = cellTightWidth - previousWidth;
            if (tw > this.width) {
                this.width = tw;
            }
        }
    }

    /**
     * @return int
     */
    public int getColumnWidth() {
        return this.width;
    }

    /**
     * @param cell
     */
    public void add(final Cell cell) {
        this.cells.add(cell);
    }

    /**
     * @return int
     */
    public int getSize() {
        return this.cells.size();
    }

    /**
     * @param index
     * @return Cell
     */
    public Cell get(final int index) {
        return this.cells.get(index);
    }

    /**
     * @return List<Cell>
     */
    public List<Cell> getCells() {
        return cells;
    }

    /**
     * @param minWidth
     * @param maxWidth
     */
    public void setWidthRange(final int minWidth, final int maxWidth) {
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
    }
}

