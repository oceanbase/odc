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
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBMView;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncDataMethod;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncSchedule;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBViewColumn;
import com.oceanbase.tools.dbbrowser.template.mysql.MysqlMViewTemplate;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 23:21
 * @since: 4.3.4
 */
public class MysqlMViewTemplateTest {
    @Test
    public void generateCreateObjectTemplate_allInputs_success() {
        DBObjectTemplate<DBMView> mysqlMViewTemplate = new MysqlMViewTemplate();
        DBMView dbmView = new DBMView();
        dbmView.setName("mv_0");
        dbmView.setSchemaName("schema_0");
        prepareMViewPrimary(dbmView);
        dbmView.setParallelismDegree(8L);
        prepareMViewPartition(dbmView);
        prepareMViewColumnGroups(dbmView);
        dbmView.setSyncDataMethod(DBMViewSyncDataMethod.REFRESH_COMPLETE);
        prepareMViewStartNowSchedule(dbmView);
        dbmView.setEnableQueryRewrite(false);
        dbmView.setEnableQueryComputation(false);

        List<DBView.DBViewUnit> viewUnits = prepareViewUnit(2);
        dbmView.setViewUnits(viewUnits);
        dbmView.setOperations(Collections.singletonList("left join"));
        dbmView.setCreateColumns(prepareQueryColumns(2));

        String expect = "create materialized view `schema_0`.`mv_0`(PRIMARY KEY (`alias_c0`))\n" +
                "PARALLEL 8\n" +
                " PARTITION BY HASH(`alias_c0`) \n" +
                "PARTITIONS 3\n" +
                " WITH COLUMN GROUP(all columns,each column)\n" +
                "REFRESH COMPLETE\n" +
                "START WITH sysdate()\n" +
                "NEXT sysdate() + INTERVAL 1 DAY\n" +
                "DISABLE QUERY REWRITE\n" +
                "DISABLE ON QUERY COMPUTATION\n" +
                "AS\n" +
                "select\n" +
                "\ttableAlias_0.`c_0` as alias_c0,\n" +
                "\ttableAlias_0.`d_0` as alias_d0,\n" +
                "\ttableAlias_1.`c_1` as alias_c1,\n" +
                "\ttableAlias_1.`d_1` as alias_d1\n" +
                "from\n" +
                "\t`database_0`.`table_0` tableAlias_0\n" +
                "\tleft join `database_1`.`table_1` tableAlias_1 on /* TODO enter attribute to join on here */";
        String actual = mysqlMViewTemplate.generateCreateObjectTemplate(dbmView);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_startAtSchedule_success() {
        DBObjectTemplate<DBMView> mysqlMViewTemplate = new MysqlMViewTemplate();
        DBMView dbmView = new DBMView();
        dbmView.setName("mv_0");
        dbmView.setSchemaName("schema_0");
        prepareMViewStartAtSchedule(dbmView);

        List<DBView.DBViewUnit> viewUnits = prepareViewUnit(2);
        dbmView.setViewUnits(viewUnits);
        dbmView.setOperations(Collections.singletonList("left join"));
        dbmView.setCreateColumns(prepareQueryColumns(2));

        String expect = "create materialized view `schema_0`.`mv_0`\n" +
                "START WITH TIMESTAMP '2025-07-11 18:00:00'\n" +
                "NEXT TIMESTAMP '2025-07-11 18:00:00' + INTERVAL 1 DAY\n" +
                "AS\n" +
                "select\n" +
                "\ttableAlias_0.`c_0` as alias_c0,\n" +
                "\ttableAlias_0.`d_0` as alias_d0,\n" +
                "\ttableAlias_1.`c_1` as alias_c1,\n" +
                "\ttableAlias_1.`d_1` as alias_d1\n" +
                "from\n" +
                "\t`database_0`.`table_0` tableAlias_0\n" +
                "\tleft join `database_1`.`table_1` tableAlias_1 on /* TODO enter attribute to join on here */";
        String actual = mysqlMViewTemplate.generateCreateObjectTemplate(dbmView);
        Assert.assertEquals(expect, actual);
    }

    private void prepareMViewStartNowSchedule(DBMView dbmView) {
        DBMViewSyncSchedule dbmViewSyncSchedule = new DBMViewSyncSchedule();
        dbmViewSyncSchedule.setStartStrategy(DBMViewSyncSchedule.StartStrategy.START_NOW);
        dbmViewSyncSchedule.setInterval(1L);
        dbmViewSyncSchedule.setUnit(DBMViewSyncSchedule.Unit.DAY);
        dbmView.setSyncSchedule(dbmViewSyncSchedule);
    }

    private void prepareMViewStartAtSchedule(DBMView dbmView) {
        DBMViewSyncSchedule dbmViewSyncSchedule = new DBMViewSyncSchedule();
        dbmViewSyncSchedule.setStartStrategy(DBMViewSyncSchedule.StartStrategy.START_AT);
        dbmViewSyncSchedule.setStartWith(new Date(1752228000000L));
        dbmViewSyncSchedule.setInterval(1L);
        dbmViewSyncSchedule.setUnit(DBMViewSyncSchedule.Unit.DAY);
        dbmView.setSyncSchedule(dbmViewSyncSchedule);
    }

    private void prepareMViewColumnGroups(DBMView dbmView) {
        List<DBColumnGroupElement> dbColumnGroupElements = new ArrayList<>();
        DBColumnGroupElement dbColumnGroupElement1 = new DBColumnGroupElement();
        dbColumnGroupElement1.setAllColumns(true);
        DBColumnGroupElement dbColumnGroupElement2 = new DBColumnGroupElement();
        dbColumnGroupElement2.setEachColumn(true);
        dbColumnGroupElements.add(dbColumnGroupElement1);
        dbColumnGroupElements.add(dbColumnGroupElement2);
        dbmView.setColumnGroups(dbColumnGroupElements);
    }

    private static void prepareMViewPartition(DBMView dbmView) {
        DBTablePartition dbTablePartition = new DBTablePartition();
        DBTablePartitionOption dbTablePartitionOption = new DBTablePartitionOption();
        dbTablePartitionOption.setType(DBTablePartitionType.HASH);
        dbTablePartitionOption.setExpression("`alias_c0`");
        dbTablePartitionOption.setPartitionsNum(3);
        dbTablePartition.setPartitionOption(dbTablePartitionOption);
        dbmView.setPartition(dbTablePartition);
    }

    private void prepareMViewPrimary(DBMView dbmView) {
        DBTableConstraint dbTableConstraint = new DBTableConstraint();
        dbTableConstraint.setType(DBConstraintType.PRIMARY_KEY);
        dbTableConstraint.setColumnNames(Collections.singletonList("alias_c0"));
        dbmView.setConstraints(Collections.singletonList(dbTableConstraint));
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

    private List<DBViewColumn> prepareQueryColumns(int size) {
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
