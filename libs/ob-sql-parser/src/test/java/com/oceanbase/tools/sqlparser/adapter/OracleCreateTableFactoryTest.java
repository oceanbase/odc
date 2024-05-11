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
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleCreateTableFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchAddition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchType;

/**
 * {@link OracleCreateTableFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-26 16:35
 * @since ODC_release_4.1.0
 */
public class OracleCreateTableFactoryTest {

    @Test
    public void generate_onlyColumnDefExists_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64))");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_externalCreateTable_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext("create external table abcd (id varchar(64))");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        expect.setExternal(true);
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_commitOption_generateSucceed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) on commit delete rows");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        expect.setCommitOption("delete");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createTableAsSelect_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table .abcd as select * from tab order by c desc fetch first 12 rows only");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        NameReference from = new NameReference(null, "tab", null);
        SelectBody selectBody =
                new SelectBody(Collections.singletonList(new Projection()), Collections.singletonList(from));
        Select select = new Select(selectBody);
        select.setOrderBy(new OrderBy(
                Collections.singletonList(new SortKey(new RelationReference("c", null), SortDirection.DESC))));
        select.setFetch(
                new Fetch(new ConstExpression("12"), FetchDirection.FIRST, FetchType.COUNT, FetchAddition.ONLY, null));
        expect.setAs(select);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sortKey_succeed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64)) sortkey (a,b)");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions
                .setSortKeys(Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b")));
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_parallel_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) parallel 12, noparallel");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setParallel(12);
        tableOptions.setNoParallel(true);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_tableMode_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) table_mode='abcd',parallel 12, noparallel");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setParallel(12);
        tableOptions.setNoParallel(true);
        tableOptions.setTableMode("'abcd'");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_duplicateScope_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) duplicate_scope='abcd'");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setDuplicateScope("'abcd'");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_locality_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) locality='abcd' force");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setLocality("'abcd' force");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_expireInfo_succeed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64)) expire_info=(1)");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setExpireInfo(new ConstExpression("1"));
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_forArchiveHighCompress_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) compress    for archive high");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setCompress("for archive high");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_useBloomFilter_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) use_bloom_filter=false");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setUseBloomFilter(false);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_readWrite_succeed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64)) read write");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setReadWrite(true);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rowMovement_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext(
                        "create table abcd (id varchar(64)) enable row movement, disable row movement enable_extended_rowid=false");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setEnableRowMovement(true);
        tableOptions.setDisableRowMovement(true);
        tableOptions.setEnableExtendedRowId(false);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_physicalAttrs_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd (id varchar(64)) pctfree=12,pctused 13,initrans 14, maxtrans 15, storage(next 14 initial 16), tablespace abc");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setPctFree(12);
        tableOptions.setPctUsed(13);
        tableOptions.setIniTrans(14);
        tableOptions.setMaxTrans(15);
        tableOptions.setStorage(Arrays.asList("next 14", "initial 16"));
        tableOptions.setTableSpace("abc");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_tableWithPartition_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd ("
                        + "id varchar(64))"
                        + "partition by range(id) (partition a values less than (-2))");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        RangePartitionElement e1 = new RangePartitionElement("a",
                Collections.singletonList(new CompoundExpression(new ConstExpression("2"), null, Operator.SUB)));
        List<Expression> cols = Collections.singletonList(new ColumnReference(null, null, "id"));
        expect.setPartition(new RangePartition(cols, Collections.singletonList(e1), null, null, false));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_formatTableOp_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd (id varchar(64)) format=(ENCODING='aaaa',LINE_DELIMITER=123,SKIP_HEADER=12,EMPTY_FIELD_AS_NULL=true,NULL_IF_EXETERNAL=(1,2,3))");
        StatementFactory<CreateTable> factory = new OracleCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        Map<String, Expression> map = new HashMap<>();
        map.put("ENCODING", new ConstExpression("'aaaa'"));
        map.put("EMPTY_FIELD_AS_NULL", new BoolValue(true));
        map.put("SKIP_HEADER", new ConstExpression("12"));
        CollectionExpression es = new CollectionExpression();
        es.addExpression(new ConstExpression("1"));
        es.addExpression(new ConstExpression("2"));
        es.addExpression(new ConstExpression("3"));
        map.put("NULL_IF_EXETERNAL", es);
        map.put("LINE_DELIMITER", new ConstExpression("123"));
        tableOptions.setFormat(map);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withColumnGroup_allColumns_succeed() {
        Create_table_stmtContext ctx = getCreateTableContext(
                "create table column_group_tbl (id varchar(64)) with column group(all columns)");
        OracleCreateTableFactory factory = new OracleCreateTableFactory(ctx);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("column_group_tbl");
        expect.setColumnGroupElements(Collections.singletonList(new ColumnGroupElement(true, false)));
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withColumnGroup_allColumns_eachColumn_succeed() {
        Create_table_stmtContext ctx = getCreateTableContext(
                "create table column_group_tbl (id varchar(64)) with column group(all columns, each column)");
        OracleCreateTableFactory factory = new OracleCreateTableFactory(ctx);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("column_group_tbl");
        expect.setColumnGroupElements(
                Arrays.asList(new ColumnGroupElement(true, false), new ColumnGroupElement(false, true)));
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_withColumnGroup_customGroup_succeed() {
        Create_table_stmtContext ctx = getCreateTableContext(
                "create table column_group_tbl (id varchar(64)) with column group(g1(id))");
        OracleCreateTableFactory factory = new OracleCreateTableFactory(ctx);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("column_group_tbl");
        List<String> columnNames = Collections.singletonList("id");
        expect.setColumnGroupElements(Collections.singletonList(new ColumnGroupElement("g1", columnNames)));
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_skipIndex_succeed() {
        Create_table_stmtContext ctx = getCreateTableContext(
                "create table skip_index_tbl (id varchar(64) SKIP_INDEX(MIN_MAX,SUM))");
        OracleCreateTableFactory factory = new OracleCreateTableFactory(ctx);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("skip_index_tbl");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        ColumnAttributes attributes = new ColumnAttributes();
        attributes.setSkipIndexTypes(Arrays.asList("MIN_MAX", "SUM"));
        ColumnDefinition column = new ColumnDefinition(new ColumnReference(null, null, "id"), dataType);
        column.setColumnAttributes(attributes);
        expect.setTableElements(Collections.singletonList(column));
        Assert.assertEquals(expect, actual);
    }

    private Create_table_stmtContext getCreateTableContext(String expr) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(expr));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.create_table_stmt();
    }

}
