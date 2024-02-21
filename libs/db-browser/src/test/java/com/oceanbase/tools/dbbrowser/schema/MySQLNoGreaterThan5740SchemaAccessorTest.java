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
package com.oceanbase.tools.dbbrowser.schema;

import static com.oceanbase.tools.dbbrowser.editor.DBObjectUtilsTest.loadAsString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.env.BaseTestEnv;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.Data;

/**
 * @author jingtian
 * @date 2023/6/8
 */
public class MySQLNoGreaterThan5740SchemaAccessorTest extends BaseTestEnv {
    private static final String BASE_PATH = "src/test/resources/table/mysql/";
    private static String ddl;
    private static String dropTables;
    private static String testProcedureDDL;
    private static String testFunctionDDL;
    private static List<MySQLNoGreaterThan5740SchemaAccessorTest.DataType> verifyDataTypes = new ArrayList<>();
    private static List<MySQLNoGreaterThan5740SchemaAccessorTest.ColumnAttributes> columnAttributes = new ArrayList<>();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(getMySQLDataSource());
    private static DBSchemaAccessor accessor = new DBSchemaAccessors(getMySQLDataSource()).createMysql();

    @BeforeClass
    public static void setUp() throws Exception {
        initVerifyDataTypes();
        initVerifyColumnAttributes();

        dropTables = loadAsString(BASE_PATH + "drop.sql");
        batchExecuteSql(dropTables, ";");

        ddl = loadAsString(BASE_PATH + "testTableColumnDDL.sql", BASE_PATH + "testTableIndexDDL.sql",
                BASE_PATH + "testConstraintDDL.sql", BASE_PATH + "testTablePartitionDDL.sql",
                BASE_PATH + "testViewDDL.sql");
        batchExecuteSql(ddl, ";");

        testFunctionDDL = loadAsString(BASE_PATH + "testFunctionDDL.sql");
        batchExecuteSql(testFunctionDDL, "/");
        testProcedureDDL = loadAsString(BASE_PATH + "testProcedureDDL.sql");
        batchExecuteSql(testProcedureDDL, "/");
    }

    @AfterClass
    public static void tearDown() {
        batchExecuteSql(dropTables, ";");
    }

    private static void batchExecuteSql(String str, String delimiter) {
        for (String ddl : Arrays.stream(str.split(delimiter)).filter(item -> StringUtils.isNotBlank(item)).collect(
                Collectors.toList())) {
            jdbcTemplate.execute(ddl);
        }
    }

    @Test
    public void getDatabase_Success() {
        DBDatabase database = accessor.getDatabase(getMySQLDataBaseName());
        Assert.assertNotNull(database);
    }

    @Test
    public void listDatabases_Success() {
        List<DBDatabase> databases = accessor.listDatabases();
        Assert.assertTrue(databases.size() > 0);
    }

    @Test
    public void listTableColumns_TestAllColumnDataTypes_Success() {
        List<DBTableColumn> columns =
                accessor.listTableColumns(getMySQLDataBaseName(), "test_data_type");
        Assert.assertEquals(columns.size(), verifyDataTypes.size());
        for (int i = 0; i < columns.size(); i++) {
            Assert.assertEquals(verifyDataTypes.get(i).getColumnName(), columns.get(i).getName());
            Assert.assertEquals(verifyDataTypes.get(i).getTypeName(), columns.get(i).getTypeName());
            Assert.assertEquals(verifyDataTypes.get(i).getDisplayPrecision(), columns.get(i).getPrecision());
            Assert.assertEquals(verifyDataTypes.get(i).getDisplayScale(), columns.get(i).getScale());
        }
    }

