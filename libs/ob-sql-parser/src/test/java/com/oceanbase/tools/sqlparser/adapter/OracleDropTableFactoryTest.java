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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleDropTableFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.droptable.DropTable;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 20:05
 * @Description: []
 */
public class OracleDropTableFactoryTest {
    @Test
    public void testDropTable_WithSchema() {
        String sql = "DROP TABLE ANY_SCHEMA.ANY_TABLE";
        Drop_table_stmtContext ctx = getDropTableContext(sql);
        OracleDropTableFactory factory = new OracleDropTableFactory(ctx);
        DropTable actual = factory.generate();

        DropTable expected = new DropTable(ctx, getRelationFactor("ANY_SCHEMA", "ANY_TABLE"), false, false);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDropTableCascadeConstraints_WithoutSchema() {
        String sql = "DROP TABLE ANY_TABLE CASCADE CONSTRAINTS";
        Drop_table_stmtContext ctx = getDropTableContext(sql);
        OracleDropTableFactory factory = new OracleDropTableFactory(ctx);
        DropTable actual = factory.generate();

        DropTable expected = new DropTable(ctx, getRelationFactor(null, "ANY_TABLE"), true, false);
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
