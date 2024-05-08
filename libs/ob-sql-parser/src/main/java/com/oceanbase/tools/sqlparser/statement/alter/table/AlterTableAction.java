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
package com.oceanbase.tools.sqlparser.statement.alter.table;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link AlterTableAction}
 *
 * @author yh263208
 * @date 2023-06-12 17:41
 * @since ODC_release_4.2.0
 * @see BaseStatement
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AlterTableAction extends BaseStatement {

    private TableOptions tableOptions;
    private Boolean moveNoCompress;
    private Boolean removePartitioning;
    private String moveCompress;
    private List<ColumnDefinition> addColumns;
    private List<ColumnReference> dropColumns;
    /**
     * candidates:
     *
     * <pre>
     *     CASCADE
     *     RESTRICT
     * </pre>
     */
    @Setter(AccessLevel.NONE)
    private String dropSingleColumnOption;
    private List<ColumnDefinition> modifyColumns;
    @Setter(AccessLevel.NONE)
    private ColumnReference renameFromColumn;
    @Setter(AccessLevel.NONE)
    private String renameToColumnName;
    private Boolean dropTableGroup;
    private RelationFactor renameToTable;
    private OutOfLineConstraint addConstraint;
    @Setter(AccessLevel.NONE)
    private String alterIndexName;
    @Setter(AccessLevel.NONE)
    private Boolean alterIndexVisible;
    @Setter(AccessLevel.NONE)
    private Integer alterIndexParallel;
    @Setter(AccessLevel.NONE)
    private Boolean alterIndexNoParallel;
    private List<String> dropConstraintNames;
    @Setter(AccessLevel.NONE)
    private String modifyConstraintName;
    @Setter(AccessLevel.NONE)
    private ConstraintState modifyConstraintState;
    private Boolean updateGlobalIndexes;
    private List<String> truncatePartitionNames;
    private List<String> truncateSubPartitionNames;
    private List<String> dropPartitionNames;
    private List<String> dropSubPartitionNames;
    private List<PartitionElement> addPartitionElements;
    @Setter(AccessLevel.NONE)
    private RelationFactor addSubPartitionElementTo;
    @Setter(AccessLevel.NONE)
    private List<SubPartitionElement> addSubPartitionElements;
    private Partition modifyPartition;
    @Setter(AccessLevel.NONE)
    private RelationFactor splitPartition;
    @Setter(AccessLevel.NONE)
    private PartitionSplitActions splitActions;
    private Expression interval;
    private Boolean enableAllTriggers;

    @Setter(AccessLevel.NONE)
    private ColumnReference alterColumn;
    @Setter(AccessLevel.NONE)
    private AlterColumnBehavior alterColumnBehavior;
    @Setter(AccessLevel.NONE)
    private ColumnReference changeColumn;
    @Setter(AccessLevel.NONE)
    private ColumnDefinition changeColumnDefinition;
    private OutOfLineIndex addIndex;
    private String dropIndexName;
    private String dropForeignKeyName;
    @Setter(AccessLevel.NONE)
    private String renameFromIndexName;
    @Setter(AccessLevel.NONE)
    private String renameToIndexName;
    @Setter(AccessLevel.NONE)
    private List<String> reorganizePartitionNames;
    @Setter(AccessLevel.NONE)
    private List<PartitionElement> reorganizePartitionIntos;
    private String charset;
    private String collation;

    private Boolean dropPrimaryKey;
    private OutOfLineConstraint modifyPrimaryKey;
    private boolean refresh;
    @Setter(AccessLevel.NONE)
    private String renameFromPartitionName;
    @Setter(AccessLevel.NONE)
    private String renameToPartitionName;
    @Setter(AccessLevel.NONE)
    private String renameFromSubPartitionName;
    @Setter(AccessLevel.NONE)
    private String renameToSubPartitionName;
    private List<ColumnGroupElement> addColumnGroupElements;
    private List<ColumnGroupElement> dropColumnGroupElements;

    public AlterTableAction(@NonNull ParserRuleContext context) {
        super(context);
    }

    public void setDropColumn(@NonNull ColumnReference dropColumn,
            String dropColumnOption) {
        this.dropColumns = Collections.singletonList(dropColumn);
        this.dropSingleColumnOption = dropColumnOption;
    }

    public void renameColumn(@NonNull ColumnReference from, @NonNull String to) {
        this.renameFromColumn = from;
        this.renameToColumnName = to;
    }

    public void renamePartition(@NonNull String from, @NonNull String to) {
        this.renameFromPartitionName = from;
        this.renameToPartitionName = to;
    }

    public void renameSubPartition(@NonNull String from, @NonNull String to) {
        this.renameFromSubPartitionName = from;
        this.renameToSubPartitionName = to;
    }

    public void renameIndex(@NonNull String from, @NonNull String to) {
        this.renameFromIndexName = from;
        this.renameToIndexName = to;
    }

    public void alterIndexVisibility(@NonNull String indexName, boolean visibility) {
        this.alterIndexName = indexName;
        this.alterIndexVisible = visibility;
    }

    public void alterIndexNoParallel(@NonNull String indexName) {
        this.alterIndexName = indexName;
        this.alterIndexNoParallel = true;
    }

    public void alterIndexParallel(@NonNull String indexName, int parallel) {
        this.alterIndexName = indexName;
        this.alterIndexParallel = parallel;
    }

    public void modifyConstraint(@NonNull String constraintName,
            @NonNull ConstraintState state) {
        this.modifyConstraintName = constraintName;
        this.modifyConstraintState = state;
    }

    public void addSubpartitionElements(@NonNull RelationFactor target,
            @NonNull List<SubPartitionElement> subPartitionElements) {
        this.addSubPartitionElementTo = target;
        this.addSubPartitionElements = subPartitionElements;
    }

    public void splitPartition(@NonNull RelationFactor target,
            @NonNull PartitionSplitActions splitActions) {
        this.splitPartition = target;
        this.splitActions = splitActions;
    }

    public void alterColumnBehavior(@NonNull ColumnReference column,
            @NonNull AlterColumnBehavior behavior) {
        this.alterColumn = column;
        this.alterColumnBehavior = behavior;
    }

    public void changeColumn(@NonNull ColumnReference column, @NonNull ColumnDefinition definition) {
        this.changeColumn = column;
        this.changeColumnDefinition = definition;
    }

    public void reorganizePartition(@NonNull List<String> names,
            @NonNull List<PartitionElement> partitionElements) {
        this.reorganizePartitionNames = names;
        this.reorganizePartitionIntos = partitionElements;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.tableOptions != null) {
            builder.append(" SET ").append(this.tableOptions);
        }
        if (Boolean.TRUE.equals(this.moveNoCompress)) {
            builder.append(" MOVE NOCOMPRESS");
        }
        if (this.moveCompress != null) {
            builder.append(" MOVE COMPRESS ").append(this.moveCompress);
        }
        if (CollectionUtils.isNotEmpty(this.addColumns)) {
            if (this.addColumns.size() == 1) {
                builder.append(" ADD ").append(this.addColumns.get(0));
            } else {
                builder.append(" ADD(")
                        .append(this.addColumns.stream()
                                .map(ColumnDefinition::toString)
                                .collect(Collectors.joining(",")))
                        .append(")");
            }
        }
        if (CollectionUtils.isNotEmpty(this.dropColumns)) {
            if (this.dropColumns.size() == 1) {
                builder.append(" DROP COLUMN ").append(this.dropColumns.get(0));
                if (this.dropSingleColumnOption != null) {
                    builder.append(" ").append(this.dropSingleColumnOption);
                }
            } else {
                builder.append(" DROP(")
                        .append(this.dropColumns.stream()
                                .map(ColumnReference::toString)
                                .collect(Collectors.joining(",")))
                        .append(")");
            }
        }
        if (this.renameFromColumn != null && this.renameToColumnName != null) {
            builder.append(" RENAME COLUMN ").append(this.renameFromColumn)
                    .append(" TO ").append(this.renameToColumnName);
        }
        if (CollectionUtils.isNotEmpty(this.modifyColumns)) {
            if (this.modifyColumns.size() == 1) {
                builder.append(" MODIFY ").append(this.modifyColumns.get(0));
            } else {
                builder.append(" MODIFY(")
                        .append(this.modifyColumns.stream()
                                .map(ColumnDefinition::toString)
                                .collect(Collectors.joining(",")))
                        .append(")");
            }
        }
        if (Boolean.TRUE.equals(this.dropTableGroup)) {
            builder.append(" DROP TABLEGROUP");
        }
        if (this.renameToTable != null) {
            builder.append(" RENAME TO ").append(this.renameToTable);
        }
        if (this.addConstraint != null) {
            builder.append(" ADD ").append(this.addConstraint);
        }
        if (this.alterIndexName != null && this.alterIndexVisible != null) {
            builder.append(" ALTER INDEX ").append(this.alterIndexName).append(" ")
                    .append(this.alterIndexVisible ? "VISIBLE" : "INVISIBLE");
        }
        if (this.alterIndexName != null && Boolean.TRUE.equals(this.alterIndexNoParallel)) {
            builder.append(" ALTER INDEX ").append(this.alterIndexName).append(" NOPARALLEL");
        }
        if (this.alterIndexName != null && this.alterIndexParallel != null) {
            builder.append(" ALTER INDEX ").append(this.alterIndexName)
                    .append(" PARALLEL=").append(this.alterIndexParallel);
        }
        if (this.modifyConstraintName != null && this.modifyConstraintState != null) {
            builder.append(" MODIFY CONSTRAINT ")
                    .append(this.modifyConstraintName).append(" ")
                    .append(this.modifyConstraintState);
        }
        if (CollectionUtils.isNotEmpty(this.dropPartitionNames)) {
            builder.append(" DROP PARTITION ").append(String.join(",", this.dropPartitionNames));
            appendUpdateGlobalIndexes(builder);
        }
        if (CollectionUtils.isNotEmpty(this.dropSubPartitionNames)) {
            builder.append(" DROP SUBPARTITION ").append(String.join(",", this.dropSubPartitionNames));
            appendUpdateGlobalIndexes(builder);
        }
        if (CollectionUtils.isNotEmpty(this.truncatePartitionNames)) {
            builder.append(" TRUNCATE PARTITION ").append(String.join(",", this.truncatePartitionNames));
            appendUpdateGlobalIndexes(builder);
        }
        if (CollectionUtils.isNotEmpty(this.truncateSubPartitionNames)) {
            builder.append(" TRUNCATE SUBPARTITION ").append(String.join(",", this.truncateSubPartitionNames));
            appendUpdateGlobalIndexes(builder);
        }
        if (CollectionUtils.isNotEmpty(this.addPartitionElements)) {
            builder.append(" ADD ").append(this.addPartitionElements.stream()
                    .map(Object::toString).collect(Collectors.joining(",")));
        }
        if (this.addSubPartitionElementTo != null
                && CollectionUtils.isNotEmpty(this.addSubPartitionElements)) {
            builder.append(" MODIFY PARTITION ")
                    .append(this.addSubPartitionElementTo)
                    .append(" ADD ")
                    .append(this.addSubPartitionElements.stream()
                            .map(Object::toString).collect(Collectors.joining(",")));
        }
        if (this.splitActions != null && this.splitPartition != null) {
            builder.append(" SPLIT PARTITION ").append(this.splitPartition)
                    .append(" ").append(this.splitActions);
        }
        if (this.modifyPartition != null) {
            builder.append(" MODIFY ").append(this.modifyPartition);
        }
        if (CollectionUtils.isNotEmpty(this.dropConstraintNames)) {
            if (this.dropConstraintNames.size() == 1) {
                builder.append(" DROP CONSTRAINT ").append(this.dropConstraintNames.get(0));
            } else {
                builder.append(" DROP CONSTRAINT(").append(String.join(",", this.dropConstraintNames)).append(")");
            }
        }
        if (this.renameFromPartitionName != null && this.renameToPartitionName != null) {
            builder.append(" RENAME PARTITION ")
                    .append(this.renameFromPartitionName).append(" TO ")
                    .append(this.renameToPartitionName);
        }
        if (this.renameFromSubPartitionName != null && this.renameToSubPartitionName != null) {
            builder.append(" RENAME SUBPARTITION ")
                    .append(this.renameFromSubPartitionName).append(" TO ")
                    .append(this.renameToSubPartitionName);
        }
        if (this.alterColumn != null && this.alterColumnBehavior != null) {
            builder.append(" ALTER ").append(this.alterColumn).append(" ").append(this.alterColumnBehavior);
        }
        if (this.changeColumnDefinition != null && this.changeColumn != null) {
            builder.append(" CHANGE ").append(this.changeColumn).append(" ").append(this.changeColumnDefinition);
        }
        if (this.addIndex != null) {
            builder.append(" ADD ").append(this.addIndex);
        }
        if (this.dropIndexName != null) {
            builder.append(" DROP INDEX ").append(this.dropIndexName);
        }
        if (this.renameFromIndexName != null && this.renameToIndexName != null) {
            builder.append(" RENAME INDEX ").append(this.renameFromIndexName)
                    .append(" TO ").append(this.renameToIndexName);
        }
        if (this.reorganizePartitionIntos != null && this.reorganizePartitionNames != null) {
            builder.append(" REORGANIZE PARTITION ")
                    .append(String.join(",", this.reorganizePartitionNames))
                    .append(" INTO (").append(this.reorganizePartitionIntos.stream()
                            .map(Object::toString).collect(Collectors.joining(",")))
                    .append(")");
        }
        if (this.dropForeignKeyName != null) {
            builder.append(" DROP FOREIGN KEY ").append(this.dropForeignKeyName);
        }
        if (this.charset != null || this.collation != null) {
            builder.append(" CONVERT TO CHARACTER SET");
            if (this.charset != null) {
                builder.append(" ").append(this.charset);
            }
            if (this.collation != null) {
                builder.append(" COLLATE ").append(this.collation);
            }
        }
        if (this.interval != null) {
            builder.append(" SET INTERVAL(").append(this.interval).append(")");
        }
        if (this.enableAllTriggers != null) {
            builder.append(" ").append(this.enableAllTriggers ? "ENABLE" : "DISABLE").append(" ALL TRIGGERS");
        }
        if (Boolean.TRUE.equals(this.dropPrimaryKey)) {
            builder.append(" DROP PRIMARY KEY");
        }
        if (this.modifyPrimaryKey != null) {
            builder.append(" MODIFY ").append(this.modifyPrimaryKey);
        }
        if (this.refresh) {
            builder.append(" REFRESH");
        }
        if (Boolean.TRUE.equals(this.removePartitioning)) {
            builder.append(" REMOVE PARTITIONING");
        }
        if (addColumnGroupElements != null) {
            builder.append(" ADD COLUMN GROUP(")
                    .append(addColumnGroupElements.stream().map(ColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        if (dropColumnGroupElements != null) {
            builder.append(" DROP COLUMN GROUP(")
                    .append(dropColumnGroupElements.stream().map(ColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

    private void appendUpdateGlobalIndexes(StringBuilder builder) {
        if (Boolean.TRUE.equals(this.updateGlobalIndexes)) {
            builder.append(" UPDATE GLOBAL INDEXES");
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class AlterColumnBehavior extends BaseStatement {

        private Expression defaultValue;

        public AlterColumnBehavior(@NonNull ParserRuleContext context) {
            super(context);
        }

        public boolean isDropDefault() {
            return this.defaultValue == null;
        }

        public boolean isSetDefault() {
            return this.defaultValue != null;
        }

        @Override
        public String toString() {
            return this.isSetDefault() ? "SET DEFAULT" + this.defaultValue : "DROP DEFAULT";
        }
    }

}
