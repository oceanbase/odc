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

public class Row {

    private List<Cell> cells;

    public Row() {
        this.cells = new ArrayList<Cell>();
    }

    /**
     * @param content
     * @param style
     * @param colSpan
     */
    public void addCell(final String content, final CellStyle style, final int colSpan) {
        this.cells.add(new Cell(content, style, colSpan));
    }

    /**
     * @param pos
     * @return boolean
     */
    public boolean hasSeparator(final int pos) {
        if (pos == 0) {
            return true;
        }
        int i = 0;
        for (Cell cell : this.cells) {
            if (i < pos) {
                if (i + cell.getColSpan() > pos) {
                    return false;
                }
            } else {
                return true;
            }
            i = i + cell.getColSpan();
        }
        return true;
    }

    /**
     * @return int
     */
    public int getSize() {
        return this.cells.size();
    }

    /**
     * @return List<Cell>
     */
    public List<Cell> getCells() {
        return this.cells;
    }

    /**
     * @param index
     * @return Cell
     */
    public Cell get(final int index) {
        return this.cells.get(index);
    }
}
