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

import static com.oceanbase.tools.dbbrowser.template.OBMySQLMViewLogTemplateTest.generateDbMaterializedViewLog;
import static com.oceanbase.tools.dbbrowser.template.OBMySQLMViewLogTemplateTest.prepareColumns;
import static com.oceanbase.tools.dbbrowser.template.OBMySQLMViewLogTemplateTest.prepareStartAtPurgeSchedule;
import static com.oceanbase.tools.dbbrowser.template.OBMySQLMViewLogTemplateTest.prepareStartNowPurgeSchedule;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeSchedule;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.template.oracle.OBOracleMViewLogTemplate;

/**
 * @description: all tests for {@link OBOracleMViewLogTemplate}
 * @author: zijia.cj
 * @date: 2025/7/16 11:17
 * @since: 4.4.0
 */
public class OBOracleMViewLogTemplateTest {

    private static OBOracleMViewLogTemplate template;

    @BeforeClass
    public static void setUp() {
        template = new OBOracleMViewLogTemplate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateCreateObjectTemplate_TimeUnitIsWeek_ThrowIllegalArgumentException() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        DBMViewLogPurgeSchedule purgeSchedule = new DBMViewLogPurgeSchedule();
        purgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.WEEK);
        mViewLog.setPurgeSchedule(purgeSchedule);
        template.generateCreateObjectTemplate(mViewLog);
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
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name\"""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithColumns_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setColumns(prepareColumns(3));
        String expect = """
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name"
            WITH ("col2","col3","col4")""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithParallelismDegree_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        mViewLog.setPurgeParallelismDegree(5L);
        String expect = """
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name"
            PARALLEL 5""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithStartNowSchedule_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        prepareStartNowPurgeSchedule(mViewLog);
        String expect = """
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name"
            PURGE START WITH CURRENT_DATE
            NEXT CURRENT_DATE + INTERVAL '1' DAY""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generateCreateObjectTemplate_WithStartAtSchedule_Success() {
        DBMaterializedViewLog mViewLog = generateDbMaterializedViewLog();
        prepareStartAtPurgeSchedule(mViewLog);
        String expect = """
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name"
            PURGE START WITH TO_DATE('2025-07-11 18:00:00', 'YYYY-MM-DD HH24:MI:SS')
            NEXT TO_DATE('2025-07-11 18:00:00', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '1' MINUTE""";
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
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name"
            PARALLEL 5
            WITH ("col2","col3","col4")
            PURGE START WITH CURRENT_DATE
            NEXT CURRENT_DATE + INTERVAL '1' DAY""";
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
            CREATE MATERIALIZED VIEW LOG ON "schema_name"."base_table_name"
            PARALLEL 2
            WITH ("col2","col3","col4","col5")
            PURGE START WITH TO_DATE('2025-07-11 18:00:00', 'YYYY-MM-DD HH24:MI:SS')
            NEXT TO_DATE('2025-07-11 18:00:00', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '1' MINUTE""";
        String actual = template.generateCreateObjectTemplate(mViewLog);
        Assert.assertEquals(expect, actual);
    }

}
