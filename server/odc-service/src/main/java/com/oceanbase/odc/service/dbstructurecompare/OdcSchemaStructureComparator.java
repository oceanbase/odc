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

package com.oceanbase.odc.service.dbstructurecompare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditorFactory;
import com.oceanbase.odc.service.dbstructurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.dbstructurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.GeneralSqlStatementBuilder;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/12/12
 * @since ODC_release_4.2.4
 */
@Slf4j
public class OdcSchemaStructureComparator implements DBStructureComparator {
    private String sourceSchemaName;
    private String targetSchemaName;
    private DBSchemaAccessor targetAccessor;
    private DBSchemaAccessor sourceAccessor;
    private DBTableEditor targetTableEditor;
    private DialectType targetDialectType;
    private Map<String, DBTable> sourceTableMapping = new HashMap<>();
    private Map<String, DBTable> targetTableMapping = new HashMap<>();
    private final String DEFAULT_SQL_DELIMITER = ";";

    public OdcSchemaStructureComparator(@NonNull ConnectionSession sourceConnectionSession,
            @NonNull ConnectionSession targetConnectionSession) {
        this.sourceAccessor = DBSchemaAccessors.create(sourceConnectionSession);
        this.targetAccessor = DBSchemaAccessors.create(targetConnectionSession);
        this.targetTableEditor = new DBTableEditorFactory(
                targetConnectionSession.getConnectType(), ConnectionSessionUtil.getVersion(targetConnectionSession))
                        .create();
        this.sourceSchemaName = ConnectionSessionUtil.getCurrentSchema(sourceConnectionSession);
        this.targetSchemaName = ConnectionSessionUtil.getCurrentSchema(targetConnectionSession);
        this.targetDialectType = targetConnectionSession.getDialectType();
    }

    @Override
    public List<DBObjectComparisonResult> compareTables() {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        initTableMapping();
        if (this.sourceTableMapping.isEmpty() && this.targetTableMapping.isEmpty()) {
            return returnVal;
        }

        List<String> toCreatedNames = new ArrayList<>();
        List<String> toComparedNames = new ArrayList<>();
        List<String> toDroppedNames = new ArrayList<>();
        for (String tableName : sourceTableMapping.keySet()) {
            if (!targetTableMapping.containsKey(tableName)) {
                toCreatedNames.add(tableName);
            } else {
                toComparedNames.add(tableName);
            }
        }
        for (String tableName : targetTableMapping.keySet()) {
            if (!sourceTableMapping.containsKey(tableName)) {
                toDroppedNames.add(tableName);
            }
        }

        List<DBObjectComparisonResult> createdResults =
                buildCreatedTableResults(toCreatedNames, sourceTableMapping, sourceSchemaName, targetSchemaName);
        List<DBObjectComparisonResult> dropResults =
                buildDroppedTableResults(toDroppedNames, targetTableMapping, sourceSchemaName, targetSchemaName);

        List<DBObjectComparisonResult> comparedResults = new ArrayList<>();
        toComparedNames.forEach(name -> {
            comparedResults.add(compareSingleTable(sourceTableMapping.get(name), targetTableMapping.get(name)));
        });

        returnVal.addAll(createdResults);
        returnVal.addAll(comparedResults);
        returnVal.addAll(dropResults);

        return returnVal;
    }

    @Override
    public List<DBObjectComparisonResult> compareTables(List<String> tableNames) {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        initTableMapping();
        if (tableNames.isEmpty()) {
            return returnVal;
        }

        tableNames.forEach(name -> {
            DBObjectComparisonResult result =
                    new DBObjectComparisonResult(DBObjectType.TABLE, name, sourceSchemaName, targetSchemaName);
            if (!sourceTableMapping.containsKey(name)) {
                result.setComparisonResult(ComparisonResult.MISSING_IN_SOURCE);
            } else {
                if (!targetTableMapping.containsKey(name)) {
                    // table to be created
                    result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
                    DBTable copiedSrcTable = new DBTable();
                    BeanUtils.copyProperties(sourceTableMapping.get(name), copiedSrcTable);
                    copiedSrcTable.setSchemaName(this.targetSchemaName);
                    result.setChangeScript(this.targetTableEditor.generateCreateObjectDDL(copiedSrcTable));
                } else {
                    // table to be compared
                    result = compareSingleTable(sourceTableMapping.get(name), targetTableMapping.get(name));
                }
            }
            returnVal.add(result);
        });

        return returnVal;
    }

