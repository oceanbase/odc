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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLAlterTableFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

public class MySQLAlterTableFactoryTest {

    @Test
    public void generate_alterTable_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter table a.b table_mode='aaa' USE_BLOOM_FILTER=true, add id varchar(64)"));
        AlterTable actual = factory.generate();

        TableOptions tableOptions = new TableOptions();
        tableOptions.setTableMode("'aaa'");
        tableOptions.setUseBloomFilter(true);
        AlterTableAction a1 = new AlterTableAction();
        a1.setTableOptions(tableOptions);

        AlterTableAction a2 = new AlterTableAction();
        CharacterType type = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition d = new ColumnDefinition(new ColumnReference(null, null, "id"), type);
        a2.setAddColumns(Collections.singletonList(d));

        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Arrays.asList(a1, a2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterTable1_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter table a.b"));
        AlterTable actual = factory.generate();

        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterExternalTableAddEmptyPartition_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter external table a.b add partition () location 'abcd'"));
        AlterTable actual = factory.generate();

        AlterTableAction action = new AlterTableAction();
        action.setExternalTableLocation("'abcd'");
        action.setAddExternalTablePartition(new HashMap<>());
        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Collections.singletonList(action));
        expect.setExternal(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterExternalTableAddPartition_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext(
                        "alter external table a.b add partition (col='aaa', col1=@@global.ss) location 'abcd'"));
        AlterTable actual = factory.generate();

        AlterTableAction action = new AlterTableAction();
        action.setExternalTableLocation("'abcd'");
        Map<String, Expression> partitions = new HashMap<>();
        partitions.put("col", new ConstExpression("'aaa'"));
        partitions.put("col1", new ConstExpression("@@global.ss"));
        action.setAddExternalTablePartition(partitions);
        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Collections.singletonList(action));
        expect.setExternal(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterExternalTableDropPartition_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter external table a.b drop partition location 'abcd'"));
        AlterTable actual = factory.generate();

        AlterTableAction action = new AlterTableAction();
        action.setExternalTableLocation("'abcd'");
        action.setDropExternalTablePartition(true);
        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Collections.singletonList(action));
        expect.setExternal(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterExternalTable1_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter external table a.b refresh"));
        AlterTable actual = factory.generate();

        AlterTableAction a = new AlterTableAction();
        a.setRefresh(true);
        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Collections.singletonList(a));
        expect.setExternal(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterTable_addColumnGroup_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter table a.b add column group(all columns)"));
        AlterTable actual = factory.generate();

        AlterTableAction action = new AlterTableAction();
        action.setAddColumnGroupElements(Collections.singletonList(new ColumnGroupElement(true, false)));
        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Collections.singletonList(action));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterTable_dropColumnGroup_succeed() {
        StatementFactory<AlterTable> factory = new MySQLAlterTableFactory(
                getAlterContext("alter table a.b drop column group(all columns)"));
        AlterTable actual = factory.generate();

        AlterTableAction action = new AlterTableAction();
        action.setDropColumnGroupElements(Collections.singletonList(new ColumnGroupElement(true, false)));
        AlterTable expect = new AlterTable(getRelationFactor("a", "b"), Collections.singletonList(action));
        Assert.assertEquals(expect, actual);
    }

    private Alter_table_stmtContext getAlterContext(String action) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(action));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.alter_table_stmt();
    }

    private RelationFactor getRelationFactor(String schema, String relation) {
        RelationFactor relationFactor = new RelationFactor(relation);
        relationFactor.setSchema(schema);
        return relationFactor;
    }

}
