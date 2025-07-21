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
package com.oceanbase.odc.service.datasecurity.recognizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/30 10:32
 */
public interface ColumnRecognizer {

    /**
     * Recognizing the column in database
     *
     * @param column column {@link DBTableColumn}
     * @return recognizing result
     */
    Optional<RecognitionResult> recognize(DBTableColumn column);

    /**
     * Batch recognizing the columns in database
     *
     * @param columns list of columns {@link DBTableColumn}
     * @return map of recognizing results, key is column identifier, value is
     *         recognizing result
     */
    default Map<String, Optional<RecognitionResult>> recognizeBatch(List<DBTableColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Optional<RecognitionResult>> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> result = recognize(column);
            results.put(columnKey, result);
        }
        return results;
    }

    /**
     * Generate a unique key for the column
     *
     * @param column column {@link DBTableColumn}
     * @return unique column key
     */
    default String getColumnKey(DBTableColumn column) {
        return String.format("%s.%s.%s",
                column.getSchemaName() != null ? column.getSchemaName() : "unknown_schema",
                column.getTableName() != null ? column.getTableName() : "unknown_table",
                column.getName() != null ? column.getName() : "unknown_column");
    }
}