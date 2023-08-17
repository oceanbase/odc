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

import com.oceanbase.tools.sqlparser.adapter.oracle.OraclePartitionFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_partition_optionContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubHashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;

/**
 * Test cases for {@link OraclePartitionFactory}
 *
 * @author yh263208
 * @date 2023-05-31 21:35
 * @since ODC_release_4.2.0
 */
public class OraclePartitionFactoryTest {

    @Test
    public void generate_hashPartition_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(
                getPartitionContext("partition by hash(a,b)"));
        Partition actual = factory.generate();

        List<Expression> cols = Arrays.asList(new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"));
        HashPartition expect = new HashPartition(cols, null, null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithPartitionElts_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by hash(a,b) partitions 12 ("
                        + "partition a.b@c,"
                        + "partition d id 14,"
                        + "partition id 15 tablespace tbs, "
                        + "partition id 16 nocompress,"
                        + "partition id 17 tablespace tbs1 compress for oltp)"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        e1.setUserVariable("@c");
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        HashPartitionElement e3 = new HashPartitionElement(null);
        PartitionOptions options = new PartitionOptions();
        options.setId(15);
        options.setTableSpace("tbs");
        e3.setPartitionOptions(options);
        HashPartitionElement e4 = new HashPartitionElement(null);
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(16);
        options1.setNoCompress(true);
        e4.setPartitionOptions(options1);
        HashPartitionElement e5 = new HashPartitionElement(null);
        PartitionOptions options2 = new PartitionOptions();
        options2.setId(17);
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e5.setPartitionOptions(options2);
        HashPartition expect = new HashPartition(cols, Arrays.asList(e1, e2, e3, e4, e5), null, 12);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithSubPartitions_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by hash(a,b) partitions 12 ("
                        + "partition a.b@c ("
                        + "subpartition a.b pctfree=13,"
                        + "subpartition b pctused 14),"
                        + "partition d id 14) noCompress"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        e1.setUserVariable("@c");
        SubHashPartitionElement se1 = new SubHashPartitionElement("b");
        se1.setSchema("a");
        PartitionOptions o1 = new PartitionOptions();
        o1.setPctFree(13);
        se1.setPartitionOptions(o1);
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setPctUsed(14);
        se2.setPartitionOptions(o2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o4 = new PartitionOptions();
        o4.setId(14);
        e2.setPartitionOptions(o4);
        HashPartition expect = new HashPartition(cols, Arrays.asList(e1, e2), null, 12);
        PartitionOptions o3 = new PartitionOptions();
        o3.setNoCompress(true);
        expect.setPartitionOptions(o3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithSubPartitionOptions_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by hash(a,b) subpartition by hash(c) subpartition template("
                        + "subpartition a.b pctfree=13,"
                        + "subpartition b pctused 14) partitions 12 ("
                        + "partition a.b@c ("
                        + "subpartition a.b pctfree=13,"
                        + "subpartition b pctused 14),"
                        + "partition d id 14) noCompress"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        e1.setUserVariable("@c");
        SubHashPartitionElement se1 = new SubHashPartitionElement("b");
        se1.setSchema("a");
        PartitionOptions o1 = new PartitionOptions();
        o1.setPctFree(13);
        se1.setPartitionOptions(o1);
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setPctUsed(14);
        se2.setPartitionOptions(o2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o7 = new PartitionOptions();
        o7.setId(14);
        e2.setPartitionOptions(o7);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "hash");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        HashPartition expect = new HashPartition(cols, Arrays.asList(e1, e2), subPartitionOption, 12);
        PartitionOptions o3 = new PartitionOptions();
        o3.setNoCompress(true);
        expect.setPartitionOptions(o3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithSubPartitionOptionsQuantity_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by hash(a,b) subpartition by hash(c) subpartitions 165 partitions 12 ("
                        + "partition a.b@c ("
                        + "subpartition a.b pctfree=13,"
                        + "subpartition b pctused 14),"
                        + "partition d id 14) noCompress"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        e1.setUserVariable("@c");
        SubHashPartitionElement se1 = new SubHashPartitionElement("b");
        se1.setSchema("a");
        PartitionOptions o1 = new PartitionOptions();
        o1.setPctFree(13);
        se1.setPartitionOptions(o1);
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setPctUsed(14);
        se2.setPartitionOptions(o2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o7 = new PartitionOptions();
        o7.setId(14);
        e2.setPartitionOptions(o7);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "hash");
        subPartitionOption.setSubPartitionNum(165);
        HashPartition expect = new HashPartition(cols, Arrays.asList(e1, e2), subPartitionOption, 12);
        PartitionOptions o3 = new PartitionOptions();
        o3.setNoCompress(true);
        expect.setPartitionOptions(o3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithPartitionElts_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by range(a,b) ("
                        + "partition a.b@c values less than (-2, maxvalue) tablespace tbs1 compress for oltp,"
                        + "partition d values less than (+3) id 14 nocompress,"
                        + "partition values less than (func(1,2)) id 15 tablespace tbs)"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(14);
        options1.setNoCompress(true);
        e2.setPartitionOptions(options1);
        FunctionParam p1 = new ExpressionParam(new ConstExpression("1"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("2"));
        RangePartitionElement e3 = new RangePartitionElement(null,
                Collections.singletonList(new FunctionCall("func", Arrays.asList(p1, p2))));
        PartitionOptions options = new PartitionOptions();
        options.setId(15);
        options.setTableSpace("tbs");
        e3.setPartitionOptions(options);
        RangePartition expect = new RangePartition(cols, Arrays.asList(e1, e2, e3), null, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitions_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by range(a,b) ("
                        + "partition a.b@c values less than (-2, maxvalue) tablespace tbs1 compress for oltp ("
                        + "subpartition a.b values less than (+3) INITRANS 12,"
                        + "subpartition b values less than (maxvalue) MAXTRANS 13),"
                        + "partition d values less than (+3) id 14 nocompress)"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        PartitionOptions sop1 = new PartitionOptions();
        sop1.setIniTrans(12);
        se1.setPartitionOptions(sop1);
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        PartitionOptions sop2 = new PartitionOptions();
        sop2.setMaxTrans(13);
        se2.setPartitionOptions(sop2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions options1 = new PartitionOptions();
        options1.setNoCompress(true);
        options1.setId(14);
        e2.setPartitionOptions(options1);
        RangePartition expect = new RangePartition(cols, Arrays.asList(e1, e2), null, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitionOptions_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by range(a,b) subpartition by range (c) subpartition template ("
                        + "subpartition a.b values less than (+3) storage(next 12 initial 15 minextents 16 maxextents 17),"
                        + "subpartition b values less than (maxvalue) tablespace tbs)("
                        + "partition a.b@c values less than (-2, maxvalue) tablespace tbs1 compress for oltp ("
                        + "subpartition a.b values less than (+3) storage(next 12 initial 15 minextents 16 maxextents 17),"
                        + "subpartition b values less than (maxvalue) tablespace tbs),"
                        + "partition d values less than (+3) id 14 nocompress)"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        PartitionOptions sop1 = new PartitionOptions();
        sop1.setStorage(Arrays.asList("next 12", "initial 15", "minextents 16", "maxextents 17"));
        se1.setPartitionOptions(sop1);
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        PartitionOptions sop2 = new PartitionOptions();
        sop2.setTableSpace("tbs");
        se2.setPartitionOptions(sop2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(14);
        options1.setNoCompress(true);
        e2.setPartitionOptions(options1);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "range");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        RangePartition expect = new RangePartition(cols, Arrays.asList(e1, e2), subPartitionOption, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitionOptionsQuantity_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by range(a,b) interval('abc') subpartition by range (c) ("
                        + "partition a.b@c values less than (2, maxvalue) tablespace tbs1 compress for oltp ("
                        + "subpartition a.b values less than (+3) storage(next 12 initial 15 minextents 16 maxextents 17),"
                        + "subpartition b values less than (maxvalue) tablespace tbs),"
                        + "partition d values less than (+3) id 14 nocompress)"));
        Partition actual = factory.generate();

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        RangePartitionElement e1 = new RangePartitionElement("b",
                Arrays.asList(new ConstExpression("2"), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        PartitionOptions sop1 = new PartitionOptions();
        sop1.setStorage(Arrays.asList("next 12", "initial 15", "minextents 16", "maxextents 17"));
        se1.setPartitionOptions(sop1);
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        PartitionOptions sop2 = new PartitionOptions();
        sop2.setTableSpace("tbs");
        se2.setPartitionOptions(sop2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(14);
        options1.setNoCompress(true);
        e2.setPartitionOptions(options1);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "range");
        RangePartition expect = new RangePartition(cols, Arrays.asList(e1, e2), subPartitionOption, null, false);
        expect.setInterval(new ConstExpression("'abc'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithPartitionElts_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by list(a,b) ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp,"
                        + "partition d values (3) id 14 nocompress,"
                        + "partition values ('aaaddd') id 15 tablespace tbs)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 =
                new ListPartitionElement("b", Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);
        ListPartitionElement e2 = new ListPartitionElement("d", Collections.singletonList(new ConstExpression("3")));
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(14);
        options1.setNoCompress(true);
        e2.setPartitionOptions(options1);
        ListPartitionElement e3 =
                new ListPartitionElement(null, Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions options = new PartitionOptions();
        options.setId(15);
        options.setTableSpace("tbs");
        e3.setPartitionOptions(options);
        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2, e3), null, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithSubPartitions_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by list(a,b) ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp("
                        + "subpartition a.b values (2) INITRANS 12,"
                        + "subpartition b values ('maxvalue') MAXTRANS 13),"
                        + "partition d values (3) id 14 nocompress,"
                        + "partition values ('aaaddd') id 15 tablespace tbs)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 =
                new ListPartitionElement("b", Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);

        SubListPartitionElement se1 =
                new SubListPartitionElement("b", Collections.singletonList(new ConstExpression("2")));
        se1.setSchema("a");
        PartitionOptions sop1 = new PartitionOptions();
        sop1.setIniTrans(12);
        se1.setPartitionOptions(sop1);

        SubListPartitionElement se2 =
                new SubListPartitionElement("b", Collections.singletonList(new ConstExpression("'maxvalue'")));
        PartitionOptions sop2 = new PartitionOptions();
        sop2.setMaxTrans(13);
        se2.setPartitionOptions(sop2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));

        ListPartitionElement e2 = new ListPartitionElement("d", Collections.singletonList(new ConstExpression("3")));
        PartitionOptions options1 = new PartitionOptions();
        options1.setNoCompress(true);
        options1.setId(14);
        e2.setPartitionOptions(options1);

        ListPartitionElement e3 =
                new ListPartitionElement(null, Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions options3 = new PartitionOptions();
        options3.setTableSpace("tbs");
        options3.setId(15);
        e3.setPartitionOptions(options3);

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2, e3), null, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithSubPartitionOptions_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by list(a,b) subpartition by list(c) subpartition template("
                        + "subpartition a.b values (2) INITRANS 12,"
                        + "subpartition b values ('maxvalue') MAXTRANS 13) ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp("
                        + "subpartition a.b values (2) INITRANS 12,"
                        + "subpartition b values ('maxvalue') MAXTRANS 13),"
                        + "partition d values (3) id 14 nocompress,"
                        + "partition values ('aaaddd') id 15 tablespace tbs)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 =
                new ListPartitionElement("b", Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);

        SubListPartitionElement se1 =
                new SubListPartitionElement("b", Collections.singletonList(new ConstExpression("2")));
        se1.setSchema("a");
        PartitionOptions sop1 = new PartitionOptions();
        sop1.setIniTrans(12);
        se1.setPartitionOptions(sop1);

        SubListPartitionElement se2 =
                new SubListPartitionElement("b", Collections.singletonList(new ConstExpression("'maxvalue'")));
        PartitionOptions sop2 = new PartitionOptions();
        sop2.setMaxTrans(13);
        se2.setPartitionOptions(sop2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));

        ListPartitionElement e2 = new ListPartitionElement("d", Collections.singletonList(new ConstExpression("3")));
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(14);
        options1.setNoCompress(true);
        e2.setPartitionOptions(options1);

        ListPartitionElement e3 =
                new ListPartitionElement(null, Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions options3 = new PartitionOptions();
        options3.setId(15);
        options3.setTableSpace("tbs");
        e3.setPartitionOptions(options3);

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "list");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2, e3), subPartitionOption, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithSubPartitionOptionsQuantity_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by list(a,b) subpartition by list(c) ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp("
                        + "subpartition a.b values (2) INITRANS 12,"
                        + "subpartition b values ('maxvalue') MAXTRANS 13),"
                        + "partition d values (3) id 14 nocompress,"
                        + "partition values ('aaaddd') id 15 tablespace tbs)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 =
                new ListPartitionElement("b", Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        e1.setUserVariable("@c");
        PartitionOptions options2 = new PartitionOptions();
        options2.setCompress("for oltp");
        options2.setTableSpace("tbs1");
        e1.setPartitionOptions(options2);

        SubListPartitionElement se1 =
                new SubListPartitionElement("b", Collections.singletonList(new ConstExpression("2")));
        se1.setSchema("a");
        PartitionOptions sop1 = new PartitionOptions();
        sop1.setIniTrans(12);
        se1.setPartitionOptions(sop1);

        SubListPartitionElement se2 =
                new SubListPartitionElement("b", Collections.singletonList(new ConstExpression("'maxvalue'")));
        PartitionOptions sop2 = new PartitionOptions();
        sop2.setMaxTrans(13);
        se2.setPartitionOptions(sop2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));

        ListPartitionElement e2 = new ListPartitionElement("d", Collections.singletonList(new ConstExpression("3")));
        PartitionOptions options1 = new PartitionOptions();
        options1.setId(14);
        options1.setNoCompress(true);
        e2.setPartitionOptions(options1);

        ListPartitionElement e3 =
                new ListPartitionElement(null, Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions options3 = new PartitionOptions();
        options3.setId(15);
        options3.setTableSpace("tbs");
        e3.setPartitionOptions(options3);

        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "list");
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2, e3), subPartitionOption, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnPartition_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by column (a,(b,c), d)"));
        Partition actual = factory.generate();

        ColumnPartition expect = new ColumnPartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"),
                new ColumnReference(null, null, "c"),
                new ColumnReference(null, null, "d")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_autoPartition_succeed() {
        StatementFactory<Partition> factory = new OraclePartitionFactory(getPartitionContext(
                "partition by range(a,b) partition size 'auto' PARTITIONS AUTO"));
        Partition actual = factory.generate();

        RangePartition expect = new RangePartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b")), null, null, null, false);
        expect.setAuto(true);
        expect.setPartitionSize(new ConstExpression("'auto'"));
        Assert.assertEquals(expect, actual);
    }

    private Opt_partition_optionContext getPartitionContext(String part) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(part));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.opt_partition_option();
    }

}
