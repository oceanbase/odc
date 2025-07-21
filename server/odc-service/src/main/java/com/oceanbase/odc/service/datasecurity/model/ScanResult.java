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

package com.oceanbase.odc.service.datasecurity.model; // 建议放在 model 包下

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScanResult {
    // 基础规则的识别结果 (如果有)
    private final Optional<RecognitionResult> basicRuleResult;
    // AI 规则的识别结果 (如果有)
    private final Optional<RecognitionResult> aiRuleResult;

    /**
     * 根据扫描模式获取最终的识别结果
     *
     * @param scanningMode 扫描模式
     * @return 最终的识别结果
     */
    public Optional<RecognitionResult> getFinalResult(ScanningModeType scanningMode) {
        switch (scanningMode) {
            case RULES_ONLY:
                return basicRuleResult;
            case JOINT_RECOGNITION:
                // 对于联合识别，Scanner已经做过决策，直接返回存在的那个结果
                return basicRuleResult.isPresent() ? basicRuleResult : aiRuleResult;
            case RULES_AND_AI:
                // 对于差异化展示模式，可以根据业务需求调整优先级策略
                return basicRuleResult.isPresent() ? basicRuleResult : aiRuleResult;
            default:
                return Optional.empty();
        }
    }

    /**
     * 判断是否有任何识别结果
     */
    public boolean hasAnyResult() {
        return basicRuleResult.isPresent() || aiRuleResult.isPresent();
    }

    /**
     * 获取所有可用的结果（用于差异化展示场景）
     */
    public List<RecognitionResult> getAllResults() {
        List<RecognitionResult> results = new ArrayList<>();
        basicRuleResult.ifPresent(results::add);
        aiRuleResult.ifPresent(results::add);
        return results;
    }
}