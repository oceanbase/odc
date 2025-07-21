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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.datasecurity.factory.ScanningStrategyFactory;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.ScanningModeType;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnMeta;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo.ScanningTaskStatus;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnType;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/25 14:43
 */
public class SensitiveColumnScanningTask implements Callable<Void> {

    private final Database database;
    private final SensitiveColumnScanner scanner;
    private final ScanningModeType scanningMode;
    private final SensitiveColumnScanningTaskInfo taskInfo;
    private final Map<String, List<DBTableColumn>> table2Columns;
    private final Map<String, List<DBTableColumn>> view2Columns;
    private final Set<SensitiveColumnMeta> existsSensitiveColumns;
    private final Map<Long, SensitiveRule> ruleMap;

    public SensitiveColumnScanningTask(Database database, List<SensitiveRule> rules, ScanningModeType scanningMode,
            SensitiveColumnScanningTaskInfo taskInfo, List<SensitiveColumnMeta> existsSensitiveColumns,
            Map<String, List<DBTableColumn>> table2Columns, Map<String, List<DBTableColumn>> view2Columns) {
        this.database = database;
        // 【修改】接收扫描模式，并创建新的扫描器
        this.scanningMode = scanningMode;
        ScanningStrategyFactory strategyFactory = new ScanningStrategyFactory();
        this.scanner = new SensitiveColumnScanner(rules, strategyFactory);
        this.table2Columns = table2Columns;
        this.view2Columns = view2Columns;
        this.taskInfo = taskInfo;
        this.existsSensitiveColumns = new HashSet<>(existsSensitiveColumns);
        // 【修改】将规则列表转换为 Map，方便通过 ID 快速查找
        this.ruleMap = rules.stream().collect(Collectors.toMap(SensitiveRule::getId, Function.identity()));
    }

    /**
     * 生成列的唯一标识符
     */
    private String getColumnKey(DBTableColumn column) {
        return String.format("%s.%s.%s",
                column.getSchemaName() != null ? column.getSchemaName() : "unknown_schema",
                column.getTableName() != null ? column.getTableName() : "unknown_table",
                column.getName() != null ? column.getName() : "unknown_column");
    }

    @Override
    public Void call() {
        try {
            taskInfo.setStatus(ScanningTaskStatus.RUNNING);
            // 调用重构后的 scanColumns 方法
            scanColumns(table2Columns, SensitiveColumnType.TABLE_COLUMN);
            scanColumns(view2Columns, SensitiveColumnType.VIEW_COLUMN);
            taskInfo.setStatus(ScanningTaskStatus.SUCCESS);
        } catch (Exception e) {
            taskInfo.setStatus(ScanningTaskStatus.FAILED);
            taskInfo.setErrorCode(ErrorCodes.Unexpected);
            taskInfo.setErrorMsg(String.format("Error during sensitive column scanning on database=%s, reason=%s",
                    database.getName(), e.getMessage()));
        } finally {
            taskInfo.setCompleteTime(new Date());
        }
        return null;
    }

    // 【修改】scanColumns 的核心逻辑改为批量扫描
    private void scanColumns(Map<String, List<DBTableColumn>> object2Columns, SensitiveColumnType columnType) {
        for (Map.Entry<String, List<DBTableColumn>> entry : object2Columns.entrySet()) {
            String objectName = entry.getKey();
            List<DBTableColumn> columns = entry.getValue();

            // 【改为批量扫描】一次性扫描整个表的所有列
            Map<String, ScanResult> scanResults = this.scanner.scanBatch(columns, this.scanningMode);

            List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
            for (DBTableColumn dbTableColumn : columns) {
                String columnKey = getColumnKey(dbTableColumn);
                ScanResult scanResult = scanResults.get(columnKey);

                if (scanResult != null) {
                    // 根据扫描模式获取最终的识别结果
                    Optional<RecognitionResult> finalResultOpt = scanResult.getFinalResult(this.scanningMode);

                    // 如果最终有识别结果，则处理
                    finalResultOpt.ifPresent(finalResult -> {
                        SensitiveColumnMeta meta = new SensitiveColumnMeta(database.getId(), objectName,
                                dbTableColumn.getName());
                        if (!existsSensitiveColumns.contains(meta)) {
                            SensitiveColumn column = createSensitiveColumn(columnType, objectName, dbTableColumn,
                                    finalResult);
                            sensitiveColumns.add(column);
                            existsSensitiveColumns.add(meta);
                        }
                    });
                }
            }
            taskInfo.addSensitiveColumns(sensitiveColumns);
            taskInfo.addFinishedTableCount();
        }
    }

    // 【新增】辅助方法，用于创建 SensitiveColumn 对象，使代码更清晰
    private SensitiveColumn createSensitiveColumn(SensitiveColumnType columnType, String objectName,
            DBTableColumn dbTableColumn, RecognitionResult result) {
        SensitiveColumn column = new SensitiveColumn();
        column.setType(columnType);
        column.setDatabase(database);
        column.setTableName(objectName);
        column.setColumnName(dbTableColumn.getName());
        // 从 RecognitionResult 获取 ruleId 和 level
        column.setSensitiveRuleId(result.getMatchedRuleId());
        column.setLevel(result.getLevel());
        // 通过 ruleId 从我们保存的 ruleMap 中找到对应的规则，再获取脱敏算法ID
        SensitiveRule matchedRule = this.ruleMap.get(result.getMatchedRuleId());
        if (matchedRule != null) {
            column.setMaskingAlgorithmId(matchedRule.getMaskingAlgorithmId());
        }
        return column;
    }
}
