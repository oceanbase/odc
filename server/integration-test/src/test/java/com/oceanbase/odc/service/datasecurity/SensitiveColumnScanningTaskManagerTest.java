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
package com.oceanbase.odc.service.datasecurity;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo.ScanningTaskStatus;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.odc.test.tool.IsolatedNameGenerator;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/29 13:52
 */
public class SensitiveColumnScanningTaskManagerTest extends ServiceTestEnv {

    private static ConnectionSession mysqlSession;

    private static ConnectionSession oracleSession;

    private static ConnectionConfig mysqlConnectionConfig;

    private static ConnectionConfig oracleConnectionConfig;

    private static final Long PROJECT_ID = 1L;
    private static final Long MASKING_ALGORITHM_ID = 1L;
    private static final String MYSQL_DATABASE_1 = IsolatedNameGenerator.generateLowerCase("test_sensitive_column_1");
    private static final String MYSQL_DATABASE_2 = IsolatedNameGenerator.generateLowerCase("test_sensitive_column_2");
    private static final String ORACLE_DATABASE_1 = IsolatedNameGenerator.generateUpperCase("TEST_SENSITIVE_COLUMN_1");
    private static final String ORACLE_DATABASE_2 = IsolatedNameGenerator.generateUpperCase("TEST_SENSITIVE_COLUMN_2");
    private static final TestSensitiveColumnSql sql = getTestSensitiveColumnSql();

    @Autowired
    private SensitiveColumnScanningTaskManager manager;

    @BeforeClass
    public static void setUp() {
        mysqlSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        mysqlSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(sql.getMysql().getCreate());
        mysqlConnectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);

        oracleSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        oracleSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .update(sql.getOracle().getCreate());
        oracleConnectionConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_ORACLE);
    }

    @AfterClass
    public static void tearDown() {
        SyncJdbcExecutor executor = mysqlSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        String executeSql = sql.getMysql().getDrop();
        executor.update(executeSql);
        executor = oracleSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        executeSql = sql.getOracle().getDrop();
        executor.update(executeSql);
    }

    @Test
    public void test_start_groovyRule_OBMySQL() {
        List<Database> databases = createDatabases(ConnectType.OB_MYSQL);
        List<SensitiveRule> rules = Arrays.asList(createGroovySensitiveRules());
        SensitiveColumnScanningTaskInfo taskInfo = manager.start(databases, rules, mysqlConnectionConfig, null);
        await().atMost(20, SECONDS)
                .until(() -> manager.get(taskInfo.getTaskId()).getStatus() == ScanningTaskStatus.SUCCESS);
        Assert.assertEquals(2, manager.get(taskInfo.getTaskId()).getSensitiveColumns().size());
    }

    @Test
    public void test_start_groovyRule_OBOracle() {
        List<Database> databases = createDatabases(ConnectType.OB_ORACLE);
        List<SensitiveRule> rules = Arrays.asList(createGroovySensitiveRules());
        SensitiveColumnScanningTaskInfo taskInfo = manager.start(databases, rules, oracleConnectionConfig, null);
        await().atMost(20, SECONDS)
                .until(() -> manager.get(taskInfo.getTaskId()).getStatus() == ScanningTaskStatus.SUCCESS);
        Assert.assertEquals(2, manager.get(taskInfo.getTaskId()).getSensitiveColumns().size());
    }

    @Test
    public void test_start_pathRule_OBMySQL() {
        List<Database> databases = createDatabases(ConnectType.OB_MYSQL);
        List<SensitiveRule> rules = Arrays.asList(createPathSensitiveRules());
        SensitiveColumnScanningTaskInfo taskInfo = manager.start(databases, rules, mysqlConnectionConfig, null);
        await().atMost(20, SECONDS)
                .until(() -> manager.get(taskInfo.getTaskId()).getStatus() == ScanningTaskStatus.SUCCESS);
        Assert.assertEquals(20, manager.get(taskInfo.getTaskId()).getSensitiveColumns().size());
    }

    @Test
    public void test_start_pathRule_OBMOracle() {
        List<Database> databases = createDatabases(ConnectType.OB_ORACLE);
        List<SensitiveRule> rules = Arrays.asList(createPathSensitiveRules());
        SensitiveColumnScanningTaskInfo taskInfo = manager.start(databases, rules, oracleConnectionConfig, null);
        await().atMost(20, SECONDS)
                .until(() -> manager.get(taskInfo.getTaskId()).getStatus() == ScanningTaskStatus.SUCCESS);
        Assert.assertEquals(20, manager.get(taskInfo.getTaskId()).getSensitiveColumns().size());
    }

    @Test
    public void test_start_RegexRule_OBMySQL() {
        List<Database> databases = createDatabases(ConnectType.OB_MYSQL);
        List<SensitiveRule> rules = Arrays.asList(createRegexSensitiveRules(ConnectType.OB_MYSQL));
        SensitiveColumnScanningTaskInfo taskInfo = manager.start(databases, rules, mysqlConnectionConfig, null);
        await().atMost(20, SECONDS)
                .until(() -> manager.get(taskInfo.getTaskId()).getStatus() == ScanningTaskStatus.SUCCESS);
        Assert.assertEquals(6, manager.get(taskInfo.getTaskId()).getSensitiveColumns().size());
    }

    @Test
    public void test_start_RegexRule_OBOracle() {
        List<Database> databases = createDatabases(ConnectType.OB_ORACLE);
        List<SensitiveRule> rules = Arrays.asList(createRegexSensitiveRules(ConnectType.OB_ORACLE));
        SensitiveColumnScanningTaskInfo taskInfo = manager.start(databases, rules, oracleConnectionConfig, null);
        await().atMost(20, SECONDS)
                .until(() -> manager.get(taskInfo.getTaskId()).getStatus() == ScanningTaskStatus.SUCCESS);
        Assert.assertEquals(6, manager.get(taskInfo.getTaskId()).getSensitiveColumns().size());
    }

    private List<Database> createDatabases(ConnectType type) {
        if (type == ConnectType.OB_MYSQL) {
            return Arrays.asList(
                    createDatabase(MYSQL_DATABASE_1),
                    createDatabase(MYSQL_DATABASE_2));
        } else {
            return Arrays.asList(
                    createDatabase(ORACLE_DATABASE_1),
                    createDatabase(ORACLE_DATABASE_2));
        }
    }

    private Database createDatabase(String name) {
        Database database = new Database();
        Project project = new Project();
        project.setId(PROJECT_ID);
        database.setProject(project);
        database.setName(name);
        return database;
    }

    private SensitiveRule createGroovySensitiveRules() {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(1L);
        rule.setType(SensitiveRuleType.GROOVY);
        rule.setMaskingAlgorithmId(MASKING_ALGORITHM_ID);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setGroovyScript("if (column.name.equalsIgnoreCase(\"birthday\")) {\n"
                + "    return true;\n"
                + "} else {\n"
                + "    return false;\n"
                + "}");
        return rule;
    }

    private SensitiveRule createPathSensitiveRules() {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(2L);
        rule.setType(SensitiveRuleType.PATH);
        rule.setMaskingAlgorithmId(MASKING_ALGORITHM_ID);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setPathIncludes(Arrays.asList("*.*.*"));
        rule.setPathExcludes(new ArrayList<>());
        return rule;
    }

    private SensitiveRule createRegexSensitiveRules(ConnectType type) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(3L);
        rule.setType(SensitiveRuleType.REGEX);
        rule.setMaskingAlgorithmId(MASKING_ALGORITHM_ID);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setDatabaseRegexExpression("^\\S*(sensitive)\\S*$");
        rule.setTableRegexExpression("^\\S*(user|salary)\\S*$");
        rule.setColumnRegexExpression("^\\S*(email|address|salary)\\S*$");
        if (type == ConnectType.OB_ORACLE) {
            rule.setDatabaseRegexExpression("^\\S*(SENSITIVE)\\S*$");
            rule.setTableRegexExpression("^\\S*(USER|SALARY)\\S*$");
            rule.setColumnRegexExpression("^\\S*(EMAIL|ADDRESS|SALARY)\\S*$");
        }
        return rule;
    }

    private static TestSensitiveColumnSql getTestSensitiveColumnSql() {
        String filepath = "src/test/resources/datasecurity/test_scanning_sensitive_columns_ddl.yaml";
        String sqlYaml;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(filepath)))) {
            sqlYaml = IOUtils.toString(in, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sqlYaml =
                MessageFormat.format(sqlYaml, MYSQL_DATABASE_1, MYSQL_DATABASE_2, ORACLE_DATABASE_1, ORACLE_DATABASE_2);
        return YamlUtils.from(sqlYaml, TestSensitiveColumnSql.class);
    }

    @Data
    private static class TestSensitiveColumnSql {
        private TestSensitiveColumnSqlMeta mysql;
        private TestSensitiveColumnSqlMeta oracle;
    }

    @Data
    private static class TestSensitiveColumnSqlMeta {
        private String create;
        private String drop;
    }

}
