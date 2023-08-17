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
package com.oceanbase.odc.common.tableFormat;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.tableformat.BorderStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;
import com.oceanbase.odc.common.util.tableformat.Table;

/**
 * @author jingtian
 * @date 2023/6/6
 * @since ODC_release_4.2.0
 */
public class TableTest {

    @Test
    public void test_create_table_with_default_border_style_success() {
        Table table = new Table(2, BorderStyle.HORIZONTAL_ONLY);
        CellStyle cs = new CellStyle();
        table.setColumnWidth(0, 5, 10);
        table.setColumnWidth(1, 5, 10);
        table.addCell("1", cs);
        table.addCell("2", cs);
        Assert.assertNotNull(table.render().toString());
    }

    @Test
    public void test_create_table_with_custom_border_style_success() {
        Table table = new Table(2,
                new BorderStyle("*", "-", "-", "*", "*", "-", "-", "*", "*", "-", "-", "*", "|", "|", "|", " ", " "));
        CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
        table.setColumnWidth(0, 5, 10);
        table.setColumnWidth(1, 5, 10);
        table.addCell("1", cs);
        table.addCell("2", cs);
        table.addCell("3", cs);
        table.addCell("4", cs);
        Assert.assertNotNull(table.render().toString());
    }
}
