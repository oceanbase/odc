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

package com.oceanbase.odc.service.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.session.util.SchemaExtractor;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

/**
 * @Author: Lebie
 * @Date: 2023/12/8 15:56
 * @Description: []
 */
public class SchemaExtractorTest {

    @Test
    public void testMySQL_listSchemaName2SqlTypes() {
        String sql1 = "select * from db1.table1;";
        String sql2 = "truncate table db1.table1;";
        Map<String, Set<SqlType>> actual = SchemaExtractor.listSchemaName2SqlTypes(
                Arrays.asList(SqlTuple.newTuple(sql1), SqlTuple.newTuple(sql2)), "default", DialectType.OB_MYSQL);
        Assert.assertTrue(actual.keySet().size() == 1 && actual.containsKey("db1"));
        Assert.assertTrue(actual.get("db1").size() == 2 && actual.get("db1").contains(SqlType.SELECT)
                && actual.get("db1").contains(SqlType.TRUNCATE));
    }

    @Test
    public void testMySQL_ListSchemaNames() {
        String sql = "select * from db1.table1";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("db1");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_FunctionDdl() {
        String sql = "create function `schema_name`.`func` (\n"
                + "\t`str1` varchar ( 45 ),\n"
                + "\t`str2` varchar ( 45 )) returns varchar ( 128 ) begin\n"
                + "return ( select concat( str1, str2 ) from dual );\n"
                + "end;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("schema_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_FunctionDdl_UseDefaultSchema() {
        String sql = "CREATE FUNCTION `func` (\n"
                + "\t`str1` VARCHAR ( 45 ),\n"
                + "\t`str2` VARCHAR ( 45 )) RETURNS VARCHAR ( 128 ) BEGIN\n"
                + "RETURN ( SELECT concat( str1, str2 ) FROM DUAL );\n"
                + "END;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("default");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_SelectFunction() {
        String sql = "select user_schema.user_function(4) from dual;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("user_schema");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_SelectFunction_UseDefaultSchema() {
        String sql = "select user_function(4) from dual;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("default");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_CallProcedure() {
        String sql = "call `schema_name`.`user_procedure`();";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("schema_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_CallProcedure_UseDefaultSchema() {
        String sql = "call user_procedure();";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("default");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ListSchemaNames_SwitchSchema() {
        String sql = "use `schema_name`;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_MYSQL, "default");
        Set<String> expect = Collections.singleton("schema_name");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMySQL_ExtractSwitchedSchemaName() {
        String sql = "use `schema_name`;";
        Optional<String> actual =
                SchemaExtractor.extractSwitchedSchemaName(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_MYSQL);
        Assert.assertEquals("schema_name", actual.get());
    }

    @Test
    public void testMySQL_ExtractSwitchedSchemaName_NotExist() {
        String sql = "select 1 from dual;";
        Optional<String> actual =
                SchemaExtractor.extractSwitchedSchemaName(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_MYSQL);
        Assert.assertFalse(actual.isPresent());
    }


    @Test
    public void testOracle_listSchemaName2SqlTypes() {
        String sql = "SELECT * FROM DB1.TABLE1";
        Map<String, Set<SqlType>> actual = SchemaExtractor
                .listSchemaName2SqlTypes(Arrays.asList(SqlTuple.newTuple(sql)), "default", DialectType.OB_ORACLE);
        Assert.assertTrue(actual.keySet().size() == 1 && actual.containsKey("DB1"));
        Assert.assertTrue(actual.get("DB1").size() == 1 && actual.get("DB1").contains(SqlType.SELECT));
    }

    @Test
    public void testOracle_listSchemaName2SqlTypes_WithDBLink() {
        String sql = "SELECT * FROM DB1.TABLE1@FAKE_DBLINK;";
        Map<String, Set<SqlType>> actual = SchemaExtractor
                .listSchemaName2SqlTypes(Arrays.asList(SqlTuple.newTuple(sql)), "default", DialectType.OB_ORACLE);
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void testOracle_ListSchemaNames() {
        String sql = "SELECT * FROM DB1.TABLE1";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "default");
        Set<String> expect = Collections.singleton("DB1");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_WithDBLink() {
        String sql = "SELECT * FROM DB1.TABLE1@FAKE_DBLINK;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "default");
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void testOracle_ListSchemaNames_FunctionDdl() {
        String sql = "CREATE OR REPLACE FUNCTION SCHEMA_NAME.INCREMENT_BY_ONE (INPUT_NUMBER IN NUMBER)\n"
                + "RETURN NUMBER IS\n"
                + "BEGIN\n"
                + "  RETURN INPUT_NUMBER + 1;\n"
                + "END INCREMENT_BY_ONE;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("SCHEMA_NAME");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_FunctionDdl_UseDefaultSchema() {
        String sql = "CREATE OR REPLACE FUNCTION INCREMENT_BY_ONE (INPUT_NUMBER IN NUMBER)\n"
                + "RETURN NUMBER IS\n"
                + "BEGIN\n"
                + "  RETURN INPUT_NUMBER + 1;\n"
                + "END INCREMENT_BY_ONE;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("DEFAULT");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_SelectFunction() {
        String sql = "SELECT USER_SCHEMA.USER_FUNCTION(4) FROM DUAL;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("USER_SCHEMA");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_SelectFunction_UseDefaultSchema() {
        String sql = "SELECT USER_FUNCTION(4) FROM DUAL;";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("DEFAULT");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_CallProcedure() {
        String sql = "CALL USER_SCHEMA_NAME.USER_PROCEDURE()";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("USER_SCHEMA_NAME");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_CallProcedure_UseDefaultSchema() {
        String sql = "CALL USER_PROCEDURE()";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("DEFAULT");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ListSchemaNames_SwitchSchema() {
        String sql = "ALTER SESSION SET CURRENT_SCHEMA = OTHER_SCHEMA";
        Set<String> actual = SchemaExtractor.listSchemaNames(Arrays.asList(sql), DialectType.OB_ORACLE, "DEFAULT");
        Set<String> expect = Collections.singleton("OTHER_SCHEMA");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testOracle_ExtractSwitchedSchemaName() {
        String sql = "ALTER SESSION SET CURRENT_SCHEMA = OTHER_SCHEMA";
        Optional<String> actual =
                SchemaExtractor.extractSwitchedSchemaName(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_ORACLE);
        Assert.assertEquals("OTHER_SCHEMA", actual.get());
    }

    @Test
    public void testOracle_ExtractSwitchedSchemaName_NotExist() {
        String sql = "SELECT 1 FROM DUAL;";
        Optional<String> actual =
                SchemaExtractor.extractSwitchedSchemaName(Arrays.asList(SqlTuple.newTuple(sql)), DialectType.OB_ORACLE);
        Assert.assertFalse(actual.isPresent());
    }

}
