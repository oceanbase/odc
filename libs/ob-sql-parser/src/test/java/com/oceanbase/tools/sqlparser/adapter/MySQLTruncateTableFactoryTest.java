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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLTruncateTableFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Truncate_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

/**
 * Test cases for {@link MySQLTruncateTableFactory}
 *
 * @author yh263208
 * @date 2023-05-23 19:57
 * @since ODC_release_4.2.4
 */
public class MySQLTruncateTableFactoryTest {

    @Test
    public void generate_truncateTable_succeed() {
        StatementFactory<TruncateTable> factory = new MySQLTruncateTableFactory(
                getTruncateTableContext("truncate table a.b"));
        TruncateTable actual = factory.generate();
        RelationFactor table = new RelationFactor("b");
        table.setSchema("a");
        TruncateTable expect = new TruncateTable(table);
        Assert.assertEquals(expect, actual);
    }

    private Truncate_table_stmtContext getTruncateTableContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.truncate_table_stmt();
    }

}
