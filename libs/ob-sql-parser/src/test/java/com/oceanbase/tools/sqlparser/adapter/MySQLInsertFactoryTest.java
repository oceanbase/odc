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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLInsertFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertBody;
import com.oceanbase.tools.sqlparser.statement.insert.SingleTableInsert;

/**
 * {@link MySQLInsertFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 20:21
 * @since ODC_release_4.1.0
 */
public class MySQLInsertFactoryTest {

    @Test
    public void generate_singleInsertWithOutColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext("insert into tab values(1,1,2)");
        StatementFactory<Insert> factory = new MySQLInsertFactory(context);
        Insert acutal = factory.generate();

        SingleTableInsert expect = new SingleTableInsert(new InsertBody());
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void generate_singleInsertWithColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext("insert into tab(col, col1, tab.col2) values(1,1,2)");
        StatementFactory<Insert> factory = new MySQLInsertFactory(context);
        Insert acutal = factory.generate();

        ColumnReference c1 = new ColumnReference(null, null, "col");
        ColumnReference c2 = new ColumnReference(null, null, "col1");
        ColumnReference c3 = new ColumnReference(null, "tab", "col2");
        InsertBody insertBody = new InsertBody();
        insertBody.setColumns(Arrays.asList(c1, c2, c3));
        SingleTableInsert expect = new SingleTableInsert(insertBody);
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void generate_singleReplaceWithColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext("replace into tab(col, col1, tab.col2) values(1,1,2)");
        StatementFactory<Insert> factory = new MySQLInsertFactory(context);
        Insert acutal = factory.generate();

        ColumnReference c1 = new ColumnReference(null, null, "col");
        ColumnReference c2 = new ColumnReference(null, null, "col1");
        ColumnReference c3 = new ColumnReference(null, "tab", "col2");
        InsertBody insertBody = new InsertBody();
        insertBody.setColumns(Arrays.asList(c1, c2, c3));
        SingleTableInsert expect = new SingleTableInsert(insertBody);
        expect.setReplace(true);
        Assert.assertEquals(expect, acutal);
    }

    private Insert_stmtContext getInsertContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.insert_stmt();
    }

}
