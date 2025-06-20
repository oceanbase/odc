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

import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.DB_TIME;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.IS_HIT_PLAN_CACHE;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.PLAN_TYPE;
import static com.oceanbase.odc.plugin.connect.obmysql.diagnose.ProfileConstants.QUEUE_TIME;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.pf4j.Extension;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.TimespanFormatUtil;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.OBExecutionServerInfo;
import com.oceanbase.odc.core.shared.model.OBSqlPlan;
import com.oceanbase.odc.core.shared.model.PlanNode;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlPlanMonitor;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.plugin.connect.api.SqlDiagnoseExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraph;
import com.oceanbase.odc.plugin.connect.model.diagnose.SqlExplain;
import com.oceanbase.odc.plugin.connect.obmysql.diagnose.DiagnoseUtil;
import com.oceanbase.odc.plugin.connect.obmysql.diagnose.PlanGraphBuilder;
import com.oceanbase.odc.plugin.connect.obmysql.diagnose.QueryProfileHelper;

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
    private static final String OB_EXPLAIN_COMPATIBLE_VERSION = "4.0";
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(".+\\|(OPERATOR +)\\|.+");

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
            String version = OBUtils.getObVersion(statement.getConnection());
            if (VersionUtils.isGreaterThanOrEqualsTo(version, OB_EXPLAIN_COMPATIBLE_VERSION)) {
                explain.setExpTree(DiagnoseUtil.getOB4xExplainTree(text));
            } else {
                explain.setExpTree(DiagnoseUtil.getExplainTree(text));
            }
            // outline
            String outline = text.split("BEGIN_OUTLINE_DATA")[1].split("END_OUTLINE_DATA")[0];
            explain.setOutline(outline);
            // 设置原始的执行计划信息
            explain.setOriginalText(text);
            explain.setShowFormatInfo(true);
            explain.setGraph(getSqlPlanGraphBySql(statement, sql));
        } catch (Exception e) {
            log.warn("Fail to parse explain result, origin plan text: {}", text, e);
            throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainFailed,
                    String.format("Fail to parse explain result, reason=%s", e.getMessage()));
        }
        return explain;
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
        String appendSql = "TRACE_ID = '" + id + "' AND IS_INNER_SQL = 0;";
        return innerGetExecutionDetail(connection, appendSql, id);
    }

    @Override
    public SqlExecDetail getExecutionDetailBySql(Connection connection, @NonNull String sql) throws SQLException {
        String appendSql = "query_sql like '%" + sql.replaceAll("'", "\\\\'") + "%';";
        return innerGetExecutionDetail(connection, appendSql, null);
    }

    @Override
    public SqlExplain getQueryProfileByTraceIdAndSessIds(Connection connection, @NonNull String traceId,
            @NonNull List<String> sessionIds) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            OBExecutionServerInfo executorInfo = getPlanIdByTraceIdAndSessIds(stmt, traceId, sessionIds);
            Verify.notEmpty(executorInfo.getPlanId(), "plan id");
            PlanGraph graph = getPlanGraph(stmt, executorInfo);
            graph.setTraceId(traceId);
            QueryProfileHelper.refreshGraph(graph, getSqlPlanMonitorRecords(stmt, traceId));

            try {
                SqlExecDetail execDetail = getExecutionDetailById(connection, traceId);
                Verify.notNull(execDetail, "exec detail");
                graph.putOverview(QUEUE_TIME,
                        TimespanFormatUtil.formatTimespan(execDetail.getQueueTime(), TimeUnit.MICROSECONDS));
                graph.putOverview(PLAN_TYPE, execDetail.getPlanType());
                graph.putOverview(IS_HIT_PLAN_CACHE, execDetail.isHitPlanCache() + "");
                if (graph.getDuration() == 0) {
                    graph.setDuration(execDetail.getExecTime());
                    graph.putOverview(DB_TIME,
                            TimespanFormatUtil.formatTimespan(execDetail.getExecTime(), TimeUnit.MICROSECONDS));
                }
            } catch (Exception e) {
                log.warn("Failed to query sql audit with OB trace_id={}.", traceId, e);
            }
            SqlExplain explain;
            try {
                explain = innerGetSqlExplainByDbmsXplan(stmt, executorInfo);
            } catch (Exception e) {
                log.warn("Failed to query plan by dbms_xplan.display_cursor.", e);
                explain = new SqlExplain();
                explain.setShowFormatInfo(false);
            }
            explain.setGraph(graph);
            return explain;
        }
    }

    protected OBExecutionServerInfo getPlanIdByTraceIdAndSessIds(Statement stmt, String traceId,
            List<String> sessionIds)
            throws SQLException {
        try {
            return OBUtils.queryPlanIdByTraceIdFromASH(stmt, traceId, sessionIds, ConnectType.OB_MYSQL);
        } catch (SQLException e) {
            return OBUtils.queryPlanIdByTraceIdFromAudit(stmt, traceId, ConnectType.OB_MYSQL);
        }
    }

    protected String getPhysicalPlanByDbmsXplan(Statement stmt, OBExecutionServerInfo executorInfo)
            throws SQLException {
        String sql = String.format("select dbms_xplan.display_cursor(%s,'ALL','%s',%s,%s)",
                executorInfo.getPlanId(), executorInfo.getIp(), executorInfo.getPort(), executorInfo.getTenantId());
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                throw new SQLException("Failed to query plan by dbms_xplan.display_cursor");
            }
            return rs.getString(1);
        }
    }

    protected PlanGraph getPlanGraph(Statement stmt, OBExecutionServerInfo executorInfo) throws SQLException {
        List<OBSqlPlan> planRecords = OBUtils.queryOBSqlPlanByPlanId(stmt, executorInfo, ConnectType.OB_MYSQL);
        return PlanGraphBuilder.buildPlanGraph(planRecords);
    }

    protected List<SqlPlanMonitor> getSqlPlanMonitorRecords(Statement stmt, String traceId) throws SQLException {
        Map<String, String> statId2Name = OBUtils.querySPMStatNames(stmt, ConnectType.OB_MYSQL);
        return OBUtils.querySqlPlanMonitorStats(
                stmt, traceId, ConnectType.OB_MYSQL, statId2Name);
    }

    /**
     * <pre>
     *     ===========================================================================================================
     *     |ID|OPERATOR              |NAME    |EST.ROWS|EST.TIME(us)|REAL.ROWS|REAL.TIME(us)|IO TIME(us)|CPU TIME(us)|
     *     -----------------------------------------------------------------------------------------------------------
     *     |0 |PX COORDINATOR MERGE  |        |697     |50767       |2        |545699       |509932     |546311      |
     *     |1 |+-EXCHANGE OUT DISTR  |:EX10007|697     |50370       |2        |543539       |0          |59          |
     *     |2 |  +-SORT              |        |697     |50259       |2        |543539       |0          |78          |
     *     |9 |  | +-SHARED HASH JOIN|        |2570    |47498       |2549     |525890       |0          |7917        |
     *     ===========================================================================================================
     *     Outputs & filters:
     *     -------------------------------------
     *       0 - output...
     * </pre>
     */
    private SqlExplain innerGetSqlExplainByDbmsXplan(Statement stmt, OBExecutionServerInfo executorInfo)
            throws SQLException {
        SqlExplain sqlExplain = new SqlExplain();
        sqlExplain.setShowFormatInfo(true);

        String planText = getPhysicalPlanByDbmsXplan(stmt, executorInfo);
        sqlExplain.setOriginalText(planText);
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
        sqlExplain.setExpTree(JsonUtils.toJson(tree));
        return sqlExplain;
    }

    private PlanGraph getSqlPlanGraphBySql(Statement statement, @NonNull String sql) throws SQLException {
        String explain = "explain format=json " + sql;
        StringBuilder planJson = new StringBuilder();
        try (ResultSet rs = statement.executeQuery(explain)) {
            while (rs.next()) {
                planJson.append(rs.getString(1));
            }
        }
        explain = "explain " + sql;
        List<String> queryPlan = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery(explain)) {
            while (rs.next()) {
                queryPlan.add(rs.getString(1).trim());
            }
        }
        String planText = String.join("\n", queryPlan);
        String[] segs = planText.split("Outputs & filters");
        String[] outputSegs = segs[1].split("Used Hint")[0].split("[0-9]+ - output");
        Map<String, String> outputFilters = new HashMap<>();
        for (int i = 1; i < outputSegs.length; i++) {
            outputFilters.put(i - 1 + "", "output" + outputSegs[i]);
        }
        return PlanGraphBuilder.buildPlanGraph(
                JsonUtils.fromJsonMap(planJson.toString(), String.class, Object.class), outputFilters);
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
