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


import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewRefreshMethod;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.DBViewColumn;
import com.oceanbase.tools.dbbrowser.template.mysql.MysqlMViewTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleMViewTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description: all tests for {@link OracleMViewTemplate}
 * @author: zijia.cj
 * @date: 2025/3/24 13:30
 * @since: 4.3.4
 */
public class OracleMViewTemplateTest {

    private MysqlMViewTemplateTest mysqlMViewTemplateTest = new MysqlMViewTemplateTest();

    @Test
    public void generateCreateObjectTemplate_allInputs_success() {
        DBObjectTemplate<DBMaterializedView> oracleMViewTemplate = new OracleMViewTemplate();
        DBMaterializedView DBMaterializedView = new DBMaterializedView();
        DBMaterializedView.setName("mv_0");
        DBMaterializedView.setSchemaName("schema_0");
        mysqlMViewTemplateTest.prepareMViewPrimary(DBMaterializedView);
        DBMaterializedView.setParallelismDegree(8L);
        mysqlMViewTemplateTest.prepareMViewPartition(DBMaterializedView);
        mysqlMViewTemplateTest.prepareMViewColumnGroups(DBMaterializedView);
        DBMaterializedView.setRefreshMethod(DBMaterializedViewRefreshMethod.REFRESH_COMPLETE);
        mysqlMViewTemplateTest.prepareMViewStartNowSchedule(DBMaterializedView);
        DBMaterializedView.setEnableQueryRewrite(false);
        DBMaterializedView.setEnableQueryComputation(false);

        List<DBView.DBViewUnit> viewUnits = mysqlMViewTemplateTest.prepareViewUnit(2);
        DBMaterializedView.setViewUnits(viewUnits);
        DBMaterializedView.setOperations(Collections.singletonList("left join"));
        DBMaterializedView.setCreateColumns(mysqlMViewTemplateTest.prepareQueryColumns(2));

        String expect = "CREATE MATERIALIZED VIEW `schema_0`.`mv_0`(PRIMARY KEY (`alias_c0`))\n" +
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
        String actual = oracleMViewTemplate.generateCreateObjectTemplate(DBMaterializedView);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_startAtSchedule_success() {
        DBObjectTemplate<DBMaterializedView> mysqlMViewTemplate = new MysqlMViewTemplate();
        DBMaterializedView DBMaterializedView = new DBMaterializedView();
        DBMaterializedView.setName("mv_0");
        DBMaterializedView.setSchemaName("schema_0");
        mysqlMViewTemplateTest.prepareMViewStartAtSchedule(DBMaterializedView);

        List<DBView.DBViewUnit> viewUnits = mysqlMViewTemplateTest.prepareViewUnit(2);
        DBMaterializedView.setViewUnits(viewUnits);
        DBMaterializedView.setOperations(Collections.singletonList("left join"));
        DBMaterializedView.setCreateColumns(mysqlMViewTemplateTest.prepareQueryColumns(2));

        String expect = "CREATE MATERIALIZED VIEW `schema_0`.`mv_0`\n" +
            "REFRESH FORCE\n" +
            "START WITH TIMESTAMP '2025-07-11 18:00:00'\n" +
            "NEXT TIMESTAMP '2025-07-11 18:00:00' + INTERVAL 1 WEEK\n" +
            "AS\n" +
            "select\n" +
            "\ttableAlias_0.`c_0` as alias_c0,\n" +
            "\ttableAlias_0.`d_0` as alias_d0,\n" +
            "\ttableAlias_1.`c_1` as alias_c1,\n" +
            "\ttableAlias_1.`d_1` as alias_d1\n" +
            "from\n" +
            "\t`database_0`.`table_0` tableAlias_0\n" +
            "\tleft join `database_1`.`table_1` tableAlias_1 on /* TODO enter attribute to join on here */";
        String actual = mysqlMViewTemplate.generateCreateObjectTemplate(DBMaterializedView);
        Assert.assertEquals(expect, actual);
    }

}
