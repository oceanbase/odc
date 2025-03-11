/*
 * Copyright (c) 2025 OceanBase.
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

import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBViewColumn;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLViewTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MysqlMViewTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 23:21
 * @since: 4.3.4
 */
public class MysqlMViewTemplateTest {
    @Test
    public void test(){
        DBObjectTemplate<DBMView> mysqlMViewTemplate = new MysqlMViewTemplate();
        DBMView dbmView = new DBMView();
        dbmView.setMVName("v_test");
        dbmView.setSchemaName("schema_0");

        List<DBView.DBViewUnit> viewUnits = prepareViewUnit(2);
        dbmView.setViewUnits(viewUnits);
        dbmView.setOperations(Collections.singletonList("left join"));
        dbmView.setCreateColumns(prepareColumns(2));

        String expect = "\n"
            + "select\n"
            + "\ttableAlias_0.`c_0` as alias_c0,\n"
            + "\ttableAlias_0.`d_0` as alias_d0,\n"
            + "\ttableAlias_1.`c_1` as alias_c1,\n"
            + "\ttableAlias_1.`d_1` as alias_d1\n"
            + "from\n"
            + "\t`database_0`.`table_0` tableAlias_0\n"
            + "\tleft join `database_1`.`table_1` tableAlias_1 on /* TODO enter attribute to join on here */";
        Assert.assertEquals(expect, mysqlMViewTemplate.generateCreateObjectTemplate(dbmView));
    }

    private List<DBView.DBViewUnit> prepareViewUnit(int size) {
        List<DBView.DBViewUnit> viewUnits = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            DBView.DBViewUnit viewUnit = new DBView.DBViewUnit();
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
