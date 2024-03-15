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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleTruncateTableFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Truncate_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

/**
 * {@link OracleTruncateTableFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 16:16
 * @since ODC_release_4.1.0
 */
public class OracleTruncateTableFactoryTest {

    @Test
    public void generate_truncateTable_succeed() {
        StatementFactory<TruncateTable> factory = new OracleTruncateTableFactory(
                getTruncateTableContext("truncate table a.b"));
        TruncateTable actual = factory.generate();
        RelationFactor table = new RelationFactor("b");
        table.setSchema("a");
        TruncateTable expect = new TruncateTable(table);
        Assert.assertEquals(expect, actual);
    }

    private Truncate_table_stmtContext getTruncateTableContext(String columnDef) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(columnDef));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.truncate_table_stmt();
    }

}
