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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/30 10:54
 */
public class PathColumnRecognizer implements ColumnRecognizer {

    private final List<FieldPathMatcher> pathIncludeMatchers;
    private final List<FieldPathMatcher> pathExcludeMatchers;

    public PathColumnRecognizer(List<String> pathIncludes, List<String> pathExcludes) {
        pathIncludeMatchers = pathIncludes.stream().map(FieldPathMatcher::new).collect(Collectors.toList());
        pathExcludeMatchers = pathExcludes.stream().map(FieldPathMatcher::new).collect(Collectors.toList());
    }

    @Override
    public boolean recognize(DBTableColumn column) {
        try {
            String schemaName = column.getSchemaName();
            String tableName = column.getTableName();
            String columnName = column.getName();
            for (FieldPathMatcher matcher : pathExcludeMatchers) {
                if (matcher.match(schemaName, tableName, columnName)) {
                    return false;
                }
            }
            for (FieldPathMatcher matcher : pathIncludeMatchers) {
                if (matcher.match(schemaName, tableName, columnName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static class FieldPathMatcher {

        private String schemaMatcher;
        private String tableMatcher;
        private String columnMatcher;
        private static final String EXPRESSION_DELIMITER = "\\.";

        public FieldPathMatcher(String expression) {
            init(expression);
        }

        private void init(String expression) {
            PreConditions.notBlank(expression, "pathExpression");
            String[] identifiers = expression.split(EXPRESSION_DELIMITER);
            this.schemaMatcher = identifiers[0];
            if (identifiers.length >= 2) {
                this.tableMatcher = identifiers[1];
            }
            if (identifiers.length >= 3) {
                this.columnMatcher = identifiers[2];
            }
        }

        public boolean match(String schemaName, String tableName, String columnName) {
            boolean match = FilenameUtils.wildcardMatch(schemaName, schemaMatcher, IOCase.INSENSITIVE);
            if (Objects.nonNull(tableMatcher)) {
                match &= FilenameUtils.wildcardMatch(tableName, tableMatcher, IOCase.INSENSITIVE);
            }
            if (Objects.nonNull(columnMatcher)) {
                match &= FilenameUtils.wildcardMatch(columnName, columnMatcher, IOCase.INSENSITIVE);
            }
            return match;
        }
    }

}
