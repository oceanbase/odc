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
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.env.BaseTestEnv;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.model.DBTypeCode;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.schema.OBMySQLSchemaAccessorTest.DataType;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.Data;

/**
 * @author jingtian
 */
public class OBOracleSchemaAccessorTest extends BaseTestEnv {
    private static final String BASE_PATH = "src/test/resources/table/oboracle/";
    private static String ddl;
    private static String dropTables;
    private static String testFunctionDDL;
    private static String testPackageDDL;
    private static String testProcedureDDL;
    private static String testTriggerDDL;
    private static List<DataType> verifyDataTypes = new ArrayList<>();
    private static List<ColumnAttributes> columnAttributes = new ArrayList<>();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(getOBOracleDataSource());
    private static final String typeName = "emp_type_" + new Random().nextInt(10000);
    private static final DBSchemaAccessor accessor = new DBSchemaAccessors(getOBOracleDataSource()).createOBOracle();


    @BeforeClass
    public static void before() throws Exception {
        initVerifyDataTypes();
        initVerifyColumnAttributes();

        ddl = loadAsString(BASE_PATH + "testTableColumnDDL.sql", BASE_PATH + "testTableIndexDDL.sql",
                BASE_PATH + "testTableConstraintDDL.sql", BASE_PATH + "testPartitionDDL.sql",
                BASE_PATH + "testViewDDL.sql",
                BASE_PATH + "testSequenceDDL.sql",
                BASE_PATH + "testSynonymDDL.sql");
        dropTables = loadAsString(BASE_PATH + "drop.sql");

        batchExcuteSql(dropTables);

        jdbcTemplate.execute(ddl);
        testFunctionDDL = loadAsString(BASE_PATH + "testFunctionDDL.sql");
        batchExcuteSql(testFunctionDDL);
        testPackageDDL = loadAsString(BASE_PATH + "testPackageDDL.sql");
        batchExcuteSql(testPackageDDL);
        testProcedureDDL = loadAsString(BASE_PATH + "testProcedureDDL.sql");
        batchExcuteSql(testProcedureDDL);
        testTriggerDDL = loadAsString(BASE_PATH + "testTriggerDDL.sql");
        batchExcuteSql(testTriggerDDL);
    }

    @AfterClass
    public static void after() throws Exception {
        batchExcuteSql(dropTables);
    }

    private static void batchExcuteSql(String str) {
        for (String ddl : str.split("/")) {
            jdbcTemplate.execute(ddl);
        }
    }

