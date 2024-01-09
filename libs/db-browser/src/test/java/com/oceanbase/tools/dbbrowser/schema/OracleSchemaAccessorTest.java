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

import java.util.List;
import java.util.Map;
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
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @author jingtian
 * @date 2023/10/9
 * @since ODC_release_4.2.4
 */
public class OracleSchemaAccessorTest extends BaseTestEnv {
    private static final String BASE_PATH = "src/test/resources/table/oracle/";
    private static String testTableDDL;
    private static String drop;
    private static String testFunctionDDL;
    private static String testPackageDDL;
    private static String testProcedureDDL;
    private static String testTypeDDL;
    private static String testSequenceDDL;
    private static String testSynonymDDL;
    private static String testTriggerDDL;
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(getOracleDataSource());
    private static DBSchemaAccessor accessor = new DBSchemaAccessors(getOracleDataSource()).createOracle();

    @BeforeClass
    public static void before() throws Exception {
        jdbcTemplate.execute("alter session set current_schema=\"" + getOracleSchema() + "\"");
        drop = loadAsString(BASE_PATH + "drop.sql");
        batchExcuteSql(drop);
        testFunctionDDL = loadAsString(BASE_PATH + "testFunctionDDL.sql");
        batchExcuteSql(testFunctionDDL);
        testProcedureDDL = loadAsString(BASE_PATH + "testProcedureDDL.sql");
        batchExcuteSql(testProcedureDDL);
        testPackageDDL = loadAsString(BASE_PATH + "testPackageDDL.sql");
        batchExcuteSql(testPackageDDL);
        testTriggerDDL = loadAsString(BASE_PATH + "testTriggerDDL.sql");
        batchExcuteSql(testTriggerDDL);
        testTypeDDL = loadAsString(BASE_PATH + "testTypeDDL.sql");
        batchExcuteSql(testTypeDDL);
        testSequenceDDL = loadAsString(BASE_PATH + "testSequenceDDL.sql");
        batchExcuteSql(testSequenceDDL);
        testSynonymDDL = loadAsString(BASE_PATH + "testSynonymDDL.sql");
        batchExcuteSql(testSynonymDDL);
        testTableDDL = loadAsString(BASE_PATH + "testTableColumnDDL.sql", BASE_PATH + "testTableConstraintDDL.sql",
                BASE_PATH + "testTableIndexDDL.sql", BASE_PATH + "testViewDDL.sql",
                BASE_PATH + "testTablePartitionDDL.sql");
        batchExcuteSql(testTableDDL);
    }

    @AfterClass
    public static void after() throws Exception {
        batchExcuteSql(drop);
    }

