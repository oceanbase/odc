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

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLPartitionFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Opt_partition_optionContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.KeyPartition;
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
 * Test cases for {@link MySQLPartitionFactory}
 *
 * @author yh263208
 * @date 2023-05-31 21:35
 * @since ODC_release_4.2.0
 */
public class MySQLPartitionFactoryTest {

    @Test
    public void generate_hashPartition_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(
                getPartitionContext("partition by hash(a)"));
        Partition actual = factory.generate();
        HashPartition expect = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), null, null, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithPartitionElts_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by hash(a) partitions 12 ("
                        + "partition a.b,"
                        + "partition d id 14)"));
        Partition actual = factory.generate();

        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        HashPartition expect = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), null, 12);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithSubPartitions_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by hash(a) partitions 12 ("
                        + "partition a.b ("
                        + "subpartition a.b,"
                        + "subpartition b engine=InnoDB),"
                        + "partition d id 14)"));
        Partition actual = factory.generate();

        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        SubHashPartitionElement se1 = new SubHashPartitionElement("b");
        se1.setSchema("a");
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setEngine("InnoDB");
        se2.setPartitionOptions(o2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        HashPartition expect = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), null, 12);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithSubPartitionOptions_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by hash(a) subpartition by hash(c) subpartition template("
                        + "subpartition a,"
                        + "subpartition b engine=InnoDB) ("
                        + "partition a.b engine=InnoDB ("
                        + "subpartition a,"
                        + "subpartition b engine=InnoDB),"
                        + "partition d id 14)"));
        Partition actual = factory.generate();

        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        PartitionOptions o1 = new PartitionOptions();
        o1.setEngine("InnoDB");
        e1.setPartitionOptions(o1);
        SubHashPartitionElement se1 = new SubHashPartitionElement("a");
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setEngine("InnoDB");
        se2.setPartitionOptions(o2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o3 = new PartitionOptions();
        o3.setId(14);
        e2.setPartitionOptions(o3);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "hash");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        HashPartition expect = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), subPartitionOption, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_hashPartitionWithSubPartitionOptionsQuantity_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by hash(a) subpartition by hash(c) subpartitions 165 partitions 12 ("
                        + "partition a.b ("
                        + "subpartition a,"
                        + "subpartition b engine=InnoDB),"
                        + "partition d id 14)"));
        Partition actual = factory.generate();

        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        SubHashPartitionElement se1 = new SubHashPartitionElement("a");
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setEngine("InnoDB");
        se2.setPartitionOptions(o2);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "hash");
        subPartitionOption.setSubPartitionNum(165);
        HashPartition expect = new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), subPartitionOption, 12);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_keyPartition_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by key (a,d) subpartition by key(c) subpartition template ("
                        + "subpartition a,"
                        + "subpartition b engine=InnoDB) partitions 144 ("
                        + "partition a.b,"
                        + "partition d id 14)"));
        Partition actual = factory.generate();

        SubHashPartitionElement se1 = new SubHashPartitionElement("a");
        SubHashPartitionElement se2 = new SubHashPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setEngine("InnoDB");
        se2.setPartitionOptions(o2);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "key");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        HashPartitionElement e1 = new HashPartitionElement("b");
        e1.setSchema("a");
        HashPartitionElement e2 = new HashPartitionElement("d");
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        KeyPartition expect = new KeyPartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "d")),
                Arrays.asList(e1, e2), subPartitionOption, 144);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_keyPartitionWithSubPartNum_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by key () subpartition by key(c) subpartitions 123"));
        Partition actual = factory.generate();

        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "key");
        subPartitionOption.setSubPartitionNum(123);
        KeyPartition expect = new KeyPartition(null, null, subPartitionOption, null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithPartitionElts_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range columns (a,b) partitions 123("
                        + "partition a.b values less than (-2, maxvalue) engine=InnoDB,"
                        + "partition d values less than (func(1,2)) id 14)"));
        Partition actual = factory.generate();

        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        PartitionOptions o = new PartitionOptions();
        o.setEngine("InnoDB");
        e1.setPartitionOptions(o);
        e1.setSchema("a");
        FunctionParam p1 = new ExpressionParam(new ConstExpression("1"));
        FunctionParam p2 = new ExpressionParam(new ConstExpression("2"));
        RangePartitionElement e3 = new RangePartitionElement("d",
                Collections.singletonList(new FunctionCall("func", Arrays.asList(p1, p2))));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e3.setPartitionOptions(o1);
        List<Expression> cols = Arrays.asList(new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"));
        RangePartition expect = new RangePartition(cols, Arrays.asList(e1, e3), null, 123, true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitions_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range(a) ("
                        + "partition a.b values less than (-2, maxvalue) ("
                        + "subpartition a.b values less than (+3),"
                        + "subpartition b values less than (maxvalue)),"
                        + "partition d values less than (+3) id 14)"));
        Partition actual = factory.generate();

        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        RangePartition expect = new RangePartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), null, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitionOptions_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range(a) subpartition by range (c) subpartition template ("
                        + "subpartition a.b values less than (+3),"
                        + "subpartition b values less than maxvalue)("
                        + "partition a.b values less than (-2, maxvalue) ("
                        + "subpartition a.b values less than (+3),"
                        + "subpartition b values less than maxvalue),"
                        + "partition d values less than (+3) id 14)"));
        Partition actual = factory.generate();

        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "range");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        RangePartition expect = new RangePartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), subPartitionOption, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitionOptionsIndividual_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range(a) subpartition by range (c) ("
                        + "partition a.b values less than (-2, maxvalue) ("
                        + "subpartition a.b values less than (+3),"
                        + "subpartition b values less than maxvalue),"
                        + "partition d values less than (+3) id 14)"));
        Partition actual = factory.generate();

        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "range");
        RangePartition expect = new RangePartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), subPartitionOption, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rangePartitionWithSubPartitionOptionsQuantity_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range(a) subpartition by range columns (c,b) ("
                        + "partition a.b values less than (-2, maxvalue) ("
                        + "subpartition a.b values less than (+3),"
                        + "subpartition b values less than maxvalue engine=InnoDB),"
                        + "partition d values less than (+3) id 14)"));
        Partition actual = factory.generate();

        RangePartitionElement e1 = new RangePartitionElement("b", Arrays.asList(
                new CompoundExpression(new ConstExpression("2"), null, Operator.SUB), new ConstExpression("maxvalue")));
        e1.setSchema("a");
        SubRangePartitionElement se1 = new SubRangePartitionElement("b",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        se1.setSchema("a");
        SubRangePartitionElement se2 =
                new SubRangePartitionElement("b", Collections.singletonList(new ConstExpression("maxvalue")));
        PartitionOptions o = new PartitionOptions();
        o.setEngine("InnoDB");
        se2.setPartitionOptions(o);
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        RangePartitionElement e2 = new RangePartitionElement("d",
                Collections.singletonList(new CompoundExpression(new ConstExpression("3"), null, Operator.ADD)));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Arrays.asList(new ColumnReference(null, null, "c"), new ColumnReference(null, null, "b")),
                "range columns");
        RangePartition expect = new RangePartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), subPartitionOption, null, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithPartitionElts_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by list(a) partitions 145 ("
                        + "partition a.b values in (default) engine=InnoDB,"
                        + "partition d values in (3) id 14)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 = new ListPartitionElement("b",
                Collections.singletonList(new ConstExpression("default")));
        PartitionOptions o = new PartitionOptions();
        o.setEngine("InnoDB");
        e1.setPartitionOptions(o);
        e1.setSchema("a");
        ListPartitionElement e2 = new ListPartitionElement("d", Collections.singletonList(new ConstExpression("3")));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        ListPartition expect = new ListPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), null, 145, false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithSubPartitions_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by list columns (a,b) ("
                        + "partition a.b values in (default) ("
                        + "subpartition a.b values in (2) Engine=InnoDB,"
                        + "subpartition b values in ('maxvalue')),"
                        + "partition d values in (3) id 14)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 = new ListPartitionElement("b",
                Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        SubListPartitionElement se1 = new SubListPartitionElement("b",
                Collections.singletonList(new ConstExpression("2")));
        se1.setSchema("a");
        PartitionOptions o = new PartitionOptions();
        o.setEngine("InnoDB");
        se1.setPartitionOptions(o);
        SubListPartitionElement se2 = new SubListPartitionElement("b",
                Collections.singletonList(new ConstExpression("'maxvalue'")));
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        ListPartitionElement e2 = new ListPartitionElement("d", Collections.singletonList(new ConstExpression("3")));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        List<Expression> cols = Arrays.asList(new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"));
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2), null, null, true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithSubPartitionOptions_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by list columns (a,b) subpartition by list(c) subpartition template("
                        + "subpartition a.b values in (2),"
                        + "subpartition b values in ('maxvalue')) ("
                        + "partition a.b values in (default) ("
                        + "subpartition a.b values in (2),"
                        + "subpartition b values in ('maxvalue')),"
                        + "partition d values in (3) id 14,"
                        + "partition f values in ('aaaddd') id 15)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 = new ListPartitionElement("b",
                Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        SubListPartitionElement se1 = new SubListPartitionElement("b",
                Collections.singletonList(new ConstExpression("2")));
        se1.setSchema("a");
        SubListPartitionElement se2 = new SubListPartitionElement("b",
                Collections.singletonList(new ConstExpression("'maxvalue'")));
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        ListPartitionElement e2 = new ListPartitionElement("d",
                Collections.singletonList(new ConstExpression("3")));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        ListPartitionElement e3 = new ListPartitionElement("f",
                Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions o2 = new PartitionOptions();
        o2.setId(15);
        e3.setPartitionOptions(o2);
        List<Expression> cols = Arrays.asList(new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"));
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Collections.singletonList(new ColumnReference(null, null, "c")), "list");
        subPartitionOption.setTemplates(Arrays.asList(se1, se2));
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2, e3), subPartitionOption, null, true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_listPartitionWithSubPartitionOptionsQuantity_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by list columns (a,b) subpartition by list columns(c,f) ("
                        + "partition a.b values in (default) ("
                        + "subpartition a.b values in (2),"
                        + "subpartition b values in ('maxvalue')),"
                        + "partition d values in (3) id 14,"
                        + "partition f values in ('aaaddd') id 15)"));
        Partition actual = factory.generate();

        ListPartitionElement e1 = new ListPartitionElement("b",
                Collections.singletonList(new ConstExpression("default")));
        e1.setSchema("a");
        SubListPartitionElement se1 = new SubListPartitionElement("b",
                Collections.singletonList(new ConstExpression("2")));
        se1.setSchema("a");
        SubListPartitionElement se2 = new SubListPartitionElement("b",
                Collections.singletonList(new ConstExpression("'maxvalue'")));
        e1.setSubPartitionElements(Arrays.asList(se1, se2));
        ListPartitionElement e2 = new ListPartitionElement("d",
                Collections.singletonList(new ConstExpression("3")));
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(14);
        e2.setPartitionOptions(o1);
        ListPartitionElement e3 = new ListPartitionElement("f",
                Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions o2 = new PartitionOptions();
        o2.setId(15);
        e3.setPartitionOptions(o2);
        List<Expression> cols = Arrays.asList(new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"));
        SubPartitionOption subPartitionOption = new SubPartitionOption(
                Arrays.asList(new ColumnReference(null, null, "c"), new ColumnReference(null, null, "f")),
                "list columns");
        ListPartition expect = new ListPartition(cols, Arrays.asList(e1, e2, e3), subPartitionOption, null, true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_columnPartition_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
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
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range columns(a,b) partition size 'auto' PARTITIONS AUTO"));
        Partition actual = factory.generate();

        RangePartition expect = new RangePartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b")), null, null, null, true);
        expect.setAuto(true);
        expect.setPartitionSize(new ConstExpression("'auto'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_autoPartition1_succeed() {
        StatementFactory<Partition> factory = new MySQLPartitionFactory(getPartitionContext(
                "partition by range(b) partition size 'auto' PARTITIONS AUTO"));
        Partition actual = factory.generate();

        RangePartition expect = new RangePartition(Collections.singletonList(
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
