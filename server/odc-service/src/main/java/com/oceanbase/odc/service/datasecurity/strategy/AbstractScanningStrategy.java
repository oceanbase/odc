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
package com.oceanbase.odc.service.datasecurity.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
public abstract class AbstractScanningStrategy implements ScanningStrategy {
    protected Optional<RecognitionResult> findFirstMatch(List<ColumnRecognizer> recognizers, DBTableColumn column) {
        for (ColumnRecognizer recognizer : recognizers) {
            Optional<RecognitionResult> result = recognizer.recognize(column);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    protected Map<String, Optional<RecognitionResult>> findAllFirstMatches(List<ColumnRecognizer> recognizers,
            List<DBTableColumn> columns) {
        if (recognizers.isEmpty() || columns.isEmpty()) {
            return createEmptyResultMap(columns);
        }
        if (recognizers.size() == 1) {
            ColumnRecognizer recognizer = recognizers.get(0);
            return recognizer.recognizeBatch(columns);
        }

        Map<String, Optional<RecognitionResult>> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> result = findFirstMatch(recognizers, column);
            results.put(columnKey, result);
        }
        return results;
    }

    protected Map<String, Optional<RecognitionResult>> createEmptyResultMap(List<DBTableColumn> columns) {
        Map<String, Optional<RecognitionResult>> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            results.put(columnKey, Optional.empty());
        }
        return results;
    }

    protected String getColumnKey(DBTableColumn column) {
        return String.format("%s.%s.%s",
                column.getSchemaName() != null ? column.getSchemaName() : "unknown_schema",
                column.getTableName() != null ? column.getTableName() : "unknown_table",
                column.getName() != null ? column.getName() : "unknown_column");
    }
}
