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
package com.oceanbase.odc.plugin.connect.oboracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.pf4j.Extension;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.model.PlanNode;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLDiagnoseExtension;
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
public class OBOracleDiagnoseExtension extends OBMySQLDiagnoseExtension {

    @Override
    public SqlExplain getPhysicalPlanBySqlId(Connection connection, @NonNull String sqlId) throws SQLException {
        OBOracleInformationExtension informationExtension = new OBOracleInformationExtension();
        String version = informationExtension.getDBVersion(connection);
        String planStatViewName = "gv$plan_cache_plan_stat";
        String planExplainViewName = "gv$plan_cache_plan_explain";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            planStatViewName = "gv$ob_plan_cache_plan_stat";
            planExplainViewName = "gv$ob_plan_cache_plan_explain";
        }
        String sql1;
        sql1 = String.format("select tenant_id, svr_ip, svr_port, plan_id, outline_data from "
                + "SYS.%s where sql_id = '%s' and rownum < 2", planStatViewName, sqlId);
        String sql2 = "select plan_id, operator," + "\"NAME\", \"ROWS\","
                + " cost, property from SYS." + planExplainViewName
                + " where tenant_id = %s and svr_ip = '%s' and svr_port = %s and plan_id = %s;";
        return innerGetPhysicalPlan(connection, sql1, sql2);
    }

    @Override
    public SqlExplain getPhysicalPlanBySql(Connection connection, @NonNull String sql) throws SQLException {
        OBOracleInformationExtension informationExtension = new OBOracleInformationExtension();
        String version = informationExtension.getDBVersion(connection);
        String planStatViewName = "gv$plan_cache_plan_stat";
        String planExplainViewName = "gv$plan_cache_plan_explain";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            planStatViewName = "gv$ob_plan_cache_plan_stat";
            planExplainViewName = "gv$ob_plan_cache_plan_explain";
        }
        String sql1;
        sql1 = "select tenant_id, svr_ip, svr_port, plan_id, outline_data from SYS."
                + planStatViewName + " where query_sql like '%"
                + sql.replaceAll("'", "''") + "%' and rownum < 2";
        String sql2 = "select plan_id, operator," + "\"NAME\", \"ROWS\","
                + " cost, property from SYS." + planExplainViewName
                + " where tenant_id = %s and svr_ip = '%s' and svr_port = %s and plan_id = %s;";
        return innerGetPhysicalPlan(connection, sql1, sql2);
    }

    @Override
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
            String svvrPort = resultSet.getString(3);
            String planId = resultSet.getString(4);
            String outline = resultSet.getString(5);
            // 设置outline
            explain.setOutline(outline.split("BEGIN_OUTLINE_DATA")[1].split("END_OUTLINE_DATA")[0]);
            querySql2 = String.format(querySql2, tenantId, svrIp, svvrPort, planId);
        }
        try (ResultSet resultSet = stmt.executeQuery(querySql2)) {
            int id = 0;
            PlanNode planTree = null;
            Integer planId = null;
            while (resultSet.next()) {
                if (null == planId) {
                    planId = Integer.parseInt(resultSet.getString(1));
                }
                String operator = resultSet.getString(2);
                // 构建计划树
                PlanNode node = new PlanNode();
                node.setId(id);
                node.setName(resultSet.getString(3));
                node.setCost(resultSet.getString(5));
                node.setDepth(DiagnoseUtil.recognizeNodeDepth(operator));
                node.setOperator(operator);
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
        String appendSql = "query_sql like '%" + sql.replaceAll("'", "''") + "%';";
        return innerGetExecutionDetail(connection, appendSql, null);
    }

    @Override
    protected SqlExecDetail innerGetExecutionDetail(Connection connection, String appendSql, String traceId)
            throws SQLException {
        OBOracleInformationExtension informationExtension = new OBOracleInformationExtension();
        String version = informationExtension.getDBVersion(connection);
        StringBuilder detailSql = new StringBuilder();
        String viewName = "gv$sql_audit";
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            viewName = "gv$ob_sql_audit";
        }
        detailSql.append("select TRACE_ID, SQL_ID, QUERY_SQL, REQUEST_TIME, ELAPSED_TIME, QUEUE_TIME, EXECUTE_TIME, "
                + "TOTAL_WAIT_TIME_MICRO, IS_HIT_PLAN, PLAN_TYPE, AFFECTED_ROWS, RETURN_ROWS, RPC_COUNT, "
                + "SSSTORE_READ_ROW_COUNT from SYS.").append(viewName).append(" WHERE ");
        detailSql.append(appendSql);
        String querySql = detailSql.toString();
        Statement stmt = connection.createStatement();
        try (ResultSet resultSet = stmt.executeQuery(querySql)) {
            return DiagnoseUtil.toSQLExecDetail(resultSet);
        } catch (Exception e) {
            log.warn("Failed to get execution detail, traceId={}", traceId, e);
            throw OBException.executeFailed(ErrorCodes.ObGetExecuteDetailFailed,
                    String.format("Failed to get execution detail, traceId=%s, message=%s", traceId, e.getMessage()));
        }
    }

}
