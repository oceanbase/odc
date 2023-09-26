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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;

/**
 * @author yaobin
 * @date 2023-07-20
 * @since 4.2.0
 */
public class DefaultTableFactory implements TableFactory {

    @Override
    public Table generateTable(int tableColumn, List<String> header, List<String> body) {
        Table table = new Table(tableColumn, BorderStyle.HORIZONTAL_ONLY);
        for (int i = 0; i < tableColumn; i++) {
            table.setColumnWidth(i, 10, 30);
        }
        // set header
        setCell(table, header);
        // set body
        setCell(table, body);
        return table;
    }

    private void setCell(Table table, List<String> rowContent) {
        if (CollectionUtils.isEmpty(rowContent)) {
            return;
        }
        CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
        rowContent.forEach(h -> table.addCell(h != null ? h : "", cs));
    }

}
