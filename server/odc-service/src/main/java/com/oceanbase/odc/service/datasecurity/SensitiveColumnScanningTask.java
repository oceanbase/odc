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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.datasecurity.factory.ScanningStrategyFactory;
import com.oceanbase.odc.service.datasecurity.model.DefaultSensitiveType;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.ScanningModeType;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnMeta;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo.ScanningTaskStatus;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnType;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/25 14:43
 */
@Slf4j
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
        this.scanningMode = scanningMode;
        ScanningStrategyFactory strategyFactory = new ScanningStrategyFactory();
        this.scanner = new SensitiveColumnScanner(rules, strategyFactory);
        this.table2Columns = table2Columns;
        this.view2Columns = view2Columns;
        this.taskInfo = taskInfo;
        this.existsSensitiveColumns = new HashSet<>(existsSensitiveColumns);
        this.ruleMap = rules.stream().collect(Collectors.toMap(SensitiveRule::getId, Function.identity()));
    }

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
            scanColumns(table2Columns, SensitiveColumnType.TABLE_COLUMN);
            if (taskInfo.isCancelled()) {
                return null;
            }
            scanColumns(view2Columns, SensitiveColumnType.VIEW_COLUMN);
        } catch (Exception e) {
            if (!taskInfo.isCancelled()) {
                taskInfo.setStatus(ScanningTaskStatus.FAILED);
                taskInfo.setErrorCode(ErrorCodes.Unexpected);
                taskInfo.setErrorMsg(String.format("Error during sensitive column scanning on database=%s, reason=%s",
                        database.getName(), e.getMessage()));
                taskInfo.setCompleteTime(new Date());
            }
        }
        return null;
    }

    private void scanColumns(Map<String, List<DBTableColumn>> object2Columns, SensitiveColumnType columnType) {
        if (object2Columns.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> tableFutures = object2Columns.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    String objectName = entry.getKey();
                    List<DBTableColumn> columns = entry.getValue();

                    try {
                        if (taskInfo.isCancelled()) {
                            return;
                        }
                        Map<String, ScanResult> scanResults = this.scanner.scanBatch(columns, this.scanningMode);
                        if (taskInfo.isCancelled()) {
                            return;
                        }

                        List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
                        for (DBTableColumn dbTableColumn : columns) {
                            String columnKey = getColumnKey(dbTableColumn);
                            ScanResult scanResult = scanResults.get(columnKey);

                            if (scanResult != null) {
                                Optional<RecognitionResult> finalResultOpt = scanResult
                                        .getFinalResult(this.scanningMode);
                                finalResultOpt.ifPresent(finalResult -> {
                                    SensitiveColumnMeta meta = new SensitiveColumnMeta(database.getId(), objectName,
                                            dbTableColumn.getName());
                                    synchronized (existsSensitiveColumns) {
                                        if (!existsSensitiveColumns.contains(meta)) {
                                            SensitiveColumn column = createSensitiveColumn(columnType, objectName,
                                                    dbTableColumn,
                                                    finalResult);
                                            sensitiveColumns.add(column);
                                            existsSensitiveColumns.add(meta);
                                        }
                                    }
                                });
                            }
                        }
                        if (!sensitiveColumns.isEmpty()) {
                            taskInfo.addSensitiveColumns(sensitiveColumns);
                        }
                        taskInfo.addFinishedTableCount();
                    } catch (Exception e) {
                        log.error("Failed to scan table {}: {}", objectName, e.getMessage(), e);
                        taskInfo.addFinishedTableCount();
                    }
                }))
                .collect(Collectors.toList());

        CompletableFuture.allOf(tableFutures.toArray(new CompletableFuture[0])).join();
    }

    private SensitiveColumn createSensitiveColumn(SensitiveColumnType columnType, String objectName,
            DBTableColumn dbTableColumn, RecognitionResult result) {
        SensitiveColumn column = new SensitiveColumn();
        column.setType(columnType);
        column.setDatabase(database);
        column.setTableName(objectName);
        column.setColumnName(dbTableColumn.getName());
        column.setSensitiveRuleId(result.getMatchedRuleId());
        column.setLevel(result.getLevel());
        Long maskingAlgorithmId = determineMaskingAlgorithmId(result);
        column.setMaskingAlgorithmId(maskingAlgorithmId);

        return column;
    }

    private Long determineMaskingAlgorithmId(RecognitionResult result) {
        SensitiveRule matchedRule = this.ruleMap.get(result.getMatchedRuleId());
        if (matchedRule == null) {
            return getSystemDefaultAlgorithmId();
        }

        if (SensitiveRuleType.AI.equals(result.getSourceRuleType()) && result.getSensitiveType() != null) {
            return handleAiRecognitionResult(result.getSensitiveType());
        }

        return matchedRule.getMaskingAlgorithmId();
    }

    private Long handleAiRecognitionResult(String sensitiveType) {
        if (DefaultSensitiveType.isDefaultType(sensitiveType)) {
            Optional<String> algorithmNameOpt = DefaultSensitiveType.getAlgorithmNameBySensitiveType(sensitiveType);
            if (algorithmNameOpt.isPresent()) {
                try {
                    MaskingAlgorithmService algorithmService = SpringContextUtil.getBean(MaskingAlgorithmService.class);
                    Optional<Long> algorithmIdOpt = algorithmService.getAlgorithmIdByName(algorithmNameOpt.get(),
                            database.getOrganizationId());
                    if (algorithmIdOpt.isPresent()) {
                        return algorithmIdOpt.get();
                    }
                } catch (Exception e) {
                    log.error("Failed to get algorithm ID by name: {}", e.getMessage(), e);
                }
            }
        }

        return getSystemDefaultAlgorithmId();
    }

    private Long getSystemDefaultAlgorithmId() {
        try {
            MaskingAlgorithmService algorithmService = SpringContextUtil.getBean(MaskingAlgorithmService.class);
            return algorithmService.getDefaultAlgorithmIdByOrganizationId(database.getOrganizationId());
        } catch (Exception e) {
            log.error("Failed to get default masking algorithm ID: {}", e.getMessage(), e);
            return null;
        }
    }
}
