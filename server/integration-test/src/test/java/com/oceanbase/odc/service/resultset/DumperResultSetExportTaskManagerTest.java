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
package com.oceanbase.odc.service.resultset;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.EncodingType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.ResultSetExportResult;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter.CSVFormat;

public class DumperResultSetExportTaskManagerTest extends ServiceTestEnv {
    private static ConnectionSession mysqlSession;
    private static ConnectionSession oracleSession;
    private static ConnectionConfig mysqlConnecitonConfig;
    private static ConnectionConfig oracleConnecitonConfig;
    private final String userId = "1";
    private final String taskId = "1";
    private final String fileName = "CUSTOM_SQL";
    private final String basePath = "data/RESULT_SET";
    private static String mysqlDefaultSchema;
    private static String oracleDefaultSchema;

    @Autowired
    private DumperResultSetExportTaskManager manager;

    @BeforeClass
    public static void classUp() {
        mysqlSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        mysqlConnecitonConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(mysqlSession);
        oracleSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        oracleConnecitonConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(oracleSession);
        String sql =
                "create table rs_export_test(`col1` varchar(64), `col2` varchar(64), `col3` varchar(64), `col4` varchar(64))";
        mysqlSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql);
        sql = "insert into rs_export_test(`col1`, `col2`, `col3`, `col4`) values('\"asd', '''', '\"\"', ',');"
                + "insert into rs_export_test(`col1`, `col2`, `col3`, `col4`) values('\"asd', '''', '\"\"', ',');"
                + "insert into rs_export_test(`col1`, `col2`, `col3`, `col4`) values('\"asd', '''', '\"\"', ',');";
        mysqlSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql);
        mysqlDefaultSchema = ConnectionSessionUtil.getCurrentSchema(mysqlSession);

        sql = "create table rs_export_test(col1 varchar(64), col2 varchar(64), col3 varchar(64), col4 varchar(64))";
        oracleSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql);
        sql = "insert into rs_export_test(col1, col2, col3, col4) values('\"asd', '''', '\"\"', ',');"
                + "insert into rs_export_test(col1, col2, col3, col4) values('\"asd', '''', '\"\"', ',');"
                + "insert into rs_export_test(col1, col2, col3, col4) values('\"asd', '''', '\"\"', ',');";
        oracleSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql);
        oracleDefaultSchema = ConnectionSessionUtil.getCurrentSchema(oracleSession);
    }

    @AfterClass
    public static void classDown() {
        String sql = "drop table rs_export_test";
        mysqlSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql);
        oracleSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql);
    }

    @Test
    public void startTask_ExportSQL_GenerateFileSuccess() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.SQL, EncodingType.UTF_8, mysqlSession);
        ResultSetExportTaskContext context = manager.start(mysqlConnecitonConfig, req, taskId);
        await().atMost(30, SECONDS).until(context::isDone);
        ResultSetExportResult result = context.get();
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        Assert.assertTrue(file.exists());
        FileUtils.forceDelete(file);
    }

    @Test
    public void startTask_ExportCSV_GenerateFileSuccess() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.CSV, EncodingType.GBK, mysqlSession);
        CSVFormat csvFormat = new CSVFormat();
        csvFormat.setColumnDelimiter('"');
        req.setCsvFormat(csvFormat);
        ResultSetExportTaskContext context = manager.start(mysqlConnecitonConfig, req, taskId);
        await().atMost(30, SECONDS).until(context::isDone);
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
        lineNumberReader.skip(Long.MAX_VALUE);
        Assert.assertEquals(4, lineNumberReader.getLineNumber());
        FileUtils.forceDelete(file);
    }

    @Test
    public void startTask_ExportCSV_ExportFailed() {
        ResultSetExportTaskParameter req = new ResultSetExportTaskParameter();
        req.setSql("select * from not_exist_table");
        req.setMaxRows(1000L);
        req.setFileFormat(DataTransferFormat.CSV);
        req.setFileEncoding(EncodingType.GBK);
        req.setDatabase(mysqlDefaultSchema);
        CSVFormat csvFormat = new CSVFormat();
        csvFormat.setColumnDelimiter('"');
        req.setCsvFormat(csvFormat);
        ResultSetExportTaskContext context = manager.start(mysqlConnecitonConfig, req, taskId);
        await().atMost(30, SECONDS).until(context::isDone);
        try {
            context.get();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("doesn't exist"));
        }
    }

    @Test
    public void startTask_ExportEXCEL_GenerateFileSuccess() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.EXCEL, EncodingType.GBK, mysqlSession);
        req.setCsvFormat(new CSVFormat());
        ResultSetExportTaskContext context = manager.start(mysqlConnecitonConfig, req, taskId);
        await().atMost(30, SECONDS).until(context::isDone);
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        Assert.assertTrue(file.exists());
        FileUtils.forceDelete(file);
    }

    @Test
    public void startTask_ExportSQL_GenerateFileSuccess_WithoutTableName() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.SQL, EncodingType.UTF_8, mysqlSession);
        req.setTableName(null);
        ResultSetExportTaskContext context = manager.start(mysqlConnecitonConfig, req, taskId);
        await().atMost(30, SECONDS).until(context::isDone);
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        Assert.assertTrue(file.exists());
        FileUtils.forceDelete(file);
    }

    @Test
    public void startTask_ExportCSV_Oracle_GenerateFileSuccess() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.CSV, EncodingType.GBK, oracleSession);
        CSVFormat csvFormat = new CSVFormat();
        csvFormat.setColumnDelimiter('"');
        req.setCsvFormat(csvFormat);
        ResultSetExportTaskContext context = manager.start(oracleConnecitonConfig, req, taskId);
        await().atMost(15, SECONDS).until(context::isDone);
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
        lineNumberReader.skip(Long.MAX_VALUE);
        Assert.assertEquals(4, lineNumberReader.getLineNumber());
        FileUtils.forceDelete(file);
    }

    @Test
    public void startTask_ExportCSV_WithMaxRowsLimit_MySQL() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.CSV, EncodingType.GBK, oracleSession);
        req.setMaxRows(1L);
        CSVFormat csvFormat = new CSVFormat();
        csvFormat.setColumnDelimiter('"');
        req.setCsvFormat(csvFormat);
        ResultSetExportTaskContext context = manager.start(oracleConnecitonConfig, req, taskId);
        await().atMost(15, SECONDS).until(context::isDone);
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
        lineNumberReader.skip(Long.MAX_VALUE);
        Assert.assertEquals(2, lineNumberReader.getLineNumber());
        FileUtils.forceDelete(file);
    }

    @Test
    public void startTask_ExportCSV_WithMaxRowsLimit_Oracle() throws Exception {
        ResultSetExportTaskParameter req =
                createResultSetExportTaskReq(DataTransferFormat.CSV, EncodingType.GBK, oracleSession);
        req.setMaxRows(1L);
        CSVFormat csvFormat = new CSVFormat();
        csvFormat.setColumnDelimiter('"');
        req.setCsvFormat(csvFormat);
        ResultSetExportTaskContext context = manager.start(oracleConnecitonConfig, req, taskId);
        await().atMost(15, SECONDS).until(context::isDone);
        File file = Paths.get(basePath, taskId, fileName + req.getFileFormat().getExtension()).toFile();
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
        lineNumberReader.skip(Long.MAX_VALUE);
        Assert.assertEquals(2, lineNumberReader.getLineNumber());
        FileUtils.forceDelete(file);
    }

    private ResultSetExportTaskParameter createResultSetExportTaskReq(DataTransferFormat format, EncodingType encoding,
            ConnectionSession session) {
        ResultSetExportTaskParameter req = new ResultSetExportTaskParameter();
        req.setSql("select * from rs_export_test");
        req.setTableName("whatever_table");
        req.setMaxRows(1000L);
        req.setFileFormat(format);
        req.setFileEncoding(encoding);
        req.setFileName(fileName);
        req.setDatabase(ConnectionSessionUtil.getCurrentSchema(session));
        return req;
    }

}
