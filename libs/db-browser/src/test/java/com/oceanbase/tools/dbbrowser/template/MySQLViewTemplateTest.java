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
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBView.DBViewUnit;
import com.oceanbase.tools.dbbrowser.model.DBViewColumn;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLViewTemplate;

/**
 * {@link MySQLViewTemplateTest}
 *
 * @author yh263208
 * @date 2023-02-27 16:54
 * @since db-browser-1.0.0_SNAPSHOT
 */
public class MySQLViewTemplateTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void generateCreateObjectTemplate_viewWithoutName_expThrown() {
        DBObjectTemplate<DBView> editor = new MySQLViewTemplate();
        DBView view = new DBView();

        thrown.expectMessage("View name can not be blank");
        thrown.expect(NullPointerException.class);
        editor.generateCreateObjectTemplate(view);
    }

    @Test
    public void generateCreateObjectTemplate_viewWithOnlyName_generateSucceed() {
        DBObjectTemplate<DBView> editor = new MySQLViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        Assert.assertEquals("create or replace view `v_test` as", editor.generateCreateObjectTemplate(view));
    }

    @Test
    public void generateCreateObjectTemplate_tableOperationsUnmatched_expThrown() {
        DBObjectTemplate<DBView> editor = new MySQLViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        view.setViewUnits(prepareViewUnit(2));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unable to calculate, operationSize<>tableSize-1");
        editor.generateCreateObjectTemplate(view);
    }

    @Test
    public void generateCreateObjectTemplate_view_generateSucceed() {
        DBObjectTemplate<DBView> editor = new MySQLViewTemplate();
        DBView view = new DBView();
        view.setViewName("v_test");
        List<DBViewUnit> viewUnits = prepareViewUnit(2);
        view.setViewUnits(viewUnits);
        view.setOperations(Collections.singletonList("left join"));
        view.setCreateColumns(prepareColumns(2));

        String expect = "create or replace view `v_test` as\n"
                + "select\n"
                + "\ttableAlias_0.`c_0` as alias_c0,\n"
                + "\ttableAlias_0.`d_0` as alias_d0,\n"
                + "\ttableAlias_1.`c_1` as alias_c1,\n"
                + "\ttableAlias_1.`d_1` as alias_d1\n"
                + "from\n"
                + "\t`database_0`.`table_0` tableAlias_0\n"
                + "\tleft join `database_1`.`table_1` tableAlias_1 on /* TODO enter attribute to join on here */";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(view));
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

}
