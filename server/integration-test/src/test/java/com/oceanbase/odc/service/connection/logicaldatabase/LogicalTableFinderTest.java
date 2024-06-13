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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;
import com.oceanbase.odc.test.database.TestDBConfigurations;

import lombok.SneakyThrows;

/**
 * @Author: Lebie
 * @Date: 2024/4/7 16:06
 * @Description: []
 */
public class LogicalTableFinderTest {
    private static final String OBMYSQL_BASE_PATH = "src/test/resources/connection/obmysql/";
    private static final JdbcTemplate OBMYSQL_JDBC_TEMPLATE =
            new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());

    @BeforeClass
    public static void setUp() throws IOException {
        String dropTables = loadAsString(OBMYSQL_BASE_PATH + "drop.sql");
        OBMYSQL_JDBC_TEMPLATE.execute(dropTables);

        String createTables = loadAsString(OBMYSQL_BASE_PATH + "create.sql");
        List<String> sqls = SqlUtils.split(DialectType.OB_MYSQL, createTables, ";");
        sqls.forEach(sql -> OBMYSQL_JDBC_TEMPLATE.execute(sql));
    }

    @AfterClass
    public static void clear() throws Exception {
        String dropTables = loadAsString(OBMYSQL_BASE_PATH + "drop.sql");
        OBMYSQL_JDBC_TEMPLATE.execute(dropTables);
    }


    @Test
    public void testOBMySQL() {
        List<LogicalTable> expected =
                YamlUtils.fromYamlList("connection/obmysql/verify.yml", LogicalTable.class);
        Collections.sort(expected, Comparator.comparing(LogicalTable::getTableNamePattern));
        List<LogicalTable> actual = new LogicalTableFinder(obtainOBMySQLDatabases()).find();

        Assert.assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            LogicalTable expectedTable = expected.get(i);
            LogicalTable actualTable = actual.get(i);
            Assert.assertEquals(expectedTable.getName(), actualTable.getName());
            Assert.assertEquals(expectedTable.getTableNamePattern(), actualTable.getTableNamePattern());
            Assert.assertEquals(expectedTable.getDatabaseNamePattern(), actualTable.getDatabaseNamePattern());
            Assert.assertEquals(expectedTable.getFullNameExpression(), actualTable.getFullNameExpression());
            Assert.assertEquals(expectedTable.getActualDataNodes().size(), actualTable.getActualDataNodes().size());
            for (int j = 0; j < expectedTable.getActualDataNodes().size(); j++) {
                DataNode expectedDataNode = expectedTable.getActualDataNodes().get(j);
                DataNode actualDataNode = actualTable.getActualDataNodes().get(j);
                Assert.assertEquals(expectedDataNode.getFullName(), actualDataNode.getFullName());
            }
        }
    }

    @SneakyThrows
    private static String loadAsString(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            sb.append(readFile(path));
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String readFile(String strFile) throws IOException {
        try (InputStream input = new FileInputStream(strFile)) {
            int available = input.available();
            byte[] bytes = new byte[available];
            input.read(bytes);
            return new String(bytes);
        }
    }

    private List<Database> obtainOBMySQLDatabases() {
        List<Database> databases = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Database database = new Database();
            database.setDataSource(TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL));
            database.setName("db_0" + i);
            databases.add(database);
        }
        Database database = new Database();
        database.setDataSource(TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL));
        database.setName("db_empty");
        databases.add(database);
        return databases;
    }
}
