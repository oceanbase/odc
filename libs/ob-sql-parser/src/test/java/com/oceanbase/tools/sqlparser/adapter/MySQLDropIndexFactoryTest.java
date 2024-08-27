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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLDropIndexFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_index_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.dropindex.DropIndex;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 19:34
 * @Description: []
 */
public class MySQLDropIndexFactoryTest {

    @Test
    public void testDropIndex_WithSchemaName() {
        String sql = "drop index any_index on any_schema.any_table";
        Drop_index_stmtContext ctx = getDropIndexContext(sql);
        MySQLDropIndexFactory factory = new MySQLDropIndexFactory(ctx);
        DropIndex actual = factory.generate();
        DropIndex expected = new DropIndex(ctx, "any_index", getRelationFactor("any_schema", "any_table"));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDropIndex_WithoutSchemaName() {
        String sql = "drop index any_index on any_table";
        Drop_index_stmtContext ctx = getDropIndexContext(sql);
        MySQLDropIndexFactory factory = new MySQLDropIndexFactory(ctx);
        DropIndex actual = factory.generate();
        DropIndex expected = new DropIndex(ctx, "any_index", getRelationFactor(null, "any_table"));
        Assert.assertEquals(expected, actual);
    }

    private Drop_index_stmtContext getDropIndexContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.drop_index_stmt();
    }

    private RelationFactor getRelationFactor(String schema, String relation) {
        RelationFactor relationFactor = new RelationFactor(relation);
        relationFactor.setSchema(schema);
        return relationFactor;
    }
}
