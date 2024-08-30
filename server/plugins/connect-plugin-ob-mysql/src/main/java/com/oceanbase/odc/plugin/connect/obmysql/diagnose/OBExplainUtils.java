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
package com.oceanbase.odc.core.sql.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.tableformat.BorderStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.PlanNode;
import com.oceanbase.odc.plugin.connect.obmysql.diagnose.DiagnoseUtil;

public class OBExplainUtils {

    private static final Pattern OPERATOR_PATTERN = Pattern.compile(".+\\|(OPERATOR +)\\|.+");

    public static Table getMySQLExplainResultSet(ResultSet resultSet) throws SQLException {

        ResultSetMetaData metaData = resultSet.getMetaData();
        int colCount = metaData.getColumnCount();
        Table table = new Table(colCount, BorderStyle.HORIZONTAL_ONLY);
        CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
        for (int i = 1; i <= colCount; i++) {
            table.setColumnWidth(i - 1, 10, metaData.getColumnDisplaySize(i));
            table.addCell(metaData.getColumnName(i), cs);
        }
        while (resultSet.next()) {
            for (int i = 1; i <= colCount; i++) {
                table.addCell(resultSet.getString(i), cs);
            }
        }
        return table;
    }

    public static String getOBMySQLExplainResultSet(ResultSet resultSet) throws SQLException {
        List<String> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(resultSet.getString(1));
        }
        if (CollectionUtils.isEmpty(list)) {
            throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainEmpty,
                String.format("Empty result from explain extended, sql=%s", resultSet.getStatement().toString()));
        }
        return String.join("\n", list);
    }

    public static PlanNode getOBMySQLExplainPlan(String planText) {

        String[] segs = planText.split("Outputs & filters")[0].split("\n");
        String headerLine = segs[1];
        Matcher matcher = OPERATOR_PATTERN.matcher(headerLine);
        if (!matcher.matches()) {
            throw new UnexpectedException("Invalid explain:" + planText);
        }
        int operatorStartIndex = matcher.start(1);
        int operatorStrLen = matcher.end(1) - operatorStartIndex;

        PlanNode tree = null;
        for (int i = 0; i < segs.length - 4; i++) {
            PlanNode node = new PlanNode();
            node.setId(i);

            String line = segs[i + 3];
            String temp = line.substring(operatorStartIndex);
            String operatorStr = temp.substring(0, operatorStrLen);
            DiagnoseUtil.recognizeNodeDepthAndOperator(node, operatorStr);

            String[] others = temp.substring(operatorStrLen).split("\\|");
            node.setName(others[1]);
            node.setRowCount(others[2]);
            node.setCost(others[3]);
            node.setRealRowCount(others[4]);
            node.setRealCost(others[5]);

            PlanNode tmpPlanNode = DiagnoseUtil.buildPlanTree(tree, node);
            if (tree == null) {
                tree = tmpPlanNode;
            }
        }
        return tree;
    }
}