    private List<DBObjectComparisonResult> buildCreatedTableResults(List<String> toCreate,
            Map<String, DBTable> sourceTableMapping, String sourceSchemaName, String targetSchemaName) {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        if (toCreate.isEmpty()) {
            return returnVal;
        }
        toCreate = getTableNamesByDependencyOrder(toCreate, sourceTableMapping, sourceSchemaName);

        toCreate.forEach(name -> {
            DBObjectComparisonResult result =
                    new DBObjectComparisonResult(DBObjectType.TABLE, name, sourceSchemaName, targetSchemaName);
            result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
            DBTable sourceTable = sourceTableMapping.get(name);
            result.setSourceDdl(sourceTable.getDDL());
            DBTable targetTable = new DBTable();
            BeanUtils.copyProperties(sourceTable, targetTable);
            targetTable.setSchemaName(targetSchemaName);
            result.setChangeScript(this.targetTableEditor.generateCreateObjectDDL(targetTable));
            returnVal.add(result);
        });
        return returnVal;
    }

    private List<DBObjectComparisonResult> buildDroppedTableResults(List<String> toDrop,
            Map<String, DBTable> targetTableMapping,
            String sourceSchemaName, String targetSchemaName) {
        List<DBObjectComparisonResult> returnVal = new LinkedList<>();
        if (toDrop.isEmpty()) {
            return returnVal;
        }
        toDrop = getTableNamesByDependencyOrder(toDrop, targetTableMapping, targetSchemaName);
        Collections.reverse(toDrop);

        toDrop.forEach(name -> {
            DBObjectComparisonResult result =
                    new DBObjectComparisonResult(DBObjectType.TABLE, name, sourceSchemaName, targetSchemaName);
            result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
            result.setTargetDdl(targetTableMapping.get(name).getDDL());
            SqlBuilder sqlBuilder = getTargetDBSqlBuilder();
            result.setChangeScript(
                    GeneralSqlStatementBuilder.drop(sqlBuilder, DBObjectType.TABLE, targetSchemaName, name));
            returnVal.add(result);
        });
        return returnVal;
    }

