/*
 * Copyright (c) 2025 OceanBase.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
public class JointRecognitionStrategy extends AbstractScanningStrategy {

    @Override
    public ScanResult scan(DBTableColumn column, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers) {
        Optional<RecognitionResult> basicResult = findFirstMatch(basicRecognizers, column);
        if (basicResult.isPresent()) {
            return new ScanResult(basicResult, Optional.empty());
        }
        Optional<RecognitionResult> aiResult = findFirstMatch(aiRecognizers, column);
        return new ScanResult(Optional.empty(), aiResult);
    }

    @Override
    public Map<String, ScanResult> scanBatch(List<DBTableColumn> columns, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers) {
        Map<String, Optional<RecognitionResult>> basicResults = findAllFirstMatches(basicRecognizers, columns);

        List<DBTableColumn> remainingColumns = new ArrayList<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> basicResult = basicResults.getOrDefault(columnKey, Optional.empty());
            if (!basicResult.isPresent()) {
                remainingColumns.add(column);
            }
        }
        Map<String, Optional<RecognitionResult>> aiResults = findAllFirstMatches(aiRecognizers, remainingColumns);

        Map<String, ScanResult> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> basicResult = basicResults.getOrDefault(columnKey, Optional.empty());

            if (basicResult.isPresent()) {
                results.put(columnKey, new ScanResult(basicResult, Optional.empty()));
            } else {
                Optional<RecognitionResult> aiResult = aiResults.getOrDefault(columnKey, Optional.empty());
                results.put(columnKey, new ScanResult(Optional.empty(), aiResult));
            }
        }

        return results;
    }
}
