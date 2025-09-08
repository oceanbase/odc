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

package com.oceanbase.odc.service.datasecurity.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.ScanningModeType;
import com.oceanbase.odc.service.datasecurity.strategy.AIOnlyStrategy;
import com.oceanbase.odc.service.datasecurity.strategy.JointRecognitionStrategy;
import com.oceanbase.odc.service.datasecurity.strategy.RulesOnlyStrategy;
import com.oceanbase.odc.service.datasecurity.strategy.ScanningStrategy;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
@Component
public class ScanningStrategyFactory {

    private final Map<ScanningModeType, ScanningStrategy> strategies = new HashMap<>();

    public ScanningStrategyFactory() {
        strategies.put(ScanningModeType.RULES_ONLY, new RulesOnlyStrategy());
        strategies.put(ScanningModeType.JOINT_RECOGNITION, new JointRecognitionStrategy());
        strategies.put(ScanningModeType.AI_ONLY, new AIOnlyStrategy());
    }

    public ScanningStrategy getStrategy(ScanningModeType mode) {
        ScanningStrategy strategy = strategies.get(mode);
        if (strategy == null) {
            return new NoOpStrategy();
        }
        return strategy;
    }

    private static class NoOpStrategy implements ScanningStrategy {
        @Override
        public ScanResult scan(DBTableColumn column,
                java.util.List<com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer> basicRecognizers,
                java.util.List<com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer> aiRecognizers) {
            return new ScanResult(Optional.empty(), Optional.empty());
        }

        @Override
        public Map<String, ScanResult> scanBatch(java.util.List<DBTableColumn> columns,
                java.util.List<com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer> basicRecognizers,
                java.util.List<com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer> aiRecognizers) {
            Map<String, ScanResult> results = new HashMap<>();
            for (DBTableColumn column : columns) {
                String columnKey = String.format("%s.%s.%s",
                        column.getSchemaName() != null ? column.getSchemaName() : "unknown_schema",
                        column.getTableName() != null ? column.getTableName() : "unknown_table",
                        column.getName() != null ? column.getName() : "unknown_column");
                results.put(columnKey, new ScanResult(Optional.empty(), Optional.empty()));
            }
            return results;
        }
    }
}
