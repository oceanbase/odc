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

import lombok.Getter;

public class Cell {

    /**
     *
     */
    @Getter
    private final String content;

    /**
     *
     */
    @Getter
    private final CellStyle style;

    /**
     *
     */
    @Getter
    private final int colSpan;

    /**
     * @param content
     * @param style
     * @param colSpan
     */
    public Cell(final String content, final CellStyle style, final int colSpan) {
        this.content = content;
        this.style = style;
        this.colSpan = colSpan;
    }

    /**
     * @param maxWidth
     * @return int
     */
    public int getTightWidth(final int maxWidth) {
        int width = this.style.getWidth(this.content);
        return width > maxWidth ? maxWidth : width;
    }

    /**
     * @param width
     * @return String
     */
    public String render(final int width) {
        return this.style.render(this.content, width);
    }
}
