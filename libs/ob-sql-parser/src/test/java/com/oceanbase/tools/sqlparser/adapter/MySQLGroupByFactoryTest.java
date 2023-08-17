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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLSortKeyFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Groupby_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sort_key_for_group_byContext;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;

/**
 * {@link MySQLGroupByFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-12 15:20
 * @since ODC_release_4.1.0
 */
public class MySQLGroupByFactoryTest {

    @Test
    public void generate_exprGroupbyClause_generateExprGroupBySucceed() {
        Groupby_clauseContext context = getGroupByClauseContext("select 1 from abc group by col");
        Sort_key_for_group_byContext s = context.sort_list_for_group_by().sort_key_for_group_by(0);
        StatementFactory<SortKey> factory = new MySQLSortKeyFactory(s);
        GroupBy actual = factory.generate();

        GroupBy expect = new SortKey(new ColumnReference(null, null, "col"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_colRefDesc_generateExprGroupBySucceed() {
        Groupby_clauseContext context = getGroupByClauseContext("select 1 from abc group by tab.col desc");
        Sort_key_for_group_byContext s = context.sort_list_for_group_by().sort_key_for_group_by(0);
        StatementFactory<SortKey> factory = new MySQLSortKeyFactory(s);
        GroupBy actual = factory.generate();

        GroupBy expect = new SortKey(new ColumnReference(null, "tab", "col"), SortDirection.DESC);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_colRefAsc_generateExprGroupBySucceed() {
        Groupby_clauseContext context = getGroupByClauseContext("select 1 from abc group by tab.col asc");
        Sort_key_for_group_byContext s = context.sort_list_for_group_by().sort_key_for_group_by(0);
        StatementFactory<SortKey> factory = new MySQLSortKeyFactory(s);
        GroupBy actual = factory.generate();

        GroupBy expect = new SortKey(new ColumnReference(null, "tab", "col"), SortDirection.ASC);
        Assert.assertEquals(expect, actual);
    }

    private Groupby_clauseContext getGroupByClauseContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.simple_select().groupby_clause();
    }

}
