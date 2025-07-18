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
package com.oceanbase.tools.dbbrowser.editor;

import static com.oceanbase.tools.dbbrowser.editor.OBMySQLMViewLogEditorTest.generateDBMaterializedViewLog;

import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleMViewLogEditor;
import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeSchedule;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/17 18:38
 * @since: 4.4.0
 */
public class OBOracleMViewLogEditorTest {

    private static OBOracleMViewLogEditor mViewLogEditor;

    @BeforeClass
    public static void setUp() {
        mViewLogEditor = new OBOracleMViewLogEditor();
    }

    @Test(expected = NullPointerException.class)
    public void generateUpdateObjectDDL_StartStrategyIsNull_ThrowNullPointerException() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        DBMViewLogPurgeSchedule newPurgeSchedule = new DBMViewLogPurgeSchedule();
        newMViewLog.setPurgeSchedule(newPurgeSchedule);
        mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateUpdateObjectDDL_TimeUnitIsWeek_ThrowIllegalArgumentException() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        DBMViewLogPurgeSchedule newPurgeSchedule = new DBMViewLogPurgeSchedule();
        newPurgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_NOW);
        newPurgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.WEEK);
        newMViewLog.setPurgeSchedule(newPurgeSchedule);
        mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
    }

    @Test
    public void generateUpdateObjectDDL_EditPurgeParallelismDegree_GenerateSucceed() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        newMViewLog.setPurgeParallelismDegree(2L);
        String ddl = mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
        String expectedDDL = """
            ALTER MATERIALIZED VIEW LOG ON "schema0"."base_table0"
            PARALLEL 2""";
        Assert.assertEquals(expectedDDL, ddl);
    }

    @Test
    public void generateUpdateObjectDDL_EditStartNowPurgeSchedule_GenerateSucceed() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        DBMViewLogPurgeSchedule newPurgeSchedule = new DBMViewLogPurgeSchedule();
        newPurgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_NOW);
        newPurgeSchedule.setInterval(1L);
        newPurgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.DAY);
        newMViewLog.setPurgeSchedule(newPurgeSchedule);
        String ddl = mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
        String expectedDDL = """
            ALTER MATERIALIZED VIEW LOG ON "schema0"."base_table0"
            PURGE START WITH CURRENT_DATE
            NEXT CURRENT_DATE + INTERVAL '1' DAY""";
        Assert.assertEquals(expectedDDL, ddl);
    }

    @Test
    public void generateUpdateObjectDDL_EditStartAtPurgeSchedule_GenerateSucceed() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        DBMViewLogPurgeSchedule newPurgeSchedule = new DBMViewLogPurgeSchedule();
        newPurgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_AT);
        newPurgeSchedule.setInterval(1L);
        newPurgeSchedule.setStartWith(new Date(1762228000000L));
        newPurgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.DAY);
        newMViewLog.setPurgeSchedule(newPurgeSchedule);
        String ddl = mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
        String expectedDDL = """
            ALTER MATERIALIZED VIEW LOG ON "schema0"."base_table0"
            PURGE START WITH TO_DATE('2025-11-04 11:46:40', 'YYYY-MM-DD HH24:MI:SS')
            NEXT TO_DATE('2025-11-04 11:46:40', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '1' DAY""";
        Assert.assertEquals(expectedDDL, ddl);
    }

    @Test
    public void generateUpdateObjectDDL_EditPurgeParallelismDegreeAndStartAtPurgeSchedule_GenerateSucceed() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        newMViewLog.setPurgeParallelismDegree(2L);
        DBMViewLogPurgeSchedule newPurgeSchedule = new DBMViewLogPurgeSchedule();
        newPurgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_AT);
        newPurgeSchedule.setInterval(1L);
        newPurgeSchedule.setStartWith(new Date(1762228000000L));
        newPurgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.DAY);
        newMViewLog.setPurgeSchedule(newPurgeSchedule);
        String ddl = mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
        String expectedDDL = """
            ALTER MATERIALIZED VIEW LOG ON "schema0"."base_table0"
            PARALLEL 2,
            PURGE START WITH TO_DATE('2025-11-04 11:46:40', 'YYYY-MM-DD HH24:MI:SS')
            NEXT TO_DATE('2025-11-04 11:46:40', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '1' DAY""";
        Assert.assertEquals(expectedDDL, ddl);
    }

    @Test
    public void generateUpdateObjectDDL_EditPurgeParallelismDegreeAndStartNowPurgeSchedule_GenerateSucceed() {
        DBMaterializedViewLog oldMViewLog = generateDBMaterializedViewLog();
        DBMaterializedViewLog newMViewLog = generateDBMaterializedViewLog();
        newMViewLog.setPurgeParallelismDegree(2L);
        DBMViewLogPurgeSchedule newPurgeSchedule = new DBMViewLogPurgeSchedule();
        newPurgeSchedule.setStartStrategy(DBMViewLogPurgeSchedule.StartStrategy.START_NOW);
        newPurgeSchedule.setInterval(1L);
        newPurgeSchedule.setUnit(DBMViewLogPurgeSchedule.TimeUnit.DAY);
        newMViewLog.setPurgeSchedule(newPurgeSchedule);
        String ddl = mViewLogEditor.generateUpdateObjectDDL(oldMViewLog, newMViewLog);
        String expectedDDL = """
            ALTER MATERIALIZED VIEW LOG ON "schema0"."base_table0"
            PARALLEL 2,
            PURGE START WITH CURRENT_DATE
            NEXT CURRENT_DATE + INTERVAL '1' DAY""";
        Assert.assertEquals(expectedDDL, ddl);
    }

}
