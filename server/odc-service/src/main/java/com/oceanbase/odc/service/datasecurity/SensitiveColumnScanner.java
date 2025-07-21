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
 * 敏感列识别的编排器，负责根据不同的扫描模式执行识别策略。
 * 使用策略模式重构，消除重复代码。
 */
public class SensitiveColumnScanner {

    private final List<ColumnRecognizer> basicRecognizers;
    private final List<ColumnRecognizer>  aiRecognizers;
    private final ScanningStrategyFactory strategyFactory;

    public SensitiveColumnScanner(List<SensitiveRule> rules, ScanningStrategyFactory strategyFactory) {
        // 在构造时，就将规则分好类，并创建对应的识别器
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

    /**
     * 核心扫描方法
     *
     * @param column 待扫描的列
     * @param mode   用户选择的扫描模式
     * @return 包含一个或两个结果的最终扫描报告
     */
    public ScanResult scan(DBTableColumn column, ScanningModeType mode) {
        ScanningStrategy strategy = strategyFactory.getStrategy(mode);
        return strategy.scan(column, basicRecognizers, aiRecognizers);
    }

    /**
     * 批量扫描方法
     *
     * @param columns 待扫描的列列表
     * @param mode    用户选择的扫描模式
     * @return 扫描结果映射，key为列名，value为扫描结果
     */
    public Map<String, ScanResult> scanBatch(List<DBTableColumn> columns, ScanningModeType mode) {
        ScanningStrategy strategy = strategyFactory.getStrategy(mode);
        return strategy.scanBatch(columns, basicRecognizers, aiRecognizers);
    }
}