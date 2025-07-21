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
package com.oceanbase.odc.service.datasecurity.recognizer;

import java.util.Optional;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/5/30 11:02
 */
public class RegexColumnRecognizer implements ColumnRecognizer {

    // 【修改】直接保存整个规则对象，以便获取 ID 和 Level
    private final SensitiveRule rule;
    private final Pattern databasePattern;
    private final Pattern tablePattern;
    private final Pattern columnPattern;
    private final Pattern columnCommentPattern;

    private static final long MATCH_TIMEOUT_MILLIS = 100L;

    // 【修改】构造函数接收 SensitiveRule 对象
    public RegexColumnRecognizer(SensitiveRule rule) {
        this.rule = rule;
        databasePattern = StringUtils.isNotBlank(rule.getDatabaseRegexExpression()) ? Pattern.compile(rule.getDatabaseRegexExpression()) : null;
        tablePattern = StringUtils.isNotBlank(rule.getTableRegexExpression()) ? Pattern.compile(rule.getTableRegexExpression()) : null;
        columnPattern = StringUtils.isNotBlank(rule.getColumnRegexExpression()) ? Pattern.compile(rule.getColumnRegexExpression()) : null;
        columnCommentPattern = StringUtils.isNotBlank(rule.getColumnCommentRegexExpression()) ? Pattern.compile(rule.getColumnCommentRegexExpression()) : null;
    }

    @Override
    public Optional<RecognitionResult> recognize(DBTableColumn column) {
        try {
            if (databasePattern != null && !databasePattern
                    .matcher(new TimeoutCharSequence(column.getSchemaName(), getTimeoutMillis())).matches()) {
                return Optional.empty();
            }
            if (tablePattern != null && !tablePattern
                    .matcher(new TimeoutCharSequence(column.getTableName(), getTimeoutMillis())).matches()) {
                return Optional.empty();
            }
            if (columnPattern != null && !columnPattern
                    .matcher(new TimeoutCharSequence(column.getName(), getTimeoutMillis())).matches()) {
                return Optional.empty();
            }
            if (columnCommentPattern != null && !columnCommentPattern
                    .matcher(new TimeoutCharSequence(column.getComment(), getTimeoutMillis())).matches()) {
                return Optional.empty();
            }
            // 【修改】如果所有条件都通过，说明匹配成功，构建并返回 RecognitionResult
            RecognitionResult result = RecognitionResult.builder()
                    .matched(true)
                    .matchedRuleId(this.rule.getId())
                    .level(this.rule.getLevel())
                    .sourceRuleType(SensitiveRuleType.REGEX)
                    .build();
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private long getTimeoutMillis() {
        return System.currentTimeMillis() + MATCH_TIMEOUT_MILLIS;
    }

    /**
     * An implementation of CharSequence that can be interrupted during Regex matching if timeout.
     */
    private static class TimeoutCharSequence implements CharSequence {

        private final CharSequence inner;

        private final long timeoutTimeMillis;

        public TimeoutCharSequence(CharSequence inner, long timeoutTimeMillis) {
            super();
            this.inner = inner;
            this.timeoutTimeMillis = timeoutTimeMillis;
        }

        @Override
        public char charAt(int index) {
            if (System.currentTimeMillis() <= timeoutTimeMillis) {
                return inner.charAt(index);
            }
            throw new RuntimeException("Regex matching timeout");
        }

        @Override
        public int length() {
            return inner.length();
        }

        @Override
        public @NonNull CharSequence subSequence(int start, int end) {
            return new TimeoutCharSequence(inner.subSequence(start, end), timeoutTimeMillis);
        }

        @Override
        public @NonNull String toString() {
            return inner.toString();
        }

    }


}