    private DBObjectComparisonResult compareSingleTable(DBTable sourceTable, DBTable targetTable) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.TABLE, sourceTable.getName(),
                this.sourceSchemaName, this.targetSchemaName);

        // compare table options
        SqlBuilder tableOptionDdl = getTargetDBSqlBuilder();
        DBTable copiedSrcTable = new DBTable();
        BeanUtils.copyProperties(sourceTable, copiedSrcTable);
        copiedSrcTable.setSchemaName(this.targetSchemaName);
        this.targetTableEditor.generateUpdateTableOptionDDL(targetTable, copiedSrcTable, tableOptionDdl);
        result.setChangeScript(tableOptionDdl.toString());
        result.setSourceDdl(sourceTable.getDDL());
        result.setTargetDdl(targetTable.getDDL());

        // compare table columns
        List<DBObjectComparisonResult> columns =
                compareTableColumns(sourceTable.getColumns(), targetTable.getColumns());
        // compare table index
        List<DBObjectComparisonResult> indexes =
                compareTableIndexes(
                        this.targetTableEditor.excludePrimaryKeyIndex(sourceTable.getIndexes(),
                                sourceTable.getConstraints()),
                        this.targetTableEditor.excludePrimaryKeyIndex(targetTable.getIndexes(),
                                targetTable.getConstraints()));
        // compare table constraints
        List<DBObjectComparisonResult> constraints =
                compareTableConstraints(
                        this.targetTableEditor.excludeUniqueConstraint(sourceTable.getIndexes(),
                                sourceTable.getConstraints()),
                        this.targetTableEditor.excludeUniqueConstraint(targetTable.getIndexes(),
                                targetTable.getConstraints()));
        // compare table partition
        DBObjectComparisonResult partition =
                compareTablePartition(sourceTable.getPartition(), targetTable.getPartition());

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

    private List<DBObjectComparisonResult> compareTableColumns(List<DBTableColumn> srcTabCols,
            List<DBTableColumn> tgtTabCols) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (srcTabCols.isEmpty() && tgtTabCols.isEmpty()) {
            return returnVal;
        }

        List<String> srcColNames = srcTabCols.stream().map(DBTableColumn::getName).collect(Collectors.toList());
        List<String> tarColNames = tgtTabCols.stream().map(DBTableColumn::getName).collect(Collectors.toList());
        Map<String, DBTableColumn> srcColMapping =
                srcTabCols.stream().collect(Collectors.toMap(DBTableColumn::getName, col -> col));
        Map<String, DBTableColumn> tarColMapping =
                tgtTabCols.stream().collect(Collectors.toMap(DBTableColumn::getName, col -> col));

        tarColNames.forEach(tarColName -> {
            if (!srcColNames.contains(tarColName)) {
                // column to be dropped
                DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, tarColName,
                        this.sourceSchemaName, this.targetSchemaName);
                result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
                result.setChangeScript(
                        appendDelimiterIfNotExist(this.targetTableEditor.getColumnEditor()
                                .generateDropObjectDDL(tarColMapping.get(tarColName))));
                returnVal.add(result);
            }
        });

        srcColNames.forEach(srcColName -> {
            DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, srcColName,
                    this.sourceSchemaName, this.targetSchemaName);
            DBTableColumn copiedSrcCol = new DBTableColumn();
            BeanUtils.copyProperties(srcColMapping.get(srcColName), copiedSrcCol);
            copiedSrcCol.setSchemaName(this.targetSchemaName);
            if (tarColNames.contains(srcColName)) {
                String ddl = this.targetTableEditor.getColumnEditor().generateUpdateObjectDDL(
                        tarColMapping.get(srcColName), copiedSrcCol);
                if (!ddl.isEmpty()) {
                    // column to be updated
                    result.setComparisonResult(ComparisonResult.INCONSISTENT);
                    result.setChangeScript(appendDelimiterIfNotExist(ddl));
                } else {
                    result.setComparisonResult(ComparisonResult.CONSISTENT);
                }
            } else {
                // column to be created
                result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
                result.setChangeScript(appendDelimiterIfNotExist(
                        this.targetTableEditor.getColumnEditor().generateCreateObjectDDL(copiedSrcCol)));
            }
            returnVal.add(result);
        });

        return returnVal;
    }

    private List<DBObjectComparisonResult> compareTableIndexes(List<DBTableIndex> srcTabIdx,
            List<DBTableIndex> tgtTabIdx) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (srcTabIdx.isEmpty() && tgtTabIdx.isEmpty()) {
            return returnVal;
        }

        List<String> srcIdxNames = srcTabIdx.stream().map(DBTableIndex::getName).collect(Collectors.toList());
        List<String> tarIdxNames = tgtTabIdx.stream().map(DBTableIndex::getName).collect(Collectors.toList());
        Map<String, DBTableIndex> srcIdxMapping =
                srcTabIdx.stream().collect(Collectors.toMap(DBTableIndex::getName, col -> col));
        Map<String, DBTableIndex> tarIdxMapping =
                tgtTabIdx.stream().collect(Collectors.toMap(DBTableIndex::getName, col -> col));

        tarIdxNames.forEach(tarIdxName -> {
            if (!srcIdxNames.contains(tarIdxName)) {
                // index to be dropped
                DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, tarIdxName,
                        this.sourceSchemaName, this.targetSchemaName);
                result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
                result.setChangeScript(
                        appendDelimiterIfNotExist(this.targetTableEditor.getIndexEditor()
                                .generateDropObjectDDL(tarIdxMapping.get(tarIdxName))));
                returnVal.add(result);
            }
        });

        srcIdxNames.forEach(srcIdxName -> {
            DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, srcIdxName,
                    this.sourceSchemaName, this.targetSchemaName);
            DBTableIndex copiedSrcIdx = new DBTableIndex();
            BeanUtils.copyProperties(srcIdxMapping.get(srcIdxName), copiedSrcIdx);
            copiedSrcIdx.setSchemaName(this.targetSchemaName);
            if (tarIdxNames.contains(srcIdxName)) {
                String ddl = this.targetTableEditor.getIndexEditor().generateUpdateObjectDDL(
                        tarIdxMapping.get(srcIdxName), copiedSrcIdx);
                if (!ddl.isEmpty()) {
                    // index to be updated
                    result.setComparisonResult(ComparisonResult.INCONSISTENT);
                    result.setChangeScript(appendDelimiterIfNotExist(ddl));
                } else {
                    result.setComparisonResult(ComparisonResult.CONSISTENT);
                }
            } else {
                // index to be created
                result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
                result.setChangeScript(appendDelimiterIfNotExist(
                        this.targetTableEditor.getIndexEditor().generateCreateObjectDDL(copiedSrcIdx)));
            }
            returnVal.add(result);
        });

        return returnVal;
    }

    private List<DBObjectComparisonResult> compareTableConstraints(List<DBTableConstraint> srcTabCons,
            List<DBTableConstraint> tgtTabCons) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (srcTabCons.isEmpty() && tgtTabCons.isEmpty()) {
            return returnVal;
        }

        List<String> srcConsNames = srcTabCons.stream().map(DBTableConstraint::getName).collect(Collectors.toList());
        List<String> tarConsNames = tgtTabCons.stream().map(DBTableConstraint::getName).collect(Collectors.toList());
        Map<String, DBTableConstraint> srcConsMapping =
                srcTabCons.stream().collect(Collectors.toMap(DBTableConstraint::getName, col -> col));
        Map<String, DBTableConstraint> tarConsMapping =
                tgtTabCons.stream().collect(Collectors.toMap(DBTableConstraint::getName, col -> col));

        tarConsNames.forEach(tarConsName -> {
            if (!srcConsNames.contains(tarConsName)) {
                // constraint to be dropped
                DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, tarConsName,
                        this.sourceSchemaName, this.targetSchemaName);
                result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
                result.setChangeScript(appendDelimiterIfNotExist(this.targetTableEditor.getConstraintEditor()
                        .generateDropObjectDDL(tarConsMapping.get(tarConsName))));
                returnVal.add(result);
            }
        });

        srcConsNames.forEach(srcConsName -> {
            DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, srcConsName,
                    this.sourceSchemaName, this.targetSchemaName);
            DBTableConstraint copiedSrcCons = new DBTableConstraint();
            BeanUtils.copyProperties(srcConsMapping.get(srcConsName), copiedSrcCons);
            copiedSrcCons.setSchemaName(this.targetSchemaName);
            if (tarConsNames.contains(srcConsName)) {
                String ddl = this.targetTableEditor.getConstraintEditor().generateUpdateObjectDDL(
                        tarConsMapping.get(srcConsName), copiedSrcCons);
                if (!ddl.isEmpty()) {
                    // constraint to be updated
                    result.setComparisonResult(ComparisonResult.INCONSISTENT);
                    result.setChangeScript(appendDelimiterIfNotExist(ddl));
                } else {
                    result.setComparisonResult(ComparisonResult.CONSISTENT);
                }
            } else {
                // constraint to be created
                result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
                result.setChangeScript(
                        appendDelimiterIfNotExist(
                                this.targetTableEditor.getConstraintEditor().generateCreateObjectDDL(copiedSrcCons)));
            }
            returnVal.add(result);
        });

        return returnVal;
    }

    private DBObjectComparisonResult compareTablePartition(DBTablePartition srcPartition,
            DBTablePartition tgtPartition) {
        DBObjectComparisonResult result =
                new DBObjectComparisonResult(DBObjectType.PARTITION, this.sourceSchemaName, this.targetSchemaName);
        if (Objects.isNull(srcPartition) && Objects.isNull(tgtPartition)) {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        } else if (Objects.isNull(srcPartition)) {
            // partition to be dropped
            result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
            result.setChangeScript(appendDelimiterIfNotExist(
                    this.targetTableEditor.getPartitionEditor().generateDropObjectDDL(tgtPartition)));
        } else if (Objects.isNull(tgtPartition)) {
            // partition to be created
            result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
            DBTablePartition copiedSrcPartition = new DBTablePartition();
            BeanUtils.copyProperties(srcPartition, copiedSrcPartition);
            copiedSrcPartition.setSchemaName(this.targetSchemaName);
            result.setChangeScript(
                    appendDelimiterIfNotExist(
                            this.targetTableEditor.getPartitionEditor().generateCreateObjectDDL(copiedSrcPartition)));
        } else {
            DBTablePartition copiedSrcPartition = new DBTablePartition();
            BeanUtils.copyProperties(srcPartition, copiedSrcPartition);
            copiedSrcPartition.setSchemaName(this.targetSchemaName);
            String ddl = this.targetTableEditor.getPartitionEditor().generateUpdateObjectDDL(tgtPartition,
                    copiedSrcPartition);
            if (ddl.isEmpty()) {
                result.setComparisonResult(ComparisonResult.CONSISTENT);
            } else {
                // partition to be updated
                result.setComparisonResult(ComparisonResult.INCONSISTENT);
                result.setChangeScript(appendDelimiterIfNotExist(ddl));
            }
        }
        return result;
    }

    /**
     * Construct a topology graph based on foreign key dependencies and obtain a list of table names
     * sorted by the topology graph.
     */
    private List<String> getTableNamesByDependencyOrder(List<String> tableNames, Map<String, DBTable> tableMapping,
            String schemaName) {
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Build dependency graph and in-degree map
        tableNames.forEach(tableName -> {
            for (DBTableConstraint constraint : tableMapping.get(tableName).getConstraints()) {
                if (constraint.getType().equals(DBConstraintType.FOREIGN_KEY)) {
                    String referenceTableName = constraint.getReferenceTableName();
                    if (schemaName.equals(constraint.getReferenceSchemaName())
                            && tableNames.contains(referenceTableName)) {
                        dependencyGraph.computeIfAbsent(referenceTableName, k -> new ArrayList<>()).add(tableName);
                        inDegree.put(tableName, inDegree.getOrDefault(tableName, 0) + 1);
                    }
                }
            }
            dependencyGraph.putIfAbsent(tableName, new ArrayList<>());
            inDegree.putIfAbsent(tableName, 0);
        });

        List<String> tabNamesByDependencyOrder = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        // Add all tables with in-degree of 0 to the queue
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        // Topological sort
        while (!queue.isEmpty()) {
            String currentTable = queue.poll();
            tabNamesByDependencyOrder.add(currentTable);

            for (String dependentTable : dependencyGraph.get(currentTable)) {
                int updatedInDegree = inDegree.get(dependentTable) - 1;
                inDegree.put(dependentTable, updatedInDegree);

                if (updatedInDegree == 0) {
                    queue.add(dependentTable);
                }
            }
        }

        // Check for a cycle, if the graph has a cycle, tables names will not be sorted
        if (tabNamesByDependencyOrder.size() != tableNames.size()) {
            log.warn("Dependency cycle detected, schemaName: {}", schemaName);
            return tableNames;
        }

        return tabNamesByDependencyOrder;
    }

    private void initTableMapping() {
        this.sourceTableMapping =
                this.sourceTableMapping.isEmpty() ? buildSchemaTables(sourceAccessor, sourceSchemaName)
                        : this.sourceTableMapping;
        this.targetTableMapping =
                this.targetTableMapping.isEmpty() ? buildSchemaTables(targetAccessor, targetSchemaName)
                        : this.targetTableMapping;
    }

    private Map<String, DBTable> buildSchemaTables(DBSchemaAccessor accessor, String schemaName) {
        Map<String, DBTable> returnVal = new HashMap<>();
        List<String> tableNames = accessor.showTables(schemaName);
        if (tableNames.isEmpty()) {
            return returnVal;
        }
        Map<String, List<DBTableColumn>> tableName2Columns = accessor.listTableColumns(schemaName);
        Map<String, List<DBTableIndex>> tableName2Indexes = accessor.listTableIndexes(schemaName);
        Map<String, List<DBTableConstraint>> tableName2Constraints = accessor.listTableConstraints(schemaName);
        Map<String, DBTableOptions> tableName2Options = accessor.listTableOptions(schemaName);
        for (String tableName : tableNames) {
            if (!tableName2Columns.containsKey(tableName)) {
                log.warn("Table: {} has no columns, schema name: {}", tableName, schemaName);
                continue;
            }
            DBTable table = new DBTable();
            table.setSchemaName(schemaName);
            table.setOwner(schemaName);
            table.setName(tableName);
            table.setColumns(tableName2Columns.getOrDefault(tableName, Lists.newArrayList()));
            table.setIndexes(tableName2Indexes.getOrDefault(tableName, Lists.newArrayList()));
            table.setConstraints(tableName2Constraints.getOrDefault(tableName, Lists.newArrayList()));
            table.setTableOptions(tableName2Options.getOrDefault(tableName, new DBTableOptions()));
            table.setPartition(accessor.getPartition(schemaName, tableName));
            table.setDDL(accessor.getTableDDL(schemaName, tableName));
            returnVal.put(tableName, table);
        }
        return returnVal;
    }

    private SqlBuilder getTargetDBSqlBuilder() {
        if (this.targetDialectType.isMysql()) {
            return new MySQLSqlBuilder();
        } else {
            return new OracleSqlBuilder();
        }
    }

    private String appendDelimiterIfNotExist(String sql) {
        String returnVal = sql.trim();
        if (!returnVal.endsWith(DEFAULT_SQL_DELIMITER)) {
            return returnVal + DEFAULT_SQL_DELIMITER + "\n";
        }
        return sql;
    }
}
