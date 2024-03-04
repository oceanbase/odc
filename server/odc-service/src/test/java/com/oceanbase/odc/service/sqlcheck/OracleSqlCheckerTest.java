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
package com.oceanbase.odc.service.sqlcheck;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.ColumnCharsetExists;
import com.oceanbase.odc.service.sqlcheck.rule.ColumnCollationExists;
import com.oceanbase.odc.service.sqlcheck.rule.ColumnNameInBlackList;
import com.oceanbase.odc.service.sqlcheck.rule.ForeignConstraintExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoDefaultValueExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoIndexNameExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoPrimaryKeyExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoPrimaryKeyNameExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoSpecificColumnExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoValidWhereClause;
import com.oceanbase.odc.service.sqlcheck.rule.NoWhereClauseExists;
import com.oceanbase.odc.service.sqlcheck.rule.NotNullColumnWithoutDefaultValue;
import com.oceanbase.odc.service.sqlcheck.rule.OracleColumnCalculation;
import com.oceanbase.odc.service.sqlcheck.rule.OracleLeftFuzzyMatch;
import com.oceanbase.odc.service.sqlcheck.rule.OracleMissingRequiredColumns;
import com.oceanbase.odc.service.sqlcheck.rule.OracleNoColumnCommentExists;
import com.oceanbase.odc.service.sqlcheck.rule.OracleNoNotNullAtInExpression;
import com.oceanbase.odc.service.sqlcheck.rule.OracleNoTableCommentExists;
import com.oceanbase.odc.service.sqlcheck.rule.OracleRestrictColumnNameCase;
import com.oceanbase.odc.service.sqlcheck.rule.OracleRestrictIndexDataTypes;
import com.oceanbase.odc.service.sqlcheck.rule.OracleRestrictPKDataTypes;
import com.oceanbase.odc.service.sqlcheck.rule.OracleRestrictTableNameCase;
import com.oceanbase.odc.service.sqlcheck.rule.OracleTooManyAlterStatement;
import com.oceanbase.odc.service.sqlcheck.rule.PreferLocalOutOfLineIndex;
import com.oceanbase.odc.service.sqlcheck.rule.ProhibitedDatatypeExists;
import com.oceanbase.odc.service.sqlcheck.rule.RestrictColumnNotNull;
import com.oceanbase.odc.service.sqlcheck.rule.RestrictDropObjectTypes;
import com.oceanbase.odc.service.sqlcheck.rule.RestrictIndexNaming;
import com.oceanbase.odc.service.sqlcheck.rule.RestrictPKNaming;
import com.oceanbase.odc.service.sqlcheck.rule.RestrictUniqueIndexNaming;
import com.oceanbase.odc.service.sqlcheck.rule.SelectStarExists;
import com.oceanbase.odc.service.sqlcheck.rule.SqlCheckRules;
import com.oceanbase.odc.service.sqlcheck.rule.TableNameInBlackList;
import com.oceanbase.odc.service.sqlcheck.rule.TooLongCharLength;
import com.oceanbase.odc.service.sqlcheck.rule.TooManyColumnDefinition;
import com.oceanbase.odc.service.sqlcheck.rule.TooManyColumnRefInIndex;
import com.oceanbase.odc.service.sqlcheck.rule.TooManyColumnRefInPrimaryKey;
import com.oceanbase.odc.service.sqlcheck.rule.TooManyInExpression;
import com.oceanbase.odc.service.sqlcheck.rule.TooManyOutOfLineIndex;
import com.oceanbase.odc.service.sqlcheck.rule.TooManyTableJoin;

/**
 * {@link OracleSqlCheckerTest}
 *
 * @author yh263208
 * @date 2022-12-16 16:57
 * @since ODC_release_4.1.0
 */
public class OracleSqlCheckerTest {

    @Test
    public void check_sqlWithColumnLeftCalculation_violationGenerated() {
        String sqls = "select col from tab where 1+3>4 and (4+5<90 or id+3<50 and (4+5=9)) and id='12'";
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, ";",
                Collections.singletonList(new OracleColumnCalculation()));
        List<CheckViolation> actual = sqlChecker.check(sqls);

        CheckViolation c =
                new CheckViolation(sqls, 1, 47, 47, 53, SqlCheckRuleType.INDEX_COLUMN_CALCULATION, new Object[] {});
        List<CheckViolation> expect = Collections.singletonList(c);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithColumnLeftCalculation_violationGenerated() {
        String[] sqls = new String[] {
                "select col from tab where 1+3>4 and (4+5<90 or id+3<50 and (4+5=9)) and id='12'",
                "select col from \ntab where ((id+4)+5) *5 not like '12'"
        };
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, ";",
                Collections.singletonList(new OracleColumnCalculation()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, ";"));

