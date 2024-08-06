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
package com.oceanbase.odc.plugin.schema.oboracle.parser;

import java.sql.Connection;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.util.FileUtil;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

/**
 * @author jingtian
 * @date 2023/7/5
 */
public class OBOracleGetDBTableByParserTest {
    private static TestDBConfiguration configuration =
            TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
    private static Connection connection;
    private static String TEST_DATABASE_NAME = configuration.getDefaultDBName();
    private static final String BASE_PATH = "src/test/resources/parser/";
    private static String ddl;
    private static String dropTables;

    @BeforeClass
    public static void setUp() throws Exception {
        connection = configuration.getDataSource().getConnection();
        dropTables = FileUtil.loadAsString(BASE_PATH + "drop.sql");
        batchExcuteSql(dropTables);
        ddl = FileUtil.loadAsString(BASE_PATH + "testGetTableByParser.sql");
        JdbcOperationsUtil.getJdbcOperations(connection).execute(ddl);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        batchExcuteSql(dropTables);
    }

    private static void batchExcuteSql(String str) {
        for (String ddl : str.split("/")) {
            JdbcOperationsUtil.getJdbcOperations(connection).execute(ddl);
        }
    }

    @Test
    public void getPartition_Hash_Success() {
        OBOracleGetDBTableByParser table =
                new OBOracleGetDBTableByParser(connection, TEST_DATABASE_NAME, "HASH_PART_BY_PARSER");
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(6L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
        Assert.assertEquals("COL1", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getPartition_list_Success() {
        OBOracleGetDBTableByParser table =
                new OBOracleGetDBTableByParser(connection, TEST_DATABASE_NAME, "LIST_PART_BY_PARSER");
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(4L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partition.getPartitionOption().getType());
        Assert.assertEquals("LOG_VALUE", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getPartition_range_Success() {
        OBOracleGetDBTableByParser table =
                new OBOracleGetDBTableByParser(connection, TEST_DATABASE_NAME, "RANGE_PART_BY_PARSER");
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(4L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partition.getPartitionOption().getType());
        Assert.assertEquals("LOG_DATE", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getConstraints_Success() {
        OBOracleGetDBTableByParser table =
                new OBOracleGetDBTableByParser(connection, TEST_DATABASE_NAME, "CONSTRAINT_MULTY_BY_PARSER");
        List<DBTableConstraint> constraints = table.listConstraints();
        Assert.assertEquals(7, constraints.size());
        for (DBTableConstraint cons : constraints) {
            if ("test约束_OBNOTNULL_1677652859452753".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.CHECK, cons.getType());
                Assert.assertEquals("COL1", cons.getColumnNames().get(0));
            } else if ("CONSTRAINT_MULTY3_OBNOTNULL_1688716122141818".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.CHECK, cons.getType());
                Assert.assertEquals("COL2", cons.getColumnNames().get(0));
            } else if ("pk3".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.PRIMARY_KEY, cons.getType());
                Assert.assertEquals("COL1", cons.getColumnNames().get(0));
            } else if ("fk3".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.FOREIGN_KEY, cons.getType());
                Assert.assertEquals("A", cons.getColumnNames().get(0));
                Assert.assertEquals(TEST_DATABASE_NAME, cons.getReferenceSchemaName());
                Assert.assertEquals("CONSTRAINT_PRIMARY_BY_PARSER", cons.getReferenceTableName());
                Assert.assertEquals("COL1", cons.getReferenceColumnNames().get(0));
                Assert.assertEquals(DBForeignKeyModifyRule.NO_ACTION, cons.getOnDeleteRule());
            } else if ("unq33".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.UNIQUE_KEY, cons.getType());
                Assert.assertEquals("COL2", cons.getColumnNames().get(0));
            } else if ("unq332".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.UNIQUE_KEY, cons.getType());
                Assert.assertEquals(2, cons.getColumnNames().size());
            } else if ("check33".equals(cons.getName())) {
                Assert.assertEquals(DBConstraintType.CHECK, cons.getType());
                Assert.assertEquals("\"COL5\" > 1", cons.getCheckClause());
            }
        }
    }

    @Test
    public void getIndex_success() {
        OBOracleGetDBTableByParser table =
                new OBOracleGetDBTableByParser(connection, TEST_DATABASE_NAME, "TEST_INDEX_BY_PARSER");
        List<DBTableIndex> indexes = table.listIndexes();
        Assert.assertEquals(7, indexes.size());
        for (DBTableIndex index : indexes) {
            if ("CONSTRAINT_UNIQUE_TEST_INDEX_BY_PARSER".equals(index.getName())) {
                Assert.assertEquals("COL5", index.getColumnNames().get(0));
                Assert.assertEquals("COL6", index.getColumnNames().get(1));
                Assert.assertEquals(2, index.getColumnNames().size());
                Assert.assertTrue(index.getUnique());
                Assert.assertEquals(DBIndexType.UNIQUE, index.getType());
                Assert.assertTrue(index.getAvailable());
            } else if ("IND_FUNCTION_BASED".equals(index.getName())) {
                Assert.assertEquals("UPPER(\"COL1\")", index.getColumnNames().get(0));
                Assert.assertEquals(DBIndexType.FUNCTION_BASED_NORMAL, index.getType());
                Assert.assertTrue(index.getAvailable());
            } else if ("UNIQUE_IDX_TEST_INDEX_BY_PARSER".equals(index.getName())) {
                Assert.assertEquals(DBIndexType.UNIQUE, index.getType());
                Assert.assertEquals("COL3", index.getColumnNames().get(0));
                Assert.assertFalse(index.getGlobal());
                Assert.assertTrue(index.getAvailable());
            } else if ("NORMAL_IDX_TEST_INDEX_BY_PARSER".equals(index.getName())) {
                Assert.assertEquals(DBIndexType.NORMAL, index.getType());
                Assert.assertEquals("COL7", index.getColumnNames().get(0));
                Assert.assertFalse(index.getGlobal());
                Assert.assertTrue(index.getAvailable());
            } else if (index.getPrimary()) {
                Assert.assertTrue(index.getUnique());
                Assert.assertEquals("ID", index.getColumnNames().get(0));
                Assert.assertTrue(index.getAvailable());
            } else if ("IND_FUNCTION_BASED2".equals(index.getName())) {
                Assert.assertEquals(DBIndexType.FUNCTION_BASED_NORMAL, index.getType());
                Assert.assertEquals("\"COL8\" + 1", index.getColumnNames().get(0));
            } else {
                Assert.assertEquals("COL2", index.getColumnNames().get(0));
                Assert.assertTrue(index.getUnique());
                Assert.assertTrue(index.getAvailable());
            }
        }
    }
}
