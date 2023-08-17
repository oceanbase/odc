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

/**
 * <p>
 * In-memory text table generator. This class will generate text tables like:
 * </p>
 *
 * <pre class='example'>
 *
 *  +---------+-----------+----------+--------+
 *  |Country  | Population|Area (km2)| Density|
 *  +---------+-----------+----------+--------+
 *  |Chile    | 17 000 000| 1 250 000|   13.60|
 *  |Argentina| 50 000 000| 3 000 000|   16.67|
 *  |Brasil   | 80 000 000| 5 000 000|   16.00|
 *  +---------+-----------+----------+--------+
 *  |Total    |147 000 000| 9 250 000|   15.89|
 *  +---------+-----------+----------+--------+
 * </pre>
 *
 * <p>
 * where the border style, shown borders/separators, column widths, alignments and other are
 * configurable.
 * </p>
 *
 * <p>
 * Cells are added using the <code>addCell()</code> method and finally rendered as a String using
 * the <code>render()</code> method.
 * </p>
 *
 * <p>
 * The entire table is built in memory, so this class is not intended for a massive numbers of
 * rows/cells. Although the maximum size of the in-memory table depends on the amount of available
 * memory the JVM has, as a rule of thumb, don't exceed 10.000 total cells. If you need to render a
 * bigger table, use the <code>StreamingTable</code> class instead.
 * </p>
 *
 * <p>
 * If no widths are specified for a column, its width will adjusted to the wider cell in the column.
 * </p>
 *
 * <p>
 * As an example, the following code:
 * </p>
 *
 * <pre class='example'>
 *
 * CellStyle cs = new CellStyle(HorizontalAlign.left, AbbreviationStyle.crop, NullStyle.emptyString);
 * Table t = new Table(2, BorderStyle.CLASSIC, ShownBorders.ALL, false, "");
 * t.addCell("abcdef", cs);
 * t.addCell("123456", cs);
 * t.addCell("mno", cs);
 * t.addCell("45689", cs);
 * t.addCell("xyztuvw", cs);
 * t.addCell("01234567", cs);
 * System.out.println(t.render());
 * </pre>
 *
 * <p>
 * will generate the table:
 * </p>
 *
 * <pre class='example'>
 *
 *  +-------+--------+
 *  |abcdef |123456  |
 *  +-------+--------+
 *  |mno    |45689   |
 *  +-------+--------+
 *  |xyztuvw|01234567|
 *  +-------+--------+
 * </pre>
 *
 * <p>
 * The generated table can be customized using a <code>BorderStyle</code>, <code>ShownBorders</code>
 * and cell widths. Besides, cell rendering can be customized on a cell basis using
 * <code>CellStyle</code>s.
 * </p>
 *
 * @author valarcon
 */
public class Table {

    private static final int DEFAULT_MIN_WIDTH = 0;

    private static final int DEFAULT_MAX_WIDTH = Integer.MAX_VALUE;

    private TableStyle tableStyle;

    private List<Row> rows;

    private List<Column> columns;

    private int totalColumns;

    private int currentColumn;

    private Row currentRow;

    /**
     * Creates a table using <code>BorderStyle.CLASSIC</code> and
     * <code>ShownBorders.SURROUND_HEADER_AND_COLUMNS</code>, no XML escaping and no left margin.
     */
    public Table() {
        initialize(totalColumns);
        this.tableStyle = new TableStyle(BorderStyle.HORIZONTAL_ONLY, ShownBorders.SURROUND_HEADER_AND_COLUMNS, 0,
                null);
    }

    /**
     * Creates a table using the specified border style and
     * <code>ShownBorders.SURROUND_HEADER_AND_COLUMNS</code>, no XML escaping and no left margin.
     *
     * @param totalColumns Total columns of this table.
     * @param borderStyle The border style to use when rendering the table.
     */
    public Table(final int totalColumns, final BorderStyle borderStyle) {
        initialize(totalColumns);
        this.tableStyle = new TableStyle(borderStyle, ShownBorders.SURROUND_HEADER_AND_COLUMNS, 0, null);
    }

