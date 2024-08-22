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
package com.oceanbase.tools.sqlparser.adapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLDropTableFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.droptable.DropTable;
import com.oceanbase.tools.sqlparser.statement.droptable.TableList;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 19:55
 * @Description: []
 */
public class MySQLDropTableFactoryTest {
    @Test
    public void testDropTable_SingleTableWithSchema() {
        String sql = "drop table any_schema.any_table";
        Drop_table_stmtContext ctx = getDropTableContext(sql);
        MySQLDropTableFactory factory = new MySQLDropTableFactory(ctx);
        DropTable actual = factory.generate();

        List<RelationFactor> relationFactors = Collections.singletonList(getRelationFactor("any_schema", "any_table"));
        TableList tableList = new TableList(ctx.table_list(), relationFactors);
        DropTable expected = new DropTable(ctx, tableList, false, false, false, false, false);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDropTemporaryTableIfExists_SingleTableWithoutSchema() {
        String sql = "drop temporary table if exists any_table";
        Drop_table_stmtContext ctx = getDropTableContext(sql);
        MySQLDropTableFactory factory = new MySQLDropTableFactory(ctx);
        DropTable actual = factory.generate();

        List<RelationFactor> relationFactors = Collections.singletonList(getRelationFactor(null, "any_table"));
        TableList tableList = new TableList(ctx.table_list(), relationFactors);
        DropTable expected = new DropTable(ctx, tableList, true, false, true, false, false);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDropMaterializedTableIfExists_MultiTablesWithSchema() {
        String sql = "drop materialized tables if exists any_schema_1.any_table_1, any_schema_2.any_table_2";
        Drop_table_stmtContext ctx = getDropTableContext(sql);
        MySQLDropTableFactory factory = new MySQLDropTableFactory(ctx);
        DropTable actual = factory.generate();

        List<RelationFactor> relationFactors =
                Arrays.asList(getRelationFactor("any_schema_1", "any_table_1"), getRelationFactor(
                        "any_schema_2", "any_table_2"));
        TableList tableList = new TableList(ctx.table_list(), relationFactors);
        DropTable expected = new DropTable(ctx, tableList, false, true, true, false, false);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDropTable_MultiTablesWithoutSchema() {
        String sql = "drop tables any_table_1, any_table_2";
        Drop_table_stmtContext ctx = getDropTableContext(sql);
        MySQLDropTableFactory factory = new MySQLDropTableFactory(ctx);
        DropTable actual = factory.generate();

        List<RelationFactor> relationFactors =
                Arrays.asList(getRelationFactor(null, "any_table_1"), getRelationFactor(null, "any_table_2"));
        TableList tableList = new TableList(ctx.table_list(), relationFactors);
        DropTable expected = new DropTable(ctx, tableList, false, false, false, false, false);
        Assert.assertEquals(expected, actual);
    }

    private Drop_table_stmtContext getDropTableContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.drop_table_stmt();
    }

    private RelationFactor getRelationFactor(String schema, String relation) {
        RelationFactor relationFactor = new RelationFactor(relation);
        relationFactor.setSchema(schema);
        return relationFactor;
    }
}
