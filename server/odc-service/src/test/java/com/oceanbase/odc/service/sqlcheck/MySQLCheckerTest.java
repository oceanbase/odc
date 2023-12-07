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
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.ColumnCharsetExists;
import com.oceanbase.odc.service.sqlcheck.rule.ColumnCollationExists;
import com.oceanbase.odc.service.sqlcheck.rule.ColumnNameInBlackList;
import com.oceanbase.odc.service.sqlcheck.rule.ForeignConstraintExists;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLColumnCalculation;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLLeftFuzzyMatch;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLMissingRequiredColumns;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLNoColumnCommentExists;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLNoNotNullAtInExpression;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLNoTableCommentExists;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictAutoIncrementDataTypes;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictAutoIncrementUnsigned;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictIndexDataTypes;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictPKAutoIncrement;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictPKDataTypes;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictTableAutoIncrement;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictTableCharset;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictTableCollation;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLTooManyAlterStatement;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLZeroFillExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoDefaultValueExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoIndexNameExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoPrimaryKeyExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoPrimaryKeyNameExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoSpecificColumnExists;
import com.oceanbase.odc.service.sqlcheck.rule.NoValidWhereClause;
import com.oceanbase.odc.service.sqlcheck.rule.NoWhereClauseExists;
import com.oceanbase.odc.service.sqlcheck.rule.NotNullColumnWithoutDefaultValue;
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
 * {@link MySQLCheckerTest}
 *
 * @author yh263208
 * @date 2022-12-14 19:56
 * @since ODC_release_4.1.0
 */
public class MySQLCheckerTest {

    @Test
    public void check_sqlWithColumnLeftCalculation_violationGenerated() {
        String sqls = "select col from tab where 1+3>4 and (4+5<90 or id+3<50 and (4+5=9)) and id='12'";
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, ";",
                Collections.singletonList(new MySQLColumnCalculation()));
        List<CheckViolation> actual = sqlChecker.check(sqls);

