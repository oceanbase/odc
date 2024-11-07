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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLAlterTableActionFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_actionContext;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction.AlterColumnBehavior;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.common.mysql.LobStorageOption;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.KeyPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.createtable.SubHashPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;

/**
 * Test cases for {@link MySQLAlterTableActionFactory}
 * 
 * @author yh263208
 * @date 2023-06-15 15:15
 * @since ODC_release_4.2.0
 */
public class MySQLAlterTableActionFactoryTest {

    @Test
    public void generate_tableOptions_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("table_mode='aaa' USE_BLOOM_FILTER=true"));
        AlterTableAction actual = factory.generate();

        TableOptions tableOptions = new TableOptions();
        tableOptions.setTableMode("'aaa'");
        tableOptions.setUseBloomFilter(true);
        AlterTableAction expect = new AlterTableAction();
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_convertCharset_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("convert to character set utf8"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setCharset("utf8");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_convertCharsetCollation_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("convert to character set utf8 collate abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setCharset("utf8");
        expect.setCollation("abcd");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_setTableOptions_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("set table_mode='aaa' USE_BLOOM_FILTER=true"));
        AlterTableAction actual = factory.generate();

        TableOptions tableOptions = new TableOptions();
        tableOptions.setTableMode("'aaa'");
        tableOptions.setUseBloomFilter(true);
        AlterTableAction expect = new AlterTableAction();
        expect.setTableOptions(tableOptions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("add id varchar(64)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType type = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition d = new ColumnDefinition(new ColumnReference(null, null, "id"), type);
        expect.setAddColumns(Collections.singletonList(d));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addColumns_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("add (id varchar(64), id1 blob)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        GeneralDataType t2 = new GeneralDataType("blob", null);
        ColumnDefinition d2 = new ColumnDefinition(new ColumnReference(null, null, "id1"), t2);
        expect.setAddColumns(Arrays.asList(d1, d2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addColumnsWithLobStorage_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("add (id varchar(64), id1 blob) json(col) store as (chunk 'aas' chunk 123)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        GeneralDataType t2 = new GeneralDataType("blob", null);
        ColumnDefinition d2 = new ColumnDefinition(new ColumnReference(null, null, "id1"), t2);
        expect.setAddColumns(Arrays.asList(d1, d2));
        LobStorageOption storageOption = new LobStorageOption("col", Arrays.asList("'aas'", "123"));
        expect.setLobStorageOption(storageOption);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropColumnCascade_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop column id cascade"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropColumn(new ColumnReference(null, null, "id"), "cascade");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropColumnRestrict_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop column id Restrict"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropColumn(new ColumnReference(null, null, "id"), "Restrict");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("modify id varchar(64)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        expect.setModifyColumns(Collections.singletonList(d1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_changeColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("change a.b id varchar(64)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        expect.changeColumn(new ColumnReference(null, "a", "b"), d1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter column a.b set default 12"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        AlterColumnBehavior behavior = new AlterColumnBehavior();
        behavior.setDefaultValue(new ConstExpression("12"));
        expect.alterColumnBehavior(new ColumnReference(null, "a", "b"), behavior);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterColumn1_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter column a.b set default (12)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        AlterColumnBehavior behavior = new AlterColumnBehavior();
        behavior.setDefaultValue(new ConstExpression("12"));
        expect.alterColumnBehavior(new ColumnReference(null, "a", "b"), behavior);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterColumnDropDefault_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter column a.b drop default"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        AlterColumnBehavior behavior = new AlterColumnBehavior();
        expect.alterColumnBehavior(new ColumnReference(null, "a", "b"), behavior);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropTg_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop tablegroup"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropTableGroup(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rename_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("rename to b"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setRenameToTable(new RelationFactor("b"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renameSchema_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("rename to a.b"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        expect.setRenameToTable(factor);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addConstraint_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("add constraint abc primary key (a,b)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        SortColumn c1 = new SortColumn(new ColumnReference(null, null, "a"));
        SortColumn c2 = new SortColumn(new ColumnReference(null, null, "b"));
        OutOfLineConstraint constraint = new OutOfLineConstraint(null, Arrays.asList(c1, c2));
        constraint.setPrimaryKey(true);
        constraint.setConstraintName("abc");
        expect.setAddConstraint(constraint);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addIndex_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("add key abc (a,b)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        SortColumn c1 = new SortColumn(new ColumnReference(null, null, "a"));
        SortColumn c2 = new SortColumn(new ColumnReference(null, null, "b"));
        expect.setAddIndex(new OutOfLineIndex("abc", Arrays.asList(c1, c2)));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropIndex_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop key abc"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropIndexName("abc");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterIndex_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter index abc visible"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.alterIndexVisibility("abc", true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterIndexNoParallel_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter index abc noParallel"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.alterIndexNoParallel("abc");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterIndexparallel_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter index abc parallel=13"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.alterIndexParallel("abc", 13);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renameIndex_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("rename index a to b"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.renameIndex("a", "b");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop partition a,b,c"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropPartitionNames(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop subpartition a,b,c"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropSubPartitionNames(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_truncatePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("truncate partition a,b,c"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setTruncatePartitionNames(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_truncateSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("truncate subpartition a,b,c"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setTruncateSubPartitionNames(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addRangePartitionElts_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext(
                        "add partition (partition a.b values less than (-2, maxvalue) engine=InnoDB,"
                                + "partition d values less than (func(1,2)) id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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

        expect.setAddPartitionElements(Arrays.asList(e1, e3));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addListPartitionElts_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("add partition (partition a.b values in (default) engine=InnoDB,"
                        + "partition d values in (3) id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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

        expect.setAddPartitionElements(Arrays.asList(e1, e2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_reorganizePartitionIntoRange_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("REORGANIZE partition a,b into ("
                        + "partition a.b values less than (-2, maxvalue) engine=InnoDB,"
                        + "partition d values less than (func(1,2)) id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        expect.reorganizePartition(Arrays.asList("a", "b"), Arrays.asList(e1, e3));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_reorganizePartitionIntoList_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("REORGANIZE partition a,b into ("
                        + "partition a.b values in (default) engine=InnoDB,"
                        + "partition d values in (3) id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        expect.reorganizePartition(Arrays.asList("a", "b"), Arrays.asList(e1, e2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyHashPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("partition by hash(a)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setModifyPartition(new HashPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), null, null, null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyListPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("partition by list(a) partitions 145 ("
                        + "partition a.b values in (default) engine=InnoDB,"
                        + "partition d values in (3) id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        expect.setModifyPartition(new ListPartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), null, 145, false));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyKeyPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("partition by key (a,d) subpartition by key(c) subpartition template ("
                        + "subpartition a,"
                        + "subpartition b engine=InnoDB) partitions 144 ("
                        + "partition a.b,"
                        + "partition d id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        KeyPartition key = new KeyPartition(Arrays.asList(
                new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "d")),
                Arrays.asList(e1, e2), subPartitionOption, 144);
        expect.setModifyPartition(key);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyRangePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("partition by range(a) subpartition by range columns (c,b) ("
                        + "partition a.b values less than (-2, maxvalue) ("
                        + "subpartition a.b values less than (+3),"
                        + "subpartition b values less than maxvalue engine=InnoDB),"
                        + "partition d values less than (+3) id 14)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        expect.setModifyPartition(new RangePartition(Collections.singletonList(
                new ColumnReference(null, null, "a")), Arrays.asList(e1, e2), subPartitionOption, null, false));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropConstraint_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop constraint abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropConstraintNames(Collections.singletonList("abcd"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropConstraints_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop constraint (abcd,bbbb)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropConstraintNames(Arrays.asList("abcd", "bbbb"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropCheckConstraint_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop check abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropConstraintNames(Collections.singletonList("abcd"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropCheckConstraints_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop check (abcd,bbbb)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropConstraintNames(Arrays.asList("abcd", "bbbb"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropForeignConstraints_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop foreign key abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropForeignKeyName("abcd");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_exchangePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("exchange partition p1 with table tbl without validation"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setExchangePartition("p1", new RelationFactor("tbl"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropPrimaryKey_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("drop primary key"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterConstraintNotEnforced_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter constraint abcd not enforced"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        ConstraintState state = new ConstraintState();
        state.setEnforced(false);
        expect.modifyConstraint("abcd", state);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_alterCheckEnforced_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("alter Check abcd enforced"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        ConstraintState state = new ConstraintState();
        state.setEnforced(true);
        expect.modifyConstraint("abcd", state);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renameColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("rename column id to abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.renameColumn(new ColumnReference(null, null, "id"), "abcd");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_removePartitioning_succeed() {
        StatementFactory<AlterTableAction> factory = new MySQLAlterTableActionFactory(
                getActionContext("remove partitioning"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setRemovePartitioning(true);
        Assert.assertEquals(expect, actual);
    }

    private Alter_table_actionContext getActionContext(String action) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(action));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.alter_table_action();
    }
}