    private static void batchExcuteSql(String str) {
        for (String ddl : str.split("/")) {
            try {
                jdbcTemplate.execute(ddl);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    @Test
    public void listTableColumns_TestGetAllColumnInSchema_Success() {
        Map<String, List<DBTableColumn>> table2Columns = accessor.listTableColumns(getOracleSchema());
        Assert.assertTrue(table2Columns.size() > 0);
    }

    @Test
    public void listTableColumns_Success() {
        List<DBTableColumn> columns =
                accessor.listTableColumns(getOracleSchema(), "TEST_COL_DATA_TYPE");
        Assert.assertEquals(13, columns.size());
    }

    @Test
    public void listTableConstraint_TestPrimaryKey_Success() {
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getOracleSchema(), "TEST_FK_PARENT");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.PRIMARY_KEY, constraintListList.get(0).getType());
        Assert.assertEquals(2, constraintListList.get(0).getColumnNames().size());
        Assert.assertEquals("TEST_FK_PARENT", constraintListList.get(0).getTableName());
        Assert.assertEquals(getOracleSchema(), constraintListList.get(0).getSchemaName());
    }

    @Test
    public void listTableConstraint_TestForeignKey_Success() {
        List<DBTableConstraint> constraintListList =
                accessor.listTableConstraints(getOracleSchema(), "TEST_FK_CHILD");
        Assert.assertEquals(1, constraintListList.size());
        Assert.assertEquals(DBConstraintType.FOREIGN_KEY, constraintListList.get(0).getType());
        Assert.assertEquals("TEST_FK_PARENT", constraintListList.get(0).getReferenceTableName());
        Assert.assertEquals("TEST_FK_CHILD", constraintListList.get(0).getTableName());
        Assert.assertEquals(getOracleSchema(), constraintListList.get(0).getSchemaName());
    }

    @Test
    public void listTableIndexes_TestPrimaryKeyIndex_Success() {
        List<DBTableIndex> indexes =
                accessor.listTableIndexes(getOracleSchema(), "TEST_PK_INDEX");
        Assert.assertEquals(1, indexes.size());
        Assert.assertEquals(DBIndexType.UNIQUE, indexes.get(0).getType());
    }

    @Test
    public void listTableIndex_TestIndexType_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOracleSchema(), "TEST_INDEX_TYPE");
        Assert.assertEquals(2, indexList.size());
        Assert.assertEquals(DBIndexType.UNIQUE, indexList.get(1).getType());
        Assert.assertEquals(DBIndexType.NORMAL, indexList.get(0).getType());
    }

    @Test
    public void listTableIndex_TestIndexRange_Success() {
        List<DBTableIndex> indexList = accessor.listTableIndexes(getOracleSchema(), "TEST_INDEX_RANGE");
        Assert.assertEquals(3, indexList.size());
        indexList.stream().forEach(idx -> {
            if ("test_global_idx".equalsIgnoreCase(idx.getName())) {
                Assert.assertTrue(idx.getGlobal());
            } else {
                Assert.assertFalse(idx.getGlobal());
            }
        });
    }

    @Test
    public void getPartition_Hash_Success() {
        DBTablePartition partition =
                accessor.getPartition(getOracleSchema(), "PART_HASH_TEST");
        Assert.assertEquals(5L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
    }

    @Test
    public void getView_Success() {
        DBView view = accessor.getView(getOracleSchema(), "VIEW_TEST");
        Assert.assertTrue(view != null && view.getColumns().size() == 2);
    }

    @Test
    public void getDatabase_Success() {
        DBDatabase database = accessor.getDatabase(getOracleSchema());
        Assert.assertNotNull(database);
        Assert.assertNotNull(database.getId());
        Assert.assertNotNull(database.getCharset());
        Assert.assertNotNull(database.getCollation());
        Assert.assertEquals(getOracleSchema(), database.getName());
    }

    @Test
    public void listDatabases_Success() {
        List<DBDatabase> databases = accessor.listDatabases();
        Assert.assertTrue(databases.size() > 0);
    }

    @Test
    public void switchDatabase_Success() {
        accessor.switchDatabase("SYS");
        Assert.assertEquals("SYS", jdbcTemplate.queryForObject(
                "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS CURRENT_SCHEMA FROM DUAL", String.class));
        accessor.switchDatabase(getOracleSchema());
        Assert.assertEquals(getOracleSchema(), jdbcTemplate.queryForObject(
                "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS CURRENT_SCHEMA FROM DUAL", String.class));
    }

    @Test
    public void listUsers_Success() {
        List<DBObjectIdentity> dbUsers = accessor.listUsers();
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).getType());
        Assert.assertNotNull(dbUsers.get(0).getName());
    }

    @Test
    public void showTablesLike_Success() {
        List<String> tables = accessor.showTablesLike(getOracleSchema(), "");
        Assert.assertTrue(tables.size() > 0);
    }

    @Test
    public void listTables_Success() {
        List<DBObjectIdentity> tables =
                accessor.listTables(getOracleSchema(), null);
        Assert.assertTrue(tables.size() > 0);
    }

    @Test
    public void listViews_Success() {
        List<DBObjectIdentity> views = accessor.listViews("SYS");
        Assert.assertTrue(views.size() > 0);
    }

    @Test
    public void listAllViews_Success() {
        List<DBObjectIdentity> views = accessor.listAllViews("ALL");
        Assert.assertTrue(views.size() > 0);
    }

    @Test
    public void listAllUserViews_Success() {
        List<DBObjectIdentity> views = accessor.listAllUserViews();
        Assert.assertTrue(views.size() > 0);
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
    public void listFunctions_Success() {
        List<DBPLObjectIdentity> functions = accessor.listFunctions(getOracleSchema());
        Assert.assertTrue(functions != null && functions.size() == 4);
    }

    @Test
    public void listFunctions_invalidFunctionList() {
        List<DBPLObjectIdentity> functions = accessor.listFunctions(getOracleSchema());
        functions = functions.stream().filter(function -> {
            if (StringUtils.equals(function.getName(), "FUNC_INVALIDE_ACCESSOR")) {
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
        DBFunction function = accessor.getFunction(getOracleSchema(), "FUNC_DETAIL_ACCESSOR");
        Assert.assertTrue(function != null
                && function.getParams().size() == 3
                && function.getVariables().size() == 1
                && function.getTypes() != null
                && function.getReturnType() != null);
        function = accessor.getFunction(getOracleSchema(), "FUNC_NO_PARAM_ACCESSOR");
        Assert.assertTrue(function != null
                && function.getParams().size() == 0
                && function.getVariables().size() == 1
                && function.getReturnType() != null);
    }

    @Test
    public void listProcedures_Success() {
        List<DBPLObjectIdentity> procedures = accessor.listProcedures(getOracleSchema());
        Assert.assertTrue(procedures.size() > 0);
    }

    @Test
    public void listProcedures_invalidProcedureList() {
        List<DBPLObjectIdentity> procedures = accessor.listProcedures(getOracleSchema());
        procedures = procedures.stream().filter(procedure -> {
            if (StringUtils.equals(procedure.getName(), "PROC_INVALID_ACCESSOR")) {
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
        DBProcedure procedure = accessor.getProcedure(getOracleSchema(), "PROC_PARAMS_ACCESSOR");
        Assert.assertTrue(procedure != null
                && procedure.getParams().size() == 4
                && procedure.getVariables().size() == 1);
        procedure = accessor.getProcedure(getOracleSchema(), "PROC_INVALID_ACCESSOR");
        Assert.assertTrue(procedure != null
                && procedure.getParams().size() == 0
                && procedure.getVariables().size() == 0);
    }

    @Test
    public void listPackages_Success() {
        List<DBPLObjectIdentity> packages = accessor.listPackages(getOracleSchema());
        Assert.assertTrue(packages != null && !packages.isEmpty());
    }

    @Test
    public void listPackages_invalidPackageList() {
        List<DBPLObjectIdentity> packages = accessor.listPackages(getOracleSchema());
        boolean flag = false;
        for (DBPLObjectIdentity dbPackage : packages) {
            if (StringUtils.containsIgnoreCase(dbPackage.getErrorMessage(), "ORA")) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getPackage_Success() {
        DBPackage dbPackage = accessor.getPackage(getOracleSchema(), "PAC_ACCESSOR");
        Assert.assertTrue(dbPackage != null
                && dbPackage.getPackageHead().getVariables().size() == 1
                && dbPackage.getPackageHead().getTypes().size() == 1
                && dbPackage.getPackageHead().getFunctions().size() == 2
                && dbPackage.getPackageHead().getProcedures().size() == 1);

        Assert.assertTrue(dbPackage.getPackageBody().getVariables().size() == 3
                && dbPackage.getPackageBody().getFunctions().size() == 2
                && dbPackage.getPackageBody().getProcedures().size() == 1);
    }

    @Test
    public void lisTriggers_Success() {
        List<DBPLObjectIdentity> triggers = accessor.listTriggers(getOracleSchema());
        Assert.assertNotEquals(null, triggers);
        boolean flag = false;
        for (DBPLObjectIdentity trigger : triggers) {
            if ("TRIGGER_ACCESSOR".equals(trigger.getName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void listTriggers_test_invalid_trigger() {
        List<DBPLObjectIdentity> triggers = accessor.listTriggers(getOracleSchema());
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
        DBTrigger trigger = accessor.getTrigger(getOracleSchema(), "TRIGGER_ACCESSOR");
        Assert.assertNotNull(trigger);
        Assert.assertEquals("TRIGGER_ACCESSOR", trigger.getTriggerName());
    }

    @Test
    public void listTypes_Success() {
        List<DBPLObjectIdentity> types = accessor.listTypes(getOracleSchema());
        Assert.assertNotNull(types);
        Assert.assertEquals(2, types.size());
    }

    @Test
    public void getType_Success() {
        DBType type = accessor.getType(getOracleSchema(), "TYPE_ACCESSOR2");
        Assert.assertNotNull(type);
        Assert.assertEquals("TYPE_ACCESSOR2", type.getTypeName());
        Assert.assertEquals(DBTypeCode.OBJECT.name(), type.getType());
        Assert.assertEquals(4, type.getTypeDetail().getVariables().size());
        Assert.assertEquals(1, type.getTypeDetail().getFunctions().size());
    }

    @Test
    public void listSequences_Success() {
        List<DBObjectIdentity> sequences = accessor.listSequences(getOracleSchema());
        Assert.assertTrue(sequences != null && !sequences.isEmpty());
    }

    @Test
    public void getSequence_Success() {
        DBSequence sequence = accessor.getSequence(getOracleSchema(), "SEQ_TEST");
        Assert.assertTrue(sequence != null && sequence.getName().equalsIgnoreCase("SEQ_TEST"));
        Assert.assertNotNull(sequence.getDdl());
    }

    @Test
    public void listSynonyms_Success() {
        List<DBObjectIdentity> synonyms = accessor.listSynonyms(getOracleSchema(), DBSynonymType.COMMON);
        Assert.assertNotEquals(0, synonyms.size());
        boolean flag = false;
        for (DBObjectIdentity synonym : synonyms) {
            if ("COMMON_SYNONYM_ACCESSOR".equals(synonym.getName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);

        synonyms = accessor.listSynonyms(getOracleSchema(), DBSynonymType.PUBLIC);
        Assert.assertNotEquals(0, synonyms.size());
        flag = false;
        for (DBObjectIdentity synonym : synonyms) {
            if ("PUBLIC_SYNONYM_ACCESSOR".equals(synonym.getName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void getSynonym_testPublicSynonymInfoForOracle() {
        DBSynonym synonym = accessor.getSynonym(getOracleSchema(), "PUBLIC_SYNONYM_ACCESSOR", DBSynonymType.PUBLIC);
        Assert.assertNotNull(synonym);
        Assert.assertEquals(DBSynonymType.PUBLIC, synonym.getSynonymType());
        Assert.assertEquals("PUBLIC_SYNONYM_ACCESSOR", synonym.getSynonymName());
    }

    @Test
    public void getSynonym_testCommonSynonymInfoForOracle() {
        DBSynonym synonym = accessor.getSynonym(getOracleSchema(), "COMMON_SYNONYM_ACCESSOR", DBSynonymType.COMMON);
        Assert.assertNotNull(synonym);
        Assert.assertEquals(DBSynonymType.COMMON, synonym.getSynonymType());
        Assert.assertEquals("COMMON_SYNONYM_ACCESSOR", synonym.getSynonymName());
    }

    @Test
    public void getTableOptions_Success() {
        DBTableOptions options =
                accessor.getTableOptions(getOracleSchema(), "PART_HASH_TEST");
        Assert.assertEquals("this is a comment", options.getComment());
        Assert.assertNotNull(options.getCharsetName());
        Assert.assertNotNull(options.getCollationName());
        Assert.assertNotNull(options.getUpdateTime());
    }
}