    @Test
    public void listUsers_Success() {
        List<DBObjectIdentity> dbUsers = accessor.listUsers();
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).getType());
        Assert.assertNotNull(dbUsers.get(0).getName());
    }

    @Test
    public void listDatabases_Success() {
        List<DBDatabase> databases = accessor.listDatabases();
        Assert.assertTrue(databases.size() > 0);
    }

    @Test
    public void listBasicSchemaColumns_TestAllColumnDataTypes_Success() {
        Map<String, List<DBTableColumn>> columns = accessor.listBasicTableColumns(getOBOracleSchema());
        Assert.assertTrue(columns.containsKey("TEST_DATA_TYPE"));
        Assert.assertEquals(verifyDataTypes.size(), columns.get("TEST_DATA_TYPE").size());
    }

    @Test
    public void listBasicTableColumns_TestAllColumnDataTypes_Success() {
        List<DBTableColumn> columns =
                accessor.listBasicTableColumns(getOBOracleSchema(), "TEST_DATA_TYPE");
        Assert.assertEquals(columns.size(), verifyDataTypes.size());
        for (int i = 0; i < columns.size(); i++) {
            Assert.assertEquals(verifyDataTypes.get(i).getColumnName(), columns.get(i).getName());
            Assert.assertEquals(verifyDataTypes.get(i).getTypeName(), columns.get(i).getTypeName());
        }
    }

    @Test
    public void listBasicViewColumns_SchemaViewColumns_Success() {
        Map<String, List<DBTableColumn>> columns = accessor.listBasicViewColumns(getOBOracleSchema());
        Assert.assertTrue(columns.containsKey("VIEW_TEST1"));
        Assert.assertTrue(columns.containsKey("VIEW_TEST2"));
        Assert.assertEquals(2, columns.get("VIEW_TEST1").size());
        Assert.assertEquals(1, columns.get("VIEW_TEST2").size());
    }

    @Test
    public void listBasicViewColumns_ViewColumns_Success() {
        List<DBTableColumn> columns = accessor.listBasicViewColumns(getOBOracleSchema(), "VIEW_TEST1");
        Assert.assertEquals(2, columns.size());
    }

    @Test
    @Ignore("TODO: fix this test")
    public void listTableColumns_TestAllColumnDataTypes_Success() {
        List<DBTableColumn> columns =
                accessor.listTableColumns(getOBOracleSchema(), "TEST_DATA_TYPE");
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
                accessor.listTableColumns(getOBOracleSchema(), "TEST_OTHER_THAN_DATA_TYPE");
        Assert.assertEquals(columns.size(), columnAttributes.size());
        for (int i = 0; i < columns.size(); i++) {
            Assert.assertEquals(columns.get(i).getName(), columnAttributes.get(i).getColumnName());
            Assert.assertEquals(columns.get(i).getNullable(), columnAttributes.get(i).getNullable());
            Assert.assertEquals(columns.get(i).getVirtual(), columnAttributes.get(i).getVirtual());
            Assert.assertEquals(columns.get(i).getDefaultValue(), columnAttributes.get(i).getDefaultValue());
            Assert.assertEquals(columns.get(i).getComment(), columnAttributes.get(i).getComments());
        }
    }

    @Test
    public void listTableColumns_TestGetAllColumnInSchema_Success() {
        Map<String, List<DBTableColumn>> table2Columns = accessor.listTableColumns(getOBOracleSchema());
        Assert.assertTrue(table2Columns.size() > 0);
    }

    @Test
    public void listTableIndex_TestIndexType_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOBOracleSchema(), "TEST_INDEX_TYPE");
        Assert.assertEquals(2, indexList.size());
        Assert.assertEquals(DBIndexType.UNIQUE, indexList.get(0).getType());
        Assert.assertEquals(DBIndexType.UNIQUE, indexList.get(1).getType());
        Assert.assertEquals(DBIndexAlgorithm.BTREE, indexList.get(0).getAlgorithm());
        Assert.assertEquals(DBIndexAlgorithm.BTREE, indexList.get(1).getAlgorithm());
    }

    @Test
    public void listTableIndex_TestIndexRange_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOBOracleSchema(), "TEST_INDEX_RANGE");
        Assert.assertEquals(2, indexList.size());
        Assert.assertEquals(true, indexList.get(0).getGlobal());
        Assert.assertEquals(false, indexList.get(1).getGlobal());
    }

    @Test
    public void listTableIndex_TestIndexAvailable_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOBOracleSchema(), "TEST_INDEX_RANGE");
        Assert.assertEquals(2, indexList.size());
        Assert.assertTrue(indexList.get(0).getAvailable());
        Assert.assertTrue(indexList.get(1).getAvailable());
    }

    @Test
    public void listTableConstraint_TestForeignKey_Success() {
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getOBOracleSchema(), "TEST_FK_CHILD");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.FOREIGN_KEY, constraintListList.get(0).getType());
        Assert.assertEquals("TEST_FK_PARENT", constraintListList.get(0).getReferenceTableName());
        Assert.assertEquals("TEST_FK_CHILD", constraintListList.get(0).getTableName());
        Assert.assertEquals(getOBOracleSchema(), constraintListList.get(0).getSchemaName());
    }

    @Test
    public void listTableConstraint_TestPrimaryKey_Success() {
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getOBOracleSchema(), "TEST_FK_PARENT");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.PRIMARY_KEY, constraintListList.get(0).getType());
        Assert.assertEquals(2, constraintListList.get(0).getColumnNames().size());
        Assert.assertNotNull("TEST_FK_PARENT", constraintListList.get(0).getTableName());
        Assert.assertNotNull(getOBOracleSchema(), constraintListList.get(0).getSchemaName());
    }

    @Test
    public void listTableConstraint_TestPrimaryKeyIndex_Success() {
        List<DBTableIndex> indexes =
                accessor.listTableIndexes(getOBOracleSchema(), "TEST_PK_INDEX");
        Assert.assertEquals(1, indexes.size());
        Assert.assertTrue(indexes.get(0).getGlobal());
    }

    @Test
    public void getPartition_Hash_Success() {
        DBTablePartition partition =
                accessor.getPartition(getOBOracleSchema(), "part_hash");
        Assert.assertEquals(5L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
    }

    @Test
    public void getTableOptions_Success() {
        DBTableOptions options =
                accessor.getTableOptions(getOBOracleSchema(), "part_hash");
        Assert.assertEquals("this is a comment", options.getComment());
    }

    @Test
    public void listSystemViews_databaseNotSYS_empty() {
        List<String> viewNames = accessor.showSystemViews("notsys");
        Assert.assertTrue(viewNames.isEmpty());
    }

    @Test
    public void listSystemViews_databaseSYS_notEmpty() {
        List<String> viewNames = accessor.showSystemViews("SYS");
        Assert.assertTrue(!viewNames.isEmpty());
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
    public void showCharset_Success() {
        List<String> charset = accessor.showCharset();
        Assert.assertTrue(charset != null && charset.size() > 0);
    }

    @Test
    public void showCollation_Success() {
        List<String> collation = accessor.showCollation();
        Assert.assertTrue(collation != null && collation.size() > 0);
    }

    @Test
    public void showViews_Success() {
        List<DBObjectIdentity> views = accessor.listViews(getOBOracleSchema());
        Assert.assertTrue(views != null && views.size() == 2);
    }

    @Test
    public void listAllViews_Success() {
        List<DBObjectIdentity> views = accessor.listAllViews("ALL");
        Assert.assertTrue(views.size() > 0);
    }

    @Test
    public void getView_Success() {
        DBView view = accessor.getView(getOBOracleSchema(), "VIEW_TEST1");
        Assert.assertTrue(view != null && view.getColumns().size() == 2);
    }

    @Test
    public void listFunctions_Success() {
        List<DBPLObjectIdentity> functions = accessor.listFunctions(getOBOracleSchema());
        Assert.assertTrue(functions != null && functions.size() == 3);
    }

    @Test
    public void listFunctions_invalidFunctionList() {
        List<DBPLObjectIdentity> functions = accessor.listFunctions(getOBOracleSchema());
        functions = functions.stream().filter(function -> {
            if (StringUtils.equals(function.getName(), "INVALIDE_FUNC")) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        boolean flag = false;
        if (StringUtils.containsIgnoreCase(functions.get(0).getErrorMessage(), "ORA")) {
            flag = true;
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getFunction_Success() {
        DBFunction function = accessor.getFunction(getOBOracleSchema(), "FUNC_DETAIL_TEST");
        Assert.assertTrue(function != null
                && function.getParams().size() == 1
                && function.getVariables().size() == 2
                && function.getTypes() != null
                && function.getReturnType() != null);
    }

    @Test
    public void showProcedures_Success() {
        List<DBPLObjectIdentity> procedures = accessor.listProcedures(getOBOracleSchema());
        Assert.assertTrue(procedures != null && procedures.size() >= 3);
    }

    @Test
    public void showProcedures_invalidProcedureList() {
        List<DBPLObjectIdentity> procedures = accessor.listProcedures(getOBOracleSchema());
        procedures = procedures.stream().filter(procedure -> {
            if (StringUtils.equals(procedure.getName(), "INVALID_PROCEDURE_TEST")) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        boolean flag = false;
        if (StringUtils.containsIgnoreCase(procedures.get(0).getErrorMessage(), "ORA")) {
            flag = true;
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getProcedure_Success() {
        DBProcedure procedure = accessor.getProcedure(getOBOracleSchema(), "PROCEDURE_DETAIL_TEST");
        Assert.assertTrue(procedure != null
                && procedure.getParams().size() == 2
                && procedure.getVariables().size() == 1);
    }

    @Test
    public void listPackages_Success() {
        List<DBPLObjectIdentity> packages = accessor.listPackages(getOBOracleSchema());
        Assert.assertTrue(packages != null && !packages.isEmpty());
    }

    @Test
    public void listPackages_invalidPackageList() {
        List<DBPLObjectIdentity> packages = accessor.listPackages(getOBOracleSchema());
        boolean flag = false;
        for (DBPLObjectIdentity dbPackage : packages) {
            if (StringUtils.containsIgnoreCase(dbPackage.getErrorMessage(), "PLS")) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getPackage_Success() {
        DBPackage dbPackage = accessor.getPackage(getOBOracleSchema(), "T_PACKAGE");
        Assert.assertTrue(dbPackage != null
                && dbPackage.getPackageHead().getVariables().size() == 1
                && dbPackage.getPackageHead().getTypes().size() == 1
                && dbPackage.getPackageHead().getFunctions().size() == 1
                && dbPackage.getPackageHead().getProcedures().size() == 1);

        Assert.assertTrue(dbPackage.getPackageBody().getVariables().size() == 1
                && dbPackage.getPackageBody().getFunctions().size() == 1
                && dbPackage.getPackageBody().getProcedures().size() == 1);
    }

    @Test
    public void listriggers_Success() {
        List<DBPLObjectIdentity> triggers = accessor.listTriggers(getOBOracleSchema());
        Assert.assertNotEquals(null, triggers);
        boolean flag = false;
        for (DBPLObjectIdentity trigger : triggers) {
            if ("TRIGGER_TEST".equals(trigger.getName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void listTriggers_InvalidTriggerList() {
        List<DBPLObjectIdentity> triggers = accessor.listTriggers(getOBOracleSchema());
        boolean flag = false;
        for (DBPLObjectIdentity trigger : triggers) {
            if (StringUtils.containsIgnoreCase(trigger.getErrorMessage(), "ORA")) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getTrigger_Success() {
        DBTrigger trigger = accessor.getTrigger(getOBOracleSchema(), "TRIGGER_TEST");
        Assert.assertNotNull(trigger);
        Assert.assertEquals("TRIGGER_TEST", trigger.getTriggerName());
    }

    @Test
    public void listTypes_Success() {
        String createTypeDdl = String.format("CREATE TYPE \"%s\" AS OBJECT (name VARCHAR2(100), ssn NUMBER)", typeName);
        jdbcTemplate.execute(createTypeDdl);

        List<DBPLObjectIdentity> types = accessor.listTypes(getOBOracleSchema());
        Assert.assertNotNull(types);
        Assert.assertEquals(1, types.size());
        Assert.assertEquals(typeName, types.get(0).getName());

        jdbcTemplate.execute(String.format("drop type \"%s\"", typeName));
    }

    @Test
    public void getType_testTypeInfoForOracleObject() {
        String createTypeDdl = String.format("CREATE TYPE \"%s\" AS OBJECT (name VARCHAR2(100), ssn NUMBER)", typeName);
        jdbcTemplate.execute(createTypeDdl);

        DBType type = accessor.getType(getOBOracleSchema(), typeName);
        Assert.assertNotNull(type);
        Assert.assertEquals(typeName, type.getTypeName());
        Assert.assertEquals(DBTypeCode.OBJECT.name(), type.getType());

        jdbcTemplate.execute(String.format("drop type \"%s\"", typeName));
    }

    @Test
    public void getType_testTypeInfoForOracleVarray() {
        String createTypeDdl = String.format("CREATE TYPE \"%s\" AS VARRAY(5) OF VARCHAR2(25);", typeName);
        jdbcTemplate.execute(createTypeDdl);

        DBType type = accessor.getType(getOBOracleSchema(), typeName);
        Assert.assertNotNull(type);
        Assert.assertEquals(typeName, type.getTypeName());
        Assert.assertEquals(DBTypeCode.VARRAY.name(), type.getType());

        jdbcTemplate.execute(String.format("drop type \"%s\"", typeName));
    }

    @Test
    public void getType_testTypeInfoForOracleTable() {
        String preDdlName = "cust_address_typ2";
        String preDdl = String.format("CREATE TYPE \"%s\" AS OBJECT\n"
                + "       ( street_address     VARCHAR2(40)\n"
                + "       , postal_code        VARCHAR2(10)\n"
                + "       , city               VARCHAR2(30)\n"
                + "       , state_province     VARCHAR2(10)\n"
                + "       , country_id         CHAR(2)\n"
                + "       );", preDdlName);
        jdbcTemplate.execute(preDdl);
        String ddl = String.format("CREATE TYPE \"%s\" AS TABLE OF \"cust_address_typ2\";", typeName);
        jdbcTemplate.execute(ddl);

        DBType type = accessor.getType(getOBOracleSchema(), typeName);
        Assert.assertNotNull(type);
        Assert.assertEquals(typeName, type.getTypeName());
        Assert.assertEquals(DBTypeCode.TABLE.name(), type.getType());

        jdbcTemplate.execute(String.format("drop type \"%s\"", typeName));
        jdbcTemplate.execute(String.format("drop type \"%s\"", preDdlName));
    }

    @Test
    public void listSynonyms_testPublicSynonymListForOracle() {
        List<DBObjectIdentity> synonyms = accessor.listSynonyms(getOBOracleSchema(), DBSynonymType.PUBLIC);
        Assert.assertNotEquals(0, synonyms.size());
        boolean flag = false;
        for (DBObjectIdentity synonym : synonyms) {
            if ("PUBLIC_SYNONYM_TEST".equals(synonym.getName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void listSynonyms_testCommonSynonymListForOracle() {
        List<DBObjectIdentity> synonyms = accessor.listSynonyms(getOBOracleSchema(), DBSynonymType.COMMON);
        Assert.assertNotEquals(0, synonyms.size());
        boolean flag = false;
        for (DBObjectIdentity synonym : synonyms) {
            if ("COMMON_SYNONYM_TEST".equals(synonym.getName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getSynonym_testPublicSynonymInfoForOracle() {
        DBSynonym synonym = accessor.getSynonym(getOBOracleSchema(), "PUBLIC_SYNONYM_TEST", DBSynonymType.PUBLIC);
        Assert.assertNotNull(synonym);
        Assert.assertEquals(DBSynonymType.PUBLIC, synonym.getSynonymType());
        Assert.assertEquals("PUBLIC_SYNONYM_TEST", synonym.getSynonymName());
    }

    @Test
    public void getSynonym_testCommonSynonymInfoForOracle() {
        DBSynonym synonym = accessor.getSynonym(getOBOracleSchema(), "COMMON_SYNONYM_TEST", DBSynonymType.COMMON);
        Assert.assertNotNull(synonym);
        Assert.assertEquals(DBSynonymType.COMMON, synonym.getSynonymType());
        Assert.assertEquals("COMMON_SYNONYM_TEST", synonym.getSynonymName());
    }

    @Test
    public void getDatabase_Success() {
        DBDatabase database = accessor.getDatabase(getOBOracleSchema());
        Assert.assertNotNull(database);
    }

    @Test
    public void listSequences_Success() {
        List<DBObjectIdentity> sequences = accessor.listSequences(getOBOracleSchema());
        Assert.assertTrue(sequences != null && !sequences.isEmpty());
    }

    @Test
    public void getSequence_Success() {
        DBSequence sequence = accessor.getSequence(getOBOracleSchema(), "SEQ_TEST");
        Assert.assertTrue(sequence != null && sequence.getName().equalsIgnoreCase("SEQ_TEST"));
        Assert.assertEquals("CREATE SEQUENCE \"SEQ_TEST\" MINVALUE 1 MAXVALUE 10 INCREMENT BY 2 CACHE 5 ORDER CYCLE;",
                sequence.getDdl());
    }

    @Test
    public void listTables_Success() {
        List<DBObjectIdentity> tables =
                accessor.listTables(getOBOracleSchema(), null);
        Assert.assertTrue(tables.size() > 0);
        tables.forEach(table -> Assert.assertTrue(!table.getName().startsWith("__idx_")));
    }

    @Test
    public void listTableColumns_test_default_null_Success() {
        List<DBTableColumn> columns =
                accessor.listTableColumns(getOBOracleSchema(), "TEST_DEFAULT_NULL");
        Assert.assertTrue(columns.size() > 0);
        Assert.assertNull(columns.get(0).getDefaultValue());
        Assert.assertNull(columns.get(1).getDefaultValue());
        Assert.assertNull(columns.get(2).getDefaultValue());
        Assert.assertEquals("'null'", columns.get(3).getDefaultValue());
    }

    private static void initVerifyColumnAttributes() {
        columnAttributes.addAll(Arrays.asList(
                ColumnAttributes.of("COL1", false, false, null, "col1_comments"),
                ColumnAttributes.of("COL2", true, false, null, null),
                ColumnAttributes.of("COL3", true, true, "(\"COL1\" + \"COL2\")", null)));
    }

    private static void initVerifyDataTypes() {
        verifyDataTypes.addAll(Arrays.asList(
                DataType.of("COL1", "NUMBER", 38, 38L, 0, 0),
                DataType.of("COL2", "NUMBER", 0, 22L, 0, 0),
                DataType.of("COL3", "CHAR", 10, 10L, 0, null),
                DataType.of("COL4", "VARCHAR2", 10, 10L, 0, null),
                DataType.of("COL5", "BLOB", 50331648, null, 0, null),
                DataType.of("COL6", "CLOB", 50331648, null, 0, null),
                DataType.of("COL7", "DATE", 0, null, 0, null),
                DataType.of("COL8", "TIMESTAMP", 0, null, 6, null),
                DataType.of("COL9", "TIMESTAMP WITH TIME ZONE", 0, null, 6, null),
                DataType.of("COL10", "TIMESTAMP WITH LOCAL TIME ZONE", 0, null, 6, null),
                DataType.of("COL11", "RAW", 100, 100L, -1, null),
                DataType.of("COL12", "INTERVAL YEAR TO MONTH", 2, null, 0, null),
                DataType.of("COL13", "INTERVAL DAY TO SECOND", 2, null, 6, null),
                DataType.of("COL14", "NUMBER", 2, null, 6, null)));
    }

    @Data
    private static class ColumnAttributes {
        private String columnName;
        private Boolean nullable;
        private Boolean virtual;
        private String defaultValue;
        private String comments;

        static ColumnAttributes of(String columnName, boolean nullable, boolean virtual,
                String defaultValue,
                String comments) {
            ColumnAttributes columnAttributes = new ColumnAttributes();
            columnAttributes.setColumnName(columnName);
            columnAttributes.setNullable(nullable);
            columnAttributes.setVirtual(virtual);
            columnAttributes.setDefaultValue(defaultValue);
            columnAttributes.setComments(comments);
            return columnAttributes;
        }
    }

}
