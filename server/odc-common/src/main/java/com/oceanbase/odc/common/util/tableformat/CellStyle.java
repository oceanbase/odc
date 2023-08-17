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

/**
 * <p>
 * Defines how the content of a cell is rendered.
 * </p>
 *
 * </p>
 * It allows to specify the text alignment, the abbreviation mode and rendering of null values.
 * </p>
 *
 * @author valarcon
 */
public class CellStyle {

    private static final HorizontalAlign DEFAULT_HORIZONTAL_ALIGN = HorizontalAlign.CENTER;

    private static final AbbreviationStyle DEFAULT_ABBREVIATION_STYLE = AbbreviationStyle.DOTS;

    private static final NullStyle DEFAULT_NULL_STYLE = NullStyle.EMPTY_STRING;

    private static final String NULL_TEXT = "<null>";

    private static final String DOTS_TEXT = "...";

    private HorizontalAlign horAlign;

    private AbbreviationStyle abbStyle;

    private NullStyle nullStyle;

    /**
     * <p>
     * Default style that assumes <b>HorizontalAlign.left</b>, <b>AbbreviationStyle.dots</b> and
     * <b>NullStyle.emptyString</b>.
     * </p>
     */

    public CellStyle() {
        initialize(DEFAULT_HORIZONTAL_ALIGN, DEFAULT_ABBREVIATION_STYLE, DEFAULT_NULL_STYLE);
    }

    /**
     * <p>
     * Style with a specified horizontal alignment, that assumes <b>AbbreviationStyle.dots</b> and
     * <b>NullStyle.emptyString</b>.
     * </p>
     */

    public CellStyle(final HorizontalAlign horAlign) {
        initialize(horAlign, DEFAULT_ABBREVIATION_STYLE, DEFAULT_NULL_STYLE);
    }

    /**
     * <p>
     * Style with a specified horizontal alignment and abbreviation style, that assumes
     * <b>NullStyle.emptyString</b>.
     * </p>
     */
    public CellStyle(final HorizontalAlign horAlign, final AbbreviationStyle abbStyle) {
        initialize(horAlign, abbStyle, DEFAULT_NULL_STYLE);
    }

    /**
     * Full constructor, that specifies all characteristics.
     *
     * @param horAlign Horizontal alignment.
     * @param abbStyle Abbreviation style.
     * @param nullStyle Null style.
     */
    public CellStyle(final HorizontalAlign horAlign, final AbbreviationStyle abbStyle, final NullStyle nullStyle) {
        initialize(horAlign, abbStyle, nullStyle);
    }

    /**
     * @param width
     * @return String
     */
    static String renderNullCell(final int width) {
        return Filler.getFiller(width);
    }

    /**
     * @param horAlign
     * @param abbStyle
     * @param nullStyle
     */
    private void initialize(final HorizontalAlign horAlign, final AbbreviationStyle abbStyle,
            final NullStyle nullStyle) {
        this.horAlign = horAlign;
        this.abbStyle = abbStyle;
        this.nullStyle = nullStyle;
    }

    /**
     * @param txt
     * @return String
     */
    private String renderUnformattedText(final String txt) {
        if (txt == null) {
            if (NullStyle.EMPTY_STRING.equals(this.nullStyle)) {
                return "";
            }
            return NULL_TEXT;
        }
        return txt;
    }

    /**
     * Returns the width of a rendered text, based on the cell content and style.
     *
     * @param txt Text to render.
     * @return width of a rendered text, based on the cell style.
     */
    public int getWidth(final String txt) {
        return renderUnformattedText(txt).length();
    }

    /**
     * Renders a text based on the cell content, style and specified width.
     *
     * @param txt Text to render.
     * @param width Fixed width to accommodate the test to.
     * @return Rendered text based on the cell style and the specified width.
     */
    public String render(final String txt, final int width) {
        String plainText = renderUnformattedText(txt);

        // Text too short.

        if (plainText.length() < width) {
            switch (this.horAlign) {
                case LEFT:
                    return alignLeft(plainText, width);
                case CENTER:
                    return alignCenter(plainText, width);
                default:
                    return alignRight(plainText, width);
            }
        }

        // Text that fits perfect.

        if (plainText.length() == width) {
            return plainText;
        }

        // Text too long.

        switch (this.abbStyle) {
            case CROP:
                return abbreviateCrop(plainText, width);
            default:
                return abbreviateDots(plainText, width);
        }
    }

    private String alignLeft(final String txt, final int width) {
        int diff = width - txt.length();
        return txt + Filler.getFiller(diff);
    }

    private String alignCenter(final String txt, final int width) {
        int diff = width - txt.length();
        int diffLeft = diff / 2;
        int diffRight = diff - diffLeft;
        return Filler.getFiller(diffLeft) + txt + Filler.getFiller(diffRight);
    }

    private String alignRight(final String txt, final int width) {
        int diff = width - txt.length();
        return Filler.getFiller(diff) + txt;
    }

    private String abbreviateCrop(final String txt, final int width) {
        return txt.substring(0, width);
    }

    private String abbreviateDots(final String txt, final int width) {
        if (width < 1) {
            return "";
        }
        if (width <= DOTS_TEXT.length()) {
            return DOTS_TEXT.substring(0, width);
        }
        return txt.substring(0, width - DOTS_TEXT.length()) + DOTS_TEXT;
    }

    /**
     * This enumeration is used to specify how a text is horizontally aligned in a cell.
     *
     * @author valarcon
     */
    public enum HorizontalAlign {

        /**
         * Will align to the left.
         */
        LEFT,

        /**
         * Will center the text.
         */
        CENTER,

        /**
         * Will align to the right.
         */
        RIGHT
    }

    /**
     * This enumeration is used to specify how to reduce a text to fit it in a small cell.
     *
     * @author valarcon
     */
    public enum AbbreviationStyle {

        /**
         * Will simply crop the text at the maximum allowed length.
         */
        CROP,

        /**
         * Will add three dots at the end of the text to show it has been abbreviated.
         */
        DOTS
    }

    /**
     * This enumeration is used to specify how to display cell with null values.
     *
     * @author valarcon
     */
    public enum NullStyle {

        /**
         * Will show a empty string (zero-length).
         */
        EMPTY_STRING,

        /**
         * Will display the text <code>&lt;null&gt;</code> instead.
         */
        NULL_TEXT
    }
}
