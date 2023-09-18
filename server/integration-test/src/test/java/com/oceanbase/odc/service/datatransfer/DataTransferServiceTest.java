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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datatransfer.dumper.AbstractOutputFile;
import com.oceanbase.odc.service.datatransfer.dumper.DataFile;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.odc.service.datatransfer.dumper.SchemaFile;
import com.oceanbase.odc.service.datatransfer.model.DataTransferParameter;
import com.oceanbase.odc.service.datatransfer.model.UploadFileResult;
import com.oceanbase.odc.service.datatransfer.task.ObLoaderDumperContext;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link DataTransferServiceTest}
 *
 * @author yh263208
 * @date 2022-07-27 17:32
 * @since ODC_release_3.4.0
 */
@Slf4j
public class DataTransferServiceTest extends ServiceTestEnv {

    private static final String TEST_TABLE_NAME = "LOADER_DUMPER_TEST";
    private static final String BUCKET = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    private final Long oracleConnId = 1L;
    private final Long mysqlConnId = 2L;
    private ConnectionConfig oracleConnConfig;
    private ConnectionConfig mysqlConnConfig;
    @MockBean
    private ConnectionService connectionService;
    @Autowired
    private LocalFileManager fileManager;
    @Autowired
    private DataTransferService dataTransferService;
    @MockBean
    private DatabaseService databaseService;

