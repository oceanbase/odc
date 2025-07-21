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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * 规则+AI策略实现
 * 同时执行基础规则和AI识别，用于差异化展示两种结果
 * 
 * @author Assistant
 * @date 2025/1/27
 */
public class RulesAndAiStrategy extends AbstractScanningStrategy {

    @Override
    public ScanResult scan(DBTableColumn column, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers) {
        Optional<RecognitionResult> basicResult = findFirstMatch(basicRecognizers, column);
        Optional<RecognitionResult> aiResult = findFirstMatch(aiRecognizers, column);
        return new ScanResult(basicResult, aiResult);
    }

    @Override
    public Map<String, ScanResult> scanBatch(List<DBTableColumn> columns, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers) {
        // 并行执行基础规则和AI识别
        CompletableFuture<Map<String, Optional<RecognitionResult>>> basicFuture = CompletableFuture
                .supplyAsync(() -> findAllFirstMatches(basicRecognizers, columns));
        CompletableFuture<Map<String, Optional<RecognitionResult>>> aiFuture = CompletableFuture
                .supplyAsync(() -> findAllFirstMatches(aiRecognizers, columns));

        // 等待两个任务完成并合并结果
        CompletableFuture.allOf(basicFuture, aiFuture).join();

        Map<String, Optional<RecognitionResult>> basicResults = basicFuture.join();
        Map<String, Optional<RecognitionResult>> aiResults = aiFuture.join();

        Map<String, ScanResult> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> basicResult = basicResults.getOrDefault(columnKey, Optional.empty());
            Optional<RecognitionResult> aiResult = aiResults.getOrDefault(columnKey, Optional.empty());
            results.put(columnKey, new ScanResult(basicResult, aiResult));
        }

        return results;
    }
}