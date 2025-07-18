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
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeSchedule;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.template.mysql.OBMySQLMViewLogTemplate;

/**
 * @description: all tests for {@link OBMySQLMViewLogTemplate}
 * @author: zijia.cj
 * @date: 2025/7/15 20:58
 * @since: 4.4.0
 */
public class OBMySQLMViewLogTemplateTest {

    private static OBMySQLMViewLogTemplate template;

    @BeforeClass
    public static void setUp() {
        template = new OBMySQLMViewLogTemplate();
    }

    @Test(expected = NullPointerException.class)
    public void generateCreateObjectTemplate_StartStrategyIsNull_TrowNullPointerException() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setPurgeSchedule(new DBMViewLogPurgeSchedule());
        template.generateCreateObjectTemplate(mViewLog);
    }

    @Test
    public void generateCreateObjectTemplate_Simple_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        String expect = """
                CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithColumns_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setColumns(prepareColumns(3));
        String expect = """
                CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`
                WITH (`col2`,`col3`,`col4`)""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithParallelismDegree_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setPurgeParallelismDegree(5L);
        String expect = """
                CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`
                PARALLEL 5""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithStartNowSchedule_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        prepareStartNowPurgeSchedule(mViewLog);
        String expect = """
                CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`
                PURGE START WITH sysdate()
                NEXT sysdate() + INTERVAL 1 DAY""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithStartAtSchedule_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        prepareStartAtPurgeSchedule(mViewLog);
        String expect = """
                CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`
                PURGE START WITH TIMESTAMP '2025-07-11 18:00:00'
                NEXT TIMESTAMP '2025-07-11 18:00:00' + INTERVAL 1 MINUTE""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_CompleteWithStartNowSchedule_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setPurgeParallelismDegree(5L);
        mViewLog.setColumns(prepareColumns(3));
        prepareStartNowPurgeSchedule(mViewLog);

        String expect = """
            CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`
            PARALLEL 5
            WITH (`col2`,`col3`,`col4`)
            PURGE START WITH sysdate()
            NEXT sysdate() + INTERVAL 1 DAY""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_CompleteWithStartAtSchedule_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setPurgeParallelismDegree(2L);
        mViewLog.setColumns(prepareColumns(4));
        prepareStartAtPurgeSchedule(mViewLog);

        String expect = """
            CREATE MATERIALIZED VIEW LOG ON `schema_name`.`base_table_name`
            PARALLEL 2
            WITH (`col2`,`col3`,`col4`,`col5`)
            PURGE START WITH TIMESTAMP '2025-07-11 18:00:00'
            NEXT TIMESTAMP '2025-07-11 18:00:00' + INTERVAL 1 MINUTE""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    public static List<DBTableColumn> prepareColumns(int size) {
        List<DBTableColumn> columns = new ArrayList<>();
        for (int i = 2; i < size + 2; i++) {
            DBTableColumn dbTableColumn = new DBTableColumn();
            dbTableColumn.setName("col" + i);
            columns.add(dbTableColumn);
        }
        return columns;
    }

    public static void prepareStartNowPurgeSchedule(DBMaterializedViewLog mViewLog) {
        DBMViewLogPurgeSchedule purgeSchedule = new DBMViewLogPurgeSchedule();
        purgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_NOW);
        purgeSchedule.setInterval(1L);
        purgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.DAY);
        mViewLog.setPurgeSchedule(purgeSchedule);
    }

    public static void prepareStartAtPurgeSchedule(DBMaterializedViewLog mViewLog) {
        DBMViewLogPurgeSchedule purgeSchedule = new DBMViewLogPurgeSchedule();
        purgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_AT);
        purgeSchedule.setStartWith(new Date(1752228000000L));
        purgeSchedule.setInterval(1L);
        purgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.MINUTE);
        mViewLog.setPurgeSchedule(purgeSchedule);
    }

    public static DBMaterializedViewLog generateDbMaterializedViewLog() {
        DBMaterializedViewLog mViewLog = new DBMaterializedViewLog();
        mViewLog.setBaseTableName("base_table_name");
        mViewLog.setSchemaName("schema_name");
        return mViewLog;
    }

}
