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
package com.oceanbase.odc.service.datasecurity.extractor;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.datasecurity.extractor.model.LogicalTable;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * @author gaoda.xy
 * @date 2023/6/7 20:39
 */
public class OBColumnExtractorTest {

    private final OBColumnExtractor obMySQLColumnExtractor = new OBColumnExtractor(DialectType.OB_MYSQL,
            TestColumnAccessor.OB_MYSQL_DATABASE_1, new TestColumnAccessor());
    private final OBColumnExtractor obOracleColumnExtractor = new OBColumnExtractor(DialectType.OB_ORACLE,
            TestColumnAccessor.OB_ORACLE_DATABASE_1, new TestColumnAccessor());

    private final SQLParser obMySQLParser = new OBMySQLParser();
    private final SQLParser obOracleParser = new OBOracleSQLParser();

    /*
     * Following is testcase for OB_MySQL mode
     */

    @Test
    public void test_parse_OBMySQL() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-case-when-value");
        Statement stmt = obMySQLParser.parse(new StringReader(sql));
    }

    @Test
    public void test_extract_OBMySQL_selectStar() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-star");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name", "birthday", "description");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectColumn() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-column");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name", "birthday", "description");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectConst() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-const");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("1");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectFunction() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-function");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("CONCAT(t1.id, name)");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectFunctionInterval() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-function-interval");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("ADDDATE(birthday, INTERVAL 31 DAY)");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCompound() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-compound");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id + name");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCompoundNegativeConst() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-compound-negative-const");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("-1");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectDemo() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-related-select-body");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "subquery");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCompoundAlias() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-compound-alias");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id_name");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCompoundSubquery() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-compound-subquery");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCompoundCollection() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-compound-collection");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCaseWhenValue() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-case-when-value");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("case_when_value_id");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectCaseWhenCondition() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-case-when-condition");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("case_when_condition_id");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectSubquery() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-subquery");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("subquery");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_selectMultiAssociatedSubquery() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "select-multi-associated-subquery");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("output");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromDual() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-dual");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("alias");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromSingleTableAlias() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-single-table-alias");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name", "birthday", "description");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromMultiTable() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-multi-table");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList(
                "id", "name", "birthday", "description",
                "id", "name", "salary",
                "id", "name", "level");
        Assert.assertEquals(10, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromMultiTableAlias() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-multi-table-alias");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList(
                "id", "name", "birthday", "description",
                "id", "name", "salary",
                "id", "name", "level");
        Assert.assertEquals(10, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromJoinTable() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-join-table");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList(
                "id", "name", "birthday", "description",
                "id", "name", "salary");
        Assert.assertEquals(7, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromJoinTableOn() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-join-table-on");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList(
                "id", "name", "birthday", "description",
                "id", "name", "salary");
        Assert.assertEquals(7, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromJoinTableUsing() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-join-table-using");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList(
                "id", "name", "birthday", "description",
                "salary");
        Assert.assertEquals(5, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromNaturalJoinTable() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-natural-join-table");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList(
                "id", "name", "birthday", "description",
                "salary");
        Assert.assertEquals(5, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromMultiJoinTable() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-multi-join-table");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name", "birthday", "description");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromSingleSubquery() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-single-subquery");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_fromMultiNestingSubquery() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "from-multi-nesting-subquery");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_unionTwoTables() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "union-two-tables");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id1", "name");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_unionMultiTables() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "union-multi-tables");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id2", "name");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteSingleTable() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-single-table");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("name");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteSingleTableAlias() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-single-table-alias");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id1", "name1", "birthday1", "description1");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteMultiTables() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-multi-tables");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name", "birthday", "description", "id", "name", "salary");
        Assert.assertEquals(7, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteMultiAssociatedTables() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-multi-associated-tables");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name", "birthday", "description");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteNestingSubCte() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-nesting-sub-cte");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteRecursiveMultiTables() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-recursive-multi-tables");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id1", "name1");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteRecursiveColumnTransfer() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-recursive-column-transfer");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("c1", "c2", "c3");
        Assert.assertEquals(3, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBMySQL_cteRecursiveNestingSubCte() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_MYSQL, "cte-recursive-nesting-sub-cte");
        LogicalTable result = obMySQLColumnExtractor.extract(obMySQLParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("id", "name");
        Assert.assertEquals(2, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }


    /*
     * Following is testcase for OB_MySQL mode
     */
    @Test
    public void test_parse_OBOracle() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_ORACLE, "select-star");
        Statement stmt = obOracleParser.parse(new StringReader(sql));
    }

    @Test
    public void test_extract_OBOracle_selectStar() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_ORACLE, "select-star");
        LogicalTable result = obOracleColumnExtractor.extract(obOracleParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("ID", "NAME", "BIRTHDAY", "DESCRIPTION");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBOracle_selectColumn() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_ORACLE, "select-column");
        LogicalTable result = obOracleColumnExtractor.extract(obOracleParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("ID", "NAME", "BIRTHDAY", "DESCRIPTION");
        Assert.assertEquals(4, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBOracle_selectRowid() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_ORACLE, "select-rowid");
        LogicalTable result = obOracleColumnExtractor.extract(obOracleParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("ROWID");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    @Test
    public void test_extract_OBOracle_selectCompound() {
        String sql = TestColumnExtractorUtil.getTestSql(DialectType.OB_ORACLE, "select-compound");
        LogicalTable result = obOracleColumnExtractor.extract(obOracleParser.parse(new StringReader(sql)));
        List<String> actualLabels = getResultColumnLabels(result);
        List<String> expectLabels = Arrays.asList("ID + NAME");
        Assert.assertEquals(1, result.getColumnList().size());
        Assert.assertEquals(expectLabels, actualLabels);
    }

    private List<String> getResultColumnLabels(LogicalTable table) {
        return table.getColumnList().stream().map(c -> StringUtils.firstNonBlank(c.getAlias(), c.getName()))
                .collect(Collectors.toList());
    }

}
