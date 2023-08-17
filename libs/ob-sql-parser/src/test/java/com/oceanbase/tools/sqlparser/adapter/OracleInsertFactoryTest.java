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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleInsertFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertBody;
import com.oceanbase.tools.sqlparser.statement.insert.MultiTableInsert;
import com.oceanbase.tools.sqlparser.statement.insert.SingleTableInsert;

/**
 * {@link OracleInsertFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 19:43
 * @since ODC_release_4.1.0
 */
public class OracleInsertFactoryTest {

    @Test
    public void generate_singleInsertWithOutColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext("insert into tab values(1,1,2)");
        StatementFactory<Insert> factory = new OracleInsertFactory(context);
        Insert acutal = factory.generate();

        SingleTableInsert expect = new SingleTableInsert(new InsertBody());
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void generate_singleInsertWithColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext("insert into tab(col, col1, tab.col2) values(1,1,2)");
        StatementFactory<Insert> factory = new OracleInsertFactory(context);
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
    public void generate_multiInsertWithColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext(
                "insert all into tab (col, col1) values(1,2) into tab1 values(1,2) select 1 from dual");
        StatementFactory<Insert> factory = new OracleInsertFactory(context);
        Insert acutal = factory.generate();

        ColumnReference c1 = new ColumnReference(null, null, "col");
        ColumnReference c2 = new ColumnReference(null, null, "col1");
        InsertBody insertBody = new InsertBody();
        insertBody.setColumns(Arrays.asList(c1, c2));
        MultiTableInsert expect = new MultiTableInsert(Arrays.asList(insertBody, new InsertBody()));
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void generate_conditionalMultiInsertWithColumns_generateSucceed() {
        Insert_stmtContext context = getInsertContext("insert all "
                + "when 1+3 then into tab(col, col1) values(1,2) into tab1 values(3,4) "
                + "when 1+3 then into tab5(tab.col, col122) values(1,2) "
                + "else into tab7(tab.col1, col12) values(1,2) select 1 from dual");
        StatementFactory<Insert> factory = new OracleInsertFactory(context);
        Insert acutal = factory.generate();

        ColumnReference c1 = new ColumnReference(null, null, "col");
        ColumnReference c2 = new ColumnReference(null, null, "col1");
        InsertBody b1 = new InsertBody();
        b1.setColumns(Arrays.asList(c1, c2));
        InsertBody b2 = new InsertBody();
        ColumnReference c3 = new ColumnReference(null, "tab", "col");
        ColumnReference c4 = new ColumnReference(null, null, "col122");
        InsertBody b3 = new InsertBody();
        b3.setColumns(Arrays.asList(c3, c4));
        ColumnReference c5 = new ColumnReference(null, "tab", "col1");
        ColumnReference c6 = new ColumnReference(null, null, "col12");
        InsertBody b4 = new InsertBody();
        b4.setColumns(Arrays.asList(c5, c6));
        MultiTableInsert expect = new MultiTableInsert(Arrays.asList(b1, b2, b3, b4));
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
