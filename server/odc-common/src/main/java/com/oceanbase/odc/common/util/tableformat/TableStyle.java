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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TableStyle {

    /**
     *
     */
    public BorderStyle borderStyle;

    /**
     *
     */
    private String prompt;

    /**
     *
     */
    private ShownBorders shownBorders;

    /**
     * @param borderStyle
     * @param shownBorders
     * @param leftMargin
     * @param prompt
     */
    public TableStyle(final BorderStyle borderStyle, final ShownBorders shownBorders, final int leftMargin,
            final String prompt) {
        this.borderStyle = borderStyle;
        this.shownBorders = shownBorders;
        this.prompt = prompt;
        if (this.prompt == null) {
            if (leftMargin > 0) {
                this.prompt = Filler.getFiller(leftMargin);
            } else {
                this.prompt = "";
            }
        }
    }

    /**
     * @param txt1
     * @param txt2
     * @return String
     */
    private String escapeXmlIfRequired(final String txt1, final String txt2) {
        return txt1 + txt2;
    }

    /**
     * @param table
     * @return String
     */
    public StringBuilder renderTable(final Table table) {
        StringBuilder sb = new StringBuilder(4096);
        int totalRows = table.getRows().size();
        Row previousRow = null;
        boolean firstRenderedRow = true;
        for (int i = 0; i < totalRows; i++) {
            Row r = table.getRows().get(i);
            boolean isFirst = i == 0;
            boolean isSecond = i == 1;
            boolean isIntermediate = (i > 1 && i < totalRows - 1);
            boolean isLast = i == (totalRows - 1);
            List<String> rr = renderRow(r, previousRow, table.getColumns(), isFirst, isSecond, isIntermediate, isLast);
            for (String line : rr) {
                if (firstRenderedRow) {
                    firstRenderedRow = false;
                } else {
                    sb.append("\n");
                }
                sb.append(line);
            }
            previousRow = r;
        }
        return sb;
    }

    /**
     * @param table
     * @return String[]
     */
    String[] renderAsStringArray(final Table table) {
        int totalRows = table.getRows().size();
        Row previousRow = null;
        List<String> allLines = new ArrayList<String>();
        for (int i = 0; i < totalRows; i++) {
            Row r = table.getRows().get(i);
            boolean isFirst = i == 0;
            boolean isSecond = i == 1;
            boolean isIntermediate = (i > 1 && i < totalRows - 1);
            boolean isLast = i == (totalRows - 1);
            List<String> rr = renderRow(r, previousRow, table.getColumns(), isFirst, isSecond, isIntermediate, isLast);
            for (String line : rr) {
                allLines.add(line);
            }
            previousRow = r;
        }

        String[] result = new String[allLines.size()];
        int i = 0;
        for (String line : allLines) {
            result[i] = line;
            i++;
        }
        return result;
    }

    /**
     * @param ap
     * @param r
     * @param previousRow
     * @param columns
     * @param isFirst
     * @param isSecond
     * @param isIntermediate
     * @param isLast
     * @throws IOException
     */
    void renderRow(final Appendable ap, final Row r, final Row previousRow, final List<Column> columns,
            final boolean isFirst, final boolean isSecond, final boolean isIntermediate, final boolean isLast)
            throws IOException {
        List<String> rr = renderRow(r, previousRow, columns, isFirst, isSecond, isIntermediate, isLast);
        boolean firstRenderedRow = isFirst;
        for (String line : rr) {
            if (firstRenderedRow) {
                firstRenderedRow = false;
            } else {
                ap.append("\n");
            }
            ap.append(line);
        }
    }

    /**
     * @param r
     * @param previousRow
     * @param columns
     * @param isFirst
     * @param isSecond
     * @param isIntermediate
     * @param isLast
     * @return List<String>
     */
    private List<String> renderRow(final Row r, final Row previousRow, final List<Column> columns,
            final boolean isFirst, final boolean isSecond, final boolean isIntermediate, final boolean isLast) {
        List<String> list = new ArrayList<String>();
        if (isFirst) {
            if (this.shownBorders.showTopBorder()) {
                list.add(
                        escapeXmlIfRequired(this.prompt,
                                this.shownBorders.renderTopBorder(columns, this.borderStyle, r)));
            }
        } else {
            if (isIntermediate && this.shownBorders.showMiddleSeparator() || //
                    isSecond && this.shownBorders.showHeaderSeparator() //
                    || isLast && this.shownBorders.showFooterSeparator()) {
                list.add(escapeXmlIfRequired(this.prompt,
                        this.shownBorders.renderMiddleSeparator(columns, this.borderStyle, previousRow, r)));
            }
        }

        list.add(escapeXmlIfRequired(this.prompt, renderContentRow(r, columns)));

        if (isLast) {
            if (this.shownBorders.showBottomBorder()) {
                list.add(escapeXmlIfRequired(this.prompt,
                        this.shownBorders.renderBottomBorder(columns, this.borderStyle, r)));
            }
        }
        return list;
    }

    /**
     * @param r
     * @param columns
     * @return String
     */
    private String renderContentRow(final Row r, final List<Column> columns) {
        StringBuilder sb = new StringBuilder(2048);

        // Left border
        if (this.shownBorders.showLeftBorder()) {
            sb.append(this.borderStyle.getLeft());
        }

        // Cells
        int totalColumns = columns.size();
        int j = 0;
        for (Cell cell : r.getCells()) {

            // cell separator
            if (j != 0) {
                if ((j > 1 && j < totalColumns - 1) && this.shownBorders.showCenterSeparator()
                        || ((j == 1) && (this.shownBorders.showLeftSeparator()))
                        || ((j == (totalColumns - 1)) && (this.shownBorders.showRightSeparator()))) {
                    sb.append(this.borderStyle.getCenter());
                }
            }

            // Cell content

            int sepWidth = this.borderStyle.getCenter().length();
            int width = -sepWidth;
            for (int pos = j; pos < j + cell.getColSpan(); pos++) {
                width = width + sepWidth + columns.get(pos).getColumnWidth();
            }
            String renderedCell = cell.render(width);
            sb.append(renderedCell);

            j = j + cell.getColSpan();
        }

        // Render missing cells
        for (; j < totalColumns; j++) {

            // cell separator
            if (j != 0) {
                if ((j > 1 && j < totalColumns - 1) && this.shownBorders.showCenterSeparator()
                        || ((j == 1) && (this.shownBorders.showLeftSeparator()))
                        || ((j == (totalColumns - 1)) && (this.shownBorders.showRightSeparator()))) {
                    sb.append(this.borderStyle.getCenter());
                }
            }

            // Cell content
            Column col = columns.get(j);
            String renderedCell = CellStyle.renderNullCell(col.getColumnWidth());
            sb.append(renderedCell);
        }

        // Right border
        if (this.shownBorders.showRightBorder()) {
            sb.append(this.borderStyle.getRight());
        }
        return sb.toString();
    }
}
