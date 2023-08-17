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
package com.oceanbase.tools.dbbrowser.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBView.DBViewUnit;
import com.oceanbase.tools.dbbrowser.model.DBViewColumn;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleViewTemplate;

/**
 * {@link OracleViewTemplateTest}
 *
 * @author yh263208
 * @date 2023-02-23 20:09
 * @since db-browser_1.0.0_SNAPSHOT
 */
public class OracleViewTemplateTest {

    @Test
    public void generateCreateObjectTemplate_readonlyView_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");
        List<DBViewUnit> viewUnits = prepareViewUnit(4);
        view.setViewUnits(viewUnits);
        view.setOperations(Arrays.asList("left join", "join", "inner join"));
        List<DBViewColumn> columns = prepareColumns(4);
        view.setCreateColumns(columns);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\ttableAlias_0.\"c_0\" AS alias_c0,\n"
                + "\ttableAlias_0.\"d_0\" AS alias_d0,\n"
                + "\ttableAlias_1.\"c_1\" AS alias_c1,\n"
                + "\ttableAlias_1.\"d_1\" AS alias_d1,\n"
                + "\ttableAlias_2.\"c_2\" AS alias_c2,\n"
                + "\ttableAlias_2.\"d_2\" AS alias_d2,\n"
                + "\ttableAlias_3.\"c_3\" AS alias_c3,\n"
                + "\ttableAlias_3.\"d_3\" AS alias_d3\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\" tableAlias_0\n"
                + "\tLEFT JOIN \"database_1\".\"table_1\" tableAlias_1 ON /* TODO enter attribute to join on here */\n"
                + "\tJOIN \"database_2\".\"table_2\" tableAlias_2 ON /* TODO enter attribute to join on here */\n"
                + "\tINNER JOIN \"database_3\".\"table_3\" tableAlias_3 ON /* TODO enter attribute to join on here */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithUnion_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");
        view.setViewUnits(prepareViewUnit(2));
        view.setCreateColumns(prepareColumns(2));
        view.setOperations(Collections.singletonList("union all"));

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\ttableAlias_0.\"c_0\" AS alias_c0,\n"
                + "\ttableAlias_0.\"d_0\" AS alias_d0\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\" tableAlias_0\n"
                + "UNION ALL \n"
                + "SELECT\n"
                + "\ttableAlias_1.\"c_1\" AS alias_c1,\n"
                + "\ttableAlias_1.\"d_1\" AS alias_d1\n"
                + "FROM\n"
                + "\t\"database_1\".\"table_1\" tableAlias_1\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithUnionAndJoin_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");
        view.setViewUnits(prepareViewUnit(5));
        view.setCreateColumns(prepareColumns(5));

        List<String> operations = new ArrayList<>();
        operations.add("left join");
        operations.add("full outer join");
        operations.add("union all");
        operations.add("right join");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\ttableAlias_0.\"c_0\" AS alias_c0,\n"
                + "\ttableAlias_0.\"d_0\" AS alias_d0,\n"
                + "\ttableAlias_1.\"c_1\" AS alias_c1,\n"
                + "\ttableAlias_1.\"d_1\" AS alias_d1,\n"
                + "\ttableAlias_2.\"c_2\" AS alias_c2,\n"
                + "\ttableAlias_2.\"d_2\" AS alias_d2\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\" tableAlias_0\n"
                + "\tLEFT JOIN \"database_1\".\"table_1\" tableAlias_1 ON /* TODO enter attribute to join on here */\n"
                + "\tFULL OUTER JOIN \"database_2\".\"table_2\" tableAlias_2 ON /* TODO enter attribute to join on here */\n"
                + "UNION ALL \n"
                + "SELECT\n"
                + "\ttableAlias_3.\"c_3\" AS alias_c3,\n"
                + "\ttableAlias_3.\"d_3\" AS alias_d3,\n"
                + "\ttableAlias_4.\"c_4\" AS alias_c4,\n"
                + "\ttableAlias_4.\"d_4\" AS alias_d4\n"
                + "FROM\n"
                + "\t\"database_3\".\"table_3\" tableAlias_3\n"
                + "\tRIGHT JOIN \"database_4\".\"table_4\" tableAlias_4 ON /* TODO enter attribute to join on here */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewCustomColumns_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");
        view.setViewUnits(prepareViewUnit(5));

        List<DBViewColumn> columns = prepareColumns(5);
        DBViewColumn c1 = new DBViewColumn();
        c1.setColumnName("lc");
        c1.setAliasName("k");
        DBViewColumn c2 = new DBViewColumn();
        c2.setColumnName("lcc");
        c2.setAliasName("q");
        columns.add(c1);
        columns.add(c2);
        view.setCreateColumns(columns);

        List<String> operations = new ArrayList<>();
        operations.add("left join");
        operations.add("full outer join");
        operations.add("union all");
        operations.add("right join");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\ttableAlias_0.\"c_0\" AS alias_c0,\n"
                + "\ttableAlias_0.\"d_0\" AS alias_d0,\n"
                + "\ttableAlias_1.\"c_1\" AS alias_c1,\n"
                + "\ttableAlias_1.\"d_1\" AS alias_d1,\n"
                + "\ttableAlias_2.\"c_2\" AS alias_c2,\n"
                + "\ttableAlias_2.\"d_2\" AS alias_d2,\n"
                + "\tlc AS k,\n"
                + "\tlcc AS q\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\" tableAlias_0\n"
                + "\tLEFT JOIN \"database_1\".\"table_1\" tableAlias_1 ON /* TODO enter attribute to join on here */\n"
                + "\tFULL OUTER JOIN \"database_2\".\"table_2\" tableAlias_2 ON /* TODO enter attribute to join on here */\n"
                + "UNION ALL \n"
                + "SELECT\n"
                + "\ttableAlias_3.\"c_3\" AS alias_c3,\n"
                + "\ttableAlias_3.\"d_3\" AS alias_d3,\n"
                + "\ttableAlias_4.\"c_4\" AS alias_c4,\n"
                + "\ttableAlias_4.\"d_4\" AS alias_d4,\n"
                + "\tlc AS k,\n"
                + "\tlcc AS q\n"
                + "FROM\n"
                + "\t\"database_3\".\"table_3\" tableAlias_3\n"
                + "\tRIGHT JOIN \"database_4\".\"table_4\" tableAlias_4 ON /* TODO enter attribute to join on here */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithoutTableAndCols_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        Assert.assertEquals("CREATE OR REPLACE VIEW \"v_test\" AS", editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithCommaOperation_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");
        view.setViewUnits(prepareViewUnit(2));
        view.setCreateColumns(prepareColumns(2));
        view.setOperations(Collections.singletonList(","));

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\ttableAlias_0.\"c_0\" AS alias_c0,\n"
                + "\ttableAlias_0.\"d_0\" AS alias_d0,\n"
                + "\ttableAlias_1.\"c_1\" AS alias_c1,\n"
                + "\ttableAlias_1.\"d_1\" AS alias_d1\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\" tableAlias_0, \"database_1\".\"table_1\" tableAlias_1\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithDuplicateColumn_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DBViewUnit viewUnit = new DBViewUnit();
            viewUnit.setDbName("database_" + i);
            viewUnit.setTableName("table_" + i);
            viewUnits.add(viewUnit);
        }
        view.setViewUnits(viewUnits);
        List<DBViewColumn> columns = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DBViewColumn viewColumn1 = new DBViewColumn();
            viewColumn1.setColumnName("c");
            viewColumn1.setAliasName("alias_c" + i);
            viewColumn1.setDbName("database_" + i);
            viewColumn1.setTableName("table_" + i);

            DBViewColumn viewColumn2 = new DBViewColumn();
            viewColumn2.setColumnName("d");
            viewColumn2.setAliasName("alias_d" + i);
            viewColumn2.setDbName("database_" + i);
            viewColumn2.setTableName("table_" + i);

            columns.add(viewColumn1);
            columns.add(viewColumn2);
        }
        view.setCreateColumns(columns);
        view.setOperations(Collections.singletonList("left join"));

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\t\"database_0\".\"table_0\".\"c\" AS alias_c0,\n"
                + "\t\"database_0\".\"table_0\".\"d\" AS alias_d0,\n"
                + "\t\"database_1\".\"table_1\".\"c\" AS alias_c1,\n"
                + "\t\"database_1\".\"table_1\".\"d\" AS alias_d1\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\"\n"
                + "\tLEFT JOIN \"database_1\".\"table_1\" ON /* TODO enter attribute to join on here */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithoutAlias_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DBViewUnit viewUnit = new DBViewUnit();
            viewUnit.setDbName("database_" + i);
            viewUnit.setTableName("table_" + i);

            viewUnits.add(viewUnit);
        }
        view.setViewUnits(viewUnits);

        List<DBViewColumn> columns = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DBViewColumn viewColumn1 = new DBViewColumn();
            viewColumn1.setColumnName("c");
            viewColumn1.setDbName("database_" + i);
            viewColumn1.setTableName("table_" + i);
            columns.add(viewColumn1);
        }
        view.setCreateColumns(columns);

        List<String> operations = new ArrayList<>();
        operations.add(",");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\t\"database_0\".\"table_0\".\"c\",\n"
                + "\t\"database_1\".\"table_1\".\"c\"\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\", \"database_1\".\"table_1\"\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithOneEmptyColumn_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DBViewUnit viewUnit = new DBViewUnit();
            viewUnit.setDbName("database_" + i);
            viewUnit.setTableName("table_" + i);

            viewUnits.add(viewUnit);
        }
        view.setViewUnits(viewUnits);

        List<String> operations = new ArrayList<>();
        operations.add(",");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT \n"
                + "\t*\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\", \"database_1\".\"table_1\"\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithSeveralEmptyColumns_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DBViewUnit viewUnit = new DBViewUnit();
            viewUnit.setDbName("database_" + i);
            viewUnit.setTableName("table_" + i);

            viewUnits.add(viewUnit);
        }
        view.setViewUnits(viewUnits);

        List<String> operations = new ArrayList<>();
        operations.add(",");
        operations.add(",");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT \n"
                + "\t*\n"
                + "FROM\n"
                + "\t\"database_0\".\"table_0\", \"database_1\".\"table_1\", \"database_2\".\"table_2\"\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithEmptyAndNotColumns_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();

        DBViewUnit viewUnit = new DBViewUnit();
        viewUnit.setDbName("database_1");
        viewUnit.setTableName("table_1");
        viewUnits.add(viewUnit);

        DBViewUnit viewUnit1 = new DBViewUnit();
        viewUnit1.setDbName("database_2");
        viewUnit1.setTableName("table_2");
        viewUnits.add(viewUnit1);

        DBViewUnit viewUnit2 = new DBViewUnit();
        viewUnit2.setDbName("database_3");
        viewUnit2.setTableName("table_3");
        viewUnits.add(viewUnit2);

        view.setViewUnits(viewUnits);

        DBViewColumn viewColumn1 = new DBViewColumn();
        viewColumn1.setColumnName("c");
        viewColumn1.setDbName("database_2");
        viewColumn1.setTableName("table_2");
        List<DBViewColumn> viewColumns = new ArrayList<>();
        viewColumns.add(viewColumn1);
        view.setCreateColumns(viewColumns);

        List<String> operations = new ArrayList<>();
        operations.add(",");
        operations.add(",");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\t\"database_2\".\"table_2\".\"c\"\n"
                + "FROM\n"
                + "\t\"database_1\".\"table_1\", \"database_2\".\"table_2\", \"database_3\".\"table_3\"\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithOrder_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();

        DBViewUnit viewUnit = new DBViewUnit();
        viewUnit.setDbName("database_1");
        viewUnit.setTableName("table_1");
        viewUnits.add(viewUnit);

        DBViewUnit viewUnit1 = new DBViewUnit();
        viewUnit1.setDbName("database_2");
        viewUnit1.setTableName("table_2");
        viewUnits.add(viewUnit1);

        DBViewUnit viewUnit2 = new DBViewUnit();
        viewUnit2.setDbName("database_3");
        viewUnit2.setTableName("table_3");
        viewUnits.add(viewUnit2);

        view.setViewUnits(viewUnits);

        DBViewColumn viewColumn3 = new DBViewColumn();
        viewColumn3.setColumnName("e");
        viewColumn3.setDbName("database_3");
        viewColumn3.setTableName("table_3");

        DBViewColumn viewColumn1 = new DBViewColumn();
        viewColumn1.setColumnName("c");
        viewColumn1.setDbName("database_2");
        viewColumn1.setTableName("table_2");

        DBViewColumn viewColumn2 = new DBViewColumn();
        viewColumn2.setColumnName("d");
        viewColumn2.setDbName("database_1");
        viewColumn2.setTableName("table_1");

        List<DBViewColumn> viewColumns = new ArrayList<>();
        viewColumns.add(viewColumn3);
        viewColumns.add(viewColumn1);
        viewColumns.add(viewColumn2);
        view.setCreateColumns(viewColumns);

        List<String> operations = new ArrayList<>();
        operations.add(",");
        operations.add(",");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\t\"database_3\".\"table_3\".\"e\",\n"
                + "\t\"database_2\".\"table_2\".\"c\",\n"
                + "\t\"database_1\".\"table_1\".\"d\"\n"
                + "FROM\n"
                + "\t\"database_1\".\"table_1\", \"database_2\".\"table_2\", \"database_3\".\"table_3\"\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_viewWithOrderAndUnionAndCustom_generateSucceed() {
        DBObjectTemplate<DBView> editor = new OracleViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setCheckOption("READ_ONLY");

        List<DBViewUnit> viewUnits = new ArrayList<>();

        DBViewUnit viewUnit = new DBViewUnit();
        viewUnit.setDbName("database_1");
        viewUnit.setTableName("table_1");
        viewUnits.add(viewUnit);

        DBViewUnit viewUnit1 = new DBViewUnit();
        viewUnit1.setDbName("database_2");
        viewUnit1.setTableName("table_2");
        viewUnits.add(viewUnit1);

        DBViewUnit viewUnit2 = new DBViewUnit();
        viewUnit2.setDbName("database_3");
        viewUnit2.setTableName("table_3");
        viewUnits.add(viewUnit2);

        view.setViewUnits(viewUnits);

        DBViewColumn viewColumn3 = new DBViewColumn();
        viewColumn3.setColumnName("e");
        viewColumn3.setDbName("database_3");
        viewColumn3.setTableName("table_3");

        DBViewColumn viewColumn1 = new DBViewColumn();
        viewColumn1.setColumnName("c");
        viewColumn1.setDbName("database_2");
        viewColumn1.setTableName("table_2");

        DBViewColumn viewColumn2 = new DBViewColumn();
        viewColumn2.setColumnName("d");
        viewColumn2.setDbName("database_1");
        viewColumn2.setTableName("table_1");

        DBViewColumn viewColumn4 = new DBViewColumn();
        viewColumn4.setColumnName("custom");

        List<DBViewColumn> viewColumns = new ArrayList<>();
        viewColumns.add(viewColumn4);
        viewColumns.add(viewColumn3);
        viewColumns.add(viewColumn1);
        viewColumns.add(viewColumn2);
        view.setCreateColumns(viewColumns);

        List<String> operations = new ArrayList<>();
        operations.add(",");
        operations.add("union");
        view.setOperations(operations);

        String expect = "CREATE OR REPLACE VIEW \"v_test\" AS\n"
                + "SELECT\n"
                + "\tcustom,\n"
                + "\t\"database_2\".\"table_2\".\"c\",\n"
                + "\t\"database_1\".\"table_1\".\"d\"\n"
                + "FROM\n"
                + "\t\"database_1\".\"table_1\", \"database_2\".\"table_2\"\n"
                + "WHERE /* TODO enter condition clause for where */\n"
                + "UNION \n"
                + "SELECT\n"
                + "\tcustom,\n"
                + "\t\"database_3\".\"table_3\".\"e\"\n"
                + "FROM\n"
                + "\t\"database_3\".\"table_3\"\n"
                + "WITH READ ONLY";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
    }

    private List<DBViewUnit> prepareViewUnit(int size) {
        List<DBViewUnit> viewUnits = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            DBViewUnit viewUnit = new DBViewUnit();
            viewUnit.setDbName("database_" + i);
            viewUnit.setTableName("table_" + i);
            viewUnit.setTableAliasName("tableAlias_" + i);

            viewUnits.add(viewUnit);
        }
        return viewUnits;
    }

    private List<DBViewColumn> prepareColumns(int size) {
        List<DBViewColumn> viewColumns = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            DBViewColumn viewColumn1 = new DBViewColumn();
            viewColumn1.setColumnName("c_" + i);
            viewColumn1.setAliasName("alias_c" + i);
            viewColumn1.setDbName("database_" + i);
            viewColumn1.setTableName("table_" + i);
            viewColumn1.setTableAliasName("tableAlias_" + i);

            DBViewColumn viewColumn2 = new DBViewColumn();
            viewColumn2.setColumnName("d_" + i);
            viewColumn2.setAliasName("alias_d" + i);
            viewColumn2.setDbName("database_" + i);
            viewColumn2.setTableName("table_" + i);
            viewColumn2.setTableAliasName("tableAlias_" + i);

            viewColumns.add(viewColumn1);
            viewColumns.add(viewColumn2);
        }
        return viewColumns;
    }

}
