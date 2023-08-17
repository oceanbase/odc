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

/**
 * <p>
 * Border style define the actual character(s) that will be rendered as borders, when a border or
 * separator is shown.
 * </p>
 *
 * <p>
 * You can use predefined styles or custom styles.
 * </p>
 *
 * <h3>Predefined Styles</h3>
 *
 * <p>
 * The following border styles are already predefined and can be used out of the box:
 * </p>
 *
 * <p>
 * <b>Note:</b> Styles starting with <b>UNICODE_</b> use characters (codepoints) outside the ASCII
 * and ISO-8859-1 (Latin-1) sets. Therefore, some Operating Systems or tools might not be able to
 * display them correctly. Avoid these predefined tiles if you need maximum compatibility.
 * </p>
 *
 * <p>
 * <b>BorderStyle.HORIZONTAL_ONLY</b>
 * </p>
 *
 * <pre class='example'>
 *
 *  --------- ----------- ---------- --------
 *  Country    Population Area (km2)  Density
 *  --------- ----------- ---------- --------
 *  Special 1          1.350.000.000    16.67
 *  --------- ----------- ---------- --------
 *  Total     147 000 000  9 250 000    15.89
 *  --------- ----------- ---------- --------
 * </pre>
 *
 * <p>
 * Use any tiles that fits your needs.
 * </p>
 */
@Getter
public class BorderStyle {

    /**
     *
     */
    public static final BorderStyle HORIZONTAL_ONLY =
            // new BorderStyle("", "-", " ", "", "", "-", " ", "", "", "-", " ", "", "", " ", "", " ", " ");
            new BorderStyle("", "-", "-", "", "", "-", "-", "", "", "-", "-", "", "", "|", "", " ", " ");

    private static final String DEFAULT_TILE = "*";

    // Top
    private String tLCorner;

    private String top;

    private String tCCorner;

    private String tRCorner;

    // Middle
    private String mLCorner;

    private String middle;

    private String mCCorner;

    private String mRCorner;

    // Bottom
    private String bLCorner;

    private String bottom;

    private String bCCorner;

    private String bRCorner;

    // Vertical separators
    private String left;

    private String center;

    private String right;

    // Column span characters
    private String upperColSpan;

    private String lowerColSpan;

    // Tile widths
    private int leftWidth;

    private int horizontalWidth;

    private int centerWidth;

    private int rightWidth;

    /**
     * Specifies a border style with multi-character tiles.
     *
     * @param tLCorner Top left corner
     * @param top Top
     * @param tCCorner Top center corner
     * @param tRCorner Top right corner
     * @param mLCorner Middle left corner
     * @param middle Middle
     * @param mCCorner Middle center corner
     * @param mRCorner Middle right corner
     * @param bLCorner Bottom left corner
     * @param bottom Bottom
     * @param bCCorner Bottom center corner
     * @param bRCorner Bottom right corner
     * @param left Left
     * @param center Center
     * @param right Right
     * @param upperColSpan middle center corner for the upper border of a colspan
     * @param lowerColSpan middle center corner for the lower border of a colspan
     */
    public BorderStyle(final String tLCorner, final String top, final String tCCorner, final String tRCorner,
            final String mLCorner, final String middle, final String mCCorner, final String mRCorner,
            final String bLCorner,
            final String bottom, final String bCCorner, final String bRCorner, final String left, final String center,
            final String right, final String upperColSpan, final String lowerColSpan) {

        this.leftWidth = maxWidth(tLCorner, mLCorner, bLCorner, left);
        this.horizontalWidth = 1; // maxWidth(top, middle, bottom);
        this.centerWidth = maxWidth(tCCorner, mCCorner, bCCorner, center, upperColSpan, lowerColSpan);
        this.rightWidth = maxWidth(tRCorner, mRCorner, bRCorner, right);

        this.tLCorner = adjustString(tLCorner, this.leftWidth);
        this.top = adjustString(top, this.horizontalWidth);
        this.tCCorner = adjustString(tCCorner, this.centerWidth);
        this.tRCorner = adjustString(tRCorner, this.rightWidth);
        this.mLCorner = adjustString(mLCorner, this.leftWidth);
        this.middle = adjustString(middle, this.horizontalWidth);
        this.mCCorner = adjustString(mCCorner, this.centerWidth);
        this.mRCorner = adjustString(mRCorner, this.rightWidth);
        this.bLCorner = adjustString(bLCorner, this.leftWidth);
        this.bottom = adjustString(bottom, this.horizontalWidth);
        this.bCCorner = adjustString(bCCorner, this.centerWidth);
        this.bRCorner = adjustString(bRCorner, this.rightWidth);
        this.left = adjustString(left, this.leftWidth);
        this.center = adjustString(center, this.centerWidth);
        this.right = adjustString(right, this.rightWidth);
        this.upperColSpan = adjustString(upperColSpan, this.centerWidth);
        this.lowerColSpan = adjustString(lowerColSpan, this.centerWidth);
    }

