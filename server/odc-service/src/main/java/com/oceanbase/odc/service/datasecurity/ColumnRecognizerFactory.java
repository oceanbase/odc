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

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.odc.service.datasecurity.recognizer.GroovyColumnRecognizer;
import com.oceanbase.odc.service.datasecurity.recognizer.PathColumnRecognizer;
import com.oceanbase.odc.service.datasecurity.recognizer.RegexColumnRecognizer;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/5/30 11:13
 */
public class ColumnRecognizerFactory {

    public static ColumnRecognizer create(@NonNull SensitiveRule rule) {
        switch (rule.getType()) {
            case REGEX:
                return new RegexColumnRecognizer(rule.getDatabaseRegexExpression(), rule.getTableRegexExpression(),
                        rule.getColumnRegexExpression(), rule.getColumnCommentRegexExpression());
            case PATH:
                return new PathColumnRecognizer(rule.getPathIncludes(), rule.getPathExcludes());
            case GROOVY:
                return new GroovyColumnRecognizer(rule.getGroovyScript());
            default:
                String errorMsg = String.format("Unsupported sensitive rule type: %s", rule.getType().name());
                throw new UnsupportedException(ErrorCodes.BadArgument, new Object[] {errorMsg}, errorMsg);
        }
    }

}
