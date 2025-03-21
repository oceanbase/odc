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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleCreateMaterializedViewFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createmview.CreateMaterializedView;
import com.oceanbase.tools.sqlparser.statement.createmview.MaterializedViewOptions;
import com.oceanbase.tools.sqlparser.statement.createmview.MaterializedViewRefreshOption;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

/**
 * @description: Test cases for {@link OracleCreateMaterializedViewFactory}
 * @author: zijia.cj
 * @date: 2025/3/20 10:50
 * @since: 4.3.4
 */
public class OracleCreateMaterializedViewFactoryTest {
    @Test
    public void generate_simpleMView_generateSucceed() {
        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
                "CREATE MATERIALIZED VIEW zijia.test_mv_allsyntax AS select col.* abc from test_table");
        OracleCreateMaterializedViewFactory factory = new OracleCreateMaterializedViewFactory(context);
        CreateMaterializedView actual = factory.generate();

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "test_table", null);
        Select asSelect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        RelationFactor viewName = new RelationFactor("test_mv_allsyntax");
        viewName.setSchema("zijia");
        CreateMaterializedView expect = new CreateMaterializedView(viewName, asSelect);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_allSyntax_generateSucceed() {
        OBParser.Create_mview_stmtContext context = getCreateMaterializedViewContext(
                "CREATE MATERIALIZED VIEW zijia.test_mv_allsyntax "
                        + "(col, PRIMARY KEY (prim)) "
                        + "TABLET_SIZE = 134217728 PCTFREE = 10 PARALLEL 5 "
                        + "partition by hash(prim) "
                        + "with column group (all columns, each column) "
                        + "REFRESH COMPLETE ON DEMAND START WITH 1 NEXT 'abc' ENABLE QUERY REWRITE DISABLE ON QUERY COMPUTATION "
                        + "AS select col.* abc from test_table "
                        + "with check option");
        OracleCreateMaterializedViewFactory factory = new OracleCreateMaterializedViewFactory(context);
        CreateMaterializedView actual = factory.generate();

        RelationReference r = new RelationReference("col", new RelationReference("*", null));
        Projection p = new Projection(r, "abc");
        NameReference from = new NameReference(null, "test_table", null);
        Select asSelect = new Select(new SelectBody(Collections.singletonList(p), Collections.singletonList(from)));
        RelationFactor viewName = new RelationFactor("test_mv_allsyntax");
        viewName.setSchema("zijia");
        CreateMaterializedView expect = new CreateMaterializedView(viewName, asSelect);

        TableOptions tableOptions = new TableOptions();
        tableOptions.setParallel(5);
        tableOptions.setTabletSize(134217728);
        tableOptions.setPctFree(10);
        expect.setTableOptions(tableOptions);

        HashPartition hashPartition = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "prim")), null, null, null);
        expect.setPartition(hashPartition);

        List<ColumnGroupElement> columnGroups = new ArrayList<>();
        columnGroups.add(new ColumnGroupElement(true, false));
        columnGroups.add(new ColumnGroupElement(false, true));
        expect.setColumnGroupElements(columnGroups);

        expect.setColumns(Collections.singletonList("col"));
        SortColumn column = new SortColumn(new ColumnReference(null, null, "prim"));
        OutOfLineConstraint pk = new OutOfLineConstraint(null, Collections.singletonList(column));
        pk.setPrimaryKey(true);
        expect.setPrimaryKey(pk);

        MaterializedViewRefreshOption refreshOption = new MaterializedViewRefreshOption(false, "COMPLETE");
        refreshOption.setRefreshMode("DEMAND");
        refreshOption.setStartWith(new ConstExpression("1"));
        refreshOption.setNext(new ConstExpression("'abc'"));

        MaterializedViewOptions options = new MaterializedViewOptions(refreshOption);
        options.setEnableQueryComputation(false);
        options.setEnableQueryWrite(true);
        expect.setViewOptions(options);
        expect.setWithOption("WITH CHECK OPTION");
        Assert.assertEquals(expect, actual);
    }

    private OBParser.Create_mview_stmtContext getCreateMaterializedViewContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_mview_stmt();
    }
}
