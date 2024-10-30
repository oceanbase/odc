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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.tool.TestRandom;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.SneakyThrows;

/**
 * @Author: Lebie
 * @Date: 2024/4/7 16:06
 * @Description: []
 */
public class LogicalTableFinderTest extends ServiceTestEnv {
    private static final String OBMYSQL_BASE_PATH = "src/test/resources/connection/obmysql/";
    private static final JdbcTemplate OBMYSQL_JDBC_TEMPLATE =
            new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());

    @MockBean
    private DBObjectRepository dbObjectRepository;

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
    @Ignore
    public void testOBMySQL() {
        List<LogicalTable> expected =
                YamlUtils.fromYamlList("connection/obmysql/verify.yml", LogicalTable.class);
        Collections.sort(expected, Comparator.comparing(LogicalTable::getTableNamePattern));
        Mockito.when(dbObjectRepository.findByDatabaseIdAndType(1L, DBObjectType.TABLE)).thenReturn(getTables_0());
        Mockito.when(dbObjectRepository.findByDatabaseIdAndType(2L, DBObjectType.TABLE)).thenReturn(getTables_1());
        Mockito.when(dbObjectRepository.findByDatabaseIdAndType(3L, DBObjectType.TABLE)).thenReturn(getTables_2());
        Mockito.when(dbObjectRepository.findByDatabaseIdAndType(4L, DBObjectType.TABLE)).thenReturn(getTables_3());
        Mockito.when(dbObjectRepository.findByDatabaseIdAndType(5L, DBObjectType.TABLE))
                .thenReturn(Collections.emptyList());

        List<LogicalTable> actual = new LogicalTableFinder(obtainOBMySQLDatabases(), dbObjectRepository).find();

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
            database.setId((long) i + 1);
            database.setDataSource(TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL));
            database.setName("db_0" + i);
            databases.add(database);
        }
        Database database = new Database();
        database.setDataSource(TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL));
        database.setName("db_empty");
        database.setId(5L);
        databases.add(database);
        return databases;
    }

    private List<DBObjectEntity> getTables_0() {
        List<DBObjectEntity> tables = new ArrayList<>();
        List<String> tableNames = new ArrayList<>();
        tableNames.addAll(Arrays.asList("tb_a_00", "tb_i_00", "tb_i_01", "tb_j_00", "tb_j_01", "tb_n", "tb_o_0",
                "tb_o_10", "tb_p", "tb_q_00_000", "tb_q_00_001", "tb_r_0", "tb_s_0", "tb_s_5", "tb_b_00", "tb_b_01",
                "tb_c_00", "tb_c_01", "tb_d_00", "tb_d_01", "tb_e_00", "tb_e_01", "tb_f_00", "tb_f_01", "tb_g_00",
                "tb_g_01", "tb_h_00", "tb_h_01", "tb_k_00", "tb_l_00", "tb_l_01", "tb_m_00", "tb_m_01"));
        for (int i = 0; i < tableNames.size(); i++) {
            DBObjectEntity table = TestRandom.nextObject(DBObjectEntity.class);
            table.setName(tableNames.get(i));
            table.setDatabaseId(1L);
            table.setType(DBObjectType.TABLE);
            table.setId(TestRandom.nextObject(Long.class));
            tables.add(table);
        }
        return tables;
    }

    private List<DBObjectEntity> getTables_1() {
        List<DBObjectEntity> tables = new ArrayList<>();
        List<String> tableNames = new ArrayList<>();
        tableNames.addAll(Arrays.asList("tb_a_01", "tb_n", "tb_o_0", "tb_o_10", "tb_p", "tb_q_01_000", "tb_q_01_001",
                "tb_r_1", "tb_s_1"));
        for (int i = 0; i < tableNames.size(); i++) {
            DBObjectEntity table = TestRandom.nextObject(DBObjectEntity.class);
            table.setName(tableNames.get(i));
            table.setDatabaseId(2L);
            table.setType(DBObjectType.TABLE);
            tables.add(table);
        }
        return tables;
    }

    private List<DBObjectEntity> getTables_2() {
        List<DBObjectEntity> tables = new ArrayList<>();
        List<String> tableNames = new ArrayList<>();
        tableNames.addAll(Arrays.asList("tb_a_02", "tb_n", "tb_o_0", "tb_o_10", "tb_q_02_000", "tb_q_02_001", "tb_r_2",
                "tb_s_2"));
        for (int i = 0; i < tableNames.size(); i++) {
            DBObjectEntity table = TestRandom.nextObject(DBObjectEntity.class);
            table.setName(tableNames.get(i));
            table.setDatabaseId(3L);
            table.setType(DBObjectType.TABLE);
            tables.add(table);
        }
        return tables;
    }

    private List<DBObjectEntity> getTables_3() {
        List<DBObjectEntity> tables = new ArrayList<>();
        List<String> tableNames = new ArrayList<>();
        tableNames.addAll(Arrays.asList("tb_a_03", "tb_n", "tb_o_0", "tb_o_10", "tb_p", "tb_q_03_000", "tb_q_03_001",
                "tb_r_3", "tb_s_3"));
        for (int i = 0; i < tableNames.size(); i++) {
            DBObjectEntity table = TestRandom.nextObject(DBObjectEntity.class);
            table.setName(tableNames.get(i));
            table.setDatabaseId(4L);
            table.setType(DBObjectType.TABLE);
            tables.add(table);
        }
        return tables;
    }
}
