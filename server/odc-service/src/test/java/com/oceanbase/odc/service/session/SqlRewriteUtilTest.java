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
package com.oceanbase.odc.service.session;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.service.session.util.SqlRewriteUtil;

public class SqlRewriteUtilTest {

    @Test
    public void addInternalROWIDColumn_WithAlias_AddRowId() {
        String sql = addInternalRowIdColumn("select t.ROWID, t.* from TEST t");
        Assert.assertEquals("select t.ROWID, t.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  from TEST t", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithStarInSelect_AddRowId() {
        String sql = addInternalRowIdColumn("select * from TEST;");
        Assert.assertEquals(
                "select TEST.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  from TEST;",
                sql);
    }

    @Test
    public void addInternalROWIDColumn_WithUnPivot_AddRowIdFailed() {
        String expect = "select 姓名,科目,成绩 from score unpivot ( 成绩 for 科目 in ( 语文, 数学, 英语 ) );";
        String sql = addInternalRowIdColumn(expect);
        Assert.assertEquals(expect, sql);
    }

    @Test
    public void addInternalROWIDColumn_WithPivot_AddRowIdFailed() {
        String expect =
                "select 姓名,科目,成绩 from score pivot(count(*) as alias_1, APPROX_COUNT_DISTINCT(1,2) for col1 in (col2 as alias_4, col2 alias_3, col3)) ooo;";
        String sql = addInternalRowIdColumn(expect);
        Assert.assertEquals(expect, sql);
    }

    @Test
    public void addInternalROWIDColumn_subQueryWithUnPivot_AddRowIdFailed() {
        String expect = "select 姓名,科目,成绩 from (select * from score) unpivot ( 成绩 for 科目 in ( 语文, 数学, 英语 ) );";
        String sql = addInternalRowIdColumn(expect);
        Assert.assertEquals(expect, sql);
    }

    @Test
    public void addInternalROWIDColumn_WithStarInSelectForUpdate_AddRowId() {
        String sql = addInternalRowIdColumn("select * from TEST for update;");
        Assert.assertEquals("select TEST.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  from TEST for update;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithUpperCaseFrom_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select * FROM TEST for update;");
        Assert.assertEquals("select TEST.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  FROM TEST for update;", sql);
    }

    @Test(expected = IllegalStateException.class)
    public void addInternalROWIDColumn_WithUpperCaseFromAndNullAfterFrom_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select * FROM;");
    }

    @Test
    public void addInternalROWIDColumn_WithDollarSign_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select * FROM GV$SQL_AUDIT;");
        Assert.assertEquals("select GV$SQL_AUDIT.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  FROM GV$SQL_AUDIT;", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithBackSlash_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select * FROM \"GV\\SQL_AUDIT\";");
        Assert.assertEquals("select \"GV\\SQL_AUDIT\".*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  FROM \"GV\\SQL_AUDIT\";",
                sql);
    }

    @Test
    public void addInternalROWIDColumn_WithStarInWhereClause_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select sid FROM GV$SQL_AUDIT WHERE sid='*';");
        Assert.assertEquals("select sid, ROWID AS \"__ODC_INTERNAL_ROWID__\"  FROM GV$SQL_AUDIT WHERE sid='*';", sql);
    }

    @Test
    public void addInternalROWIDColumn_StarWithinSelect_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select* FROM GV$SQL_AUDIT WHERE sid='*';");
        Assert.assertEquals(
                "selectGV$SQL_AUDIT.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  FROM GV$SQL_AUDIT WHERE sid='*';", sql);
    }

    @Test
    public void addInternalROWIDColumn_FromSelectBody_AddRowIdFail() {
        String sql = addInternalRowIdColumn("select * from (select 1 from dual)");
        Assert.assertEquals("select * from (select 1 from dual)", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithAlias_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select * from GV$SQL_AUDIT g");
        Assert.assertEquals("select g.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  from GV$SQL_AUDIT g", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithSchema_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select * from SYS.GV$SQL_AUDIT");
        Assert.assertEquals("select SYS.GV$SQL_AUDIT.*, ROWID AS \"__ODC_INTERNAL_ROWID__\"  from SYS.GV$SQL_AUDIT",
                sql);
    }

    @Test
    public void addInternalROWIDColumn_WithMultiTable_AddRowIdFail() {
        String sql = addInternalRowIdColumn("select * from a, b");
        Assert.assertEquals("select * from a, b", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithHint_AddRowIdSuccess() {
        String sql = addInternalRowIdColumn("select /*+ monitor */ id from test");
        Assert.assertEquals("select /*+ monitor */ id, ROWID AS \"__ODC_INTERNAL_ROWID__\"  from test", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithDistinct_AddRowIdFail() {
        String sql = addInternalRowIdColumn("select distinct val from test t1");
        Assert.assertEquals("select distinct val from test t1", sql);
    }

    @Test
    public void addInternalROWIDColumn_WithDBlink_AddRowIdFailed() {
        String expect = "select * from aaa@bbb;";
        String sql = addInternalRowIdColumn(expect);
        Assert.assertEquals(expect, sql);
    }

    private String addInternalRowIdColumn(String sql) {
        return SqlRewriteUtil.addInternalRowIdColumn(sql,
                AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_ORACLE, 0).buildAst(sql));
    }

}
