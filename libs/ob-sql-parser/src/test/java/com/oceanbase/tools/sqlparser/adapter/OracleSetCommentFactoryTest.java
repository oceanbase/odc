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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleSetCommentFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_comment_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.SetComment;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

/**
 * Test cases for {@link OracleSetCommentFactory}
 *
 * @author yh263208
 * @date 2023-07-31 11:48
 * @since ODC_release_4.2.0
 */
public class OracleSetCommentFactoryTest {

    @Test
    public void generate_commentTable_succeed() {
        StatementFactory<SetComment> factory = new OracleSetCommentFactory(
                getContext("comment on table abcd.tb@quqywe is 'asdasdad'"));
        SetComment actual = factory.generate();

        RelationFactor table = new RelationFactor("tb");
        table.setSchema("abcd");
        table.setUserVariable("@quqywe");
        SetComment expect = new SetComment(table, "'asdasdad'");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_commentColumn_succeed() {
        StatementFactory<SetComment> factory = new OracleSetCommentFactory(
                getContext("comment on column abcd.tb.quqywe is 'asdasdad'"));
        SetComment actual = factory.generate();

        ColumnReference column = new ColumnReference("abcd", "tb", "quqywe");
        SetComment expect = new SetComment(column, "'asdasdad'");
        Assert.assertEquals(expect, actual);
    }

    private Set_comment_stmtContext getContext(String action) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(action));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.set_comment_stmt();
    }
}
