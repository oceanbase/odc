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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLCreateTableFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.IntervalExpression;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

/**
 * {@link MySQLCreateTableFactoryTest}
 *
 * @author yh263208
 * @date 2022-12-26 16:02
 * @since ODC_release_4.1.0
 */
public class MySQLCreateTableFactoryTest {

    @Test
    public void generate_onlyColumnDefExists_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64))");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_temporaryTable_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext("create temporary table abcd (id varchar(64))");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        expect.setTemporary(true);
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_externalTable_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext("create external table abcd (id varchar(64))");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        expect.setExternal(true);
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_createTableAsSelect_generateSucceed() {
        Create_table_stmtContext context = getCreateTableContext("create table .abcd as select * from tab");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        NameReference from = new NameReference(null, "tab", null);
        SelectBody selectBody =
                new SelectBody(Collections.singletonList(new Projection()), Collections.singletonList(from));
        Select select = new Select(selectBody);
        expect.setAs(select);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_sortKey_succeed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64)) sortkey (a,b)");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
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
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
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
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
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
    public void generate_rowFormat_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) row_format=COMPACT,duplicate_scope='abcd'");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setDuplicateScope("'abcd'");
        tableOptions.setRowFormat("COMPACT");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_charsetAndCollation_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext(
                        "create table abcd (id varchar(64)) default charset=utf8,collate=u8mb4,locality='abcd' force");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setLocality("'abcd' force");
        tableOptions.setCharset("utf8");
        tableOptions.setCollation("u8mb4");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_autoIncrement_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) expire_info=(1) auto_increment=15");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setExpireInfo(new ConstExpression("1"));
        tableOptions.setAutoIncrement(new BigDecimal("15"));
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_Compression_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) COMPRESSION 'aaa'");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setCompression("'aaa'");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_useBloomFilter_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) use_bloom_filter=false");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
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
    public void generate_readonly_succeed() {
        Create_table_stmtContext context = getCreateTableContext("create table abcd (id varchar(64)) read only");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setReadOnly(true);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_comment_succeed() {
        Create_table_stmtContext context =
                getCreateTableContext("create table abcd (id varchar(64)) lob_inrow_threshold=456 comment 'aaaaa'");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setComment("'aaaaa'");
        tableOptions.setLobInRowThreshold(456);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_physicalAttrs_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd (id varchar(64)) default_lob_inrow_threshold=123 pctfree=12 tablespace abc");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setPctFree(12);
        tableOptions.setTableSpace("abc");
        tableOptions.setDefaultLobInRowThreshold(123);
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_formatTableOp_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd (id varchar(64)) kv_attributes='12' format=(ENCODING='aaaa',LINE_DELIMITER=123,SKIP_HEADER=12,EMPTY_FIELD_AS_NULL=true,NULL_IF_EXETERNAL=(1,2,3))");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
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
        tableOptions.setKvAttributes("'12'");
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_ob40NewTableOptions_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd (id varchar(64)) delay_key_write=12 avg_row_length=13 checksum=15 auto_increment_mode='aaa' enable_extended_rowid=true ttl(12 + interval 12 year, 13 + interval 45 day)");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        TableOptions tableOptions = new TableOptions();
        tableOptions.setDelayKeyWrite(12);
        tableOptions.setAvgRowLength(13);
        tableOptions.setChecksum(15);
        tableOptions.setAutoIncrementMode("'aaa'");
        tableOptions.setEnableExtendedRowId(true);
        tableOptions.setTtls(Arrays.asList(
                new CompoundExpression(new ConstExpression("12"),
                        new IntervalExpression(new ConstExpression("12"), "year"), Operator.ADD),
                new CompoundExpression(new ConstExpression("13"),
                        new IntervalExpression(new ConstExpression("45"), "day"), Operator.ADD)));
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_partition_succeed() {
        Create_table_stmtContext context = getCreateTableContext(
                "create table abcd ("
                        + "id varchar(64)) partition by hash(a) partitions 12 ("
                        + "partition a.b,"
                        + "partition d id 14)");
        StatementFactory<CreateTable> factory = new MySQLCreateTableFactory(context);
        CreateTable actual = factory.generate();

        CreateTable expect = new CreateTable("abcd");
        DataType dataType = new CharacterType("varchar", new BigDecimal("64"));
        expect.setTableElements(
                Collections.singletonList(new ColumnDefinition(new ColumnReference(null, null, "id"), dataType)));
        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        expect.setPartition(new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), null, 12));
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
