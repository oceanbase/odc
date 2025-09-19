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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.datasecurity.factory.ColumnRecognizerFactory;
import com.oceanbase.odc.service.datasecurity.factory.ScanningStrategyFactory;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.ScanningModeType;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.odc.service.datasecurity.strategy.ScanningStrategy;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
public class SensitiveColumnScanner {

    private final List<ColumnRecognizer> basicRecognizers;
    private final List<ColumnRecognizer> aiRecognizers;
    private final ScanningStrategyFactory strategyFactory;

    public SensitiveColumnScanner(List<SensitiveRule> rules, ScanningStrategyFactory strategyFactory) {
        this.basicRecognizers = rules.stream()
                .filter(r -> r.getType() != SensitiveRuleType.AI)
                .map(ColumnRecognizerFactory::create)
                .collect(Collectors.toList());
        this.aiRecognizers = rules.stream()
                .filter(r -> r.getType() == SensitiveRuleType.AI)
                .map(ColumnRecognizerFactory::create)
                .collect(Collectors.toList());
        this.strategyFactory = strategyFactory;
    }

    public ScanResult scan(DBTableColumn column, ScanningModeType mode) {
        ScanningStrategy strategy = strategyFactory.getStrategy(mode);
        return strategy.scan(column, basicRecognizers, aiRecognizers);
    }

    public Map<String, ScanResult> scanBatch(List<DBTableColumn> columns, ScanningModeType mode) {
        ScanningStrategy strategy = strategyFactory.getStrategy(mode);
        return strategy.scanBatch(columns, basicRecognizers, aiRecognizers);
    }
}
