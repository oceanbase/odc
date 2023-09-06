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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo.ScanningTaskStatus;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2023/5/25 14:43
 */
public class SensitiveColumnScanningTask implements Callable<Void> {

    private final Database database;
    private final List<SensitiveRule> rules;
    private final SensitiveColumnScanningTaskInfo taskInfo;
    private final Map<String, List<DBTableColumn>> table2Columns;
    private final Set<SimplifySensitiveColumn> existsSensitiveColumns;

    public SensitiveColumnScanningTask(Database database, List<SensitiveRule> rules,
            SensitiveColumnScanningTaskInfo taskInfo, Map<String, List<DBTableColumn>> table2Columns,
            List<SensitiveColumn> existsSensitiveColumns) {
        this.database = database;
        this.rules = rules;
        this.table2Columns = table2Columns;
        this.taskInfo = taskInfo;
        if (CollectionUtils.isNotEmpty(existsSensitiveColumns)) {
            this.existsSensitiveColumns = existsSensitiveColumns.stream()
                    .map(c -> new SimplifySensitiveColumn(c.getDatabase().getId(), c.getTableName(), c.getColumnName()))
                    .collect(Collectors.toSet());
        } else {
            this.existsSensitiveColumns = new HashSet<>();
        }
    }

    @Override
    public Void call() throws Exception {
        try {
            taskInfo.setStatus(ScanningTaskStatus.RUNNING);
            SensitiveColumnRecognizer recognizer = new SensitiveColumnRecognizer(rules);
            Set<String> tables = table2Columns.keySet();
            for (String tableName : tables) {
                List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
                for (DBTableColumn dbTableColumn : table2Columns.get(tableName)) {
                    if (recognizer.recognize(dbTableColumn) && !existsSensitiveColumns.contains(
                            new SimplifySensitiveColumn(database.getId(), tableName, dbTableColumn.getName()))) {
                        SensitiveColumn column = new SensitiveColumn();
                        column.setDatabase(database);
                        column.setTableName(tableName);
                        column.setColumnName(dbTableColumn.getName());
                        column.setMaskingAlgorithmId(recognizer.maskingAlgorithmId());
                        column.setSensitiveRuleId(recognizer.sensitiveRuleId());
                        column.setLevel(recognizer.sensitiveLevel());
                        sensitiveColumns.add(column);
                    }
                }
                taskInfo.addSensitiveColumns(sensitiveColumns);
                taskInfo.addFinishedTableCount();
            }
        } catch (Exception e) {
            taskInfo.setCompleteTime(new Date());
            taskInfo.setStatus(ScanningTaskStatus.FAILED);
            taskInfo.setErrorCode(ErrorCodes.Unexpected);
            taskInfo.setErrorMsg(String.format("Some errors happen when scanning sensitive column, database=%s",
                    database.getName()));
        }
        return null;
    }

    @AllArgsConstructor
    private static class SimplifySensitiveColumn {
        private Long databaseId;
        private String tableName;
        private String columnName;

        @Override
        public int hashCode() {
            return Objects.hash(databaseId, tableName.toLowerCase(), columnName.toLowerCase());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SimplifySensitiveColumn) {
                SimplifySensitiveColumn other = (SimplifySensitiveColumn) obj;
                return Objects.equals(databaseId, other.databaseId)
                        && Objects.equals(tableName.toLowerCase(), other.tableName.toLowerCase())
                        && Objects.equals(columnName.toLowerCase(), other.columnName.toLowerCase());
            }
            return false;
        }
    }

}
