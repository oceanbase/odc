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
package com.oceanbase.tools.sqlparser;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

/**
 * {@link OBMySQLParserTest}
 *
 * @author yh263208
 * @date 2023-02-15 21:05
 * @since sqlparser_1.0.0_SNAPSHOT
 */
public class OBMySQLParserTest {

    @Test
    public void parse_CreateTableOrganizationHeap() {
        SQLParser parser = new OBMySQLParser();
        Statement actual = parser
                .parse(new StringReader("create table create_table_with_option_demo (c1 int) ORGANIZATION HEAP;"));
        Assert.assertEquals("ORGANIZATION HEAP", ((CreateTable) actual).getTableOptions().getText());
    }

    @Test
    public void parse_CreateTableOrganizationIndex() {
        SQLParser parser = new OBMySQLParser();
        Statement actual = parser
                .parse(new StringReader("create table create_table_with_option_demo (c1 int) ORGANIZATION INDEX;"));
        Assert.assertEquals("ORGANIZATION INDEX", ((CreateTable) actual).getTableOptions().getText());
    }

    @Test
    public void parse_enableMacroBlockBloomFilterEqualsFalse_parseSucceed() {
        SQLParser parser = new OBMySQLParser();
        Statement actual = parser
                .parse(new StringReader("CREATE TABLE `test_date` (\n" +
                        "  `a` date NOT NULL,\n" +
                        "  `b` date DEFAULT NULL\n" +
                        ") ORGANIZATION INDEX DEFAULT CHARSET = utf8mb4 ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = TRUE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 0\n"
                        +
                        " partition by range columns(a)\n" +
                        "(partition `p_2022_11` values less than ('2022-11-01'))"));
        Assert.assertNotNull(actual);
    }

    @Test
    public void parse_enableMacroBlockBloomFilterEqualsTrue_parseSucceed() {
        SQLParser parser = new OBMySQLParser();
        Statement actual = parser
                .parse(new StringReader("CREATE TABLE `test_date` (\n" +
                        "  `a` date NOT NULL,\n" +
                        "  `b` date DEFAULT NULL\n" +
                        ") ORGANIZATION INDEX DEFAULT CHARSET = utf8mb4 ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = TRUE ENABLE_MACRO_BLOCK_BLOOM_FILTER = TRUE TABLET_SIZE = 134217728 PCTFREE = 0\n"
                        +
                        " partition by range columns(a)\n" +
                        "(partition `p_2022_11` values less than ('2022-11-01'))"));
        Assert.assertNotNull(actual);
    }

    @Test
    public void parse_selectStatement_parseSucceed() {
        SQLParser parser = new OBMySQLParser();
        Statement actual = parser.parse(new StringReader("select col.* abc from dual;"));

        ColumnReference r = new ColumnReference(null, "col", "*");
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "dual", null);
        Select expect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parse_updateStatement_parseSucceed() {
        SQLParser sqlParser = new OBMySQLParser();
        Statement actual = sqlParser.parse(new StringReader("update tab set col=1 where col=100"));

        FromReference tableReference = new NameReference(null, "tab", null);
        Update expect = new Update(Arrays.asList(tableReference), Arrays.asList(new UpdateAssign(
                Arrays.asList(new ColumnReference(null, null, "col")),
                new ConstExpression("1"), false)));
        ColumnReference left = new ColumnReference(null, null, "col");
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parse_deleteStatement_parseSucceed() {
        SQLParser sqlParser = new OBMySQLParser();
        Statement actual = sqlParser.parse(new StringReader("delete from tab where col=100"));

        NameReference nameReference = new NameReference(null, "tab", null);
        Delete expect = new Delete(nameReference);
        ColumnReference left = new ColumnReference(null, null, "col");
        ConstExpression right = new ConstExpression("100");
        expect.setWhere(new CompoundExpression(left, right, Operator.EQ));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parse_insertStatement_parseSucceed() {
        SQLParser sqlParser = new OBMySQLParser();
        Statement acutal = sqlParser.parse(new StringReader("insert a.b values(1,default)"));

        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        InsertTable insertTable = new InsertTable(factor);
        List<List<Expression>> values = new ArrayList<>();
        values.add(Arrays.asList(new ConstExpression("1"), new ConstExpression("default")));
        insertTable.setValues(values);
        Insert expect = new Insert(Collections.singletonList(insertTable), null);
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void parse_createtableStatement_parseSucceed() {
        SQLParser sqlParser = new OBMySQLParser();
        String sql = "create table any_schema.abcd (id varchar(64))";
        Statement actual = sqlParser.parse(new StringReader(sql));

        CreateTable expect = new CreateTable(getCreateTableContext(sql), getRelationFactor("any_schema", "abcd"));
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    private Create_table_stmtContext getCreateTableContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_table_stmt();
    }

    private RelationFactor getRelationFactor(String schema, String relation) {
        RelationFactor relationFactor = new RelationFactor(relation);
        relationFactor.setSchema(schema);
        return relationFactor;
    }

}
