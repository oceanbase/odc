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

package com.oceanbase.odc.service.datasecurity.model;


import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author fenyf
 * @date 2025/7/18 17:52
 */
@Getter
@AllArgsConstructor
public class ScanResult {
    private final Optional<RecognitionResult> basicRuleResult;
    private final Optional<RecognitionResult> aiRuleResult;

    public Optional<RecognitionResult> getFinalResult(ScanningModeType scanningMode) {
        switch (scanningMode) {
            case RULES_ONLY:
                return basicRuleResult;
            case AI_ONLY:
                return aiRuleResult;
            case JOINT_RECOGNITION:
                return basicRuleResult.isPresent() ? basicRuleResult : aiRuleResult;
            default:
                return Optional.empty();
        }
    }
}
