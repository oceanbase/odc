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

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * 抽象扫描策略基类，提供公共的工具方法
 * 
 * @author Assistant
 * @date 2025/1/27
 */
public abstract class AbstractScanningStrategy implements ScanningStrategy {

    /**
     * 从识别器列表中找到第一个匹配的结果
     *
     * @param recognizers 识别器列表
     * @param column      待识别的列
     * @return 第一个匹配的识别结果
     */
    protected Optional<RecognitionResult> findFirstMatch(List<ColumnRecognizer> recognizers, DBTableColumn column) {
        for (ColumnRecognizer recognizer : recognizers) {
            Optional<RecognitionResult> result = recognizer.recognize(column);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * 批量查找所有列的第一个匹配结果
     *
     * @param recognizers 识别器列表
     * @param columns     待识别的列列表
     * @return 列标识符到识别结果的映射
     */
    protected Map<String, Optional<RecognitionResult>> findAllFirstMatches(List<ColumnRecognizer> recognizers,
            List<DBTableColumn> columns) {
        if (recognizers.isEmpty() || columns.isEmpty()) {
            return createEmptyResultMap(columns);
        }

        // 尝试使用批量识别（优先用于AI识别器）
        if (recognizers.size() == 1) {
            ColumnRecognizer recognizer = recognizers.get(0);
            return recognizer.recognizeBatch(columns);
        }

        // 多个识别器时，逐个处理以保证优先级
        Map<String, Optional<RecognitionResult>> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            Optional<RecognitionResult> result = findFirstMatch(recognizers, column);
            results.put(columnKey, result);
        }
        return results;
    }

    /**
     * 为列列表创建空结果映射
     *
     * @param columns 列列表
     * @return 空结果映射
     */
    protected Map<String, Optional<RecognitionResult>> createEmptyResultMap(List<DBTableColumn> columns) {
        Map<String, Optional<RecognitionResult>> results = new HashMap<>();
        for (DBTableColumn column : columns) {
            String columnKey = getColumnKey(column);
            results.put(columnKey, Optional.empty());
        }
        return results;
    }

    /**
     * 生成列的唯一标识符
     *
     * @param column 列信息
     * @return 列标识符
     */
    protected String getColumnKey(DBTableColumn column) {
        return String.format("%s.%s.%s",
                column.getSchemaName() != null ? column.getSchemaName() : "unknown_schema",
                column.getTableName() != null ? column.getTableName() : "unknown_table",
                column.getName() != null ? column.getName() : "unknown_column");
    }
}