    @Test
    public void listTableColumns_TestColumnAttributesOtherThanDataType_Success() {
        List<DBTableColumn> columns =
                accessor.listTableColumns(getMySQLDataBaseName(), "test_other_than_data_type");
        Assert.assertEquals(columns.size(), columnAttributes.size());
        for (int i = 0; i < columns.size(); i++) {
            Assert.assertEquals(columns.get(i).getName(), columnAttributes.get(i).getColumnName());
            Assert.assertEquals(columns.get(i).getNullable(), columnAttributes.get(i).getNullable());
            Assert.assertEquals(columns.get(i).getAutoIncrement(), columnAttributes.get(i).getAutoIncrement());
            Assert.assertEquals(columns.get(i).getVirtual(), columnAttributes.get(i).getVirtual());
            Assert.assertEquals(columns.get(i).getDefaultValue(), columnAttributes.get(i).getDefaultValue());
            Assert.assertEquals(columns.get(i).getComment(), columnAttributes.get(i).getComments());
        }
    }

    @Test
    public void listTableIndex_TestIndexType_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getMySQLDataBaseName(), "test_index_type");
        Assert.assertEquals(5, indexList.size());
        Assert.assertEquals(DBIndexAlgorithm.BTREE, indexList.get(0).getAlgorithm());
        Assert.assertEquals(DBIndexAlgorithm.BTREE, indexList.get(1).getAlgorithm());
        Assert.assertEquals(DBIndexAlgorithm.SPATIAL, indexList.get(3).getAlgorithm());
        Assert.assertEquals(DBIndexAlgorithm.FULLTEXT, indexList.get(4).getAlgorithm());
        Assert.assertEquals(DBIndexType.UNIQUE, indexList.get(0).getType());
        Assert.assertEquals(DBIndexType.UNIQUE, indexList.get(1).getType());
        Assert.assertEquals(DBIndexType.NORMAL, indexList.get(2).getType());
        Assert.assertEquals(DBIndexType.SPATIAL, indexList.get(3).getType());
        Assert.assertEquals(DBIndexType.FULLTEXT, indexList.get(4).getType());
    }

    @Test
    public void listTableIndex_TestIndexAvailable_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getMySQLDataBaseName(), "test_index_type");
        Assert.assertTrue(indexList.get(0).getAvailable());
    }

    @Test
    public void listTableIndex_get_all_index_in_schema_Success() {
        Map<String, List<DBTableIndex>> map = accessor.listTableIndexes(getMySQLDataBaseName());
        Assert.assertNotNull(map);
        Assert.assertTrue(map.size() > 0);
    }

    @Test
    public void listTableConstraint_TestForeignKey_Success() {
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getMySQLDataBaseName(), "test_fk_child");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.FOREIGN_KEY, constraintListList.get(0).getType());
        Assert.assertEquals("test_fk_parent", constraintListList.get(0).getReferenceTableName());
    }

    @Test
    public void listTableConstraint_TestPrimaryKey_Success() {
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getMySQLDataBaseName(), "test_fk_parent");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.PRIMARY_KEY, constraintListList.get(0).getType());
        Assert.assertEquals(2, constraintListList.get(0).getColumnNames().size());
    }

    @Test
    public void listSystemViews_information_schema_not_empty() {
        List<String> viewNames = accessor.showSystemViews("information_schema");
        Assert.assertTrue(!viewNames.isEmpty());
    }

    @Test
    public void listAllSystemViews_Success() {
        List<DBObjectIdentity> sysViews = accessor.listAllSystemViews();
        Assert.assertTrue(sysViews != null && sysViews.size() > 0);
    }

    @Test
    public void listSystemViews_databaseNotFound_empty() {
        List<String> viewNames = accessor.showSystemViews("databaseNotExists");
        Assert.assertTrue(viewNames.isEmpty());
    }

    @Test
    public void getPartition_Hash_Success() {
        DBTablePartition partition =
                accessor.getPartition(getMySQLDataBaseName(), "part_hash");
        Assert.assertEquals(5L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
    }

    @Test
    public void getPartition_List_Success() {
        DBTablePartition partition =
                accessor.getPartition(getMySQLDataBaseName(), "part_list");
        Assert.assertEquals(5L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partition.getPartitionOption().getType());
        Assert.assertEquals(2, partition.getPartitionDefinitions().get(0).getValuesList().size());
    }

    @Test
    public void getPartition_Range_Success() {
        DBTablePartition partition =
                accessor.getPartition(getMySQLDataBaseName(), "part_range");
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partition.getPartitionOption().getType());
        Assert.assertEquals("10", partition.getPartitionDefinitions().get(0).getMaxValues().get(0));
    }

    @Test
    public void listTablePartitions_noCandidates_listSucceed() {
        Map<String, DBTablePartition> actual = accessor.listTablePartitions(getMySQLDataBaseName(), null);
        DBTablePartition partiHash = actual.get("part_hash");
        Assert.assertEquals(5L, partiHash.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partiHash.getPartitionOption().getType());

        DBTablePartition partiList = actual.get("part_list");
        Assert.assertEquals(5L, partiList.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partiList.getPartitionOption().getType());
        Assert.assertEquals(2, partiList.getPartitionDefinitions().get(0).getValuesList().size());

        DBTablePartition partiRange = actual.get("part_range");
        Assert.assertEquals(3L, partiRange.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partiRange.getPartitionOption().getType());
        Assert.assertEquals("10", partiRange.getPartitionDefinitions().get(0).getMaxValues().get(0));
    }

    @Test
    public void listTablePartitions_candidatesExists_listSucceed() {
        Map<String, DBTablePartition> actual = accessor.listTablePartitions(getMySQLDataBaseName(),
                Arrays.asList("part_hash", "part_list"));
        DBTablePartition partiHash = actual.get("part_hash");
        Assert.assertEquals(5L, partiHash.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partiHash.getPartitionOption().getType());

        DBTablePartition partiList = actual.get("part_list");
        Assert.assertEquals(5L, partiList.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partiList.getPartitionOption().getType());
        Assert.assertEquals(2, partiList.getPartitionDefinitions().get(0).getValuesList().size());

        Assert.assertNull(actual.get("part_range"));
    }

    @Test
    public void listTableColumns_filterByTableName_Success() {
        Map<String, List<DBTableColumn>> table2Columns = accessor.listTableColumns(getMySQLDataBaseName(),
                Arrays.asList("view_test2", "part_list"));
        Assert.assertEquals(2, table2Columns.size());
    }

    @Test
    public void listTableOptions_Success() {
        Map<String, DBTableOptions> table2Options =
                accessor.listTableOptions(getMySQLDataBaseName());
        Assert.assertTrue(table2Options.containsKey("part_hash"));
    }

    @Test
    public void showTablelike_Success() {
        List<DBObjectIdentity> tables = accessor.listTables(getMySQLDataBaseName(), null);
        Assert.assertTrue(tables != null && tables.size() > 0);
    }

    @Test
    public void showViews_Success() {
        List<DBObjectIdentity> views = accessor.listViews(getMySQLDataBaseName());
        Assert.assertTrue(views != null && views.size() == 2);
    }

    @Test
    public void getView_Success() {
        DBView view = accessor.getView(getMySQLDataBaseName(), "view_test1");
        Assert.assertTrue(view != null && view.getColumns().size() == 2);
    }

    @Test
    public void showVariables_Success() {
        List<DBVariable> variables = accessor.showVariables();
        List<DBVariable> sessionVariables = accessor.showSessionVariables();
        List<DBVariable> globalVariables = accessor.showGlobalVariables();
        Assert.assertTrue(variables != null && variables.size() > 0);
        Assert.assertTrue(sessionVariables != null && sessionVariables.size() > 0);
        Assert.assertTrue(globalVariables != null && globalVariables.size() > 0);
    }

    @Test
    public void getFunction_Success() {
        DBFunction function = accessor.getFunction(getMySQLDataBaseName(), "function_test");
        Assert.assertTrue(function != null
                && function.getParams().size() == 2
                && function.getReturnType() != null);
    }

    @Test
    public void getProcedure_Success() {
        DBProcedure procedure = accessor.getProcedure(getMySQLDataBaseName(), "procedure_detail_test");
        Assert.assertTrue(procedure != null
                && procedure.getParams().size() == 2
                && procedure.getVariables().size() == 1);
    }

    @Test
    public void showTables_unknowDatabase_Success() {
        List<String> tables = accessor.showTables("abc");
        Assert.assertTrue(tables.size() == 0);
    }

    @Test
    public void getTableOptions_Success() {
        DBTableOptions options =
                accessor.getTableOptions(getMySQLDataBaseName(), "test_data_type");
        Assert.assertTrue(Objects.nonNull(options.getCharsetName()));
        Assert.assertTrue(Objects.nonNull(options.getCollationName()));
    }

    @Test
    public void getTables_success() {
        Map<String, DBTable> tables = accessor.getTables(getMySQLDataBaseName(), null);
        Assert.assertTrue(tables.size() > 0);
    }

    private static void initVerifyColumnAttributes() {
        columnAttributes.addAll(Arrays.asList(
                MySQLNoGreaterThan5740SchemaAccessorTest.ColumnAttributes.of("col1", false, false, true, null,
                        "col1_comments"),
                MySQLNoGreaterThan5740SchemaAccessorTest.ColumnAttributes.of("col2", true, false, false, null, "")));
    }

    private static void initVerifyDataTypes() {
        verifyDataTypes.addAll(Arrays.asList(
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col1", "int", 10, 10L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col2", "decimal", 10, 10L, 2, 2),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col3", "decimal", 10, 10L, 2, 2),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col4", "bit", 8, 8L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col5", "tinyint", 3, 3L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col6", "smallint", 5, 5L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col7", "mediumint", 7, 7L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col8", "bigint", 19, 19L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col9", "float", 10, 10L, 2, 2),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col10", "double", 10, 10L, 2, 2),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col11", "varchar", 10, 10L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col12", "char", 10, 10L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col13", "text", 65535, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col14", "tinytext", 255, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col15", "mediumtext", 16777215, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col16", "longtext", 50331647, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col17", "blob", 65535, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col18", "tinyblob", 255, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col19", "longblob", 50331647, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col20", "mediumblob", 16777215, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col21", "binary", 16, 16L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col22", "varbinary", 16, 16L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col23", "timestamp", 0, 0L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col24", "time", 0, 0L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col25", "date", 0, null, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col26", "datetime", 0, 0L, 0, null),
                MySQLNoGreaterThan5740SchemaAccessorTest.DataType.of("col27", "year", 0, 0L, 0, null)));
    }

    @Data
    public static class DataType {
        private String columnName;
        private String typeName;
        private Long displayPrecision;
        private Long precision;
        private Integer displayScale;
        private Integer scale;

        public static MySQLNoGreaterThan5740SchemaAccessorTest.DataType of(String columnName, String typeName,
                long precision, Long displayPrecision, int scale,
                Integer displayScale) {
            MySQLNoGreaterThan5740SchemaAccessorTest.DataType dataType =
                    new MySQLNoGreaterThan5740SchemaAccessorTest.DataType();
            dataType.setColumnName(columnName);
            dataType.setTypeName(typeName);
            dataType.setPrecision(precision);
            dataType.setScale(scale);
            dataType.setDisplayPrecision(displayPrecision);
            dataType.setDisplayScale(displayScale);
            return dataType;
        }
    }

    @Data
    private static class ColumnAttributes {
        private String columnName;
        private Boolean nullable;
        private Boolean virtual;
        private Boolean autoIncrement;
        private String defaultValue;
        private String comments;

        static MySQLNoGreaterThan5740SchemaAccessorTest.ColumnAttributes of(String columnName, boolean nullable,
                boolean virtual, boolean autoIncrement,
                String defaultValue,
                String comments) {
            MySQLNoGreaterThan5740SchemaAccessorTest.ColumnAttributes columnAttributes =
                    new MySQLNoGreaterThan5740SchemaAccessorTest.ColumnAttributes();
            columnAttributes.setColumnName(columnName);
            columnAttributes.setNullable(nullable);
            columnAttributes.setVirtual(virtual);
            columnAttributes.setAutoIncrement(autoIncrement);
            columnAttributes.setDefaultValue(defaultValue);
            columnAttributes.setComments(comments);
            return columnAttributes;
        }
    }
}