    /**
     * Specifies a border style with single-character tiles.
     *
     * @param customStyle 15-character String where every character is a tile, in the following order:
     *        <ul>
     *        <li>Top left corner</li>
     *        <li>Top</li>
     *        <li>Top center corner</li>
     *        <li>Top right corner</li>
     *        <li>Middle left corner</li>
     *        <li>Middle</li>
     *        <li>Middle center corner</li>
     *        <li>Middle right corner</li>
     *        <li>Bottom left corner</li>
     *        <li>Bottom</li>
     *        <li>Bottom center corner</li>
     *        <li>Bottom right corner</li>
     *        <li>Left</li>
     *        <li>Center</li>
     *        <li>Right</li>
     *        <li>Upper colspan</li>
     *        <li>Lower colspan</li>
     *        </ul>
     */
    public BorderStyle(final String customStyle) {
        this.leftWidth = 1;
        this.horizontalWidth = 1;
        this.centerWidth = 1;
        this.rightWidth = 1;

        this.tLCorner = get(customStyle, 0);
        this.top = get(customStyle, 1);
        this.tCCorner = get(customStyle, 2);
        this.tRCorner = get(customStyle, 3);
        this.mLCorner = get(customStyle, 4);
        this.middle = get(customStyle, 5);
        this.mCCorner = get(customStyle, 6);
        this.mRCorner = get(customStyle, 7);
        this.bLCorner = get(customStyle, 8);
        this.bottom = get(customStyle, 9);
        this.bCCorner = get(customStyle, 10);
        this.bRCorner = get(customStyle, 11);
        this.left = get(customStyle, 12);
        this.center = get(customStyle, 13);
        this.right = get(customStyle, 14);
        this.upperColSpan = get(customStyle, 15);
        this.lowerColSpan = get(customStyle, 16);
    }

    /**
     * @param a
     * @param b
     * @param c
     * @return int
     */
    private int maxWidth(final String a, final String b, final String c) {
        return Math.max(Math.max(tileWidth(a), tileWidth(b)), tileWidth(c));
    }

    /**
     * @param a
     * @param b
     * @param c
     * @param d
     * @return int
     */
    private int maxWidth(final String a, final String b, final String c, final String d) {
        return Math.max(Math.max(Math.max(tileWidth(a), tileWidth(b)), tileWidth(c)), tileWidth(d));
    }

    /**
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @return int
     */
    private int maxWidth(final String a, final String b, final String c, final String d, final String e,
            final String f) {
        return Math.max(maxWidth(a, b, c), maxWidth(d, e, f));
    }

    /**
     * @param txt
     * @param width
     * @return String
     */
    private String adjustString(final String txt, final int width) {
        if (txt == null) {
            return Filler.getFiller(width);
        }
        if (txt.length() == width) {
            return txt;
        }
        if (txt.length() > width) {
            return txt.substring(0, width);
        }
        int diff = width - txt.length();
        return txt + Filler.getFiller(diff);
    }

    /**
     * @param tile
     * @return int
     */
    private int tileWidth(final String tile) {
        if (tile == null) {
            return 0;
        }
        return tile.length();
    }

    private String get(final String style, final int index) {
        if (style == null) {
            return DEFAULT_TILE;
        }
        if (index < 0 || index >= style.length()) {
            return DEFAULT_TILE;
        }
        return style.substring(index, index + 1);
    }

    // Accessors
    public String getBCCorner() {
        return this.bCCorner;
    }

    public String getBLCorner() {
        return this.bLCorner;
    }

    public String getBottom() {
        return this.bottom;
    }

    public String getBRCorner() {
        return this.bRCorner;
    }

    public String getCenter() {
        return this.center;
    }

    public String getLeft() {
        return this.left;
    }

    public String getMCCorner() {
        return this.mCCorner;
    }

    public String getMiddle() {
        return this.middle;
    }

    public String getMLCorner() {
        return this.mLCorner;
    }

    public String getMRCorner() {
        return this.mRCorner;
    }

    public String getRight() {
        return this.right;
    }

    public String getTop() {
        return this.top;
    }

    public String getTCCorner() {
        return this.tCCorner;
    }

    public String getTLCorner() {
        return this.tLCorner;
    }

    public String getTRCorner() {
        return this.tRCorner;
    }

    public String getUpperColSpan() {
        return this.upperColSpan;
    }

    public String getLowerColSpan() {
        return this.lowerColSpan;
    }

    public int getLeftWidth() {
        return leftWidth;
    }

    public int getHorizontalWidth() {
        return horizontalWidth;
    }

    public int getCenterWidth() {
        return centerWidth;
    }

    public int getRightWidth() {
        return rightWidth;
    }
}
