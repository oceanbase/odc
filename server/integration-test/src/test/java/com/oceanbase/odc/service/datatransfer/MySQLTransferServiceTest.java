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

package com.oceanbase.odc.service.datatransfer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.AbstractOutputFile;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.DataFile;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.ExportOutput;
import com.oceanbase.odc.plugin.task.api.datatransfer.dumper.SchemaFile;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTaskContext;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.extern.slf4j.Slf4j;

/**
 * Due to the current single test on GitHub being executed by ob-farm, it is temporarily not
 * possible to obtain the lfs file. So this unit test will be temporarily skipped and manually
 * executed by the developer.
 */
@Slf4j
@Ignore("run it manually")
public class MySQLTransferServiceTest extends ServiceTestEnv {
    private static final String BUCKET = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    private static final String TEST_TABLE_NAME = "loader_dumper_test";
    private final Long connectionId = 1L;
    private ConnectionConfig connectionConfig;

    @Autowired
    private DataTransferService dataTransferService;
    @Autowired
    private LocalFileManager fileManager;
    @MockBean
    private ConnectionService connectionService;

    @Before
    public void setUp() throws Exception {
        FileUtils.forceDelete(fileManager.getWorkingDir(TaskType.IMPORT, null));
        connectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.from(DialectType.MYSQL));
        connectionConfig.setId(connectionId);
        setUpEnv();
        Mockito.when(connectionService.getForConnect(connectionId)).thenReturn(connectionConfig);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(connectionId)).thenReturn(connectionConfig);
    }

    /*
     * dump test
     */
    @Test
    public void create_dumpSchemaAndData_bothSchemaAndDataDumped() throws Exception {
        DataTransferTaskContext context =
                dataTransferService.create(BUCKET, getDumpConfig(connectionConfig.getDefaultSchema(), true, true));
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));

        ExportOutput exportOutput = new ExportOutput(getDumpFile());
        assertFileCountEquals(exportOutput, 2);
        assertObjectTypeIn(exportOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(exportOutput, new HashSet<>(Arrays.asList(DataFile.class, SchemaFile.class)));
    }

    @Test
    public void create_dumpSchema_onlySchemaDumped() throws Exception {
        DataTransferTaskContext context =
                dataTransferService.create(BUCKET, getDumpConfig(connectionConfig.getDefaultSchema(), false, true));
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));

        ExportOutput exportOutput = new ExportOutput(getDumpFile());
        assertFileCountEquals(exportOutput, 1);
        assertObjectTypeIn(exportOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(exportOutput, new HashSet<>(Collections.singletonList(SchemaFile.class)));
    }

    @Test
    public void create_dumpSchema_onlySchemaDumped_mergeSchemaFiles() throws Exception {
        DataTransferConfig config = getDumpConfig(connectionConfig.getDefaultSchema(), false, true);
        config.setMergeSchemaFiles(true);
        DataTransferTaskContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));

        File target = new File(fileManager
                .getWorkingDir(TaskType.EXPORT, DataTransferService.CLIENT_DIR_PREFIX + BUCKET).getAbsolutePath());
        File[] files = target.listFiles(f -> f.getName().endsWith("schema.sql"));
        Assert.assertEquals(1, files.length);
    }

    @Test
    public void create_dumpData_onlyDataDumped() throws Exception {
        DataTransferTaskContext context =
                dataTransferService.create(BUCKET, getDumpConfig(connectionConfig.getDefaultSchema(), true, false));
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));

        ExportOutput exportOutput = new ExportOutput(getDumpFile());
        assertFileCountEquals(exportOutput, 1);
        assertObjectTypeIn(exportOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(exportOutput, new HashSet<>(Collections.singletonList(DataFile.class)));
    }

    /*
     * load test
     */
    @Test
    public void create_loadSchemaAndData_schemaAndDataLoaded() throws Exception {
        File dumpFile = dumpSchemaAndDataForLoad(DialectType.MYSQL);
        assertTableNotExists();

        DataTransferTaskContext context =
                dataTransferService.create(BUCKET, getLoadConfig(false, connectionConfig.getDefaultSchema(),
                        Collections.singletonList(dumpFile.getAbsolutePath()), true, true));
        Assert.assertNotNull(context.get(600, TimeUnit.SECONDS));
        assertTableExists();
        assertTableCountEquals(2);
    }

    @Test
    public void create_loadSchema_schemaLoaded() throws Exception {
        File dumpFile = dumpSchemaAndDataForLoad(DialectType.MYSQL);
        assertTableNotExists();

        DataTransferTaskContext context = dataTransferService.create(BUCKET, getLoadConfig(false,
                connectionConfig.getDefaultSchema(), Collections.singletonList(dumpFile.getAbsolutePath()), false,
                true));
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));
        assertTableExists();
        assertTableCountEquals(0);
    }

    @Test
    public void create_loadExternalSql_dataLoaded() throws Exception {
        String sqlScript = "INSERT INTO " + TEST_TABLE_NAME + " VALUES ('3', 'Marry'),('4', 'Tom');";
        File target = copyFile(new ByteArrayInputStream(sqlScript.getBytes()), "sql");

        DataTransferTaskContext context = dataTransferService.create(BUCKET, getLoadConfig(true,
                connectionConfig.getDefaultSchema(), Collections.singletonList(target.getAbsolutePath()), true, false));
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));
        assertTableExists();
        assertTableCountEquals(4);
    }

    private void setUpEnv() throws Exception {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String create = "CREATE TABLE " + TEST_TABLE_NAME + "(COL1 varchar(64), COL2 varchar(64))";
            String drop = "DROP TABLE " + TEST_TABLE_NAME;
            String insert = "INSERT INTO " + TEST_TABLE_NAME + " VALUES ('1', 'rojer'),('2', 'David')";
            try (Statement statement = connection.createStatement()) {
                try {
                    statement.executeUpdate(drop);
                } catch (Exception e) {
                    log.warn("Failed to drop table, message={}", e.getMessage());
                }
                statement.executeUpdate(create);
                Assert.assertEquals(2, statement.executeUpdate(insert));
            }
        }
    }

    private File dumpSchemaAndDataForLoad(DialectType dialectType) throws Exception {
        DataTransferConfig config = getDumpConfig(connectionConfig.getDefaultSchema(), true, true);
        DataTransferTaskContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(60, TimeUnit.SECONDS));
        File dumpFile = getDumpFile();
        File returnVal = copyFile(new FileInputStream(dumpFile), "zip");
        FileUtils.forceDelete(dumpFile);
        clearEnv();
        return returnVal;
    }

    private void clearEnv() throws Exception {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String drop = "DROP TABLE " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(drop);
            }
        }
    }

    private File copyFile(InputStream inputStream, String extend) throws IOException {
        File target = fileManager.getWorkingDir(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extend;
        File dest = new File(target.getAbsoluteFile() + File.separator + fileName);
        FileOutputStream outputStream = new FileOutputStream(dest);
        IOUtils.copy(inputStream, outputStream);
        return dest;
    }

    private DataTransferConfig getDumpConfig(String schema, boolean data, boolean ddl) {
        DataTransferConfig config = new DataTransferConfig();
        config.setConnectionId(connectionId);
        config.setSchemaName(schema);
        config.setTransferType(DataTransferType.EXPORT);
        config.setDataTransferFormat(DataTransferFormat.SQL);
        config.setTransferData(data);
        config.setTransferDDL(ddl);
        config.setBatchCommitNum(600);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName(TEST_TABLE_NAME.toLowerCase());
        object.setDbObjectType(ObjectType.TABLE);
        config.setExportDbObjects(new LinkedList<>(Collections.singleton(object)));
        config.setStopWhenError(true);
        config.setCsvConfig(new CsvConfig());
        try {
            config.setExportFilePath(this.fileManager.getWorkingDir(TaskType.EXPORT, "").getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return config;
    }

    private DataTransferConfig getLoadConfig(boolean external,
            String schema, List<String> importFileNames, boolean data, boolean ddl) {
        DataTransferConfig config = new DataTransferConfig();
        config.setSchemaName(schema);
        config.setConnectionId(connectionId);
        config.setTransferType(DataTransferType.IMPORT);
        config.setDataTransferFormat(DataTransferFormat.SQL);
        config.setTransferData(data);
        config.setImportFileName(importFileNames);
        if (importFileNames.size() == 1 && importFileNames.get(0).endsWith("zip")) {
            config.setFileType("zip");
        }
        config.setTransferDDL(ddl);
        if (external) {
            config.setTransferData(true);
            config.setTransferDDL(false);
        }
        config.setBatchCommitNum(600);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName(TEST_TABLE_NAME.toLowerCase());
        object.setDbObjectType(ObjectType.TABLE);
        config.setExportDbObjects(Collections.singletonList(object));
        config.setReplaceSchemaWhenExists(true);
        config.setTruncateTableBeforeImport(true);
        config.setStopWhenError(true);
        config.setCsvConfig(new CsvConfig());
        if (external) {
            config.setFileType("SQL");
        }
        return config;
    }

    private File getDumpFile() throws IOException {
        File target = new File(fileManager
                .getWorkingDir(TaskType.EXPORT, DataTransferService.CLIENT_DIR_PREFIX + BUCKET).getAbsolutePath());
        List<File> files = Arrays.stream(target.listFiles()).filter(file -> file.getName().endsWith("zip"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, files.size());
        return files.get(0);
    }

    private void assertTableExists() throws SQLException {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(1) FROM " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    Assert.assertTrue(resultSet.next());
                }
            }
        }
    }

    private void assertTableNotExists() throws SQLException {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(1) FROM " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    Assert.fail();
                } catch (Exception e) {
                    boolean flag =
                            e.getMessage().contains("does not exist") || e.getMessage().contains("doesn't exist");
                    Assert.assertTrue(flag);
                }
            }
        }
    }

    private void assertFileTypeMatchAll(ExportOutput ExportOutput,
            Set<Class<? extends AbstractOutputFile>> fileClasses) {
        List<AbstractOutputFile> outputFileList = ExportOutput.getAllDumpFiles();
        Set<Class<? extends AbstractOutputFile>> matched = new HashSet<>();
        outputFileList.forEach(f -> {
            Assert.assertTrue(fileClasses.contains(f.getClass()));
            matched.add(f.getClass());
        });
        Assert.assertEquals(matched, fileClasses);
    }

    private void assertFileCountEquals(ExportOutput ExportOutput, int count) {
        List<AbstractOutputFile> outputFileList = ExportOutput.getAllDumpFiles();
        Assert.assertEquals(count, outputFileList.size());
    }

    private void assertObjectTypeIn(ExportOutput ExportOutput, Set<ObjectType> objectTypeSet) {
        List<AbstractOutputFile> outputFileList = ExportOutput.getAllDumpFiles();
        outputFileList.forEach(f -> Assert.assertTrue(objectTypeSet.contains(f.getObjectType())));
    }

    private void assertTableCountEquals(int count) throws SQLException {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(1) FROM " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(count, resultSet.getInt(1));
                }
            }
        }
    }

}
