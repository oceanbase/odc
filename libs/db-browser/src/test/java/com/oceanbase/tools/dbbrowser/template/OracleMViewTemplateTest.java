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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewRefreshMethod;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleMViewTemplate;

/**
 * @description: all tests for {@link OracleMViewTemplate}
 * @author: zijia.cj
 * @date: 2025/3/24 13:30
 * @since: 4.3.4
 */
public class OracleMViewTemplateTest {

    @Test
    public void generateCreateObjectTemplate_allInputs_success() {
        DBObjectTemplate<DBMaterializedView> oracleMViewTemplate = new OracleMViewTemplate();
        DBMaterializedView dbMaterializedView = new DBMaterializedView();
        dbMaterializedView.setName("mv_0");
        dbMaterializedView.setSchemaName("schema_0");
        MysqlMViewTemplateTest.prepareMViewPrimary(dbMaterializedView);
        dbMaterializedView.setParallelismDegree(8L);
        MysqlMViewTemplateTest.prepareMViewPartition(dbMaterializedView);
        MysqlMViewTemplateTest.prepareMViewColumnGroups(dbMaterializedView);
        dbMaterializedView.setRefreshMethod(DBMaterializedViewRefreshMethod.REFRESH_COMPLETE);
        MysqlMViewTemplateTest.prepareMViewStartNowSchedule(dbMaterializedView);
        dbMaterializedView.setEnableQueryRewrite(false);
        dbMaterializedView.setEnableQueryComputation(false);

        List<DBView.DBViewUnit> viewUnits = MysqlMViewTemplateTest.prepareViewUnit(2);
        dbMaterializedView.setViewUnits(viewUnits);
        dbMaterializedView.setOperations(Collections.singletonList("left join"));
        dbMaterializedView.setCreateColumns(MysqlMViewTemplateTest.prepareQueryColumns(2));

        String expect = "CREATE MATERIALIZED VIEW \"schema_0\".\"mv_0\"(PRIMARY KEY (\"alias_c0\"))\n" +
                "PARALLEL 8\n" +
                " PARTITION BY HASH(alias_c0) \n" +
                "PARTITIONS 3\n" +
                " WITH COLUMN GROUP(all columns,each column)\n" +
                "REFRESH COMPLETE\n" +
                "START WITH CURRENT_DATE\n" +
                "NEXT CURRENT_DATE + INTERVAL '1' DAY\n" +
                "DISABLE QUERY REWRITE\n" +
                "DISABLE ON QUERY COMPUTATION\n" +
                "AS\n" +
                "SELECT\n" +
                "\ttableAlias_0.\"c_0\" AS alias_c0,\n" +
                "\ttableAlias_0.\"d_0\" AS alias_d0,\n" +
                "\ttableAlias_1.\"c_1\" AS alias_c1,\n" +
                "\ttableAlias_1.\"d_1\" AS alias_d1\n" +
                "FROM\n" +
                "\t\"database_0\".\"table_0\" tableAlias_0\n" +
                "\tLEFT JOIN \"database_1\".\"table_1\" tableAlias_1 ON /* TODO enter attribute to join on here */";
        String actual = oracleMViewTemplate.generateCreateObjectTemplate(dbMaterializedView);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_startAtSchedule_success() {
        DBObjectTemplate<DBMaterializedView> oracleMViewTemplate = new OracleMViewTemplate();
        DBMaterializedView DBMaterializedView = new DBMaterializedView();
        DBMaterializedView.setName("mv_0");
        DBMaterializedView.setSchemaName("schema_0");
        MysqlMViewTemplateTest.prepareMViewStartAtSchedule(DBMaterializedView);

        List<DBView.DBViewUnit> viewUnits = MysqlMViewTemplateTest.prepareViewUnit(2);
        DBMaterializedView.setViewUnits(viewUnits);
        DBMaterializedView.setOperations(Collections.singletonList("left join"));
        DBMaterializedView.setCreateColumns(MysqlMViewTemplateTest.prepareQueryColumns(2));

        String expect = "CREATE MATERIALIZED VIEW \"schema_0\".\"mv_0\"\n" +
                "REFRESH FORCE\n" +
                "START WITH TO_DATE('2025-07-11 18:00:00', 'YYYY-MM-DD HH24:MI:SS')\n" +
                "NEXT TO_DATE('2025-07-11 18:00:00', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '1' MINUTE\n" +
                "AS\n" +
                "SELECT\n" +
                "\ttableAlias_0.\"c_0\" AS alias_c0,\n" +
                "\ttableAlias_0.\"d_0\" AS alias_d0,\n" +
                "\ttableAlias_1.\"c_1\" AS alias_c1,\n" +
                "\ttableAlias_1.\"d_1\" AS alias_d1\n" +
                "FROM\n" +
                "\t\"database_0\".\"table_0\" tableAlias_0\n" +
                "\tLEFT JOIN \"database_1\".\"table_1\" tableAlias_1 ON /* TODO enter attribute to join on here */";
        String actual = oracleMViewTemplate.generateCreateObjectTemplate(DBMaterializedView);
        Assert.assertEquals(expect, actual);
    }

}
