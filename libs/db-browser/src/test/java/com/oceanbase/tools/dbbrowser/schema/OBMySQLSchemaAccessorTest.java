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
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.env.BaseTestEnv;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBUser;
import com.oceanbase.tools.dbbrowser.model.DBUserLockType;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;

import lombok.Data;

/**
 * @author jingtian
 */
public class OBMySQLSchemaAccessorTest extends BaseTestEnv {
    private final String BASE_PATH = "src/test/resources/table/obmysql/";
    private String ddl;
    private String dropTables;
    private String testProcedureDDL;
    private String testFunctionDDL;
    private List<DataType> verifyDataTypes = new ArrayList<>();
    private List<ColumnAttributes> columnAttributes = new ArrayList<>();
    private JdbcTemplate jdbcTemplate = new JdbcTemplate(getOBMySQLDataSource());

    @Before
    public void setUp() throws Exception {
        initVerifyDataTypes();
        initVerifyColumnAttributes();

        dropTables = loadAsString(BASE_PATH + "drop.sql");
        jdbcTemplate.execute(dropTables);

        ddl = loadAsString(BASE_PATH + "testTableColumnDDL.sql", BASE_PATH + "testTableIndexDDL.sql",
                BASE_PATH + "testTableConstraintDDL.sql", BASE_PATH + "testPartitionDDL.sql",
                BASE_PATH + "testViewDDL.sql");
        jdbcTemplate.execute(ddl);

        testProcedureDDL = loadAsString(BASE_PATH + "testProcedureDDL.sql");
        batchExcuteSql(testProcedureDDL);
        testFunctionDDL = loadAsString(BASE_PATH + "testFunctionDDL.sql");
        batchExcuteSql(testFunctionDDL);
    }

    @After
    public void tearDown() throws Exception {
        jdbcTemplate.execute(dropTables);
    }

    private void batchExcuteSql(String str) {
        for (String ddl : str.split("/")) {
            jdbcTemplate.execute(ddl);
        }
    }