    @Before
    public void setUp() throws Exception {
        FileUtils.forceDelete(fileManager.getWorkingDir(TaskType.IMPORT, null));
        mysqlConnConfig = buildTestConnection(DialectType.OB_MYSQL);
        oracleConnConfig = buildTestConnection(DialectType.OB_ORACLE);
        setUpEnv(mysqlConnConfig);
        setUpEnv(oracleConnConfig);
        Mockito.when(connectionService.getForConnect(mysqlConnId)).thenReturn(mysqlConnConfig);
        Mockito.when(connectionService.getForConnect(oracleConnId)).thenReturn(oracleConnConfig);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(mysqlConnId)).thenReturn(mysqlConnConfig);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(oracleConnId)).thenReturn(oracleConnConfig);
    }

    @Test
    public void create_dumpSchemaAndDataForOracleMode_bothSchemaAndDataDumped() throws Exception {
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, getOracleDumpConfig(true, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        DumperOutput dumperOutput = new DumperOutput(getDumpFile());
        assertFileCountEquals(dumperOutput, 2);
        assertObjectTypeIn(dumperOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(dumperOutput, new HashSet<>(Arrays.asList(DataFile.class, SchemaFile.class)));
    }

    @Test
    public void create_dumpSchemaForOracleMode_onlySchemaDumped() throws Exception {
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, getOracleDumpConfig(false, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        DumperOutput dumperOutput = new DumperOutput(getDumpFile());
        assertFileCountEquals(dumperOutput, 1);
        assertObjectTypeIn(dumperOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(dumperOutput, new HashSet<>(Collections.singletonList(SchemaFile.class)));
    }

    @Test
    public void create_dumpSchemaForOracleMode_onlySchemaDumped_mergeSchemaFiles() throws Exception {
        DataTransferParameter config = getOracleDumpConfig(false, true);
        config.setMergeSchemaFiles(true);
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        File target = new File(fileManager
                .getWorkingDir(TaskType.EXPORT, DataTransferService.CLIENT_DIR_PREFIX + BUCKET).getAbsolutePath());
        File[] files = target.listFiles(f -> f.getName().endsWith("schema.sql"));
        Assert.assertEquals(1, files.length);
    }

    @Test
    public void create_dumpDataForOracleMode_onlyDataDumped() throws Exception {
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, getOracleDumpConfig(true, false));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        DumperOutput dumperOutput = new DumperOutput(getDumpFile());
        assertFileCountEquals(dumperOutput, 1);
        assertObjectTypeIn(dumperOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(dumperOutput, new HashSet<>(Collections.singletonList(DataFile.class)));
    }

    @Test
    public void create_dumpSchemaAndDataForMysqlMode_bothSchemaAndDataDumped() throws Exception {
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, getMysqlDumpConfig(true, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        DumperOutput dumperOutput = new DumperOutput(getDumpFile());
        assertFileCountEquals(dumperOutput, 2);
        assertObjectTypeIn(dumperOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(dumperOutput, new HashSet<>(Arrays.asList(DataFile.class, SchemaFile.class)));
    }

    @Test
    public void create_dumpSchemaForMysqlMode_onlySchemaDumped() throws Exception {
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, getMysqlDumpConfig(false, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        DumperOutput dumperOutput = new DumperOutput(getDumpFile());
        assertFileCountEquals(dumperOutput, 1);
        assertObjectTypeIn(dumperOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(dumperOutput, new HashSet<>(Collections.singletonList(SchemaFile.class)));
    }

    @Test
    public void create_dumpDataForMysqlMode_onlyDataDumped() throws Exception {
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, getMysqlDumpConfig(true, false));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));

        DumperOutput dumperOutput = new DumperOutput(getDumpFile());
        assertFileCountEquals(dumperOutput, 1);
        assertObjectTypeIn(dumperOutput, new HashSet<>(Collections.singleton(ObjectType.TABLE)));
        assertFileTypeMatchAll(dumperOutput, new HashSet<>(Collections.singletonList(DataFile.class)));
    }

    @Test
    public void create_loadSchemaAndDataForOracleMode_schemaAndDataLoaded() throws Exception {
        File dumpFile = dumpSchemaAndDataForLoad(DialectType.OB_ORACLE);
        assertOracleModeTableNotExists();

        ObLoaderDumperContext context = dataTransferService.create(BUCKET,
                getOracleLoadConfig(Collections.singletonList(dumpFile.getAbsolutePath()), false, true, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        assertOracleModeTableExists();
        assertOracleModeTableCountEquals(2);
    }

    @Test
    public void create_loadSchemaForOracleMode_schemaLoaded() throws Exception {
        File dumpFile = dumpSchemaAndDataForLoad(DialectType.OB_ORACLE);
        assertOracleModeTableNotExists();

        ObLoaderDumperContext context = dataTransferService.create(BUCKET,
                getOracleLoadConfig(Collections.singletonList(dumpFile.getAbsolutePath()), false, false, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        assertOracleModeTableExists();
        assertOracleModeTableCountEquals(0);
    }

    @Test
    public void create_loadSchemaAndDataForMysqlMode_schemaAndDataLoaded() throws Exception {
        File dumpFile = dumpSchemaAndDataForLoad(DialectType.OB_MYSQL);
        assertMysqlModeTableNotExists();

        ObLoaderDumperContext context = dataTransferService.create(BUCKET,
                getMysqlLoadConfig(Collections.singletonList(dumpFile.getAbsolutePath()), false, true, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        assertMysqlModeTableExists();
        assertMysqlModeTableCountEquals(2);
    }

    @Test
    public void create_loadSchemaForMysqlMode_schemaLoaded() throws Exception {
        File dumpFile = dumpSchemaAndDataForLoad(DialectType.OB_MYSQL);
        assertMysqlModeTableNotExists();

        ObLoaderDumperContext context = dataTransferService.create(BUCKET,
                getMysqlLoadConfig(Collections.singletonList(dumpFile.getAbsolutePath()), false, false, true));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        assertMysqlModeTableExists();
        assertMysqlModeTableCountEquals(0);
    }

    @Test
    public void create_loadExternalSqlForOracleMode_dataLoaded() throws Exception {
        String sqlScript = "INSERT INTO " + TEST_TABLE_NAME + " VALUES ('3', 'Marry'),('4', 'Tom');";
        File target = copyFile(new ByteArrayInputStream(sqlScript.getBytes()), "sql");

        ObLoaderDumperContext context = dataTransferService.create(BUCKET,
                getOracleLoadConfig(Collections.singletonList(target.getAbsolutePath()), true, true, false));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        assertOracleModeTableExists();
        assertOracleModeTableCountEquals(4);
    }

    @Test
    public void create_loadExternalSqlForMysqlMode_dataLoaded() throws Exception {
        String sqlScript = "INSERT INTO " + TEST_TABLE_NAME + " VALUES ('3', 'Marry'),('4', 'Tom');";
        File target = copyFile(new ByteArrayInputStream(sqlScript.getBytes()), "sql");

        ObLoaderDumperContext context = dataTransferService.create(BUCKET,
                getMysqlLoadConfig(Collections.singletonList(target.getAbsolutePath()), true, true, false));
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        assertMysqlModeTableExists();
        assertMysqlModeTableCountEquals(4);
    }

    @Test
    public void create_validSysUserExists_nonCloudModeUsed() throws Exception {
        DataTransferParameter config = getOracleDumpConfig(true, true);
        config.setSysUser(oracleConnConfig.getSysTenantUsername());
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void create_validSysUserPasswdExists_nonCloudModeUsed() throws Exception {
        DataTransferParameter config = getOracleDumpConfig(true, true);
        config.setSysUser(oracleConnConfig.getSysTenantUsername());
        config.setSysPassword(oracleConnConfig.getSysTenantPassword());
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void create_validSysUserInvalidPasswdExists_nonCloudModeUsed() throws Exception {
        DataTransferParameter config = getOracleDumpConfig(true, true);
        config.setSysUser(oracleConnConfig.getSysTenantUsername());
        config.setSysPassword("abcde");
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void getMetaInfo_validZipFileInput_getMetaInfo() throws Exception {
        File target = dumpSchemaAndDataForLoad(DialectType.OB_ORACLE);
        assertOracleModeTableNotExists();

        UploadFileResult actual = dataTransferService.getMetaInfo(target.getAbsolutePath());
        UploadFileResult expect = new UploadFileResult();
        expect.setFormat(DataFormat.SQL);
        expect.setFileType("ZIP");
        expect.setFileName(target.getAbsolutePath());
        expect.setContainsData(true);
        expect.setContainsSchema(true);
        Map<ObjectType, Set<String>> importFileNames = new HashMap<>();
        importFileNames.putIfAbsent(ObjectType.TABLE, Collections.singleton(TEST_TABLE_NAME));
        expect.setImportObjects(importFileNames);

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void getMetaInfo_inValidZipFileInput_getMetaInfo() throws Exception {
        String sqlScript = "INSERT INTO " + TEST_TABLE_NAME + " VALUES ('3', 'Marry'),('4', 'Tom');";
        File target = copyFile(new ByteArrayInputStream(sqlScript.getBytes()), "zip");

        UploadFileResult actual = dataTransferService.getMetaInfo(target.getAbsolutePath());
        Assert.assertEquals(ErrorCodes.ImportInvalidFileType, actual.getErrorCode());
    }

    @Test
    public void getExportObjectNames_oracleMode_getNonNull() {
        Database database = new Database();
        database.setId(1L);
        database.setName(oracleConnConfig.defaultSchema());
        Project project = new Project();
        project.setId(1L);
        database.setProject(project);
        database.setDataSource(oracleConnConfig);
        Mockito.when(databaseService.detail(1L)).thenReturn(database);
        Map<ObjectType, Set<String>> actual =
                dataTransferService.getExportObjectNames(1L, null);
        Assert.assertTrue(actual.get(ObjectType.TABLE).contains(TEST_TABLE_NAME));
    }

    @Test
    public void getExportObjectNames_mysqlMode_getNonNull() {
        Database database = new Database();
        database.setId(1L);
        database.setName(mysqlConnConfig.defaultSchema());
        Project project = new Project();
        project.setId(1L);
        database.setProject(project);
        database.setDataSource(mysqlConnConfig);
        Mockito.when(databaseService.detail(1L)).thenReturn(database);
        Map<ObjectType, Set<String>> actual =
                dataTransferService.getExportObjectNames(1L, null);
        Assert.assertTrue(actual.get(ObjectType.TABLE).contains(TEST_TABLE_NAME.toLowerCase()));
    }

    @Test
    public void test_GetCsvFileInfo() throws IOException {
        URL url = DataTransferService.class.getClassLoader().getResource("datatransfer/TEST.csv");
        File csvFile = copyFile(url.openStream(), "csv");
        CsvConfig csvConfig = new CsvConfig();
        csvConfig.setFileName(csvFile.getAbsolutePath());
        List<CsvColumnMapping> csvFileInfo = dataTransferService.getCsvFileInfo(csvConfig);
        Assert.assertEquals(2, csvFileInfo.size());
    }

    private File copyFile(InputStream inputStream, String extend) throws IOException {
        File target = fileManager.getWorkingDir(TaskType.IMPORT, LocalFileManager.UPLOAD_BUCKET);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extend;
        File dest = new File(target.getAbsoluteFile() + File.separator + fileName);
        FileOutputStream outputStream = new FileOutputStream(dest);
        IOUtils.copy(inputStream, outputStream);
        return dest;
    }

    private File dumpSchemaAndDataForLoad(DialectType dialectType) throws Exception {
        DataTransferParameter config;
        if (dialectType == DialectType.OB_MYSQL) {
            config = getMysqlDumpConfig(true, true);
        } else {
            config = getOracleDumpConfig(true, true);
        }
        ObLoaderDumperContext context = dataTransferService.create(BUCKET, config);
        Assert.assertNotNull(context.get(10, TimeUnit.SECONDS));
        File dumpFile = getDumpFile();
        File returnVal = copyFile(new FileInputStream(dumpFile), "zip");
        FileUtils.forceDelete(dumpFile);
        if (dialectType == DialectType.OB_MYSQL) {
            clearEnv(mysqlConnConfig);
        } else {
            clearEnv(oracleConnConfig);
        }
        return returnVal;
    }

    private void assertOracleModeTableExists() throws SQLException {
        assertTableExists(oracleConnConfig);
    }

    private void assertMysqlModeTableExists() throws SQLException {
        assertTableExists(mysqlConnConfig);
    }

    private void assertOracleModeTableNotExists() throws SQLException {
        assertTableNotExists(oracleConnConfig);
    }

    private void assertMysqlModeTableNotExists() throws SQLException {
        assertTableNotExists(mysqlConnConfig);
    }

    private void assertOracleModeTableCountEquals(int count) throws SQLException {
        assertTableCountEquals(oracleConnConfig, count);
    }

    private void assertMysqlModeTableCountEquals(int count) throws SQLException {
        assertTableCountEquals(mysqlConnConfig, count);
    }

    private void assertTableCountEquals(ConnectionConfig connectionConfig, int count) throws SQLException {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig, ConnectionAccountType.MAIN);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(1) FROM " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(count, resultSet.getInt(1));
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        }
    }

    private void assertTableExists(ConnectionConfig connectionConfig) throws SQLException {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig, ConnectionAccountType.MAIN);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT COUNT(1) FROM " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    Assert.assertTrue(resultSet.next());
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        }
    }

    private void assertTableNotExists(ConnectionConfig connectionConfig) throws SQLException {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig, ConnectionAccountType.MAIN);
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

    private void assertFileTypeMatchAll(DumperOutput dumperOutput,
            Set<Class<? extends AbstractOutputFile>> fileClasses) {
        List<AbstractOutputFile> outputFileList = dumperOutput.getAllDumpFiles();
        Set<Class<? extends AbstractOutputFile>> matched = new HashSet<>();
        outputFileList.forEach(f -> {
            Assert.assertTrue(fileClasses.contains(f.getClass()));
            matched.add(f.getClass());
        });
        Assert.assertEquals(matched, fileClasses);
    }

    private void assertFileCountEquals(DumperOutput dumperOutput, int count) {
        List<AbstractOutputFile> outputFileList = dumperOutput.getAllDumpFiles();
        Assert.assertEquals(count, outputFileList.size());
    }

    private void assertObjectTypeIn(DumperOutput dumperOutput, Set<ObjectType> objectTypeSet) {
        List<AbstractOutputFile> outputFileList = dumperOutput.getAllDumpFiles();
        outputFileList.forEach(f -> Assert.assertTrue(objectTypeSet.contains(f.getObjectType())));
    }

    private void assertDataMakingResultsExcepted(File dumperZipFile) throws IOException {
        ZipFile zipFile = new ZipFile(dumperZipFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        String actual = "";
        String excepted = "\"masked\",\"masked\"";
        do {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".csv")) {
                InputStream is = zipFile.getInputStream(entry);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                actual = br.readLine();
                break;
            }
        } while (entries.hasMoreElements());
        Assert.assertEquals(excepted, actual);
    }

    private void setUpEnv(ConnectionConfig connectionConfig) throws Exception {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig, ConnectionAccountType.MAIN);
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

    private void clearEnv(ConnectionConfig connectionConfig) throws Exception {
        DataSourceFactory factory = new DruidDataSourceFactory(connectionConfig, ConnectionAccountType.MAIN);
        DataSource dataSource = factory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String drop = "DROP TABLE " + TEST_TABLE_NAME;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(drop);
            }
        }
    }

    private File getDumpFile() throws IOException {
        File target = new File(fileManager
                .getWorkingDir(TaskType.EXPORT, DataTransferService.CLIENT_DIR_PREFIX + BUCKET).getAbsolutePath());
        List<File> files = Arrays.stream(target.listFiles()).filter(file -> file.getName().endsWith("zip"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, files.size());
        return files.get(0);
    }

    private ConnectionConfig buildTestConnection(DialectType dialectType) {
        ConnectionConfig connection = TestConnectionUtil.getTestConnectionConfig(ConnectType.from(dialectType));
        connection.setId(oracleConnId);
        if (DialectType.OB_MYSQL == dialectType) {
            connection.setId(mysqlConnId);
        }
        return connection;
    }

    private DataTransferParameter getOracleDumpConfig(boolean data, boolean ddl) {
        return getDumpConfig(DialectType.OB_ORACLE, oracleConnConfig.getDefaultSchema(), data, ddl);
    }

    private DataTransferParameter getMysqlDumpConfig(boolean data, boolean ddl) {
        return getDumpConfig(DialectType.OB_MYSQL, mysqlConnConfig.getDefaultSchema(), data, ddl);
    }

    private DataTransferParameter getDumpConfig(DialectType dialectType,
            String schema, boolean data, boolean ddl) {
        DataTransferParameter config = new DataTransferParameter();
        config.setConnectionId(dialectType == DialectType.OB_MYSQL ? mysqlConnId : oracleConnId);
        config.setSchemaName(schema);
        config.setTransferType(DataTransferType.EXPORT);
        config.setDataTransferFormat(DataTransferFormat.SQL);
        config.setTransferData(data);
        config.setTransferDDL(ddl);
        config.setBatchCommitNum(100);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName(TEST_TABLE_NAME);
        if (dialectType.isMysql()) {
            object.setObjectName(TEST_TABLE_NAME.toLowerCase());
        }
        object.setDbObjectType(ObjectType.TABLE);
        config.setExportDbObjects(new LinkedList<>(Collections.singleton(object)));
        config.setCsvConfig(new CsvConfig());
        try {
            config.setExportFilePath(this.fileManager.getWorkingDir(TaskType.EXPORT, "").getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return config;
    }

    private DataTransferParameter getOracleLoadConfig(List<String> importFileNames,
            boolean external, boolean data, boolean ddl) {
        return getLoadConfig(DialectType.OB_ORACLE, external,
                oracleConnConfig.getDefaultSchema(), importFileNames, data, ddl);
    }

    private DataTransferParameter getMysqlLoadConfig(List<String> importFileNames,
            boolean external, boolean data, boolean ddl) {
        return getLoadConfig(DialectType.OB_MYSQL, external,
                mysqlConnConfig.getDefaultSchema(), importFileNames, data, ddl);
    }

    private DataTransferParameter getLoadConfig(DialectType dialectType, boolean external,
            String schema, List<String> importFileNames, boolean data, boolean ddl) {
        DataTransferParameter config = new DataTransferParameter();
        config.setSchemaName(schema);
        config.setConnectionId(dialectType == DialectType.OB_MYSQL ? mysqlConnId : oracleConnId);
        config.setTransferType(DataTransferType.IMPORT);
        config.setDataTransferFormat(DataTransferFormat.SQL);
        config.setTransferData(data);
        config.setImportFileName(importFileNames);
        config.setTransferDDL(ddl);
        if (external) {
            config.setTransferData(true);
            config.setTransferDDL(false);
        }
        config.setBatchCommitNum(100);
        DataTransferObject object = new DataTransferObject();
        object.setObjectName(TEST_TABLE_NAME);
        if (dialectType.isMysql()) {
            object.setObjectName(TEST_TABLE_NAME.toLowerCase());
        }
        object.setDbObjectType(ObjectType.TABLE);
        config.setExportDbObjects(Collections.singletonList(object));
        config.setReplaceSchemaWhenExists(true);
        config.setTruncateTableBeforeImport(true);
        config.setCsvConfig(new CsvConfig());
        if (external) {
            config.setFileType("SQL");
        }
        return config;
    }

}
