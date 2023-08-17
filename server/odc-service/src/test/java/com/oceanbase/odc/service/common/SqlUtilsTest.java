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
package com.oceanbase.odc.service.common;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.common.util.SqlUtils;

public class SqlUtilsTest {

    @Test
    public void isCurrentTimestampExpression_CurrentTimestamp_ReturnTrue() {
        boolean matched = SqlUtils.isCurrentTimestampExpression("CURRENT_TIMESTAMP");
        Assert.assertTrue(matched);
    }

    @Test
    public void isCurrentTimestampExpression_Blank_ReturnFalse() {
        boolean matched = SqlUtils.isCurrentTimestampExpression(null);
        Assert.assertFalse(matched);
    }

    @Test
    public void isCurrentTimestampExpression_NotCurrentTimestamp_ReturnFalse() {
        boolean matched = SqlUtils.isCurrentTimestampExpression("self_defined_function()");
        Assert.assertFalse(matched);
    }

    @Test
    public void appendLimit() {
        String sql = "select * from xx";
        String resultSql = SqlUtils.appendLimit(sql, 157);
        Assert.assertEquals("select * from xx limit 157", resultSql);

        sql = "select * from xx limit 200";
        resultSql = SqlUtils.appendLimit(sql, 157);
        Assert.assertEquals(sql, resultSql);
    }

    @Test
    public void appendFetchFirst() {
        String sql = "select * from xx";
        String resultSql = SqlUtils.appendFetchFirst(sql, 157);
        Assert.assertEquals("select * from xx fetch first 157 rows only", resultSql);

        sql = "select * from xx fetch first 200 rows only";
        resultSql = SqlUtils.appendFetchFirst(sql, 157);
        Assert.assertEquals(sql, resultSql);
    }

    @Test
    public void appendRownumCondition() {
        String sql = "select * from xx";
        String resultSql = SqlUtils.appendRownumCondition(sql, 157);
        Assert.assertEquals("select * from xx where rownum <= 157", resultSql);

        sql = "select * from xx where id < 10";
        resultSql = SqlUtils.appendRownumCondition(sql, 157);
        Assert.assertEquals(sql, resultSql);
    }

    @Test
    public void removeComments() {
        String sql = "select xx from table -- this is comment";
        SqlCommentProcessor commentProcessor = new SqlCommentProcessor(DialectType.OB_ORACLE, false, false);
        String resultSql = SqlUtils.removeComments(commentProcessor, sql);
        Assert.assertEquals("select xx from table ", resultSql);
    }

    @Test
    public void addInternalROWIDColumn_WithAlias_AddRowId() {
        String sql = SqlUtils.addInternalROWIDColumn("select t.ROWID, t.* from TEST t");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", t.ROWID, t.* from TEST t", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithStarInSelect_AddRowId() {
        String sql = SqlUtils.addInternalROWIDColumn("select * from TEST;");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", TEST.* from TEST;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithStarInSelectForUpdate_AddRowId() {
        String sql = SqlUtils.addInternalROWIDColumn("select * from TEST for update;");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", TEST.* from TEST for update;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithUpperCaseFrom_AddRowIdSuccess() {
        String sql = SqlUtils.addInternalROWIDColumn("select * FROM TEST for update;");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", TEST.* FROM TEST for update;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithUpperCaseFromAndNullAfterFrom_AddRowIdSuccess() {
        String sql = SqlUtils.addInternalROWIDColumn("select * FROM;");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", .* FROM;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithDollarSign_AddRowIdSuccess() {
        String sql = SqlUtils.addInternalROWIDColumn("select * FROM GV$SQL_AUDIT;");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", GV$SQL_AUDIT.* FROM GV$SQL_AUDIT;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithBackSlash_AddRowIdSuccess() {
        String sql = SqlUtils.addInternalROWIDColumn("select * FROM GV\\SQL_AUDIT;");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", GV\\SQL_AUDIT.* FROM GV\\SQL_AUDIT;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithStarInWhereClause_AddRowIdSuccess() {
        String sql = SqlUtils.addInternalROWIDColumn("select sid FROM GV$SQL_AUDIT WHERE sid='*';");
        Assert.assertEquals("select ROWID AS \"__ODC_INTERNAL_ROWID__\", sid FROM GV$SQL_AUDIT WHERE sid='*';", sql);
    }

    @Test
    public void replaceTemporaryTableName_WithOracleUserPrefix() {
        String sql = "CREATE GLOBAL TEMPORARY TABLE \"TABLE1\"(T1 TABLE1_C1)";
        String s = StringUtils.replace(sql, " TABLE ",
                " TABLE " + StringUtils.quoteOracleIdentifier("SYS") + ".", 1);
        Assert.assertEquals("CREATE GLOBAL TEMPORARY TABLE \"SYS\".\"TABLE1\"(T1 TABLE1_C1)", s);

        sql = "CREATE SHARDED TABLE \"TABLE1\"(T1 TABLE1_C1)";
        s = StringUtils.replace(sql, " TABLE ",
                " TABLE " + StringUtils.quoteOracleIdentifier("SYS") + ".", 1);
        Assert.assertEquals("CREATE SHARDED TABLE \"SYS\".\"TABLE1\"(T1 TABLE1_C1)", s);

    }

}
