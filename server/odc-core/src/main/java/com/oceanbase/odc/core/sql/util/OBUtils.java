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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OBUtils {

    public static final String INVALID_TRACE_ID = "__trace_id_not_exists__";

    /**
     * extract trace_id from `show trace` command result <br>
     * keyValue examples: <br>
     * - OB3.2.3- `trace_id:YB426451985D-0005D3191FDEEE9D-0-0` <br>
     * - OB3.2.3+ `xxx, enqueue_ts:123892389329, trace_id:YB426451985D-0005D3191FDEEE9D-0-0`
     *
     * @param keyValue from show trace result
     * @return traceId, null if extract failed
     */
    public static String extractTraceIdFromKeyValue(String keyValue) {
        PreConditions.notNull(keyValue, "keyValue");
        String[] split = keyValue.split("trace_id:");
        if (split.length < 2) {
            log.debug("Unexpected traceId format");
            return null;
        }
        String traceId = split[1];
        int indexOfSeparator = StringUtils.indexOf(traceId, ',');
        return StringUtils.INDEX_NOT_FOUND == indexOfSeparator ? traceId
                : StringUtils.substring(traceId, 0, indexOfSeparator);
    }

    /**
     * 通用方法，所有版本OB均适配
     */
    public static String getTraceId(@NonNull ConnectionSession session, @NonNull String querySql) {
        String schema = ConnectionSessionUtil.getCurrentSchema(session);
        Verify.notNull(schema, "CurrentSchema");
        SqlBuilder sqlBuilder = getBuilder(session.getConnectType());
        String version = ConnectionSessionUtil.getVersion(session);
        Verify.notNull(version, "ObVersion");
        sqlBuilder.append("SELECT TRACE_ID FROM ");
        sqlBuilder.append(buildSqlAudit(session.getConnectType(), version));

        sqlBuilder.append(" WHERE DB_NAME=")
                .value(schema)
                .append(" AND QUERY_SQL LIKE ")
                .value(querySql)
                .append(" ORDER BY REQUEST_TIME DESC");
        SyncJdbcExecutor jdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        return jdbcExecutor.query(sqlBuilder.toString(), rs -> {
            Verify.verify(rs.next(), "ResultSet is empty");
            return rs.getString("TRACE_ID");
        });
    }

    /**
     * 查询上一条SQL的执行详情，包括traceId以及实际执行时间。存在性能问题
     */
    public static SqlExecDetail getLastExecuteDetails(@NonNull Statement statement,
            @NonNull ConnectType connectType,
            @NonNull String version,
            @NonNull String connectionId) throws SQLException {
        if (connectType.isCloud()) {
            if (VersionUtils.isLessThanOrEqualsTo(version, "2.2.77")) {
                String traceId = OBUtils.getLastTraceIdBefore400(statement);
                return OBUtils.queryExecuteDetailByTraceId(statement, traceId, connectType, version);
            } else {
                return OBUtils.getLastExecuteDetailsAfter2277(statement, connectType, version);
            }
        }
        SqlBuilder sqlBuilder = getBuilder(connectType);
        sqlBuilder.append("SELECT TRACE_ID, ELAPSED_TIME, EXECUTE_TIME FROM ");
        sqlBuilder.append(buildSqlAudit(connectType, version));
        sqlBuilder.append(" WHERE SID=")
                .append(connectionId)
                .append(" ORDER BY REQUEST_TIME DESC;");
        try (ResultSet rs = statement.executeQuery(sqlBuilder.toString())) {
            return extractExecuteDetail(rs);
        } catch (Exception ex) {
            throw new UnexpectedException("Query sql execute details failed, reason=" + ex.getMessage());
        }
    }

    /**
     * {@code show trace} query 适用于 OceanBase 4.0 之前版本，基本不存在性能问题
     */
    public static SqlExecTime getLastExecuteDetailsBefore400(@NonNull Statement statement, @NonNull String version)
            throws SQLException {
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0.0")) {
            throw new UnsupportedOperationException("'show trace' does not support OceanBase " + version);
        }
        SqlExecTime execDetail = new SqlExecTime();
        boolean isExecuteStage = false;
        long waitTime = 0;
        long executeTime = 0;
        // trace_id 所在行，3.2.3 版本后移动至 process begin 阶段
        String traceField = VersionUtils.isGreaterThanOrEqualsTo(version, "3.2.3") ? "process begin" : "query begin";
        StringBuilder traceContent = new StringBuilder("\n");
        try (ResultSet resultSet = statement.executeQuery("show trace")) {
            while (resultSet.next()) {
                String title = resultSet.getString(1);
                String keyValue = resultSet.getString(2);
                String timeStr = resultSet.getString(3);
                appendTraceRow(traceContent, title, keyValue, timeStr);
                if (!NumberUtils.isDigits(timeStr)) {
                    continue;
                }
                long time = Long.parseLong(timeStr);
                if (isExecuteStage) {
                    executeTime += time;
                    if ("execution end".equalsIgnoreCase(title)) {
                        isExecuteStage = false;
                    }
                } else {
                    waitTime += time;
                    if (traceField.equalsIgnoreCase(title)) {
                        execDetail.setTraceId(extractTraceIdFromKeyValue(resultSet.getString(2)));
                    } else if ("execution begin".equalsIgnoreCase(title)) {
                        isExecuteStage = true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error occurred when show trace, trace content:{}", traceContent, e);
            throw e;
        }
        if (waitTime == 0 || executeTime == 0) {
            log.warn("Abnormal time-consuming calculation, trace content:{}", traceContent);
        }
        execDetail.setElapsedMicroseconds(waitTime + executeTime);
        execDetail.setExecuteMicroseconds(executeTime);
        return execDetail;
    }

    private static void appendTraceRow(StringBuilder out, String title, String keyValue, String timeStr) {
        out.append(title);
        out.append(" | ");
        out.append(keyValue);
        out.append(" | ");
        out.append(timeStr);
        out.append("\n");
    }

    /**
     * 除了 {@code show trace} 以外，还可以使用 {@code select last_trace_id() from dual} 查询 trace_id，不过这个方法不是很稳定：
     *
     * <pre>
     *     1.4.79 版本支持，返回示例：
     *     +-------------------------------------------------------------+
     *     | last_trace_id()                                             |
     *     +-------------------------------------------------------------+
     *     | 100.69.96.13:11967, TraceId: Y2EBF6445600D-0005DC82BEAAD474 |
     *     +-------------------------------------------------------------+
     *     2.x 版本不支持；
     *     3.x 和 4.0 版本支持，返回示例：
     *     +------------------------------------+
     *     | LAST_TRACE_ID()                    |
     *     +------------------------------------+
     *     | Y2F036445600D-0005E992E0E208F1-0-0 |
     *     +------------------------------------+
     * </pre>
     *
     * 函数存在稳定性问题，类似 {@code select xx from gv$sql_audit where trace_id=(select last_trace_id()) }
     * 的查询可能得到空结果。跟晓楚确认，这种方法不建议使用。
     */
    public static String getLastTraceIdAfter2277(@NonNull Statement statement) throws SQLException {
        String sql = "select last_trace_id() from dual";
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : INVALID_TRACE_ID;
        }
    }

    /**
     * 适用于 OceanBase 4.0 之前的版本
     */
    public static String getLastTraceIdBefore400(@NonNull Statement statement) throws SQLException {
        String sql = "show trace where keyvalue like '%trace_id:%'";
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? OBUtils.extractTraceIdFromKeyValue(rs.getString(2)) : INVALID_TRACE_ID;
        }
    }

    /**
     * 通用方法，所有版本OB均适配
     */
    public static SqlExecDetail queryExecuteDetailByTraceId(@NonNull Statement statement, @NonNull String traceId,
            @NonNull ConnectType connectType, @NonNull String version) throws SQLException {
        SqlBuilder sqlBuilder = getBuilder(connectType);
        sqlBuilder.append("SELECT TRACE_ID, ELAPSED_TIME, EXECUTE_TIME FROM ")
                .append(buildSqlAudit(connectType, version))
                .append(" WHERE TRACE_ID=")
                .value(traceId);

        try (ResultSet resultSet = statement.executeQuery(sqlBuilder.toString())) {
            return extractExecuteDetail(resultSet);
        }
    }

    /**
     * 不使用 connection_id 标识，而使用 last_trace_id() 函数，因此不适用于 OceanBase 3.x 之前的版本。 由于 last_trace_id()
     * 函数存在稳定性问题，类似 {@code select xx from gv$sql_audit where trace_id=(select last_trace_id()) }
     * 的查询可能得到空结果，所以首先执行一次查询，将 trace_id 设置为临时变量
     */
    public static SqlExecDetail getLastExecuteDetailsAfter2277(@NonNull Statement statement,
            @NonNull ConnectType connectType,
            @NonNull String version) throws SQLException {
        SqlBuilder sqlBuilder = getBuilder(connectType)
                .append("SELECT LAST_TRACE_ID() INTO @LAST_TRACE_ID;")
                .append("SELECT TRACE_ID, ELAPSED_TIME, EXECUTE_TIME FROM ")
                .append(buildSqlAudit(connectType, version))
                .append(" WHERE TRACE_ID=@LAST_TRACE_ID;");

        statement.execute(sqlBuilder.toString());
        if (statement.getMoreResults()) {
            try (ResultSet resultSet = statement.getResultSet()) {
                return extractExecuteDetail(resultSet);
            }
        } else {
            throw new UnexpectedException("Query sql execute details failed");
        }
    }

    /**
     * ob获取数字版本号 keyValue examples: <br>
     * - OceanBase 3.2.3.0 (r1-1b84d1f5d66d859e33f3a5d4bdd5bb66c00ea97e) (Built Jun 7 2022 12:36:17)
     *
     * @param obVersionComment
     * @return
     */
    public static String parseObVersionComment(String obVersionComment) {
        Validate.notBlank(obVersionComment);
        String[] obVersion = obVersionComment.split("\\s+");
        if (obVersion == null) {
            String errMsg = "version comment is empty, " + obVersionComment;
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed, new Object[] {errMsg}, errMsg);
        }
        if (obVersion.length < 4) {
            String errMsg = "failed to get version comment, " + obVersionComment;
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed, new Object[] {errMsg}, errMsg);
        }
        return obVersion[1];
    }

    public static String getObVersion(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("show variables like 'version_comment'")) {
                if (resultSet.next()) {
                    return OBUtils.parseObVersionComment(resultSet.getString("value"));
                }
                throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                        new Object[] {"Result set is empty"}, "Result set is empty");
            }
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.QueryDBVersionFailed,
                    new Object[] {e.getMessage()}, e.getMessage());
        }
    }

    private static String buildSqlAudit(ConnectType connectType, String version) {
        StringBuilder str = new StringBuilder();
        if (connectType.getDialectType().isOracle()) {
            str.append("SYS.");
        } else {
            str.append("oceanbase.");
        }
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            str.append("GV$OB_SQL_AUDIT");
        } else {
            str.append("GV$SQL_AUDIT");
        }
        return str.toString();
    }

    private static SqlExecDetail extractExecuteDetail(ResultSet resultSet) throws SQLException {
        Verify.verify(resultSet.next(), "ResultSet is empty");
        String traceId = resultSet.getString("TRACE_ID");
        long elapsedTime = resultSet.getLong("ELAPSED_TIME");
        long executeTime = resultSet.getLong("EXECUTE_TIME");
        SqlExecDetail execDetail = new SqlExecDetail();
        execDetail.setTraceId(traceId);
        execDetail.setTotalTime(elapsedTime);
        execDetail.setExecTime(executeTime);
        return execDetail;
    }

    private static SqlBuilder getBuilder(ConnectType connectType) {
        DialectType dialectType = connectType.getDialectType();
        if (dialectType.isMysql()) {
            return new MySQLSqlBuilder();
        } else if (dialectType.isOracle()) {
            return new OracleSqlBuilder();
        } else if (dialectType.isDoris()) {
            return new MySQLSqlBuilder();
        }
        throw new IllegalArgumentException("Unsupported dialect type, " + dialectType);
    }

    public static String queryDBMSOutput(@NonNull Connection connection, Integer maxLines) throws SQLException {
        /**
         * 这里为什么是{@link Integer.MAX_VALUE}
         *
         * 数据库在定义 dbms 输出的时候采用的自定义 type 为： <code>
         *     TYPE DBMSOUTPUT_LINESARRAY IS VARRAY(2147483647) OF VARCHAR2(32767);
         * </code> 可以看到这个 varray 的最大容量就是 2147483647，这个值就是 {@link Integer.MAX_VALUE}，因此默认定为该值
         */
        int maxLineNum = Integer.MAX_VALUE;
        if (maxLines != null && maxLines >= 0) {
            maxLineNum = maxLines;
        }
        String querySql = "DECLARE\n"
                + "  STATUS        INTEGER;\n"
                + "  L_NUM         INTEGER := ?;\n"
                + "  L_LINE        VARCHAR2(32767) := '';\n"
                + "  L_LINES_ARRAY CLOB;\n"
                + "BEGIN\n"
                + "  IF L_NUM > 0 THEN\n"
                + "    FOR i IN 1 .. L_NUM LOOP\n"
                + "      DBMS_OUTPUT.GET_LINE(L_LINE, STATUS);\n"
                + "      IF STATUS = 1 THEN"
                + "        EXIT;"
                + "      ELSIF i = 1 THEN\n"
                + "        L_LINES_ARRAY := L_LINE;\n"
                + "      ELSE\n"
                + "        L_LINES_ARRAY := L_LINES_ARRAY || '\n' || L_LINE;\n"
                + "      END IF;\n"
                + "    END LOOP;\n"
                + "  END IF;\n"
                + "  ? := L_LINES_ARRAY;\n"
                + "END;";
        try (CallableStatement callableStatement = connection.prepareCall(querySql)) {
            callableStatement.setInt(1, maxLineNum);
            callableStatement.registerOutParameter(2, Types.CLOB);
            callableStatement.executeQuery();
            return callableStatement.getString(2);
        }
    }

    public static String queryOBProxySessId(@NonNull Statement statement, @NonNull DialectType dialectType,
            @NonNull String connectionId) throws SQLException {
        String proxySessId = null;
        String sql = "select proxy_sessid from "
                + (dialectType.isMysql() ? "oceanbase" : "sys")
                + ".v$ob_processlist where id = "
                + connectionId;
        try (ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) {
                proxySessId = rs.getString(1);
            }
        }
        return proxySessId;
    }

    public static List<String> querySessionIdsByProxySessId(@NonNull Statement statement,
            @NonNull String proxySessId, ConnectType connectType) throws SQLException {
        DialectType dialectType = connectType.getDialectType();
        SqlBuilder sqlBuilder = getBuilder(connectType)
                .append("select id from ")
                .append(dialectType.isMysql() ? "oceanbase" : "sys")
                .append(".v$ob_processlist where proxy_sessid = ")
                .append(proxySessId);
        List<String> ids = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery(sqlBuilder.toString())) {
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        }
        return ids;
    }

    /**
     * OceanBase only supports ASH views in versions higher than 4.0. Therefore, this method is not
     * applicable to earlier versions, please use sql_audit instead.
     */
    public static String queryTraceIdFromASH(@NonNull Statement statement,
            @NonNull List<String> sessionIds, ConnectType connectType) throws SQLException {
        DialectType dialectType = connectType.getDialectType();
        SqlBuilder sqlBuilder = getBuilder(connectType)
                .append("select trace_id from ")
                .append(dialectType.isMysql() ? "oceanbase" : "sys")
                .append(".v$active_session_history where session_id in (")
                .append(String.join(",", sessionIds))
                .append(")")
                .append(dialectType.isMysql() ? " limit 1" : " and rownum=1");
        try (ResultSet rs = statement.executeQuery(sqlBuilder.toString())) {
            if (!rs.next()) {
                throw new SQLException("No result found in ASH.");
            }
            return rs.getString(1);
        }
    }

    /**
     * OceanBase only supports ASH views in versions higher than 4.0. Therefore, this method is not
     * applicable to earlier versions, please use sql_audit instead.
     */
    public static String queryPlanIdByTraceId(@NonNull Statement statement, String traceId, ConnectType connectType)
            throws SQLException {
        DialectType dialectType = connectType.getDialectType();
        SqlBuilder sqlBuilder = getBuilder(connectType)
                .append("select plan_id from ")
                .append(dialectType.isMysql() ? "oceanbase" : "sys")
                .append(".v$active_session_history where trace_id=")
                .value(traceId);
        try (ResultSet rs = statement.executeQuery(sqlBuilder.toString())) {
            if (!rs.next()) {
                throw new SQLException("No result found in ASH.");
            }
            return rs.getString(1);
        }
    }

}
