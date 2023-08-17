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

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/30 10:30
 */
public class SensitiveColumnRecognizer implements ColumnRecognizer {

    private Long sensitiveRuleId;
    private Long maskingAlgorithmId;
    private SensitiveLevel sensitiveLevel;
    private final List<SensitiveRule> sensitiveRules;
    private final List<ColumnRecognizer> recognizers;

    public SensitiveColumnRecognizer(List<SensitiveRule> rules) {
        this.sensitiveRules = rules;
        this.recognizers = new ArrayList<>();
        for (SensitiveRule rule : this.sensitiveRules) {
            this.recognizers.add(ColumnRecognizerFactory.create(rule));
        }
    }

    public Long sensitiveRuleId() {
        return this.sensitiveRuleId;
    }

    public Long maskingAlgorithmId() {
        return this.maskingAlgorithmId;
    }

    public SensitiveLevel sensitiveLevel() {
        return this.sensitiveLevel;
    }

    @Override
    public boolean recognize(DBTableColumn column) {
        for (int i = 0; i < recognizers.size(); i++) {
            if (recognizers.get(i).recognize(column)) {
                SensitiveRule rule = sensitiveRules.get(i);
                this.sensitiveRuleId = rule.getId();
                this.maskingAlgorithmId = rule.getMaskingAlgorithmId();
                this.sensitiveLevel = rule.getLevel();
                return true;
            }
        }
        return false;
    }

}
