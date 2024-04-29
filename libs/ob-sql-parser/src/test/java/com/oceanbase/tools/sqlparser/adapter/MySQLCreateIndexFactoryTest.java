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
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLCreateIndexFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_index_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

/**
 * Test cases for {@link MySQLCreateIndexFactory}
 *
 * @author yh263208
 * @date 2023-06-02 15:39
 * @since ODC_release_4.2.0
 */
public class MySQLCreateIndexFactoryTest {

    @Test
    public void generate_createIndex_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create index abc on tb (col, col1)"));
        CreateIndex actual = factory.generate();

        CreateIndex expect = new CreateIndex(new RelationFactor("abc"),
                new RelationFactor("tb"), Arrays.asList(
                        new SortColumn(new ColumnReference(null, null, "col")),
                        new SortColumn(new ColumnReference(null, null, "col1"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createIndexWithUv_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create index abc@uv1 on tb@uv2 (col, col1)"));
        CreateIndex actual = factory.generate();

        RelationFactor r1 = new RelationFactor("abc");
        r1.setUserVariable("@uv1");
        RelationFactor r2 = new RelationFactor("tb");
        r2.setUserVariable("@uv2");
        CreateIndex expect = new CreateIndex(r1, r2, Arrays.asList(
                new SortColumn(new ColumnReference(null, null, "col")),
                new SortColumn(new ColumnReference(null, null, "col1"))));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createUniqueIndex_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create unique index chz.abc on piaoyue.tb (col, col1)"));
        CreateIndex actual = factory.generate();

        RelationFactor relation = new RelationFactor("abc");
        relation.setSchema("chz");
        RelationFactor on = new RelationFactor("tb");
        on.setSchema("piaoyue");
        CreateIndex expect = new CreateIndex(relation, on, Arrays.asList(
                new SortColumn(new ColumnReference(null, null, "col")),
                new SortColumn(new ColumnReference(null, null, "col1"))));
        expect.setUnique(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createFullTextIndex_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create fulltext index if not exists chz.abc on piaoyue.tb (col, col1)"));
        CreateIndex actual = factory.generate();

        RelationFactor relation = new RelationFactor("abc");
        relation.setSchema("chz");
        RelationFactor on = new RelationFactor("tb");
        on.setSchema("piaoyue");
        CreateIndex expect = new CreateIndex(relation, on, Arrays.asList(
                new SortColumn(new ColumnReference(null, null, "col")),
                new SortColumn(new ColumnReference(null, null, "col1"))));
        expect.setFullText(true);
        expect.setIfNotExists(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createSpatialIndex_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create spatial index if not exists chz.abc on piaoyue.tb (col, col1)"));
        CreateIndex actual = factory.generate();

        RelationFactor relation = new RelationFactor("abc");
        relation.setSchema("chz");
        RelationFactor on = new RelationFactor("tb");
        on.setSchema("piaoyue");
        CreateIndex expect = new CreateIndex(relation, on, Arrays.asList(
                new SortColumn(new ColumnReference(null, null, "col")),
                new SortColumn(new ColumnReference(null, null, "col1"))));
        expect.setSpatial(true);
        expect.setIfNotExists(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createIndexUsingBtree_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create unique index abc using btree on tb (col, col1) using hash"));
        CreateIndex actual = factory.generate();

        CreateIndex expect = new CreateIndex(new RelationFactor("abc"),
                new RelationFactor("tb"), Arrays.asList(
                        new SortColumn(new ColumnReference(null, null, "col")),
                        new SortColumn(new ColumnReference(null, null, "col1"))));
        expect.setUnique(true);
        IndexOptions indexOptions = new IndexOptions();
        indexOptions.setUsingBtree(true);
        indexOptions.setUsingHash(true);
        expect.setIndexOptions(indexOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createIndexWithColumnGroup_allColumns_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create index abc on tb (col, col1) with column group(all columns)"));
        CreateIndex actual = factory.generate();

        CreateIndex expect = new CreateIndex(new RelationFactor("abc"),
                new RelationFactor("tb"), Arrays.asList(
                        new SortColumn(new ColumnReference(null, null, "col")),
                        new SortColumn(new ColumnReference(null, null, "col1"))));
        expect.setColumnGroupElements(Collections.singletonList(new ColumnGroupElement(true, false)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createIndexWithColumnGroup_allColumns_eachColumn_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create index abc on tb (col, col1) with column group(all columns, each column)"));
        CreateIndex actual = factory.generate();

        CreateIndex expect = new CreateIndex(new RelationFactor("abc"),
                new RelationFactor("tb"), Arrays.asList(
                        new SortColumn(new ColumnReference(null, null, "col")),
                        new SortColumn(new ColumnReference(null, null, "col1"))));
        expect.setColumnGroupElements(
                Arrays.asList(new ColumnGroupElement(true, false), new ColumnGroupElement(false, true)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withColumnGroup_customGroup_succeed() {
        StatementFactory<CreateIndex> factory = new MySQLCreateIndexFactory(
                getCreateIdxContext("create index abc on tb (col, col1) with column group(g1(col), g2(col, col1))"));
        CreateIndex actual = factory.generate();

        CreateIndex expect = new CreateIndex(new RelationFactor("abc"),
                new RelationFactor("tb"), Arrays.asList(
                        new SortColumn(new ColumnReference(null, null, "col")),
                        new SortColumn(new ColumnReference(null, null, "col1"))));
        List<ColumnGroupElement> columnGroupElements = Arrays.asList(
                new ColumnGroupElement("g1", Collections.singletonList("col")),
                new ColumnGroupElement("g2", Arrays.asList("col", "col1")));
        expect.setColumnGroupElements(columnGroupElements);
        Assert.assertEquals(expect, actual);
    }

    private Create_index_stmtContext getCreateIdxContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_index_stmt();
    }

}
