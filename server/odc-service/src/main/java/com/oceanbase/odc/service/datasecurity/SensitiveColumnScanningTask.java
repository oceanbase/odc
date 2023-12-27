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
import java.util.Set;
import java.util.concurrent.Callable;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.connection.database.model.Database;
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
    private final SensitiveColumnRecognizer recognizer;
    private final SensitiveColumnScanningTaskInfo taskInfo;
    private final Map<String, List<DBTableColumn>> table2Columns;
    private final Map<String, List<DBTableColumn>> view2Columns;
    private final Set<SensitiveColumnMeta> existsSensitiveColumns;

    public SensitiveColumnScanningTask(Database database, List<SensitiveRule> rules,
            SensitiveColumnScanningTaskInfo taskInfo, List<SensitiveColumnMeta> existsSensitiveColumns,
            Map<String, List<DBTableColumn>> table2Columns, Map<String, List<DBTableColumn>> view2Columns) {
        this.database = database;
        this.recognizer = new SensitiveColumnRecognizer(rules);
        this.table2Columns = table2Columns;
        this.view2Columns = view2Columns;
        this.taskInfo = taskInfo;
        this.existsSensitiveColumns = new HashSet<>(existsSensitiveColumns);
    }

    @Override
    public Void call() throws Exception {
        try {
            taskInfo.setStatus(ScanningTaskStatus.RUNNING);
            scanColumns(table2Columns, SensitiveColumnType.TABLE_COLUMN);
            scanColumns(view2Columns, SensitiveColumnType.VIEW_COLUMN);
        } catch (Exception e) {
            taskInfo.setCompleteTime(new Date());
            taskInfo.setStatus(ScanningTaskStatus.FAILED);
            taskInfo.setErrorCode(ErrorCodes.Unexpected);
            taskInfo.setErrorMsg(String.format("Some errors happen when scanning sensitive column, database=%s",
                    database.getName()));
        }
        return null;
    }

    private void scanColumns(Map<String, List<DBTableColumn>> object2Columns, SensitiveColumnType columnType) {
        for (String objectName : object2Columns.keySet()) {
            List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
            for (DBTableColumn dbTableColumn : object2Columns.get(objectName)) {
                if (recognizer.recognize(dbTableColumn) && !existsSensitiveColumns
                        .contains(new SensitiveColumnMeta(database.getId(), objectName, dbTableColumn.getName()))) {
                    SensitiveColumn column = new SensitiveColumn();
                    column.setType(columnType);
                    column.setDatabase(database);
                    column.setTableName(objectName);
                    column.setColumnName(dbTableColumn.getName());
                    column.setMaskingAlgorithmId(recognizer.maskingAlgorithmId());
                    column.setSensitiveRuleId(recognizer.sensitiveRuleId());
                    column.setLevel(recognizer.sensitiveLevel());
                    sensitiveColumns.add(column);
                    existsSensitiveColumns
                            .add(new SensitiveColumnMeta(database.getId(), objectName, dbTableColumn.getName()));
                }
            }
            taskInfo.addSensitiveColumns(sensitiveColumns);
            taskInfo.addFinishedTableCount();
        }
    }

}
