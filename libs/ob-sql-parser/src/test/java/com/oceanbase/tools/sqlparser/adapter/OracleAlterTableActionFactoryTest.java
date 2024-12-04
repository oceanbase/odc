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
import java.util.List;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleAlterTableActionFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_table_actionContext;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.alter.table.PartitionSplitActions;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.GeneralDataType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.createtable.SpecialPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;

/**
 * Test cases for {@link OracleAlterTableActionFactory}
 * 
 * @author yh263208
 * @date 2023-06-14 20:18
 * @since ODC-release-4.2.0
 */
public class OracleAlterTableActionFactoryTest {

    @Test
    public void generate_tableOptions_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
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
    public void generate_exchangePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("exchange partition p1 with table tbl including indexes without validation"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setExchangePartition("p1", new RelationFactor("tbl"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_setInterval_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("set interval('abc')"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setInterval(new ConstExpression("'abc'"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_setIntervalNon_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("set interval()"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_enableAllTriggers_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("enable all triggers"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setEnableAllTriggers(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_disableAllTriggers_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("disable all triggers"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setEnableAllTriggers(false);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_setTableOptions_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
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
    public void generate_moveNoCompress_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("move nocompress"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setMoveNoCompress(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_moveCompress_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("move compress basic"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setMoveCompress("basic");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("add id varchar2(64)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType type = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition d = new ColumnDefinition(new ColumnReference(null, null, "id"), type);
        expect.setAddColumns(Collections.singletonList(d));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addColumns_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("add (id varchar2(64), id1 blob)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        GeneralDataType t2 = new GeneralDataType("blob", null);
        ColumnDefinition d2 = new ColumnDefinition(new ColumnReference(null, null, "id1"), t2);
        expect.setAddColumns(Arrays.asList(d1, d2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropColumnCascade_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop column id cascade"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropColumn(new ColumnReference(null, null, "id"), "cascade");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropColumnRestrict_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop column id Restrict"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropColumn(new ColumnReference(null, null, "id"), "Restrict");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropColumns_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop (id, id1)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropColumns(
                Arrays.asList(new ColumnReference(null, null, "id"), new ColumnReference(null, null, "id1")));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renameColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("rename column id to abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.renameColumn(new ColumnReference(null, null, "id"), "abcd");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyColumn_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify id varchar2(64)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        expect.setModifyColumns(Collections.singletonList(d1));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyColumns_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify (id varchar2(64), id1 blob)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        CharacterType t1 = new CharacterType("varchar2", new BigDecimal("64"));
        ColumnDefinition d1 = new ColumnDefinition(new ColumnReference(null, null, "id"), t1);
        GeneralDataType t2 = new GeneralDataType("blob", null);
        ColumnDefinition d2 = new ColumnDefinition(new ColumnReference(null, null, "id1"), t2);
        expect.setModifyColumns(Arrays.asList(d1, d2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropTg_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop tablegroup"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropTableGroup(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_rename_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("rename to b"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setRenameToTable(new RelationFactor("b"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renameSchema_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
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
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
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
    public void generate_alterIndex_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("alter index abc visible"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.alterIndexVisibility("abc", true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyConstraint_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify constraint abc rely enable validate"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        ConstraintState constraintState = new ConstraintState();
        constraintState.setValidate(true);
        constraintState.setEnable(true);
        constraintState.setRely(true);
        expect.modifyConstraint("abc", constraintState);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyConstraint1_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("enable constraint abc"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        ConstraintState constraintState = new ConstraintState();
        constraintState.setEnable(true);
        expect.modifyConstraint("abc", constraintState);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop partition a,b,c"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropPartitionNames(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop subpartition a,b,c update global indexes"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropSubPartitionNames(Arrays.asList("a", "b", "c"));
        expect.setUpdateGlobalIndexes(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_truncatePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("truncate partition a,b,c update global indexes"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setTruncatePartitionNames(Arrays.asList("a", "b", "c"));
        expect.setUpdateGlobalIndexes(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_truncateSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("truncate subpartition a,b,c"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setTruncateSubPartitionNames(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addRangePartitionElts_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext(
                        "add partition a.b@c values less than (-2, maxvalue) tablespace tbs1 compress for oltp,"
                                + "partition d values less than (+3) id 14 nocompress"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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

        expect.setAddPartitionElements(Arrays.asList(e1, e2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_addListPartitionElts_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("add partition a.b@c values (default) tablespace tbs1 compress for oltp,"
                        + "partition d values (3) id 14 nocompress"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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

        expect.setAddPartitionElements(Arrays.asList(e1, e2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyPartitionAddRangeSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify partition a.b add " +
                        "subpartition a.b values less than (+3) storage(next 12 initial 15 minextents 16 maxextents 17),"
                        + "subpartition b values less than (maxvalue) tablespace tbs"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        expect.addSubpartitionElements(factor, Arrays.asList(se1, se2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyPartitionAddListSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify partition a.b add "
                        + "subpartition a.b values (2) INITRANS 12,"
                        + "subpartition b values ('maxvalue') MAXTRANS 13"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        RelationFactor factor = new RelationFactor("b");
        factor.setSchema("a");
        expect.addSubpartitionElements(factor, Arrays.asList(se1, se2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_splitPartitionList_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("split partition b values(a,b)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        PartitionSplitActions actions = new PartitionSplitActions();
        actions.setListExprs(Arrays.asList(new RelationReference("a", null), new RelationReference("b", null)));
        expect.splitPartition(new RelationFactor("b"), actions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_splitPartitionRange_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("split partition b at(1,2) into (partition,partition id 12,partition a.b id 13)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        PartitionSplitActions actions = new PartitionSplitActions();
        actions.setRangeExprs(Arrays.asList(new ConstExpression("1"), new ConstExpression("2")));
        SpecialPartitionElement e1 = new SpecialPartitionElement(null);
        SpecialPartitionElement e2 = new SpecialPartitionElement(null);
        PartitionOptions o1 = new PartitionOptions();
        o1.setId(12);
        e2.setPartitionOptions(o1);
        SpecialPartitionElement e3 = new SpecialPartitionElement("b");
        PartitionOptions o2 = new PartitionOptions();
        o2.setId(13);
        e3.setPartitionOptions(o2);
        e3.setSchema("a");
        actions.setIntos(Arrays.asList(e1, e2, e3));
        expect.splitPartition(new RelationFactor("b"), actions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_splitRangePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("split partition b into ("
                        + "partition a.b@c values less than (-2, maxvalue) tablespace tbs1 compress for oltp,"
                        + "partition d values less than (+3) id 14 nocompress, partition id 12)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        PartitionSplitActions actions = new PartitionSplitActions();
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
        SpecialPartitionElement e3 = new SpecialPartitionElement(null);
        PartitionOptions o2 = new PartitionOptions();
        o2.setId(12);
        e3.setPartitionOptions(o2);
        actions.setIntos(Arrays.asList(e1, e2, e3));
        expect.splitPartition(new RelationFactor("b"), actions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_splitRangePartition1_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("split partition b into ("
                        + "partition a.b@c values less than (-2, maxvalue) tablespace tbs1 compress for oltp,"
                        + "partition d values less than (+3) id 14 nocompress)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        PartitionSplitActions actions = new PartitionSplitActions();
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
        actions.setIntos(Arrays.asList(e1, e2));
        expect.splitPartition(new RelationFactor("b"), actions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_splitListPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("split partition b into ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp,"
                        + "partition d values (3) id 14 nocompress)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        PartitionSplitActions actions = new PartitionSplitActions();
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
        actions.setIntos(Arrays.asList(e1, e2));
        expect.splitPartition(new RelationFactor("b"), actions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_splitListPartition1_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("split partition b into ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp,"
                        + "partition d values (3) id 14 nocompress, partition id 12)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        PartitionSplitActions actions = new PartitionSplitActions();
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
        options1.setNoCompress(true);
        options1.setId(14);
        e2.setPartitionOptions(options1);
        SpecialPartitionElement e3 = new SpecialPartitionElement(null);
        PartitionOptions o2 = new PartitionOptions();
        o2.setId(12);
        e3.setPartitionOptions(o2);
        actions.setIntos(Arrays.asList(e1, e2, e3));
        expect.splitPartition(new RelationFactor("b"), actions);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyHashPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify partition by hash(a,b)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        List<Expression> cols = Arrays.asList(new ColumnReference(null, null, "a"),
                new ColumnReference(null, null, "b"));
        expect.setModifyPartition(new HashPartition(cols, null, null, null));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyListPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify partition by list(a,b) ("
                        + "partition a.b@c values (default) tablespace tbs1 compress for oltp,"
                        + "partition d values (3) id 14 nocompress,"
                        + "partition values ('aaaddd') id 15 tablespace tbs)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        options1.setNoCompress(true);
        options1.setId(14);
        e2.setPartitionOptions(options1);
        ListPartitionElement e3 =
                new ListPartitionElement(null, Collections.singletonList(new ConstExpression("'aaaddd'")));
        PartitionOptions options = new PartitionOptions();
        options.setId(15);
        options.setTableSpace("tbs");
        e3.setPartitionOptions(options);
        List<Expression> cols =
                Arrays.asList(new ColumnReference(null, null, "a"), new ColumnReference(null, null, "b"));
        expect.setModifyPartition(new ListPartition(cols, Arrays.asList(e1, e2, e3), null, null, false));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyRangePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify partition by range(a,b) subpartition by range (c) ("
                        + "partition a.b@c values less than (2, maxvalue) tablespace tbs1 compress for oltp ("
                        + "subpartition a.b values less than (+3) storage(next 12 initial 15 minextents 16 maxextents 17),"
                        + "subpartition b values less than (maxvalue) tablespace tbs),"
                        + "partition d values less than (+3) id 14 nocompress)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
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
        expect.setModifyPartition(new RangePartition(cols, Arrays.asList(e1, e2), subPartitionOption, null, false));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropConstraint_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop constraint abcd"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropConstraintNames(Collections.singletonList("abcd"));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_dropPrimaryKey_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("drop primary key"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.setDropPrimaryKey(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyPK_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify primary key(id)"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        List<SortColumn> columns = Collections.singletonList(new SortColumn(new ColumnReference(null, null, "id")));
        OutOfLineConstraint pk = new OutOfLineConstraint(null, columns);
        pk.setPrimaryKey(true);
        expect.setModifyPrimaryKey(pk);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_modifyPKWithOptions_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify primary key(id) global"));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        List<SortColumn> columns = Collections.singletonList(new SortColumn(new ColumnReference(null, null, "id")));
        ConstraintState state = new ConstraintState();
        IndexOptions options = new IndexOptions();
        options.setGlobal(true);
        state.setIndexOptions(options);
        OutOfLineConstraint pk = new OutOfLineConstraint(state, columns);
        pk.setPrimaryKey(true);
        expect.setModifyPrimaryKey(pk);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renamePartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("rename partition \"aaa\" to \"bbb\""));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.renamePartition("\"aaa\"", "\"bbb\"");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void generate_renameSubPartition_succeed() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("rename subpartition \"aaa\" to \"bbb\""));
        AlterTableAction actual = factory.generate();

        AlterTableAction expect = new AlterTableAction();
        expect.renameSubPartition("\"aaa\"", "\"bbb\"");
        Assert.assertEquals(expect, actual);
    }

    @Test(expected = ParseCancellationException.class)
    public void generate_modifyPKWithUsingIndex_wrong() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify primary key(id) using index global"));
        factory.generate();
    }

    @Test(expected = ParseCancellationException.class)
    public void generate_modifyPKWithUsingIndex1_wrong() {
        StatementFactory<AlterTableAction> factory = new OracleAlterTableActionFactory(
                getActionContext("modify primary key(id) using index"));
        factory.generate();
    }

    private Alter_table_actionContext getActionContext(String action) {
        OBLexer lexer = new OBLexer(CharStreams.fromString(action));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.alter_table_action();
    }

}
