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
 * 联合识别策略实现
 * 优先使用基础规则识别，如果基础规则没有匹配到，则使用AI识别器作为补充
 * 不一致时信任规则结果
 * 
 * @author Assistant
 * @date 2025/1/27
 */
public class JointRecognitionStrategy extends AbstractScanningStrategy {

    @Override
    public ScanResult scan(DBTableColumn column, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers) {
        Optional<RecognitionResult> basicResult = findFirstMatch(basicRecognizers, column);

        // 如果基础规则已经识别出来，就以此为准，不再调用AI
        if (basicResult.isPresent()) {
            return new ScanResult(basicResult, Optional.empty());
        }

        // 否则，调用AI作为补充
        Optional<RecognitionResult> aiResult = findFirstMatch(aiRecognizers, column);
        return new ScanResult(Optional.empty(), aiResult);
    }

    @Override
    public Map<String, ScanResult> scanBatch(List<DBTableColumn> columns, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers) {
        Map<String, Optional<RecognitionResult>> basicResults = findAllFirstMatches(basicRecognizers, columns);

        // 收集没有被基础规则识别的列
        List<DBTableColumn> remainingColumns = new ArrayList<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> basicResult = basicResults.getOrDefault(columnKey, Optional.empty());
            if (!basicResult.isPresent()) {
                remainingColumns.add(column);
            }
        }

        // 对剩余的列进行AI识别
        Map<String, Optional<RecognitionResult>> aiResults = findAllFirstMatches(aiRecognizers, remainingColumns);

        // 合并结果
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