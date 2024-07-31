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
package com.oceanbase.odc.plugin.connect.obmysql.diagnose;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.PlanNode;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;

/**
 * @author jingtian
 * @date 2023/6/12
 * @since ODC_release_4.2.0
 */
public class DiagnoseUtil {
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(".+\\|(OPERATOR +)\\|.+");

    public static PlanNode buildPlanTree(PlanNode planTree, PlanNode node) {
        if (planTree == null) {
            planTree = node;
        } else {
            recursiveBuild(planTree, node);
        }
        return planTree;
    }

    public static String getNonexistKey(PlanNode node, String key) {
        LinkedHashMap<String, PlanNode> map = node.getChildren();
        String tmpKey = key;
        while (true) {
            if (map.containsKey(tmpKey)) {
                tmpKey = tmpKey + " ";
            } else {
                break;
            }
        }
        return tmpKey;
    }

    public static boolean recursiveBuild(PlanNode planTree, PlanNode node) {
        boolean flag;
        if (planTree.getChildren() == null
                || planTree.getChildren().size() == 0
                || (planTree.getDepth() < node.getDepth()
                        && planTree.getChildren().values().stream().allMatch(p -> p.getDepth() >= node.getDepth()))) {
            String key = node.getOperator();
            key = getNonexistKey(planTree, key);
            planTree.getChildren().put(key, node);
            flag = true;
        } else {
            LinkedHashMap<String, PlanNode> children = planTree.getChildren();
            Map.Entry<String, PlanNode> nodeEntry =
                    (Map.Entry<String, PlanNode>) children.entrySet().toArray()[children.size() - 1];
            flag = recursiveBuild(nodeEntry.getValue(), node);
            if (flag) {
                return flag;
            }
        }
        return flag;
    }

