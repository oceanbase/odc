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
import java.util.LinkedHashMap;
import java.util.Map;

import com.oceanbase.odc.core.shared.model.PlanNode;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;

/**
 * @author jingtian
 * @date 2023/6/12
 * @since ODC_release_4.2.0
 */
public class DiagnoseUtil {
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
        PlanNode tempNode = planTree;
        boolean flag = false;
        if (tempNode.getDepth() + 1 == node.getDepth()) {
            String key = node.getOperator();
            key = getNonexistKey(tempNode, key);
            tempNode.getChildren().put(key, node);
            flag = true;
        } else {
            LinkedHashMap<String, PlanNode> children = tempNode.getChildren();
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

    public static SqlExecDetail toSQLExecDetail(ResultSet resultSet) throws SQLException {
        SqlExecDetail detail = new SqlExecDetail();
        if (!resultSet.next()) {
            return detail;
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
}
