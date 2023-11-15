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

import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/5/30 11:02
 */
public class RegexColumnRecognizer implements ColumnRecognizer {

    private final Pattern databasePattern;
    private final Pattern tablePattern;
    private final Pattern columnPattern;
    private final Pattern columnCommentPattern;

    public RegexColumnRecognizer(String databaseRegex, String tableRegex, String columnRegex, String commentRegex) {
        databasePattern = StringUtils.isNotBlank(databaseRegex) ? Pattern.compile(databaseRegex) : null;
        tablePattern = StringUtils.isNotBlank(tableRegex) ? Pattern.compile(tableRegex) : null;
        columnPattern = StringUtils.isNotBlank(columnRegex) ? Pattern.compile(columnRegex) : null;
        columnCommentPattern = StringUtils.isNotBlank(commentRegex) ? Pattern.compile(commentRegex) : null;
    }

    @Override
    public boolean recognize(DBTableColumn column) {
        try {
            if (databasePattern != null
                    && !databasePattern.matcher(new InterruptibleCharSequence(column.getSchemaName())).matches()) {
                return false;
            }
            if (tablePattern != null
                    && !tablePattern.matcher(new InterruptibleCharSequence(column.getTableName())).matches()) {
                return false;
            }
            if (columnPattern != null
                    && !columnPattern.matcher(new InterruptibleCharSequence(column.getName())).matches()) {
                return false;
            }
            if (columnCommentPattern != null
                    && !columnCommentPattern.matcher(new InterruptibleCharSequence(column.getComment())).matches()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * An implementation of CharSequence that can be interrupted during Regex matching.
     */
    private static class InterruptibleCharSequence implements CharSequence {

        CharSequence inner;

        public InterruptibleCharSequence(CharSequence inner) {
            super();
            this.inner = inner;
        }

        @Override
        public char charAt(int index) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeException("Interrupted!");
            }
            return inner.charAt(index);
        }

        @Override
        public int length() {
            return inner.length();
        }

        @Override
        public @NonNull CharSequence subSequence(int start, int end) {
            return new InterruptibleCharSequence(inner.subSequence(start, end));
        }

        @Override
        public @NonNull String toString() {
            return inner.toString();
        }

    }


}
