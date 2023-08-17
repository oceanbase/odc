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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLRenameTableFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Rename_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTableAction;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

/**
 * Test cases for {@link MySQLRenameTableFactory}
 *
 * @author yh263208
 * @date 2023-06-15 17:06
 * @since ODC_release_4.2.0
 */
public class MySQLRenameTableFactoryTest {

    @Test
    public void generate_renameTable_succeed() {
        StatementFactory<RenameTable> factory =
                new MySQLRenameTableFactory(getContext("rename table a.b to d.e, r to p"));
        RenameTable actual = factory.generate();

        RelationFactor from = new RelationFactor("b");
        from.setSchema("a");
        RelationFactor to = new RelationFactor("e");
        to.setSchema("d");
        RenameTableAction action = new RenameTableAction(from, to);

        RelationFactor from1 = new RelationFactor("r");
        RelationFactor to1 = new RelationFactor("p");
        RenameTableAction action1 = new RenameTableAction(from1, to1);

        RenameTable expect = new RenameTable(Arrays.asList(action, action1));
        Assert.assertEquals(expect, actual);
    }

    private Rename_table_stmtContext getContext(String action) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(action));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.rename_table_stmt();
    }

}