        SqlCheckRuleType type = SqlCheckRuleType.INDEX_COLUMN_CALCULATION;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 47, 47, 53, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 2, 10, 27, 53, type, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithFuzzyMatch_violationGenerated() {
        String[] sqls = new String[] {
                "select col from tab where 1+3>4 and (col like '%abc') and id='12'",
                "select col from tab where 5+6<90 or col like '%xxxx%' escape 'abcdd'",
                "select col from \ntab where ((8+4)+5) *5 not like '12%'"
        };
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleLeftFuzzyMatch()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.INDEX_COLUMN_FUZZY_MATCH;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 37, 37, 51, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 36, 36, 67, type, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithTooManyInExprs_violationGenerated() {
        String[] sqls = new String[] {
                "select col from tab where 1+3>4 and col in ('abc','abc','abc','abc') or col not in ('abc','abc','abc','abc')",
                "select col from tab where 5+6<90 or col not in ('abc','abc','abc')",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TooManyInExpression(4)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_IN_EXPR;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 36, 36, 67, type, new Object[] {4, 4});
        CheckViolation c2 = new CheckViolation(sqls[0], 1, 72, 72, 107, type, new Object[] {4, 4});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithTooManyJoins_violationGenerated() {
        String[] sqls = new String[] {
                "select col from ab join b using (col, col) join b using (ccc, bbb) join b using (col, col)",
                "select col from ab join b using (col, col) join b using (ccc, bbb) union all select col from ab join b using (col, col) join b using (ccc, bbb) join b using (col, col)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TooManyTableJoin(4)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_TABLE_JOIN;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 16, 16, 89, type, new Object[] {4, 4});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 93, 93, 166, type, new Object[] {4, 4});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithNotNullAtNotInExpr_violationGenerated() {
        String[] sqls = new String[] {
                "select col from tab where col not in (select col from aaa where aaa.col is not null)",
                "select col from tab where col not in (1,2,3,null)",
                "select col from tab where col not in (select col from aaa where col_1 is not null)"
        };
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleNoNotNullAtInExpression()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_NOT_NULL_EXISTS_NOT_IN;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 26, 26, 48, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 26, 26, 81, type, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_updateWithoutIllegalWhereClause_violationGenerated() {
        String[] sqls = new String[] {
                "update tab set col=1",
                "update tab set col=1 where col=4 and 4+5=9",
                "update tab set col=1 where col=4"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoValidWhereClause()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_VALID_WHERE_CLAUSE;
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 37, 37, 41, type, new Object[] {});
        List<CheckViolation> expect = Collections.singletonList(c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_updateWithoutWhereClause_violationGenerated() {
        String[] sqls = new String[] {
                "update tab set col=1",
                "update tab set col=1 where col=4 and 4+5=9",
                "update tab set col=1 where col=4"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoWhereClauseExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_WHERE_CLAUSE_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 19, type, new Object[] {});
        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_deleteWithoutIllegalWhereClause_violationGenerated() {
        String[] sqls = new String[] {
                "delete from tab",
                "delete from tab where col=4 and 4+5=9",
                "delete from tab where col=4"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoValidWhereClause()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_VALID_WHERE_CLAUSE;
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 32, 32, 36, type, new Object[] {});
        List<CheckViolation> expect = Collections.singletonList(c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_deleteWithoutWhereClause_violationGenerated() {
        String[] sqls = new String[] {
                "delete from tab",
                "delete from tab where col=4 and 4+5=9",
                "delete from tab where col=4"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoWhereClauseExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_WHERE_CLAUSE_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 14, type, new Object[] {});
        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_insertWithIllegalColumnRefs_violationGenerated() {
        String[] sqls = new String[] {
                "insert into tab values(1,2,3)",
                "insert into tab(col, col2) values(1,2,3)",
                "insert all "
                        + "when 1+3 then into tab(col, col1) values(1,2) into tab1 values(3,4) "
                        + "when 1+3 then into tab5(tab.col, col122) values(1,2) "
                        + "else into tab7(tab.col1, col12) values(1,2) select 1 from dual"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoSpecificColumnExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_SPECIFIC_COLUMN_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 7, 7, 28, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 57, 57, 77, type, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tooManyColumnDefs_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\" (id varchar(64), content integer)",
                "create table \"abcd\" (id varchar(64), content integer, age varchar(32))",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TooManyColumnDefinition(3)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_COLUMNS;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 54, 54, 68, type, new Object[] {"abcd", 3, 3});
        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tooManyIndexes_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\" (id varchar(64), index i_a(id) local, index i_b(id) global, index i_c(id) BLOCK_SIZE=200)",
                "create table \"abcd\" (id varchar(64), content integer, age varchar(32))",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Arrays.asList(new TooManyOutOfLineIndex(2), new PreferLocalOutOfLineIndex()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_INDEX_KEYS;
        SqlCheckRuleType type1 = SqlCheckRuleType.PREFER_LOCAL_INDEX;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 80, 80, 107, type, new Object[] {"abcd", 2});
        CheckViolation c2 = new CheckViolation(sqls[0], 1, 58, 58, 77, type1, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[0], 1, 80, 80, 107, type1, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tooLongVarchar_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\" (id char(1001), index i_a(id) local, index i_b(id) global, index i_c(id) BLOCK_SIZE=200)",
                "create table \"abcd\" (id char(1000), content integer, age char(32))",
                "alter table abcd add (id char(1000), id1 char(1005), id2 blob)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TooLongCharLength(1000)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_LONG_CHAR_LENGTN;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 24, 24, 33, type, new Object[] {1000, "1001"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 41, 41, 50, type, new Object[] {1000, "1005"});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_uniqueIndexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE aaaa (ID VARCHAR2(64), CONSTRAINT \"UK_AAA\" UNIQUE (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), CONSTRAINT \"AAA\" UNIQUE (ID))",
                "create unique index chz.\"u_aaa\" on ttt(name)",
                "alter table test_unique_tb add constraint uk_constraint_utu unique(name)",
                "alter table test_unique_tb add constraint aaassf unique(name)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictUniqueIndexNaming("uk_\\w+")));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_UNIQUE_INDEX_NAMING;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 54, type, new Object[] {"aaa", "uk_\\w+"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 36, 36, 63, type, new Object[] {"\"AAA\"", "uk_\\w+"});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 0, 0, 43, type, new Object[] {"\"u_aaa\"", "uk_\\w+"});
        CheckViolation c4 = new CheckViolation(sqls[5], 1, 31, 31, 60, type, new Object[] {"aaassf", "uk_\\w+"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_defaultUniqueIndexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE aaaa (ID VARCHAR2(64), CONSTRAINT \"UK_aaaa_id\" UNIQUE (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), CONSTRAINT \"AAA\" UNIQUE (ID))",
                "create unique index chz.\"u_aaa\" on ttt(name)",
                "alter table test_unique_tb add constraint uk_test_unique_tb_name unique(name)",
                "alter table test_unique_tb add constraint aaassf unique(name)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictUniqueIndexNaming(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_UNIQUE_INDEX_NAMING;
        String pattern = "uk_${table-name}_${column-name-1}_${column-name-2}_...";
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 54, type, new Object[] {"aaa", pattern});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 36, 36, 63, type, new Object[] {"\"AAA\"", pattern});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 0, 0, 43, type, new Object[] {"\"u_aaa\"", pattern});
        CheckViolation c4 = new CheckViolation(sqls[5], 1, 31, 31, 60, type, new Object[] {"aaassf", pattern});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_pkNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64) primary key)",
                "create table aaaa(id varchar2(64) constraint aaa primary key)",
                "CREATE TABLE aaaa (ID VARCHAR2(64), CONSTRAINT \"pk_aaaa_id\" primary key (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), CONSTRAINT \"AAA\" primary key (ID))",
                "alter table test_pk_tb add constraint pk_test_pk_tb_name primary key(name)",
                "alter table test_pk_tb add constraint aaassf primary key(name)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictPKNaming("pk_\\w+")));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_NAMING;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 34, 34, 59, type, new Object[] {"aaa", "pk_\\w+"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 36, 36, 68, type, new Object[] {"\"AAA\"", "pk_\\w+"});
        CheckViolation c3 = new CheckViolation(sqls[5], 1, 27, 27, 61, type, new Object[] {"aaassf", "pk_\\w+"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_defaultPKNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64) primary key)",
                "create table aaaa(id varchar2(64) constraint aaa primary key)",
                "CREATE TABLE aaaa (ID VARCHAR2(64), CONSTRAINT \"pk_aaaa_id\" primary key (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), CONSTRAINT \"AAA\" primary key (ID))",
                "alter table test_pk_tb add constraint pk_test_pk_tb_name primary key(name)",
                "alter table test_pk_tb add constraint aaassf primary key(name)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictPKNaming(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_NAMING;
        String pattern = "pk_${table-name}_${column-name-1}_${column-name-2}_...";
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 34, 34, 59, type, new Object[] {"aaa", pattern});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 36, 36, 68, type, new Object[] {"\"AAA\"", pattern});
        CheckViolation c3 = new CheckViolation(sqls[5], 1, 27, 27, 61, type, new Object[] {"aaassf", pattern});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_foreignKeyExists_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE aaaa (ID VARCHAR2(64) constraint fk_c references a.b (id) on delete cascade)",
                "CREATE TABLE bbbb (ID VARCHAR2(64), CONSTRAINT \"AAA\" foreign key (a,c) references a.b(id) on delete set null)",
                "ALTER TABLE TB ADD CONSTRAINT ABC FOREIGN KEY(C1,C2) REFERENCES A.B(C1, C2)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new ForeignConstraintExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.FOREIGN_CONSTRAINT_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 35, 35, 87, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 36, 36, 107, type, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 15, 15, 74, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noPrimaryKeyExists_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE aaaa (ID VARCHAR2(64) constraint pk_name primary key)",
                "CREATE TABLE bbbb (ID VARCHAR2(64), primary key(id, id1) using index)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoPrimaryKeyExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_PRIMARY_KEY_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 55, type, new Object[] {});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tableNameInBlackList_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\"(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE uuiioo(ID VARCHAR2(64) constraint pk_name primary key)",
                "alter table \"abcd\" rename to abcd",
                "alter table \"abcd\" rename to kkkk",
                "rename a.b to chz.ABCD"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TableNameInBlackList(Collections.singleton("abcd"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TABLE_NAME_IN_BLACK_LIST;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 57, type, new Object[] {"\"abcd\"", "abcd"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 29, 29, 32, type, new Object[] {"abcd", "abcd"});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 14, 14, 21, type, new Object[] {"ABCD", "abcd"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_colRefsInIndex_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64), "
                        + "index idx_name(col1, col2, col3), "
                        + "index idx_name_1(col1, col2), "
                        + "constraint abc unique (col1, col2, col3), "
                        + "primary key(col1, col2))",
                "alter table abcd add constraint abcd primary key(col1, col2)",
                "alter table abcd add constraint abc unique (col1, col2, col3)",
                "create index idx_name on chz.tb(col1, col2, col3)",
                "create index idx_name on chz.tb(col1, col2)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TooManyColumnRefInIndex(2)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_COL_REFS_IN_INDEX;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 65, type, new Object[] {3, 2});
        CheckViolation c2 = new CheckViolation(sqls[0], 1, 98, 98, 137, type, new Object[] {3, 2});
        CheckViolation c3 = new CheckViolation(sqls[2], 1, 17, 17, 60, type, new Object[] {3, 2});
        CheckViolation c4 = new CheckViolation(sqls[3], 1, 0, 0, 48, type, new Object[] {3, 2});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictPrimaryKeyType_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd("
                        + "id varchar2(64) primary key,"
                        + "name blob,"
                        + "\"age\" number(5,3),"
                        + "constraint abcd_pk primary key(name, \"age\"))",
                "alter table abcd add primary key (NAME)",
                "alter table abcd add primary key (age)",
                "alter table \"abcd\" add primary key (NAME)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(sqls[0]);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictPKDataTypes(jdbcTemplate,
                        new HashSet<>(Arrays.asList("int", "number")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_DATATYPES;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 32, type, new Object[] {"varchar2", "number,int"});
        CheckViolation c2 = new CheckViolation(sqls[0], 1, 105, 105, 108, type, new Object[] {"blob", "number,int"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 34, 34, 37, type, new Object[] {"blob", "number,int"});
        CheckViolation c4 = new CheckViolation(sqls[3], 1, 36, 36, 39, type, new Object[] {"blob", "number,int"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictIndexType_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd (\n"
                        + "id       varchar2(64) primary key,\n"
                        + "name     blob         unique,\n"
                        + "\"age\"  number(5,3)  unique,\n"
                        + "age_t    clob         constraint pk primary key,\n"
                        + "constraint abcd_pk1 primary key(name, \"age\"),\n"
                        + "constraint abcd_pk2 primary key(id, \"age\"),\n"
                        + "constraint abcd_pk3 primary key(name, \"AGE_T\"),\n"
                        + "unique (name, \"age\"),\n"
                        + "unique (id, \"age\"),\n"
                        + "unique (name, Age_T),\n"
                        + "index(id, \"age\"),\n"
                        + "index(name, age_t))",
                "alter table abcd add primary key (name)",
                "alter table abcd add primary key (\"age\")",
                "alter table abcd add unique (name, age_t)",
                "alter table abcd add unique (\"age\")",
                "create index abcd_idx1 on abcd(id)",
                "create index abcd_idx2 on abcd(id, name)",
                "create index abcd_idx3 on abcd(age_t, name)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(sqls[0]);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictIndexDataTypes(jdbcTemplate,
                        new HashSet<>(Arrays.asList("varchar2", "number")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_DATATYPES;
        CheckViolation c1 =
                new CheckViolation(sqls[0], 3, 9, 64, 67, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c2 =
                new CheckViolation(sqls[0], 5, 9, 122, 125, type, new Object[] {"age_t", "clob", "number,varchar2"});
        CheckViolation c3 =
                new CheckViolation(sqls[0], 6, 32, 194, 197, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c4 =
                new CheckViolation(sqls[0], 8, 32, 284, 287, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c5 = new CheckViolation(sqls[0], 8, 38, 290, 296, type,
                new Object[] {"\"AGE_T\"", "clob", "number,varchar2"});
        CheckViolation c6 =
                new CheckViolation(sqls[0], 9, 8, 308, 311, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c7 =
                new CheckViolation(sqls[0], 11, 8, 350, 353, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c8 =
                new CheckViolation(sqls[0], 11, 14, 356, 360, type, new Object[] {"Age_T", "clob", "number,varchar2"});
        CheckViolation c9 =
                new CheckViolation(sqls[0], 13, 6, 388, 391, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c10 =
                new CheckViolation(sqls[0], 13, 12, 394, 398, type, new Object[] {"age_t", "clob", "number,varchar2"});

        CheckViolation c11 =
                new CheckViolation(sqls[1], 1, 34, 34, 37, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c12 =
                new CheckViolation(sqls[3], 1, 29, 29, 32, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c13 =
                new CheckViolation(sqls[3], 1, 35, 35, 39, type, new Object[] {"age_t", "clob", "number,varchar2"});

        CheckViolation c14 =
                new CheckViolation(sqls[6], 1, 35, 35, 38, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c15 =
                new CheckViolation(sqls[7], 1, 31, 31, 35, type, new Object[] {"age_t", "clob", "number,varchar2"});
        CheckViolation c16 =
                new CheckViolation(sqls[7], 1, 38, 38, 41, type, new Object[] {"name", "blob", "number,varchar2"});

        List<CheckViolation> expect =
                Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictIndexType1_violationGenerated() {
        String ddl = "create table abcd (\n"
                + "id       varchar2(64) primary key,\n"
                + "name     blob         unique,\n"
                + "\"age\"  number(5,3)  unique,\n"
                + "age_t    clob         constraint pk primary key,\n"
                + "constraint abcd_pk1 primary key(name, \"age\"),\n"
                + "constraint abcd_pk2 primary key(id, \"age\"),\n"
                + "constraint abcd_pk3 primary key(name, \"AGE_T\"),\n"
                + "unique (name, \"age\"),\n"
                + "unique (id, \"age\"),\n"
                + "unique (name, Age_T),\n"
                + "index(id, \"age\"),\n"
                + "index(name, age_t))";
        String[] sqls = new String[] {
                "alter table abcd add primary key (name)",
                "alter table abcd add primary key (\"age\")",
                "alter table abcd add unique (name, age_t)",
                "alter table abcd add unique (\"age\")",
                "create index abcd_idx1 on abcd(id)",
                "create index abcd_idx2 on abcd(id, name)",
                "create index abcd_idx3 on abcd(age_t, name)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(ddl);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictIndexDataTypes(jdbcTemplate,
                        new HashSet<>(Arrays.asList("varchar2", "number")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_DATATYPES;
        CheckViolation c11 =
                new CheckViolation(sqls[0], 1, 34, 34, 37, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c12 =
                new CheckViolation(sqls[2], 1, 29, 29, 32, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c13 =
                new CheckViolation(sqls[2], 1, 35, 35, 39, type, new Object[] {"age_t", "clob", "number,varchar2"});

        CheckViolation c14 =
                new CheckViolation(sqls[5], 1, 35, 35, 38, type, new Object[] {"name", "blob", "number,varchar2"});
        CheckViolation c15 =
                new CheckViolation(sqls[6], 1, 31, 31, 35, type, new Object[] {"age_t", "clob", "number,varchar2"});
        CheckViolation c16 =
                new CheckViolation(sqls[6], 1, 38, 38, 41, type, new Object[] {"name", "blob", "number,varchar2"});

        List<CheckViolation> expect = Arrays.asList(c11, c12, c13, c14, c15, c16);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_colRefsInPK_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64), "
                        + "constraint abc primary key (col1, col2, col3), "
                        + "primary key(col1, col2))",
                "alter table abcd add constraint abcd primary key(col1, col2)",
                "alter table abcd add constraint abc primary key (col1, col2, col3)",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new TooManyColumnRefInPrimaryKey(2)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_COL_REFS_IN_PRIMARY_KEY;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 78, type, new Object[] {3, 2});
        CheckViolation c3 = new CheckViolation(sqls[2], 1, 17, 17, 65, type, new Object[] {3, 2});

        List<CheckViolation> expect = Arrays.asList(c1, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_indexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64), index using btree (id))",
                "CREATE TABLE aaaa (ID VARCHAR2(64), index \"idx_AAA\" (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), index \"AAA\" (ID))",
                "create index chz.\"u_aaa\" on ttt(name)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictIndexNaming("idx_\\w+")));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_NAMING;
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 36, 36, 51, type, new Object[] {"\"AAA\"", "idx_\\w+"});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 0, 0, 36, type, new Object[] {"\"u_aaa\"", "idx_\\w+"});

        List<CheckViolation> expect = Arrays.asList(c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_defaultIndexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64), index using btree (id))",
                "CREATE TABLE aaaa (ID VARCHAR2(64), index \"idx_aaaa_ID\" (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), index \"AAA\" (ID))",
                "create index chz.\"u_aaa\" on ttt(name)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictIndexNaming(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_NAMING;
        String pattern = "idx_${table-name}_${column-name-1}_${column-name-2}_...";
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 36, 36, 51, type, new Object[] {"\"AAA\"", pattern});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 0, 0, 36, type, new Object[] {"\"u_aaa\"", pattern});

        List<CheckViolation> expect = Arrays.asList(c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noIndexName_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar2(64), index using btree (id))",
                "create table abcdder(id varchar2(64) unique, id blob primary key, content varchar(64) constraint ancd unique)",
                "CREATE TABLE aaaa (ID VARCHAR2(64), index \"idx_AAA\" (ID))",
                "CREATE TABLE bbbb (ID VARCHAR2(64), primary key(id), constraint aaaaa primary key (dd))",
                "CREATE TABLE rrff5 (ID VARCHAR2(64), unique (id), constraint aaaaa unique (dd), check(12))",
                "alter table test_unique_tb add unique (id1), add constraint abcde unique (id3)",
                "alter table test_unique_tb add check(1), add primary key(id5), add constraint aaa primary key(id6)",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoIndexNameExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_INDEX_NAME_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 35, 35, 56, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 37, 37, 42, type, new Object[] {});
        CheckViolation c5 = new CheckViolation(sqls[4], 1, 37, 37, 47, type, new Object[] {});
        CheckViolation c6 = new CheckViolation(sqls[5], 1, 27, 27, 42, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c5, c6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noPrimaryKeyName_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcdder(id varchar2(64) unique, id blob primary key, content varchar(64) constraint ancd unique)",
                "CREATE TABLE bbbb (ID VARCHAR2(64), primary key(id), constraint aaaaa primary key (dd))",
                "alter table test_unique_tb add check(1), add primary key(id5), add constraint aaa primary key(id6)",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoPrimaryKeyNameExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_PRIMARY_KEY_NAME_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 53, 53, 63, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 36, 36, 50, type, new Object[] {});
        CheckViolation c5 = new CheckViolation(sqls[2], 1, 41, 41, 60, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c5);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnCharset_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) charset utf8)",
                "create table aaaa1(id varchar(64))",
                "alter table accd add id varchar(64) charset utf8"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new ColumnCharsetExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_CHARSET_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 44, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 24, 24, 47, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnCollation_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) collate utf8mb4)",
                "create table aaaa1(id varchar(64))",
                "alter table abcd add id varchar(64) collate utf8mb4"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new ColumnCollationExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_COLLATION_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 47, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 24, 24, 50, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnIsNullable_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), col1 blob, col2 number not null)",
                "alter table abdcd add id number default 123, add id1 nchar(23) null, add col1 blob, add col2 number not null"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new RestrictColumnNotNull(new HashSet<>(Collections.singletonList("blob")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_IS_NULLABLE;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 31, type, new Object[] {"varchar(64)", "blob"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 22, 22, 42, type, new Object[] {"number", "blob"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 49, 49, 66, type, new Object[] {"nchar(23)", "blob"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noDefaultValue_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), col1 blob, col2 number default 123)",
                "alter table abdcd add id number unique, add id1 nchar(23) null, add col1 blob, add col2 number default 567"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new NoDefaultValueExists(new HashSet<>(Collections.singletonList("blob")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_DEFAULT_VALUE_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 31, type, new Object[] {"varchar(64)", "blob"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 22, 22, 37, type, new Object[] {"number", "blob"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 44, 44, 61, type, new Object[] {"nchar(23)", "blob"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnNameInBlackList_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\"(\"abcd\" varchar2(64) constraint aaa unique)",
                "CREATE TABLE uuiioo(uuiioo VARCHAR2(64) constraint pk_name primary key)",
                "alter table \"abcd\" add abcd varchar2(64)",
                "alter table \"abcd\" rename column aaaaa to AbCd"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new ColumnNameInBlackList(Collections.singleton("abcd"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_NAME_IN_BLACK_LIST;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 20, 20, 25, type, new Object[] {"\"abcd\"", "abcd"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 23, 23, 26, type, new Object[] {"abcd", "abcd"});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 19, 19, 45, type, new Object[] {"AbCd", "abcd"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictColumnUppercase_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\"(\"abcd\" varchar2(64) constraint aaa unique)",
                "CREATE TABLE uuiioo(iiuuy VARCHAR2(64) constraint pk_name primary key)",
                "alter table \"abcd\" add \"abcd\" varchar2(64)",
                "alter table \"abcd\" rename column aaaaa to AbCd"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictColumnNameCase(false, true)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_COLUMN_NAME_CASE;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 20, 20, 25, type, new Object[] {"\"abcd\""});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 23, 23, 28, type, new Object[] {"\"abcd\""});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictColumnLowercase_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\"(\"abcd\" varchar2(64) constraint aaa unique)",
                "CREATE TABLE uuiioo(iiuuy VARCHAR2(64) constraint pk_name primary key)",
                "alter table \"abcd\" add \"abcd\" varchar2(64)",
                "alter table \"abcd\" rename column aaaaa to AbCd"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictColumnNameCase(true, null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_COLUMN_NAME_CASE;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 20, 20, 24, type, new Object[] {"iiuuy"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 19, 19, 45, type, new Object[] {"AbCd"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tableNameUppercase_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\"(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE uuiioo(ID VARCHAR2(64) constraint pk_name primary key)",
                "alter table \"abcd\" rename to abcd",
                "alter table \"abcd\" rename to \"kkkk\"",
                "rename a.b to chz.ABCD"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictTableNameCase(null, true)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_TABLE_NAME_CASE;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 57, type, new Object[] {"\"abcd\""});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 29, 29, 34, type, new Object[] {"\"kkkk\""});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tableNameLowercase_violationGenerated() {
        String[] sqls = new String[] {
                "create table \"abcd\"(id varchar2(64) constraint aaa unique)",
                "CREATE TABLE uuiioo(ID VARCHAR2(64) constraint pk_name primary key)",
                "alter table \"abcd\" rename to abcd",
                "alter table \"abcd\" rename to \"kkkk\"",
                "rename a.b to chz.ABCD"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                Collections.singletonList(new OracleRestrictTableNameCase(true, false)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_TABLE_NAME_CASE;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 0, 0, 66, type, new Object[] {"uuiioo"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 29, 29, 32, type, new Object[] {"abcd"});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 14, 14, 21, type, new Object[] {"ABCD"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_selectStarExists_violationGenerated() {
        String[] sqls = {
                "select a.b, * from tb",
                "select a from tb union all select tb1.* from tb1",
                "select 1+1 from dual"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(new SelectStarExists()));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.SELECT_STAR_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 12, 12, 12, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 34, 34, 38, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_requireColumns_violationGenerated() {
        String[] sqls = {
                "create table abcd(ID int, col1 varchar(64))",
                "create table abcd(ID int, create_gmt timestamp(6))"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(
                        new OracleMissingRequiredColumns(new HashSet<>(Arrays.asList("id", "\"create_gmt\"")))));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.MISSING_REQUIRED_COLUMNS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 42, type, new Object[] {"\"create_gmt\""});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 0, 0, 49, type, new Object[] {"\"create_gmt\""});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_drop_violationGenerated() {
        String[] sqls = {
                "create table abcd(ID int, col1 varchar(64))",
                "drop procedure abcd",
                "drop function abcd",
                "alter table ansdnd drop primary key, drop column asbdasd.asdas",
                "alter table asdasda drop partition asda,asdasd"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(new RestrictDropObjectTypes(
                        new HashSet<>(Arrays.asList("procedure", "partition")))));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_DROP_OBJECT_TYPES;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 0, 0, 17, type,
                new Object[] {"FUNCTION", "partition,procedure"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 19, 19, 34, type,
                new Object[] {"CONSTRAINT", "partition,procedure"});
        CheckViolation c3 = new CheckViolation(sqls[3], 1, 37, 37, 61, type,
                new Object[] {"COLUMN", "partition,procedure"});

        Assert.assertEquals(Arrays.asList(c1, c2, c3), actual);
    }

    @Test
    public void check_tooManyAlterviolationGenerated() {
        String[] sqls = {
                "create table abcd(id integer, col1 varchar(64))",
                "alter table abcd add id integer",
                "alter table \"abcd\" add id integer",
                "alter table \"abcd1\" add id integer",
                "alter table abcd add id integer",
                "alter table \"abcd1\" add id integer",
                "alter table abcd add id integer",
                "alter table abcd1 add id integer",
                "select 123 from dual"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(new OracleTooManyAlterStatement(2)));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_ALTER_STATEMENT;
        CheckViolation c1 = new CheckViolation(sqls[4], 1, 0, 0, 30, type, new Object[] {3, "ABCD", 2});

        Assert.assertEquals(Collections.singletonList(c1), actual);
    }

    @Test
    public void check_notNullColWothoutDefaultVal_violationGenerated() {
        String[] sqls = {
                "create table abcd(ida blob not null, col1 varchar(64) not null default 'abcd')",
                "alter table abcd add idb varchar2(64) not null",
                "alter table abcd add idv clob not null default 123"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(new NotNullColumnWithoutDefaultValue()));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.NOT_NULL_COLUMN_WITHOUT_DEFAULT_VALUE;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 34, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 21, 21, 45, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_prohibitedDataTypes_violationGenerated() {
        String[] sqls = {
                "create table abcd(ida blob not null, col1 varchar(64) not null default 'abcd')",
                "alter table abcd add idb varchar2(64) not null",
                "alter table abcd add idv clob not null default 123"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null,
                Collections.singletonList(new ProhibitedDatatypeExists(new HashSet<>(Arrays.asList("blob", "clob")))));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.PROHIBITED_DATATYPE_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 22, 22, 25, type, new Object[] {"blob", "clob,blob"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 25, 25, 28, type, new Object[] {"clob", "clob,blob"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noTableComment_violationGenerated() {
        String[] sqls = {
                "create table abcd(ida blob not null, col1 varchar(64) not null default 'abcd')",
                "alter table abcd add idb varchar2(64) not null",
                "alter table abcd add idv clob not null default 123",
                "create table \"abcd\"(ida blob not null, col1 varchar(64) not null default 'abcd')",
                "comment on table abcd is 'ababahas'",
                "alter table abcd add idv clob not null default 123"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(new OracleNoTableCommentExists(() -> "test")));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.NO_TABLE_COMMENT_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[3], 1, 0, 0, 79, type, new Object[] {"\"abcd\""});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noColumnComment_violationGenerated() {
        String[] sqls = {
                "alter table abcd add idb varchar2(64) not null",
                "alter table abcd add idv clob not null default 123",
                "create table \"abcd\"(ida blob not null, col1 varchar(64) not null default 'abcd')",
                "comment on column abcd.col1 is 'ababahas'",
                "comment on column \"abcd\".ida is 'ababahas'",
                "comment on column oop.\"abcd\".col1 is 'ababahas'",
                "alter table abcd add idv clob not null default 123"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE,
                null, Collections.singletonList(new OracleNoColumnCommentExists(() -> "test")));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.NO_COLUMN_COMMENT_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 39, 39, 78, type, new Object[] {"col1"});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_PlExists_noViolationGenerated() {
        String sql = "CREATE OR REPLACE function fun1 return int as v1 int;\n"
                + "begin\n"
                + "  v1 := 2;\n"
                + "return v1;\n"
                + "end; $$";
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                SqlCheckRules.getAllDefaultRules(null, DialectType.OB_ORACLE));
        Assert.assertTrue(sqlChecker.check(sql).isEmpty());
    }

    @Test
    public void check_wrongPlExists_violationGenerated() {
        String sql = "CREATE OR REPLACE function fun1 return int as v1 int;\n"
                + "begin\n"
                + "  v1 := 2\n"
                + "return v1;\n"
                + "end; $$";
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_ORACLE, "$$",
                SqlCheckRules.getAllDefaultRules(null, DialectType.OB_ORACLE));
        List<CheckViolation> actual = sqlChecker.check(sql);

        SqlCheckRuleType type = SqlCheckRuleType.SYNTAX_ERROR;
        CheckViolation c1 = new CheckViolation(actual.get(0).getText(), 1, 0, 0, 85, type,
                new Object[] {
                        "You have an error in your SQL syntax; check the manual for the right syntax to use near 'ATE OR REPLACE function...' at line 1, col 18"});
        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void buildCheckResults_servalResultExist_orderSucceed() {
        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_INDEX_KEYS;
        SqlCheckRuleType type1 = SqlCheckRuleType.PREFER_LOCAL_INDEX;
        CheckViolation c1 = new CheckViolation("1", 1, 80, 80, 107, type, new Object[] {"abcd"});
        c1.setOffset(1);
        CheckViolation c2 = new CheckViolation("2", 1, 58, 58, 77, type1, new Object[] {});
        c2.setOffset(2);
        CheckViolation c3 = new CheckViolation("1", 1, 80, 80, 107, type1, new Object[] {});
        c3.setOffset(1);
        List<CheckViolation> violations = Arrays.asList(c1, c2, c3);

        List<CheckResult> actual = SqlCheckUtil.buildCheckResults(violations);
        CheckResult r1 = new CheckResult("1", Arrays.asList(c1, c3));
        CheckResult r2 = new CheckResult("2", Collections.singletonList(c2));
        List<CheckResult> expect = Arrays.asList(r1, r2);
        Assert.assertEquals(expect, actual);
    }

    private String joinAndAppend(String[] sqls, String delimiter) {
        return String.join(delimiter, sqls) + delimiter;
    }

    private List<OffsetString> toOffsetString(String[] strs) {
        return Arrays.stream(strs).map(s -> new OffsetString(0, s)).collect(Collectors.toList());
    }

}
