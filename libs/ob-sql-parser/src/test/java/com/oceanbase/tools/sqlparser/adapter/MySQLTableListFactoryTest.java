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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLTableListFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_listContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.droptable.TableList;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 20:49
 * @Description: []
 */
public class MySQLTableListFactoryTest {
    @Test
    public void testGenerate_SingleTable() {
        String sql = "any_schema1.any_table1";
        Table_listContext ctx = getTableListContext(sql);
        MySQLTableListFactory factory = new MySQLTableListFactory(ctx);
        TableList actual = factory.generate();
        TableList expected = new TableList(ctx,
                Arrays.asList(getRelationFactor("any_schema1", "any_table1")));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGenerate_MultiTables() {
        String sql = "any_schema1.any_table1,any_schema2.any_table2";
        Table_listContext ctx = getTableListContext(sql);
        MySQLTableListFactory factory = new MySQLTableListFactory(ctx);
        TableList actual = factory.generate();
        TableList expected = new TableList(ctx,
                Arrays.asList(getRelationFactor("any_schema1", "any_table1"),
                        getRelationFactor("any_schema2", "any_table2")));

        Assert.assertEquals(expected, actual);
    }

    private Table_listContext getTableListContext(String str) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(str));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.table_list();
    }

    private RelationFactor getRelationFactor(String schema, String relation) {
        RelationFactor relationFactor = new RelationFactor(relation);
        relationFactor.setSchema(schema);
        return relationFactor;
    }
}