    @Test
    public void listUsers_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBObjectIdentity> dbUsers = accessor.listUsers();
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).getType());
        Assert.assertNotNull(dbUsers.get(0).getName());
    }

    @Test
    public void listUsersDetail_SpecifyUsername_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<String> users = new ArrayList<>();
        users.add("root");
        users.add("roottest1");
        List<DBUser> dbUsers = accessor.detailUsers(users);
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).type());
        Assert.assertNotNull(dbUsers.get(0).getName());
        Optional<DBUser> dbUser = dbUsers.stream().filter(a -> a.getName().equals("root")).findFirst();
        Assert.assertTrue(dbUser.isPresent());
        Assert.assertSame(DBUserLockType.UNLOCKED, dbUser.get().getLockedStatus());

        boolean exists = dbUsers.stream().anyMatch(a -> a.getName().equals("roottest1"));
        Assert.assertFalse(exists);
    }

    @Test
    public void listUsersDetail_NoSpecifyUsername_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBUser> dbUsers = accessor.detailUsers(null);
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).type());
        Assert.assertNotNull(dbUsers.get(0).getName());
        Optional<DBUser> dbUser = dbUsers.stream().filter(a -> a.getName().equals("root")).findFirst();
        Assert.assertTrue(dbUser.isPresent());
        Assert.assertSame(DBUserLockType.UNLOCKED, dbUser.get().getLockedStatus());
    }

    @Test
    public void listBasicSchemaColumns_TestAllColumnDataTypes_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        Map<String, List<DBTableColumn>> columns = accessor.listBasicTableColumns(getOBMySQLDataBaseName());
        Assert.assertTrue(columns.containsKey("test_data_type"));
        Assert.assertEquals(verifyDataTypes.size(), columns.get("test_data_type").size());
    }

    @Test
    public void listBasicTableColumns_TestAllColumnDataTypes_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableColumn> columns =
                accessor.listBasicTableColumns(getOBMySQLDataBaseName(), "test_data_type");
        Assert.assertEquals(columns.size(), verifyDataTypes.size());
        for (int i = 0; i < columns.size(); i++) {
            Assert.assertEquals(verifyDataTypes.get(i).getColumnName(), columns.get(i).getName());
            Assert.assertEquals(verifyDataTypes.get(i).getTypeName(), columns.get(i).getTypeName());
        }
    }

    @Test
    public void listBasicViewColumns_SchemaViewColumns_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        Map<String, List<DBTableColumn>> columns = accessor.listBasicViewColumns(getOBMySQLDataBaseName());
        Assert.assertTrue(columns.containsKey("view_test1"));
        Assert.assertTrue(columns.containsKey("view_test2"));
        Assert.assertEquals(2, columns.get("view_test1").size());
        Assert.assertEquals(1, columns.get("view_test2").size());
    }

    @Test
    public void listBasicViewColumns_ViewColumns_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableColumn> columns = accessor.listBasicViewColumns(getOBMySQLDataBaseName(), "view_test1");
        Assert.assertEquals(2, columns.size());
    }

    @Test
    public void listTableColumns_TestAllColumnDataTypes_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableColumn> columns =
                accessor.listTableColumns(getOBMySQLDataBaseName(), "test_data_type");
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
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableColumn> columns =
                accessor.listTableColumns(getOBMySQLDataBaseName(), "test_other_than_data_type");
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
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOBMySQLDataBaseName(), "test_index_type");
        Assert.assertEquals(2, indexList.size());
        Assert.assertEquals(DBIndexAlgorithm.BTREE, indexList.get(0).getAlgorithm());
        Assert.assertEquals(DBIndexAlgorithm.BTREE, indexList.get(1).getAlgorithm());
    }

    @Test
    public void listTableIndex_TestIndexRange_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOBMySQLDataBaseName(), "test_index_range");
        Assert.assertEquals(2, indexList.size());
        Assert.assertEquals(true, indexList.get(0).getGlobal());
        Assert.assertEquals(false, indexList.get(1).getGlobal());
    }

    @Test
    public void listTableIndex_TestIndexAvailable_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOBMySQLDataBaseName(), "test_index_type");
        Assert.assertTrue(indexList.get(0).getAvailable());
    }

    @Test
    public void listTableIndex_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        Map<String, List<DBTableIndex>> indexes = accessor.listTableIndexes(getOBMySQLDataBaseName());
        Assert.assertTrue(indexes.size() > 0);
        Assert.assertTrue(indexes.get("test_index_type").get(0).getAvailable());
    }

    @Test
    public void listTableConstraint_TestForeignKey_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getOBMySQLDataBaseName(), "test_fk_child");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.FOREIGN_KEY, constraintListList.get(0).getType());
        Assert.assertEquals("test_fk_parent", constraintListList.get(0).getReferenceTableName());
    }

    @Test
    public void listTableConstraint_TestPrimaryKey_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getOBMySQLDataBaseName(), "test_fk_parent");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.PRIMARY_KEY, constraintListList.get(0).getType());
        Assert.assertEquals(2, constraintListList.get(0).getColumnNames().size());
    }

    @Test
    public void testParseType_MultiValues_Success() {
        String dataType = "enum('x-small','small','medium','large','x-large')";
        List<String> enums = DBSchemaAccessorUtil.parseEnumValues(dataType);
        Assert.assertEquals("x-small", enums.get(0));
        Assert.assertEquals("small", enums.get(1));
        Assert.assertEquals("medium", enums.get(2));
        Assert.assertEquals("large", enums.get(3));
        Assert.assertEquals("x-large", enums.get(4));
    }

    @Test
    public void testParseType_ContainsEmptyString_Success() {
        String dataType = "enum('x-small','')";
        List<String> enums = DBSchemaAccessorUtil.parseEnumValues(dataType);
        Assert.assertEquals("x-small", enums.get(0));
        Assert.assertEquals("", enums.get(1));
    }

    @Test
    public void listSystemViews_information_schema_not_empty() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<String> viewNames = accessor.showSystemViews("information_schema");
        Assert.assertTrue(!viewNames.isEmpty());
    }

    @Test
    public void listAllSystemViews_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBObjectIdentity> sysViews = accessor.listAllSystemViews();
        Assert.assertTrue(sysViews != null && sysViews.size() > 0);
    }

    @Test
    public void listSystemViews_databaseNotFound_empty() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<String> viewNames = accessor.showSystemViews("databaseNotExists");
        Assert.assertTrue(viewNames.isEmpty());
    }

    @Test
    public void getPartition_Hash_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBTablePartition partition =
                accessor.getPartition(getOBMySQLDataBaseName(), "part_hash");
        Assert.assertEquals(5L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
    }

    @Test
    public void getTableOptions_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBTableOptions options =
                accessor.getTableOptions(getOBMySQLDataBaseName(), "part_hash");
        Assert.assertEquals("utf8mb4", options.getCharsetName());
    }

    @Test
    public void listTableOptions_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        Map<String, DBTableOptions> table2Options =
                accessor.listTableOptions(getOBMySQLDataBaseName());
        Assert.assertTrue(table2Options.containsKey("part_hash"));
    }

    @Test
    public void showTablelike_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBObjectIdentity> tables = accessor.listTables(getOBMySQLDataBaseName(), null);
        Assert.assertTrue(tables != null && tables.size() > 0);
    }

    @Test
    public void showViews_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBObjectIdentity> views = accessor.listViews(getOBMySQLDataBaseName());
        Assert.assertTrue(views != null && views.size() == 2);
    }

    @Test
    public void getView_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBView view = accessor.getView(getOBMySQLDataBaseName(), "view_test1");
        Assert.assertTrue(view != null && view.getColumns().size() == 2);
    }

    @Test
    public void showVariables_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBVariable> variables = accessor.showVariables();
        List<DBVariable> sessionVariables = accessor.showSessionVariables();
        List<DBVariable> globalVariables = accessor.showGlobalVariables();
        Assert.assertTrue(variables != null && variables.size() > 0);
        Assert.assertTrue(sessionVariables != null && sessionVariables.size() > 0);
        Assert.assertTrue(globalVariables != null && globalVariables.size() > 0);
    }

    @Test
    public void showCharset_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<String> charset = accessor.showCharset();
        Assert.assertTrue(charset != null && charset.size() > 0);
    }

    @Test
    public void showCollation_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<String> collation = accessor.showCollation();
        Assert.assertTrue(collation != null && collation.size() > 0);
    }

    @Test
    public void listFunctions_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBPLObjectIdentity> functions = accessor.listFunctions(getOBMySQLDataBaseName());
        Assert.assertTrue(functions != null && functions.size() == 1);
    }

    @Test
    public void getFunction_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBFunction function = accessor.getFunction(getOBMySQLDataBaseName(), "function_test");
        Assert.assertTrue(function != null
                && function.getParams().size() == 2
                && function.getReturnType() != null);
    }

    @Test
    public void listProcedures_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBPLObjectIdentity> procedures = accessor.listProcedures(getOBMySQLDataBaseName());
        Assert.assertTrue(!procedures.isEmpty());
    }

    @Test
    public void getProcedure_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBProcedure procedure = accessor.getProcedure(getOBMySQLDataBaseName(), "procedure_detail_test");
        Assert.assertTrue(procedure != null
                && procedure.getParams().size() == 2
                && procedure.getVariables().size() == 1);
    }

    @Test
    public void getProcedure_without_parameters_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBProcedure procedure = accessor.getProcedure(getOBMySQLDataBaseName(), "procedure_without_parameters");
        Assert.assertTrue(procedure != null
                && procedure.getParams().size() == 0
                && procedure.getVariables().size() == 0);
    }

    @Test
    public void getDatabase_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBDatabase database = accessor.getDatabase(getOBMySQLDataBaseName());
        Assert.assertNotNull(database);
    }

    @Test
    public void listDatabases_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBDatabase> databases = accessor.listDatabases();
        Assert.assertTrue(databases.size() > 0);
    }

    @Test
    public void getPartition_List_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBTablePartition partition =
                accessor.getPartition(getOBMySQLDataBaseName(), "part_list");
        Assert.assertEquals(5L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partition.getPartitionOption().getType());
        Assert.assertEquals(2, partition.getPartitionDefinitions().get(0).getValuesList().size());
    }

    @Test
    public void getPartition_Range_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        DBTablePartition partition =
                accessor.getPartition(getOBMySQLDataBaseName(), "part_range");
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partition.getPartitionOption().getType());
        Assert.assertEquals("10", partition.getPartitionDefinitions().get(0).getMaxValues().get(0));
    }

    @Test
    public void listTableColumns_test_in_mysql_schema_view_as_base_table_Success() {
        DBSchemaAccessor accessor = new DBSchemaAccessors(getOBMySQLDataSource()).createOBMysql();
        List<DBTableColumn> columns = accessor.listTableColumns("mysql", "time_zone_transition");
        Assert.assertEquals(3, columns.size());
    }

    private void initVerifyColumnAttributes() {
        columnAttributes.addAll(Arrays.asList(
                ColumnAttributes.of("col1", false, false, true, null, "col1_comments"),
                ColumnAttributes.of("col2", true, false, false, null, "")));
    }

    private void initVerifyDataTypes() {
        verifyDataTypes.addAll(Arrays.asList(
                DataType.of("col1", "int", 11, 11L, 0, null),
                DataType.of("col2", "decimal", 10, 10L, 2, 2),
                DataType.of("col3", "decimal", 10, 10L, 2, 2),
                DataType.of("col4", "bit", 8, 8L, 0, null),
                DataType.of("col5", "tinyint", 4, 4L, 0, null),
                DataType.of("col6", "smallint", 6, 6L, 0, null),
                DataType.of("col7", "mediumint", 9, 9L, 0, null),
                DataType.of("col8", "bigint", 20, 20L, 0, null),
                DataType.of("col9", "float", 10, 10L, 2, 2),
                DataType.of("col10", "double", 10, 10L, 2, 2),
                DataType.of("col11", "varchar", 10, 10L, 0, null),
                DataType.of("col12", "char", 10, 10L, 0, null),
                DataType.of("col13", "text", 65535, null, 0, null),
                DataType.of("col14", "tinytext", 255, null, 0, null),
                DataType.of("col15", "mediumtext", 16777215, null, 0, null),
                DataType.of("col16", "longtext", 50331647, null, 0, null),
                DataType.of("col17", "blob", 65535, null, 0, null),
                DataType.of("col18", "tinyblob", 255, null, 0, null),
                DataType.of("col19", "longblob", 50331647, null, 0, null),
                DataType.of("col20", "mediumblob", 16777215, null, 0, null),
                DataType.of("col21", "binary", 16, 16L, 0, null),
                DataType.of("col22", "varbinary", 16, 16L, 0, null),
                DataType.of("col23", "timestamp", 0, 0L, 0, null),
                DataType.of("col24", "time", 0, 0L, 0, null),
                DataType.of("col25", "date", 0, null, 0, null),
                DataType.of("col26", "datetime", 0, 0L, 0, null),
                DataType.of("col27", "year", 0, 0L, 0, null)));
    }

    @Data
    public static class DataType {
        private String columnName;
        private String typeName;
        private Long displayPrecision;
        private Long precision;
        private Integer displayScale;
        private Integer scale;

        public static DataType of(String columnName, String typeName, long precision, Long displayPrecision, int scale,
                Integer displayScale) {
            DataType dataType = new DataType();
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

        static ColumnAttributes of(String columnName, boolean nullable, boolean virtual, boolean autoIncrement,
                String defaultValue,
                String comments) {
            ColumnAttributes columnAttributes = new ColumnAttributes();
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