    /**
     * @param totalColumns
     */
    private void initialize(final int totalColumns) {
        this.totalColumns = totalColumns;
        this.rows = new ArrayList<Row>();
        this.columns = new ArrayList<Column>();
        for (int i = 0; i < this.totalColumns; i++) {
            this.columns.add(new Column(i, DEFAULT_MIN_WIDTH, DEFAULT_MAX_WIDTH));
        }
        this.currentColumn = 0;
        this.currentRow = null;
    }

    /**
     * Sets the minimum and maximum desired column widths of a specific column. If no width range is
     * specified for a column, its width will be adjusted to the wider cell in the column.
     *
     * @param col Column whose desired widths will be set. First column is 0 (zero).
     *        </p>
     * @param minWidth Minimum desired width.
     * @param maxWidth Maximum desired width.
     */
    public void setColumnWidth(final int col, final int minWidth, final int maxWidth) {
        this.columns.get(col).setWidthRange(minWidth, maxWidth);
    }

    /**
     * Adds a cell with the default CellStyle. See <code>CellStyle</code> for details on its default
     * characteristics.
     *
     * @param content Cell text.
     */
    public void addCell(final String content) {
        addCell(content, new CellStyle());
    }

    /**
     * Adds a cell with a colspan and the default CellStyle.
     *
     * @param content Cell text.
     * @param colSpan Columns this cell will span through.
     */
    public void addCell(final String content, final int colSpan) {
        addCell(content, new CellStyle(), colSpan);
    }

    /**
     * Adds a cell with a specific cell style.
     *
     * @param content Cell text.
     * @param style Cell style to use when rendering the cell content.
     */
    public void addCell(final String content, final CellStyle style) {
        addCell(content, style, 1);
    }

    /**
     * Adds a cell with a specific cell style and colspan.
     *
     * @param content Cell text.
     * @param style Cell style to use when rendering the cell content.
     * @param colSpan Columns this cell will span through.
     */
    public void addCell(final String content, final CellStyle style, final int colSpan) {
        if (this.currentRow == null || this.currentColumn >= this.totalColumns) {
            this.currentRow = new Row();
            this.rows.add(currentRow);
            this.currentColumn = 0;
        }
        int adjColSpan = colSpan > 0 ? colSpan : 1;
        if (this.currentColumn + adjColSpan > this.totalColumns) {
            adjColSpan = this.totalColumns - this.currentColumn;
        }
        this.currentRow.addCell(content, style, adjColSpan);
        this.currentColumn = this.currentColumn + adjColSpan;
    }

    List<Row> getRows() {
        return this.rows;
    }

    List<Column> getColumns() {
        return this.columns;
    }

    /**
     * Renders the table as a multi-line String.
     *
     * @return rendered table.
     */
    public StringBuilder render() {
        calculateColumnsWidth();
        return this.tableStyle.renderTable(this);
    }

    /**
     * Renders the table as a String array.
     *
     * @return rendered table
     */
    public String[] renderAsStringArray() {
        calculateColumnsWidth();
        return this.tableStyle.renderAsStringArray(this);
    }

    int getTotalColumns() {
        return this.totalColumns;
    }

    /**
     *
     */
    private void calculateColumnsWidth() {

        // First we connect the columns with the cells.
        for (Row r : this.rows) {
            int startCol = 0;
            for (Cell cell : r.getCells()) {
                int endCol = startCol + cell.getColSpan() - 1;
                try {
                    Column col = this.columns.get(endCol);
                    col.add(cell);
                    startCol = startCol + cell.getColSpan();
                } catch (IndexOutOfBoundsException e) {
                    // Nothing to do.
                }
            }
        }

        // Then we calculate the appropriate column width for each one.
        for (Column col : this.columns) {
            col.calculateWidth(this.columns, this.tableStyle.borderStyle.getTCCorner().length());
        }
    }
}
