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
package com.oceanbase.odc.service.structurecompare.comparedbobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.common.util.ListUtils;
import com.oceanbase.odc.common.util.TopoOrderComparator;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.GeneralSqlStatementBuilder;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class TableStructureComparator implements DBObjectStructureComparator<DBTable> {
    private DBTableEditor tgtTableEditor;
    private DialectType tgtDialectType;
    private String srcSchemaName;
    private String tgtSchemaName;

    public TableStructureComparator(DBTableEditor tgtTableEditor, DialectType tgtDialectType, String srcSchemaName,
            String tgtSchemaName) {
        this.tgtTableEditor = tgtTableEditor;
        this.tgtDialectType = tgtDialectType;
        this.srcSchemaName = srcSchemaName;
        this.tgtSchemaName = tgtSchemaName;
    }

    @Override
    public List<DBObjectComparisonResult> compare(List<DBTable> srcTables, List<DBTable> tgtTables) {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        if (srcTables.isEmpty() && tgtTables.isEmpty()) {
            return returnVal;
        } else if (srcTables.isEmpty()) {
            return buildDroppedTableResults(tgtTables.stream().map(DBTable::getName).collect(Collectors.toList()),
                    new HashMap<>(), srcSchemaName, tgtSchemaName);
        } else if (tgtTables.isEmpty()) {
            return buildCreatedTableResults(srcTables.stream().map(DBTable::getName).collect(Collectors.toList()),
                    new HashMap<>(), srcSchemaName, tgtSchemaName);
        }

        String srcSchemaName = srcTables.get(0).getSchemaName();
        String tgtSchemaName = tgtTables.get(0).getSchemaName();
        Map<String, DBTable> srcTableMapping =
                srcTables.stream().collect(Collectors.toMap(DBTable::getName, table -> table));
        Map<String, DBTable> tgtTableMapping =
                tgtTables.stream().collect(Collectors.toMap(DBTable::getName, table -> table));
        List<String> toCreatedNames = new ArrayList<>();
        List<String> toComparedNames = new ArrayList<>();
        List<String> toDroppedNames = new ArrayList<>();

        for (String tableName : srcTableMapping.keySet()) {
            if (!tgtTableMapping.containsKey(tableName)) {
                toCreatedNames.add(tableName);
            } else {
                toComparedNames.add(tableName);
            }
        }
        for (String tableName : tgtTableMapping.keySet()) {
            if (!srcTableMapping.containsKey(tableName)) {
                toDroppedNames.add(tableName);
            }
        }

        List<DBObjectComparisonResult> createdResults =
                buildCreatedTableResults(toCreatedNames, srcTableMapping, srcSchemaName, tgtSchemaName);
        List<DBObjectComparisonResult> dropResults =
                buildDroppedTableResults(toDroppedNames, tgtTableMapping, srcSchemaName, tgtSchemaName);

        List<DBObjectComparisonResult> comparedResults = new ArrayList<>();
        toComparedNames.forEach(name -> {
            comparedResults.add(compare(srcTableMapping.get(name), tgtTableMapping.get(name)));
        });

        returnVal.addAll(createdResults);
        returnVal.addAll(comparedResults);
        returnVal.addAll(dropResults);

        return returnVal;
    }

    private List<DBObjectComparisonResult> buildCreatedTableResults(List<String> toCreate,
            Map<String, DBTable> srcTableMapping, String srcSchemaName, String tgtSchemaName) {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        if (toCreate.isEmpty()) {
            return returnVal;
        }
        toCreate = getTableNamesByDependencyOrder(toCreate, srcTableMapping, srcSchemaName);

        toCreate.forEach(name -> {
            DBObjectComparisonResult result =
                    new DBObjectComparisonResult(DBObjectType.TABLE, name, srcSchemaName, tgtSchemaName);
            result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
            DBTable sourceTable = srcTableMapping.get(name);
            result.setSourceDdl(sourceTable.getDDL());
            DBTable targetTable = new DBTable();
            BeanUtils.copyProperties(sourceTable, targetTable);
            targetTable.setSchemaName(tgtSchemaName);
            result.setChangeScript(this.tgtTableEditor.generateCreateObjectDDL(targetTable));
            returnVal.add(result);
        });
        return returnVal;
    }

    private List<DBObjectComparisonResult> buildDroppedTableResults(List<String> toDrop,
            Map<String, DBTable> tgtTableMapping,
            String srcSchemaName, String tgtSchemaName) {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        if (toDrop.isEmpty()) {
            return returnVal;
        }
        toDrop = getTableNamesByDependencyOrder(toDrop, tgtTableMapping, tgtSchemaName);
        Collections.reverse(toDrop);

        toDrop.forEach(name -> {
            DBObjectComparisonResult result =
                    new DBObjectComparisonResult(DBObjectType.TABLE, name, srcSchemaName, tgtSchemaName);
            result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
            result.setTargetDdl(tgtTableMapping.get(name).getDDL());
            SqlBuilder sqlBuilder = getTargetDBSqlBuilder();
            result.setChangeScript(
                    GeneralSqlStatementBuilder.drop(sqlBuilder, DBObjectType.TABLE, tgtSchemaName, name));
            returnVal.add(result);
        });
        return returnVal;
    }

    /**
     * Construct a topology graph based on foreign key dependencies and obtain a list of table names
     * sorted by the topology graph.
     */
    private List<String> getTableNamesByDependencyOrder(List<String> tableNames, Map<String, DBTable> tableMapping,
            String schemaName) {
        Map<String, Set<String>> dependencyGraph = new HashMap<>();

        // Build dependency graph
        tableNames.forEach(tableName -> {
            for (DBTableConstraint constraint : tableMapping.get(tableName).getConstraints()) {
                if (constraint.getType().equals(DBConstraintType.FOREIGN_KEY)) {
                    String referenceTableName = constraint.getReferenceTableName();
                    if (schemaName.equals(constraint.getReferenceSchemaName())
                            && tableNames.contains(referenceTableName)) {
                        dependencyGraph.computeIfAbsent(referenceTableName, k -> new HashSet<>()).add(tableName);
                    }
                }
            }
        });

        if (dependencyGraph.isEmpty()) {
            return tableNames;
        }
        ListUtils.sortByTopoOrder(tableNames, new TopoOrderComparator<>(dependencyGraph));
        return tableNames;
    }

    private SqlBuilder getTargetDBSqlBuilder() {
        if (this.tgtDialectType.isMysql()) {
            return new MySQLSqlBuilder();
        } else {
            return new OracleSqlBuilder();
        }
    }

    @Override
    public DBObjectComparisonResult compare(DBTable sourceTable, DBTable targetTable) {
        if (Objects.isNull(sourceTable) && Objects.isNull(targetTable)) {
            throw new IllegalStateException("Both source table and target table are null");
        } else if (Objects.isNull(sourceTable)) {
            return buildDroppedTableResults(Collections.singletonList(targetTable.getName()),
                    Collections.singletonMap(targetTable.getName(), targetTable), this.srcSchemaName,
                    this.tgtSchemaName).get(0);
        } else if (Objects.isNull(targetTable)) {
            return buildCreatedTableResults(Collections.singletonList(sourceTable.getName()),
                    Collections.singletonMap(sourceTable.getName(), sourceTable), this.srcSchemaName,
                    this.tgtSchemaName).get(0);
        }

        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.TABLE, sourceTable.getName(),
                sourceTable.getSchemaName(), targetTable.getSchemaName());

        String srcSchemaName = sourceTable.getSchemaName();
        String tgtSchemaName = targetTable.getSchemaName();

        // compare table options
        SqlBuilder tableOptionDdl = getTargetDBSqlBuilder();
        DBTable copiedSrcTable = new DBTable();
        BeanUtils.copyProperties(sourceTable, copiedSrcTable);
        copiedSrcTable.setSchemaName(targetTable.getSchemaName());
        this.tgtTableEditor.generateUpdateTableOptionDDL(targetTable, copiedSrcTable, tableOptionDdl);
        result.setChangeScript(tableOptionDdl.toString());
        result.setSourceDdl(sourceTable.getDDL());
        result.setTargetDdl(targetTable.getDDL());

        // compare table columns
        List<DBObjectComparisonResult> columns =
                new TableColumnStructureComparator(
                        (DBTableColumnEditor) this.tgtTableEditor.getColumnEditor(), srcSchemaName, tgtSchemaName)
                                .compare(sourceTable.getColumns(), targetTable.getColumns());

        // compare table index
        List<DBObjectComparisonResult> indexes =
                new TableIndexStructureComparator((DBTableIndexEditor) this.tgtTableEditor.getIndexEditor(),
                        srcSchemaName, tgtSchemaName)
                                .compare(
                                        this.tgtTableEditor.excludePrimaryKeyIndex(sourceTable.getIndexes(),
                                                sourceTable.getConstraints()),
                                        this.tgtTableEditor.excludePrimaryKeyIndex(targetTable.getIndexes(),
                                                targetTable.getConstraints()));

        // compare table constraints
        List<DBObjectComparisonResult> constraints = new TableConstraintStructureComparator(
                (DBTableConstraintEditor) this.tgtTableEditor.getConstraintEditor(), srcSchemaName, tgtSchemaName)
                        .compare(
                                this.tgtTableEditor.excludeUniqueConstraint(sourceTable.getIndexes(),
                                        sourceTable.getConstraints()),
                                this.tgtTableEditor.excludeUniqueConstraint(targetTable.getIndexes(),
                                        targetTable.getConstraints()));

        // compare table partition
        DBObjectComparisonResult partition =
                new TablePartitionStructureComparator((DBTablePartitionEditor) this.tgtTableEditor.getPartitionEditor(),
                        srcSchemaName, tgtSchemaName)
                                .compare(sourceTable.getPartition(), targetTable.getPartition());

        if (columns.stream().allMatch(item -> item.getComparisonResult().equals(ComparisonResult.CONSISTENT))
                && indexes.stream().allMatch(item -> item.getComparisonResult().equals(ComparisonResult.CONSISTENT))
                && constraints.stream().allMatch(item -> item.getComparisonResult().equals(ComparisonResult.CONSISTENT))
                && partition.getComparisonResult().equals(ComparisonResult.CONSISTENT)
                && tableOptionDdl.toString().isEmpty()) {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        } else {
            result.setComparisonResult(ComparisonResult.INCONSISTENT);
        }

        result.setSubDBObjectComparisonResult(columns);
        result.getSubDBObjectComparisonResult().addAll(indexes);
        result.getSubDBObjectComparisonResult().addAll(constraints);
        result.getSubDBObjectComparisonResult().add(partition);

        return result;
    }
}
