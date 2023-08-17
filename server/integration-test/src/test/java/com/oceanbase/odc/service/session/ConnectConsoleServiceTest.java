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
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.ExpiredSessionException;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.model.TableIdentity;
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
import com.oceanbase.odc.service.session.model.BinaryContent;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * Test cases for {@link ConnectConsoleService}
 *
 * @author yh263208
 * @date 2021-11-20 23:31
 * @since ODC_release_3.2.2
 */
public class ConnectConsoleServiceTest extends ServiceTestEnv {

    private final String sessionid = "10000";
    @MockBean
    private ConnectSessionService sessionService;
    @Autowired
    private ConnectConsoleService consoleService;
    @Autowired
    private DefaultDBSessionManage defaultConnectSessionManage;

    @Autowired

    @Test
    public void getAsyncResult_killSessionSql_successResult() throws Exception {
        String sql = "kill session /*";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        SqlAsyncExecuteResp resp = consoleService.execute(sessionid, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getAsyncResult(sessionid, resp.getRequestId());

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void killSession_directLink() {
        String sql = "kill session 12345";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        List<JdbcGeneralResult> jdbcGeneralResults =
                defaultConnectSessionManage.executeKillSession(sessionService.nullSafeGet(sessionid),
                        Collections.singletonList(SqlTuple.newTuple(sql)), sql);
        Assert.assertFalse(jdbcGeneralResults.isEmpty());
    }

    @Test
    public void getAsyncResult_wrongKillSessionSql_failedResult() throws Exception {
        String sql = "kill session /*";
        JdbcGeneralResult failedResult = JdbcGeneralResult.failedResult(SqlTuple.newTuple(sql), new Exception("test"));
        injectAsyncJdbcExecutor(failedResult);
        SqlAsyncExecuteResp resp = consoleService.execute(sessionid, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getAsyncResult(sessionid, resp.getRequestId());

        Assert.assertFalse(resultList.isEmpty());
        Assert.assertSame(SqlExecuteStatus.FAILED, resultList.get(0).getStatus());
    }

    @Test
    public void getAsyncResult_delimiter_getResultSucceed() throws Exception {
        String sql = "delimiter $$";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        SqlAsyncExecuteResp resp = consoleService.execute(sessionid, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getAsyncResult(sessionid, resp.getRequestId());

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void getAsyncResult_commonSQLForOracle_getResultSucceed() throws Exception {
        String sql = "select * from tableaas";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)));
        SqlAsyncExecuteResp resp = consoleService.execute(sessionid, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getAsyncResult(sessionid, resp.getRequestId());

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void getAsyncResult_commonSQLForMysql_getSucceed() throws Exception {
        String sql = "select * from tableaas";
        injectAsyncJdbcExecutor(JdbcGeneralResult.successResult(SqlTuple.newTuple(sql)),
                ConnectType.OB_MYSQL);
        SqlAsyncExecuteResp resp = consoleService.execute(sessionid, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getAsyncResult(sessionid, resp.getRequestId());

        Assert.assertFalse(resultList.isEmpty());
    }

    @Test
    public void generateResult_editableResultSet_isEditable() throws Exception {
        String sql = "select * from table_test";
        JdbcGeneralResult executeResult = getJdbcGeneralResultWithQueryData(sql);
        injectAsyncJdbcExecutor(executeResult, ConnectType.OB_MYSQL);
        SqlAsyncExecuteResp resp = consoleService.execute(sessionid, getSqlAsyncExecuteReq(sql));
        List<SqlExecuteResult> resultList = consoleService.getAsyncResult(sessionid, resp.getRequestId());

        Assert.assertTrue(resultList.get(0).getResultSetMetaData().isEditable());
    }

    @Test
    public void getBinaryContent_skipSeveralBytes_readSucceed() throws IOException {
        ConnectionSession session = new TestConnectionSession("12", new ByteArrayInputStream("abcd".getBytes()));
        Mockito.when(sessionService.nullSafeGet(Mockito.anyString())).thenReturn(session);
        CrossLinkedVirtualTable table = new CrossLinkedVirtualTable("tableId");
        long rowId = 1;
        int colId = 1;
        BinaryContentMetaData metaData = new BinaryContentMetaData("filePath", 0, 0);
        VirtualElement elt = new CommonVirtualElement("tableId", rowId, colId, "test_type", "test_name", metaData);
        table.put(elt);
        ConnectionSessionUtil.setQueryCache(session, table);
        BinaryContent actual =
                consoleService.getBinaryContent("12", "tableId", rowId, colId, 2L, 1, ValueEncodeType.TXT);
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
        Mockito.when(sessionService.nullSafeGet(sessionid)).thenReturn(new TestConnectionSession(
                sessionid, connectType, buildTestConnection(connectType), asyncJdbcExecutor, syncJdbcExecutor));
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

        Cache<TableIdentity, List<DBTableColumn>> tableColumnsCache =
                Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(20, TimeUnit.MINUTES).build();
        List<DBTableColumn> columns = new ArrayList<>();
        columns.add(new DBTableColumn());
        tableColumnsCache.put(TableIdentity.of("schema_test", "table_test"), columns);
        ConnectionSessionUtil.setTableColumnCache(this, tableColumnsCache);
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