    public static int recognizeNodeDepth(String operator) {
        int depth = -1;
        for (int i = 0; i < operator.length(); i++) {
            if (operator.charAt(i) == ' ') {
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    public static void recognizeNodeDepthAndOperator(PlanNode node, String operator) {
        int index = operator.indexOf("+-");
        if (index != -1) {
            node.setDepth(index / 2);
            node.setOperator(operator.substring(index + 2).trim());
            return;
        }
        index = operator.indexOf("|-");
        if (index != -1) {
            node.setDepth(index / 2);
            node.setOperator(operator.substring(index + 2).trim());
            return;
        }
        node.setDepth(-1);
        node.setOperator(operator.trim());
    }

    public static SqlExecDetail toSQLExecDetail(ResultSet resultSet) throws SQLException {
        SqlExecDetail detail = new SqlExecDetail();
        if (!resultSet.next()) {
            throw new IllegalStateException("No result found in sql audit.");
        }
        detail.setTraceId(resultSet.getString(1));
        detail.setSqlId(resultSet.getString(2));
        detail.setSql(resultSet.getString(3));
        detail.setReqTime(Long.parseLong(resultSet.getString(4)) / 1000);
        detail.setTotalTime(Long.parseLong(resultSet.getString(5)));
        detail.setQueueTime(Long.parseLong(resultSet.getString(6)));
        detail.setExecTime(Long.parseLong(resultSet.getString(7)));
        detail.setWaitTime(Long.parseLong(resultSet.getString(8)));
        detail.setHitPlanCache("1".equals(resultSet.getString(9)));
        detail.setPlanType(getPlanTypeName(Integer.parseInt(resultSet.getString(10))));
        detail.setReturnRows(Integer.parseInt(resultSet.getString(12)));
        detail.setAffectedRows(Integer.parseInt(resultSet.getString(11)));
        detail.setRpcCount(Integer.parseInt(resultSet.getString(13)));
        detail.setSsstoreRead(Integer.parseInt(resultSet.getString(14)));
        return detail;
    }

    public static String getPlanTypeName(int code) {
        switch (code) {
            case 0:
                return "UNINITIALIZED";
            case 1:
                return "LOCAL";
            case 2:
                return "REMOTE";
            case 3:
                return "DISTRIBUTED";
            default:
                return "UNCERTAIN";
        }
    }

    /**
     *
     * |0 |HASH JOIN | |98011 |258722| |1 | SUBPLAN SCAN |SUBQUERY_TABLE |101 |99182 | |2 | HASH
     * DISTINCT| |101 |99169 | |3 | TABLE SCAN |t_test_get_explain2|100000 |66272 | |4 | TABLE SCAN |t1
     * |100000 |68478 |
     */
    public static String getExplainTree(String text) {
        String[] splits = text.split("Outputs & filters");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("|0 ");
        String temp = splits[0].split("--\n\\|0")[1].split("\\|\n==")[0];
        stringBuilder.append(temp).append("|");
        String formatText = stringBuilder.toString();
        // output & filter
        String[] outputSegs = splits[1].split("Used Hint")[0].split("[0-9]+ - output");
        Map<Integer, String> outputFilters = new HashMap<>();
        for (int i = 1; i < outputSegs.length; i++) {
            String seg = outputSegs[i].replaceAll("\\(0x[A-Za-z0-9]+\\)", "");
            String tmp = "output" + seg;
            outputFilters.put(i - 1, tmp);
        }

        String[] segs = formatText.split("\\|");
        PlanNode planTree = null;
        for (int i = 0; i < segs.length; i++) {
            if ((i - 1) % 6 == 0) {
                String id = segs[i].trim();
                String operator = segs[i + 1];
                String name = segs[i + 2].trim();
                String rowCount = segs[i + 3].trim();
                String cost = segs[i + 4].trim();

                int depth = DiagnoseUtil.recognizeNodeDepth(operator);
                PlanNode node = new PlanNode();
                node.setId(Integer.parseInt(id.trim()));
                node.setName(name);
                node.setCost(cost);
                node.setDepth(depth);
                node.setOperator(operator);
                node.setRowCount(rowCount);
                node.setOutputFilter(outputFilters.get(Integer.parseInt(id)));
                PlanNode tmpPlanNode = DiagnoseUtil.buildPlanTree(planTree, node);
                if (planTree == null) {
                    planTree = tmpPlanNode;
                }
            }
        }
        return JsonUtils.toJson(planTree);
    }

    public static String getOB4xExplainTree(String text) {
        String[] segs = text.split("Outputs & filters");

        // output & filter
        String[] outputSegs = segs[1].split("Used Hint")[0].split("[0-9]+ - output");
        Map<Integer, String> outputFilters = new HashMap<>();
        for (int i = 1; i < outputSegs.length; i++) {
            String seg = outputSegs[i].replaceAll("\\(0x[A-Za-z0-9]+\\)", "");
            String tmp = "output" + seg;
            outputFilters.put(i - 1, tmp);
        }

        String[] lines = segs[0].split("\n");
        String headerLine = lines[1];
        Matcher matcher = OPERATOR_PATTERN.matcher(headerLine);
        if (!matcher.matches()) {
            throw new UnexpectedException("Invalid explain:" + text);
        }
        int operatorStartIndex = matcher.start(1);
        int operatorStrLen = matcher.end(1) - operatorStartIndex;

        PlanNode tree = null;
        for (int i = 0; i < lines.length - 4; i++) {
            PlanNode node = new PlanNode();
            node.setId(i);

            String line = lines[i + 3];
            String temp = line.substring(operatorStartIndex);
            String operatorStr = temp.substring(0, operatorStrLen);
            DiagnoseUtil.recognizeNodeDepthAndOperator(node, operatorStr);

            String[] others = temp.substring(operatorStrLen).split("\\|");
            node.setName(others[1]);
            node.setRowCount(others[2]);
            node.setCost(others[3]);
            node.setOutputFilter(outputFilters.get(i));

            PlanNode tmpPlanNode = DiagnoseUtil.buildPlanTree(tree, node);
            if (tree == null) {
                tree = tmpPlanNode;
            }
        }
        return JsonUtils.toJson(tree);
    }

}
