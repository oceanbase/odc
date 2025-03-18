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
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.CreateMaterializedViewOpts;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshInterval;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOnClause;
import com.oceanbase.tools.sqlparser.statement.creatematerializedview.MaterializedViewRefreshOpts;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
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
    public void parse_createMViewStatement_parseSucceed() {
        SQLParser parser = new OBMySQLParser();
        Statement actual = parser.parse(new StringReader(
                "CREATE MATERIALIZED VIEW `zijia`.`test_mv_allsyntax` (PRIMARY KEY (prim)) " +
                        "DEFAULT CHARSET = gbk ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE ENABLE_MACRO_BLOCK_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5\n"
                        +
                        " partition by hash(prim)\n" +
                        " WITH COLUMN GROUP(all columns, each column) REFRESH COMPLETE ON DEMAND START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY ENABLE QUERY REWRITE ENABLE ON QUERY COMPUTATION "
                        +
                        "AS select `zijia`.`test_mv_base`.`col1` AS `prim`,`zijia`.`test_mv_base`.`col2` AS `col2`,`zijia`.`test_mv_base`.`col3` AS `col3`,`zijia`.`test_mv_base`.`col4` AS `col4` from `zijia`.`test_mv_base`"));

        TableOptions tableOptions = new TableOptions();
        tableOptions.setParallel(5);
        tableOptions.setBlockSize(16384);
        tableOptions.setReplicaNum(1);
        tableOptions.setUseBloomFilter(false);
        tableOptions.setTabletSize(134217728);
        tableOptions.setPctFree(10);
        tableOptions.setCompression("'zstd_1.3.8'");
        tableOptions.setRowFormat("DYNAMIC");
        tableOptions.setCharset("gbk");
        tableOptions.setEnableMacroBlockBloomFilter(false);

        HashPartition hashPartition = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "prim")), null, null, null);

        List<ColumnGroupElement> columnGroupElements = new ArrayList<>();
        columnGroupElements.add(new ColumnGroupElement(true, false));
        columnGroupElements.add(new ColumnGroupElement(false, true));

        MaterializedViewRefreshInterval materializedViewRefreshInterval =
                new MaterializedViewRefreshInterval("sysdate()", 1L, "DAY");
        MaterializedViewRefreshOpts materializedViewRefreshOpts = new MaterializedViewRefreshOpts("COMPLETE",
                materializedViewRefreshInterval, new MaterializedViewRefreshOnClause("DEMAND"));
        CreateMaterializedViewOpts createMaterializedViewOpts =
                new CreateMaterializedViewOpts(true, true, materializedViewRefreshOpts);

        CreateMaterializedView createMaterializedView = new CreateMaterializedView();
        createMaterializedView.setTableOptions(tableOptions);
        createMaterializedView.setPartition(hashPartition);
        createMaterializedView.setColumnGroupElements(columnGroupElements);
        createMaterializedView.setCreateMaterializedViewOpts(createMaterializedViewOpts);

        Assert.assertEquals(createMaterializedView, actual);
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
