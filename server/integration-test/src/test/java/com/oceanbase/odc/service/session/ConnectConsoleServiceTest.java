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
package com.oceanbase.odc.service.session;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.ExpiredSessionException;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.FutureResult;
import com.oceanbase.odc.core.sql.execute.GeneralAsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.GeneralSyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.cache.BinaryDataManager;
import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.model.CommonVirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.table.CrossLinkedVirtualTable;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.core.sql.execute.model.JdbcResultSetMetaData;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.session.DefaultDBSessionManage;
import com.oceanbase.odc.service.dml.ValueEncodeType;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.AsyncExecuteResultResp;
import com.oceanbase.odc.service.session.model.BinaryContent;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * Test cases for {@link ConnectConsoleService}
 *
 * @author yh263208
 * @date 2021-11-20 23:31
 * @since ODC_release_3.2.2
 */
public class ConnectConsoleServiceTest extends ServiceTestEnv {

    private final String sessionId = "10000";
    @MockBean
    private ConnectSessionService sessionService;
    @Autowired
    private ConnectConsoleService consoleService;
    @Autowired
    private DefaultDBSessionManage defaultConnectSessionManage;

    @Test
    public void editProcedureForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestProcedure = "DROP PROCEDURE IF EXISTS ODC_TEST_PROCEDURE;";
        syncJdbcExecutor.execute(dropTestProcedure);
        String createTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num INT, OUT square INT)\n"
                + "BEGIN\n"
                + "    SET square = num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTestProcedure);
        String dropODCTempProcedure =
                "DROP PROCEDURE IF EXISTS " + ConnectConsoleService.ODC_TEMP_PROCEDURE_PREFIX + "ODC_TEST_PROCEDURE;";
        syncJdbcExecutor.execute(dropODCTempProcedure);
        String editTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num1 INT, OUT square1 INT)\n"
                + "BEGIN\n"
                + "    SET square1 = num1 * num1;\n"
                + "END";
        SqlAsyncExecuteResp sqlAsyncExecuteResp = getSqlAsyncExecuteResp(
                testConnectionSession, editTestProcedure, "ODC_TEST_PROCEDURE", DBObjectType.PROCEDURE);
        List<SqlExecuteResult> sqlExecuteResults = new ArrayList<>();
        AsyncExecuteResultResp moreResults = null;
        do {
            moreResults =
                    consoleService.getMoreResults(testConnectionSession.getId(), sqlAsyncExecuteResp.getRequestId());
            sqlExecuteResults.addAll(moreResults.getResults());
        } while (!moreResults.isFinished());
        Assert.assertTrue(sqlExecuteResults.size() == 4);
        for (SqlExecuteResult sqlExecuteResult : sqlExecuteResults) {
            Assert.assertTrue(sqlExecuteResult.getStatus() == SqlExecuteStatus.SUCCESS);
        }
        syncJdbcExecutor.execute(dropTestProcedure);
        syncJdbcExecutor.execute(dropODCTempProcedure);
    }

    @Test
    public void editProcedureForOBMysql_odcTempProcedureHaveExisted_failResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestProcedure = "DROP PROCEDURE IF EXISTS ODC_TEST_PROCEDURE;";
        syncJdbcExecutor.execute(dropTestProcedure);
        String createTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num INT, OUT square INT)\n"
                + "BEGIN\n"
                + "    SET square = num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTestProcedure);
        String dropODCTempProcedure =
                "DROP PROCEDURE IF EXISTS " + ConnectConsoleService.ODC_TEMP_PROCEDURE_PREFIX + "ODC_TEST_PROCEDURE;";
        syncJdbcExecutor.execute(dropODCTempProcedure);
        String createTempProcedure = "CREATE PROCEDURE " + ConnectConsoleService.ODC_TEMP_PROCEDURE_PREFIX
                + "ODC_TEST_PROCEDURE(IN num INT, OUT square INT)\n"
                + "BEGIN\n"
                + "    SET square = num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTempProcedure);
        String editTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num1 INT, OUT square1 INT)\n"
                + "BEGIN\n"
                + "    SET square1 = num1 * num1;\n"
                + "END";
        SqlAsyncExecuteResp sqlAsyncExecuteResp = getSqlAsyncExecuteResp(
                testConnectionSession, editTestProcedure, "ODC_TEST_PROCEDURE", DBObjectType.PROCEDURE);
        List<SqlExecuteResult> sqlExecuteResults = new ArrayList<>();
        AsyncExecuteResultResp moreResults = null;
        do {
            moreResults =
                    consoleService.getMoreResults(testConnectionSession.getId(), sqlAsyncExecuteResp.getRequestId());
            sqlExecuteResults.addAll(moreResults.getResults());
        } while (!moreResults.isFinished());
        Assert.assertTrue(sqlExecuteResults.size() == 4);
        for (int i = 0; i < sqlExecuteResults.size(); i++) {
            if (i == 0) {
                Assert.assertTrue(sqlExecuteResults.get(i).getStatus() == SqlExecuteStatus.FAILED);
            } else {
                Assert.assertTrue(sqlExecuteResults.get(i).getStatus() == SqlExecuteStatus.CANCELED);
            }
        }
        syncJdbcExecutor.execute(dropTestProcedure);
        syncJdbcExecutor.execute(dropODCTempProcedure);
    }

    @Test
    public void editFunctionForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestFunction = "DROP FUNCTION IF EXISTS ODC_TEST_FUNCTION;";
        syncJdbcExecutor.execute(dropTestFunction);
        String createTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num * num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTestFunction);
        String dropODCTempFunction =
                "DROP FUNCTION IF EXISTS " + ConnectConsoleService.ODC_TEMP_FUNCTION_PREFIX + "ODC_TEST_FUNCTION;";
        syncJdbcExecutor.execute(dropODCTempFunction);
        String editTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num1 INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num1 * num1 * num1;\n"
                + "END";
        SqlAsyncExecuteResp sqlAsyncExecuteResp = getSqlAsyncExecuteResp(
                testConnectionSession, editTestFunction, "ODC_TEST_FUNCTION", DBObjectType.FUNCTION);
        List<SqlExecuteResult> sqlExecuteResults = new ArrayList<>();
        AsyncExecuteResultResp moreResults = null;
        do {
            moreResults =
                    consoleService.getMoreResults(testConnectionSession.getId(), sqlAsyncExecuteResp.getRequestId());
            sqlExecuteResults.addAll(moreResults.getResults());
        } while (!moreResults.isFinished());
        for (SqlExecuteResult sqlExecuteResult : sqlExecuteResults) {
            Assert.assertTrue(sqlExecuteResult.getStatus() == SqlExecuteStatus.SUCCESS);
        }
        syncJdbcExecutor.execute(dropTestFunction);
        syncJdbcExecutor.execute(dropODCTempFunction);
    }

    @Test
    public void editFunctionForOBMysql_odcTempFunctionHaveExisted_failResultResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestFunction = "DROP FUNCTION IF EXISTS ODC_TEST_FUNCTION;";
        syncJdbcExecutor.execute(dropTestFunction);
        String createTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num * num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTestFunction);
        String dropODCTempFunction =
                "DROP FUNCTION IF EXISTS " + ConnectConsoleService.ODC_TEMP_FUNCTION_PREFIX + "ODC_TEST_FUNCTION;";
        syncJdbcExecutor.execute(dropODCTempFunction);
        String createODCTempFunction =
                "CREATE FUNCTION " + ConnectConsoleService.ODC_TEMP_FUNCTION_PREFIX + "ODC_TEST_FUNCTION(num INT) \n"
                        + "RETURNS INT\n"
                        + "BEGIN\n"
                        + "    RETURN num * num * num;\n"
                        + "END";
        syncJdbcExecutor.execute(createODCTempFunction);
        String editTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num1 INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num1 * num1 * num1;\n"
                + "END";
        SqlAsyncExecuteResp sqlAsyncExecuteResp = getSqlAsyncExecuteResp(
                testConnectionSession, editTestFunction, "ODC_TEST_FUNCTION", DBObjectType.FUNCTION);
        List<SqlExecuteResult> sqlExecuteResults = new ArrayList<>();
        AsyncExecuteResultResp moreResults = null;
        do {
            moreResults =
                    consoleService.getMoreResults(testConnectionSession.getId(), sqlAsyncExecuteResp.getRequestId());
            sqlExecuteResults.addAll(moreResults.getResults());
        } while (!moreResults.isFinished());
        Assert.assertTrue(sqlExecuteResults.size() == 4);
        for (int i = 0; i < sqlExecuteResults.size(); i++) {
            if (i == 0) {
                Assert.assertTrue(sqlExecuteResults.get(i).getStatus() == SqlExecuteStatus.FAILED);
            } else {
                Assert.assertTrue(sqlExecuteResults.get(i).getStatus() == SqlExecuteStatus.CANCELED);
            }
        }
        syncJdbcExecutor.execute(dropTestFunction);
        syncJdbcExecutor.execute(dropODCTempFunction);
    }

    @Test
    public void editTriggerForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestTrigger = "DROP TRIGGER IF EXISTS ODC_TEST_TRIGGER;";
        syncJdbcExecutor.execute(dropTestTrigger);
        String dropODCTempTestTrigger =
                "DROP TRIGGER IF EXISTS " + ConnectConsoleService.ODC_TEMP_TRIGGER_PREFIX + "ODC_TEST_TRIGGER;";
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        String dropTestTable = "DROP TABLE IF EXISTS ODC_TEST_TRIGGER_TABLE;";
        syncJdbcExecutor.execute(dropTestTable);
        String createTestTable = "CREATE TABLE ODC_TEST_TRIGGER_TABLE (\n"
                + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                + "    value INT\n"
                + ");";
        syncJdbcExecutor.execute(createTestTable);
        String createTestTrigger = "CREATE TRIGGER ODC_TEST_TRIGGER\n"
                + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                + "FOR EACH ROW\n"
                + "BEGIN\n"
                + "    SET NEW.value = NEW.value * 1;\n"
                + "END";
        syncJdbcExecutor.execute(createTestTrigger);
        String editTestTrigger = "CREATE TRIGGER ODC_TEST_TRIGGER\n"
                + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                + "FOR EACH ROW\n"
                + "BEGIN\n"
                + "    SET NEW.value = NEW.value * 2;\n"
                + "END";
        SqlAsyncExecuteResp sqlAsyncExecuteResp = getSqlAsyncExecuteResp(
                testConnectionSession, editTestTrigger, "ODC_TEST_TRIGGER", DBObjectType.TRIGGER);
        List<SqlExecuteResult> sqlExecuteResults = new ArrayList<>();
        AsyncExecuteResultResp moreResults = null;
        do {
            moreResults =
                    consoleService.getMoreResults(testConnectionSession.getId(), sqlAsyncExecuteResp.getRequestId());
            sqlExecuteResults.addAll(moreResults.getResults());
        } while (!moreResults.isFinished());
        for (SqlExecuteResult sqlExecuteResult : sqlExecuteResults) {
            Assert.assertTrue(sqlExecuteResult.getStatus() == SqlExecuteStatus.SUCCESS);
        }
        syncJdbcExecutor.execute(dropTestTrigger);
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        syncJdbcExecutor.execute(dropTestTable);
    }

    @Test
    public void editTriggerForOBMysql_odcTempTriggerHaveExisted_failResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestTrigger =
                "DROP TRIGGER IF EXISTS " + ConnectConsoleService.ODC_TEMP_TRIGGER_PREFIX + "ODC_TEST_TRIGGER;";
        syncJdbcExecutor.execute(dropTestTrigger);
        String dropODCTempTestTrigger =
                "DROP TRIGGER IF EXISTS " + ConnectConsoleService.ODC_TEMP_TRIGGER_PREFIX + "ODC_TEST_TRIGGER;";
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        String dropTestTable = "DROP TABLE IF EXISTS ODC_TEST_TRIGGER_TABLE;";
        syncJdbcExecutor.execute(dropTestTable);
        String createTestTable = "CREATE TABLE ODC_TEST_TRIGGER_TABLE (\n"
                + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                + "    value INT\n"
                + ");";
        syncJdbcExecutor.execute(createTestTable);
        String createTestTrigger = "CREATE TRIGGER ODC_TEST_TRIGGER\n"
                + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                + "FOR EACH ROW\n"
                + "BEGIN\n"
                + "    SET NEW.value = NEW.value * 1;\n"
                + "END";
        syncJdbcExecutor.execute(createTestTrigger);
        String createODCTempTrigger =
                "CREATE TRIGGER " + ConnectConsoleService.ODC_TEMP_TRIGGER_PREFIX + "ODC_TEST_TRIGGER\n"
                        + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                        + "FOR EACH ROW\n"
                        + "BEGIN\n"
                        + "    SET NEW.value = NEW.value * 1;\n"
                        + "END";
        syncJdbcExecutor.execute(createODCTempTrigger);
        String editTestTrigger =
                "CREATE TRIGGER " + ConnectConsoleService.ODC_TEMP_TRIGGER_PREFIX + "ODC_TEST_TRIGGER\n"
                        + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                        + "FOR EACH ROW\n"
                        + "BEGIN\n"
                        + "    SET NEW.value = NEW.value * 2;\n"
                        + "END";
        SqlAsyncExecuteResp sqlAsyncExecuteResp = getSqlAsyncExecuteResp(
                testConnectionSession, editTestTrigger, "ODC_TEST_TRIGGER", DBObjectType.TRIGGER);
        List<SqlExecuteResult> sqlExecuteResults = new ArrayList<>();
        AsyncExecuteResultResp moreResults = null;
        do {
            moreResults =
                    consoleService.getMoreResults(testConnectionSession.getId(), sqlAsyncExecuteResp.getRequestId());
            sqlExecuteResults.addAll(moreResults.getResults());
        } while (!moreResults.isFinished());
        for (SqlExecuteResult sqlExecuteResult : sqlExecuteResults) {
            Assert.assertTrue(sqlExecuteResult.getStatus() == SqlExecuteStatus.SUCCESS);
        }
        syncJdbcExecutor.execute(dropTestTrigger);
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        syncJdbcExecutor.execute(dropTestTable);
    }

    @Test
    public void getAsyncResult_killSessionSql_successResult() throws Exception {
        String sql = "kill session /*";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        SqlAsyncExecuteResp resp = consoleService.streamExecute(sessionId, getSqlAsyncExecuteReq(sql));
        injectExecuteContext(sessionId, resp.getRequestId(), JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        List<SqlExecuteResult> resultList = consoleService.getMoreResults(sessionId, resp.getRequestId()).getResults();

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void killSession_directLink() {
        String sql = "kill session 12345";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        List<JdbcGeneralResult> jdbcGeneralResults = defaultConnectSessionManage.executeKillSession(
                sessionService.nullSafeGet(sessionId), Collections.singletonList(SqlTuple.newTuple(sql)), sql);
        Assert.assertFalse(jdbcGeneralResults.isEmpty());
    }

    @Test
    public void getAsyncResult_wrongKillSessionSql_failedResult() throws Exception {
        String sql = "kill session /*";
        JdbcGeneralResult failedResult = JdbcGeneralResult.failedResult(SqlTuple.newTuple(sql), new Exception("test"));
        injectAsyncJdbcExecutor(failedResult);
        SqlAsyncExecuteResp resp = consoleService.streamExecute(sessionId, getSqlAsyncExecuteReq(sql));
        injectExecuteContext(sessionId, resp.getRequestId(), failedResult);
        List<SqlExecuteResult> resultList = consoleService.getMoreResults(sessionId, resp.getRequestId()).getResults();

        Assert.assertFalse(resultList.isEmpty());
        Assert.assertSame(SqlExecuteStatus.FAILED, resultList.get(0).getStatus());
    }

    @Test
    public void getAsyncResult_delimiter_getResultSucceed() throws Exception {
        String sql = "delimiter $$";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        SqlAsyncExecuteResp resp = consoleService.streamExecute(sessionId, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getMoreResults(sessionId, resp.getRequestId()).getResults();

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void getAsyncResult_commonSQLForOracle_getResultSucceed() throws Exception {
        String sql = "select * from tableaas";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        SqlAsyncExecuteResp resp = consoleService.streamExecute(sessionId, getSqlAsyncExecuteReq(sql));
        injectExecuteContext(sessionId, resp.getRequestId(), JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        List<SqlExecuteResult> resultList = consoleService.getMoreResults(sessionId, resp.getRequestId()).getResults();

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void getAsyncResult_commonSQLForMysql_getSucceed() throws Exception {
        String sql = "select * from tableaas";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)),
                ConnectType.OB_MYSQL);
        SqlAsyncExecuteResp resp = consoleService.streamExecute(sessionId, getSqlAsyncExecuteReq(sql));
        injectExecuteContext(sessionId, resp.getRequestId(), JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        List<SqlExecuteResult> resultList = consoleService.getMoreResults(sessionId, resp.getRequestId()).getResults();

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void generateResult_editableResultSet_isEditable() throws Exception {
        String sql = "select * from table_test";
        JdbcGeneralResult executeResult = getJdbcGeneralResultWithQueryData(sql);
        injectAsyncJdbcExecutor(executeResult, ConnectType.OB_MYSQL);
        SqlAsyncExecuteResp resp = consoleService.streamExecute(sessionId, getSqlAsyncExecuteReq(sql));
        injectExecuteContext(sessionId, resp.getRequestId(), executeResult);
        List<SqlExecuteResult> resultList = consoleService.getMoreResults(sessionId, resp.getRequestId()).getResults();

        Assert.assertTrue(resultList.get(0).getResultSetMetaData().isEditable());
    }

    @Test
    public void getBinaryContent_skipSeveralBytes_readSucceed() throws IOException {
        ConnectionSession session = new TestConnectionSession(
                sessionId, new ByteArrayInputStream("abcd".getBytes()));
        Mockito.when(sessionService.nullSafeGet(sessionId)).thenReturn(session);
        CrossLinkedVirtualTable table = new CrossLinkedVirtualTable("tableId");
        long rowId = 1;
        int colId = 1;
        BinaryContentMetaData metaData = new BinaryContentMetaData("filePath", 0, 0);
        VirtualElement elt = new CommonVirtualElement("tableId", rowId, colId, "test_type", "test_name", metaData);
        table.put(elt);
        ConnectionSessionUtil.setQueryCache(session, table);
        BinaryContent actual = consoleService.getBinaryContent(sessionId, "tableId",
                rowId, colId, 2L, 1, ValueEncodeType.TXT);
        BinaryContent expect = new BinaryContent("c".getBytes(), 4, ValueEncodeType.TXT);
        Assert.assertEquals(expect, actual);
    }

    private void injectAsyncJdbcExecutor(JdbcGeneralResult result) {
        injectAsyncJdbcExecutor(result, ConnectType.OB_ORACLE);
    }

    private void injectAsyncJdbcExecutor(JdbcGeneralResult result, ConnectType connectType) {
        GeneralAsyncJdbcExecutor asyncJdbcExecutor = Mockito.mock(GeneralAsyncJdbcExecutor.class);
        Mockito.when(asyncJdbcExecutor.execute(Mockito.any(OdcStatementCallBack.class)))
                .thenReturn(FutureResult.successResultList(result));
        GeneralSyncJdbcExecutor syncJdbcExecutor = Mockito.mock(GeneralSyncJdbcExecutor.class);
        Mockito.when(syncJdbcExecutor.execute(Mockito.any(OdcStatementCallBack.class)))
                .thenReturn(Collections.singletonList(result));
        ConnectionSession session = new TestConnectionSession(sessionId, connectType,
                buildTestConnection(connectType), asyncJdbcExecutor, syncJdbcExecutor);
        Mockito.when(sessionService.nullSafeGet(sessionId, true)).thenReturn(session);
        Mockito.when(sessionService.nullSafeGet(sessionId)).thenReturn(session);
    }

    private SqlAsyncExecuteReq getSqlAsyncExecuteReq(String sql) {
        SqlAsyncExecuteReq req = new SqlAsyncExecuteReq();
        req.setSql(sql);
        req.setQueryLimit(1000);
        req.setAutoCommit(true);
        req.setShowTableColumnInfo(true);
        return req;
    }

    private ConnectionConfig buildTestConnection(ConnectType connectType) {
        ConnectionConfig connection = TestConnectionUtil.getTestConnectionConfig(connectType);
        long connectionId = 1L;
        connection.setId(connectionId);
        return connection;
    }

    private void injectExecuteContext(String sessionId, String requestId, JdbcGeneralResult result) {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId);
        AsyncExecuteContext context =
                (AsyncExecuteContext) ConnectionSessionUtil.getExecuteContext(connectionSession, requestId);
        context.addSqlExecutionResults(Collections.singletonList(result));
    }

    private JdbcGeneralResult getJdbcGeneralResultWithQueryData(String sql) throws SQLException {
        JdbcGeneralResult executeResult = JdbcGeneralResult.successResult(SqlTuple.newTuple(sql));

        ResultSetMetaData resultSetMetaData = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(resultSetMetaData.getCatalogName(Mockito.any(int.class))).thenReturn("schema_test");
        Mockito.when(resultSetMetaData.getTableName(Mockito.any(int.class))).thenReturn("table_test");
        Mockito.when(resultSetMetaData.getColumnCount()).thenReturn(1);

        JdbcResultSetMetaData jdbcResultSetMetaData = new JdbcResultSetMetaData(resultSetMetaData);
        JdbcQueryResult jdbcQueryResult = Mockito.mock(JdbcQueryResult.class);
        Mockito.when(jdbcQueryResult.getMetaData()).thenReturn(jdbcResultSetMetaData);

        executeResult.setQueryResult(jdbcQueryResult);
        return executeResult;
    }

    private SqlAsyncExecuteResp getSqlAsyncExecuteResp(ConnectionSession testConnectionSession,
            String editPLSql, String plName, DBObjectType plType) throws Exception {
        SqlAsyncExecuteReq sqlAsyncExecuteReq = getEditPLAsyncExecuteReq(editPLSql, plName, plType);
        Mockito.when(sessionService.nullSafeGet(testConnectionSession.getId(), true)).thenReturn(testConnectionSession);
        Mockito.when(sessionService.nullSafeGet(testConnectionSession.getId())).thenReturn(testConnectionSession);
        SqlAsyncExecuteResp sqlAsyncExecuteResp =
                consoleService.streamExecute(testConnectionSession.getId(), sqlAsyncExecuteReq);
        return sqlAsyncExecuteResp;
    }

    private SqlAsyncExecuteReq getEditPLAsyncExecuteReq(String editTestProcedure, String plName, DBObjectType plType) {
        SqlAsyncExecuteReq editProcedureReq = new SqlAsyncExecuteReq();
        editProcedureReq.setSql(editTestProcedure);
        editProcedureReq.setSplit(false);
        editProcedureReq.setPlName(plName);
        editProcedureReq.setPlType(plType);
        return editProcedureReq;
    }
}

class TestBinaryDataManager implements BinaryDataManager {

    private final InputStream input;

    public TestBinaryDataManager() {
        this.input = null;
    }

    public TestBinaryDataManager(InputStream input) {
        this.input = input;
    }

    @Override
    public BinaryContentMetaData write(@NonNull InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public InputStream read(@NonNull BinaryContentMetaData metaData) throws IOException {
        if (this.input != null) {
            return this.input;
        }
        throw new UnsupportedOperationException("read");
    }

    @Override
    public void close() throws Exception {

    }
}


class TestConnectionSession implements ConnectionSession {
    private final GeneralAsyncJdbcExecutor asyncJdbcExecutor;
    private final GeneralSyncJdbcExecutor syncJdbcExecutor;
    private final String id;
    private final Map<Object, Object> map = new HashMap<>();
    private final ConnectType connectType;

    public TestConnectionSession(String id, ConnectType connectType, ConnectionConfig connectionConfig,
            GeneralAsyncJdbcExecutor asyncJdbcExecutor, GeneralSyncJdbcExecutor syncJdbcExecutor) {
        this.asyncJdbcExecutor = asyncJdbcExecutor;
        this.syncJdbcExecutor = syncJdbcExecutor;
        this.id = id;
        ConnectionSessionUtil.setConnectionConfig(this, connectionConfig);
        ConnectionSessionUtil.setBinaryDataManager(this, new TestBinaryDataManager());
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true, true);
        ConnectionSessionUtil.setSqlCommentProcessor(this, processor);
        this.setAttribute(ConnectionSessionConstants.OB_VERSION, "3.2.40");
        this.map.putIfAbsent(ConnectionSessionConstants.SESSION_TIME_ZONE, "Asia/Shanghai");
        this.map.putIfAbsent(ConnectionSessionConstants.NLS_DATE_FORMAT_NAME, "DD-MON-RR");
        this.map.putIfAbsent(ConnectionSessionConstants.NLS_TIMESTAMP_FORMAT_NAME, "DD-MON-RR");
        this.map.putIfAbsent(ConnectionSessionConstants.NLS_TIMESTAMP_TZ_FORMAT_NAME, "DD-MON-RR");
        this.connectType = connectType;
    }

    public TestConnectionSession(String id, InputStream inputStream) {
        this(id, inputStream, ConnectType.OB_MYSQL);
    }

    public TestConnectionSession(String id, InputStream inputStream, ConnectType connectType) {
        this.asyncJdbcExecutor = null;
        this.syncJdbcExecutor = null;
        this.id = id;
        ConnectionSessionUtil.setBinaryDataManager(this, new TestBinaryDataManager(inputStream));
        SqlCommentProcessor processor =
                new SqlCommentProcessor(connectType.getDialectType(), true, true);
        ConnectionSessionUtil.setSqlCommentProcessor(this, processor);
        this.map.putIfAbsent(ConnectionSessionConstants.SESSION_TIME_ZONE, "Asia/Shanghai");
        this.map.putIfAbsent(ConnectionSessionConstants.NLS_DATE_FORMAT_NAME, "DD-MON-RR");
        this.map.putIfAbsent(ConnectionSessionConstants.NLS_TIMESTAMP_FORMAT_NAME, "DD-MON-RR");
        this.map.putIfAbsent(ConnectionSessionConstants.NLS_TIMESTAMP_TZ_FORMAT_NAME, "DD-MON-RR");
        this.connectType = connectType;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void register(@NonNull String name,
            @NonNull DataSourceFactory dataSourceFactory) {}

    @Override
    public ConnectType getConnectType() {
        return this.connectType;
    }

    @Override
    public DialectType getDialectType() {
        return this.connectType.getDialectType();
    }

    @Override
    public boolean getDefaultAutoCommit() {
        return false;
    }

    @Override
    public Date getStartTime() {
        return null;
    }

    @Override
    public Date getLastAccessTime() {
        return null;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void expire() {

    }

    @Override
    public void touch() throws ExpiredSessionException {

    }

    @Override
    public long getTimeoutMillis() {
        return 0;
    }

    @Override
    public Collection<Object> getAttributeKeys() throws ExpiredSessionException {
        return map.keySet();
    }

    @Override
    public Object getAttribute(Object key) throws ExpiredSessionException {
        return map.get(key);
    }

    @Override
    public void setAttribute(Object key, Object value) throws ExpiredSessionException {
        map.put(key, value);
    }

    @Override
    public Object removeAttribute(Object key) throws ExpiredSessionException {
        return map.put(key, null);
    }

    @Override
    public SyncJdbcExecutor getSyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        return this.syncJdbcExecutor;
    }

    @Override
    public AsyncJdbcExecutor getAsyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        return this.asyncJdbcExecutor;
    }

}