        CheckViolation c = new CheckViolation(sqls, 1, 47, 47, 53,
                SqlCheckRuleType.INDEX_COLUMN_CALCULATION, new Object[] {});
        List<CheckViolation> expect = Collections.singletonList(c);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithColumnLeftCalculation_violationGenerated() {
        String[] sqls = new String[] {
                "select col from tab where 1+3>4 and (4+5<90 or id+3<50 and (4+5=9)) and id='12'",
                "select col from \ntab where ((id+4)+5) *5 not like '12'"
        };
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, ";",
                Collections.singletonList(new MySQLColumnCalculation()));
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
                "select col from tab where true or col not like 'aaa' '%abcd'",
                "select col from \ntab where ((8+4)+5) *5 not like '12%'"
        };
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLLeftFuzzyMatch()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.INDEX_COLUMN_FUZZY_MATCH;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 37, 37, 51, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 36, 36, 67, type, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[2], 1, 34, 34, 59, type, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_sqlsWithTooManyInExprs_violationGenerated() {
        String[] sqls = new String[] {
                "select col from tab where 1+3>4 and col in ('abc','abc','abc','abc') or col not in ('abc','abc','abc','abc')",
                "select col from tab where 5+6<90 or col not in ('abc','abc','abc')",
        };
        SqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLNoNotNullAtInExpression()));
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
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
                "replace into tab values(1,2,3)",
                "insert into tab set col='asbcd'"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new NoSpecificColumnExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_SPECIFIC_COLUMN_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 12, 12, 28, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 13, 13, 29, type, new Object[] {});
        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tooManyColumnDefs_violationGenerated() {
        String[] sqls = new String[] {
                "create table `abcd` (id varchar(64), content integer)",
                "create table `abcd` (id varchar(64), content integer, age varchar(32))",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
                "create table `abcd` (id varchar(64), index i_a(id) local, index i_b(id) global, index i_c(id) BLOCK_SIZE=200)",
                "create table `abcd` (id varchar(64), content integer, age varchar(32))",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
                "create table `abcd` (id char(1001), index i_a(id) local, index i_b(id) global, index i_c(id) BLOCK_SIZE=200)",
                "create table `abcd` (id char(1000), content integer, age char(32))",
                "alter table abcd add (id char(1000), id1 char(1005), id2 blob)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
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
                "create table aaaa(id varchar(64) unique)",
                "CREATE TABLE aaaa (ID VARCHAR(64), CONSTRAINT `UK_AAA` UNIQUE (ID))",
                "CREATE TABLE bbbb (ID VARCHAR(64), CONSTRAINT `AAA` UNIQUE (ID))",
                "create unique index chz.`u_aaa` on ttt(name)",
                "alter table test_unique_tb add unique key `uuiyt`(name)",
                "alter table test_unique_tb add constraint llopi unique(name)",
                "alter table test_unique_tb add constraint `uk_00998` unique(name);"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictUniqueIndexNaming("uk_\\w+")));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_UNIQUE_INDEX_NAMING;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 35, 35, 62, type, new Object[] {"`AAA`", "uk_\\w+"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 0, 0, 43, type, new Object[] {"`u_aaa`", "uk_\\w+"});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 31, 31, 54, type, new Object[] {"`uuiyt`", "uk_\\w+"});
        CheckViolation c4 = new CheckViolation(sqls[5], 1, 31, 31, 59, type, new Object[] {"llopi", "uk_\\w+"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_defaultUniqueIndexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) unique)",
                "CREATE TABLE aaaa (ID VARCHAR(64), CONSTRAINT `UK_aaaa_id` UNIQUE (ID))",
                "CREATE TABLE bbbb (ID VARCHAR(64), CONSTRAINT `AAA` UNIQUE (ID))",
                "create unique index chz.`u_aaa` on ttt(name)",
                "alter table test_unique_tb add unique key `uuiyt`(name)",
                "alter table test_unique_tb add constraint llopi unique(name)",
                "alter table test_unique_tb add constraint `uk_test_unique_tb_name` unique(name);"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictUniqueIndexNaming(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_UNIQUE_INDEX_NAMING;
        String pattern = "uk_${table-name}_${column-name-1}_${column-name-2}_...";
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 35, 35, 62, type, new Object[] {"`AAA`", pattern});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 0, 0, 43, type, new Object[] {"`u_aaa`", pattern});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 31, 31, 54, type, new Object[] {"`uuiyt`", pattern});
        CheckViolation c4 = new CheckViolation(sqls[5], 1, 31, 31, 59, type, new Object[] {"llopi", pattern});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_pkNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) primary key)",
                "CREATE TABLE aaaa (ID VARCHAR(64), CONSTRAINT `pk_aaaa_id` primary key (ID))",
                "CREATE TABLE bbbb (ID VARCHAR(64), CONSTRAINT `AAA` primary key (ID))",
                "alter table test_pk_tb add primary key (name)",
                "alter table test_pk_tb add constraint llopi primary key(name)",
                "alter table test_pk_tb add constraint `pk_test_pk_tb_name` primary key(name);"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictPKNaming("pk_\\w+")));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_NAMING;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 35, 35, 67, type, new Object[] {"`AAA`", "pk_\\w+"});
        CheckViolation c2 = new CheckViolation(sqls[4], 1, 27, 27, 60, type, new Object[] {"llopi", "pk_\\w+"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_defaultPKNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) primary key)",
                "CREATE TABLE aaaa (ID VARCHAR(64), CONSTRAINT `pk_aaaa_id` primary key (ID))",
                "CREATE TABLE bbbb (ID VARCHAR(64), CONSTRAINT `AAA` primary key (ID))",
                "alter table test_pk_tb add primary key (name)",
                "alter table test_pk_tb add constraint llopi primary key(name)",
                "alter table test_pk_tb add constraint `pk_test_pk_tb_name` primary key(name);"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictPKNaming(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_NAMING;
        String pattern = "pk_${table-name}_${column-name-1}_${column-name-2}_...";
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 35, 35, 67, type, new Object[] {"`AAA`", pattern});
        CheckViolation c2 = new CheckViolation(sqls[4], 1, 27, 27, 60, type, new Object[] {"llopi", pattern});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_foreignKeyExists_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64))",
                "CREATE TABLE bbbb (ID VARCHAR(64), CONSTRAINT `AAA` foreign key (a,c) references a.b(id) on delete set null)",
                "alter table abcd add foreign key abcd(col1, col2) references chz.tb(col1) on delete cascade on update no action"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new ForeignConstraintExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.FOREIGN_CONSTRAINT_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 35, 35, 106, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 17, 17, 110, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noPrimaryKeyExists_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64))",
                "CREATE TABLE aaaa (ID VARCHAR(64) primary key)",
                "CREATE TABLE bbbb (ID VARCHAR(64), constraint pk_name primary key(id, id1) comment 'aaaa')"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new NoPrimaryKeyExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_PRIMARY_KEY_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 32, type, new Object[] {});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noTableCommentsExists_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64))",
                "CREATE TABLE aaaa (ID VARCHAR(64) primary key) comment 'aaaa'",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLNoTableCommentExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_TABLE_COMMENT_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 32, type, new Object[] {"aaaa"});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tableNameInBlackList_violationGenerated() {
        String[] sqls = new String[] {
                "create table `abcd`(id varchar(64))",
                "CREATE TABLE uuiioo(ID VARCHAR(64))",
                "alter table `abcd` rename to abcd",
                "alter table `abcd` rename to kkkk",
                "rename table a.b to chz.ABCD"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new TableNameInBlackList(Collections.singleton("abcd"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TABLE_NAME_IN_BLACK_LIST;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 34, type, new Object[] {"`abcd`", "abcd"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 29, 29, 32, type, new Object[] {"abcd", "abcd"});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 20, 20, 27, type, new Object[] {"ABCD", "abcd"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictTableCharset_violationGenerated() {
        String[] sqls = new String[] {
                "create table `abcd`(id varchar(64))",
                "CREATE TABLE uuiioo(ID VARCHAR(64)) charset=gbk",
                "alter table `abcd` rename to abcd",
                "alter table `abcd` rename to kkkk, convert to character set gbk",
                "alter table `abcd` convert to character set utf8mb4",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictTableCharset(Collections.singleton("utf8mb4"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_TABLE_CHARSET;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 36, 36, 46, type, new Object[] {"gbk", "utf8mb4"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 35, 35, 62, type, new Object[] {"gbk", "utf8mb4"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictTableCollation_violationGenerated() {
        String[] sqls = new String[] {
                "create table `abcd`(id varchar(64))",
                "CREATE TABLE uuiioo(ID VARCHAR(64)) collate=utf8mb4_generated_ci",
                "alter table `abcd` rename to abcd",
                "alter table `abcd` rename to kkkk, convert to character set gbk collate utf8mb4_generated_ci",
                "alter table `abcd` convert to character set utf8mb4",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictTableCollation(Collections.singleton("utf8mb4"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_TABLE_COLLATION;
        CheckViolation c1 =
                new CheckViolation(sqls[1], 1, 36, 36, 63, type, new Object[] {"utf8mb4_generated_ci", "utf8mb4"});
        CheckViolation c2 =
                new CheckViolation(sqls[3], 1, 35, 35, 91, type, new Object[] {"utf8mb4_generated_ci", "utf8mb4"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_colRefsInIndex_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64), "
                        + "key idx_name(col1, col2, col3), "
                        + "key idx_name_1(col1, col2), "
                        + "constraint abc unique key(col1, col2, col3), "
                        + "primary key(col1, col2))",
                "alter table abcd add key idx_name(col1, col2, col3)",
                "alter table abcd add constraint abcd primary key(col1, col2)",
                "alter table abcd add constraint abc unique key(col1, col2, col3)",
                "create index idx_name on chz.tb(col1, col2, col3)",
                "create index idx_name on chz.tb(col1, col2)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new TooManyColumnRefInIndex(2)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_COL_REFS_IN_INDEX;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 63, type, new Object[] {3, 2});
        CheckViolation c2 = new CheckViolation(sqls[0], 1, 94, 94, 136, type, new Object[] {3, 2});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 17, 17, 50, type, new Object[] {3, 2});
        CheckViolation c4 = new CheckViolation(sqls[3], 1, 17, 17, 63, type, new Object[] {3, 2});
        CheckViolation c5 = new CheckViolation(sqls[4], 1, 0, 0, 48, type, new Object[] {3, 2});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4, c5);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictPrimaryKeyType_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd("
                        + "id varchar(64) primary key,"
                        + "name blob,"
                        + "`age` int,"
                        + "constraint abcd_pk primary key(name, `age`))",
                "alter table abcd add primary key (name)",
                "alter table abcd add primary key (age)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(sqls[0]);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictPKDataTypes(jdbcTemplate,
                        new HashSet<>(Arrays.asList("float", "int")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_DATATYPES;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 31, type, new Object[] {"varchar", "float,int"});
        CheckViolation c2 = new CheckViolation(sqls[0], 1, 96, 96, 99, type, new Object[] {"blob", "float,int"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 34, 34, 37, type, new Object[] {"blob", "float,int"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_colRefsInPrimaryKey_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64), "
                        + "primary key (col1, col2, col3), "
                        + "primary key (col1, col2))",
                "alter table abcd add primary key (col1, col2, col3)",
                "alter table abcd add constraint abcd primary key(col1, col2)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new TooManyColumnRefInPrimaryKey(2)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_COL_REFS_IN_PRIMARY_KEY;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 63, type, new Object[] {3, 2});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 17, 17, 50, type, new Object[] {3, 2});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictAutoIncrement_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64) primary key auto_increment, name int)",
                "create table abcd1(id varchar(64) primary key, name int auto_increment)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictPKAutoIncrement(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_AUTO_INCREMENT;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 19, 19, 44, type, new Object[] {});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictAutoIncrementDataTypes_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64) primary key auto_increment, name int)",
                "create table abcd1(id varchar(64) primary key, name bigint auto_increment)",
                "alter table abcd1 add column id int(11) primary key auto_increment",
                "alter table abcd1 add column name bigint auto_increment)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictAutoIncrementDataTypes(Collections.singleton("bigint"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_AUTO_INCREMENT_DATATYPES;
        CheckViolation c1 =
                new CheckViolation(sqls[0], 1, 21, 21, 31, type, new Object[] {"id", "varchar(64)", "bigint"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 32, 32, 38, type, new Object[] {"id", "int(11)", "bigint"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictAutoIncrementOutOfLinConstraint_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64) auto_increment,name int,age text,constraint pk primary key(name,age))",
                "create table abcd(id varchar(64) auto_increment,name int,age text,constraint pk primary key(id))",
                "create table abcd(id varchar(64) auto_increment,name int,age text,constraint pk primary key(age))",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictPKAutoIncrement(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_AUTO_INCREMENT;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 92, 92, 94, type, new Object[] {});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictAutoIncrementAlterConstraint_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd(id varchar(64) auto_increment,name int,age text)",
                "alter table abcd add constraint pk primary key(name,age)",
                "alter table abcd add constraint pk primary key(id)",
                "alter table `abcd` add constraint pk primary key(`age`)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(sqls[0]);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictPKAutoIncrement(jdbcTemplate)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_AUTO_INCREMENT;
        CheckViolation c1 = new CheckViolation(sqls[3], 1, 49, 49, 53, type, new Object[] {});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictAutoIncrementAlterConstraint1_violationGenerated() {
        String ddl = "create table abcd(id varchar(64) auto_increment,name int,age text)";
        String[] sqls = new String[] {
                "alter table abcd add constraint pk primary key(name,age)",
                "alter table abcd add constraint pk primary key(id)",
                "alter table `abcd` add constraint pk primary key(`age`)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(ddl);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictPKAutoIncrement(jdbcTemplate)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_PK_AUTO_INCREMENT;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 49, 49, 53, type, new Object[] {});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_indexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), index (id))",
                "CREATE TABLE aaaa (ID VARCHAR(64), index idx_name (ID))",
                "CREATE TABLE bbbb (ID VARCHAR(64), fulltext index `AAA` (ID))",
                "create index chz.`u_aaa` on ttt(name)",
                "alter table test_unique_tb add key `uuiyt`(name)",
                "alter table test_unique_tb add index `idx_cass`(name)",
                "alter table test_unique_tb add fulltext index `uuiyt`(name)",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictIndexNaming("idx_\\w+")));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_NAMING;
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 35, 35, 59, type, new Object[] {"`AAA`", "idx_\\w+"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 0, 0, 36, type, new Object[] {"`u_aaa`", "idx_\\w+"});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 27, 27, 47, type, new Object[] {"`uuiyt`", "idx_\\w+"});
        CheckViolation c4 = new CheckViolation(sqls[6], 1, 27, 27, 58, type, new Object[] {"`uuiyt`", "idx_\\w+"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_defaultIndexNamingPattern_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), index (id))",
                "CREATE TABLE aaaa (ID VARCHAR(64), index idx_aaaa_id (ID))",
                "CREATE TABLE bbbb (ID VARCHAR(64), fulltext index `AAA` (ID))",
                "create index chz.`u_aaa` on ttt(name)",
                "alter table test_unique_tb add key `uuiyt`(name)",
                "alter table `test_unique_tb` add index `idx_test_unique_tb_name`(name)",
                "alter table test_unique_tb add fulltext index `uuiyt`(name)",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictIndexNaming(null)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_NAMING;
        String pattern = "idx_${table-name}_${column-name-1}_${column-name-2}_...";
        CheckViolation c1 = new CheckViolation(sqls[2], 1, 35, 35, 59, type, new Object[] {"`AAA`", pattern});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 0, 0, 36, type, new Object[] {"`u_aaa`", pattern});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 27, 27, 47, type, new Object[] {"`uuiyt`", pattern});
        CheckViolation c4 = new CheckViolation(sqls[6], 1, 27, 27, 58, type, new Object[] {"`uuiyt`", pattern});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noIndexNameExists_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), index (id))",
                "CREATE TABLE aaaa (ID VARCHAR(64), index idx_name (ID), constraint check(1), constraint primary key (id))",
                "CREATE TABLE bbbb (ID VARCHAR(64), fulltext index `AAA` (ID), constraint unique key (id))",
                "alter table test_unique_tb add key (name), add unique key abcd(id), add unique key(id1), add constraint abcde unique key(id3)",
                "alter table test_unique_tb add index(name), add check(1), add primary key(id5)",
                "alter table test_unique_tb add fulltext index `uuiyt`(name), add constraint uuuu primary key(id6)",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new NoIndexNameExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_INDEX_NAME_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 34, 34, 43, type, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[2], 1, 62, 62, 87, type, new Object[] {});
        CheckViolation c4 = new CheckViolation(sqls[3], 1, 68, 68, 86, type, new Object[] {});
        CheckViolation c5 = new CheckViolation(sqls[3], 1, 27, 27, 40, type, new Object[] {});
        CheckViolation c7 = new CheckViolation(sqls[4], 1, 27, 27, 41, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c3, c4, c5, c7);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noprimaryKeyNameExists_violationGenerated() {
        String[] sqls = new String[] {
                "CREATE TABLE aaaa (ID VARCHAR(64), index idx_name (ID), constraint check(1), constraint primary key (id))",
                "CREATE TABLE bbbb (ID VARCHAR(64) primary key, fulltext index `AAA` (ID), constraint unique key (id))",
                "CREATE TABLE cccc (ID VARCHAR(64), constraint abcdet primary key (id))",
                "alter table test_unique_tb add index(name), add check(1), add primary key(id5)",
                "alter table test_unique_tb add fulltext index `uuiyt`(name), add constraint uuuu primary key(id6)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new NoPrimaryKeyNameExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_PRIMARY_KEY_NAME_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 77, 77, 103, type, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 34, 34, 44, type, new Object[] {});
        CheckViolation c4 = new CheckViolation(sqls[3], 1, 58, 58, 77, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c3, c4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_zeroFill_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id int(11) zerofill, index (id))",
                "CREATE TABLE aaaa (ID int(11))",
                "alter table abcd add id int(11) zerofill"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLZeroFillExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.ZEROFILL_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 36, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 24, 24, 39, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnCharset_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) charset utf8)",
                "create table aaaa1(id varchar(64))",
                "create table aaaa2(id enum('a','b','c') charset utf8)",
                "create table aaaa3(id enum('a','b','c'))",
                "alter table abcd add id varchar(64) charset utf8"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new ColumnCharsetExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_CHARSET_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 44, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 22, 22, 51, type, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 24, 24, 47, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnCollation_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64) collate utf8mb4)",
                "create table aaaa1(id varchar(64))",
                "create table aaaa2(id enum('a','b','c') collate utf8mb4)",
                "create table aaaa3(id enum('a','b','c'))",
                "alter table abcd add id varchar(64) collate utf8mb4"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new ColumnCollationExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_COLLATION_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 47, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 22, 22, 54, type, new Object[] {});
        CheckViolation c3 = new CheckViolation(sqls[4], 1, 24, 24, 50, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnIsNullable_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), col1 text, col2 double not null)",
                "alter table abdcd add id double comment 'aaa', add id1 float null, add col1 text, add col2 double not null"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new RestrictColumnNotNull(new HashSet<>(Collections.singletonList("text")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_IS_NULLABLE;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 31, type, new Object[] {"varchar(64)", "text"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 22, 22, 44, type, new Object[] {"double", "text"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 51, 51, 64, type, new Object[] {"float", "text"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noDefaultValue_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), col1 text, col2 double default 123)",
                "alter table abdcd add id double comment 'aaa', add id1 float null, add col1 text, add col2 double default 4456"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new NoDefaultValueExists(new HashSet<>(Collections.singletonList("text")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_DEFAULT_VALUE_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 31, type, new Object[] {"varchar(64)", "text"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 22, 22, 44, type, new Object[] {"double", "text"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 51, 51, 64, type, new Object[] {"float", "text"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_noColumnComment_violationGenerated() {
        String[] sqls = new String[] {
                "create table aaaa(id varchar(64), col2 double comment '123')",
                "alter table abdcd add id double default 123, add id1 float null, add col2 double comment '4456'"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLNoColumnCommentExists()));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.NO_COLUMN_COMMENT_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 31, type, new Object[] {"id"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 22, 22, 42, type, new Object[] {"id"});
        CheckViolation c3 = new CheckViolation(sqls[1], 1, 49, 49, 62, type, new Object[] {"id1"});

        List<CheckViolation> expect = Arrays.asList(c1, c2, c3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_columnNameInBlackList_violationGenerated() {
        String[] sqls = new String[] {
                "create table `abcd`(`abcd` varchar(64))",
                "CREATE TABLE uuiioo(asdasdas VARCHAR(64))",
                "alter table `abcd` add abcd varchar(64)"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new ColumnNameInBlackList(Collections.singleton("abcd"))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.COLUMN_NAME_IN_BLACK_LIST;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 20, 20, 25, type, new Object[] {"`abcd`", "abcd"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 23, 23, 26, type, new Object[] {"abcd", "abcd"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictTableAutoIncrement_violationGenerated() {
        String[] sqls = new String[] {
                "create table `abcd`(`abcd` varchar(64))",
                "CREATE TABLE uuiioo(asdasdas VARCHAR(64)) auto_increment=100",
                "CREATE TABLE uuiioo(asdasdas VARCHAR(64)) auto_increment=1",
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictTableAutoIncrement(1)));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_TABLE_AUTO_INCREMENT;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 38, type, new Object[] {1, "N/A"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 42, 42, 59, type, new Object[] {1, "100"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_selectStarExists_violationGenerated() {
        String[] sqls = {
                "select a.b, * from tb",
                "select a from tb union all select tb1.* from tb1",
                "select 1+1 from dual"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
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
                "create table abcd(id int, col1 varchar(64))",
                "create table abcd(id int, create_gmt timestamp(6))"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
                null, Collections.singletonList(
                        new MySQLMissingRequiredColumns(new HashSet<>(Arrays.asList("id", "`create_gmt`")))));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.MISSING_REQUIRED_COLUMNS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 0, 0, 42, type, new Object[] {"`create_gmt`"});

        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_autoincrementUnsinged_violationGenerated() {
        String[] sqls = {
                "create table abcd(id int auto_increment, col1 varchar(64))",
                "create table abcd(id int unsigned auto_increment, create_gmt timestamp(6))",
                "alter table abcd add id int auto_increment",
                "alter table abcd add id int unsigned auto_increment"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
                null, Collections.singletonList(new MySQLRestrictAutoIncrementUnsigned()));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_AUTO_INCREMENT_UNSIGNED;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 21, 21, 23, type, new Object[] {"int"});
        CheckViolation c2 = new CheckViolation(sqls[2], 1, 24, 24, 26, type, new Object[] {"int"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_tooManyAlterviolationGenerated() {
        String[] sqls = {
                "create table abcd(id int auto_increment, col1 varchar(64))",
                "create table abcd(id int unsigned auto_increment, create_gmt timestamp(6))",
                "alter table abcd add id int auto_increment",
                "alter table `abcd` add id int unsigned auto_increment",
                "alter table `abcd1` add id int unsigned auto_increment",
                "alter table abcd add id int unsigned auto_increment",
                "alter table `abcd1` add id int unsigned auto_increment",
                "alter table abcd add id int unsigned auto_increment",
                "alter table `abcd1` add id int unsigned auto_increment",
                "select 123 from dual"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
                null, Collections.singletonList(new MySQLTooManyAlterStatement(2)));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.TOO_MANY_ALTER_STATEMENT;
        CheckViolation c1 = new CheckViolation(sqls[5], 1, 0, 0, 50, type, new Object[] {3, "abcd", 2});
        CheckViolation c2 = new CheckViolation(sqls[8], 1, 0, 0, 53, type, new Object[] {3, "abcd1", 2});

        Assert.assertEquals(Arrays.asList(c1, c2), actual);
    }

    @Test
    public void check_notNullColWothoutDefaultVal_violationGenerated() {
        String[] sqls = {
                "create table abcd(ida text not null, col1 varchar(64) not null default 'abcd')",
                "alter table abcd add idb blob not null",
                "alter table abcd add idv int unsigned auto_increment not null default 123"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
                null, Collections.singletonList(new NotNullColumnWithoutDefaultValue()));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.NOT_NULL_COLUMN_WITHOUT_DEFAULT_VALUE;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 18, 18, 34, type, new Object[] {});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 21, 21, 37, type, new Object[] {});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_prohibitedDataType_violationGenerated() {
        String[] sqls = {
                "create table abcd(ida text not null, col1 varchar(64) not null default 'abcd')",
                "alter table abcd add idb blob not null",
                "alter table abcd add idv int unsigned auto_increment not null default 123"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
                null,
                Collections.singletonList(new ProhibitedDatatypeExists(new HashSet<>(Arrays.asList("text", "blob")))));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.PROHIBITED_DATATYPE_EXISTS;
        CheckViolation c1 = new CheckViolation(sqls[0], 1, 22, 22, 25, type, new Object[] {"text", "blob,text"});
        CheckViolation c2 = new CheckViolation(sqls[1], 1, 25, 25, 28, type, new Object[] {"blob", "blob,text"});

        List<CheckViolation> expect = Arrays.asList(c1, c2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictIndexType_violationGenerated() {
        String[] sqls = new String[] {
                "create table abcd (\n"
                        + "id       varchar(64) primary key,\n"
                        + "name     blob        unique key,\n"
                        + "`age`    int         unique key,\n"
                        + "age_t    text        key,\n"
                        + "constraint abcd_pk1 primary key(name, `age`),\n"
                        + "constraint abcd_pk2 primary key(id, `age`),\n"
                        + "constraint abcd_pk3 primary key(name, `age_t`),\n"
                        + "unique key(name, `age`),\n"
                        + "unique key(id, `age`),\n"
                        + "unique key(name, `age_t`),\n"
                        + "index(id, `age`),\n"
                        + "index(name, `age_t`))",
                "alter table abcd add primary key (`name`)",
                "alter table abcd add primary key (age)",
                "alter table abcd add unique key (name, `age_t`)",
                "alter table abcd add unique key (age)",
                "alter table abcd add index(id, `age`)",
                "alter table abcd add index(name, age_t)",
                "create index abcd_idx1 on abcd(id)",
                "create index abcd_idx2 on abcd(id, name)",
                "create index abcd_idx3 on abcd(age_t, name)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(sqls[0]);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictIndexDataTypes(jdbcTemplate,
                        new HashSet<>(Arrays.asList("varchar", "int")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_DATATYPES;
        CheckViolation c1 =
                new CheckViolation(sqls[0], 3, 9, 63, 66, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c2 =
                new CheckViolation(sqls[0], 5, 9, 129, 132, type, new Object[] {"age_t", "text", "varchar,int"});
        CheckViolation c3 =
                new CheckViolation(sqls[0], 6, 32, 178, 181, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c4 =
                new CheckViolation(sqls[0], 8, 32, 268, 271, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c5 =
                new CheckViolation(sqls[0], 8, 38, 274, 280, type, new Object[] {"`age_t`", "text", "varchar,int"});
        CheckViolation c6 =
                new CheckViolation(sqls[0], 9, 11, 295, 298, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c7 =
                new CheckViolation(sqls[0], 11, 11, 343, 346, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c8 =
                new CheckViolation(sqls[0], 11, 17, 349, 355, type, new Object[] {"`age_t`", "text", "varchar,int"});
        CheckViolation c9 =
                new CheckViolation(sqls[0], 13, 6, 383, 386, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c10 =
                new CheckViolation(sqls[0], 13, 12, 389, 395, type, new Object[] {"`age_t`", "text", "varchar,int"});

        CheckViolation c11 =
                new CheckViolation(sqls[1], 1, 34, 34, 39, type, new Object[] {"`name`", "blob", "varchar,int"});
        CheckViolation c12 =
                new CheckViolation(sqls[3], 1, 33, 33, 36, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c13 =
                new CheckViolation(sqls[3], 1, 39, 39, 45, type, new Object[] {"`age_t`", "text", "varchar,int"});

        CheckViolation c14 =
                new CheckViolation(sqls[6], 1, 27, 27, 30, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c15 =
                new CheckViolation(sqls[6], 1, 33, 33, 37, type, new Object[] {"age_t", "text", "varchar,int"});
        CheckViolation c16 =
                new CheckViolation(sqls[8], 1, 35, 35, 38, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c17 =
                new CheckViolation(sqls[9], 1, 31, 31, 35, type, new Object[] {"age_t", "text", "varchar,int"});
        CheckViolation c18 =
                new CheckViolation(sqls[9], 1, 38, 38, 41, type, new Object[] {"name", "blob", "varchar,int"});

        List<CheckViolation> expect =
                Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_restrictIndexType1_violationGenerated() {
        String ddl = "create table abcd (\n"
                + "id       varchar(64) primary key,\n"
                + "name     blob        unique key,\n"
                + "`age`    int         unique key,\n"
                + "age_t    text        key,\n"
                + "constraint abcd_pk1 primary key(name, `age`),\n"
                + "constraint abcd_pk2 primary key(id, `age`),\n"
                + "constraint abcd_pk3 primary key(name, `age_t`),\n"
                + "unique key(name, `age`),\n"
                + "unique key(id, `age`),\n"
                + "unique key(name, `age_t`),\n"
                + "index(id, `age`),\n"
                + "index(name, `age_t`))";
        String[] sqls = new String[] {
                "alter table abcd add primary key (`name`)",
                "alter table abcd add primary key (age)",
                "alter table abcd add unique key (name, `age_t`)",
                "alter table abcd add unique key (age)",
                "alter table abcd add index(id, `age`)",
                "alter table abcd add index(name, age_t)",
                "create index abcd_idx1 on abcd(id)",
                "create index abcd_idx2 on abcd(id, name)",
                "create index abcd_idx3 on abcd(age_t, name)"
        };
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.any(RowMapper.class)))
                .thenReturn(ddl);
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                Collections.singletonList(new MySQLRestrictIndexDataTypes(jdbcTemplate,
                        new HashSet<>(Arrays.asList("varchar", "int")))));
        List<CheckViolation> actual = sqlChecker.check(joinAndAppend(sqls, "$$"));

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_INDEX_DATATYPES;
        CheckViolation c11 =
                new CheckViolation(sqls[0], 1, 34, 34, 39, type, new Object[] {"`name`", "blob", "varchar,int"});
        CheckViolation c12 =
                new CheckViolation(sqls[2], 1, 33, 33, 36, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c13 =
                new CheckViolation(sqls[2], 1, 39, 39, 45, type, new Object[] {"`age_t`", "text", "varchar,int"});

        CheckViolation c14 =
                new CheckViolation(sqls[5], 1, 27, 27, 30, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c15 =
                new CheckViolation(sqls[5], 1, 33, 33, 37, type, new Object[] {"age_t", "text", "varchar,int"});
        CheckViolation c16 =
                new CheckViolation(sqls[7], 1, 35, 35, 38, type, new Object[] {"name", "blob", "varchar,int"});
        CheckViolation c17 =
                new CheckViolation(sqls[8], 1, 31, 31, 35, type, new Object[] {"age_t", "text", "varchar,int"});
        CheckViolation c18 =
                new CheckViolation(sqls[8], 1, 38, 38, 41, type, new Object[] {"name", "blob", "varchar,int"});

        List<CheckViolation> expect = Arrays.asList(c11, c12, c13, c14, c15, c16, c17, c18);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void check_drop_violationGenerated() {
        String[] sqls = {
                "create table abcd(ida text not null, col1 varchar(64) not null default 'abcd')",
                "drop procedure abcd",
                "drop function abcd",
                "alter table abcd drop index `abdhfg`, drop subpartition `pppoi`",
                "alter table iiiop drop partition a,c,b,f"
        };
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL,
                null, Collections.singletonList(new RestrictDropObjectTypes(
                        new HashSet<>(Arrays.asList("function", "partition", "subpartition")))));
        List<CheckViolation> actual = sqlChecker.check(toOffsetString(sqls), null);

        SqlCheckRuleType type = SqlCheckRuleType.RESTRICT_DROP_OBJECT_TYPES;
        CheckViolation c1 = new CheckViolation(sqls[1], 1, 0, 0, 18, type, new Object[] {
                "PROCEDURE", "partition,subpartition,function"});
        CheckViolation c2 = new CheckViolation(sqls[3], 1, 17, 17, 35, type, new Object[] {
                "INDEX", "partition,subpartition,function"});

        Assert.assertEquals(Arrays.asList(c1, c2), actual);
    }

    @Test
    public void check_PlExists_noViolationGenerated() {
        String sql = "create procedure pro7 ( OUT `a` varchar(45)) begin\n"
                + "declare\n"
                + "  tid int;\n"
                + "set\n"
                + "  a = 2;\n"
                + "set\n"
                + "  tid = a + 2;\n"
                + "select\n"
                + "  tid;\n"
                + "end; $$";
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                SqlCheckRules.getAllDefaultRules(null, DialectType.OB_MYSQL));
        Assert.assertTrue(sqlChecker.check(sql).isEmpty());
    }

    @Test
    public void check_wrongPlExists_violationGenerated() {
        String sql = "create procedure pro7 ( OUT `a` varchar(45)) begin\n"
                + "declare\n"
                + "  tid int;\n"
                + "set\n"
                + "  a = 2\n"
                + "set\n"
                + "  tid = a + 2;\n"
                + "select\n"
                + "  tid;\n"
                + "end; $$";
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(DialectType.OB_MYSQL, "$$",
                SqlCheckRules.getAllDefaultRules(null, DialectType.OB_MYSQL));
        List<CheckViolation> actual = sqlChecker.check(sql);

        SqlCheckRuleType type = SqlCheckRuleType.SYNTAX_ERROR;
        CheckViolation c1 = new CheckViolation(actual.get(0).getText(), 1, 0, 0, 119, type,
                new Object[] {
                        "You have an error in your SQL syntax; check the manual for the right syntax to use near 'create procedure...' at line 1, col 7"});
        List<CheckViolation> expect = Collections.singletonList(c1);
        Assert.assertEquals(expect, actual);
    }

    private String joinAndAppend(String[] sqls, String delimiter) {
        return String.join(delimiter, sqls) + delimiter;
    }

    private List<OffsetString> toOffsetString(String[] strs) {
        return Arrays.stream(strs).map(s -> new OffsetString(0, s)).collect(Collectors.toList());
    }
}
