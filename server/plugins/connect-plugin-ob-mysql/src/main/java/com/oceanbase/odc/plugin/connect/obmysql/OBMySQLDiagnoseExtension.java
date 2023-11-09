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
package com.oceanbase.odc.plugin.connect.obmysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.pf4j.Extension;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.model.PlanNode;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.plugin.connect.api.SqlDiagnoseExtensionPoint;
import com.oceanbase.odc.plugin.connect.obmysql.diagnose.DiagnoseUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/6/2
 * @since ODC_release_4.2.0
 */
@Slf4j
@Extension
public class OBMySQLDiagnoseExtension implements SqlDiagnoseExtensionPoint {

    @Override
    public SqlExplain getExplain(Statement statement, @NonNull String sql) throws SQLException {
        String explainSql = "explain extended " + sql;
        String text;
        try (ResultSet resultSet = statement.executeQuery(explainSql)) {
            List<String> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            if (CollectionUtils.isEmpty(list)) {
                throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainEmpty,
                        String.format("Empty result from explain extended, sql=%s", explainSql));
            }
            text = String.join("\n", list);
        } catch (Exception e) {
            throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainFailed, e.getMessage());
        }
        SqlExplain explain = new SqlExplain();
        try {
            String[] segs = text.split("Outputs & filters");
            // 层次计划信息
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("|0 ");
            String temp = segs[0].split("--\n\\|0")[1].split("\\|\n==")[0];
            stringBuilder.append(temp).append("|");
            String formatText = stringBuilder.toString();
            // output & filter
            String[] outputSegs = segs[1].split("Used Hint")[0].split("[0-9]+ - output");
            Map<Integer, String> outputFilters = new HashMap<>();
            for (int i = 1; i < outputSegs.length; i++) {
                // 去除内存地址
                String seg = outputSegs[i].replaceAll("\\(0x[A-Za-z0-9]+\\)", "");
                String tmp = "output" + seg;
                outputFilters.put(i - 1, tmp);
            }
            String jsonTree = getJsonTreeText(formatText, outputFilters);
            explain.setExpTree(jsonTree);
            // outline
            String outline = text.split("BEGIN_OUTLINE_DATA")[1].split("END_OUTLINE_DATA")[0];
            explain.setOutline(outline);
            // 设置原始的执行计划信息
            explain.setOriginalText(text);
            explain.setShowFormatInfo(true);
        } catch (Exception e) {
            log.warn("Fail to parse explain result, origin plan text: {}", text, e);
            throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainFailed,
                    String.format("Fail to parse explain result, reason=%s", e.getMessage()));
        }
        return explain;
    }

    /**
     *
     * |0 |HASH JOIN | |98011 |258722| |1 | SUBPLAN SCAN |SUBQUERY_TABLE |101 |99182 | |2 | HASH
     * DISTINCT| |101 |99169 | |3 | TABLE SCAN |t_test_get_explain2|100000 |66272 | |4 | TABLE SCAN |t1
     * |100000 |68478 |
     *
     * @param jsonText
     * @return
     */
    private String getJsonTreeText(String jsonText, Map<Integer, String> outputFilters) {
        String[] segs = jsonText.split("\\|");
        PlanNode planTree = null;
        for (int i = 0; i < segs.length; i++) {
            if ((i - 1) % 6 == 0) {
                String id = segs[i].trim();
                String operator = segs[i + 1];
                String name = segs[i + 2].trim();
                String rowCount = segs[i + 3].trim();
                String cost = segs[i + 4].trim();
                // 构建计划树
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
        return JSON.toJSONString(planTree);
    }

    @Override
    public SqlExplain getPhysicalPlanBySqlId(Connection connection, @NonNull String sqlId) throws SQLException {
        OBMySQLInformationExtension informationExtension = new OBMySQLInformationExtension();
        String version = informationExtension.getDBVersion(connection);
        String planStatViewName = "gv$plan_cache_plan_stat";
        String planExplainViewName = "gv$plan_cache_plan_explain";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            planStatViewName = "gv$ob_plan_cache_plan_stat";
            planExplainViewName = "gv$ob_plan_cache_plan_explain";
        }
        String sql1;
        sql1 = String.format(
                "select tenant_id, svr_ip, svr_port, plan_id, outline_data from oceanbase.%s where sql_id = '%s' limit 1",
                planStatViewName, sqlId);
        String sql2 = "select `plan_id`, `operator`, `name`, `rows`, `cost`, `property` from oceanbase."
                + planExplainViewName + " where tenant_id = %s and ip ='%s' and port = %s and plan_id = %s;";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            sql2 = "select `plan_id`, `operator`, `name`, `rows`, `cost`, `property` from oceanbase."
                    + planExplainViewName
                    + " where tenant_id = %s and svr_ip = '%s' and svr_port = %s and plan_id = %s;";
        }
        return innerGetPhysicalPlan(connection, sql1, sql2);
    }

    @Override
    public SqlExplain getPhysicalPlanBySql(Connection connection, @NonNull String sql) throws SQLException {
        OBMySQLInformationExtension informationExtension = new OBMySQLInformationExtension();
        String version = informationExtension.getDBVersion(connection);
        String planStatViewName = "gv$plan_cache_plan_stat";
        String planExplainViewName = "gv$plan_cache_plan_explain";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            planStatViewName = "gv$ob_plan_cache_plan_stat";
            planExplainViewName = "gv$ob_plan_cache_plan_explain";
        }
        String sql1;
        sql1 = "select tenant_id, svr_ip, svr_port, plan_id, outline_data from oceanbase."
                + planStatViewName + " where query_sql like '%"
                + sql.replaceAll("'", "\\\\'") + "%' limit 1;";
        String sql2 = "select `plan_id`, `operator`, `name`, `rows`, `cost`, `property` from oceanbase."
                + planExplainViewName + " where tenant_id = %s and ip ='%s' and port = %s and plan_id = %s;";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            sql2 = "select `plan_id`, `operator`, `name`, `rows`, `cost`, `property` from oceanbase."
                    + planExplainViewName
                    + " where tenant_id = %s and svr_ip = '%s' and svr_port = %s and plan_id = %s;";
        }
        return innerGetPhysicalPlan(connection, sql1, sql2);
    }

    protected SqlExplain innerGetPhysicalPlan(Connection connection, String querySql1, String querySql2)
            throws SQLException {
        SqlExplain explain = new SqlExplain();
        Statement stmt = connection.createStatement();
        try (ResultSet resultSet = stmt.executeQuery(querySql1)) {
            if (!resultSet.next()) {
                throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainEmpty,
                        String.format("No plan stat found, may unsupported command, sql=%s", querySql1));
            }
            String tenantId = resultSet.getString(1);
            String svrIp = resultSet.getString(2);
            String svrPort = resultSet.getString(3);
            String planId = resultSet.getString(4);
            String outline = resultSet.getString(5);
            // 设置outline
            explain.setOutline(outline.split("BEGIN_OUTLINE_DATA")[1].split("END_OUTLINE_DATA")[0]);
            querySql2 = String.format(querySql2, tenantId, svrIp, svrPort, planId);
        }
        try (ResultSet resultSet = stmt.executeQuery(querySql2)) {
            int id = 0;
            PlanNode planTree = null;
            Integer planId = null;
            while (resultSet.next()) {
                if (null == planId) {
                    planId = Integer.parseInt(resultSet.getString(1));
                }
                // 构建计划树
                PlanNode node = new PlanNode();
                node.setId(id);
                node.setName(resultSet.getString(3));
                node.setCost(resultSet.getString(5));
                node.setDepth(DiagnoseUtil.recognizeNodeDepth(resultSet.getString(2)));
                node.setOperator(resultSet.getString(2));
                node.setRowCount(resultSet.getString(4));
                node.setOutputFilter(resultSet.getString(6));
                PlanNode tmpPlanNode = DiagnoseUtil.buildPlanTree(planTree, node);
                if (planTree == null) {
                    planTree = tmpPlanNode;
                }
                id++;
            }
            String jsonStr = JSON.toJSONString(planTree);
            explain.setExpTree(jsonStr);
        } catch (Exception e) {
            throw OBException.executeFailed(ErrorCodes.ObGetExecuteDetailFailed, e.getMessage());
        }
        return explain;
    }

    @Override
    public SqlExecDetail getExecutionDetailById(Connection connection, @NonNull String id) throws SQLException {
        // v$sql_audit中对于分区表会有多条记录，需要过滤
        String appendSql = "TRACE_ID = '" + id + "' AND LENGTH(QUERY_SQL) > 0;";
        return innerGetExecutionDetail(connection, appendSql, id);
    }

    @Override
    public SqlExecDetail getExecutionDetailBySql(Connection connection, @NonNull String sql) throws SQLException {
        String appendSql = "query_sql like '%" + sql.replaceAll("'", "\\\\'") + "%';";
        return innerGetExecutionDetail(connection, appendSql, null);
    }

    protected SqlExecDetail innerGetExecutionDetail(Connection connection, String appendSql, String traceId)
            throws SQLException {
        OBMySQLInformationExtension informationExtension = new OBMySQLInformationExtension();
        String version = informationExtension.getDBVersion(connection);
        String viewName = "gv$sql_audit";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            viewName = "gv$ob_sql_audit";
        }
        StringBuilder detailSql = new StringBuilder();
        detailSql.append("select TRACE_ID, SQL_ID, QUERY_SQL, REQUEST_TIME, ELAPSED_TIME, QUEUE_TIME, EXECUTE_TIME, "
                + "TOTAL_WAIT_TIME_MICRO, IS_HIT_PLAN, PLAN_TYPE, AFFECTED_ROWS, RETURN_ROWS, RPC_COUNT, "
                + "SSSTORE_READ_ROW_COUNT from oceanbase.").append(viewName).append(" WHERE ");
        detailSql.append(appendSql);
        Statement stmt = connection.createStatement();
        try (ResultSet resultSet = stmt.executeQuery(detailSql.toString())) {
            return DiagnoseUtil.toSQLExecDetail(resultSet);
        } catch (Exception e) {
            log.warn("Failed to get execution detail, traceId={}", traceId, e);
            throw OBException.executeFailed(ErrorCodes.ObGetExecuteDetailFailed,
                    String.format("Failed to get execution detail, traceId=%s, message=%s", traceId, e.getMessage()));
        }
    }

}
